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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.en16931.basics.ConversionHelper;
import com.helger.en16931.basics.codelist.EN16931CodeLists;
import com.helger.en16931.basics.EEN16931DateFormatCode;

/**
 * Abstract base class of all UBL to CII converters, holding everything that is independent of the
 * EN 16931 edition. It deliberately references neither a UBL nor a CII type, because the JAXB
 * models of the two editions are unrelated Java classes with identical names.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public abstract class AbstractToCIIConverterBase
{
  /**
   * The UNTDID 2379 date format used for all CII date elements. Both editions default to it; only
   * BT-166 of the 2026 edition uses {@link #CII_DATE_TIME_FORMAT} instead.
   */
  protected static final EEN16931DateFormatCode CII_DATE_FORMAT = EEN16931DateFormatCode.CCYYMMDD;

  /**
   * The UNTDID 2379 date <em>and time</em> format. It is used for BT-2 exactly when BT-166 is
   * present, because CII represents the two with a single element - see BT-2-1 and BT-166-1.
   */
  protected static final EEN16931DateFormatCode CII_DATE_TIME_FORMAT = EEN16931DateFormatCode.CCYYMMDDHHMMSSZHHMM;

  private static final DateTimeFormatter CII_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern (CII_DATE_TIME_FORMAT.getJavaPattern ());

  protected static <T> boolean ifNotNull (@Nullable final T aObj, @NonNull final Consumer <? super T> aConsumer)
  {
    return ConversionHelper.ifNotNull (aObj, aConsumer);
  }

  protected static boolean ifNotEmpty (@Nullable final String s, @NonNull final Consumer <? super String> aConsumer)
  {
    return ConversionHelper.ifNotEmpty (s, aConsumer);
  }

  /**
   * Drop the placeholder that a syntax binding prescribes where an element is mandatory but the
   * business term behind it is absent - see
   * {@link com.helger.en16931.basics.codelist.EN16931CodeLists#MISSING_VALUE_PLACEHOLDER}. UBL
   * needs it for BT-13 and for the line references BT-132, BT-190, BT-192 and BT-199, and it must
   * never be copied to CII, where those elements are genuinely optional.
   *
   * @param sValue
   *        The value read from the source document. May be <code>null</code>.
   * @return <code>null</code> if the value is the placeholder.
   */
  @Nullable
  protected static String withoutPlaceholder (@Nullable final String sValue)
  {
    return EN16931CodeLists.MISSING_VALUE_PLACEHOLDER.equals (sValue) ? null : sValue;
  }

  /**
   * Format a date the way the CII syntax expects it, in the UNTDID 2379 format
   * {@link #CII_DATE_FORMAT}.
   *
   * @param aLocalDate
   *        The date to format. May be <code>null</code>.
   * @return <code>null</code> if the provided date is <code>null</code>.
   */
  @Nullable
  protected static String createFormattedDateValue (@Nullable final LocalDate aLocalDate)
  {
    return CII_DATE_FORMAT.getAsString (aLocalDate);
  }

  /**
   * Format a date and time the way the CII syntax expects it, in the UNTDID 2379 format
   * {@link #CII_DATE_TIME_FORMAT}. The result carries the UTC offset, as in
   * <code>20250115120503+0100</code>.
   *
   * @param aDateTime
   *        The date and time to format. May be <code>null</code>.
   * @return <code>null</code> if the provided date and time is <code>null</code>.
   */
  @Nullable
  protected static String createFormattedDateTimeValue (@Nullable final OffsetDateTime aDateTime)
  {
    return aDateTime == null ? null : CII_DATE_TIME_FORMATTER.format (aDateTime);
  }
}
