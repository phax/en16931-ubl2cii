/*
 * Copyright (C) 2024-2025 Philip Helger
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

import java.util.function.Supplier;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.list.ErrorList;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.AllowanceChargeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.CommodityClassificationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.CustomerPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.InvoiceLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.ItemPropertyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.ItemType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PaymentMeansType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PaymentTermsType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PeriodType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.ProjectReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.SupplierPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxCategoryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxSchemeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxSubtotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxTotalType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.TaxAmountType;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;
import un.unece.uncefact.data.standard.crossindustryinvoice._100.CrossIndustryInvoiceType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.*;
import un.unece.uncefact.data.standard.unqualifieddatatype._100.CodeType;
import un.unece.uncefact.data.standard.unqualifieddatatype._100.QuantityType;

/**
 * UBL 2.1 Invoice to CII D16B converter.
 *
 * @author Vartika Rastogi
 * @author Philip Helger
 */
public final class UBL21InvoiceToCIID16BConverter extends AbstractToCIID16BConverter
{
  private UBL21InvoiceToCIID16BConverter ()
  {}

  @Nonnull
  private static SupplyChainTradeLineItemType _convertInvoiceLine (@Nonnull final InvoiceLineType aUBLLine)
  {
    final SupplyChainTradeLineItemType ret = new SupplyChainTradeLineItemType ();
    final DocumentLineDocumentType aDLDT = new DocumentLineDocumentType ();

    aDLDT.setLineID (aUBLLine.getIDValue ());

    for (final var aUBLNote : aUBLLine.getNote ())
      aDLDT.addIncludedNote (convertNote (aUBLNote));

    ret.setAssociatedDocumentLineDocument (aDLDT);

    // SpecifiedTradeProduct
    final TradeProductType aTPT = new TradeProductType ();
    final ItemType aUBLItem = aUBLLine.getItem ();
    if (aUBLItem.getStandardItemIdentification () != null)
      aTPT.setGlobalID (convertID (aUBLItem.getStandardItemIdentification ().getID ()));

    if (aUBLItem.getSellersItemIdentification () != null)
      aTPT.setSellerAssignedID (aUBLItem.getSellersItemIdentification ().getIDValue ());

    aTPT.addName (convertText (aUBLItem.getNameValue ()));

    if (aUBLItem.hasDescriptionEntries ())
      aTPT.setDescription (aUBLItem.getDescriptionAtIndex (0).getValue ());

    // ApplicableProductCharacteristic
    for (final ItemPropertyType aUBLAddItemProp : aUBLLine.getItem ().getAdditionalItemProperty ())
    {
      final ProductCharacteristicType aPCT = new ProductCharacteristicType ();
      ifNotNull (convertText (aUBLAddItemProp.getNameValue ()), aPCT::addDescription);
      ifNotNull (convertText (aUBLAddItemProp.getValueValue ()), aPCT::addValue);
      aTPT.addApplicableProductCharacteristic (aPCT);
    }

    // DesignatedProductClassification
    for (final CommodityClassificationType aUBLCC : aUBLLine.getItem ().getCommodityClassification ())
    {
      final ProductClassificationType aPCT = new ProductClassificationType ();
      final CodeType aCT = new CodeType ();
      ifNotEmpty (aUBLCC.getItemClassificationCode ().getListID (), aCT::setListID);
      ifNotEmpty (aUBLCC.getItemClassificationCode ().getValue (), aCT::setValue);
      aPCT.setClassCode (aCT);
      aTPT.addDesignatedProductClassification (aPCT);
    }
    ret.setSpecifiedTradeProduct (aTPT);

    // BuyerOrderReferencedDocument
    final ReferencedDocumentType aRDT = new ReferencedDocumentType ();
    if (aUBLLine.hasOrderLineReferenceEntries ())
    {
      final var aUBLOrderLineRef = aUBLLine.getOrderLineReferenceAtIndex (0);

      // BT-132
      aRDT.setLineID (aUBLOrderLineRef.getLineIDValue ());
    }

    // NetPriceProductTradePrice
    final TradePriceType aLTPT = new TradePriceType ();
    if (aUBLLine.getPrice () != null && aUBLLine.getPrice ().getPriceAmount () != null)
    {
      aLTPT.addChargeAmount (convertAmount (aUBLLine.getPrice ().getPriceAmount ()));
    }

    // SpecifiedLineTradeAgreement
    final LineTradeAgreementType aLTAT = new LineTradeAgreementType ();
    aLTAT.setBuyerOrderReferencedDocument (aRDT);
    aLTAT.setNetPriceProductTradePrice (aLTPT);
    ret.setSpecifiedLineTradeAgreement (aLTAT);

    // SpecifiedLineTradeDelivery
    final LineTradeDeliveryType aLTDT = new LineTradeDeliveryType ();
    final QuantityType aQuantity = new QuantityType ();
    aQuantity.setUnitCode (aUBLLine.getInvoicedQuantity ().getUnitCode ());
    aQuantity.setValue (aUBLLine.getInvoicedQuantity ().getValue ());
    aLTDT.setBilledQuantity (aQuantity);
    ret.setSpecifiedLineTradeDelivery (aLTDT);

    // SpecifiedLineTradeSettlement
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

    final TradeSettlementLineMonetarySummationType aLineMonetarySum = new TradeSettlementLineMonetarySummationType ();
    ifNotNull (convertAmount (aUBLLine.getLineExtensionAmount ()), aLineMonetarySum::addLineTotalAmount);

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

  @Nonnull
  private static HeaderTradeSettlementType _createApplicableHeaderTradeSettlement (@Nonnull final InvoiceType aUBLDoc)
  {
    final HeaderTradeSettlementType ret = new HeaderTradeSettlementType ();

    final PaymentMeansType aUBLPaymentMeans = aUBLDoc.hasPaymentMeansEntries () ? aUBLDoc.getPaymentMeansAtIndex (0)
                                                                                : null;

    if (aUBLPaymentMeans != null && aUBLPaymentMeans.hasPaymentIDEntries ())
      ifNotEmpty (aUBLPaymentMeans.getPaymentIDAtIndex (0).getValue (), x -> ret.addPaymentReference (convertText (x)));

    // Invoice currency code BT-5
    ifNotEmpty (aUBLDoc.getDocumentCurrencyCodeValue (), ret::setInvoiceCurrencyCode);

    // Tax currency code BT-6
    ifNotEmpty (aUBLDoc.getTaxCurrencyCodeValue (), ret::setTaxCurrencyCode);

    ifNotNull (convertParty (aUBLDoc.getPayeeParty ()), ret::setPayeeTradeParty);

    if (aUBLPaymentMeans != null)
    {
      final TradeSettlementPaymentMeansType aPaymentMeans = new TradeSettlementPaymentMeansType ();
      ifNotEmpty (aUBLPaymentMeans.getPaymentMeansCodeValue (), aPaymentMeans::setTypeCode);

      final CreditorFinancialAccountType aCFAT = new CreditorFinancialAccountType ();
      if (aUBLPaymentMeans.getPayeeFinancialAccount () != null)
        ifNotEmpty (aUBLPaymentMeans.getPayeeFinancialAccount ().getIDValue (), aCFAT::setIBANID);
      aPaymentMeans.setPayeePartyCreditorFinancialAccount (aCFAT);
      ret.addSpecifiedTradeSettlementPaymentMeans (aPaymentMeans);
    }

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
    ifNotNull (aUBLDoc.getTaxPointDateValue (),
               x -> fGetOrCreateTradeTax.get ().setTaxPointDate (convertDate (x.toLocalDate ())));

    if (aUBLDoc.hasInvoicePeriodEntries ())
    {
      final PeriodType aUBLPeriod = aUBLDoc.getInvoicePeriodAtIndex (0);

      // Value added tax point date code BT-8
      if (aUBLPeriod.hasDescriptionCodeEntries ())
        ifNotEmpty (aUBLPeriod.getDescriptionCodeAtIndex (0).getValue (),
                    x -> fGetOrCreateTradeTax.get ().setDueDateTypeCode (x));

      final SpecifiedPeriodType aSPT = new SpecifiedPeriodType ();
      if (aUBLPeriod.getStartDate () != null)
        aSPT.setStartDateTime (convertDateTime (aUBLPeriod.getStartDate ().getValueLocal ()));
      if (aUBLPeriod.getEndDate () != null)
        aSPT.setEndDateTime (convertDateTime (aUBLPeriod.getEndDate ().getValueLocal ()));
      ret.setBillingSpecifiedPeriod (aSPT);
    }

    for (final AllowanceChargeType aUBLAllowanceCharge : aUBLDoc.getAllowanceCharge ())
      ret.addSpecifiedTradeAllowanceCharge (convertSpecifiedTradeAllowanceCharge (aUBLAllowanceCharge));

    for (final PaymentTermsType aUBLPaymentTerms : aUBLDoc.getPaymentTerms ())
      ret.addSpecifiedTradePaymentTerms (convertSpecifiedTradePaymentTerms (aUBLPaymentTerms,
                                                                            aUBLPaymentMeans,
                                                                            aUBLDoc.getDueDateValue ()));

    final ICommonsList <TaxAmountType> aUBLTaxTotalAmounts = new CommonsArrayList <> (aUBLDoc.getTaxTotal (),
                                                                                      TaxTotalType::getTaxAmount);
    ret.setSpecifiedTradeSettlementHeaderMonetarySummation (createSpecifiedTradeSettlementHeaderMonetarySummation (aUBLDoc.getLegalMonetaryTotal (),
                                                                                                                   aUBLTaxTotalAmounts));

    ifNotEmpty (aUBLDoc.getAccountingCostValue (), x -> {
      final TradeAccountingAccountType aTAAT = new TradeAccountingAccountType ();
      aTAAT.setID (x);
      ret.addReceivableSpecifiedTradeAccountingAccount (aTAAT);
    });

    return ret;
  }

  @Nullable
  public static CrossIndustryInvoiceType convertToCrossIndustryInvoice (@Nonnull final InvoiceType aUBLDoc,
                                                                        @Nonnull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aUBLDoc, "UBLInvoice");
    ValueEnforcer.notNull (aErrorList, "ErrorList");

    final CrossIndustryInvoiceType aCIIInvoice = new CrossIndustryInvoiceType ();

    {
      final ExchangedDocumentContextType aEDCT = new ExchangedDocumentContextType ();
      // BT-24
      ifNotEmpty (aUBLDoc.getCustomizationIDValue (), x -> {
        final DocumentContextParameterType aDCP = new DocumentContextParameterType ();
        aDCP.setID (x);
        aEDCT.addGuidelineSpecifiedDocumentContextParameter (aDCP);
      });
      aCIIInvoice.setExchangedDocumentContext (aEDCT);
    }

    {
      final ExchangedDocumentType aEDT = new ExchangedDocumentType ();

      // Invoice number BT-1
      ifNotEmpty (aUBLDoc.getIDValue (), aEDT::setID);

      // Invoice type code BT-3
      ifNotEmpty (aUBLDoc.getInvoiceTypeCodeValue (), aEDT::setTypeCode);

      // IssueDate BT-2
      ifNotNull (aUBLDoc.getIssueDate (), x -> aEDT.setIssueDateTime (convertDateTime (x.getValueLocal ())));

      // Add IncludedNote
      for (final var aNote : aUBLDoc.getNote ())
        aEDT.addIncludedNote (convertNote (aNote));

      aCIIInvoice.setExchangedDocument (aEDT);
    }

    {
      final SupplyChainTradeTransactionType aSCTT = new SupplyChainTradeTransactionType ();

      // IncludedSupplyChainTradeLineItem
      for (final var aLine : aUBLDoc.getInvoiceLine ())
        aSCTT.addIncludedSupplyChainTradeLineItem (_convertInvoiceLine (aLine));

      // ApplicableHeaderTradeAgreement
      {
        final HeaderTradeAgreementType aHTAT = new HeaderTradeAgreementType ();

        // Buyer reference (BT-10)
        ifNotEmpty (aUBLDoc.getBuyerReferenceValue (), aHTAT::setBuyerReference);

        // SellerTradeParty
        final SupplierPartyType aSupplierParty = aUBLDoc.getAccountingSupplierParty ();
        if (aSupplierParty != null)
          aHTAT.setSellerTradeParty (convertParty (aSupplierParty.getParty ()));

        // BuyerTradeParty
        final CustomerPartyType aCustomerParty = aUBLDoc.getAccountingCustomerParty ();
        if (aCustomerParty != null)
          aHTAT.setBuyerTradeParty (convertParty (aCustomerParty.getParty ()));

        // Project reference BT-11
        if (aUBLDoc.hasProjectReferenceEntries ())
        {
          final ProjectReferenceType aProjRef = aUBLDoc.getProjectReferenceAtIndex (0);
          ifNotEmpty (aProjRef.getIDValue (), x -> {
            final ProcuringProjectType aProcuringProject = new ProcuringProjectType ();
            aProcuringProject.setID (x);
            // Constant value according to EN
            aProcuringProject.setName ("Project reference");
            aHTAT.setSpecifiedProcuringProject (aProcuringProject);
          });
        }

        // Purchase order reference BT-13
        if (aUBLDoc.getOrderReference () != null && aUBLDoc.getOrderReference ().getID () != null)
        {
          final ReferencedDocumentType aRDT = new ReferencedDocumentType ();
          aRDT.setIssuerAssignedID (aUBLDoc.getOrderReference ().getIDValue ());
          aHTAT.setBuyerOrderReferencedDocument (aRDT);
        }

        // ContractReferencedDocument
        if (aUBLDoc.hasContractDocumentReferenceEntries ())
        {
          final ReferencedDocumentType aCRDT = new ReferencedDocumentType ();
          aCRDT.setIssuerAssignedID (aUBLDoc.getContractDocumentReferenceAtIndex (0).getIDValue ());
          aHTAT.setContractReferencedDocument (aCRDT);
        }

        // AdditionalReferencedDocument
        for (final var aUBLDocDesc : aUBLDoc.getAdditionalDocumentReference ())
          aHTAT.addAdditionalReferencedDocument (convertAdditionalReferencedDocument (aUBLDocDesc));
        aSCTT.setApplicableHeaderTradeAgreement (aHTAT);
      }

      // ApplicableHeaderTradeDelivery
      aSCTT.setApplicableHeaderTradeDelivery (createApplicableHeaderTradeDelivery (aUBLDoc.hasDeliveryEntries () ? aUBLDoc.getDeliveryAtIndex (0)
                                                                                                                 : null));

      // ApplicableHeaderTradeSettlement
      aSCTT.setApplicableHeaderTradeSettlement (_createApplicableHeaderTradeSettlement (aUBLDoc));

      aCIIInvoice.setSupplyChainTradeTransaction (aSCTT);
    }

    return aCIIInvoice;
  }
}
