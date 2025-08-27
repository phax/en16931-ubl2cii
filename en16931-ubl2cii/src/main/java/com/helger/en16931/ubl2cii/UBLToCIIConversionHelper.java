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

import java.io.InputStream;
import java.io.OutputStream;

import org.w3c.dom.Document;

import com.helger.annotation.WillClose;
import com.helger.annotation.WillNotClose;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.state.ESuccess;
import com.helger.cii.d16b.CIID16BCrossIndustryInvoiceTypeMarshaller;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.ubl21.UBL21Marshaller;
import com.helger.xml.XMLHelper;
import com.helger.xml.sax.WrappedCollectingSAXErrorHandler;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xml.serialize.read.DOMReaderSettings;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import oasis.names.specification.ubl.schema.xsd.creditnote_21.CreditNoteType;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;
import un.unece.uncefact.data.standard.crossindustryinvoice._100.CrossIndustryInvoiceType;

/**
 * @author Vartika Gupta
 * @author Philip Helger
 */
@Immutable
public final class UBLToCIIConversionHelper
{
  private UBLToCIIConversionHelper ()
  {}

  @Nullable
  public static CrossIndustryInvoiceType convertUBL21InvoiceToCIID16B (@Nonnull @WillNotClose final InputStream aIS,
                                                                       @Nonnull final ErrorList aErrorList)
  {
    // Read UBL 2.1
    final InvoiceType aUBLInvoice = UBL21Marshaller.invoice ().setCollectErrors (aErrorList).read (aIS);
    if (aUBLInvoice == null)
      return null;

    // Main conversion
    return UBL21InvoiceToCIID16BConverter.convertToCrossIndustryInvoice (aUBLInvoice, aErrorList);
  }

  @Nonnull
  public static ESuccess convertUBL21InvoiceToCIID16B (@Nonnull @WillNotClose final InputStream aIS,
                                                       @Nonnull @WillClose final OutputStream aOS,
                                                       @Nonnull final ErrorList aErrorList)
  {
    final CrossIndustryInvoiceType aCrossIndustryInvoice = convertUBL21InvoiceToCIID16B (aIS, aErrorList);
    if (aCrossIndustryInvoice == null)
      return ESuccess.FAILURE;

    // Write CII D16B XML
    return new CIID16BCrossIndustryInvoiceTypeMarshaller ().setFormattedOutput (true)
                                                           .setCollectErrors (aErrorList)
                                                           .write (aCrossIndustryInvoice, aOS);
  }

  @Nullable
  public static CrossIndustryInvoiceType convertUBL21CreditNoteToCIID16B (@Nonnull @WillNotClose final InputStream aIS,
                                                                          @Nonnull final ErrorList aErrorList)
  {
    // Read UBL 2.1
    final CreditNoteType aUBLCreditNote = UBL21Marshaller.creditNote ().setCollectErrors (aErrorList).read (aIS);
    if (aUBLCreditNote == null)
      return null;

    // Main conversion
    return UBL21CreditNoteToCIID16BConverter.convertToCrossIndustryInvoice (aUBLCreditNote, aErrorList);
  }

  @Nonnull
  public static ESuccess convertUBL21CreditNoteToCIID16B (@Nonnull @WillNotClose final InputStream aIS,
                                                          @Nonnull @WillClose final OutputStream aOS,
                                                          @Nonnull final ErrorList aErrorList)
  {
    // Main conversion
    final CrossIndustryInvoiceType aCrossIndustryInvoice = convertUBL21CreditNoteToCIID16B (aIS, aErrorList);
    if (aCrossIndustryInvoice == null)
      return ESuccess.FAILURE;

    // Write CII D16B XML
    return new CIID16BCrossIndustryInvoiceTypeMarshaller ().setFormattedOutput (true)
                                                           .setCollectErrors (aErrorList)
                                                           .write (aCrossIndustryInvoice, aOS);
  }

  @Nullable
  public static CrossIndustryInvoiceType convertUBL21AutoDetectToCIID16B (@Nonnull @WillNotClose final InputStream aIS,
                                                                          @Nonnull final ErrorList aErrorList)
  {
    // Read exactly once into XML
    final Document aDoc = DOMReader.readXMLDOM (aIS,
                                                new DOMReaderSettings ().setErrorHandler (new WrappedCollectingSAXErrorHandler (aErrorList)));
    if (aDoc == null || aDoc.getDocumentElement () == null)
      return null;

    final String sRootLocalName = aDoc.getDocumentElement ().getLocalName ();

    if ("Invoice".equals (sRootLocalName))
    {
      // Read UBL 2.1 Invoice
      final InvoiceType aUBLInvoice = UBL21Marshaller.invoice ().setCollectErrors (aErrorList).read (aDoc);
      if (aUBLInvoice == null)
        return null;

      // Main conversion
      return UBL21InvoiceToCIID16BConverter.convertToCrossIndustryInvoice (aUBLInvoice, aErrorList);
    }

    if ("CreditNote".equals (sRootLocalName))
    {
      // Read UBL 2.1 Credit Note
      final CreditNoteType aUBLCreditNote = UBL21Marshaller.creditNote ().setCollectErrors (aErrorList).read (aDoc);
      if (aUBLCreditNote == null)
        return null;

      // Main conversion
      return UBL21CreditNoteToCIID16BConverter.convertToCrossIndustryInvoice (aUBLCreditNote, aErrorList);
    }

    aErrorList.add (SingleError.builderError ()
                               .errorText ("The XML document type " +
                                           XMLHelper.getQName (aDoc.getDocumentElement ()) +
                                           " is not supported")
                               .build ());
    return null;
  }

  @Nonnull
  public static ESuccess convertUBL21AutoDetectToCIID16B (@Nonnull @WillNotClose final InputStream aIS,
                                                          @Nonnull @WillClose final OutputStream aOS,
                                                          @Nonnull final ErrorList aErrorList)
  {
    final CrossIndustryInvoiceType aCrossIndustryInvoice = convertUBL21AutoDetectToCIID16B (aIS, aErrorList);
    if (aCrossIndustryInvoice == null)
      return ESuccess.FAILURE;

    // Write CII D16B XML
    return new CIID16BCrossIndustryInvoiceTypeMarshaller ().setFormattedOutput (true)
                                                           .setCollectErrors (aErrorList)
                                                           .write (aCrossIndustryInvoice, aOS);
  }
}
