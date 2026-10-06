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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.cii.d16b.CIID16BCrossIndustryInvoiceTypeMarshaller;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.en16931.ubl2cii.MockSettings;
import com.helger.io.resource.FileSystemResource;
import com.helger.phive.api.execute.ValidationExecutionManager;
import com.helger.phive.api.result.ValidationResult;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.api.validity.IValidityDeterminator;
import com.helger.phive.xml.source.ValidationSourceXML;
import com.helger.ubl21.UBL21Marshaller;

import oasis.names.specification.ubl.schema.xsd.creditnote_21.CreditNoteType;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;
import un.unece.uncefact.data.standard.crossindustryinvoice._100.CrossIndustryInvoiceType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.SupplyChainTradeLineItemType;

/**
 * Valid UBL 2.1 documents that are outside of the regular test corpus, because they contain
 * something a round trip cannot preserve. Each must convert to a CII D16B document that is valid
 * against the XSD and the EN 16931 Schematron.
 */
public final class UBL21EdgeCaseTest
{
  private static final String BASE_DIR = "src/test/resources/edge/ubl21/";

  private static void _assertValid (@NonNull final File aFile, @NonNull final DVRCoordinate aVESID)
  {
    final ValidationResultList aResultList = ValidationExecutionManager.executeValidation (IValidityDeterminator.createDefault (),
                                                                                           MockSettings.VES_REGISTRY.getOfID (aVESID),
                                                                                           ValidationSourceXML.create (new FileSystemResource (aFile)));
    for (final ValidationResult aResult : aResultList)
      assertTrue (aFile.getName () + ": " + aResult.getErrorList ().toString (), aResult.getErrorList ().isEmpty ());
  }

  private static void _assertValid (@NonNull final String sFilename, @NonNull final CrossIndustryInvoiceType aCII)
  {
    final Document aDoc = new CIID16BCrossIndustryInvoiceTypeMarshaller ().getAsDocument (aCII);
    assertNotNull ("The created CII D16B document of '" + sFilename + "' is not XSD valid", aDoc);

    final ValidationResultList aResultList = ValidationExecutionManager.executeValidation (IValidityDeterminator.createDefault (),
                                                                                           MockSettings.VES_REGISTRY.getOfID (MockSettings.VID_CII_2017),
                                                                                           ValidationSourceXML.create (sFilename,
                                                                                                                       aDoc));
    for (final ValidationResult aResult : aResultList)
      assertTrue (sFilename + ": " + aResult.getErrorList ().toString (), aResult.getErrorList ().isEmpty ());
  }

  @NonNull
  private static CrossIndustryInvoiceType _convertInvoice (@NonNull final String sFilename)
  {
    final File aFile = new File (BASE_DIR, sFilename);
    _assertValid (aFile, MockSettings.VID_UBL_INV_2017);

    final ErrorList aErrorList = new ErrorList ();
    final InvoiceType aUBL = UBL21Marshaller.invoice ().setCollectErrors (aErrorList).read (aFile);
    assertNotNull ("Failed to read " + aFile + ": " + aErrorList, aUBL);

    final CrossIndustryInvoiceType aCII = UBL21InvoiceToCIID16BConverter.convertToCrossIndustryInvoice (aUBL, aErrorList);
    assertTrue ("Errors: " + aErrorList, aErrorList.containsNoError ());
    assertNotNull (aCII);

    _assertValid (sFilename, aCII);
    return aCII;
  }

  @NonNull
  private static CrossIndustryInvoiceType _convertCreditNote (@NonNull final String sFilename)
  {
    final File aFile = new File (BASE_DIR, sFilename);
    _assertValid (aFile, MockSettings.VID_UBL_CN_2017);

    final ErrorList aErrorList = new ErrorList ();
    final CreditNoteType aUBL = UBL21Marshaller.creditNote ().setCollectErrors (aErrorList).read (aFile);
    assertNotNull ("Failed to read " + aFile + ": " + aErrorList, aUBL);

    final CrossIndustryInvoiceType aCII = UBL21CreditNoteToCIID16BConverter.convertToCrossIndustryInvoice (aUBL,
                                                                                                           aErrorList);
    assertTrue ("Errors: " + aErrorList, aErrorList.containsNoError ());
    assertNotNull (aCII);

    _assertValid (sFilename, aCII);
    return aCII;
  }

  @NonNull
  private static SupplyChainTradeLineItemType _line (@NonNull final CrossIndustryInvoiceType aCII, final int nIndex)
  {
    return aCII.getSupplyChainTradeTransaction ().getIncludedSupplyChainTradeLineItemAtIndex (nIndex);
  }

  private static void _assertEmptyCommodityClassificationSkipped (@NonNull final CrossIndustryInvoiceType aCII)
  {
    // BT-158 of line 1 is kept, the empty classification of line 2 has nothing to map
    assertEquals (1, _line (aCII, 0).getSpecifiedTradeProduct ().getDesignatedProductClassificationCount ());
    assertEquals (0, _line (aCII, 1).getSpecifiedTradeProduct ().getDesignatedProductClassificationCount ());
  }

  @Test
  public void testEmptyCommodityClassification ()
  {
    _assertEmptyCommodityClassificationSkipped (_convertInvoice ("edge-empty-commodity-classification-invoice.xml"));
    _assertEmptyCommodityClassificationSkipped (_convertCreditNote ("edge-empty-commodity-classification-creditnote.xml"));
  }
}
