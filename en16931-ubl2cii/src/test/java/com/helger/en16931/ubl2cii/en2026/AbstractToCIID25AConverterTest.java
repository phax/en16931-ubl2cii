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

import java.math.BigDecimal;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;

import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_25.TaxAmountType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeSettlementHeaderMonetarySummationType;

/**
 * Test class for class {@link AbstractToCIID25AConverter}.
 */
public final class AbstractToCIID25AConverterTest
{
  @NonNull
  private static TaxAmountType _taxAmount (@NonNull final String sValue, @NonNull final String sCurrencyID)
  {
    final TaxAmountType ret = new TaxAmountType ();
    ret.setValue (new BigDecimal (sValue));
    ret.setCurrencyID (sCurrencyID);
    return ret;
  }

  @Test
  public void testTaxTotalAmountOncePerCurrency ()
  {
    // UBL BR-CO-15 forbids a second cac:TaxTotal in BT-5, but if one is there, only the first may
    // become a ram:TaxTotalAmount
    final ICommonsList <TaxAmountType> aAmounts = new CommonsArrayList <> (_taxAmount ("19", "EUR"),
                                                                       _taxAmount ("19", "EUR"),
                                                                       _taxAmount ("17", "GBP"));
    final TradeSettlementHeaderMonetarySummationType aSum = AbstractToCIID25AConverter.createSpecifiedTradeSettlementHeaderMonetarySummation (null,
                                                                                                                                              aAmounts,
                                                                                                                                              "GBP");
    assertEquals (2, aSum.getTaxTotalAmountCount ());
    assertEquals ("EUR", aSum.getTaxTotalAmountAtIndex (0).getCurrencyID ());
    assertEquals ("GBP", aSum.getTaxTotalAmountAtIndex (1).getCurrencyID ());
  }
}
