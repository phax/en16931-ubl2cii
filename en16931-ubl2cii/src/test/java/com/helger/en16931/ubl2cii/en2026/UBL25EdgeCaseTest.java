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

import static com.helger.en16931.ubl2cii.en2026.MockD25ASettings.assertXPath;
import static com.helger.en16931.ubl2cii.en2026.MockD25ASettings.assertXPathCount;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.cii.d25a.CIID25ACrossIndustryInvoiceTypeMarshaller;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.ubl25.UBL25Marshaller;

import oasis.names.specification.ubl.schema.xsd.invoice_25.InvoiceType;
import un.unece.uncefact.data.standard.cii.d25a.CrossIndustryInvoiceType;

/**
 * Valid UBL 2.5 documents that are outside of the regular test corpus, because they contain
 * something a round trip cannot preserve. Each must convert to a CII D25A document that is valid
 * against the XSD.
 */
public final class UBL25EdgeCaseTest
{
  private static final String BASE_DIR = "src/test/resources/edge/ubl25/";
  private static final String LINE = "rsm:SupplyChainTradeTransaction/ram:IncludedSupplyChainTradeLineItem[1]";

  @NonNull
  private static Element _convertInvoice (@NonNull final String sFilename)
  {
    final File aFile = new File (BASE_DIR, sFilename);
    final ErrorList aErrorList = new ErrorList ();
    final InvoiceType aUBL = UBL25Marshaller.invoice ().setCollectErrors (aErrorList).read (aFile);
    assertNotNull ("Failed to read " + aFile + ": " + aErrorList, aUBL);

    final CrossIndustryInvoiceType aCII = UBL25InvoiceToCIID25AConverter.convertToCrossIndustryInvoice (aUBL, aErrorList);
    assertTrue ("Errors: " + aErrorList, aErrorList.containsNoError ());
    assertNotNull (aCII);

    final Document aDoc = new CIID25ACrossIndustryInvoiceTypeMarshaller ().getAsDocument (aCII);
    assertNotNull ("The created CII D25A document of '" + sFilename + "' is not XSD valid", aDoc);
    return aDoc.getDocumentElement ();
  }

  @Test
  public void testEmptyCommodityClassification ()
  {
    final Element e = _convertInvoice ("d25a-edge-empty-commodity-classification-invoice-ubl.xml");

    // Only the regular classification is BT-158, the empty one has nothing to map
    assertXPathCount (e, LINE + "/ram:SpecifiedTradeProduct/ram:DesignatedProductClassification", 1);
  }

  @Test
  public void testPriceDiscountWithoutGrossPrice ()
  {
    final Element e = _convertInvoice ("d25a-edge-price-discount-invoice-ubl.xml");

    // BT-148 = BT-146 + BT-147
    final String sAgr = LINE + "/ram:SpecifiedLineTradeAgreement";
    assertXPath (e, sAgr + "/ram:GrossPriceProductTradePrice/ram:ChargeAmount", "30");
    assertXPath (e, sAgr + "/ram:GrossPriceProductTradePrice/ram:AppliedTradeAllowanceCharge/ram:ActualAmount", "5");
    assertXPath (e, sAgr + "/ram:NetPriceProductTradePrice/ram:ChargeAmount", "25");
  }
}
