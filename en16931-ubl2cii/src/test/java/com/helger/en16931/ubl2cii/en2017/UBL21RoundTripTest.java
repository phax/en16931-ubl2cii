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
package com.helger.en16931.ubl2cii.en2017;

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
import com.helger.en16931.cii2ubl.en2017.CIID16BToUBL21Converter;
import com.helger.en16931.ubl2cii.MockRoundTrip;
import com.helger.en16931.ubl2cii.MockSettings;
import com.helger.ubl21.UBL21Marshaller;

import oasis.names.specification.ubl.schema.xsd.creditnote_21.CreditNoteType;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;
import un.unece.uncefact.data.standard.crossindustryinvoice._100.CrossIndustryInvoiceType;

/**
 * Round-trip test for the EN 16931:2017 binding in the direction this library converts: UBL 2.1
 * &rarr; CII D16B (via this library) &rarr; UBL 2.1 (via <code>en16931-cii2ubl</code>).<br>
 * {@link CIIRoundTripTest} starts at CII and therefore cannot see a business term that
 * <em>this</em> library drops, because the value is already gone from the UBL input it receives.
 * This test closes that gap, and it does so against a corpus of real Peppol documents that was not
 * produced by either library.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public final class UBL21RoundTripTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (UBL21RoundTripTest.class);

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
    // "mapped-from-cii" instead - see
    // AbstractCIIToUBLConverterBase.DEFAULT_CARD_ACCOUNT_NETWORK_ID.
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
    final CIID16BToUBL21Converter aCIIToUBL = new CIID16BToUBL21Converter ();

    final Document aOrigDoc;
    final Document aRoundTripDoc;
    if (bIsInvoice)
    {
      // Step 1: read the source UBL 2.1, validating it against the UBL 2.1 XSD
      final InvoiceType aOrigUBL = UBL21Marshaller.invoice ().read (aFile);
      assertNotNull ("Failed to read the UBL 2.1 Invoice " + aFile, aOrigUBL);
      aOrigDoc = UBL21Marshaller.invoice ().getAsDocument (aOrigUBL);

      // Step 2: UBL 2.1 -> CII D16B, the leg of this library
      final CrossIndustryInvoiceType aCII = UBL21InvoiceToCIID16BConverter.convertToCrossIndustryInvoice (aOrigUBL,
                                                                                                          aErrorList);
      assertTrue ("UBL->CII errors for " + aFile + ": " + aErrorList, aErrorList.containsNoError ());
      assertNotNull ("UBL->CII returned null for " + aFile, aCII);

      // Step 3: CII D16B -> UBL 2.1, the leg of en16931-cii2ubl
      aErrorList.clear ();
      final InvoiceType aRoundTripUBL = aCIIToUBL.convertToInvoice (aCII, aErrorList);
      assertTrue ("CII->UBL errors for " + aFile + ": " + aErrorList, aErrorList.containsNoError ());
      assertNotNull ("CII->UBL returned null for " + aFile, aRoundTripUBL);
      aRoundTripDoc = UBL21Marshaller.invoice ().getAsDocument (aRoundTripUBL);
    }
    else
    {
      final CreditNoteType aOrigUBL = UBL21Marshaller.creditNote ().read (aFile);
      assertNotNull ("Failed to read the UBL 2.1 Credit Note " + aFile, aOrigUBL);
      aOrigDoc = UBL21Marshaller.creditNote ().getAsDocument (aOrigUBL);

      final CrossIndustryInvoiceType aCII = UBL21CreditNoteToCIID16BConverter.convertToCrossIndustryInvoice (aOrigUBL,
                                                                                                             aErrorList);
      assertTrue ("UBL->CII errors for " + aFile + ": " + aErrorList, aErrorList.containsNoError ());
      assertNotNull ("UBL->CII returned null for " + aFile, aCII);

      aErrorList.clear ();
      final CreditNoteType aRoundTripUBL = aCIIToUBL.convertToCreditNote (aCII, aErrorList);
      assertTrue ("CII->UBL errors for " + aFile + ": " + aErrorList, aErrorList.containsNoError ());
      assertNotNull ("CII->UBL returned null for " + aFile, aRoundTripUBL);
      aRoundTripDoc = UBL21Marshaller.creditNote ().getAsDocument (aRoundTripUBL);
    }
    assertNotNull ("Failed to serialize the original UBL 2.1 of " + aFile, aOrigDoc);
    assertNotNull ("Failed to serialize the round-tripped UBL 2.1 of " + aFile, aRoundTripDoc);

    // Step 4: compare the leaf values
    final ICommonsList <String> aLosses = MockRoundTrip.getUnexpectedLosses (aOrigDoc,
                                                                             aRoundTripDoc,
                                                                             UBL21RoundTripTest::_isExpectedLoss);
    if (aLosses.isEmpty ())
      LOGGER.info ("Round trip of " +
                   aFile.getName () +
                   " lost nothing of its " +
                   MockRoundTrip.getAllLeaves (aOrigDoc).size () +
                   " leaf values");
    else
      LOGGER.warn ("Round trip of " + aFile.getName () + " lost " + aLosses.size () + " leaf values");

    final ICommonsList <String> ret = new CommonsArrayList <> ();
    for (final String sLoss : aLosses)
      ret.add (aFile.getName () + " :: " + sLoss);
    return ret;
  }

  @Test
  public void testRoundTripAllUBL21Files ()
  {
    final ICommonsList <String> aAllLosses = new CommonsArrayList <> ();
    for (final File aFile : MockSettings.getAllTestFilesUBL21Invoice ())
      aAllLosses.addAll (_roundTrip (aFile, true));
    for (final File aFile : MockSettings.getAllTestFilesUBL21CreditNote ())
      aAllLosses.addAll (_roundTrip (aFile, false));

    if (aAllLosses.isNotEmpty ())
      fail ("The UBL 2.1 -> CII D16B -> UBL 2.1 round trip lost " +
            aAllLosses.size () +
            " leaf values that are not documented as unpreservable:\n  " +
            String.join ("\n  ", aAllLosses));
  }
}
