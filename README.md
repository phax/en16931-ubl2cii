# en16931-ubl2cii

<!-- ph-badge-start -->
[![Sonatype Central](https://maven-badges.sml.io/sonatype-central/com.helger/en16931-ubl2cii-parent-pom/badge.svg)](https://maven-badges.sml.io/sonatype-central/com.helger/en16931-ubl2cii-parent-pom/)
[![javadoc](https://javadoc.io/badge2/com.helger/en16931-ubl2cii/javadoc.svg)](https://javadoc.io/doc/com.helger/en16931-ubl2cii)

> If this project saved you some time or made your day a little easier, a star would mean a lot — it helps others find it too.
<!-- ph-badge-end -->

Converter library for EN 16931 invoices and credit notes from UBL to CII.

Both editions of the standard are supported, each with exactly one syntax pair:

| Edition | Input | Output | Specification identifier (BT-24) |
|---------|-------|--------|----------------------------------|
| EN 16931:2017 | UBL 2.1 | CII D16B | `urn:cen.eu:en16931:2017` |
| EN 16931:2026 | UBL 2.5 | CII D25A | `urn:cen.eu:en16931:2026` |

This is a Java 17+ library that converts a Universal Business Language (UBL) document into a Cross
Industry Invoice (CII) document following the rules of the European Norm (EN) 16931, which defines a
common semantic data model for electronic invoices in Europe. Invoices and Credit Notes are both
supported, and a command line client is included.

This is the counterpart to https://github.com/phax/en16931-cii2ubl, which converts in the other
direction. The facts about the standard that both projects need - the editions, the code lists and
the BT-24 based edition detection - live in https://github.com/phax/en16931-basics.

This version is based on the version of @VartikaG02: https://github.com/VartikaG02/en16931-ubl2cii - it was extended by adding CII conformance to the EN.

This library is licensed under the Apache License Version 2.0.

# Usage

## As a library

`UBLToCIIDispatcher` is the entry point when the edition is not known in advance. It determines the
document type from the document element and the EN 16931 edition from BT-24, and returns either a
CII D16B or a CII D25A `CrossIndustryInvoiceType`:

```java
final ErrorList aErrorList = new ErrorList ();
final Document aUBLDoc = DOMReader.readXMLDOM (aSourceFile);
// null = detect the edition from BT-24; pass EEN16931Edition.EN2017 or EN2026 to force it
final Serializable aCII = UBLToCIIDispatcher.convertUBLtoCII (aUBLDoc, null, aErrorList);
if (aCII != null && aErrorList.containsNoError ())
  UBLToCIIDispatcher.writeCII (aCII, FileHelper.getOutputStream (aDestFile), aErrorList);
```

The edition is never guessed: a missing or unknown BT-24 adds an error to the `ErrorList` and
returns `null`.

When the edition *is* known, call the converter of that edition directly:

| Edition | Invoice | Credit Note |
|---------|---------|-------------|
| 2017 | `en2017.UBL21InvoiceToCIID16BConverter` | `en2017.UBL21CreditNoteToCIID16BConverter` |
| 2026 | `en2026.UBL25InvoiceToCIID25AConverter` | `en2026.UBL25CreditNoteToCIID25AConverter` |

`UBLToCIIConversionHelper` offers the same as stream based convenience methods.

Conversion is successful only if a non-`null` result is returned **and** the `ErrorList` contains no
error.

## Command line

```bash
java -jar en16931-ubl2cii-cli-full.jar -t ./output invoice.xml
```

The EN 16931 edition is taken from BT-24 of each source file. Use `--en-version 2017` or
`--en-version 2026` to force it, which is what documents with a non-conformant BT-24 need.

# News and noteworthy

v3.0.0 - 2026-09-07
* Added support for **EN 16931:2026** - UBL 2.5 to CII D25A - covering all 284 rows of the syntax mapping
* The converters moved into edition specific sub-packages `com.helger.en16931.ubl2cii.en2017` and `.en2026` - this is a breaking change for existing imports
* Added `UBLToCIIDispatcher`, which determines the document type from the document element and the EN 16931 edition from BT-24
* Added `--en-version 2017|2026` to the command line client; without it the edition is detected per file
* Now using [en16931-basics](https://github.com/phax/en16931-basics) for the code lists, the UNTDID 2379 date formats and the BT-24 based edition detection
* Fixed BT-9 (Payment due date) being written into every `cac:PaymentTerms` instead of only the first one, which became visible when `cac:PaymentTerms` turned 0..n in the 2026 binding
* Fixed BT-90 (Bank assigned creditor identifier) also being written as a party identifier BT-29/BT-46/BT-60. The two share the UBL element `cac:PartyIdentification/cbc:ID` and are told apart by `@schemeID="SEPA"` only, so the resulting `ram:GlobalID schemeID="SEPA"` violated BR-CL-10. Affects both editions
* Added round trip tests in the UBL to CII to UBL direction for both editions - `en2017.UBL21RoundTripTest` and `en2026.UBL25RoundTripTest` - which is the direction that can detect a business term dropped by this library
* Added hand written coverage documents to the test corpus for the 17 business terms of the 2017 mapping and the 8 of the 2026 mapping that no sample document contained
* Now using `EEN16931TaxSchemeCode.LOC` and the new discriminator constants of [en16931-basics](https://github.com/phax/en16931-basics) 1.0.1 instead of local copies. BT-32-1 falls out of `EN16931CodeLists.mapTaxSchemeCodeUBLToCII` now, so the local `NATIONAL_TAX_SCHEME`, `NATIONAL_TAX_SCHEME_CII` and `NON_VAT_TAX_CODE_LIST_ID` constants are gone
* Requires en16931-cii2ubl 4.0.1 for the tests, which contains the mirror image of the BT-90 fix
* Fixed BT-11-1 (Project name) being written as the hard coded string `Project reference`. The binding says to repeat the BT-11 value there, because CII makes the name mandatory as soon as `ram:SpecifiedProcuringProject` exists and UBL has no counterpart for it. Affects both editions
* BT-13 (Purchase order reference) is no longer read from a `cac:OrderReference/cbc:ID` that carries the placeholder. `cbc:ID` is mandatory there, so a document with only BT-14 has no BT-13 value to read. Affects both editions
* The placeholder for a line reference that the UBL binding requires but the document does not have is now `EN16931CodeLists.MISSING_VALUE_PLACEHOLDER` of en16931-basics, and it is recognised on BT-188 and BT-200 as well as on the line identifiers. en16931-cii2ubl 4.0.1 writes the same `None`, so a BT-190, BT-192 or BT-199 that the source CII never had is no longer invented by a round trip

v2.2.0 - 2026-04-22
* Added mapping of BT-23 (Business process type) for Invoice and CreditNote converters. See [#1](https://github.com/phax/en16931-ubl2cii/pull/1) - thx @Loulouw
* Fixed BT-27/BT-44 (Party name) mapping: `RegistrationName` now correctly maps to `TradeParty/Name` instead of `TradingBusinessName`
* Fixed BT-28/BT-45 (Trading name) mapping: `PartyName/Name` now correctly maps to `SpecifiedLegalOrganization/TradingBusinessName` instead of `TradeParty/Name`
* Fixed empty `SpecifiedLegalOrganization` and `BuyerOrderReferencedDocument` elements being emitted when no data is present
* Added mapping of BT-14 (Sales order reference)
* Added mapping of BT-15 (Receiving advice reference)
* Added mapping of BT-16 (Despatch advice reference)
* Added mapping of BT-17 (Tender or lot reference) with correct TypeCode="50"
* Added mapping of BT-18-1/BT-128-1 (Invoiced object identifier scheme ID) → `ReferenceTypeCode`
* Added mapping of BT-33 (Seller additional legal information): `CompanyLegalForm` → `TradeParty/Description`
* Added mapping of BT-70 (Deliver to party name)
* Added mapping of BT-82 (Payment means text): `PaymentMeansCode/@Name` → `Information`
* Added mapping of BT-85 (Payment account name)
* Added mapping of BT-86 (Payment service provider identifier / BIC)
* Added mapping of BT-156 (Item Buyer's identifier)
* Added mapping of BT-159 (Item country of origin)
* Added mapping of BG-3 (Preceding invoice reference): BT-25/BT-26
* Added mapping of BG-6/BG-9 (Seller/Buyer contact): BT-41/BT-56 (contact point), BT-42/BT-57 (telephone), BT-43/BT-58 (email)
* Added mapping of BG-11/BG-12 (Seller tax representative party and postal address): BT-62/BT-63/BT-64..BT-69
* Added mapping of BG-18 (Payment card information): BT-87/BT-88
* Added mapping of BG-19 (Direct debit): BT-89/BT-90/BT-91
* Added mapping of BG-26 (Invoice line period): BT-134/BT-135
* Added mapping of BG-27/BG-28 (Invoice line allowances and charges): BT-136..BT-145
* Added mapping of BT-147/BT-148 (Item price discount / gross price)
* Added mapping of BT-149/BT-150 (Item price base quantity and unit of measure)
* Added round-trip test suite (CII → UBL → CII) using en16931-cii2ubl as test dependency
* No more OSGI bundle packaging

v2.1.0 - 2025-11-16
* Updated to ph-commons 12.1.0
* Using JSpecify annotations

v2.0.1 - 2025-09-22
* Added missing mapping of BT-125-2 (Attachment filename).

v2.0.0 - 2025-08-27
* Requires Java 17 as the minimum version
* Updated to ph-commons 12.0.0

v1.1.3 - 2025-07-20
* Copying all `PartyTaxScheme` elements to CII

v1.1.2 - 2025-05-31
* Only pushing to Maven Central

v1.1.1 - 2025-03-20
* Improved mapping for BT-6, BT-7, BT-8, BT-9, BT-10, BT-11 and BT-111

v1.1.0 - 2025-02-22
* Added a simple command line client
* The created CII documents are now compliant to the EN 16931:2017 validation artefacts
* Added support to convert UBL 2.1 CreditNotes as well
* Initial fork from https://github.com/VartikaG02/en16931-ubl2cii

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.
