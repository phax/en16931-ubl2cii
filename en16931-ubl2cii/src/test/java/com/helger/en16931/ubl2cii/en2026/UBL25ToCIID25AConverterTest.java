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
   * The business terms carried over unchanged from the 2017 binding, asserted on the comprehensive
   * invoice. These are the 182 rows the A3 bulk port brought along; the point of asserting them is
   * that the port did not silently lose any of them.
   */
  /**
   * BT-90 shares <code>cac:PartyIdentification/cbc:ID</code> with the party identifiers BT-29,
   * BT-46 and BT-60 and is told apart by <code>@schemeID="SEPA"</code> alone. CII has a dedicated
   * element for it, so it must not become a party identifier as well - the scheme identifier of one
   * of those must be an ISO 6523 ICD code, which BR-CL-10 enforces for the 2017 binding and no
   * Schematron enforces for 2026 yet.
   */
  @Test
  public void testBT90IsNotAPartyIdentifier ()
  {
    final Element e = convertAndValidate ("d25a-coverage-invoice-ubl.xml", true);

    final String sAgr = "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeAgreement";
    final String sSet = "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeSettlement";

    // BT-90 Bank assigned creditor identifier - one dedicated element, no scheme identifier
    assertXPath (e, sSet + "/ram:CreditorReferenceID", "DE98ZZZ09999999999");
    assertNoXPath (e, sSet + "/ram:CreditorReferenceID/@schemeID");

    // and nowhere else - neither as BT-29 nor with the scheme identifier of BT-90-1
    assertNoXPath (e, sAgr + "/ram:SellerTradeParty/ram:ID");
    assertNoXPath (e, sAgr + "/ram:SellerTradeParty/ram:GlobalID");
  }

  @Test
  public void testFullInvoiceCarriedOverTerms ()
  {
    final Element e = convertAndValidate ("d25a-full-invoice-ubl.xml", true);

    final String sAgr = "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeAgreement";
    final String sDel = "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeDelivery";
    final String sSet = "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeSettlement";
    final String sLine = "rsm:SupplyChainTradeTransaction/ram:IncludedSupplyChainTradeLineItem[1]";

    // --- BG-2 PROCESS CONTROL and the document header ------------------------------------------
    // BT-24 / BT-23
    assertXPath (e,
                 "rsm:ExchangedDocumentContext/ram:GuidelineSpecifiedDocumentContextParameter/ram:ID",
                 "urn:cen.eu:en16931:2026");
    assertXPath (e,
                 "rsm:ExchangedDocumentContext/ram:BusinessProcessSpecifiedDocumentContextParameter/ram:ID",
                 "urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");
    // BT-1 / BT-3 / BT-2
    assertXPath (e, "rsm:ExchangedDocument/ram:ID", "D25A-FULL-INV-1");
    assertXPath (e, "rsm:ExchangedDocument/ram:TypeCode", "380");
    assertXPath (e, "rsm:ExchangedDocument/ram:IssueDateTime/udt:DateTimeString", "20260115");

    // --- BG-4 SELLER, BG-5 SELLER POSTAL ADDRESS -----------------------------------------------
    // BT-27 name, BT-28 trading name, BT-29/BT-29-1 identifier, BT-30/BT-30-1 legal registration
    assertXPath (e, sAgr + "/ram:SellerTradeParty/ram:Name", "Seller Ltd");
    assertXPath (e, sAgr + "/ram:SellerTradeParty/ram:SpecifiedLegalOrganization/ram:TradingBusinessName",
                 "Seller Trading Name");
    assertXPath (e, sAgr + "/ram:SellerTradeParty/ram:GlobalID", "4035811234567");
    assertXPath (e, sAgr + "/ram:SellerTradeParty/ram:GlobalID/@schemeID", "0088");
    assertXPath (e, sAgr + "/ram:SellerTradeParty/ram:SpecifiedLegalOrganization/ram:ID", "FN123456x");
    assertXPath (e, sAgr + "/ram:SellerTradeParty/ram:SpecifiedLegalOrganization/ram:ID/@schemeID", "0198");
    // BT-31 VAT identifier
    assertXPath (e, sAgr + "/ram:SellerTradeParty/ram:SpecifiedTaxRegistration/ram:ID", "ATU12345678");
    // BT-35 address line 1, BT-37 city, BT-38 post code, BT-40 country code
    assertXPath (e, sAgr + "/ram:SellerTradeParty/ram:PostalTradeAddress/ram:LineOne", "Main Street 1");
    assertXPath (e, sAgr + "/ram:SellerTradeParty/ram:PostalTradeAddress/ram:CityName", "Vienna");
    assertXPath (e, sAgr + "/ram:SellerTradeParty/ram:PostalTradeAddress/ram:PostcodeCode", "1010");
    assertXPath (e, sAgr + "/ram:SellerTradeParty/ram:PostalTradeAddress/ram:CountryID", "AT");

    // --- BG-7 BUYER, BG-8 BUYER POSTAL ADDRESS -------------------------------------------------
    // BT-44 name, BT-48 VAT identifier, BT-50 address line 1, BT-52 city, BT-53 post code, BT-55
    assertXPath (e, sAgr + "/ram:BuyerTradeParty/ram:Name", "Buyer Ltd");
    assertXPath (e, sAgr + "/ram:BuyerTradeParty/ram:SpecifiedTaxRegistration/ram:ID", "ATU87654321");
    assertXPath (e, sAgr + "/ram:BuyerTradeParty/ram:PostalTradeAddress/ram:LineOne", "Buyer Street 2");
    assertXPath (e, sAgr + "/ram:BuyerTradeParty/ram:PostalTradeAddress/ram:CityName", "Salzburg");
    assertXPath (e, sAgr + "/ram:BuyerTradeParty/ram:PostalTradeAddress/ram:PostcodeCode", "5020");
    assertXPath (e, sAgr + "/ram:BuyerTradeParty/ram:PostalTradeAddress/ram:CountryID", "AT");

    // --- BG-10 PAYEE ---------------------------------------------------------------------------
    // BT-59 name, BT-60/BT-60-1 identifier, BT-61/BT-61-1 legal registration
    assertXPath (e, sSet + "/ram:PayeeTradeParty/ram:Name", "Payee Ltd");
    assertXPath (e, sSet + "/ram:PayeeTradeParty/ram:GlobalID", "4035822222222");
    assertXPath (e, sSet + "/ram:PayeeTradeParty/ram:SpecifiedLegalOrganization/ram:ID", "FN999999z");

    // --- BG-11 / BG-12 SELLER TAX REPRESENTATIVE PARTY AND ADDRESS -----------------------------
    // BT-62 name, BT-63 VAT identifier, BT-64 address line 1, BT-66 city, BT-67 post code, BT-69
    assertXPath (e, sAgr + "/ram:SellerTaxRepresentativeTradeParty/ram:Name", "Tax Rep GmbH");
    assertXPath (e, sAgr + "/ram:SellerTaxRepresentativeTradeParty/ram:SpecifiedTaxRegistration/ram:ID",
                 "ATU11111111");
    assertXPath (e, sAgr + "/ram:SellerTaxRepresentativeTradeParty/ram:PostalTradeAddress/ram:LineOne",
                 "Rep Street 3");
    assertXPath (e, sAgr + "/ram:SellerTaxRepresentativeTradeParty/ram:PostalTradeAddress/ram:CityName", "Vienna");
    assertXPath (e, sAgr + "/ram:SellerTaxRepresentativeTradeParty/ram:PostalTradeAddress/ram:PostcodeCode", "1020");
    assertXPath (e, sAgr + "/ram:SellerTaxRepresentativeTradeParty/ram:PostalTradeAddress/ram:CountryID", "AT");

    // --- Header references ---------------------------------------------------------------------
    // BT-11 project, BT-12 contract, BT-13 purchase order, BT-14 sales order
    assertXPath (e, sAgr + "/ram:SpecifiedProcuringProject/ram:ID", "PROJECT-7");
    assertXPath (e, sAgr + "/ram:ContractReferencedDocument/ram:IssuerAssignedID", "CONTRACT-42");
    assertXPath (e, sAgr + "/ram:BuyerOrderReferencedDocument/ram:IssuerAssignedID", "PO-2026-0001");
    assertXPath (e, sAgr + "/ram:SellerOrderReferencedDocument/ram:IssuerAssignedID", "SO-2026-9");
    // BT-17 tender or lot reference, written with the fixed type code "50" of BT-17-1
    assertXPath (e, sAgr + "/ram:AdditionalReferencedDocument[ram:TypeCode='50']/ram:IssuerAssignedID", "TENDER-5");
    // BT-18/BT-18-1/BT-18-2 invoiced object identifier
    assertXPath (e, sAgr + "/ram:AdditionalReferencedDocument[ram:TypeCode='130']/ram:IssuerAssignedID", "METER-9");
    assertXPath (e, sAgr + "/ram:AdditionalReferencedDocument[ram:TypeCode='130']/ram:ReferenceTypeCode", "AVE");
    // BG-24 supporting document with BT-122, BT-122-1, BT-123 and BT-124
    assertXPath (e, sAgr + "/ram:AdditionalReferencedDocument[ram:TypeCode='916']/ram:IssuerAssignedID", "DOC-916");
    assertXPath (e, sAgr + "/ram:AdditionalReferencedDocument[ram:TypeCode='916']/ram:Name", "Supporting document");
    assertXPath (e, sAgr + "/ram:AdditionalReferencedDocument[ram:TypeCode='916']/ram:URIID",
                 "https://example.org/doc");
    // BT-15 receiving advice, BT-16 despatch advice
    assertXPath (e, sDel + "/ram:ReceivingAdviceReferencedDocument/ram:IssuerAssignedID", "RECEIPT-4");
    assertXPath (e, sDel + "/ram:DespatchAdviceReferencedDocument/ram:IssuerAssignedID", "DESPATCH-3");

    // --- BG-3 PRECEDING INVOICE REFERENCE, now 0..n ---------------------------------------------
    assertXPathCount (e, sSet + "/ram:InvoiceReferencedDocument", 2);
    // BT-25 number, BT-26 issue date
    assertXPath (e, sSet + "/ram:InvoiceReferencedDocument[1]/ram:IssuerAssignedID", "PREV-INV-1");
    assertXPath (e,
                 sSet + "/ram:InvoiceReferencedDocument[1]/ram:FormattedIssueDateTime/qdt:DateTimeString",
                 "20251215");
    assertXPath (e, sSet + "/ram:InvoiceReferencedDocument[2]/ram:IssuerAssignedID", "PREV-INV-2");

    // --- BG-13 DELIVERY INFORMATION and BG-15 DELIVER TO ADDRESS -------------------------------
    // BT-70 party name, BT-71/BT-71-1 location identifier, BT-72 actual delivery date
    assertXPath (e, sDel + "/ram:ShipToTradeParty/ram:Name", "Delivery Site");
    assertXPath (e, sDel + "/ram:ShipToTradeParty/ram:GlobalID", "4035811111111");
    assertXPath (e, sDel + "/ram:ActualDeliverySupplyChainEvent/ram:OccurrenceDateTime/udt:DateTimeString",
                 "20260112");
    // BT-75 address line 1, BT-77 city, BT-78 post code, BT-80 country code
    assertXPath (e, sDel + "/ram:ShipToTradeParty/ram:PostalTradeAddress/ram:LineOne", "Delivery Road 7");
    assertXPath (e, sDel + "/ram:ShipToTradeParty/ram:PostalTradeAddress/ram:CityName", "Linz");
    assertXPath (e, sDel + "/ram:ShipToTradeParty/ram:PostalTradeAddress/ram:PostcodeCode", "4020");
    assertXPath (e, sDel + "/ram:ShipToTradeParty/ram:PostalTradeAddress/ram:CountryID", "AT");

    // --- BG-14 INVOICING PERIOD, BT-5, BT-6, BT-7, BT-19 ----------------------------------------
    assertXPath (e, sSet + "/ram:BillingSpecifiedPeriod/ram:StartDateTime/udt:DateTimeString", "20260101");
    assertXPath (e, sSet + "/ram:BillingSpecifiedPeriod/ram:EndDateTime/udt:DateTimeString", "20260131");
    assertXPath (e, sSet + "/ram:InvoiceCurrencyCode", "EUR");
    assertXPath (e, sSet + "/ram:TaxCurrencyCode", "USD");
    assertXPath (e, sSet + "/ram:ApplicableTradeTax[1]/ram:TaxPointDate/udt:DateString", "20260110");
    assertXPath (e, sSet + "/ram:ReceivableSpecifiedTradeAccountingAccount/ram:ID", "COST-CENTRE-1");

    // --- BG-16 / BG-17 PAYMENT INSTRUCTIONS and CREDIT TRANSFER --------------------------------
    // BT-81 payment means code, BT-82 payment means text, BT-83 remittance information
    assertXPath (e, sSet + "/ram:SpecifiedTradeSettlementPaymentMeans/ram:TypeCode", "58");
    assertXPath (e, sSet + "/ram:SpecifiedTradeSettlementPaymentMeans/ram:Information", "SEPA credit transfer");
    assertXPath (e, sSet + "/ram:PaymentReference", "REMIT-1");
    // BT-84 account identifier, BT-85 account name, BT-86 provider identifier
    assertXPath (e,
                 sSet +
                        "/ram:SpecifiedTradeSettlementPaymentMeans/ram:PayeePartyCreditorFinancialAccount/ram:IBANID",
                 "AT611904300234573201");
    assertXPath (e,
                 sSet +
                        "/ram:SpecifiedTradeSettlementPaymentMeans/ram:PayeePartyCreditorFinancialAccount/ram:AccountName",
                 "Seller Account");
    assertXPath (e,
                 sSet +
                        "/ram:SpecifiedTradeSettlementPaymentMeans/ram:PayeeSpecifiedCreditorFinancialInstitution/ram:BICID",
                 "GIBAATWWXXX");

    // --- BG-20 / BG-21 document level allowances and charges -----------------------------------
    // BT-92 amount, BT-93 base amount, BT-94 percentage, BT-95 category, BT-96 rate, BT-97 reason,
    // BT-98 reason code
    assertXPath (e, sSet + "/ram:SpecifiedTradeAllowanceCharge[1]/ram:ChargeIndicator/udt:Indicator", "false");
    assertXPath (e, sSet + "/ram:SpecifiedTradeAllowanceCharge[1]/ram:ActualAmount", "5");
    assertXPath (e, sSet + "/ram:SpecifiedTradeAllowanceCharge[1]/ram:BasisAmount", "100");
    assertXPath (e, sSet + "/ram:SpecifiedTradeAllowanceCharge[1]/ram:CalculationPercent", "5.00");
    assertXPath (e, sSet + "/ram:SpecifiedTradeAllowanceCharge[1]/ram:CategoryTradeTax/ram:CategoryCode", "S");
    assertXPath (e,
                 sSet + "/ram:SpecifiedTradeAllowanceCharge[1]/ram:CategoryTradeTax/ram:RateApplicablePercent",
                 "20");
    assertXPath (e, sSet + "/ram:SpecifiedTradeAllowanceCharge[1]/ram:Reason", "Volume discount");
    assertXPath (e, sSet + "/ram:SpecifiedTradeAllowanceCharge[1]/ram:ReasonCode", "95");
    // BT-99 to BT-105 on the charge
    assertXPath (e, sSet + "/ram:SpecifiedTradeAllowanceCharge[2]/ram:ChargeIndicator/udt:Indicator", "true");
    assertXPath (e, sSet + "/ram:SpecifiedTradeAllowanceCharge[2]/ram:ActualAmount", "15");
    assertXPath (e, sSet + "/ram:SpecifiedTradeAllowanceCharge[2]/ram:Reason", "Freight");
    assertXPath (e, sSet + "/ram:SpecifiedTradeAllowanceCharge[2]/ram:ReasonCode", "FC");

    // --- BG-22 DOCUMENT TOTALS -----------------------------------------------------------------
    final String sSum = sSet + "/ram:SpecifiedTradeSettlementHeaderMonetarySummation";
    // BT-106 to BT-115
    assertXPath (e, sSum + "/ram:LineTotalAmount", "100");
    assertXPath (e, sSum + "/ram:AllowanceTotalAmount", "5");
    assertXPath (e, sSum + "/ram:ChargeTotalAmount", "15");
    assertXPath (e, sSum + "/ram:TaxBasisTotalAmount", "110");
    // BT-110 in BT-5 and BT-111 in BT-6, told apart by BT-110-1 / BT-111-1
    assertXPath (e, sSum + "/ram:TaxTotalAmount[@currencyID='EUR']", "22");
    assertXPath (e, sSum + "/ram:TaxTotalAmount[@currencyID='USD']", "24.2");
    assertXPath (e, sSum + "/ram:GrandTotalAmount", "132");
    assertXPath (e, sSum + "/ram:TotalPrepaidAmount", "32");
    assertXPath (e, sSum + "/ram:DuePayableAmount", "100");

    // --- BG-23 VAT BREAKDOWN -------------------------------------------------------------------
    // BT-116 taxable amount, BT-117 tax amount, BT-118 category, BT-119 rate
    assertXPath (e, sSet + "/ram:ApplicableTradeTax[1]/ram:BasisAmount", "100");
    assertXPath (e, sSet + "/ram:ApplicableTradeTax[1]/ram:CalculatedAmount", "20");
    assertXPath (e, sSet + "/ram:ApplicableTradeTax[1]/ram:CategoryCode", "S");
    assertXPath (e, sSet + "/ram:ApplicableTradeTax[1]/ram:RateApplicablePercent", "20");
    // BT-120 exemption reason text, BT-121 exemption reason code
    assertXPath (e, sSet + "/ram:ApplicableTradeTax[2]/ram:ExemptionReason", "Reverse charge");
    assertXPath (e, sSet + "/ram:ApplicableTradeTax[2]/ram:ExemptionReasonCode", "VATEX-EU-AE");

    // --- BG-25 INVOICE LINE --------------------------------------------------------------------
    // BT-126 identifier, BT-127 note, BT-129/BT-130 quantity, BT-131 net amount, BT-133 accounting
    assertXPath (e, sLine + "/ram:AssociatedDocumentLineDocument/ram:LineID", "1");
    assertXPath (e, sLine + "/ram:AssociatedDocumentLineDocument/ram:IncludedNote/ram:Content", "Line one note");
    assertXPath (e, sLine + "/ram:SpecifiedLineTradeDelivery/ram:BilledQuantity", "4");
    assertXPath (e, sLine + "/ram:SpecifiedLineTradeDelivery/ram:BilledQuantity/@unitCode", "C62");
    assertXPath (e,
                 sLine +
                        "/ram:SpecifiedLineTradeSettlement/ram:SpecifiedTradeSettlementLineMonetarySummation/ram:LineTotalAmount",
                 "100");
    assertXPath (e,
                 sLine + "/ram:SpecifiedLineTradeSettlement/ram:ReceivableSpecifiedTradeAccountingAccount/ram:ID",
                 "LINE-COST-CENTRE");
    // BT-128/BT-128-1 line object identifier
    assertXPath (e,
                 sLine + "/ram:SpecifiedLineTradeSettlement/ram:AdditionalReferencedDocument/ram:IssuerAssignedID",
                 "OBJ-1");
    assertXPath (e,
                 sLine + "/ram:SpecifiedLineTradeSettlement/ram:AdditionalReferencedDocument/ram:ReferenceTypeCode",
                 "AVE");

    // --- BG-26 INVOICE LINE PERIOD -------------------------------------------------------------
    // BT-134 start date, BT-135 end date
    assertXPath (e,
                 sLine + "/ram:SpecifiedLineTradeSettlement/ram:BillingSpecifiedPeriod/ram:StartDateTime/udt:DateTimeString",
                 "20260101");
    assertXPath (e,
                 sLine + "/ram:SpecifiedLineTradeSettlement/ram:BillingSpecifiedPeriod/ram:EndDateTime/udt:DateTimeString",
                 "20260131");

    // --- BG-27 / BG-28 line allowances and charges ---------------------------------------------
    final String sLineAC = sLine + "/ram:SpecifiedLineTradeSettlement/ram:SpecifiedTradeAllowanceCharge";
    // BT-136 to BT-140 on the allowance
    assertXPath (e, sLineAC + "[1]/ram:ChargeIndicator/udt:Indicator", "false");
    assertXPath (e, sLineAC + "[1]/ram:ActualAmount", "10");
    assertXPath (e, sLineAC + "[1]/ram:BasisAmount", "100");
    assertXPath (e, sLineAC + "[1]/ram:CalculationPercent", "10.00");
    assertXPath (e, sLineAC + "[1]/ram:Reason", "Line discount");
    assertXPath (e, sLineAC + "[1]/ram:ReasonCode", "95");
    // BT-141 to BT-145 on the charge
    assertXPath (e, sLineAC + "[2]/ram:ChargeIndicator/udt:Indicator", "true");
    assertXPath (e, sLineAC + "[2]/ram:ActualAmount", "10");
    assertXPath (e, sLineAC + "[2]/ram:Reason", "Line freight");
    assertXPath (e, sLineAC + "[2]/ram:ReasonCode", "FC");

    // --- BG-29 PRICE DETAILS -------------------------------------------------------------------
    final String sPrice = sLine + "/ram:SpecifiedLineTradeAgreement";
    // BT-146 net price, BT-147/BT-147-1 discount, BT-148 gross price, BT-149/BT-150 base quantity
    assertXPath (e, sPrice + "/ram:NetPriceProductTradePrice/ram:ChargeAmount", "25");
    assertXPath (e,
                 sPrice + "/ram:GrossPriceProductTradePrice/ram:AppliedTradeAllowanceCharge/ram:ActualAmount",
                 "5");
    assertXPath (e,
                 sPrice +
                         "/ram:GrossPriceProductTradePrice/ram:AppliedTradeAllowanceCharge/ram:ChargeIndicator/udt:Indicator",
                 "false");
    assertXPath (e, sPrice + "/ram:GrossPriceProductTradePrice/ram:ChargeAmount", "30");
    assertXPath (e, sPrice + "/ram:GrossPriceProductTradePrice/ram:BasisQuantity", "1");
    assertXPath (e, sPrice + "/ram:GrossPriceProductTradePrice/ram:BasisQuantity/@unitCode", "C62");

    // --- BG-30 LINE VAT INFORMATION ------------------------------------------------------------
    // BT-151 category code, BT-151-1 tax code, BT-152 rate
    assertXPath (e, sLine + "/ram:SpecifiedLineTradeSettlement/ram:ApplicableTradeTax/ram:CategoryCode", "S");
    assertXPath (e, sLine + "/ram:SpecifiedLineTradeSettlement/ram:ApplicableTradeTax/ram:TypeCode", "VAT");
    assertXPath (e,
                 sLine + "/ram:SpecifiedLineTradeSettlement/ram:ApplicableTradeTax/ram:RateApplicablePercent",
                 "20");

    // --- BG-31 ITEM INFORMATION ----------------------------------------------------------------
    final String sItem = sLine + "/ram:SpecifiedTradeProduct";
    // BT-153 name, BT-154 description, BT-155 seller identifier, BT-156 buyer identifier,
    // BT-157/BT-157-1 standard identifier, BT-158/BT-158-1/BT-158-2 classification, BT-159 origin
    assertXPath (e, sItem + "/ram:Name", "Widget");
    assertXPath (e, sItem + "/ram:Description", "A round widget");
    assertXPath (e, sItem + "/ram:SellerAssignedID", "SELLER-ART-1");
    assertXPath (e, sItem + "/ram:BuyerAssignedID", "BUYER-ART-1");
    assertXPath (e, sItem + "/ram:GlobalID", "1234567890128");
    assertXPath (e, sItem + "/ram:GlobalID/@schemeID", "0160");
    assertXPath (e, sItem + "/ram:DesignatedProductClassification/ram:ClassCode", "CLASS-1");
    assertXPath (e, sItem + "/ram:DesignatedProductClassification/ram:ClassCode/@listID", "TST");
    assertXPath (e, sItem + "/ram:DesignatedProductClassification/ram:ClassCode/@listVersionID", "1.0");
    assertXPath (e, sItem + "/ram:OriginTradeCountry/ram:ID", "AT");

    // --- BG-32 ITEM ATTRIBUTE ------------------------------------------------------------------
    // BT-160 name, BT-161a value as text
    assertXPathCount (e, sItem + "/ram:ApplicableProductCharacteristic", 2);
    assertXPath (e, sItem + "/ram:ApplicableProductCharacteristic[1]/ram:Description", "Colour");
    assertXPath (e, sItem + "/ram:ApplicableProductCharacteristic[1]/ram:Value", "Blue");
    assertXPath (e, sItem + "/ram:ApplicableProductCharacteristic[2]/ram:Description", "Weight");
    assertXPath (e, sItem + "/ram:ApplicableProductCharacteristic[2]/ram:Value", "2 kg");
  }

  /**
   * The credit note renames of the carried-over rows: <code>cac:CreditNoteLine</code>,
   * <code>cbc:CreditedQuantity</code> and <code>cbc:CreditNoteTypeCode</code> all have to land in
   * exactly the same CII elements as their invoice counterparts.
   */
  @Test
  public void testFullCreditNoteCarriedOverTerms ()
  {
    final Element e = convertAndValidate ("d25a-full-creditnote-ubl.xml", false);

    final String sLine = "rsm:SupplyChainTradeTransaction/ram:IncludedSupplyChainTradeLineItem[1]";

    // BT-3 from cbc:CreditNoteTypeCode
    assertXPath (e, "rsm:ExchangedDocument/ram:TypeCode", "381");
    // BT-126 and BT-129/BT-130 from cac:CreditNoteLine / cbc:CreditedQuantity
    assertXPath (e, sLine + "/ram:AssociatedDocumentLineDocument/ram:LineID", "1");
    assertXPath (e, sLine + "/ram:SpecifiedLineTradeDelivery/ram:BilledQuantity", "4");
    assertXPath (e, sLine + "/ram:SpecifiedLineTradeDelivery/ram:BilledQuantity/@unitCode", "C62");
    // BT-153 item name and BT-146 net price, to show the line is fully converted
    assertXPath (e, sLine + "/ram:SpecifiedTradeProduct/ram:Name", "Widget");
    assertXPath (e, sLine + "/ram:SpecifiedLineTradeAgreement/ram:NetPriceProductTradePrice/ram:ChargeAmount", "25");
    // BT-27 and BT-44, to show the header is fully converted
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeAgreement/ram:SellerTradeParty/ram:Name",
                 "Seller Ltd");
    assertXPath (e,
                 "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeAgreement/ram:BuyerTradeParty/ram:Name",
                 "Buyer Ltd");
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

  /** The line level document references added in 2026, and BG-39. */
  @Test
  public void testNewLineReferences ()
  {
    final Element e = convertAndValidate ("d25a-new-lineref-invoice-ubl.xml", true);

    final String sLine = "rsm:SupplyChainTradeTransaction/ram:IncludedSupplyChainTradeLineItem[1]";
    final String sAgr = sLine + "/ram:SpecifiedLineTradeAgreement";
    final String sDel = sLine + "/ram:SpecifiedLineTradeDelivery";
    final String sSet = sLine + "/ram:SpecifiedLineTradeSettlement";

    // BT-188 Invoice line purchase order reference + BT-132 its line reference
    assertXPath (e, sAgr + "/ram:BuyerOrderReferencedDocument/ram:IssuerAssignedID", "LINE-PO-1");
    assertXPath (e, sAgr + "/ram:BuyerOrderReferencedDocument/ram:LineID", "PO-LINE-5");
    // BT-200 Invoice line sales order reference + BT-201 its line reference
    assertXPath (e, sAgr + "/ram:SellerOrderReferencedDocument/ram:IssuerAssignedID", "LINE-SO-1");
    assertXPath (e, sAgr + "/ram:SellerOrderReferencedDocument/ram:LineID", "SO-LINE-9");

    // BT-189 Invoice line despatch advice reference + BT-190 its line reference
    assertXPath (e, sDel + "/ram:DespatchAdviceReferencedDocument/ram:IssuerAssignedID", "LINE-DESP-1");
    assertXPath (e, sDel + "/ram:DespatchAdviceReferencedDocument/ram:LineID", "DESP-LINE-2");
    // BT-191 Invoice line receiving advice reference + BT-192 its line reference
    assertXPath (e, sDel + "/ram:ReceivingAdviceReferencedDocument/ram:IssuerAssignedID", "LINE-RECV-1");
    assertXPath (e, sDel + "/ram:ReceivingAdviceReferencedDocument/ram:LineID", "RECV-LINE-3");
    // BT-198 Invoice line delivery note reference + BT-199 its line reference
    assertXPath (e, sDel + "/ram:DeliveryNoteReferencedDocument/ram:IssuerAssignedID", "LINE-DELN-1");
    assertXPath (e, sDel + "/ram:DeliveryNoteReferencedDocument/ram:LineID", "DELN-LINE-4");

    // BG-39 LINE-LEVEL PRECEDING INVOICE REFERENCE
    // BT-217 Line-level preceding invoice reference
    assertXPath (e, sSet + "/ram:InvoiceReferencedDocument/ram:IssuerAssignedID", "LINE-PREV-INV-1");
    // BT-219 Line-level preceding invoice type code
    assertXPath (e, sSet + "/ram:InvoiceReferencedDocument/ram:TypeCode", "380");
    // BT-220 Line-level preceding invoice line reference
    assertXPath (e, sSet + "/ram:InvoiceReferencedDocument/ram:LineID", "PREV-LINE-7");
    // BT-218 Line-level preceding invoice issue date and BT-218-1 its format code. The source
    // mapped this to cbc:IssueTime, which cannot hold a date - see finding 7 of the mapping table.
    assertXPath (e,
                 sSet + "/ram:InvoiceReferencedDocument/ram:FormattedIssueDateTime/qdt:DateTimeString",
                 "20251210");
    assertXPath (e,
                 sSet + "/ram:InvoiceReferencedDocument/ram:FormattedIssueDateTime/qdt:DateTimeString/@format",
                 "102");
  }

  /** BG-37, BG-38 and the item and tax terms added in 2026. */
  @Test
  public void testNewLineDeliveryItemAndTax ()
  {
    final Element e = convertAndValidate ("d25a-new-linedelivery-invoice-ubl.xml", true);

    final String sLine = "rsm:SupplyChainTradeTransaction/ram:IncludedSupplyChainTradeLineItem[1]";
    final String sShipTo = sLine + "/ram:SpecifiedLineTradeDelivery/ram:ShipToTradeParty";

    // BG-37 INVOICE LINE DELIVERY INFORMATION
    // BT-185 Invoice line deliver to party name
    assertXPath (e, sShipTo + "/ram:Name", "Line Delivery Site");
    // BT-186 Invoice line deliver to location identifier and BT-186-1 its scheme identifier.
    // The source maps these two to the *header* ShipToTradeParty although BG-37 is line level,
    // which cannot work for more than one line - see finding 8 of the mapping table.
    assertXPath (e, sShipTo + "/ram:GlobalID", "4035813333333");
    assertXPath (e, sShipTo + "/ram:GlobalID/@schemeID", "0088");
    // BT-187 Invoice line actual delivery date and BT-187-1 its format code
    assertXPath (e,
                 sLine +
                        "/ram:SpecifiedLineTradeDelivery/ram:ActualDeliverySupplyChainEvent/ram:OccurrenceDateTime/udt:DateTimeString",
                 "20260113");
    assertXPath (e,
                 sLine +
                        "/ram:SpecifiedLineTradeDelivery/ram:ActualDeliverySupplyChainEvent/ram:OccurrenceDateTime/udt:DateTimeString/@format",
                 "102");

    // BG-38 INVOICE LINE DELIVER TO ADDRESS - BT-203 to BT-209
    assertXPath (e, sShipTo + "/ram:PostalTradeAddress/ram:LineOne", "Line Delivery Road 1");
    assertXPath (e, sShipTo + "/ram:PostalTradeAddress/ram:LineTwo", "Gate 5");
    assertXPath (e, sShipTo + "/ram:PostalTradeAddress/ram:LineThree", "Dock C");
    assertXPath (e, sShipTo + "/ram:PostalTradeAddress/ram:CityName", "Innsbruck");
    assertXPath (e, sShipTo + "/ram:PostalTradeAddress/ram:PostcodeCode", "6020");
    assertXPath (e, sShipTo + "/ram:PostalTradeAddress/ram:CountrySubDivisionName", "Tirol");
    assertXPath (e, sShipTo + "/ram:PostalTradeAddress/ram:CountryID", "AT");

    // BG-30 LINE VAT INFORMATION
    final String sLineTax = sLine + "/ram:SpecifiedLineTradeSettlement/ram:ApplicableTradeTax";
    // BT-194 Invoiced item exemption reason text
    assertXPath (e, sLineTax + "/ram:ExemptionReason", "Item exempt");
    // BT-195 Invoiced item VAT exemption reason and specification code
    assertXPath (e, sLineTax + "/ram:ExemptionReasonCode", "VATEX-EU-G");
    // BT-196 Goods/services code
    assertXPath (e, sLineTax + "/ram:SupplyTypeCode", "SUPPLY-L");

    // BG-32 ITEM ATTRIBUTE - the first one uses BT-161a, the second BT-161b
    final String sAttr = sLine + "/ram:SpecifiedTradeProduct/ram:ApplicableProductCharacteristic";
    // BT-160 Item attribute name and BT-161a Item attribute value as text
    assertXPath (e, sAttr + "[1]/ram:Description", "Colour");
    assertXPath (e, sAttr + "[1]/ram:Value", "Blue");
    // Exactly one of BT-161a and BT-161b per attribute - rule CII-SR-504
    assertNoXPath (e, sAttr + "[1]/ram:ValueMeasure");
    assertNoXPath (e, sAttr + "[1]/ram:TypeCode");
    // BT-211 Item attribute code
    assertXPath (e, sAttr + "[2]/ram:TypeCode", "AAO");
    // BT-161b Item attribute value as a measure, with BT-212 as its unit of measure code
    assertXPath (e, sAttr + "[2]/ram:ValueMeasure", "65");
    assertXPath (e, sAttr + "[2]/ram:ValueMeasure/@unitCode", "P1");
    assertNoXPath (e, sAttr + "[2]/ram:Value");
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
