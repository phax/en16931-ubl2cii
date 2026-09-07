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

import java.io.OutputStream;
import java.io.Serializable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.annotation.WillClose;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.cii.d16b.CIID16BCrossIndustryInvoiceTypeMarshaller;
import com.helger.cii.d25a.CIID25ACrossIndustryInvoiceTypeMarshaller;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.basics.EEN16931Edition;
import com.helger.en16931.basics.EEN16931SyntaxKind;
import com.helger.en16931.ubl2cii.en2017.UBL21CreditNoteToCIID16BConverter;
import com.helger.en16931.ubl2cii.en2017.UBL21InvoiceToCIID16BConverter;
import com.helger.en16931.ubl2cii.en2026.UBL25CreditNoteToCIID25AConverter;
import com.helger.en16931.ubl2cii.en2026.UBL25InvoiceToCIID25AConverter;
import com.helger.ubl21.UBL21Marshaller;
import com.helger.ubl25.UBL25Marshaller;
import com.helger.xml.XMLHelper;

/**
 * Converts a UBL document to CII, choosing the EN 16931 edition and the document type
 * automatically.
 * <ul>
 * <li>The <b>document type</b> - Invoice or Credit Note - comes from the document element.</li>
 * <li>The <b>edition</b> comes from BT-24 (Specification identifier), see
 * {@link EEN16931Edition}. It cannot be taken from the XML namespaces, because UBL 2.1 and UBL 2.5
 * declare identical ones. An explicit edition can be passed in, which skips the detection.</li>
 * </ul>
 * The result is either a CII D16B or a CII D25A <code>CrossIndustryInvoiceType</code>. Those are
 * unrelated Java classes with the same name, so the common return type is {@link Serializable}. Use
 * {@link #writeCII(Serializable, OutputStream, ErrorList)} to serialize it without having to type
 * switch.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
@Immutable
public final class UBLToCIIDispatcher
{
  private static final Logger LOGGER = LoggerFactory.getLogger (UBLToCIIDispatcher.class);

  private UBLToCIIDispatcher ()
  {}

  private static void _error (@NonNull final ErrorList aErrorList, @NonNull final String sMsg)
  {
    aErrorList.add (SingleError.builderError ().errorText (sMsg).build ());
  }

  /**
   * Determine the EN 16931 edition of a UBL document from BT-24.
   *
   * @param aUBLNode
   *        The UBL document or its document element. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to fill if the edition cannot be determined. May not be
   *        <code>null</code>.
   * @return <code>null</code> if BT-24 is absent or matches no known edition. An error is added to
   *         the error list in that case - the edition is never guessed.
   */
  @Nullable
  public static EEN16931Edition detectEdition (@NonNull final Node aUBLNode, @NonNull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aUBLNode, "UBLNode");
    ValueEnforcer.notNull (aErrorList, "ErrorList");

    final String sSpecID = EEN16931Edition.getSpecificationIdentifier (aUBLNode);
    final EEN16931Edition ret = EEN16931Edition.getFromSpecificationIdentifierOrNull (sSpecID);
    if (ret == null)
      _error (aErrorList,
              sSpecID == null ? "Cannot determine the EN 16931 edition, because BT-24 (Specification identifier) is missing. Set the edition explicitly."
                              : "Cannot determine the EN 16931 edition from the BT-24 value '" +
                                sSpecID +
                                "'. Set the edition explicitly.");
    return ret;
  }

  /**
   * Convert a UBL document to CII.
   *
   * @param aUBLNode
   *        The UBL document or its document element. May not be <code>null</code>.
   * @param eForcedEdition
   *        The EN 16931 edition to use. May be <code>null</code> to detect it from BT-24.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   * @return <code>null</code> on error. Either a CII D16B or a CII D25A
   *         <code>CrossIndustryInvoiceType</code>, depending on the edition.
   */
  @Nullable
  public static Serializable convertUBLtoCII (@NonNull final Node aUBLNode,
                                              @Nullable final EEN16931Edition eForcedEdition,
                                              @NonNull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aUBLNode, "UBLNode");
    ValueEnforcer.notNull (aErrorList, "ErrorList");

    final EEN16931SyntaxKind eSyntaxKind = EEN16931SyntaxKind.getFromNodeOrNull (aUBLNode);
    if (eSyntaxKind != EEN16931SyntaxKind.UBL_INVOICE && eSyntaxKind != EEN16931SyntaxKind.UBL_CREDIT_NOTE)
    {
      final Element aDocElement = aUBLNode instanceof final Document aDoc ? aDoc.getDocumentElement ()
                                                                          : aUBLNode instanceof final Element aElem
                                                                                                                    ? aElem
                                                                                                                    : null;
      _error (aErrorList,
              "The XML document type " +
                          (aDocElement == null ? "(none)" : XMLHelper.getQName (aDocElement).toString ()) +
                          " is not a UBL Invoice and not a UBL Credit Note");
      return null;
    }

    EEN16931Edition eEdition = eForcedEdition;
    if (eEdition == null)
    {
      eEdition = detectEdition (aUBLNode, aErrorList);
      if (eEdition == null)
        return null;
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Detected EN 16931 edition " + eEdition.getID () + " and syntax " + eSyntaxKind.getID ());
    }

    final boolean bIsInvoice = eSyntaxKind == EEN16931SyntaxKind.UBL_INVOICE;
    switch (eEdition)
    {
      case EN2017:
      {
        if (bIsInvoice)
        {
          final var aUBLInvoice = UBL21Marshaller.invoice ().setCollectErrors (aErrorList).read (aUBLNode);
          return aUBLInvoice == null ? null
                                     : UBL21InvoiceToCIID16BConverter.convertToCrossIndustryInvoice (aUBLInvoice,
                                                                                                     aErrorList);
        }
        final var aUBLCreditNote = UBL21Marshaller.creditNote ().setCollectErrors (aErrorList).read (aUBLNode);
        return aUBLCreditNote == null ? null
                                      : UBL21CreditNoteToCIID16BConverter.convertToCrossIndustryInvoice (aUBLCreditNote,
                                                                                                        aErrorList);
      }
      case EN2026:
      {
        if (bIsInvoice)
        {
          final var aUBLInvoice = UBL25Marshaller.invoice ().setCollectErrors (aErrorList).read (aUBLNode);
          return aUBLInvoice == null ? null
                                     : UBL25InvoiceToCIID25AConverter.convertToCrossIndustryInvoice (aUBLInvoice,
                                                                                                     aErrorList);
        }
        final var aUBLCreditNote = UBL25Marshaller.creditNote ().setCollectErrors (aErrorList).read (aUBLNode);
        return aUBLCreditNote == null ? null
                                      : UBL25CreditNoteToCIID25AConverter.convertToCrossIndustryInvoice (aUBLCreditNote,
                                                                                                         aErrorList);
      }
      default:
        throw new IllegalStateException ("Unsupported EN 16931 edition " + eEdition);
    }
  }

  /**
   * Serialize a CII document created by {@link #convertUBLtoCII(Node, EEN16931Edition, ErrorList)},
   * picking the marshaller that matches its release.
   *
   * @param aCII
   *        The CII D16B or CII D25A document to write. May not be <code>null</code>.
   * @param aOS
   *        The stream to write to. May not be <code>null</code>. Is closed by this method.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   * @return {@link ESuccess#FAILURE} if the object is of neither release, or if writing fails.
   */
  @NonNull
  public static ESuccess writeCII (@NonNull final Serializable aCII,
                                   @NonNull @WillClose final OutputStream aOS,
                                   @NonNull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aCII, "CII");
    ValueEnforcer.notNull (aOS, "OutputStream");
    ValueEnforcer.notNull (aErrorList, "ErrorList");

    if (aCII instanceof final un.unece.uncefact.data.standard.crossindustryinvoice._100.CrossIndustryInvoiceType aD16B)
      return new CIID16BCrossIndustryInvoiceTypeMarshaller ().setFormattedOutput (true)
                                                             .setCollectErrors (aErrorList)
                                                             .write (aD16B, aOS);

    if (aCII instanceof final un.unece.uncefact.data.standard.cii.d25a.CrossIndustryInvoiceType aD25A)
      return new CIID25ACrossIndustryInvoiceTypeMarshaller ().setFormattedOutput (true)
                                                             .setCollectErrors (aErrorList)
                                                             .write (aD25A, aOS);

    _error (aErrorList, "The object of type " + aCII.getClass ().getName () + " is not a CII document");
    return ESuccess.FAILURE;
  }
}
