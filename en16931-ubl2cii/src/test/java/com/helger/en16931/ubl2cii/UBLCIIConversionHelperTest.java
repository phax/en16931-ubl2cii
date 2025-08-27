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

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.helger.diagnostics.error.list.ErrorList;
import com.helger.io.file.FileHelper;

import un.unece.uncefact.data.standard.crossindustryinvoice._100.CrossIndustryInvoiceType;

/**
 * Test class for class {@link UBLToCIIConversionHelper}.
 *
 * @author Philip Helger
 */
public final class UBLCIIConversionHelperTest
{
  @Test
  public void testConvertAndValidateAllAutoDetect () throws IOException
  {
    for (final File aFile : MockSettings.getAllTestFilesUBL21Invoice ())
      try (InputStream aIS = FileHelper.getInputStream (aFile))
      {
        final ErrorList aErrorList = new ErrorList ();
        final CrossIndustryInvoiceType aCII = UBLToCIIConversionHelper.convertUBL21AutoDetectToCIID16B (aIS, aErrorList);
        assertNotNull (aCII);
      }

    for (final File aFile : MockSettings.getAllTestFilesUBL21CreditNote ())
      try (InputStream aIS = FileHelper.getInputStream (aFile))
      {
        final ErrorList aErrorList = new ErrorList ();
        final CrossIndustryInvoiceType aCII = UBLToCIIConversionHelper.convertUBL21AutoDetectToCIID16B (aIS, aErrorList);
        assertNotNull (aCII);
      }
  }
}
