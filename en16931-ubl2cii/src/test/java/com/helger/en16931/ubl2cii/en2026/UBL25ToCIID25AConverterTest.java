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
import static com.helger.en16931.ubl2cii.en2026.MockD25ASettings.convertAndValidate;

import java.io.File;

import org.junit.Test;
import org.w3c.dom.Element;

/**
 * Test class for the EN 16931:2026 converters {@link UBL25InvoiceToCIID25AConverter} and
 * {@link UBL25CreditNoteToCIID25AConverter}.<br>
 * There is no EN 16931 Schematron for the 2026 edition yet, so correctness is established by XSD
 * validity of both sides plus one XPath assertion per business term, with the expressions taken
 * from <code>docs/en16931-2026-syntax.md</code>.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public final class UBL25ToCIID25AConverterTest
{
  /**
   * Convert every file of the corpus. This asserts XSD validity of the input and of the created CII
   * D25A on all of them, which is the floor every other test builds on.
   */
  @Test
  public void testConvertAndValidateAll ()
  {
    for (final File aFile : MockD25ASettings.getAllTestFilesUBL25Invoice ())
      convertAndValidate (aFile.getName (), true);
    for (final File aFile : MockD25ASettings.getAllTestFilesUBL25CreditNote ())
      convertAndValidate (aFile.getName (), false);
  }

  @Test
  public void testMinimalInvoice ()
  {
    final Element e = convertAndValidate ("d25a-minimal-invoice-ubl.xml", true);

    // BT-24 Specification identifier
    assertXPath (e,
                 "rsm:ExchangedDocumentContext/ram:GuidelineSpecifiedDocumentContextParameter/ram:ID",
                 "urn:cen.eu:en16931:2026");
    // BT-23 Business process type
    assertXPath (e,
                 "rsm:ExchangedDocumentContext/ram:BusinessProcessSpecifiedDocumentContextParameter/ram:ID",
                 "urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");
    // BT-1 Invoice number
    assertXPath (e, "rsm:ExchangedDocument/ram:ID", "D25A-MIN-INV-1");
    // BT-3 Invoice type code
    assertXPath (e, "rsm:ExchangedDocument/ram:TypeCode", "380");
    // BT-2 Invoice issue date and BT-2-1 its format code
    assertXPath (e, "rsm:ExchangedDocument/ram:IssueDateTime/udt:DateTimeString", "20260115");
    assertXPath (e, "rsm:ExchangedDocument/ram:IssueDateTime/udt:DateTimeString/@format", "102");
    // BT-5 Invoice currency code
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeSettlement/ram:InvoiceCurrencyCode",
                 "EUR");
    // BT-27 Seller name
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeAgreement/ram:SellerTradeParty/ram:Name",
                 "Seller Ltd");
    // BT-44 Buyer name
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeAgreement/ram:BuyerTradeParty/ram:Name",
                 "Buyer Ltd");
    // BT-112 Invoice total amount with VAT
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeSettlement/ram:SpecifiedTradeSettlementHeaderMonetarySummation/ram:GrandTotalAmount",
                 "120");
    // BT-126 Invoice line identifier
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:IncludedSupplyChainTradeLineItem/ram:AssociatedDocumentLineDocument/ram:LineID",
                 "1");
    // BT-129/BT-130 Invoiced quantity and unit of measure
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:IncludedSupplyChainTradeLineItem/ram:SpecifiedLineTradeDelivery/ram:BilledQuantity",
                 "4");
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:IncludedSupplyChainTradeLineItem/ram:SpecifiedLineTradeDelivery/ram:BilledQuantity/@unitCode",
                 "C62");
    // BT-153 Item name
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:IncludedSupplyChainTradeLineItem/ram:SpecifiedTradeProduct/ram:Name",
                 "Test item");
  }

  @Test
  public void testMinimalCreditNote ()
  {
    final Element e = convertAndValidate ("d25a-minimal-creditnote-ubl.xml", false);

    // BT-24 Specification identifier
    assertXPath (e,
                 "rsm:ExchangedDocumentContext/ram:GuidelineSpecifiedDocumentContextParameter/ram:ID",
                 "urn:cen.eu:en16931:2026");
    // BT-1 Invoice number
    assertXPath (e, "rsm:ExchangedDocument/ram:ID", "D25A-MIN-CN-1");
    // BT-3 Invoice type code - cbc:CreditNoteTypeCode on the UBL side
    assertXPath (e, "rsm:ExchangedDocument/ram:TypeCode", "381");
    // BT-129/BT-130 - cbc:CreditedQuantity on the UBL side
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:IncludedSupplyChainTradeLineItem/ram:SpecifiedLineTradeDelivery/ram:BilledQuantity",
                 "4");
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:IncludedSupplyChainTradeLineItem/ram:SpecifiedLineTradeDelivery/ram:BilledQuantity/@unitCode",
                 "C62");
  }
}
