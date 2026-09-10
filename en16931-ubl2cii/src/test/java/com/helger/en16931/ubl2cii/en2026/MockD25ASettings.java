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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.helger.base.state.ESuccess;
import com.helger.cii.d25a.CCIID25A;
import com.helger.cii.d25a.CIID25ACrossIndustryInvoiceTypeMarshaller;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.io.file.FileSystemIterator;
import com.helger.io.file.FilenameHelper;
import com.helger.ubl25.UBL25Marshaller;
import com.helger.xml.namespace.MapBasedNamespaceContext;
import com.helger.xml.xpath.XPathHelper;

import oasis.names.specification.ubl.schema.xsd.creditnote_25.CreditNoteType;
import oasis.names.specification.ubl.schema.xsd.invoice_25.InvoiceType;
import un.unece.uncefact.data.standard.cii.d25a.CrossIndustryInvoiceType;

/**
 * Test infrastructure for the EN 16931:2026 conversion.<br>
 * The conversion is verified in three steps:
 * <ol>
 * <li>the UBL 2.5 source file is read through the UBL 2.5 marshaller, which validates it against
 * the UBL 2.5 XSD</li>
 * <li>the created CII D25A document is written through the D25A marshaller, which validates it
 * against the D25A XSD - a schema invalid document makes the write fail</li>
 * <li>the individual business terms are asserted with XPath expressions taken from
 * <code>docs/en16931-2026-syntax.md</code></li>
 * </ol>
 * All XPath expressions are relative to the document element, exactly like the CII paths in the
 * mapping table, which are relative to <code>/rsm:CrossIndustryInvoice</code>.<br>
 * The UBL 2.5 test files are the output of <code>en16931-cii2ubl</code> for its hand written CII
 * D25A instances, so the corpus covers every row of the mapping table by construction. The D25A
 * originals are kept in <code>src/test/resources/external/cii-d25a/</code> as the round-trip
 * reference.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public final class MockD25ASettings
{
  public static final String BASE_TEST_DIR_INVOICE = "src/test/resources/external/ubl25/inv/";
  public static final String BASE_TEST_DIR_CREDIT_NOTE = "src/test/resources/external/ubl25/cn/";
  public static final String BASE_TEST_DIR_CII_D25A = "src/test/resources/external/cii-d25a/";
  public static final String BASE_DEST_DIR = "generated/cii-d25a/";

  private static final Logger LOGGER = LoggerFactory.getLogger (MockD25ASettings.class);

  private static final MapBasedNamespaceContext NS_CTX = new MapBasedNamespaceContext ().addMapping ("rsm",
                                                                                                     CCIID25A.XML_SCHEMA_CII_NAMESPACE_URL)
                                                                                        .addMapping ("ram",
                                                                                                     CCIID25A.XML_SCHEMA_RAM_NAMESPACE_URL)
                                                                                        .addMapping ("udt",
                                                                                                     CCIID25A.XML_SCHEMA_UDT_NAMESPACE_URL)
                                                                                        .addMapping ("qdt",
                                                                                                     CCIID25A.XML_SCHEMA_QDT_NAMESPACE_URL);

  private MockD25ASettings ()
  {}

  @NonNull
  private static XPathExpression _createXPath (@NonNull final String sXPath)
  {
    final XPathExpression ret = XPathHelper.createNewXPathExpression (XPathHelper.createNewXPath (NS_CTX), sXPath);
    assertNotNull ("Failed to compile the XPath expression '" + sXPath + "'", ret);
    return ret;
  }

  @NonNull
  private static ICommonsList <File> _getAllTestFiles (@NonNull final String sDir)
  {
    final ICommonsList <File> ret = new CommonsArrayList <> ();
    for (final File f : new FileSystemIterator (new File (sDir)))
      if (f.isFile () && f.getName ().endsWith (".xml"))
        ret.add (f);
    return ret;
  }

  @NonNull
  public static ICommonsList <File> getAllTestFilesUBL25Invoice ()
  {
    return _getAllTestFiles (BASE_TEST_DIR_INVOICE);
  }

  @NonNull
  public static ICommonsList <File> getAllTestFilesUBL25CreditNote ()
  {
    return _getAllTestFiles (BASE_TEST_DIR_CREDIT_NOTE);
  }

  /**
   * Convert a UBL 2.5 file to CII D25A and return the result as the document element. The source is
   * validated against the UBL 2.5 XSD on read and the result against the D25A XSD on write.
   *
   * @param sFilename
   *        Filename relative to the invoice respectively credit note test directory. May not be
   *        <code>null</code>.
   * @param bIsInvoice
   *        <code>true</code> if the source is an Invoice, <code>false</code> for a CreditNote.
   * @return The document element of the created CII D25A document. Never <code>null</code>.
   */
  @NonNull
  public static Element convertAndValidate (@NonNull final String sFilename, final boolean bIsInvoice)
  {
    final File aSrcFile = new File (bIsInvoice ? BASE_TEST_DIR_INVOICE : BASE_TEST_DIR_CREDIT_NOTE, sFilename);
    assertTrue ("Not existing: " + aSrcFile.getAbsolutePath (), aSrcFile.exists ());

    LOGGER.info ("Converting " + aSrcFile.toString () + " to CII D25A");

    final ErrorList aErrorList = new ErrorList ();
    final CrossIndustryInvoiceType aCII;
    if (bIsInvoice)
    {
      // Step 1: read - validates against the UBL 2.5 XSD
      final InvoiceType aUBLInvoice = UBL25Marshaller.invoice ().setCollectErrors (aErrorList).read (aSrcFile);
      assertNotNull ("Failed to read '" + sFilename + "' as UBL 2.5 Invoice: " + aErrorList.toString (), aUBLInvoice);

      // Step 2: convert
      aCII = UBL25InvoiceToCIID25AConverter.convertToCrossIndustryInvoice (aUBLInvoice, aErrorList);
    }
    else
    {
      final CreditNoteType aUBLCreditNote = UBL25Marshaller.creditNote ().setCollectErrors (aErrorList).read (aSrcFile);
      assertNotNull ("Failed to read '" + sFilename + "' as UBL 2.5 Credit Note: " + aErrorList.toString (),
                     aUBLCreditNote);

      aCII = UBL25CreditNoteToCIID25AConverter.convertToCrossIndustryInvoice (aUBLCreditNote, aErrorList);
    }
    assertTrue ("Errors: " + aErrorList.toString (), aErrorList.containsNoError ());
    assertNotNull ("Conversion of '" + sFilename + "' returned null", aCII);

    // Step 3: write - validates against the CII D25A XSD
    final File aDestFile = new File (BASE_DEST_DIR, FilenameHelper.getBaseName (sFilename) + "-cii.xml");
    final ESuccess eSuccess = new CIID25ACrossIndustryInvoiceTypeMarshaller ().setFormattedOutput (true)
                                                                              .write (aCII, aDestFile);
    assertTrue ("The created CII D25A document of '" + sFilename + "' is not XSD valid", eSuccess.isSuccess ());

    final Document aDoc = new CIID25ACrossIndustryInvoiceTypeMarshaller ().getAsDocument (aCII);
    assertNotNull ("Failed to serialize the CII D25A document of '" + sFilename + "'", aDoc);

    return aDoc.getDocumentElement ();
  }

  @Nullable
  private static Node _selectSingle (@NonNull final Node aCtx, @NonNull final String sXPath)
  {
    try
    {
      return (Node) _createXPath (sXPath).evaluate (aCtx, XPathConstants.NODE);
    }
    catch (final XPathExpressionException ex)
    {
      fail ("Failed to evaluate the XPath expression '" + sXPath + "': " + ex.getMessage ());
      return null;
    }
  }

  @NonNull
  private static NodeList _selectAll (@NonNull final Node aCtx, @NonNull final String sXPath)
  {
    try
    {
      return (NodeList) _createXPath (sXPath).evaluate (aCtx, XPathConstants.NODESET);
    }
    catch (final XPathExpressionException ex)
    {
      fail ("Failed to evaluate the XPath expression '" + sXPath + "': " + ex.getMessage ());
      throw new IllegalStateException (ex);
    }
  }

  /**
   * Assert that the provided XPath selects at least one node, and that the first one has the
   * provided text content.
   *
   * @param aCtx
   *        The context node, usually the document element. May not be <code>null</code>.
   * @param sXPath
   *        The XPath relative to the context node. May not be <code>null</code>.
   * @param sExpected
   *        The expected text content. May not be <code>null</code>.
   */
  public static void assertXPath (@NonNull final Node aCtx,
                                  @NonNull final String sXPath,
                                  @NonNull final String sExpected)
  {
    final Node aNode = _selectSingle (aCtx, sXPath);
    assertNotNull ("No node found for '" + sXPath + "', expected the value '" + sExpected + "'", aNode);
    assertEquals ("Wrong value at '" + sXPath + "'", sExpected, aNode.getTextContent ());
  }

  /**
   * Assert that the provided XPath selects no node at all.
   *
   * @param aCtx
   *        The context node, usually the document element. May not be <code>null</code>.
   * @param sXPath
   *        The XPath relative to the context node. May not be <code>null</code>.
   */
  public static void assertNoXPath (@NonNull final Node aCtx, @NonNull final String sXPath)
  {
    final Node aNode = _selectSingle (aCtx, sXPath);
    if (aNode != null)
      fail ("Expected no node at '" + sXPath + "' but found one with the value '" + aNode.getTextContent () + "'");
  }

  /**
   * Assert the number of nodes selected by the provided XPath.
   *
   * @param aCtx
   *        The context node, usually the document element. May not be <code>null</code>.
   * @param sXPath
   *        The XPath relative to the context node. May not be <code>null</code>.
   * @param nExpected
   *        The expected number of nodes.
   */
  public static void assertXPathCount (@NonNull final Node aCtx, @NonNull final String sXPath, final int nExpected)
  {
    assertEquals ("Wrong number of nodes at '" + sXPath + "'", nExpected, _selectAll (aCtx, sXPath).getLength ());
  }
}
