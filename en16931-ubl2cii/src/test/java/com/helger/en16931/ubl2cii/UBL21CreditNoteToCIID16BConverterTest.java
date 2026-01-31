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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Locale;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.state.ESuccess;
import com.helger.base.string.StringImplode;
import com.helger.cii.d16b.CIID16BCrossIndustryInvoiceTypeMarshaller;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.io.file.FilenameHelper;
import com.helger.io.resource.FileSystemResource;
import com.helger.phive.api.execute.ValidationExecutionManager;
import com.helger.phive.api.result.ValidationResult;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.api.validity.IValidityDeterminator;
import com.helger.phive.xml.source.ValidationSourceXML;
import com.helger.ubl21.UBL21Marshaller;

import oasis.names.specification.ubl.schema.xsd.creditnote_21.CreditNoteType;
import un.unece.uncefact.data.standard.crossindustryinvoice._100.CrossIndustryInvoiceType;

/**
 * Test class for class {@link UBL21CreditNoteToCIID16BConverter}.
 *
 * @author Philip Helger
 */
public final class UBL21CreditNoteToCIID16BConverterTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (UBL21CreditNoteToCIID16BConverterTest.class);

  @Test
  public void testConvertAndValidateAllInvoices ()
  {
    for (final File aFile : MockSettings.getAllTestFilesUBL21CreditNote ())
    {
      LOGGER.info ("Converting " + aFile.toString () + " to CII D16B");

      // Read as UBL
      final ErrorList aErrorList = new ErrorList ();
      final CreditNoteType aUBLCreditNote = UBL21Marshaller.creditNote ().setCollectErrors (aErrorList).read (aFile);
      assertTrue ("Errors: " + aErrorList.toString (), aErrorList.containsNoError ());
      assertNotNull (aUBLCreditNote);

      // Main conversion
      final CrossIndustryInvoiceType aCrossIndustryInvoice = UBL21CreditNoteToCIID16BConverter.convertToCrossIndustryInvoice (aUBLCreditNote,
                                                                                                                              aErrorList);
      assertTrue ("Errors: " + aErrorList.toString (), aErrorList.containsNoError ());
      assertNotNull (aCrossIndustryInvoice);

      // Save converted file
      final File aDestFile = new File ("generated/cii/cn-" +
                                       FilenameHelper.getBaseName (aFile.getName ()) +
                                       "-cii.xml");
      final ESuccess eSuccess = new CIID16BCrossIndustryInvoiceTypeMarshaller ().setFormattedOutput (true)
                                                                                .setCollectErrors (aErrorList)
                                                                                .write (aCrossIndustryInvoice,
                                                                                        aDestFile);
      if (aErrorList.containsAtLeastOneError ())
        LOGGER.error (new CIID16BCrossIndustryInvoiceTypeMarshaller ().setFormattedOutput (true)
                                                                      .setUseSchema (false)
                                                                      .setCollectErrors (aErrorList)
                                                                      .getAsString (aCrossIndustryInvoice));
      assertTrue ("Errors: " + aErrorList.toString (), aErrorList.containsNoError ());
      assertTrue (eSuccess.isSuccess ());

      // Validate against EN16931 validation rules
      final ValidationResultList aResultList = ValidationExecutionManager.executeValidation (IValidityDeterminator.createDefault (),
                                                                                             MockSettings.VES_REGISTRY.getOfID (MockSettings.VID_CII_2017),
                                                                                             ValidationSourceXML.create (new FileSystemResource (aDestFile)));

      // Check that no errors (but maybe warnings) are contained
      for (final ValidationResult aResult : aResultList)
      {
        if (aResult.getErrorList ().isNotEmpty ())
        {
          // Log Invoice
          LOGGER.error (new CIID16BCrossIndustryInvoiceTypeMarshaller ().setFormattedOutput (true)
                                                                        .getAsString (aCrossIndustryInvoice));

          // Log errors
          LOGGER.error (StringImplode.imploder ()
                                     .source (aResult.getErrorList (),
                                              x -> x.getErrorFieldName () + " - " + x.getErrorText (Locale.ROOT))
                                     .separator ('\n')
                                     .build ());
        }
        assertTrue ("Errors: " + aResult.getErrorList ().toString (), aResult.getErrorList ().isEmpty ());
      }
    }
  }
}
