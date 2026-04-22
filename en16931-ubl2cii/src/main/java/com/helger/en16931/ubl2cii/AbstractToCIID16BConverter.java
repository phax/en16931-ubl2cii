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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.numeric.BigHelper;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
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
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradeContactType;
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
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern ("yyyyMMdd");

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

    return DATE_FORMATTER.format (aLocalDate);
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

  // BG-1: BT-21/BT-22 Invoice note
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

  // Converts BG-5/BG-8/BG-12/BG-15 postal address
  @Nullable
  protected static TradeAddressType convertAddress (@Nullable final AddressType aUBLAddress)
  {
    if (aUBLAddress == null)
      return null;

    final TradeAddressType ret = new TradeAddressType ();
    // BT-35/BT-50/BT-64/BT-75 Address line 1
    ifNotEmpty (aUBLAddress.getStreetNameValue (), ret::setLineOne);
    // BT-36/BT-51/BT-65/BT-76 Address line 2
    ifNotEmpty (aUBLAddress.getAdditionalStreetNameValue (), ret::setLineTwo);
    // BT-162/BT-163/BT-164/BT-165 Address line 3
    if (aUBLAddress.hasAddressLineEntries ())
      ifNotEmpty (aUBLAddress.getAddressLineAtIndex (0).getLineValue (), ret::setLineThree);
    // BT-37/BT-52/BT-66/BT-77 City
    ifNotEmpty (aUBLAddress.getCityNameValue (), ret::setCityName);
    // BT-38/BT-53/BT-67/BT-78 Post code
    ifNotEmpty (aUBLAddress.getPostalZoneValue (), ret::setPostcodeCode);
    // BT-39/BT-54/BT-68/BT-79 Country subdivision
    if (aUBLAddress.getCountrySubentity () != null)
      ret.addCountrySubDivisionName (convertText (aUBLAddress.getCountrySubentity ().getValue ()));
    // BT-40/BT-55/BT-69/BT-80 Country code
    if (aUBLAddress.getCountry () != null)
      ifNotEmpty (aUBLAddress.getCountry ().getIdentificationCodeValue (), ret::setCountryID);
    return ret;
  }

  // BG-4/BG-7/BG-10/BG-11 Party conversion
  @Nullable
  protected static TradePartyType convertParty (@Nullable final PartyType aUBLParty)
  {
    if (aUBLParty == null)
      return null;

    final TradePartyType aTPT = new TradePartyType ();
    // BT-29/BT-29-1/BT-46/BT-46-1/BT-60/BT-60-1 Party identifier
    for (final var aUBLPartyID : aUBLParty.getPartyIdentification ())
      ifNotNull (convertID (aUBLPartyID.getID ()), aTPT::addID);

    if (aUBLParty.hasPartyLegalEntityEntries ())
    {
      final PartyLegalEntityType aUBLLegalEntity = aUBLParty.getPartyLegalEntity ().get (0);

      // BT-27/BT-44/BT-59/BT-62 Party name
      // UBL RegistrationName is the legal name → CII TradeParty/Name
      ifNotEmpty (aUBLLegalEntity.getRegistrationNameValue (), aTPT::setName);

      final LegalOrganizationType aLOT = new LegalOrganizationType ();
      boolean bUseLOT = false;
      // BT-28/BT-45 Trading name
      // UBL PartyName/Name is the trading name → CII TradingBusinessName
      if (aUBLParty.hasPartyNameEntries ())
        if (ifNotEmpty (aUBLParty.getPartyNameAtIndex (0).getNameValue (), aLOT::setTradingBusinessName))
          bUseLOT = true;
      // BT-30/BT-30-1/BT-47/BT-47-1/BT-61/BT-61-1 Legal registration identifier
      if (ifNotNull (convertID (aUBLLegalEntity.getCompanyID ()), aLOT::setID))
        bUseLOT = true;
      if (ifNotNull (convertAddress (aUBLLegalEntity.getRegistrationAddress ()), aLOT::setPostalTradeAddress))
        bUseLOT = true;

      if (bUseLOT)
        aTPT.setSpecifiedLegalOrganization (aLOT);

      // BT-33 Seller additional legal information
      ifNotEmpty (aUBLLegalEntity.getCompanyLegalFormValue (), x -> aTPT.addDescription (convertText (x)));
    }
    else
    {
      // No PartyLegalEntity — fall back to PartyName for the party name
      if (aUBLParty.hasPartyNameEntries ())
        ifNotEmpty (aUBLParty.getPartyNameAtIndex (0).getNameValue (), aTPT::setName);
    }

    // BG-5/BG-8 Postal address
    ifNotNull (convertAddress (aUBLParty.getPostalAddress ()), aTPT::setPostalTradeAddress);

    // BT-34/BT-34-1/BT-49/BT-49-1 Electronic address
    if (aUBLParty.getEndpointID () != null)
    {
      final UniversalCommunicationType aUCT = new UniversalCommunicationType ();
      ifNotNull (convertID (aUBLParty.getEndpointID ()), aUCT::setURIID);
      aTPT.addURIUniversalCommunication (aUCT);
    }

    // BT-31/BT-32/BT-48/BT-63 Tax registration
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

    // BG-6/BG-9 Contact (BT-41/BT-42/BT-43 and BT-56/BT-57/BT-58)
    if (aUBLParty.getContact () != null)
    {
      final var aUBLContact = aUBLParty.getContact ();
      final TradeContactType aTCT = new TradeContactType ();
      boolean bUseContact = false;

      // BT-41/BT-56 Contact point
      if (ifNotEmpty (aUBLContact.getNameValue (), aTCT::setPersonName))
        bUseContact = true;

      // BT-42/BT-57 Contact telephone number
      if (StringHelper.isNotEmpty (aUBLContact.getTelephoneValue ()))
      {
        final UniversalCommunicationType aPhone = new UniversalCommunicationType ();
        aPhone.setCompleteNumber (aUBLContact.getTelephoneValue ());
        aTCT.setTelephoneUniversalCommunication (aPhone);
        bUseContact = true;
      }

      // BT-43/BT-58 Contact email address
      if (StringHelper.isNotEmpty (aUBLContact.getElectronicMailValue ()))
      {
        final UniversalCommunicationType aEmail = new UniversalCommunicationType ();
        final IDType aEmailID = new IDType ();
        aEmailID.setValue (aUBLContact.getElectronicMailValue ());
        aEmail.setURIID (aEmailID);
        aTCT.setEmailURIUniversalCommunication (aEmail);
        bUseContact = true;
      }

      if (bUseContact)
        aTPT.addDefinedTradeContact (aTCT);
    }

    return aTPT;
  }

  // BG-24/BT-17/BT-18: Additional/Originator/Object document reference
  @NonNull
  protected static ReferencedDocumentType convertAdditionalReferencedDocument (@NonNull final DocumentReferenceType aUBLDocRef)
  {
    final ReferencedDocumentType aURDT = new ReferencedDocumentType ();

    // BT-122 Supporting document reference
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

    // BT-123 Supporting document description
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
      // BT-124 External document location
      if (aUBLAttachment.getExternalReference () != null && aUBLAttachment.getExternalReference ().getURI () != null)
      {
        ifNotEmpty (aUBLAttachment.getExternalReference ().getURI ().getValue (), aURDT::setURIID);
      }

      // BT-125 Attached document
      if (aUBLAttachment.getEmbeddedDocumentBinaryObject () != null)
      {
        final BinaryObjectType aBOT = new BinaryObjectType ();
        // BT-125-1 Attached document Mime code
        ifNotEmpty (aUBLAttachment.getEmbeddedDocumentBinaryObject ().getMimeCode (), aBOT::setMimeCode);
        ifNotNull (aUBLAttachment.getEmbeddedDocumentBinaryObject ().getValue (), aBOT::setValue);
        // BT-125-2 Attached document Filename
        ifNotEmpty (aUBLAttachment.getEmbeddedDocumentBinaryObject ().getFilename (), aBOT::setFilename);
        aURDT.addAttachmentBinaryObject (aBOT);
      }
    }
    return aURDT;
  }

  // BG-13 DELIVERY INFORMATION
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
        // BT-71/BT-71-1 Deliver to location identifier
        ifNotNull (convertID (aUBLLocation.getID ()), aTPTHT::addID);
        // BG-15 DELIVER TO ADDRESS
        ifNotNull (convertAddress (aUBLLocation.getAddress ()), aTPTHT::setPostalTradeAddress);
        ret.setShipToTradeParty (aTPTHT);
      }

      // BT-72 Actual delivery date
      if (aUBLDelivery.getActualDeliveryDate () != null)
      {
        final SupplyChainEventType aSCET = new SupplyChainEventType ();
        aSCET.setOccurrenceDateTime (convertDateTime (aUBLDelivery.getActualDeliveryDate ().getValueLocal ()));
        ret.setActualDeliverySupplyChainEvent (aSCET);
      }
    }
    return ret;
  }

  // BG-23 VAT BREAKDOWN
  @NonNull
  protected static TradeTaxType convertApplicableTradeTax (@NonNull final TaxSubtotalType aUBLTaxSubtotal)
  {
    final TaxCategoryType aUBLTaxCategory = aUBLTaxSubtotal.getTaxCategory ();
    final TaxSchemeType aUBLTaxScheme = aUBLTaxCategory.getTaxScheme ();

    final TradeTaxType ret = new TradeTaxType ();
    // BT-118 VAT category code scheme
    if (aUBLTaxScheme != null)
      ifNotEmpty (aUBLTaxScheme.getIDValue (), ret::setTypeCode);
    // BT-118 VAT category code
    ifNotEmpty (aUBLTaxCategory.getIDValue (), ret::setCategoryCode);
    // BT-117 VAT category tax amount
    ifNotNull (convertAmount (aUBLTaxSubtotal.getTaxAmount ()), ret::addCalculatedAmount);
    ifNotEmpty (aUBLTaxCategory.getIDValue (), ret::setCategoryCode);
    // BT-116 VAT category taxable amount
    ifNotNull (convertAmount (aUBLTaxSubtotal.getTaxableAmount ()), ret::addBasisAmount);
    // BT-119 VAT category rate
    ifNotNull (aUBLTaxCategory.getPercentValue (), ret::setRateApplicablePercent);
    // BT-120 VAT exemption reason text
    if (aUBLTaxCategory.hasTaxExemptionReasonEntries ())
      ifNotEmpty (aUBLTaxCategory.getTaxExemptionReasonAtIndex (0).getValue (), ret::setExemptionReason);
    // BT-121 VAT exemption reason code
    ifNotEmpty (aUBLTaxCategory.getTaxExemptionReasonCodeValue (), ret::setExemptionReasonCode);
    return ret;
  }

  // BG-20/BG-21/BG-27/BG-28 Allowance/Charge details
  @NonNull
  protected static TradeAllowanceChargeType convertSpecifiedTradeAllowanceCharge (@NonNull final AllowanceChargeType aUBLAllowanceCharge)
  {
    final TradeAllowanceChargeType ret = new TradeAllowanceChargeType ();

    final IndicatorType aITDC = new IndicatorType ();
    aITDC.setIndicator (Boolean.valueOf (aUBLAllowanceCharge.getChargeIndicator ().isValue ()));
    ret.setChargeIndicator (aITDC);

    // BT-92/BT-99/BT-136/BT-141 Amount
    ret.addActualAmount (convertAmount (aUBLAllowanceCharge.getAmount ()));
    // BT-98/BT-105/BT-140/BT-145 Reason code
    ifNotEmpty (aUBLAllowanceCharge.getAllowanceChargeReasonCodeValue (), ret::setReasonCode);
    // BT-97/BT-104/BT-139/BT-144 Reason
    if (aUBLAllowanceCharge.hasAllowanceChargeReasonEntries ())
      ret.setReason (aUBLAllowanceCharge.getAllowanceChargeReason ().get (0).getValue ());
    // BT-94/BT-101/BT-138/BT-143 Percentage
    ifNotNull (aUBLAllowanceCharge.getMultiplierFactorNumericValue (), ret::setCalculationPercent);
    // BT-93/BT-100/BT-137/BT-142 Base amount
    ifNotNull (aUBLAllowanceCharge.getBaseAmountValue (), ret::setBasisAmount);

    // BT-95/BT-102 VAT category code and BT-96/BT-103 VAT rate
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

  // BT-20 Payment terms + BT-9 Payment due date
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

  // BG-22 DOCUMENT TOTALS
  @NonNull
  protected static TradeSettlementHeaderMonetarySummationType createSpecifiedTradeSettlementHeaderMonetarySummation (@Nullable final MonetaryTotalType aUBLMonetaryTotal,
                                                                                                                     @Nullable final ICommonsList <TaxAmountType> aUBLTaxTotalAmounts)
  {
    final TradeSettlementHeaderMonetarySummationType ret = new TradeSettlementHeaderMonetarySummationType ();
    if (aUBLMonetaryTotal != null)
    {
      // BT-106 Sum of Invoice line net amount
      ifNotNull (convertAmount (aUBLMonetaryTotal.getLineExtensionAmount ()), ret::addLineTotalAmount);
      // BT-108 Sum of charges on document level
      ifNotNull (convertAmount (aUBLMonetaryTotal.getChargeTotalAmount ()), ret::addChargeTotalAmount);
      // BT-107 Sum of allowances on document level
      ifNotNull (convertAmount (aUBLMonetaryTotal.getAllowanceTotalAmount ()), ret::addAllowanceTotalAmount);
      // BT-109 Invoice total amount without VAT
      ifNotNull (convertAmount (aUBLMonetaryTotal.getTaxExclusiveAmount ()), ret::addTaxBasisTotalAmount);
    }

    // BT-110/BT-111 Invoice total VAT amount (in document/accounting currency)
    for (final TaxAmountType aUBLTaxAmount : aUBLTaxTotalAmounts)
    {
      // Currency ID is required here
      ifNotNull (convertAmount (aUBLTaxAmount, true), ret::addTaxTotalAmount);
    }

    if (aUBLMonetaryTotal != null)
    {
      // BT-114 Rounding amount
      ifNotNull (convertAmount (aUBLMonetaryTotal.getPayableRoundingAmount ()), ret::addRoundingAmount);
      // BT-112 Invoice total amount with VAT
      ifNotNull (convertAmount (aUBLMonetaryTotal.getTaxInclusiveAmount ()), ret::addGrandTotalAmount);
      // BT-113 Paid amount
      ifNotNull (convertAmount (aUBLMonetaryTotal.getPrepaidAmount ()), ret::addTotalPrepaidAmount);
      // BT-115 Amount due for payment
      ifNotNull (convertAmount (aUBLMonetaryTotal.getPayableAmount ()), ret::addDuePayableAmount);
    }

    return ret;
  }
}
