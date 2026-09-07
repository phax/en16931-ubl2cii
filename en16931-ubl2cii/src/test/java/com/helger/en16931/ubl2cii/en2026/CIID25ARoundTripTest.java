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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.base.string.StringHelper;
import com.helger.cii.d25a.CIID25ACrossIndustryInvoiceTypeMarshaller;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedSet;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.basics.codelist.EN16931CodeLists;
import com.helger.en16931.cii2ubl.en2026.CIID25AToUBL25Converter;
import com.helger.en16931.ubl2cii.MockRoundTrip;
import com.helger.en16931.ubl2cii.UBLToCIIDispatcher;
import com.helger.io.file.FileSystemIterator;
import com.helger.xml.XMLHelper;

import oasis.names.specification.ubl.schema.xsd.creditnote_25.CreditNoteType;
import oasis.names.specification.ubl.schema.xsd.invoice_25.InvoiceType;
import un.unece.uncefact.data.standard.cii.d25a.CrossIndustryInvoiceType;

/**
 * Round-trip test for the EN 16931:2026 binding: CII D25A &rarr; UBL 2.5 (via
 * <code>en16931-cii2ubl</code>) &rarr; CII D25A (via this library).<br>
 * The UBL 2.5 corpus of {@link MockD25ASettings} is the output of the first leg, so this test is
 * what keeps the two libraries honest about the leg in between. It compares the leaf values of the
 * original CII with those of the re-created one and fails if a value is lost that is not in
 * {@link #EXPECTED_LOSSES}.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public final class CIID25ARoundTripTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (CIID25ARoundTripTest.class);

  /**
   * Leaf element paths whose value a round trip cannot preserve, with the reason. Anything else
   * that goes missing is a defect in one of the two libraries.
   */
  private static final ICommonsOrderedSet <String> EXPECTED_LOSSES;
  static
  {
    EXPECTED_LOSSES = new CommonsLinkedHashSet <> ();
    // BT-11-1 Project name is a CII only element with no UBL counterpart, so the original value
    // cannot survive. On the way back the binding prescribes repeating BT-11 as the name, which is
    // what this library writes - the same element, a different value.
    EXPECTED_LOSSES.add ("/SpecifiedProcuringProject/Name");
    // BT-90 Bank assigned creditor identifier. EN 16931 defines it inside BG-19 DIRECT DEBIT, and
    // en16931-cii2ubl maps it only there. All but one of the CII originals carry
    // ram:CreditorReferenceID next to a credit transfer payment means instead, which is outside
    // BG-19, so the value is dropped on the way to UBL. The one direct debit original round trips
    // it - so this is a property of the test data, not a defect of either library.
    EXPECTED_LOSSES.add ("/ApplicableHeaderTradeSettlement/CreditorReferenceID");
    // BT-114 Rounding amount. Every CII original has it as "0.00", and en16931-cii2ubl
    // deliberately skips a zero rounding amount - a documented work around for
    // https://github.com/ConnectingEurope/eInvoicing-EN16931/issues/242. A non-zero rounding
    // amount does round trip, see the coverage invoice of UBL25RoundTripTest.
    EXPECTED_LOSSES.add ("/SpecifiedTradeSettlementHeaderMonetarySummation/RoundingAmount");
    // BT-149/BT-150 Item price base quantity. CII carries it on the net *and* on the gross price,
    // UBL has a single cac:Price/cbc:BaseQuantity for both. The way back can only restore one of
    // them, and it restores the gross one, because that is the one cii2ubl reads. An inherent
    // limitation of the UBL representation, not of either library.
    EXPECTED_LOSSES.add ("/NetPriceProductTradePrice/BasisQuantity");
    // BT-84/BT-91 Payment and debited account identifier. CII distinguishes ram:IBANID from
    // ram:ProprietaryID, UBL has one cbc:ID for both, so the distinction cannot survive a round
    // trip - an account written as ProprietaryID comes back as IBANID.
    EXPECTED_LOSSES.add ("/PayeePartyCreditorFinancialAccount/ProprietaryID");
    EXPECTED_LOSSES.add ("/PayerPartyDebtorFinancialAccount/ProprietaryID");
  }

  @NonNull
  private static ICommonsList <File> _getAllD25AFiles ()
  {
    final ICommonsList <File> ret = new CommonsArrayList <> ();
    for (final File f : new FileSystemIterator (new File (MockD25ASettings.BASE_TEST_DIR_CII_D25A)))
      if (f.isFile () && f.getName ().endsWith (".xml"))
        ret.add (f);
    return ret;
  }

  /**
   * Both converters normalise the trailing zeroes of a decimal, so "20.00" and "20" are the same
   * value. Comparing the literals would report hundreds of differences that are not data loss.
   */
  @NonNull
  private static String _normalizeValue (@NonNull final String sValue)
  {
    try
    {
      return new BigDecimal (sValue).stripTrailingZeros ().toPlainString ();
    }
    catch (final NumberFormatException ex)
    {
      return sValue;
    }
  }

  /**
   * Collect every leaf element as "path=value", so two documents can be compared without depending
   * on element order.
   */
  private static void _collectLeaves (@NonNull final Element aElement,
                                      @NonNull final String sParentPath,
                                      @NonNull final ICommonsOrderedSet <String> aTarget)
  {
    final String sPath = sParentPath + "/" + XMLHelper.getQName (aElement).getLocalPart ();

    boolean bHasChildElement = false;
    for (Node aChild = aElement.getFirstChild (); aChild != null; aChild = aChild.getNextSibling ())
      if (aChild.getNodeType () == Node.ELEMENT_NODE)
      {
        bHasChildElement = true;
        _collectLeaves ((Element) aChild, sPath, aTarget);
      }

    if (!bHasChildElement)
    {
      final String sValue = StringHelper.trim (aElement.getTextContent ());
      if (StringHelper.isNotEmpty (sValue))
        aTarget.add (sPath + "=" + _normalizeValue (sValue));
    }
  }

  @NonNull
  private static ICommonsOrderedSet <String> _getAllLeaves (@NonNull final Document aDoc)
  {
    final ICommonsOrderedSet <String> ret = new CommonsLinkedHashSet <> ();
    _collectLeaves (aDoc.getDocumentElement (), "", ret);
    return ret;
  }

  private static boolean _isExpectedLoss (@NonNull final String sLeaf)
  {
    final String sPath = sLeaf.substring (0, sLeaf.indexOf ('='));
    return EXPECTED_LOSSES.containsAny (sPath::endsWith);
  }

  /**
   * BT-189 without BT-190: <code>cbc:LineID</code> is mandatory inside the UBL
   * <code>cac:DespatchLineReference</code>, so en16931-cii2ubl has to invent a value for it. Both
   * libraries have to agree on which one, or the round trip invents a line reference that the
   * original document never had. The agreed value is
   * {@link EN16931CodeLists#MISSING_VALUE_PLACEHOLDER}, which this library drops again.
   */
  @Test
  public void testLineReferencePlaceholderIsNotInvented ()
  {
    final CIID25ACrossIndustryInvoiceTypeMarshaller aCIIMarshaller = new CIID25ACrossIndustryInvoiceTypeMarshaller ();
    final CrossIndustryInvoiceType aOrigCII = aCIIMarshaller.read (new File (MockD25ASettings.BASE_TEST_DIR_CII_D25A +
                                                                            "d25a-new-lineref-invoice.xml"));
    assertNotNull (aOrigCII);

    // BT-189 stays, BT-190 goes - the UBL element around it remains mandatory
    aOrigCII.getSupplyChainTradeTransaction ()
            .getIncludedSupplyChainTradeLineItemAtIndex (0)
            .getSpecifiedLineTradeDelivery ()
            .getDespatchAdviceReferencedDocument ()
            .setLineID ((un.unece.uncefact.data.standard.cii.d25a.udt.IDType) null);

    final ErrorList aErrorList = new ErrorList ();
    final Serializable aUBL = new CIID25AToUBL25Converter ().convertToInvoice (aOrigCII, aErrorList);
    assertTrue (aErrorList.toString (), aErrorList.containsNoError ());
    assertNotNull (aUBL);

    // en16931-cii2ubl writes the placeholder into the mandatory element
    final Document aUBLDoc = com.helger.ubl25.UBL25Marshaller.invoice ()
                                                             .getAsDocument ((InvoiceType) aUBL);
    assertNotNull (aUBLDoc);
    assertTrue ("en16931-cii2ubl no longer writes the agreed placeholder",
                MockRoundTrip.getAllLeaves (aUBLDoc)
                             .containsKey ("/Invoice/InvoiceLine/DespatchLineReference/LineID=" +
                                           EN16931CodeLists.MISSING_VALUE_PLACEHOLDER));

    // and this library drops it again, so no BT-190 is invented
    aErrorList.clear ();
    final Serializable aRoundTripCII = UBLToCIIDispatcher.convertUBLtoCII (aUBLDoc, null, aErrorList);
    assertTrue (aErrorList.toString (), aErrorList.containsNoError ());
    assertNotNull (aRoundTripCII);

    final Document aRoundTripDoc = aCIIMarshaller.getAsDocument ((CrossIndustryInvoiceType) aRoundTripCII);
    assertNotNull (aRoundTripDoc);
    for (final String sLeaf : MockRoundTrip.getAllLeaves (aRoundTripDoc).keySet ())
      if (sLeaf.startsWith ("/CrossIndustryInvoice/SupplyChainTradeTransaction/IncludedSupplyChainTradeLineItem/SpecifiedLineTradeDelivery/DespatchAdviceReferencedDocument/LineID"))
        fail ("The round trip invented the line reference " + sLeaf);
  }

  /**
   * BT-14 without BT-13: <code>cbc:ID</code> is mandatory in the UBL <code>cac:OrderReference</code>
   * that BT-14 needs, so en16931-cii2ubl writes the agreed placeholder there. This library has to
   * recognise it, or the round trip invents a purchase order reference the original never had.
   */
  @Test
  public void testOrderReferencePlaceholderIsNotInvented ()
  {
    final CIID25ACrossIndustryInvoiceTypeMarshaller aCIIMarshaller = new CIID25ACrossIndustryInvoiceTypeMarshaller ();
    final CrossIndustryInvoiceType aOrigCII = aCIIMarshaller.read (new File (MockD25ASettings.BASE_TEST_DIR_CII_D25A +
                                                                            "d25a-full-invoice.xml"));
    assertNotNull (aOrigCII);

    // BT-14 stays, BT-13 goes - the UBL element around it remains mandatory
    aOrigCII.getSupplyChainTradeTransaction ()
            .getApplicableHeaderTradeAgreement ()
            .setBuyerOrderReferencedDocument (null);

    final ErrorList aErrorList = new ErrorList ();
    final Serializable aUBL = new CIID25AToUBL25Converter ().convertToInvoice (aOrigCII, aErrorList);
    assertTrue (aErrorList.toString (), aErrorList.containsNoError ());
    assertNotNull (aUBL);

    final Document aUBLDoc = com.helger.ubl25.UBL25Marshaller.invoice ().getAsDocument ((InvoiceType) aUBL);
    assertNotNull (aUBLDoc);
    assertTrue ("en16931-cii2ubl no longer writes the agreed placeholder",
                MockRoundTrip.getAllLeaves (aUBLDoc)
                             .containsKey ("/Invoice/OrderReference/ID=" +
                                           EN16931CodeLists.MISSING_VALUE_PLACEHOLDER));

    aErrorList.clear ();
    final Serializable aRoundTripCII = UBLToCIIDispatcher.convertUBLtoCII (aUBLDoc, null, aErrorList);
    assertTrue (aErrorList.toString (), aErrorList.containsNoError ());
    assertNotNull (aRoundTripCII);

    final Document aRoundTripDoc = aCIIMarshaller.getAsDocument ((CrossIndustryInvoiceType) aRoundTripCII);
    assertNotNull (aRoundTripDoc);
    for (final String sLeaf : MockRoundTrip.getAllLeaves (aRoundTripDoc).keySet ())
      if (sLeaf.contains ("/BuyerOrderReferencedDocument/"))
        fail ("The round trip invented the purchase order reference " + sLeaf);
  }

  @Test
  public void testRoundTripAllD25AFiles ()
  {
    final CIID25AToUBL25Converter aCIIToUBL = new CIID25AToUBL25Converter ();
    final CIID25ACrossIndustryInvoiceTypeMarshaller aCIIMarshaller = new CIID25ACrossIndustryInvoiceTypeMarshaller ();

    final ICommonsList <File> aAllFiles = _getAllD25AFiles ();
    assertTrue ("Suspiciously few D25A files found: " + aAllFiles.size (), aAllFiles.size () >= 16);

    final ICommonsOrderedSet <String> aAllUnexpectedLosses = new CommonsLinkedHashSet <> ();
    for (final File aFile : aAllFiles)
    {
      // Step 1: read the original CII D25A
      final CrossIndustryInvoiceType aOrigCII = aCIIMarshaller.read (aFile);
      assertNotNull ("Failed to read " + aFile, aOrigCII);

      // Step 2: CII D25A -> UBL 2.5
      final ErrorList aErrorList = new ErrorList ();
      final Serializable aUBL = aCIIToUBL.convertCIItoUBL (aFile, aErrorList);
      assertTrue ("CII->UBL errors for " + aFile + ": " + aErrorList, aErrorList.containsNoError ());
      assertNotNull ("CII->UBL returned null for " + aFile, aUBL);
      assertTrue ("Neither an Invoice nor a Credit Note for " + aFile,
                  aUBL instanceof InvoiceType || aUBL instanceof CreditNoteType);

      // Step 3: UBL 2.5 -> CII D25A, through the dispatcher, so the edition detection is exercised
      aErrorList.clear ();
      final Document aUBLDoc = aUBL instanceof final InvoiceType aInv ? com.helger.ubl25.UBL25Marshaller.invoice ()
                                                                                                        .getAsDocument (aInv)
                                                                      : com.helger.ubl25.UBL25Marshaller.creditNote ()
                                                                                                        .getAsDocument ((CreditNoteType) aUBL);
      assertNotNull ("Failed to serialize the UBL 2.5 document of " + aFile, aUBLDoc);
      final Serializable aRoundTripCII = UBLToCIIDispatcher.convertUBLtoCII (aUBLDoc, null, aErrorList);
      assertTrue ("UBL->CII errors for " + aFile + ": " + aErrorList, aErrorList.containsNoError ());
      assertNotNull ("UBL->CII returned null for " + aFile, aRoundTripCII);
      assertTrue ("Expected CII D25A for " + aFile, aRoundTripCII instanceof CrossIndustryInvoiceType);

      // Step 4: compare the leaf values
      final Document aOrigDoc = aCIIMarshaller.getAsDocument (aOrigCII);
      final Document aRoundTripDoc = aCIIMarshaller.getAsDocument ((CrossIndustryInvoiceType) aRoundTripCII);
      assertNotNull (aOrigDoc);
      assertNotNull (aRoundTripDoc);

      final ICommonsOrderedSet <String> aOrigLeaves = _getAllLeaves (aOrigDoc);
      final ICommonsOrderedSet <String> aRoundTripLeaves = _getAllLeaves (aRoundTripDoc);

      for (final String sLeaf : aOrigLeaves)
        if (!aRoundTripLeaves.contains (sLeaf) && !_isExpectedLoss (sLeaf))
          aAllUnexpectedLosses.add (aFile.getName () + " :: " + sLeaf);

      LOGGER.info ("Round-tripped " +
                   aFile.getName () +
                   ": " +
                   aOrigLeaves.size () +
                   " leaf values in, " +
                   aRoundTripLeaves.size () +
                   " out");
    }

    if (aAllUnexpectedLosses.isNotEmpty ())
      fail ("The CII D25A -> UBL 2.5 -> CII D25A round trip lost " +
            aAllUnexpectedLosses.size () +
            " leaf values that neither library documents as unpreservable:\n  " +
            String.join ("\n  ", aAllUnexpectedLosses));
  }
}
