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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.collection.commons.CommonsTreeSet;
import com.helger.collection.commons.ICommonsOrderedSet;
import com.helger.collection.commons.ICommonsSortedSet;
import com.helger.io.file.SimpleFileIO;

/**
 * Ensures that every business term of the EN 16931:2026 mapping table is accounted for in the
 * <code>.en2026</code> converters.<br>
 * The check is deliberately crude - it only verifies that each identifier is named somewhere in the
 * converter sources. That is enough to catch a whole business term being forgotten, which is
 * exactly what happened to cii2ubl with BT-122-1: it is not listed in the "Business Terms and
 * Groups New in 2026" table of the mapping document, so it slipped through a section by section
 * implementation.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public final class MappingCoverageTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MappingCoverageTest.class);

  private static final File MAPPING_FILE = new File ("../docs/en16931-2026-syntax.md");
  private static final String CONVERTER_DIR = "src/main/java/com/helger/en16931/ubl2cii/en2026/";
  private static final String [] CONVERTER_FILES = { "AbstractToCIID25AConverter.java",
                                                     "UBL25InvoiceToCIID25AConverter.java",
                                                     "UBL25CreditNoteToCIID25AConverter.java" };

  /**
   * Business terms that exist only in UBL and have no CII counterpart at all - the mapping table
   * shows "&mdash;" in the CII column. There is nothing to write to the CII side for them.<br>
   * Note that this is the mirror image of the exclusion list in cii2ubl, which skips the 16 <em>CII
   * only</em> rows. Those 16 are all relevant here, because this converter has to <em>produce</em>
   * them - they are the UNTDID 2379 <code>@format</code> codes, BT-11-1 (Project name) and BT-17-1
   * (Tender or lot reference type code).
   */
  private static final Set <String> UBL_ONLY = new CommonsTreeSet <> (new CommonsArrayList <> (
                                                                                               // The
                                                                                               // fixed
                                                                                               // cac:TaxScheme/cbc:ID
                                                                                               // values;
                                                                                               // CII
                                                                                               // uses
                                                                                               // ram:ID/@schemeID
                                                                                               // instead
                                                                                               "BT-31-2",
                                                                                               "BT-32-2",
                                                                                               "BT-48-2",
                                                                                               "BT-63-2",
                                                                                               // UBL
                                                                                               // only
                                                                                               // bookkeeping
                                                                                               "BT-116-1",
                                                                                               "BT-122-1-1",
                                                                                               "BT-148-1",
                                                                                               "BT-179-1"));

  private static ICommonsOrderedSet <String> _getAllMappingRows ()
  {
    final String sMapping = SimpleFileIO.getFileAsString (MAPPING_FILE, StandardCharsets.UTF_8);
    assertTrue ("Failed to read " + MAPPING_FILE.getAbsolutePath (), StringHelper.isNotEmpty (sMapping));

    // Only real mapping rows have a cardinality column like "1..1" or "0..n"
    final ICommonsOrderedSet <String> ret = new CommonsLinkedHashSet <> ();
    final Matcher aMatcher = Pattern.compile ("^\\|\\s*(B[TG]-[0-9a-z\\-]+)\\s*\\|[^|]*\\|\\s*[0-9]\\.\\.[0-9n]\\s*\\|",
                                              Pattern.MULTILINE).matcher (sMapping);
    while (aMatcher.find ())
      ret.add (aMatcher.group (1));
    return ret;
  }

  @Test
  public void testEveryMappingRowIsAccountedFor ()
  {
    final ICommonsOrderedSet <String> aAllRows = _getAllMappingRows ();
    assertTrue ("Suspiciously few mapping rows found: " + aAllRows.size (), aAllRows.size () > 250);

    final StringBuilder aSB = new StringBuilder ();
    for (final String sFilename : CONVERTER_FILES)
    {
      final File aFile = new File (CONVERTER_DIR, sFilename);
      final String s = SimpleFileIO.getFileAsString (aFile, StandardCharsets.UTF_8);
      assertTrue ("Failed to read " + aFile.getAbsolutePath (), StringHelper.isNotEmpty (s));
      aSB.append (s);
    }
    final String sConverters = aSB.toString ();

    final ICommonsSortedSet <String> aMissing = new CommonsTreeSet <> ();
    for (final String sID : aAllRows)
      if (!UBL_ONLY.contains (sID))
      {
        // Match the identifier only when it is not the prefix of a longer one, so that "BT-12"
        // is not satisfied by an occurrence of "BT-122"
        if (!Pattern.compile (Pattern.quote (sID) + "(?![0-9\\-])").matcher (sConverters).find ())
          aMissing.add (sID);
      }

    LOGGER.info ("EN 16931:2026 mapping coverage: " +
                 (aAllRows.size () - aMissing.size ()) +
                 " of " +
                 aAllRows.size () +
                 " rows accounted for (" +
                 UBL_ONLY.size () +
                 " of them are UBL only)");

    if (aMissing.isNotEmpty ())
      fail ("The following " +
            aMissing.size () +
            " business terms of the EN 16931:2026 mapping table are not named anywhere in the " +
            ".en2026 converters. Either implement them, or add them to UBL_ONLY if they genuinely " +
            "have no CII counterpart: " +
            aMissing);
  }
}
