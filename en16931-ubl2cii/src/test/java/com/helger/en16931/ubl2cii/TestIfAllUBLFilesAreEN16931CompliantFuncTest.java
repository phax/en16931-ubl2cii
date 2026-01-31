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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Locale;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.string.StringImplode;
import com.helger.io.resource.FileSystemResource;
import com.helger.phive.api.execute.ValidationExecutionManager;
import com.helger.phive.api.result.ValidationResult;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.api.validity.IValidityDeterminator;
import com.helger.phive.xml.source.ValidationSourceXML;

/**
 * Test if all UBL test files are EN 16931 compliant.
 *
 * @author Philip Helger
 */
public final class TestIfAllUBLFilesAreEN16931CompliantFuncTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (TestIfAllUBLFilesAreEN16931CompliantFuncTest.class);

  @Test
  @Ignore ("This test only needs to run if new test files are added")
  public void testIfAllUBLInvoicesAreENCompliant ()
  {
    for (final File aFile : MockSettings.getAllTestFilesUBL21Invoice ())
    {
      LOGGER.info ("Testing " + aFile.toString () + " UBL Invoice");

      // Validate against EN16931 validation rules
      final ValidationResultList aResultList = ValidationExecutionManager.executeValidation (IValidityDeterminator.createDefault (),
                                                                                             MockSettings.VES_REGISTRY.getOfID (MockSettings.VID_UBL_INV_2017),
                                                                                             ValidationSourceXML.create (new FileSystemResource (aFile)));

      // Check that no errors (but maybe warnings) are contained
      for (final ValidationResult aResult : aResultList)
      {
        if (!aResult.getErrorList ().isEmpty ())
          LOGGER.error (StringImplode.imploder ()
                                     .source (aResult.getErrorList (),
                                              x -> x.getErrorFieldName () + " - " + x.getErrorText (Locale.ROOT))
                                     .separator ('\n')
                                     .build ());
        assertTrue ("Errors: " + aResult.getErrorList ().toString (), aResult.getErrorList ().isEmpty ());
      }
    }
  }

  @Test
  @Ignore ("This test only needs to run if new test files are added")
  public void testIfAllUBLCreditNotesAreENCompliant ()
  {
    for (final File aFile : MockSettings.getAllTestFilesUBL21CreditNote ())
    {
      LOGGER.info ("Testing " + aFile.toString () + " UBL CreditNote");

      // Validate against EN16931 validation rules
      final ValidationResultList aResultList = ValidationExecutionManager.executeValidation (IValidityDeterminator.createDefault (),
                                                                                             MockSettings.VES_REGISTRY.getOfID (MockSettings.VID_UBL_CN_2017),
                                                                                             ValidationSourceXML.create (new FileSystemResource (aFile)));

      // Check that no errors (but maybe warnings) are contained
      for (final ValidationResult aResult : aResultList)
      {
        if (!aResult.getErrorList ().isEmpty ())
          LOGGER.error (StringImplode.imploder ()
                                     .source (aResult.getErrorList (),
                                              x -> x.getErrorFieldName () + " - " + x.getErrorText (Locale.ROOT))
                                     .separator ('\n')
                                     .build ());
        assertTrue ("Errors: " + aResult.getErrorList ().toString (), aResult.getErrorList ().isEmpty ());
      }
    }
  }
}
