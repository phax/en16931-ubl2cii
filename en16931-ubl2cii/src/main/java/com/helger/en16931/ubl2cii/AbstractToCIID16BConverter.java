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

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.numeric.BigHelper;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.xml.XMLOffsetDate;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.AddressType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.AllowanceChargeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.AttachmentType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.DeliveryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.DocumentReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.LocationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.MonetaryTotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyLegalEntityType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyTaxSchemeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PaymentMeansType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PaymentTermsType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxCategoryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxSchemeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxSubtotalType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.TaxAmountType;
import un.unece.uncefact.data.standard.qualifieddatatype._100.FormattedDateTimeType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.HeaderTradeDeliveryType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.LegalOrganizationType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.ReferencedDocumentType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.SupplyChainEventType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TaxRegistrationType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradeAddressType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradeAllowanceChargeType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradePartyType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradePaymentTermsType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradeSettlementHeaderMonetarySummationType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradeTaxType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.UniversalCommunicationType;
import un.unece.uncefact.data.standard.unqualifieddatatype._100.AmountType;
import un.unece.uncefact.data.standard.unqualifieddatatype._100.BinaryObjectType;
import un.unece.uncefact.data.standard.unqualifieddatatype._100.IDType;
import un.unece.uncefact.data.standard.unqualifieddatatype._100.IndicatorType;
import un.unece.uncefact.data.standard.unqualifieddatatype._100.TextType;

/**
 * Abstract base class to convert UBL to CII D16B
 *
 * @author Philip Helger
 */
public abstract class AbstractToCIID16BConverter
{
  private static final String CII_DATE_FORMAT = "102";

  protected static <T> boolean ifNotNull (@Nullable final T aObj, @NonNull final Consumer <? super T> aConsumer)
  {
    if (aObj == null)
      return false;
    aConsumer.accept (aObj);
    return true;
  }

  protected static boolean ifNotEmpty (@Nullable final String s, @NonNull final Consumer <? super String> aConsumer)
  {
    if (StringHelper.isEmpty (s))
      return false;
    aConsumer.accept (s);
    return true;
  }

  protected static boolean isOriginatorDocumentReferenceTypeCode (@Nullable final String s)
  {
    // BT-17
    return "50".equals (s);
  }

  protected static boolean isValidDocumentReferenceTypeCode (@Nullable final String s)
  {
    // BT-17 or BT-18
    // Value 916 from BT-122 should not lead to a DocumentTypeCode
    return isOriginatorDocumentReferenceTypeCode (s) || "130".equals (s);
  }

  @Nullable
  private static String _getAsVAIfNecessary (@Nullable final String s)
  {
    if ("VAT".equals (s))
      return "VA";
    return s;
  }

  @Nullable
  protected static String createFormattedDateValue (@Nullable final LocalDate aLocalDate)
  {
    if (aLocalDate == null)
      return null;

    final SimpleDateFormat aFormatter = new SimpleDateFormat ("yyyyMMdd");
    final Date aDate = PDTFactory.createDate (aLocalDate);
    return aFormatter.format (aDate);
  }

  private static un.unece.uncefact.data.standard.unqualifieddatatype._100.DateTimeType.@Nullable DateTimeString createDateTimeString (@Nullable final LocalDate aLocalDate)
  {
    if (aLocalDate == null)
      return null;

    final un.unece.uncefact.data.standard.unqualifieddatatype._100.DateTimeType.DateTimeString aret = new un.unece.uncefact.data.standard.unqualifieddatatype._100.DateTimeType.DateTimeString ();
    aret.setFormat (CII_DATE_FORMAT);
    aret.setValue (createFormattedDateValue (aLocalDate));
    return aret;
  }

  protected static un.unece.uncefact.data.standard.unqualifieddatatype._100.@Nullable DateTimeType convertDateTime (@Nullable final LocalDate aLocalDate)
  {
    if (aLocalDate == null)
      return null;

    final un.unece.uncefact.data.standard.unqualifieddatatype._100.DateTimeType ret = new un.unece.uncefact.data.standard.unqualifieddatatype._100.DateTimeType ();
    ret.setDateTimeString (createDateTimeString (aLocalDate));
    return ret;
  }

  private static un.unece.uncefact.data.standard.unqualifieddatatype._100.DateType.@Nullable DateString createDateString (@Nullable final LocalDate aLocalDate)
  {
    if (aLocalDate == null)
      return null;

    final un.unece.uncefact.data.standard.unqualifieddatatype._100.DateType.DateString aret = new un.unece.uncefact.data.standard.unqualifieddatatype._100.DateType.DateString ();
    aret.setFormat (CII_DATE_FORMAT);
    aret.setValue (createFormattedDateValue (aLocalDate));
    return aret;
  }

  protected static un.unece.uncefact.data.standard.unqualifieddatatype._100.@Nullable DateType convertDate (@Nullable final LocalDate aLocalDate)
  {
    if (aLocalDate == null)
      return null;

    final un.unece.uncefact.data.standard.unqualifieddatatype._100.DateType ret = new un.unece.uncefact.data.standard.unqualifieddatatype._100.DateType ();
    ret.setDateString (createDateString (aLocalDate));
    return ret;
  }

  @Nullable
  protected static TextType convertText (@Nullable final String sValue)
  {
    if (sValue == null)
      return null;

    final TextType ret = new TextType ();
    ret.setValue (sValue);
    return ret;
  }

  @Nullable
  protected static IDType convertID (final com.helger.xsds.ccts.cct.schemamodule.@Nullable IdentifierType aUBLID)
  {
    if (aUBLID == null)
      return null;

    final IDType ret = new IDType ();
    ifNotNull (aUBLID.getSchemeID (), ret::setSchemeID);
    ifNotNull (aUBLID.getValue (), ret::setValue);
    return ret;
  }

  @Nullable
  protected static AmountType convertAmount (final com.helger.xsds.ccts.cct.schemamodule.@Nullable AmountType aUBLAmount,
                                             final boolean bWithCurrency)
  {
    if (aUBLAmount == null)
      return null;

    final AmountType ret = new AmountType ();
    if (bWithCurrency)
      ret.setCurrencyID (aUBLAmount.getCurrencyID ());
    ifNotNull (BigHelper.getWithoutTrailingZeroes (aUBLAmount.getValue ()), ret::setValue);
    return ret;
  }

  @Nullable
  protected static AmountType convertAmount (final com.helger.xsds.ccts.cct.schemamodule.@Nullable AmountType aUBLAmount)
  {
    return convertAmount (aUBLAmount, false);
  }

  protected static un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.@Nullable NoteType convertNote (final oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.@Nullable NoteType aUBLNote)
  {
    if (aUBLNote == null || aUBLNote.getValue () == null)
      return null;

    final un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.NoteType ret = new un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.NoteType ();
    final TextType aTT = new TextType ();
    aTT.setValue (aUBLNote.getValue ());
    ret.addContent (aTT);
    return ret;
  }

  @Nullable
  protected static TradeAddressType convertAddress (@Nullable final AddressType aUBLAddress)
  {
    if (aUBLAddress == null)
      return null;

    final TradeAddressType ret = new TradeAddressType ();
    ifNotEmpty (aUBLAddress.getStreetNameValue (), ret::setLineOne);
    ifNotEmpty (aUBLAddress.getAdditionalStreetNameValue (), ret::setLineTwo);
    if (aUBLAddress.hasAddressLineEntries ())
      ifNotEmpty (aUBLAddress.getAddressLineAtIndex (0).getLineValue (), ret::setLineThree);
    ifNotEmpty (aUBLAddress.getCityNameValue (), ret::setCityName);
    ifNotEmpty (aUBLAddress.getPostalZoneValue (), ret::setPostcodeCode);
    if (aUBLAddress.getCountrySubentity () != null)
      ret.addCountrySubDivisionName (convertText (aUBLAddress.getCountrySubentity ().getValue ()));
    if (aUBLAddress.getCountry () != null)
      ifNotEmpty (aUBLAddress.getCountry ().getIdentificationCodeValue (), ret::setCountryID);
    return ret;
  }

  @Nullable
  protected static TradePartyType convertParty (@Nullable final PartyType aUBLParty)
  {
    if (aUBLParty == null)
      return null;

    final TradePartyType aTPT = new TradePartyType ();
    for (final var aUBLPartyID : aUBLParty.getPartyIdentification ())
      ifNotNull (convertID (aUBLPartyID.getID ()), aTPT::addID);

    if (aUBLParty.hasPartyNameEntries ())
      ifNotEmpty (aUBLParty.getPartyNameAtIndex (0).getNameValue (), aTPT::setName);

    if (aUBLParty.hasPartyLegalEntityEntries ())
    {
      final PartyLegalEntityType aUBLLegalEntity = aUBLParty.getPartyLegalEntity ().get (0);

      final LegalOrganizationType aLOT = new LegalOrganizationType ();
      ifNotEmpty (aUBLLegalEntity.getRegistrationNameValue (), aLOT::setTradingBusinessName);
      ifNotNull (convertID (aUBLLegalEntity.getCompanyID ()), aLOT::setID);
      ifNotNull (convertAddress (aUBLLegalEntity.getRegistrationAddress ()), aLOT::setPostalTradeAddress);

      if (StringHelper.isEmpty (aTPT.getNameValue ()))
      {
        // Fill mandatory field
        ifNotEmpty (aUBLLegalEntity.getRegistrationNameValue (), aTPT::setName);
      }

      aTPT.setSpecifiedLegalOrganization (aLOT);
    }

    ifNotNull (convertAddress (aUBLParty.getPostalAddress ()), aTPT::setPostalTradeAddress);

    if (aUBLParty.getEndpointID () != null)
    {
      final UniversalCommunicationType aUCT = new UniversalCommunicationType ();
      ifNotNull (convertID (aUBLParty.getEndpointID ()), aUCT::setURIID);
      aTPT.addURIUniversalCommunication (aUCT);
    }

    for (final PartyTaxSchemeType aUBLPartyTaxScheme : aUBLParty.getPartyTaxScheme ())
      if (aUBLPartyTaxScheme.getCompanyIDValue () != null)
      {
        final TaxRegistrationType aTaxReg = new TaxRegistrationType ();
        final IDType aID = convertID (aUBLPartyTaxScheme.getCompanyID ());
        if (aUBLPartyTaxScheme.getTaxScheme () != null)
        {
          // MUST use "VA" scheme
          ifNotEmpty (_getAsVAIfNecessary (aUBLPartyTaxScheme.getTaxScheme ().getIDValue ()), aID::setSchemeID);
        }
        aTaxReg.setID (aID);
        aTPT.addSpecifiedTaxRegistration (aTaxReg);
      }
    return aTPT;
  }

  @NonNull
  protected static ReferencedDocumentType convertAdditionalReferencedDocument (@NonNull final DocumentReferenceType aUBLDocRef)
  {
    final ReferencedDocumentType aURDT = new ReferencedDocumentType ();

    ifNotEmpty (aUBLDocRef.getIDValue (), aURDT::setIssuerAssignedID);

    // Add DocumentTypeCode where possible
    if (isValidDocumentReferenceTypeCode (aUBLDocRef.getDocumentTypeCodeValue ()))
      aURDT.setTypeCode (aUBLDocRef.getDocumentTypeCodeValue ());
    else
      aURDT.setTypeCode ("916");

    if (aUBLDocRef.getIssueDate () != null)
    {
      final FormattedDateTimeType aFIDT = new FormattedDateTimeType ();
      aFIDT.setDateTimeString (createFormattedDateValue (aUBLDocRef.getIssueDateValueLocal ()));
      aURDT.setFormattedIssueDateTime (aFIDT);
    }

    for (final var aUBLDocDesc : aUBLDocRef.getDocumentDescription ())
    {
      final TextType aText = new TextType ();
      ifNotEmpty (aUBLDocDesc.getValue (), aText::setValue);
      ifNotEmpty (aUBLDocDesc.getLanguageID (), aText::setLanguageID);
      ifNotEmpty (aUBLDocDesc.getLanguageLocaleID (), aText::setLanguageLocaleID);
      aURDT.addName (aText);
    }

    final AttachmentType aUBLAttachment = aUBLDocRef.getAttachment ();
    if (aUBLAttachment != null)
    {
      // External Reference and Embedded Document Binary Object should be
      // mutually exclusive
      if (aUBLAttachment.getExternalReference () != null && aUBLAttachment.getExternalReference ().getURI () != null)
      {
        ifNotEmpty (aUBLAttachment.getExternalReference ().getURI ().getValue (), aURDT::setURIID);
      }

      if (aUBLAttachment.getEmbeddedDocumentBinaryObject () != null)
      {
        final BinaryObjectType aBOT = new BinaryObjectType ();
        ifNotEmpty (aUBLAttachment.getEmbeddedDocumentBinaryObject ().getMimeCode (), aBOT::setMimeCode);
        ifNotNull (aUBLAttachment.getEmbeddedDocumentBinaryObject ().getValue (), aBOT::setValue);
        ifNotEmpty (aUBLAttachment.getEmbeddedDocumentBinaryObject ().getFilename (), aBOT::setFilename);
        aURDT.addAttachmentBinaryObject (aBOT);
      }
    }
    return aURDT;
  }

  @Nullable
  protected static HeaderTradeDeliveryType createApplicableHeaderTradeDelivery (@Nullable final DeliveryType aUBLDelivery)
  {
    // Object is mandatory
    final HeaderTradeDeliveryType ret = new HeaderTradeDeliveryType ();

    if (aUBLDelivery != null)
    {
      final LocationType aUBLLocation = aUBLDelivery.getDeliveryLocation ();
      if (aUBLLocation != null)
      {
        final TradePartyType aTPTHT = new TradePartyType ();
        ifNotNull (convertID (aUBLLocation.getID ()), aTPTHT::addID);
        ifNotNull (convertAddress (aUBLLocation.getAddress ()), aTPTHT::setPostalTradeAddress);
        ret.setShipToTradeParty (aTPTHT);
      }

      if (aUBLDelivery.getActualDeliveryDate () != null)
      {
        final SupplyChainEventType aSCET = new SupplyChainEventType ();
        aSCET.setOccurrenceDateTime (convertDateTime (aUBLDelivery.getActualDeliveryDate ().getValueLocal ()));
        ret.setActualDeliverySupplyChainEvent (aSCET);
      }
    }
    return ret;
  }

  @NonNull
  protected static TradeTaxType convertApplicableTradeTax (@NonNull final TaxSubtotalType aUBLTaxSubtotal)
  {
    final TaxCategoryType aUBLTaxCategory = aUBLTaxSubtotal.getTaxCategory ();
    final TaxSchemeType aUBLTaxScheme = aUBLTaxCategory.getTaxScheme ();

    final TradeTaxType ret = new TradeTaxType ();
    if (aUBLTaxScheme != null)
      ifNotEmpty (aUBLTaxScheme.getIDValue (), ret::setTypeCode);
    ifNotEmpty (aUBLTaxCategory.getIDValue (), ret::setCategoryCode);
    ifNotNull (convertAmount (aUBLTaxSubtotal.getTaxAmount ()), ret::addCalculatedAmount);
    ifNotEmpty (aUBLTaxCategory.getIDValue (), ret::setCategoryCode);
    ifNotNull (convertAmount (aUBLTaxSubtotal.getTaxableAmount ()), ret::addBasisAmount);
    ifNotNull (aUBLTaxCategory.getPercentValue (), ret::setRateApplicablePercent);
    if (aUBLTaxCategory.hasTaxExemptionReasonEntries ())
      ifNotEmpty (aUBLTaxCategory.getTaxExemptionReasonAtIndex (0).getValue (), ret::setExemptionReason);
    ifNotEmpty (aUBLTaxCategory.getTaxExemptionReasonCodeValue (), ret::setExemptionReasonCode);
    return ret;
  }

  @NonNull
  protected static TradeAllowanceChargeType convertSpecifiedTradeAllowanceCharge (@NonNull final AllowanceChargeType aUBLAllowanceCharge)
  {
    final TradeAllowanceChargeType ret = new TradeAllowanceChargeType ();

    final IndicatorType aITDC = new IndicatorType ();
    aITDC.setIndicator (Boolean.valueOf (aUBLAllowanceCharge.getChargeIndicator ().isValue ()));
    ret.setChargeIndicator (aITDC);

    ret.addActualAmount (convertAmount (aUBLAllowanceCharge.getAmount ()));
    ifNotEmpty (aUBLAllowanceCharge.getAllowanceChargeReasonCodeValue (), ret::setReasonCode);
    if (aUBLAllowanceCharge.hasAllowanceChargeReasonEntries ())
      ret.setReason (aUBLAllowanceCharge.getAllowanceChargeReason ().get (0).getValue ());
    ifNotNull (aUBLAllowanceCharge.getMultiplierFactorNumericValue (), ret::setCalculationPercent);
    ifNotNull (aUBLAllowanceCharge.getBaseAmountValue (), ret::setBasisAmount);

    if (aUBLAllowanceCharge.hasTaxCategoryEntries ())
    {
      final TaxCategoryType aUBLTaxCategory = aUBLAllowanceCharge.getTaxCategoryAtIndex (0);
      final TaxSchemeType aUBLTaxSchene = aUBLTaxCategory.getTaxScheme ();

      final TradeTaxType aTradeTax = new TradeTaxType ();
      if (aUBLTaxSchene != null)
        ifNotEmpty (aUBLTaxSchene.getIDValue (), aTradeTax::setTypeCode);
      ifNotEmpty (aUBLTaxCategory.getIDValue (), aTradeTax::setCategoryCode);
      ifNotNull (aUBLTaxCategory.getPercentValue (), aTradeTax::setRateApplicablePercent);
      ret.addCategoryTradeTax (aTradeTax);
    }

    return ret;
  }

  @NonNull
  protected static TradePaymentTermsType convertSpecifiedTradePaymentTerms (@NonNull final PaymentTermsType aUBLPaymenTerms,
                                                                            @Nullable final PaymentMeansType aUBLPaymentMeans,
                                                                            @Nullable final XMLOffsetDate aInvoiceDueDate)
  {
    final TradePaymentTermsType ret = new TradePaymentTermsType ();
    for (final var aNote : aUBLPaymenTerms.getNote ())
      ret.addDescription (convertText (aNote.getValue ()));

    // Invoice - Payment due date BT-9
    if (aInvoiceDueDate != null)
      ret.setDueDateDateTime (convertDateTime (aInvoiceDueDate.toLocalDate ()));
    else
    {
      // Credit Note - Payment due date BT-9
      if (aUBLPaymentMeans != null && aUBLPaymentMeans.getPaymentDueDate () != null)
        ret.setDueDateDateTime (convertDateTime (aUBLPaymentMeans.getPaymentDueDate ().getValueLocal ()));
    }
    return ret;
  }

  @NonNull
  protected static TradeSettlementHeaderMonetarySummationType createSpecifiedTradeSettlementHeaderMonetarySummation (@Nullable final MonetaryTotalType aUBLMonetaryTotal,
                                                                                                                     @Nullable final ICommonsList <TaxAmountType> aUBLTaxTotalAmounts)
  {
    final TradeSettlementHeaderMonetarySummationType ret = new TradeSettlementHeaderMonetarySummationType ();
    if (aUBLMonetaryTotal != null)
    {
      ifNotNull (convertAmount (aUBLMonetaryTotal.getLineExtensionAmount ()), ret::addLineTotalAmount);
      ifNotNull (convertAmount (aUBLMonetaryTotal.getChargeTotalAmount ()), ret::addChargeTotalAmount);
      ifNotNull (convertAmount (aUBLMonetaryTotal.getAllowanceTotalAmount ()), ret::addAllowanceTotalAmount);
      ifNotNull (convertAmount (aUBLMonetaryTotal.getTaxExclusiveAmount ()), ret::addTaxBasisTotalAmount);
    }

    // BT-110 and BT-111
    for (final TaxAmountType aUBLTaxAmount : aUBLTaxTotalAmounts)
    {
      // Currency ID is required here
      ifNotNull (convertAmount (aUBLTaxAmount, true), ret::addTaxTotalAmount);
    }

    if (aUBLMonetaryTotal != null)
    {
      ifNotNull (convertAmount (aUBLMonetaryTotal.getPayableRoundingAmount ()), ret::addRoundingAmount);
      ifNotNull (convertAmount (aUBLMonetaryTotal.getTaxInclusiveAmount ()), ret::addGrandTotalAmount);
      ifNotNull (convertAmount (aUBLMonetaryTotal.getPrepaidAmount ()), ret::addTotalPrepaidAmount);
      ifNotNull (convertAmount (aUBLMonetaryTotal.getPayableAmount ()), ret::addDuePayableAmount);
    }

    return ret;
  }
}
