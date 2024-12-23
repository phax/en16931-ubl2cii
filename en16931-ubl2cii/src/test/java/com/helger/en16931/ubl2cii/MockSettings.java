/*
 * Copyright (C) 2024 Philip Helger
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

import java.io.File;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.file.FileSystemRecursiveIterator;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.phive.api.executorset.ValidationExecutorSetRegistry;
import com.helger.phive.en16931.EN16931Validation;
import com.helger.phive.xml.source.IValidationSourceXML;

final class MockSettings
{
  static final DVRCoordinate VID_CII_2017 = EN16931Validation.VID_CII_1313.getWithVersionLatestRelease ();

  static final ValidationExecutorSetRegistry <IValidationSourceXML> VES_REGISTRY = new ValidationExecutorSetRegistry <> ();
  static
  {
    EN16931Validation.initEN16931 (VES_REGISTRY);
  }

  @Nonnull
  @Nonempty
  @ReturnsMutableCopy
  public static ICommonsList <File> getAllTestFilesUBL21Invoice ()
  {
    final ICommonsList <File> ret = new CommonsArrayList <> ();
    for (final File f : new FileSystemRecursiveIterator (new File ("src/test/resources/external/ubl21/inv")))
      if (f.isFile () && f.getName ().endsWith (".xml"))
        ret.add (f);
    return ret;
  }
}
