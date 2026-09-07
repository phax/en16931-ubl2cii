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

import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.basics.codelist.EN16931CodeLists;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.AllowanceChargeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.CommodityClassificationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.CreditNoteLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.CustomerPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.ItemPropertyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.ItemType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PaymentMeansType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PaymentTermsType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PeriodType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.ProjectReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.SupplierPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.TaxCategoryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.TaxSchemeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.TaxSubtotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.TaxTotalType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_25.TaxAmountType;
import oasis.names.specification.ubl.schema.xsd.creditnote_25.CreditNoteType;
import un.unece.uncefact.data.standard.cii.d25a.CrossIndustryInvoiceType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.CreditorFinancialAccountType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.CreditorFinancialInstitutionType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.DebtorFinancialAccountType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.DebtorFinancialInstitutionType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.DocumentContextParameterType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.DocumentLineDocumentType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.ExchangedDocumentContextType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.ExchangedDocumentType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.HeaderTradeAgreementType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.HeaderTradeDeliveryType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.HeaderTradeSettlementType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.LineTradeAgreementType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.LineTradeDeliveryType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.LineTradeSettlementType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.ProcuringProjectType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.ProductCharacteristicType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.ProductClassificationType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.ReferencedDocumentType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.SpecifiedPeriodType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.SupplyChainTradeLineItemType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.SupplyChainTradeTransactionType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeAccountingAccountType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeAllowanceChargeType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeCountryType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeCurrencyExchangeType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradePaymentTermsType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradePriceType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeProductType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeSettlementFinancialCardType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeSettlementLineMonetarySummationType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeSettlementPaymentMeansType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeTaxType;
import un.unece.uncefact.data.standard.cii.d25a.udt.CodeType;
import un.unece.uncefact.data.standard.cii.d25a.udt.IDType;
import un.unece.uncefact.data.standard.cii.d25a.udt.QuantityType;
import un.unece.uncefact.data.standard.cii.d25a.udt.RateType;

/**
 * UBL 2.5 Credit Note to CII D25A converter, following EN 16931:2026.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public final class UBL25CreditNoteToCIID25AConverter extends AbstractToCIID25AConverter
{
  private UBL25CreditNoteToCIID25AConverter ()
  {}

  // BG-25 CREDIT NOTE LINE
  @NonNull
  private static SupplyChainTradeLineItemType _convertCreditNoteLine (@NonNull final CreditNoteLineType aUBLLine)
  {
    final SupplyChainTradeLineItemType ret = new SupplyChainTradeLineItemType ();
    final DocumentLineDocumentType aDLDT = new DocumentLineDocumentType ();

    // BT-126
    aDLDT.setLineID (aUBLLine.getIDValue ());

    // BT-127
    for (final var aUBLNote : aUBLLine.getNote ())
      aDLDT.addIncludedNote (convertNote (aUBLNote));

    ret.setAssociatedDocumentLineDocument (aDLDT);

    // SpecifiedTradeProduct
    final TradeProductType aTPT = new TradeProductType ();
    final ItemType aUBLItem = aUBLLine.getItem ();
    // BT-157/BT-157-1
    if (aUBLItem.getStandardItemIdentification () != null)
      aTPT.setGlobalID (convertID (aUBLItem.getStandardItemIdentification ().getID ()));

    // BT-155 Item Seller's identifier
    if (aUBLItem.getSellersItemIdentification () != null)
      aTPT.setSellerAssignedID (aUBLItem.getSellersItemIdentification ().getIDValue ());

    // BT-156 Item Buyer's identifier
    if (aUBLItem.getBuyersItemIdentification () != null)
      aTPT.setBuyerAssignedID (aUBLItem.getBuyersItemIdentification ().getIDValue ());

    // BT-153
    aTPT.addName (convertText (getFirstValue (aUBLItem.getName ())));

    // BT-154
    if (aUBLItem.hasDescriptionEntries ())
      aTPT.addDescription (convertText (aUBLItem.getDescriptionAtIndex (0).getValue ()));

    // BG-32 (BT-160/BT-161)
    for (final ItemPropertyType aUBLAddItemProp : aUBLLine.getItem ().getAdditionalItemProperty ())
    {
      final ProductCharacteristicType aPCT = new ProductCharacteristicType ();
      ifNotNull (convertText (aUBLAddItemProp.getNameValue ()), aPCT::addDescription);
      ifNotNull (convertText (aUBLAddItemProp.getValueValue ()), aPCT::addValue);
      aTPT.addApplicableProductCharacteristic (aPCT);
    }

    // BT-158/BT-158-1/BT-158-2
    for (final CommodityClassificationType aUBLCC : aUBLLine.getItem ().getCommodityClassification ())
    {
      final ProductClassificationType aPCT = new ProductClassificationType ();
      final CodeType aCT = new CodeType ();
      // BT-158-1 listID
      ifNotEmpty (aUBLCC.getItemClassificationCode ().getListID (), aCT::setListID);
      // BT-158-2 listVersionID
      ifNotEmpty (aUBLCC.getItemClassificationCode ().getListVersionID (), aCT::setListVersionID);
      ifNotEmpty (aUBLCC.getItemClassificationCode ().getValue (), aCT::setValue);
      aPCT.setClassCode (aCT);
      aTPT.addDesignatedProductClassification (aPCT);
    }

    // BT-159 Item country of origin
    if (aUBLItem.getOriginCountry () != null)
    {
      final TradeCountryType aTCT = new TradeCountryType ();
      ifNotEmpty (aUBLItem.getOriginCountry ().getIdentificationCodeValue (), aTCT::setID);
      aTPT.setOriginTradeCountry (aTCT);
    }

    ret.setSpecifiedTradeProduct (aTPT);

    // BT-132 Referenced purchase order line reference
    ReferencedDocumentType aRDT = null;
    if (aUBLLine.hasOrderLineReferenceEntries ())
    {
      final var aOrderLineRef = aUBLLine.getOrderLineReferenceAtIndex (0);
      if (StringHelper.isNotEmpty (aOrderLineRef.getLineIDValue ()))
      {
        aRDT = new ReferencedDocumentType ();
        aRDT.setLineID (aOrderLineRef.getLineIDValue ());
      }
    }

    // BG-29 PRICE DETAILS
    TradePriceType aNetPrice = null;
    TradePriceType aGrossPrice = null;
    if (aUBLLine.getPrice () != null)
    {
      final var aUBLPrice = aUBLLine.getPrice ();

      // BT-146 Item net price
      if (aUBLPrice.getPriceAmount () != null)
      {
        aNetPrice = new TradePriceType ();
        aNetPrice.addChargeAmount (convertAmount (aUBLPrice.getPriceAmount ()));
      }

      // BT-147/BT-148 Item price discount / gross price
      if (aUBLPrice.hasAllowanceChargeEntries ())
      {
        final AllowanceChargeType aUBLPriceAC = aUBLPrice.getAllowanceChargeAtIndex (0);
        aGrossPrice = new TradePriceType ();

        // BT-148 Item gross price
        if (aUBLPriceAC.getBaseAmount () != null)
          aGrossPrice.addChargeAmount (convertAmount (aUBLPriceAC.getBaseAmount ()));

        // BT-147 Item price discount
        if (aUBLPriceAC.getAmount () != null)
        {
          final TradeAllowanceChargeType aGrossPriceAC = new TradeAllowanceChargeType ();
          final un.unece.uncefact.data.standard.cii.d25a.udt.IndicatorType aInd = new un.unece.uncefact.data.standard.cii.d25a.udt.IndicatorType ();
          aInd.setIndicator (Boolean.FALSE);
          aGrossPriceAC.setChargeIndicator (aInd);
          aGrossPriceAC.addActualAmount (convertAmount (aUBLPriceAC.getAmount ()));
          aGrossPrice.addAppliedTradeAllowanceCharge (aGrossPriceAC);
        }

        // BT-149/BT-150 base quantity on gross price
        // cii2ubl prefers the gross BasisQuantity, so place it back on gross
        if (aUBLPrice.getBaseQuantity () != null)
        {
          final QuantityType aBQ = new QuantityType ();
          aBQ.setValue (aUBLPrice.getBaseQuantity ().getValue ());
          aBQ.setUnitCode (aUBLPrice.getBaseQuantity ().getUnitCode ());
          aGrossPrice.setBasisQuantity (aBQ);
        }
      }
      else
      {
        // BT-149/BT-150 base quantity on net price (no gross price present)
        if (aNetPrice != null && aUBLPrice.getBaseQuantity () != null)
        {
          final QuantityType aBQ = new QuantityType ();
          aBQ.setValue (aUBLPrice.getBaseQuantity ().getValue ());
          aBQ.setUnitCode (aUBLPrice.getBaseQuantity ().getUnitCode ());
          aNetPrice.setBasisQuantity (aBQ);
        }
      }
    }

    // SpecifiedLineTradeAgreement
    final LineTradeAgreementType aLTAT = new LineTradeAgreementType ();
    if (aRDT != null)
      aLTAT.setBuyerOrderReferencedDocument (aRDT);
    if (aGrossPrice != null)
      aLTAT.setGrossPriceProductTradePrice (aGrossPrice);
    if (aNetPrice != null)
      aLTAT.setNetPriceProductTradePrice (aNetPrice);
    ret.setSpecifiedLineTradeAgreement (aLTAT);

    // BT-129/BT-130 Credited quantity
    final LineTradeDeliveryType aLTDT = new LineTradeDeliveryType ();
    final QuantityType aQuantity = new QuantityType ();
    aQuantity.setUnitCode (aUBLLine.getCreditedQuantity ().getUnitCode ());
    aQuantity.setValue (aUBLLine.getCreditedQuantity ().getValue ());
    aLTDT.setBilledQuantity (aQuantity);
    ret.setSpecifiedLineTradeDelivery (aLTDT);

    // BG-30 (BT-151/BT-152)
    final LineTradeSettlementType aLineTradeSettlement = new LineTradeSettlementType ();
    for (final TaxCategoryType aUBLTaxCategory : aUBLLine.getItem ().getClassifiedTaxCategory ())
    {
      final TaxSchemeType aUBLTaxScheme = aUBLTaxCategory.getTaxScheme ();

      final TradeTaxType aTradeTax = new TradeTaxType ();
      if (aUBLTaxScheme != null)
        ifNotEmpty (aUBLTaxCategory.getTaxScheme ().getIDValue (), aTradeTax::setTypeCode);
      ifNotEmpty (aUBLTaxCategory.getIDValue (), aTradeTax::setCategoryCode);
      ifNotNull (aUBLTaxCategory.getPercentValue (), aTradeTax::setRateApplicablePercent);
      aLineTradeSettlement.addApplicableTradeTax (aTradeTax);
    }

    // BG-26 INVOICE LINE PERIOD (BT-134/BT-135)
    if (aUBLLine.hasInvoicePeriodEntries ())
    {
      final PeriodType aUBLLinePeriod = aUBLLine.getInvoicePeriodAtIndex (0);
      final SpecifiedPeriodType aLineSPT = new SpecifiedPeriodType ();
      if (aUBLLinePeriod.getStartDate () != null)
        aLineSPT.setStartDateTime (convertDateTime (aUBLLinePeriod.getStartDate ().getValueLocal ()));
      if (aUBLLinePeriod.getEndDate () != null)
        aLineSPT.setEndDateTime (convertDateTime (aUBLLinePeriod.getEndDate ().getValueLocal ()));
      aLineTradeSettlement.setBillingSpecifiedPeriod (aLineSPT);
    }

    // BT-128/BT-128-1 Invoice line object identifier
    for (final var aUBLLineDocRef : aUBLLine.getDocumentReference ())
      aLineTradeSettlement.addAdditionalReferencedDocument (convertAdditionalReferencedDocument (aUBLLineDocRef));

    // BG-27 INVOICE LINE ALLOWANCES / BG-28 INVOICE LINE CHARGES
    for (final AllowanceChargeType aUBLLineAC : aUBLLine.getAllowanceCharge ())
      aLineTradeSettlement.addSpecifiedTradeAllowanceCharge (convertSpecifiedTradeAllowanceCharge (aUBLLineAC));

    // BT-131
    final TradeSettlementLineMonetarySummationType aLineMonetarySum = new TradeSettlementLineMonetarySummationType ();
    ifNotNull (convertAmount (aUBLLine.getLineExtensionAmount ()), aLineMonetarySum::addLineTotalAmount);

    // BT-133
    if (aUBLLine.getAccountingCostValue () != null)
    {
      final TradeAccountingAccountType aTAATL = new TradeAccountingAccountType ();
      aTAATL.setID (aUBLLine.getAccountingCostValue ());
      aLineTradeSettlement.addReceivableSpecifiedTradeAccountingAccount (aTAATL);
    }

    aLineTradeSettlement.setSpecifiedTradeSettlementLineMonetarySummation (aLineMonetarySum);
    ret.setSpecifiedLineTradeSettlement (aLineTradeSettlement);

    return ret;
  }

  @NonNull
  private static HeaderTradeSettlementType _createApplicableHeaderTradeSettlement (@NonNull final CreditNoteType aUBLDoc)
  {
    final HeaderTradeSettlementType ret = new HeaderTradeSettlementType ();

    // Use the first PaymentMeans for fields that are header-level in CII
    final PaymentMeansType aUBLPaymentMeans = aUBLDoc.hasPaymentMeansEntries () ? aUBLDoc.getPaymentMeansAtIndex (0)
                                                                                : null;

    // BT-83 Remittance information (header-level in CII)
    if (aUBLPaymentMeans != null && aUBLPaymentMeans.hasPaymentIDEntries ())
      ifNotEmpty (aUBLPaymentMeans.getPaymentIDAtIndex (0).getValue (), x -> ret.addPaymentReference (convertText (x)));

    // Invoice currency code BT-5
    ifNotEmpty (aUBLDoc.getDocumentCurrencyCodeValue (), ret::setInvoiceCurrencyCode);

    // Tax currency code BT-6
    ifNotEmpty (aUBLDoc.getTaxCurrencyCodeValue (), ret::setTaxCurrencyCode);

    // BT-167 VAT accounting currency exchange rate, BT-167-1 target and BT-167-2 source currency
    if (aUBLDoc.getTaxExchangeRate () != null)
    {
      final var aUBLExchangeRate = aUBLDoc.getTaxExchangeRate ();
      final TradeCurrencyExchangeType aTCET = new TradeCurrencyExchangeType ();
      boolean bUseExchangeRate = false;

      // BT-167 Exchange rate
      if (ifNotNull (aUBLExchangeRate.getCalculationRateValue (), x -> {
        final RateType aRate = new RateType ();
        aRate.setValue (x);
        aTCET.setConversionRate (aRate);
      }))
        bUseExchangeRate = true;

      // BT-167-1 Target currency code - the invoice currency BT-5
      if (ifNotEmpty (aUBLExchangeRate.getTargetCurrencyCodeValue (), aTCET::setTargetCurrencyCode))
        bUseExchangeRate = true;

      // BT-167-2 Source currency code - the VAT accounting currency BT-6
      if (ifNotEmpty (aUBLExchangeRate.getSourceCurrencyCodeValue (), aTCET::setSourceCurrencyCode))
        bUseExchangeRate = true;

      if (bUseExchangeRate)
        ret.setInvoiceApplicableTradeCurrencyExchange (aTCET);
    }

    // BG-10 PAYEE
    ifNotNull (convertParty (aUBLDoc.getPayeeParty ()), ret::setPayeeTradeParty);

    // BG-16 PAYMENT INSTRUCTIONS — convert ALL PaymentMeans
    for (final PaymentMeansType aUBLPM : aUBLDoc.getPaymentMeans ())
    {
      final TradeSettlementPaymentMeansType aCIIPM = new TradeSettlementPaymentMeansType ();
      // BT-81 Payment means type code
      ifNotEmpty (aUBLPM.getPaymentMeansCodeValue (), aCIIPM::setTypeCode);

      // BT-82 Payment means text
      if (aUBLPM.getPaymentMeansCode () != null)
        ifNotEmpty (aUBLPM.getPaymentMeansCode ().getName (), x -> aCIIPM.addInformation (convertText (x)));

      // BG-17 CREDIT TRANSFER
      if (aUBLPM.getPayeeFinancialAccount () != null)
      {
        final var aUBLAccount = aUBLPM.getPayeeFinancialAccount ();
        final CreditorFinancialAccountType aCFAT = new CreditorFinancialAccountType ();
        // BT-84 Payment account identifier
        ifNotEmpty (aUBLAccount.getIDValue (), aCFAT::setIBANID);
        // BT-85 Payment account name
        ifNotEmpty (aUBLAccount.getNameValue (), aCFAT::setAccountName);
        aCIIPM.addPayeePartyCreditorFinancialAccount (aCFAT);

        // BT-86 Payment service provider identifier (BIC)
        if (aUBLAccount.getFinancialInstitutionBranch () != null)
        {
          final CreditorFinancialInstitutionType aCFIT = new CreditorFinancialInstitutionType ();
          ifNotNull (convertID (aUBLAccount.getFinancialInstitutionBranch ().getID ()), aCFIT::setBICID);
          aCIIPM.setPayeeSpecifiedCreditorFinancialInstitution (aCFIT);
        }
      }

      // BG-18 PAYMENT CARD INFORMATION
      if (aUBLPM.hasCardAccountEntries ())
      {
        final var aUBLCard = aUBLPM.getCardAccountAtIndex (0);
        final TradeSettlementFinancialCardType aCard = new TradeSettlementFinancialCardType ();
        // BT-87 Payment card primary account number
        ifNotNull (convertID (aUBLCard.getPrimaryAccountNumberID ()), aCard::setID);
        // BT-88 Payment card holder name
        ifNotEmpty (aUBLCard.getHolderNameValue (), aCard::setCardholderName);
        aCIIPM.setApplicableTradeSettlementFinancialCard (aCard);
      }

      // BG-19 DIRECT DEBIT
      if (aUBLPM.getPaymentMandate () != null)
      {
        final var aUBLMandate = aUBLPM.getPaymentMandate ();

        // BT-91 Debited account identifier
        if (aUBLMandate.getPayerFinancialAccount () != null)
        {
          final var aUBLPayerAccount = aUBLMandate.getPayerFinancialAccount ();
          final DebtorFinancialAccountType aDFAT = new DebtorFinancialAccountType ();
          ifNotEmpty (aUBLPayerAccount.getIDValue (), aDFAT::setIBANID);
          // BT-216 Debited account name
          ifNotEmpty (aUBLPayerAccount.getNameValue (), aDFAT::setAccountName);
          aCIIPM.setPayerPartyDebtorFinancialAccount (aDFAT);

          // BT-215 Debited account payment service provider identifier
          if (aUBLPayerAccount.getFinancialInstitutionBranch () != null)
          {
            final DebtorFinancialInstitutionType aDFIT = new DebtorFinancialInstitutionType ();
            if (ifNotNull (convertID (aUBLPayerAccount.getFinancialInstitutionBranch ().getID ()), aDFIT::setBICID))
              aCIIPM.setPayerSpecifiedDebtorFinancialInstitution (aDFIT);
          }
        }
      }

      ret.addSpecifiedTradeSettlementPaymentMeans (aCIIPM);
    }

    // BG-23 VAT BREAKDOWN
    for (final TaxTotalType aUBLTaxTotal : aUBLDoc.getTaxTotal ())
      for (final TaxSubtotalType aUBLTaxSubtotal : aUBLTaxTotal.getTaxSubtotal ())
        ret.addApplicableTradeTax (convertApplicableTradeTax (aUBLTaxSubtotal));

    final Supplier <TradeTaxType> fGetOrCreateTradeTax = () -> {
      if (ret.hasApplicableTradeTaxEntries ())
        return ret.getApplicableTradeTaxAtIndex (0);
      final TradeTaxType aTradeTax = new TradeTaxType ();
      ret.addApplicableTradeTax (aTradeTax);
      return aTradeTax;
    };

    // Value added tax point date BT-7
    // Value added tax point date BT-7
    // EN 16931 rule CII-SR-461 requires at most one TaxPointDate across all
    // ApplicableTradeTax entries, so set it only on the first one.
    ifNotNull (aUBLDoc.getTaxPointDateValue (),
               x -> fGetOrCreateTradeTax.get ().setTaxPointDate (convertDate (x.toLocalDate ())));

    if (aUBLDoc.hasInvoicePeriodEntries ())
    {
      final PeriodType aUBLPeriod = aUBLDoc.getInvoicePeriodAtIndex (0);

      // Value added tax point date code BT-8
      // BT-8 Value added tax point date code (reverse-mapped to CII code list)
      if (aUBLPeriod.hasDescriptionCodeEntries ())
        ifNotEmpty (EN16931CodeLists.mapDueDateTypeCodeUBLToCII (aUBLPeriod.getDescriptionCodeAtIndex (0).getValue ()),
                    x -> fGetOrCreateTradeTax.get ().setDueDateTypeCode (x));

      // BG-14 (BT-73/BT-74)
      if (aUBLPeriod.getStartDate () != null || aUBLPeriod.getEndDate () != null)
      {
        final SpecifiedPeriodType aSPT = new SpecifiedPeriodType ();
        if (aUBLPeriod.getStartDate () != null)
          aSPT.setStartDateTime (convertDateTime (aUBLPeriod.getStartDate ().getValueLocal ()));
        if (aUBLPeriod.getEndDate () != null)
          aSPT.setEndDateTime (convertDateTime (aUBLPeriod.getEndDate ().getValueLocal ()));
        ret.setBillingSpecifiedPeriod (aSPT);
      }
    }

    // BG-20/BG-21
    for (final AllowanceChargeType aUBLAllowanceCharge : aUBLDoc.getAllowanceCharge ())
      ret.addSpecifiedTradeAllowanceCharge (convertSpecifiedTradeAllowanceCharge (aUBLAllowanceCharge));

    // BT-20 Payment terms + BT-9 Payment due date.
    // Since UBL 2.2 the credit note has a native cbc:DueDate, so it is read the same way as on the
    // invoice and no longer from cac:PaymentMeans/cbc:PaymentDueDate.
    for (final PaymentTermsType aUBLPaymentTerms : aUBLDoc.getPaymentTerms ())
      ret.addSpecifiedTradePaymentTerms (convertSpecifiedTradePaymentTerms (aUBLPaymentTerms,
                                                                            aUBLPaymentMeans,
                                                                            aUBLDoc.getDueDateValue ()));

    // BT-9: If no PaymentTerms exist but DueDate is present, create one for the due date
    if (!ret.hasSpecifiedTradePaymentTermsEntries () && aUBLDoc.getDueDateValue () != null)
    {
      final TradePaymentTermsType aTPT = new TradePaymentTermsType ();
      aTPT.setDueDateDateTime (convertDateTime (aUBLDoc.getDueDateValue ().toLocalDate ()));
      ret.addSpecifiedTradePaymentTerms (aTPT);
    }

    // BG-19: BT-89 Mandate reference identifier
    if (aUBLPaymentMeans != null && aUBLPaymentMeans.getPaymentMandate () != null)
    {
      ifNotEmpty (aUBLPaymentMeans.getPaymentMandate ().getIDValue (), x -> {
        // Add to the first (or only) payment terms
        final TradePaymentTermsType aTPT;
        if (ret.hasSpecifiedTradePaymentTermsEntries ())
          aTPT = ret.getSpecifiedTradePaymentTermsAtIndex (0);
        else
        {
          aTPT = new TradePaymentTermsType ();
          ret.addSpecifiedTradePaymentTerms (aTPT);
        }
        aTPT.addDirectDebitMandateID (convertID (aUBLPaymentMeans.getPaymentMandate ().getID ()));
      });
    }

    // BG-19: BT-90 Bank assigned creditor identifier
    // In UBL this is on the Seller or Payee PartyIdentification with @schemeID="SEPA"
    // Check both parties since it may be on either one
    {
      boolean bFound = false;
      for (final var aUBLParty : new oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PartyType [] {
          aUBLDoc.getPayeeParty (),
          aUBLDoc.getAccountingSupplierParty () != null ? aUBLDoc.getAccountingSupplierParty ().getParty () : null })
      {
        if (bFound || aUBLParty == null)
          continue;
        for (final var aUBLPartyID : aUBLParty.getPartyIdentification ())
          if (aUBLPartyID.getID () != null && "SEPA".equals (aUBLPartyID.getID ().getSchemeID ()))
          {
            if (ifNotEmpty (aUBLPartyID.getID ().getValue (), ret::setCreditorReferenceID))
              bFound = true;
            break;
          }
      }
    }

    // BG-3 PRECEDING INVOICE REFERENCE (BT-25/BT-26)
    for (final var aUBLBillingRef : aUBLDoc.getBillingReference ())
    {
      if (aUBLBillingRef.getInvoiceDocumentReference () != null)
      {
        final var aUBLInvRef = aUBLBillingRef.getInvoiceDocumentReference ();
        final ReferencedDocumentType aIRD = new ReferencedDocumentType ();
        // BT-25 Preceding Invoice number
        ifNotEmpty (aUBLInvRef.getIDValue (), aIRD::setIssuerAssignedID);
        // BT-202 Preceding invoice type code
        ifNotEmpty (aUBLInvRef.getDocumentTypeCodeValue (), aIRD::setTypeCode);
        // BT-26 Preceding Invoice issue date and BT-26-1 its format code
        if (aUBLInvRef.getIssueDate () != null)
          aIRD.setFormattedIssueDateTime (convertFormattedDateTime (aUBLInvRef.getIssueDateValueLocal ()));
        ret.addInvoiceReferencedDocument (aIRD);
      }
    }

    // BG-22 DOCUMENT TOTALS
    final ICommonsList <TaxAmountType> aUBLTaxTotalAmounts = new CommonsArrayList <> (aUBLDoc.getTaxTotal (),
                                                                                      TaxTotalType::getTaxAmount);
    ret.setSpecifiedTradeSettlementHeaderMonetarySummation (createSpecifiedTradeSettlementHeaderMonetarySummation (aUBLDoc.getLegalMonetaryTotal (),
                                                                                                                   aUBLTaxTotalAmounts));

    // BT-19
    ifNotEmpty (aUBLDoc.getAccountingCostValue (), x -> {
      final TradeAccountingAccountType aTAAT = new TradeAccountingAccountType ();
      aTAAT.setID (x);
      ret.addReceivableSpecifiedTradeAccountingAccount (aTAAT);
    });

    return ret;
  }

  @Nullable
  public static CrossIndustryInvoiceType convertToCrossIndustryInvoice (@NonNull final CreditNoteType aUBLDoc,
                                                                        @NonNull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aUBLDoc, "UBLCreditNote");
    ValueEnforcer.notNull (aErrorList, "ErrorList");

    final CrossIndustryInvoiceType aCIIInvoice = new CrossIndustryInvoiceType ();

    {
      // BG-2
      final ExchangedDocumentContextType aEDCT = new ExchangedDocumentContextType ();
      // BT-24
      ifNotEmpty (aUBLDoc.getCustomizationIDValue (), x -> {
        final DocumentContextParameterType aDCP = new DocumentContextParameterType ();
        aDCP.setID (x);
        aEDCT.addGuidelineSpecifiedDocumentContextParameter (aDCP);
      });
      // BT-23
      ifNotEmpty (aUBLDoc.getProfileIDValue (), x -> {
        final DocumentContextParameterType aDCP = new DocumentContextParameterType ();
        aDCP.setID (x);
        aEDCT.addBusinessProcessSpecifiedDocumentContextParameter (aDCP);
      });
      aCIIInvoice.setExchangedDocumentContext (aEDCT);
    }

    {
      final ExchangedDocumentType aEDT = new ExchangedDocumentType ();

      // Invoice number BT-1
      ifNotEmpty (aUBLDoc.getIDValue (), aEDT::setID);

      // Invoice type code BT-3
      ifNotEmpty (aUBLDoc.getCreditNoteTypeCodeValue (), aEDT::setTypeCode);

      // BT-2 Invoice issue date, BT-2-1 its format code, BT-166 Invoice issue time and BT-166-1
      // its format code - all four are one CII element
      ifNotNull (aUBLDoc.getIssueDate (),
                 x -> aEDT.setIssueDateTime (convertDateTime (x.getValueLocal (), aUBLDoc.getIssueTimeValue ())));

      // BG-1 CREDIT NOTE NOTE - BT-21 and BT-22
      for (final var aUBLAnnotation : aUBLDoc.getAnnotation ())
        ifNotNull (convertAnnotation (aUBLAnnotation), aEDT::addIncludedNote);

      aCIIInvoice.setExchangedDocument (aEDT);
    }

    {
      final SupplyChainTradeTransactionType aSCTT = new SupplyChainTradeTransactionType ();

      // BG-25
      for (final var aLine : aUBLDoc.getCreditNoteLine ())
        aSCTT.addIncludedSupplyChainTradeLineItem (_convertCreditNoteLine (aLine));

      // ApplicableHeaderTradeAgreement
      {
        final HeaderTradeAgreementType aHTAT = new HeaderTradeAgreementType ();

        // BT-10 Buyer reference and BT-10-1 its scheme identifier.
        // Since UBL 2.5 this is cac:BuyerAssignedReference (0..n) instead of cbc:BuyerReference,
        // and CII D25A has ram:BuyerReferenceID instead of ram:BuyerReference.
        for (final var aUBLBuyerRef : aUBLDoc.getBuyerAssignedReference ())
          ifNotEmpty (getFirstValue (aUBLBuyerRef.getBuyerReference ()), x -> {
            final IDType aID = new IDType ();
            aID.setValue (x);
            // BT-10-1 Buyer reference Scheme identifier
            ifNotEmpty (aUBLBuyerRef.getBuyerReferenceCodeValue (), aID::setSchemeID);
            aHTAT.addBuyerReferenceID (aID);
          });

        // BG-4 SELLER
        final SupplierPartyType aSupplierParty = aUBLDoc.getAccountingSupplierParty ();
        if (aSupplierParty != null)
          aHTAT.setSellerTradeParty (convertParty (aSupplierParty.getParty ()));

        // BG-7 BUYER
        final CustomerPartyType aCustomerParty = aUBLDoc.getAccountingCustomerParty ();
        if (aCustomerParty != null)
          aHTAT.setBuyerTradeParty (convertParty (aCustomerParty.getParty ()));

        // Project reference BT-11 and BT-11-1.
        // Since UBL 2.2 the credit note has a native cac:ProjectReference, so it is no longer
        // squeezed into cac:AdditionalDocumentReference.
        if (aUBLDoc.hasProjectReferenceEntries ())
        {
          final ProjectReferenceType aProjRef = aUBLDoc.getProjectReferenceAtIndex (0);
          ifNotEmpty (aProjRef.getIDValue (), x -> {
            final ProcuringProjectType aProcuringProject = new ProcuringProjectType ();
            aProcuringProject.setID (x);
            // BT-11-1 Project name is mandatory in CII as soon as the container is used
            aProcuringProject.setName ("Project reference");
            aHTAT.setSpecifiedProcuringProject (aProcuringProject);
          });
        }

        // Purchase order reference BT-13
        if (aUBLDoc.getOrderReference () != null)
        {
          if (StringHelper.isNotEmpty (aUBLDoc.getOrderReference ().getIDValue ()))
          {
            final ReferencedDocumentType aBuyerOrderRDT = new ReferencedDocumentType ();
            aBuyerOrderRDT.setIssuerAssignedID (aUBLDoc.getOrderReference ().getIDValue ());
            aHTAT.setBuyerOrderReferencedDocument (aBuyerOrderRDT);
          }

          // BT-14 Sales order reference
          if (StringHelper.isNotEmpty (aUBLDoc.getOrderReference ().getSalesOrderIDValue ()))
          {
            final ReferencedDocumentType aSellerOrderRDT = new ReferencedDocumentType ();
            aSellerOrderRDT.setIssuerAssignedID (aUBLDoc.getOrderReference ().getSalesOrderIDValue ());
            aHTAT.setSellerOrderReferencedDocument (aSellerOrderRDT);
          }
        }

        // BT-12 Contract reference
        if (aUBLDoc.hasContractDocumentReferenceEntries ())
        {
          final ReferencedDocumentType aCRDT = new ReferencedDocumentType ();
          aCRDT.setIssuerAssignedID (aUBLDoc.getContractDocumentReferenceAtIndex (0).getIDValue ());
          aHTAT.setContractReferencedDocument (aCRDT);
        }

        // BG-11 SELLER TAX REPRESENTATIVE PARTY (BT-62/BT-63, BG-12)
        if (aUBLDoc.getTaxRepresentativeParty () != null)
          aHTAT.setSellerTaxRepresentativeTradeParty (convertParty (aUBLDoc.getTaxRepresentativeParty ()));

        // BT-17 Tender or lot reference
        for (final var aUBLOrigRef : aUBLDoc.getOriginatorDocumentReference ())
        {
          final ReferencedDocumentType aOrigRDT = new ReferencedDocumentType ();
          ifNotEmpty (aUBLOrigRef.getIDValue (), aOrigRDT::setIssuerAssignedID);
          aOrigRDT.setTypeCode (EN16931CodeLists.DOCUMENT_TYPE_CODE_ORIGINATOR_DOCUMENT);
          aHTAT.addAdditionalReferencedDocument (aOrigRDT);
        }

        // BG-24 ADDITIONAL SUPPORTING DOCUMENTS + BT-18/BT-18-1
        for (final var aUBLDocDesc : aUBLDoc.getAdditionalDocumentReference ())
          aHTAT.addAdditionalReferencedDocument (convertAdditionalReferencedDocument (aUBLDocDesc));
        aSCTT.setApplicableHeaderTradeAgreement (aHTAT);
      }

      // BG-13 DELIVERY INFORMATION
      {
        final HeaderTradeDeliveryType aHTDT = createApplicableHeaderTradeDelivery (aUBLDoc.hasDeliveryEntries () ? aUBLDoc.getDeliveryAtIndex (0)
                                                                                                                 : null);
        // BT-16 Despatch advice reference
        if (aUBLDoc.hasDespatchDocumentReferenceEntries ())
        {
          final ReferencedDocumentType aDespatchRDT = new ReferencedDocumentType ();
          aDespatchRDT.setIssuerAssignedID (aUBLDoc.getDespatchDocumentReferenceAtIndex (0).getIDValue ());
          aHTDT.setDespatchAdviceReferencedDocument (aDespatchRDT);
        }

        // BT-15 Receiving advice reference
        if (aUBLDoc.hasReceiptDocumentReferenceEntries ())
        {
          final ReferencedDocumentType aReceiptRDT = new ReferencedDocumentType ();
          aReceiptRDT.setIssuerAssignedID (aUBLDoc.getReceiptDocumentReferenceAtIndex (0).getIDValue ());
          aHTDT.setReceivingAdviceReferencedDocument (aReceiptRDT);
        }

        // BT-197 Delivery note reference - new in 2026, UBL 2.5 and CII D25A both have it
        if (aUBLDoc.hasDeliveryNoteDocumentReferenceEntries ())
        {
          final ReferencedDocumentType aDeliveryNoteRDT = new ReferencedDocumentType ();
          if (ifNotEmpty (aUBLDoc.getDeliveryNoteDocumentReferenceAtIndex (0).getIDValue (),
                          aDeliveryNoteRDT::setIssuerAssignedID))
            aHTDT.setDeliveryNoteReferencedDocument (aDeliveryNoteRDT);
        }

        aSCTT.setApplicableHeaderTradeDelivery (aHTDT);
      }

      // ApplicableHeaderTradeSettlement
      aSCTT.setApplicableHeaderTradeSettlement (_createApplicableHeaderTradeSettlement (aUBLDoc));

      aCIIInvoice.setSupplyChainTradeTransaction (aSCTT);
    }

    return aCIIInvoice;
  }
}
