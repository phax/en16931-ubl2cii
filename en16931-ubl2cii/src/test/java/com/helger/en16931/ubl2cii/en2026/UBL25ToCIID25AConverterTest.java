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

import static com.helger.en16931.ubl2cii.en2026.MockD25ASettings.assertNoXPath;
import static com.helger.en16931.ubl2cii.en2026.MockD25ASettings.assertXPath;
import static com.helger.en16931.ubl2cii.en2026.MockD25ASettings.assertXPathCount;
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

  /**
   * The six paths that really changed between the 2017 and the 2026 binding, on the invoice side.
   * Everything else that differs textually between the two mapping documents is base path notation.
   */
  @Test
  public void testHeaderInvoice2026PathChanges ()
  {
    final Element e = convertAndValidate ("d25a-header-invoice-ubl.xml", true);

    // BG-1 - since UBL 2.5 the note is cac:Annotation with a real subject code element, so the
    // "#code#" prefix of the 2017 binding is gone. Two notes, only the first has BT-21.
    assertXPathCount (e, "rsm:ExchangedDocument/ram:IncludedNote", 2);
    // BT-21 Invoice note subject code
    assertXPath (e, "rsm:ExchangedDocument/ram:IncludedNote[1]/ram:SubjectCode", "AAI");
    // BT-22 Invoice note
    assertXPath (e, "rsm:ExchangedDocument/ram:IncludedNote[1]/ram:Content", "Payment within 30 days");
    assertXPath (e, "rsm:ExchangedDocument/ram:IncludedNote[2]/ram:Content", "Second note without a subject code");
    assertNoXPath (e, "rsm:ExchangedDocument/ram:IncludedNote[2]/ram:SubjectCode");

    // BT-10 Buyer reference - ram:BuyerReferenceID since CII D25A, not ram:BuyerReference
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeAgreement/ram:BuyerReferenceID",
                 "BUYER-REF-4711");
    // BT-10-1 Buyer reference Scheme identifier
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeAgreement/ram:BuyerReferenceID/@schemeID",
                 "ADE");
    assertNoXPath (e, "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeAgreement/ram:BuyerReference");

    // BT-31 Seller VAT identifier, BT-31-1 scheme "VA" from UBL BT-31-2 "VAT"
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeAgreement/ram:SellerTradeParty/ram:SpecifiedTaxRegistration/ram:ID[@schemeID='VA']",
                 "ATU12345678");
    // BT-32 Seller tax registration identifier, BT-32-1 scheme "FC" from UBL BT-32-2 "LOC".
    // In the 2017 binding BT-32 was "anything except VAT" and had no fixed CII scheme identifier.
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeAgreement/ram:SellerTradeParty/ram:SpecifiedTaxRegistration/ram:ID[@schemeID='FC']",
                 "FC-987654");
    assertNoXPath (e,
                   "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeAgreement/ram:SellerTradeParty/ram:SpecifiedTaxRegistration/ram:ID[@schemeID='LOC']");

    // BT-127 Invoice line note - unchanged, it must NOT follow BG-1 into cac:Annotation because it
    // has no subject code counterpart
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:IncludedSupplyChainTradeLineItem/ram:AssociatedDocumentLineDocument/ram:IncludedNote/ram:Content",
                 "Line level note");
    assertNoXPath (e,
                   "rsm:SupplyChainTradeTransaction/ram:IncludedSupplyChainTradeLineItem/ram:AssociatedDocumentLineDocument/ram:IncludedNote/ram:SubjectCode");
  }

  /** The business terms added at header level in the 2026 edition. */
  @Test
  public void testNewHeaderTerms ()
  {
    final Element e = convertAndValidate ("d25a-new-header-invoice-ubl.xml", true);

    // BT-2 + BT-166: CII writes both into one element, and BT-166-1 switches the UNTDID 2379
    // format code from "102" to "208"
    assertXPath (e, "rsm:ExchangedDocument/ram:IssueDateTime/udt:DateTimeString", "20260115120503+0100");
    assertXPath (e, "rsm:ExchangedDocument/ram:IssueDateTime/udt:DateTimeString/@format", "208");

    // BT-167 VAT accounting currency exchange rate
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeSettlement/ram:InvoiceApplicableTradeCurrencyExchange/ram:ConversionRate",
                 "1.1000");
    // BT-167-1 Target currency code - the invoice currency BT-5
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeSettlement/ram:InvoiceApplicableTradeCurrencyExchange/ram:TargetCurrencyCode",
                 "EUR");
    // BT-167-2 Source currency code - the VAT accounting currency BT-6
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeSettlement/ram:InvoiceApplicableTradeCurrencyExchange/ram:SourceCurrencyCode",
                 "USD");

    // BT-197 Delivery note reference
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeDelivery/ram:DeliveryNoteReferencedDocument/ram:IssuerAssignedID",
                 "DELNOTE-8");

    // BT-202 Preceding invoice type code
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeSettlement/ram:InvoiceReferencedDocument[1]/ram:TypeCode",
                 "380");

    // BT-216 Debited account name
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeSettlement/ram:SpecifiedTradeSettlementPaymentMeans/ram:PayerPartyDebtorFinancialAccount/ram:AccountName",
                 "Buyer Account");
    // BT-215 Debited account payment service provider identifier
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeSettlement/ram:SpecifiedTradeSettlementPaymentMeans/ram:PayerSpecifiedDebtorFinancialInstitution/ram:BICID",
                 "SPSBAT2SXXX");
  }

  /**
   * BG-33 payment terms, BG-35 early payment discount and BG-36 late payment penalty. UBL puts all
   * three into <code>cac:PaymentTerms</code> with no explicit discriminator, CII has a separate
   * container for each - so the interesting part is that they never get merged.
   */
  @Test
  public void testNewPaymentTerms ()
  {
    final Element e = convertAndValidate ("d25a-new-paymentterms-invoice-ubl.xml", true);

    final String sPT = "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeSettlement/ram:SpecifiedTradePaymentTerms";

    // One CII container per UBL cac:PaymentTerms
    assertXPathCount (e, sPT, 3);

    // BG-33: BT-20 Payment term text
    assertXPath (e, sPT + "[1]/ram:Description", "Net 30 days");
    // BT-9 is 0..1, so it may appear on the first container only
    assertXPath (e, sPT + "[1]/ram:DueDateDateTime/udt:DateTimeString", "20260214");
    assertXPathCount (e, sPT + "/ram:DueDateDateTime", 1);

    // BG-35: BT-170 Discount end date and BT-170-1 its format code
    assertXPath (e, sPT + "[2]/ram:ApplicableTradePaymentDiscountTerms/ram:BasisDateTime/udt:DateTimeString", "20260125");
    assertXPath (e,
                 sPT + "[2]/ram:ApplicableTradePaymentDiscountTerms/ram:BasisDateTime/udt:DateTimeString/@format",
                 "102");
    // BT-171 Discount percentage
    assertXPath (e, sPT + "[2]/ram:ApplicableTradePaymentDiscountTerms/ram:CalculationPercent", "2.00");
    // BT-172 Discount amount
    assertXPath (e, sPT + "[2]/ram:ApplicableTradePaymentDiscountTerms/ram:ActualDiscountAmount", "2.64");

    // BG-36: BT-181 Penalty start date and BT-181-1 its format code
    assertXPath (e, sPT + "[3]/ram:ApplicableTradePaymentPenaltyTerms/ram:BasisDateTime/udt:DateTimeString", "20260215");
    assertXPath (e,
                 sPT + "[3]/ram:ApplicableTradePaymentPenaltyTerms/ram:BasisDateTime/udt:DateTimeString/@format",
                 "102");
    // BT-182 Penalty yearly interest percentage
    assertXPath (e, sPT + "[3]/ram:ApplicableTradePaymentPenaltyTerms/ram:CalculationPercent", "9.20");
    // BT-183 Penalty amount
    assertXPath (e, sPT + "[3]/ram:ApplicableTradePaymentPenaltyTerms/ram:ActualPenaltyAmount", "12.5");

    // The three groups must never be merged into one container
    assertNoXPath (e, sPT + "[1]/ram:ApplicableTradePaymentDiscountTerms");
    assertNoXPath (e, sPT + "[1]/ram:ApplicableTradePaymentPenaltyTerms");
    assertNoXPath (e, sPT + "[2]/ram:ApplicableTradePaymentPenaltyTerms");
    assertNoXPath (e, sPT + "[3]/ram:ApplicableTradePaymentDiscountTerms");
  }

  /** BG-34 charges on behalf of a third party, on both document types. */
  @Test
  public void testNewChargesOnBehalfOfThirdParty ()
  {
    final String sFA = "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeSettlement/ram:SpecifiedFinancialAdjustment";

    final Element eInv = convertAndValidate ("d25a-new-bg34-invoice-ubl.xml", true);
    assertXPathCount (eInv, sFA, 2);
    // BT-179 Charge amount collected on behalf of a third party
    assertXPath (eInv, sFA + "[1]/ram:ActualAmount", "3.2");
    // BT-180 Charges specification
    assertXPath (eInv, sFA + "[1]/ram:Reason", "Copyright levy");
    assertXPath (eInv, sFA + "[2]/ram:ActualAmount", "1.75");
    assertXPath (eInv, sFA + "[2]/ram:Reason", "Recycling fee");

    // The UBL collection lines must not become ordinary invoice lines
    assertXPathCount (eInv, "rsm:SupplyChainTradeTransaction/ram:IncludedSupplyChainTradeLineItem", 1);

    // The credit note reads cac:CollectionCreditNoteLine and produces the same CII
    final Element eCN = convertAndValidate ("d25a-new-bg34-creditnote-ubl.xml", false);
    assertXPathCount (eCN, sFA, 2);
    assertXPath (eCN, sFA + "[1]/ram:ActualAmount", "3.2");
    assertXPath (eCN, sFA + "[1]/ram:Reason", "Copyright levy");
  }

  /** The business terms added to BG-20, BG-21 and BG-23 in the 2026 edition. */
  @Test
  public void testNewAllowanceChargeAndVATBreakdown ()
  {
    final Element e = convertAndValidate ("d25a-new-allowchg-invoice-ubl.xml", true);

    final String sAC = "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeSettlement/ram:SpecifiedTradeAllowanceCharge";
    final String sTT = "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeSettlement/ram:ApplicableTradeTax";

    // BG-20 DOCUMENT LEVEL ALLOWANCE - ChargeIndicator false
    assertXPath (e, sAC + "[1]/ram:ChargeIndicator/udt:Indicator", "false");
    // BT-173 Document level allowance exemption reason text
    assertXPath (e, sAC + "[1]/ram:CategoryTradeTax/ram:ExemptionReason", "Intra-community supply");
    // BT-174 Document level allowance VAT exemption reason and specification code
    assertXPath (e, sAC + "[1]/ram:CategoryTradeTax/ram:ExemptionReasonCode", "VATEX-EU-IC");
    // BT-213 Document level allowance goods/services code
    assertXPath (e, sAC + "[1]/ram:CategoryTradeTax/ram:SupplyTypeCode", "SUPPLY-A");
    // BT-98 Document level allowance reason code - no list identifier, it is not a non-VAT tax
    assertXPath (e, sAC + "[1]/ram:ReasonCode", "95");
    assertNoXPath (e, sAC + "[1]/ram:ReasonCode/@listID");

    // BG-21 DOCUMENT LEVEL CHARGE - ChargeIndicator true
    assertXPath (e, sAC + "[2]/ram:ChargeIndicator/udt:Indicator", "true");
    // BT-175 Document level charge or tax exemption reason text
    assertXPath (e, sAC + "[2]/ram:CategoryTradeTax/ram:ExemptionReason", "Not subject to VAT");
    // BT-176 VAT exemption reason and specification code of the document level charge or tax
    assertXPath (e, sAC + "[2]/ram:CategoryTradeTax/ram:ExemptionReasonCode", "VATEX-EU-O");
    // BT-214 Document level charge goods/services code
    assertXPath (e, sAC + "[2]/ram:CategoryTradeTax/ram:SupplyTypeCode", "SUPPLY-B");
    // BT-177 Document level non-VAT tax code, and BT-177-1 the list identifier that identifies it
    // as one. This is the only place a list identifier may be propagated.
    assertXPath (e, sAC + "[2]/ram:ReasonCode", "ENV");
    assertXPath (e, sAC + "[2]/ram:ReasonCode/@listID", "5153");
    assertXPath (e, sAC + "[2]/ram:ReasonCode/@listAgencyID", "6");

    // BG-23 VAT BREAKDOWN
    // BT-184 VAT breakdown currency - written only where it differs from BT-5
    assertNoXPath (e, sTT + "[1]/ram:CurrencyCode");
    assertXPath (e, sTT + "[2]/ram:CurrencyCode", "USD");
    // BT-210 VAT breakdown goods/services code
    assertXPath (e, sTT + "[2]/ram:SupplyTypeCode", "SUPPLY-C");
    // BT-120/BT-121 stay where they were
    assertXPath (e, sTT + "[2]/ram:ExemptionReason", "Reverse charge");
    assertXPath (e, sTT + "[2]/ram:ExemptionReasonCode", "VATEX-EU-AE");
  }

  /**
   * The two path changes that only affect the credit note: BT-9 and BT-11 have native UBL elements
   * since UBL 2.2, so the 2017 workarounds are gone.
   */
  @Test
  public void testHeaderCreditNote2026PathChanges ()
  {
    final Element e = convertAndValidate ("d25a-header-creditnote-ubl.xml", false);

    // BT-9 Payment due date - from /CreditNote/cbc:DueDate, not cac:PaymentMeans/cbc:PaymentDueDate
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeSettlement/ram:SpecifiedTradePaymentTerms/ram:DueDateDateTime/udt:DateTimeString",
                 "20260214");
    // BT-9-1 Payment due date code
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeSettlement/ram:SpecifiedTradePaymentTerms/ram:DueDateDateTime/udt:DateTimeString/@format",
                 "102");

    // BT-11 Project reference - from /CreditNote/cac:ProjectReference, not from
    // cac:AdditionalDocumentReference
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeAgreement/ram:SpecifiedProcuringProject/ram:ID",
                 "PROJECT-7");
    // BT-11-1 Project name is mandatory in CII as soon as the container is used
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeAgreement/ram:SpecifiedProcuringProject/ram:Name",
                 "Project reference");

    // BG-1 and BT-10/BT-10-1 behave exactly as on the invoice
    assertXPath (e, "rsm:ExchangedDocument/ram:IncludedNote[1]/ram:SubjectCode", "AAI");
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeAgreement/ram:BuyerReferenceID/@schemeID",
                 "ADE");
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
