/*
 * Copyright (C) 2024-2026 Philip Helger
 * http://www.helger.com
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.en16931.ubl2cii.en2026;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedSet;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.cii2ubl.en2026.CIID25AToUBL25Converter;
import com.helger.en16931.ubl2cii.MockRoundTrip;
import com.helger.io.file.FileHelper;
import com.helger.io.file.FilenameHelper;
import com.helger.ubl25.UBL25Marshaller;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;

import oasis.names.specification.ubl.schema.xsd.creditnote_25.CreditNoteType;
import oasis.names.specification.ubl.schema.xsd.invoice_25.InvoiceType;
import un.unece.uncefact.data.standard.cii.d25a.CrossIndustryInvoiceType;

/**
 * Round-trip test for the EN 16931:2026 binding in the direction this library converts: UBL 2.5
 * &rarr; CII D25A (via this library) &rarr; UBL 2.5 (via <code>en16931-cii2ubl</code>).<br>
 * {@link CIID25ARoundTripTest} starts at CII and therefore cannot see a business term that
 * <em>this</em> library drops, because the value is already gone from the UBL input it receives.
 * This test closes that gap: every leaf value of the source UBL document that does not come back is
 * either a documented limitation of the CII syntax or a defect of the mapping.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public final class UBL25RoundTripTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (UBL25RoundTripTest.class);

  private static final String BASE_DEST_DIR = "generated/roundtrip/ubl25/";

  /**
   * Leaf values that a UBL &rarr; CII &rarr; UBL round trip cannot preserve, with the reason.
   * Anything else that goes missing is a defect in one of the two libraries.
   */
  private static final ICommonsOrderedSet <String> EXPECTED_LOSSES;
  static
  {
    EXPECTED_LOSSES = new CommonsLinkedHashSet <> ();
    // cbc:NetworkID of the payment card is mandatory in UBL but it is not an EN 16931 business
    // term and CII has no counterpart, so it cannot survive. en16931-cii2ubl writes the constant
    // "mapped-from-cii" instead - see AbstractCIIToUBLConverterBase.DEFAULT_CARD_ACCOUNT_NETWORK_ID.
    EXPECTED_LOSSES.add ("/Invoice/PaymentMeans/CardAccount/NetworkID");
    EXPECTED_LOSSES.add ("/CreditNote/PaymentMeans/CardAccount/NetworkID");
  }

  private static boolean _isExpectedLoss (@NonNull final String sLeaf)
  {
    return EXPECTED_LOSSES.containsAny (sLeaf::startsWith);
  }

  @NonNull
  private static ICommonsList <String> _roundTrip (@NonNull final File aFile, final boolean bIsInvoice)
  {
    final ErrorList aErrorList = new ErrorList ();

    // Step 1: read the source UBL 2.5, validating it against the UBL 2.5 XSD
    final Document aOrigDoc = bIsInvoice ? UBL25Marshaller.invoice ().getAsDocument (readInvoice (aFile))
                                         : UBL25Marshaller.creditNote ().getAsDocument (readCreditNote (aFile));
    assertNotNull ("Failed to read " + aFile, aOrigDoc);

    // Step 2: UBL 2.5 -> CII D25A, the leg of this library
    final CrossIndustryInvoiceType aCII;
    if (bIsInvoice)
      aCII = UBL25InvoiceToCIID25AConverter.convertToCrossIndustryInvoice (readInvoice (aFile), aErrorList);
    else
      aCII = UBL25CreditNoteToCIID25AConverter.convertToCrossIndustryInvoice (readCreditNote (aFile), aErrorList);
    assertTrue ("UBL->CII errors for " + aFile + ": " + aErrorList, aErrorList.containsNoError ());
    assertNotNull ("UBL->CII returned null for " + aFile, aCII);

    // Step 3: CII D25A -> UBL 2.5, the leg of en16931-cii2ubl
    aErrorList.clear ();
    final CIID25AToUBL25Converter aCIIToUBL = new CIID25AToUBL25Converter ();
    final Document aRoundTripDoc;
    if (bIsInvoice)
    {
      final InvoiceType aUBL = aCIIToUBL.convertToInvoice (aCII, aErrorList);
      assertTrue ("CII->UBL errors for " + aFile + ": " + aErrorList, aErrorList.containsNoError ());
      assertNotNull ("CII->UBL returned null for " + aFile, aUBL);
      aRoundTripDoc = UBL25Marshaller.invoice ().getAsDocument (aUBL);
    }
    else
    {
      final CreditNoteType aUBL = aCIIToUBL.convertToCreditNote (aCII, aErrorList);
      assertTrue ("CII->UBL errors for " + aFile + ": " + aErrorList, aErrorList.containsNoError ());
      assertNotNull ("CII->UBL returned null for " + aFile, aUBL);
      aRoundTripDoc = UBL25Marshaller.creditNote ().getAsDocument (aUBL);
    }
    assertNotNull ("Failed to serialize the round-tripped UBL 2.5 of " + aFile, aRoundTripDoc);

    // Step 4: compare the leaf values
    final ICommonsList <String> aLosses = MockRoundTrip.getUnexpectedLosses (aOrigDoc,
                                                                            aRoundTripDoc,
                                                                            UBL25RoundTripTest::_isExpectedLoss);
    if (aLosses.isNotEmpty ())
    {
      // Keep the round trip result for manual inspection
      final File aOut = new File (BASE_DEST_DIR + FilenameHelper.getBaseName (aFile.getName ()) + "-roundtrip.xml");
      XMLWriter.writeToStream (aRoundTripDoc, FileHelper.getOutputStream (aOut), XMLWriterSettings.DEFAULT_XML_SETTINGS);
      LOGGER.warn ("Round trip of " + aFile.getName () + " lost " + aLosses.size () + " leaf values");
    }
    else
      LOGGER.info ("Round trip of " +
                   aFile.getName () +
                   " lost nothing of its " +
                   MockRoundTrip.getAllLeaves (aOrigDoc).size () +
                   " leaf values");

    final ICommonsList <String> ret = new CommonsArrayList <> ();
    for (final String sLoss : aLosses)
      ret.add (aFile.getName () + " :: " + sLoss);
    return ret;
  }

  @NonNull
  private static InvoiceType readInvoice (@NonNull final File aFile)
  {
    final InvoiceType ret = UBL25Marshaller.invoice ().read (aFile);
    assertNotNull ("Failed to read the UBL 2.5 Invoice " + aFile, ret);
    return ret;
  }

  @NonNull
  private static CreditNoteType readCreditNote (@NonNull final File aFile)
  {
    final CreditNoteType ret = UBL25Marshaller.creditNote ().read (aFile);
    assertNotNull ("Failed to read the UBL 2.5 Credit Note " + aFile, ret);
    return ret;
  }

  @Test
  public void testRoundTripAllUBL25Files ()
  {
    final ICommonsList <String> aAllLosses = new CommonsArrayList <> ();
    for (final File aFile : MockD25ASettings.getAllTestFilesUBL25Invoice ())
      aAllLosses.addAll (_roundTrip (aFile, true));
    for (final File aFile : MockD25ASettings.getAllTestFilesUBL25CreditNote ())
      aAllLosses.addAll (_roundTrip (aFile, false));

    if (aAllLosses.isNotEmpty ())
      fail ("The UBL 2.5 -> CII D25A -> UBL 2.5 round trip lost " +
            aAllLosses.size () +
            " leaf values that are not documented as unpreservable:\n  " +
            String.join ("\n  ", aAllLosses));
  }
}
