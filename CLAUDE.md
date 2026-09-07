# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Unidirectional converter from UBL to CII, following the EN 16931 European e-invoicing standard.
Java 17+ library plus an optional CLI wrapper. Counterpart to
[en16931-cii2ubl](https://github.com/phax/en16931-cii2ubl).

Both editions of the standard are supported, each with exactly one syntax pair:

| Edition | Input | Output | Specification identifier (BT-24) |
|---------|-------|--------|----------------------------------|
| EN 16931:2017 | UBL 2.1 | CII D16B | `urn:cen.eu:en16931:2017` |
| EN 16931:2026 | UBL 2.5 | CII D25A | `urn:cen.eu:en16931:2026` |

## Build Commands

```bash
mvn clean install                # Full build with tests
mvn clean test                   # Run all tests

# Single test class
mvn test -pl en16931-ubl2cii -Dtest=UBL25ToCIID25AConverterTest

# Single test method
mvn test -pl en16931-ubl2cii -Dtest=UBL25ToCIID25AConverterTest#testFullInvoiceCarriedOverTerms

# Build the CLI fat JAR
mvn clean package -pl en16931-ubl2cii-cli
java -jar en16931-ubl2cii-cli/target/en16931-ubl2cii-cli-full.jar -t ./output invoice.xml
```

## Modules

- **en16931-ubl2cii** — core conversion library
- **en16931-ubl2cii-cli** — picocli command line tool (shade plugin produces the fat JAR)

## Architecture

```
com.helger.en16931.basics                      from en16931-basics, not from this repo
  EEN16931Edition                              EN2017 / EN2026 + BT-24 detection for UBL and CII
  EEN16931SyntaxKind                           UBL Invoice / UBL Credit Note / CII
  EEN16931DateFormatCode                       UNTDID 2379 date formats of the CII binding
  ConversionHelper                             ifNotNull, ifNotEmpty
  codelist.EN16931CodeLists                    BT-3, BT-8, BT-17/BT-18, BT-31 and the BG-24 code 916

com.helger.en16931.ubl2cii                     edition independent
  AbstractToCIIConverterBase                   the three members that reference neither a UBL nor a
                                               CII type: ifNotNull/ifNotEmpty and the date formatting
  UBLToCIIDispatcher                           detect document type + edition -> route -> writeCII
  UBLToCIIConversionHelper                     stream based convenience facade for both editions
  UBLToCIIVersion                              version constants from properties

com.helger.en16931.ubl2cii.en2017              UBL 2.1 -> CII D16B
  AbstractToCIID16BConverter
  UBL21InvoiceToCIID16BConverter
  UBL21CreditNoteToCIID16BConverter

com.helger.en16931.ubl2cii.en2026              UBL 2.5 -> CII D25A
  AbstractToCIID25AConverter
  UBL25InvoiceToCIID25AConverter
  UBL25CreditNoteToCIID25AConverter
```

The converters are **not auto-generated** — they are manually maintained. The two editions cannot
share code, because both JAXB model pairs are unrelated Java classes with identical names: CII D25A
lives in `un.unece.uncefact.data.standard.cii.d25a.*` and D16B in
`un.unece.uncefact.data.standard.crossindustryinvoice._100`, UBL 2.5 in `…xsd.*_25` and UBL 2.1 in
`…xsd.*_21`. **Generics cannot bridge this.** What *can* be shared is logic that takes already
extracted values — see `convertSpecifiedFinancialAdjustment`, which takes an amount and a string
rather than a line.

The converters are **static**, unlike cii2ubl's: this project has no conversion settings at all.

`ifNotNull` and `ifNotEmpty` stay as `protected static` members of `AbstractToCIIConverterBase` and
only forward to `ConversionHelper` — the mapping code calls them several hundred times and the
unqualified call is what keeps it readable.

### Conversion flow

1. Determine the document type from the document element and the edition from BT-24
   (`UBLToCIIDispatcher`)
2. Parse the UBL XML with `UBL21Marshaller` respectively `UBL25Marshaller`
3. Map UBL elements to CII field by field, using the EN 16931 Business Term codes; comments in the
   converters reference them
4. Serialize with `CIID16BCrossIndustryInvoiceTypeMarshaller` respectively
   `CIID25ACrossIndustryInvoiceTypeMarshaller`

### Error handling

Converters take an `ErrorList`. Conversion is successful only if a non-null result is returned
**and** the error list contains no errors. The edition is never guessed — a missing or unknown BT-24
produces an error entry.

## Testing

JUnit 4. Test classes live in the package of the code they test.

- **EN 16931:2017** — `en2017.UBL21InvoiceToCIID16BConverterTest` and its credit note counterpart
  convert the UBL 2.1 samples in `src/test/resources/external/ubl21/` and validate the CII output
  against the EN 16931 Schematron via phive-rules.
- **EN 16931:2026** — `en2026.UBL25ToCIID25AConverterTest` uses the UBL 2.5 corpus in
  `src/test/resources/external/ubl25/`, because **no Schematron exists for 2026 yet**. Correctness
  rests on XSD validity of both sides plus one XPath assertion per business term, with the
  expressions taken from the mapping table. `MockD25ASettings` provides `assertXPath` /
  `assertNoXPath` / `assertXPathCount`.
- `en2026.MappingCoverageTest` fails if any row of `docs/en16931-2026-syntax.md` is never named in
  the 2026 converters. It exists because the mapping document's own "New in 2026" table is not a
  complete diff — cii2ubl missed BT-122-1 that way.
- **Round trips, in both directions.** `en2017.CIIRoundTripTest` and `en2026.CIID25ARoundTripTest`
  start at CII; `en2017.UBL21RoundTripTest` and `en2026.UBL25RoundTripTest` start at UBL, which is
  the direction this library converts. Only the latter two can see a business term that *this*
  library drops — in the CII → UBL → CII direction the value is already gone from the UBL input we
  receive. All four use `MockRoundTrip`, which reduces a document to the multiset of its leaf
  values — every childless element plus every attribute — so the comparison ignores element order
  but still catches a container written once instead of three times. Each test has an
  `EXPECTED_LOSSES` set naming every value a round trip cannot preserve *and why*; anything else
  fails. Both UBL round trips are lossless today apart from `cbc:NetworkID`.
- Converted output is written to `en16931-ubl2cii/generated/cii/` and `generated/cii-d25a/`, both
  **tracked in git**. `git status` on those folders is the regression check — it is how the 2017
  path was proven unchanged through the restructuring.
  `generated/roundtrip/` holds mismatch diagnostics only and is gitignored.

### The test corpus

The UBL 2.5 files in `ubl25/inv/` and `ubl25/cn/` are the output of en16931-cii2ubl for its 16 hand
written CII D25A instances, and those originals are kept in `src/test/resources/external/cii-d25a/`
as the round-trip reference.

Being cii2ubl's output, that corpus contains only what cii2ubl emits — it does **not** cover all
284 mapping rows, contrary to what one might assume. The same holds for the UBL 2.1 Peppol samples,
which never exercised 17 of the 180 rows of the 2017 mapping. The `coverage` files close both gaps
and are the only hand written UBL documents here:

| File | Business terms it exists for |
|------|------------------------------|
| `ubl21/inv/coverage/en16931-coverage-invoice.xml` | BT-8, BT-15, BT-16, BT-17, BT-26, BT-87, BT-88, BT-114, BT-125 with BT-125-1 and BT-125-2, BT-128 with BT-128-1, BT-156, BT-158-2 |
| `ubl21/inv/coverage/en16931-coverage-directdebit-invoice.xml` | BT-89, BT-90, BT-91 |
| `ubl25/inv/d25a-coverage-invoice-ubl.xml` | BT-8, BT-89, BT-90, BT-91, BT-114, BG-24 with BT-122 to BT-125 |
| `ubl25/inv/d25a-coverage-card-invoice-ubl.xml` | BT-87, BT-88 |

The payment terms are split across files because CII-SR-467 forbids more than one distinct payment
means type code per document, and because BT-90 only survives a round trip inside BG-19 — see the
traps below.

What no round trip can reach is the fixed-value discriminator rows — BT-122-1 (`916`), and the
`@schemeID='VA'` respectively `cac:TaxScheme/cbc:ID='VAT'` alternatives of BT-31, BT-32, BT-48 and
BT-63. cii2ubl emits only one of each pair, so the other form would always be reported as a loss.
`MappingCoverageTest` is what guards those.

## Key Dependencies

- **en16931-basics** — everything that changes when the *standard* changes. Do not add a local copy
  of any of it; put it there instead. Sibling checkout: `../en16931-basics`
- **ph-commons** — Helger utilities, error handling, collection types
- **ph-ubl** — UBL 2.1 and 2.5 JAXB models and marshalling
- **ph-cii** — CII D16B and D25A JAXB models and marshalling
- **phive-rules-en16931** — EN 16931 validation rules (test scope only)
- **en16931-cii2ubl** — test scope only, for the two round-trip tests

## Field Mapping Reference

- `docs/en16931-2026-syntax.md` — **the source of truth for EN 16931:2026** (UBL 2.5 / CII D25A)
- `docs/en16931-2017-syntax.md` — the same for EN 16931:2017 (UBL 2.1 / CII D16B)
- `docs/plan-3.0.0.md` — the 3.0.0 implementation plan, including the findings and traps met along
  the way
- Both mapping documents carry a "Findings in the Source Documents" section. Two defects are
  recorded there and corrected in the tables: BT-218 (a date mapped to a time element) and BT-186 /
  BT-186-1 (line level terms mapped to a header element).

## Traps

- **The CII D25A schema declares defaults on `@listID` and `@listAgencyID`** of
  `qdt:AllowanceChargeReasonCodeType`. Only the fixed `5153` of BT-177-1 / BT-193-1 may ever be
  written, because that attribute is the sole discriminator between BT-105/BT-145 and BT-177/BT-193.
- **BT-2 and BT-166 are one CII element.** Writing the issue time means switching the UNTDID 2379
  format code from `102` to `208`.
- **UBL gives no discriminator for BG-33 / BG-35 / BG-36** — all three share `cac:PaymentTerms`.
  They are told apart by the elements they use, and one CII container is emitted per UBL one.
- **`cac:PaymentTerms` is 0..n since 2026**, so anything 0..1 inside it — BT-9 in particular —
  belongs on the first one only.
- **`cac:PartyIdentification/cbc:ID` is BT-29/BT-46/BT-60 *and* BT-90**, told apart only by
  `@schemeID="SEPA"` (`AbstractToCIIConverterBase.BT_90_SCHEME_ID`). BT-90 has a dedicated CII
  element, so it must be skipped in the party identifier loop — writing it as `ram:GlobalID` too
  violates BR-CL-10, because `SEPA` is not an ISO 6523 ICD code.
- **BT-90 lives inside BG-19 DIRECT DEBIT.** en16931-cii2ubl maps `ram:CreditorReferenceID` back to
  UBL only when the payment means is a direct debit, which is what the EN 16931 model prescribes. A
  CII document carrying it next to a credit transfer therefore loses it — that is the data being
  wrong, not the converter.
