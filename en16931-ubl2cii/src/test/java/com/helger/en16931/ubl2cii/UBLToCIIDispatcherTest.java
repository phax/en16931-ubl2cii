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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.basics.EEN16931Edition;
import com.helger.io.file.SimpleFileIO;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Test class for class {@link UBLToCIIDispatcher}.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public final class UBLToCIIDispatcherTest
{
  private static final String BASE_DIR_UBL21_INVOICE = "src/test/resources/external/ubl21/inv/peppol/";
  private static final String BASE_DIR_UBL25_INVOICE = "src/test/resources/external/ubl25/inv/";
  private static final String BASE_DIR_UBL25_CREDIT_NOTE = "src/test/resources/external/ubl25/cn/";

  private static final String UBL_HEAD_INVOICE = "<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\"" +
                                                 " xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">";

  @NonNull
  private static Document _parse (@NonNull final String sXML)
  {
    final Document ret = DOMReader.readXMLDOM (sXML);
    assertNotNull ("Failed to parse the test XML", ret);
    return ret;
  }

  @NonNull
  private static Document _invoiceWithBT24 (@Nullable final String sBT24)
  {
    return _parse (UBL_HEAD_INVOICE +
                   (sBT24 == null ? "" : "<cbc:CustomizationID>" + sBT24 + "</cbc:CustomizationID>") +
                   "<cbc:ID>TEST-1</cbc:ID>" +
                   "</Invoice>");
  }

  @Test
  public void testDetectEdition2017 ()
  {
    final ErrorList aErrorList = new ErrorList ();
    assertEquals (EEN16931Edition.EN2017,
                  UBLToCIIDispatcher.detectEdition (_invoiceWithBT24 ("urn:cen.eu:en16931:2017"), aErrorList));
    assertTrue (aErrorList.containsNoError ());
  }

  @Test
  public void testDetectEdition2026 ()
  {
    final ErrorList aErrorList = new ErrorList ();
    assertEquals (EEN16931Edition.EN2026,
                  UBLToCIIDispatcher.detectEdition (_invoiceWithBT24 ("urn:cen.eu:en16931:2026"), aErrorList));
    assertTrue (aErrorList.containsNoError ());
  }

  /**
   * The customization identifier of a national or sectoral specification appends its own identifier
   * after the EN 16931 one, so BT-24 must be matched by prefix.
   */
  @Test
  public void testDetectEditionWithCompliantSuffix ()
  {
    final ErrorList aErrorList = new ErrorList ();
    assertEquals (EEN16931Edition.EN2017,
                  UBLToCIIDispatcher.detectEdition (_invoiceWithBT24 ("urn:cen.eu:en16931:2017#compliant#urn:xeinkauf.de:kosit:xrechnung_3.0"),
                                                    aErrorList));
    assertTrue (aErrorList.containsNoError ());

    assertEquals (EEN16931Edition.EN2026,
                  UBLToCIIDispatcher.detectEdition (_invoiceWithBT24 ("urn:cen.eu:en16931:2026#compliant#urn:example:profile"),
                                                    aErrorList));
    assertTrue (aErrorList.containsNoError ());
  }

  /** A missing BT-24 must produce an error, never a guessed edition. */
  @Test
  public void testDetectEditionMissingBT24 ()
  {
    final ErrorList aErrorList = new ErrorList ();
    assertNull (UBLToCIIDispatcher.detectEdition (_invoiceWithBT24 (null), aErrorList));
    assertTrue (aErrorList.containsAtLeastOneError ());
  }

  /** An unknown BT-24 must produce an error, never a guessed edition. */
  @Test
  public void testDetectEditionUnknownBT24 ()
  {
    final ErrorList aErrorList = new ErrorList ();
    assertNull (UBLToCIIDispatcher.detectEdition (_invoiceWithBT24 ("urn:ferd:CrossIndustryDocument:invoice:1p0:comfort"),
                                                  aErrorList));
    assertTrue (aErrorList.containsAtLeastOneError ());
  }

  /** The dispatcher routes on two axes - the edition and the document type. */
  @Test
  public void testRouting ()
  {
    final ErrorList aErrorList = new ErrorList ();

    final Document aUBL21Invoice = DOMReader.readXMLDOM (new File (BASE_DIR_UBL21_INVOICE + "base-example.xml"));
    assertNotNull (aUBL21Invoice);
    final Serializable aD16B = UBLToCIIDispatcher.convertUBLtoCII (aUBL21Invoice, null, aErrorList);
    assertTrue (aErrorList.toString (), aErrorList.containsNoError ());
    assertNotNull (aD16B);
    assertTrue ("Expected CII D16B but got " + aD16B.getClass ().getName (),
                aD16B instanceof un.unece.uncefact.data.standard.crossindustryinvoice._100.CrossIndustryInvoiceType);

    aErrorList.clear ();
    final Document aUBL25Invoice = DOMReader.readXMLDOM (new File (BASE_DIR_UBL25_INVOICE +
                                                                   "d25a-full-invoice-ubl.xml"));
    assertNotNull (aUBL25Invoice);
    final Serializable aD25A = UBLToCIIDispatcher.convertUBLtoCII (aUBL25Invoice, null, aErrorList);
    assertTrue (aErrorList.toString (), aErrorList.containsNoError ());
    assertNotNull (aD25A);
    assertTrue ("Expected CII D25A but got " + aD25A.getClass ().getName (),
                aD25A instanceof un.unece.uncefact.data.standard.cii.d25a.CrossIndustryInvoiceType);

    aErrorList.clear ();
    final Document aUBL25CreditNote = DOMReader.readXMLDOM (new File (BASE_DIR_UBL25_CREDIT_NOTE +
                                                                      "d25a-full-creditnote-ubl.xml"));
    assertNotNull (aUBL25CreditNote);
    final Serializable aD25ACN = UBLToCIIDispatcher.convertUBLtoCII (aUBL25CreditNote, null, aErrorList);
    assertTrue (aErrorList.toString (), aErrorList.containsNoError ());
    assertNotNull (aD25ACN);
  }

  /**
   * An explicit edition skips the detection, which is what makes a document with an unusable BT-24
   * convertible at all.
   */
  @Test
  public void testForcedEdition ()
  {
    // A real UBL 2.1 invoice, but with the legacy ZUGFeRD identifier in BT-24 - about 2% of a real
    // world corpus looks like this and cannot be auto-detected
    final String sXML = SimpleFileIO.getFileAsString (new File (BASE_DIR_UBL21_INVOICE + "base-example.xml"),
                                                      StandardCharsets.UTF_8);
    assertNotNull (sXML);
    final Document aDoc = _parse (sXML.replaceFirst ("<cbc:CustomizationID>[^<]*</cbc:CustomizationID>",
                                                     "<cbc:CustomizationID>urn:ferd:CrossIndustryDocument:invoice:1p0:comfort</cbc:CustomizationID>"));

    // Without a forced edition this fails on the unknown BT-24
    final ErrorList aErrorList = new ErrorList ();
    assertNull (UBLToCIIDispatcher.convertUBLtoCII (aDoc, null, aErrorList));
    assertTrue (aErrorList.containsAtLeastOneError ());

    // With a forced edition it converts
    aErrorList.clear ();
    final Serializable aCII = UBLToCIIDispatcher.convertUBLtoCII (aDoc, EEN16931Edition.EN2017, aErrorList);
    assertTrue (aErrorList.toString (), aErrorList.containsNoError ());
    assertNotNull (aCII);
    assertTrue (aCII instanceof un.unece.uncefact.data.standard.crossindustryinvoice._100.CrossIndustryInvoiceType);
  }

  /** Anything that is neither a UBL Invoice nor a UBL Credit Note must be rejected. */
  @Test
  public void testUnsupportedDocumentType ()
  {
    final ErrorList aErrorList = new ErrorList ();
    final Document aDoc = _parse ("<Order xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Order-2\"/>");
    assertNull (UBLToCIIDispatcher.convertUBLtoCII (aDoc, null, aErrorList));
    assertTrue (aErrorList.containsAtLeastOneError ());
  }
}
