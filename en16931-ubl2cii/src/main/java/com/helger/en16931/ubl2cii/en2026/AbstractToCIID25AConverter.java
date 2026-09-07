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

import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.numeric.BigHelper;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.xml.XMLOffsetDate;
import com.helger.datetime.xml.XMLOffsetTime;
import com.helger.en16931.basics.codelist.EN16931CodeLists;
import com.helger.en16931.ubl2cii.AbstractToCIIConverterBase;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.AddressType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.AllowanceChargeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.AnnotationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.AttachmentType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.DeliveryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.DocumentReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.LocationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.MonetaryTotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PartyLegalEntityType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PartyTaxSchemeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PaymentMeansType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PaymentTermsType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.TaxCategoryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.TaxSchemeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.TaxSubtotalType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_25.TaxAmountType;
import un.unece.uncefact.data.standard.cii.d25a.qdt.AllowanceChargeReasonCodeType;
import un.unece.uncefact.data.standard.cii.d25a.qdt.FormattedDateTimeType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.FinancialAdjustmentType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.HeaderTradeDeliveryType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.LegalOrganizationType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.ReferencedDocumentType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.SupplyChainEventType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TaxRegistrationType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeAddressType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeContactType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeAllowanceChargeType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradePartyType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradePaymentDiscountTermsType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradePaymentPenaltyTermsType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradePaymentTermsType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeSettlementHeaderMonetarySummationType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeTaxType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.UniversalCommunicationType;
import un.unece.uncefact.data.standard.cii.d25a.udt.AmountType;
import un.unece.uncefact.data.standard.cii.d25a.udt.BinaryObjectType;
import un.unece.uncefact.data.standard.cii.d25a.udt.CodeType;
import un.unece.uncefact.data.standard.cii.d25a.udt.IDType;
import un.unece.uncefact.data.standard.cii.d25a.udt.IndicatorType;
import un.unece.uncefact.data.standard.cii.d25a.udt.TextType;

/**
 * Abstract base class to convert UBL 2.5 to CII D25A, following EN 16931:2026.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public abstract class AbstractToCIID25AConverter extends AbstractToCIIConverterBase
{
  /**
   * BT-177-1/BT-193-1 Non-VAT tax code list identifier. In the EN core only UNTDID 5153 is
   * permitted, and its presence is what distinguishes BT-177/BT-193 from BT-105/BT-145.
   */
  public static final String NON_VAT_TAX_CODE_LIST_ID = "5153";

  /** BT-32-2 National tax code - a fixed value in the UBL binding since EN 16931:2026 */
  public static final String NATIONAL_TAX_SCHEME = "LOC";
  /** BT-32-1 National tax registration scheme identifier of the CII binding, UNTDID 1153 */
  public static final String NATIONAL_TAX_SCHEME_CII = "FC";

  /**
   * Read the value of the first entry of a UBL element that widened from 0..1 to 0..n between UBL
   * 2.1 and UBL 2.5. Every business term affected by that widening stayed 0..1 in the semantic
   * model, so only the first entry can carry one.
   *
   * @param aUBLList
   *        The list to read. May not be <code>null</code>.
   * @return <code>null</code> if the list is empty.
   */
  @Nullable
  protected static String getFirstValue (@NonNull final List <? extends com.helger.xsds.ccts.cct.schemamodule.TextType> aUBLList)
  {
    return aUBLList.isEmpty () ? null : aUBLList.get (0).getValue ();
  }

  @Nullable
  protected static FormattedDateTimeType convertFormattedDateTime (@Nullable final LocalDate aLocalDate)
  {
    if (aLocalDate == null)
      return null;

    // BT-26-1/BT-218-1 and the other qdt:DateTimeString format codes - always "102"
    final FormattedDateTimeType ret = new FormattedDateTimeType ();
    final FormattedDateTimeType.DateTimeString aDTS = new FormattedDateTimeType.DateTimeString ();
    aDTS.setFormat (CII_DATE_FORMAT.getID ());
    aDTS.setValue (createFormattedDateValue (aLocalDate));
    ret.setDateTimeString (aDTS);
    return ret;
  }

  private static un.unece.uncefact.data.standard.cii.d25a.udt.DateTimeType.@Nullable DateTimeString createDateTimeString (@Nullable final LocalDate aLocalDate)
  {
    if (aLocalDate == null)
      return null;

    // BT-9-1/BT-72-1/BT-73-1/BT-74-1/BT-134-1/BT-135-1/BT-170-1/BT-181-1/BT-187-1 - the UNTDID
    // 2379 format code of a udt:DateTimeString is "102" everywhere except for BT-166
    final un.unece.uncefact.data.standard.cii.d25a.udt.DateTimeType.DateTimeString aret = new un.unece.uncefact.data.standard.cii.d25a.udt.DateTimeType.DateTimeString ();
    aret.setFormat (CII_DATE_FORMAT.getID ());
    aret.setValue (createFormattedDateValue (aLocalDate));
    return aret;
  }

  protected static un.unece.uncefact.data.standard.cii.d25a.udt.@Nullable DateTimeType convertDateTime (@Nullable final LocalDate aLocalDate)
  {
    if (aLocalDate == null)
      return null;

    final un.unece.uncefact.data.standard.cii.d25a.udt.DateTimeType ret = new un.unece.uncefact.data.standard.cii.d25a.udt.DateTimeType ();
    ret.setDateTimeString (createDateTimeString (aLocalDate));
    return ret;
  }

  // BT-2 Invoice issue date + BT-166 Invoice issue time.
  // CII represents the two with a single element, and the UNTDID 2379 format code says which of
  // them is present: "102" for the date alone (BT-2-1), "208" for date and time including the UTC
  // offset (BT-166-1).
  protected static un.unece.uncefact.data.standard.cii.d25a.udt.@Nullable DateTimeType convertDateTime (@Nullable final LocalDate aLocalDate,
                                                                                                        @Nullable final XMLOffsetTime aTime)
  {
    if (aLocalDate == null)
      return null;

    // BT-166 absent - BT-2-1 format "102", date only
    if (aTime == null)
      return convertDateTime (aLocalDate);

    // BT-166 present - BT-166-1 format "208", date and time
    final un.unece.uncefact.data.standard.cii.d25a.udt.DateTimeType ret = new un.unece.uncefact.data.standard.cii.d25a.udt.DateTimeType ();
    final un.unece.uncefact.data.standard.cii.d25a.udt.DateTimeType.DateTimeString aDTS = new un.unece.uncefact.data.standard.cii.d25a.udt.DateTimeType.DateTimeString ();
    aDTS.setFormat (CII_DATE_TIME_FORMAT.getID ());
    aDTS.setValue (createFormattedDateTimeValue (aTime.atDate (aLocalDate)));
    ret.setDateTimeString (aDTS);
    return ret;
  }

  private static un.unece.uncefact.data.standard.cii.d25a.udt.DateType.@Nullable DateString createDateString (@Nullable final LocalDate aLocalDate)
  {
    if (aLocalDate == null)
      return null;

    // BT-7-1 - the UNTDID 2379 format code of a udt:DateString is always "102"
    final un.unece.uncefact.data.standard.cii.d25a.udt.DateType.DateString aret = new un.unece.uncefact.data.standard.cii.d25a.udt.DateType.DateString ();
    aret.setFormat (CII_DATE_FORMAT.getID ());
    aret.setValue (createFormattedDateValue (aLocalDate));
    return aret;
  }

  protected static un.unece.uncefact.data.standard.cii.d25a.udt.@Nullable DateType convertDate (@Nullable final LocalDate aLocalDate)
  {
    if (aLocalDate == null)
      return null;

    final un.unece.uncefact.data.standard.cii.d25a.udt.DateType ret = new un.unece.uncefact.data.standard.cii.d25a.udt.DateType ();
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

  // BG-1: BT-21 Invoice note subject code + BT-22 Invoice note
  // Since UBL 2.5 the subject code has an element of its own, so the "#code#" prefix hack of the
  // EN 16931:2017 binding is gone.
  protected static un.unece.uncefact.data.standard.cii.d25a.rabie.@Nullable NoteType convertAnnotation (@Nullable final AnnotationType aUBLAnnotation)
  {
    if (aUBLAnnotation == null)
      return null;

    final un.unece.uncefact.data.standard.cii.d25a.rabie.NoteType ret = new un.unece.uncefact.data.standard.cii.d25a.rabie.NoteType ();

    // BT-21 Invoice note subject code
    ifNotEmpty (aUBLAnnotation.getSubjectCodeValue (), x -> {
      final CodeType aSubjectCode = new CodeType ();
      aSubjectCode.setValue (x);
      ret.addSubjectCode (aSubjectCode);
    });

    // BT-22 Invoice note
    ifNotEmpty (getFirstValue (aUBLAnnotation.getAnnotationContent ()), x -> ret.addContent (convertText (x)));

    return ret.hasNoSubjectCodeEntries () && ret.hasNoContentEntries () ? null : ret;
  }

  // BT-127 Invoice line note. Unlike BG-1 this stayed a plain cbc:Note in 2026, because it has no
  // subject code counterpart.
  protected static un.unece.uncefact.data.standard.cii.d25a.rabie.@Nullable NoteType convertNote (final oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_25.@Nullable NoteType aUBLNote)
  {
    if (aUBLNote == null || aUBLNote.getValue () == null)
      return null;

    final un.unece.uncefact.data.standard.cii.d25a.rabie.NoteType ret = new un.unece.uncefact.data.standard.cii.d25a.rabie.NoteType ();
    ret.addContent (convertText (aUBLNote.getValue ()));
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
    ifNotEmpty (getFirstValue (aUBLAddress.getStreetName ()), ret::setLineOne);
    // BT-36/BT-51/BT-65/BT-76 Address line 2
    ifNotEmpty (getFirstValue (aUBLAddress.getAdditionalStreetName ()), ret::setLineTwo);
    // BT-162/BT-163/BT-164/BT-165 Address line 3
    if (aUBLAddress.hasAddressLineEntries ())
      ifNotEmpty (getFirstValue (aUBLAddress.getAddressLineAtIndex (0).getLine ()), ret::setLineThree);
    // BT-37/BT-52/BT-66/BT-77 City
    ifNotEmpty (getFirstValue (aUBLAddress.getCityName ()), ret::setCityName);
    // BT-38/BT-53/BT-67/BT-78 Post code
    ifNotEmpty (getFirstValue (aUBLAddress.getPostalZone ()), ret::setPostcodeCode);
    // BT-39/BT-54/BT-68/BT-79 Country subdivision
    ifNotEmpty (getFirstValue (aUBLAddress.getCountrySubentity ()),
                x -> ret.addCountrySubDivisionName (convertText (x)));
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
    // If the UBL ID has a schemeID, use CII GlobalID; otherwise use CII ID
    for (final var aUBLPartyID : aUBLParty.getPartyIdentification ())
    {
      final IDType aCIIID = convertID (aUBLPartyID.getID ());
      if (aCIIID != null)
      {
        if (StringHelper.isNotEmpty (aCIIID.getSchemeID ()))
          aTPT.addGlobalID (aCIIID);
        else
          aTPT.addID (aCIIID);
      }
    }

    if (aUBLParty.hasPartyLegalEntityEntries ())
    {
      final PartyLegalEntityType aUBLLegalEntity = aUBLParty.getPartyLegalEntity ().get (0);

      // BT-27/BT-44/BT-59/BT-62 Party name
      // UBL RegistrationName is the legal name → CII TradeParty/Name
      final boolean bHasRegistrationName = ifNotEmpty (aUBLLegalEntity.getRegistrationNameValue (), aTPT::setName);

      final LegalOrganizationType aLOT = new LegalOrganizationType ();
      boolean bUseLOT = false;
      if (aUBLParty.hasPartyNameEntries ())
      {
        if (bHasRegistrationName)
        {
          // BT-28/BT-45 Trading name
          // UBL PartyName/Name is the trading name → CII TradingBusinessName
          if (ifNotEmpty (aUBLParty.getPartyNameAtIndex (0).getNameValue (), aLOT::setTradingBusinessName))
            bUseLOT = true;
        }
        else
        {
          // BT-59/BT-62 Payee/TaxRep name (no RegistrationName present)
          // UBL PartyName/Name is the party name → CII TradeParty/Name
          ifNotEmpty (aUBLParty.getPartyNameAtIndex (0).getNameValue (), aTPT::setName);
        }
      }
      // BT-30/BT-30-1/BT-47/BT-47-1/BT-61/BT-61-1 Legal registration identifier
      if (ifNotNull (convertID (aUBLLegalEntity.getCompanyID ()), aLOT::setID))
        bUseLOT = true;
      if (ifNotNull (convertAddress (aUBLLegalEntity.getRegistrationAddress ()), aLOT::setPostalTradeAddress))
        bUseLOT = true;

      if (bUseLOT)
        aTPT.setSpecifiedLegalOrganization (aLOT);

      // BT-33 Seller additional legal information
      ifNotEmpty (getFirstValue (aUBLLegalEntity.getCompanyLegalForm ()),
                  x -> aTPT.addDescription (convertText (x)));
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
          // BT-31-1/BT-48-1/BT-63-1: UBL BT-31-2/BT-48-2/BT-63-2 "VAT" becomes CII "VA", and
          // BT-32-1: since EN 16931:2026 the national tax code BT-32-2 is the fixed value "LOC",
          // which becomes CII "FC". The 2017 binding had no fixed value here and accepted anything
          // except "VAT" for BT-32.
          final String sUBLTaxScheme = aUBLPartyTaxScheme.getTaxScheme ().getIDValue ();
          ifNotEmpty (NATIONAL_TAX_SCHEME.equals (sUBLTaxScheme) ? NATIONAL_TAX_SCHEME_CII
                                                                 : EN16931CodeLists.mapTaxSchemeCodeUBLToCII (sUBLTaxScheme),
                      aID::setSchemeID);
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

    // BT-18-1/BT-128-1 Scheme identifier → ReferenceTypeCode
    if (aUBLDocRef.getID () != null)
      ifNotEmpty (aUBLDocRef.getID ().getSchemeID (), aURDT::setReferenceTypeCode);

    // Add DocumentTypeCode where possible
    if (EN16931CodeLists.isValidDocumentReferenceTypeCode (aUBLDocRef.getDocumentTypeCodeValue ()))
      aURDT.setTypeCode (aUBLDocRef.getDocumentTypeCodeValue ());
    else
      aURDT.setTypeCode (EN16931CodeLists.DOCUMENT_TYPE_CODE_SUPPORTING_DOCUMENT);

    // BT-26 Preceding Invoice issue date / document issue date
    if (aUBLDocRef.getIssueDate () != null)
      aURDT.setFormattedIssueDateTime (convertFormattedDateTime (aUBLDocRef.getIssueDateValueLocal ()));

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
      final TradePartyType aTPTHT = new TradePartyType ();
      boolean bUseShipToParty = false;

      if (aUBLLocation != null)
      {
        // BT-71/BT-71-1 Deliver to location identifier
        final IDType aLocID = convertID (aUBLLocation.getID ());
        if (aLocID != null)
        {
          if (StringHelper.isNotEmpty (aLocID.getSchemeID ()))
            aTPTHT.addGlobalID (aLocID);
          else
            aTPTHT.addID (aLocID);
          bUseShipToParty = true;
        }
        // BG-15 DELIVER TO ADDRESS
        if (ifNotNull (convertAddress (aUBLLocation.getAddress ()), aTPTHT::setPostalTradeAddress))
          bUseShipToParty = true;
      }

      // BT-70 Deliver to party name
      if (aUBLDelivery.getDeliveryParty () != null &&
          aUBLDelivery.getDeliveryParty ().hasPartyNameEntries ())
      {
        if (ifNotEmpty (aUBLDelivery.getDeliveryParty ().getPartyNameAtIndex (0).getNameValue (), aTPTHT::setName))
          bUseShipToParty = true;
      }

      if (bUseShipToParty)
        ret.setShipToTradeParty (aTPTHT);

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
  protected static TradeTaxType convertApplicableTradeTax (@NonNull final TaxSubtotalType aUBLTaxSubtotal,
                                                           @Nullable final String sInvoiceCurrencyCode)
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
    // BT-184 VAT breakdown currency.
    // UBL requires @currencyID on every amount, CII has ram:CurrencyCode as 0..1 to express that a
    // breakdown line is in the VAT accounting currency BT-6 rather than the invoice currency BT-5.
    // It is therefore written only when it actually differs from BT-5 - writing it always would
    // add an element to every breakdown that carries no information.
    if (aUBLTaxSubtotal.getTaxAmount () != null)
    {
      final String sCurrencyID = aUBLTaxSubtotal.getTaxAmount ().getCurrencyID ();
      if (StringHelper.isNotEmpty (sCurrencyID) && !sCurrencyID.equals (sInvoiceCurrencyCode))
        ret.setCurrencyCode (sCurrencyID);
    }
    // BT-210 VAT breakdown goods/services code
    ifNotEmpty (aUBLTaxCategory.getSupplyTypeCodeValue (), ret::setSupplyTypeCode);
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
    // BT-98/BT-105/BT-140/BT-145 Reason code, and BT-177/BT-193 non-VAT tax code.
    // The two share cbc:AllowanceChargeReasonCode and are told apart by @listID = "5153"
    // (BT-177-1 respectively BT-193-1). The list identifier may only be propagated for that fixed
    // value: the CII schema declares a default on @listID, so a blanket copy would put a bogus
    // list identifier on every BT-98/BT-105/BT-140/BT-145 and destroy the discriminator.
    if (aUBLAllowanceCharge.getAllowanceChargeReasonCode () != null)
      ifNotEmpty (aUBLAllowanceCharge.getAllowanceChargeReasonCodeValue (), x -> {
        final AllowanceChargeReasonCodeType aReasonCode = new AllowanceChargeReasonCodeType ();
        aReasonCode.setValue (x);
        final var aUBLReasonCode = aUBLAllowanceCharge.getAllowanceChargeReasonCode ();
        if (NON_VAT_TAX_CODE_LIST_ID.equals (aUBLReasonCode.getListID ()))
        {
          // BT-177-1/BT-193-1 Non-VAT tax code list identifier
          aReasonCode.setListID (aUBLReasonCode.getListID ());
          ifNotEmpty (aUBLReasonCode.getListAgencyID (), aReasonCode::setListAgencyID);
        }
        ret.setReasonCode (aReasonCode);
      });
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
      // BT-173/BT-175/BT-194 Exemption reason text
      if (aUBLTaxCategory.hasTaxExemptionReasonEntries ())
        ifNotEmpty (aUBLTaxCategory.getTaxExemptionReasonAtIndex (0).getValue (), aTradeTax::setExemptionReason);
      // BT-174/BT-176/BT-195 VAT exemption reason and specification code
      ifNotEmpty (aUBLTaxCategory.getTaxExemptionReasonCodeValue (), aTradeTax::setExemptionReasonCode);
      // BT-213/BT-214/BT-196 Goods/services code
      ifNotEmpty (aUBLTaxCategory.getSupplyTypeCodeValue (), aTradeTax::setSupplyTypeCode);
      ret.addCategoryTradeTax (aTradeTax);
    }

    return ret;
  }

  // BG-35 EARLY PAYMENT DISCOUNT (BT-170, BT-170-1, BT-171, BT-172).
  // UBL puts all of BG-33, BG-35 and BG-36 into cac:PaymentTerms and gives no explicit
  // discriminator, so the group is recognised by the elements it uses.
  @Nullable
  private static TradePaymentDiscountTermsType _convertPaymentDiscountTerms (@NonNull final PaymentTermsType aUBLPaymentTerms)
  {
    final TradePaymentDiscountTermsType ret = new TradePaymentDiscountTermsType ();
    boolean bUse = false;

    // BT-170 Discount end date and BT-170-1 its format code
    if (aUBLPaymentTerms.getSettlementPeriod () != null)
      if (ifNotNull (aUBLPaymentTerms.getSettlementPeriod ().getEndDate (),
                     x -> ret.setBasisDateTime (convertDateTime (x.getValueLocal ()))))
        bUse = true;

    // BT-171 Discount percentage
    if (ifNotNull (aUBLPaymentTerms.getSettlementDiscountPercentValue (), ret::setCalculationPercent))
      bUse = true;

    // BT-172 Discount amount
    if (ifNotNull (convertAmount (aUBLPaymentTerms.getSettlementDiscountAmount ()), ret::setActualDiscountAmount))
      bUse = true;

    return bUse ? ret : null;
  }

  // BG-36 LATE PAYMENT PENALTY (BT-181, BT-181-1, BT-182, BT-183)
  @Nullable
  private static TradePaymentPenaltyTermsType _convertPaymentPenaltyTerms (@NonNull final PaymentTermsType aUBLPaymentTerms)
  {
    final TradePaymentPenaltyTermsType ret = new TradePaymentPenaltyTermsType ();
    boolean bUse = false;

    // BT-181 Penalty start date and BT-181-1 its format code
    if (aUBLPaymentTerms.getPenaltyPeriod () != null)
      if (ifNotNull (aUBLPaymentTerms.getPenaltyPeriod ().getStartDate (),
                     x -> ret.setBasisDateTime (convertDateTime (x.getValueLocal ()))))
        bUse = true;

    // BT-182 Penalty yearly interest percentage
    if (aUBLPaymentTerms.getPenaltyInterestRate () != null)
      if (ifNotNull (aUBLPaymentTerms.getPenaltyInterestRate ().getInterestRatePercentValue (),
                     ret::setCalculationPercent))
        bUse = true;

    // BT-183 Penalty amount
    if (ifNotNull (convertAmount (aUBLPaymentTerms.getPenaltyAmount ()), ret::setActualPenaltyAmount))
      bUse = true;

    return bUse ? ret : null;
  }

  // BG-33 PAYMENT TERMS: BT-20 Payment term text, plus BT-9 Payment due date, BG-35 and BG-36.
  // Since 2026 cac:PaymentTerms is 0..n, so BT-9 - which is 0..1 - may only be written to one of
  // them; bWithDueDate says which.
  @NonNull
  protected static TradePaymentTermsType convertSpecifiedTradePaymentTerms (@NonNull final PaymentTermsType aUBLPaymenTerms,
                                                                            @Nullable final PaymentMeansType aUBLPaymentMeans,
                                                                            @Nullable final XMLOffsetDate aInvoiceDueDate,
                                                                            final boolean bWithDueDate)
  {
    final TradePaymentTermsType ret = new TradePaymentTermsType ();
    // BT-20 Payment term text
    for (final var aNote : aUBLPaymenTerms.getNote ())
      ret.addDescription (convertText (aNote.getValue ()));

    if (bWithDueDate)
    {
      // BT-9 Payment due date and BT-9-1 its format code
      if (aInvoiceDueDate != null)
        ret.setDueDateDateTime (convertDateTime (aInvoiceDueDate.toLocalDate ()));
      else
        if (aUBLPaymentMeans != null && aUBLPaymentMeans.getPaymentDueDate () != null)
          ret.setDueDateDateTime (convertDateTime (aUBLPaymentMeans.getPaymentDueDate ().getValueLocal ()));
    }

    // BG-35 EARLY PAYMENT DISCOUNT
    ifNotNull (_convertPaymentDiscountTerms (aUBLPaymenTerms), ret::setApplicableTradePaymentDiscountTerms);

    // BG-36 LATE PAYMENT PENALTY
    ifNotNull (_convertPaymentPenaltyTerms (aUBLPaymenTerms), ret::setApplicableTradePaymentPenaltyTerms);

    return ret;
  }

  // BG-34 CHARGES ON BEHALF OF A THIRD PARTY.
  // UBL carries the group as a line (cac:CollectionInvoiceLine respectively
  // cac:CollectionCreditNoteLine), CII as ram:SpecifiedFinancialAdjustment. BT-179-1 (Line
  // identifier) is UBL only bookkeeping and has no CII counterpart, so it is dropped here.
  // The two collection line types of UBL are unrelated Java classes, so the already extracted
  // values are handed over instead of the line itself.
  @Nullable
  protected static FinancialAdjustmentType convertSpecifiedFinancialAdjustment (final com.helger.xsds.ccts.cct.schemamodule.@Nullable AmountType aUBLAmount,
                                                                                @Nullable final String sDescription)
  {
    final FinancialAdjustmentType ret = new FinancialAdjustmentType ();
    boolean bUse = false;

    // BT-179 Charge amount collected on behalf of a third party
    if (ifNotNull (convertAmount (aUBLAmount), ret::addActualAmount))
      bUse = true;

    // BT-180 Charges specification
    if (ifNotEmpty (sDescription, x -> ret.addReason (convertText (x))))
      bUse = true;

    return bUse ? ret : null;
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
    // Skip zero values — cii2ubl creates a synthetic TaxTotal with value 0 when
    // CII has no TaxTotalAmount (because UBL mandates TaxTotal). Emitting it
    // back would produce an element that wasn't in the original CII.
    for (final TaxAmountType aUBLTaxAmount : aUBLTaxTotalAmounts)
    {
      if (aUBLTaxAmount.getValue () != null && aUBLTaxAmount.getValue ().signum () != 0)
      {
        // Currency ID is required here
        ifNotNull (convertAmount (aUBLTaxAmount, true), ret::addTaxTotalAmount);
      }
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
