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

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.xml.XMLHelper;

/**
 * Shared infrastructure of all round-trip tests. It reduces a document to the multiset of its leaf
 * values - the text of every element without child elements plus every attribute - so two documents
 * can be compared without depending on element order or on the syntax in between.<br>
 * Values are counted rather than collected in a set, because a converter that writes only the first
 * of several repeated containers would otherwise pass unnoticed.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public final class MockRoundTrip
{
  private MockRoundTrip ()
  {}

  /**
   * Both directions normalise the trailing zeroes of a decimal, so "20.00" and "20" are the same
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

  private static void _addValue (@NonNull final ICommonsOrderedMap <String, Integer> aTarget,
                                 @NonNull final String sPath,
                                 @NonNull final String sValue)
  {
    aTarget.merge (sPath + "=" + _normalizeValue (sValue),
                   Integer.valueOf (1),
                   (a, b) -> Integer.valueOf (a.intValue () + b.intValue ()));
  }

  private static void _collectLeavesRecursive (@NonNull final Element aElement,
                                               @NonNull final String sParentPath,
                                               @NonNull final ICommonsOrderedMap <String, Integer> aTarget)
  {
    final String sPath = sParentPath + "/" + XMLHelper.getQName (aElement).getLocalPart ();

    // Attributes carry business terms of their own - the unit code of BT-130 or the scheme
    // identifier of BT-29 are attributes, not elements
    final NamedNodeMap aAttrs = aElement.getAttributes ();
    for (int i = 0; i < aAttrs.getLength (); ++i)
    {
      final Attr aAttr = (Attr) aAttrs.item (i);
      // Namespace declarations are syntax, not data
      if (!"http://www.w3.org/2000/xmlns/".equals (aAttr.getNamespaceURI ()))
      {
        final String sAttrValue = StringHelper.trim (aAttr.getValue ());
        if (StringHelper.isNotEmpty (sAttrValue))
          _addValue (aTarget, sPath + "/@" + aAttr.getLocalName (), sAttrValue);
      }
    }

    boolean bHasChildElement = false;
    for (Node aChild = aElement.getFirstChild (); aChild != null; aChild = aChild.getNextSibling ())
      if (aChild.getNodeType () == Node.ELEMENT_NODE)
      {
        bHasChildElement = true;
        _collectLeavesRecursive ((Element) aChild, sPath, aTarget);
      }

    if (!bHasChildElement)
    {
      final String sValue = StringHelper.trim (aElement.getTextContent ());
      if (StringHelper.isNotEmpty (sValue))
        _addValue (aTarget, sPath, sValue);
    }
  }

  /**
   * Collect every leaf value of the document as "path=value" mapped to the number of occurrences.
   *
   * @param aDoc
   *        The document to analyze. May not be <code>null</code>.
   * @return Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsOrderedMap <String, Integer> getAllLeaves (@NonNull final Document aDoc)
  {
    final ICommonsOrderedMap <String, Integer> ret = new CommonsLinkedHashMap <> ();
    _collectLeavesRecursive (aDoc.getDocumentElement (), "", ret);
    return ret;
  }

  /**
   * Determine every leaf value of the original document that the round trip did not reproduce, in
   * the original number of occurrences.
   *
   * @param aOrig
   *        The original document. May not be <code>null</code>.
   * @param aRoundTrip
   *        The document that came back. May not be <code>null</code>.
   * @param aIsExpectedLoss
   *        Predicate that receives the "path=value" of a lost leaf and returns <code>true</code> if
   *        the loss is documented and therefore not a defect. May not be <code>null</code>.
   * @return The description of every undocumented loss. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <String> getUnexpectedLosses (@NonNull final Document aOrig,
                                                           @NonNull final Document aRoundTrip,
                                                           @NonNull final Predicate <String> aIsExpectedLoss)
  {
    final ICommonsOrderedMap <String, Integer> aOrigLeaves = getAllLeaves (aOrig);
    final ICommonsOrderedMap <String, Integer> aRoundTripLeaves = getAllLeaves (aRoundTrip);

    final ICommonsList <String> ret = new CommonsArrayList <> ();
    for (final Map.Entry <String, Integer> aEntry : aOrigLeaves.entrySet ())
    {
      final int nOrig = aEntry.getValue ().intValue ();
      final int nRoundTrip = aRoundTripLeaves.getOrDefault (aEntry.getKey (), Integer.valueOf (0)).intValue ();
      if (nRoundTrip < nOrig && !aIsExpectedLoss.test (aEntry.getKey ()))
        ret.add (aEntry.getKey () + (nRoundTrip == 0 ? "" : " (" + nOrig + "x in, " + nRoundTrip + "x out)"));
    }
    return ret;
  }
}
