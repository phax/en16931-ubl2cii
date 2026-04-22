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
package com.helger.en16931.ubl2cii;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.cii.d16b.CIID16BCrossIndustryInvoiceTypeMarshaller;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.cii2ubl.CIIToUBL21Converter;
import com.helger.io.file.FilenameHelper;

import oasis.names.specification.ubl.schema.xsd.creditnote_21.CreditNoteType;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;
import un.unece.uncefact.data.standard.crossindustryinvoice._100.CrossIndustryInvoiceType;

/**
 * Round-trip test: CII → UBL 2.1 (via en16931-cii2ubl) → CII (via
 * en16931-ubl2cii). Compares the re-created CII XML with the original to
 * detect data loss in the conversion chain.
 *
 * @author Philip Helger
 */
public final class CIIRoundTripTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (CIIRoundTripTest.class);

  private static final CIID16BCrossIndustryInvoiceTypeMarshaller CII_MARSHALLER = new CIID16BCrossIndustryInvoiceTypeMarshaller ();

  @Test
  public void testRoundTripInvoices ()
  {
    final CIIToUBL21Converter aCIIToUBL = new CIIToUBL21Converter ();

    for (final File aFile : MockSettings.getAllTestFilesCIIInvoice ())
    {
      LOGGER.info ("Round-trip testing " + aFile.getName ());

      // Step 1: Read source CII
      final CrossIndustryInvoiceType aOrigCII = CII_MARSHALLER.read (aFile);
      assertNotNull ("Failed to read CII: " + aFile, aOrigCII);

      // Step 2: CII → UBL Invoice
      final ErrorList aErrorList = new ErrorList ();
      final InvoiceType aUBLInvoice = aCIIToUBL.convertToInvoice (aOrigCII, aErrorList);
      assertTrue ("CII→UBL errors for " + aFile + ": " + aErrorList.toString (), aErrorList.containsNoError ());
      assertNotNull ("CII→UBL returned null for " + aFile, aUBLInvoice);

      // Step 3: UBL Invoice → CII
      aErrorList.clear ();
      final CrossIndustryInvoiceType aRoundTripCII = UBL21InvoiceToCIID16BConverter.convertToCrossIndustryInvoice (aUBLInvoice,
                                                                                                                    aErrorList);
      assertTrue ("UBL→CII errors for " + aFile + ": " + aErrorList.toString (), aErrorList.containsNoError ());
      assertNotNull ("UBL→CII returned null for " + aFile, aRoundTripCII);

      // Step 4: Serialize both to XML and compare
      final String sOrigXML = CII_MARSHALLER.setFormattedOutput (true).getAsString (aOrigCII);
      final String sRoundTripXML = CII_MARSHALLER.setFormattedOutput (true).getAsString (aRoundTripCII);
      assertNotNull ("Failed to serialize original CII: " + aFile, sOrigXML);
      assertNotNull ("Failed to serialize round-trip CII: " + aFile, sRoundTripXML);

      if (!sOrigXML.equals (sRoundTripXML))
      {
        // Write both files for manual inspection
        final String sBaseName = FilenameHelper.getBaseName (aFile.getName ());
        final File aOrigOut = new File ("generated/roundtrip/" + sBaseName + "-orig.xml");
        final File aRoundTripOut = new File ("generated/roundtrip/" + sBaseName + "-roundtrip.xml");
        CII_MARSHALLER.setFormattedOutput (true).write (aOrigCII, aOrigOut);
        CII_MARSHALLER.setFormattedOutput (true).write (aRoundTripCII, aRoundTripOut);

        LOGGER.warn ("Round-trip mismatch for " +
                     aFile.getName () +
                     " - wrote original to " +
                     aOrigOut.getPath () +
                     " and round-trip to " +
                     aRoundTripOut.getPath ());
      }
    }
  }

  @Test
  public void testRoundTripCreditNotes ()
  {
    final CIIToUBL21Converter aCIIToUBL = new CIIToUBL21Converter ();

    for (final File aFile : MockSettings.getAllTestFilesCIICreditNote ())
    {
      LOGGER.info ("Round-trip testing " + aFile.getName ());

      // Step 1: Read source CII
      final CrossIndustryInvoiceType aOrigCII = CII_MARSHALLER.read (aFile);
      assertNotNull ("Failed to read CII: " + aFile, aOrigCII);

      // Step 2: CII → UBL Credit Note
      final ErrorList aErrorList = new ErrorList ();
      final CreditNoteType aUBLCreditNote = aCIIToUBL.convertToCreditNote (aOrigCII, aErrorList);
      assertTrue ("CII→UBL errors for " + aFile + ": " + aErrorList.toString (), aErrorList.containsNoError ());
      assertNotNull ("CII→UBL returned null for " + aFile, aUBLCreditNote);

      // Step 3: UBL Credit Note → CII
      aErrorList.clear ();
      final CrossIndustryInvoiceType aRoundTripCII = UBL21CreditNoteToCIID16BConverter.convertToCrossIndustryInvoice (aUBLCreditNote,
                                                                                                                       aErrorList);
      assertTrue ("UBL→CII errors for " + aFile + ": " + aErrorList.toString (), aErrorList.containsNoError ());
      assertNotNull ("UBL→CII returned null for " + aFile, aRoundTripCII);

      // Step 4: Serialize both to XML and compare
      final String sOrigXML = CII_MARSHALLER.setFormattedOutput (true).getAsString (aOrigCII);
      final String sRoundTripXML = CII_MARSHALLER.setFormattedOutput (true).getAsString (aRoundTripCII);
      assertNotNull ("Failed to serialize original CII: " + aFile, sOrigXML);
      assertNotNull ("Failed to serialize round-trip CII: " + aFile, sRoundTripXML);

      if (!sOrigXML.equals (sRoundTripXML))
      {
        // Write both files for manual inspection
        final String sBaseName = FilenameHelper.getBaseName (aFile.getName ());
        final File aOrigOut = new File ("generated/roundtrip/" + sBaseName + "-orig.xml");
        final File aRoundTripOut = new File ("generated/roundtrip/" + sBaseName + "-roundtrip.xml");
        CII_MARSHALLER.setFormattedOutput (true).write (aOrigCII, aOrigOut);
        CII_MARSHALLER.setFormattedOutput (true).write (aRoundTripCII, aRoundTripOut);

        LOGGER.warn ("Round-trip mismatch for " +
                     aFile.getName () +
                     " - wrote original to " +
                     aOrigOut.getPath () +
                     " and round-trip to " +
                     aRoundTripOut.getPath ());
      }
    }
  }
}
