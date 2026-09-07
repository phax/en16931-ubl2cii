# Plan: en16931-ubl2cii 3.0.0

Status: **A0-A13 done** - all 284 mapping rows implemented. A3 absorbed A5, and the coverage guard of A16 was pulled forward into phase 4 to serve as its worklist. · Created 2026-09-07 · Version: 3.0.0-SNAPSHOT · Branch: `master`

## 1. Goal

Add the **EN 16931:2026** syntax binding alongside the existing **EN 16931:2017** binding, and
adopt `en16931-basics` for everything that is a fact of the standard rather than a decision of this
converter.

| Edition | Input | Output | Spec identifier (BT-24) |
|---------|-------|--------|-------------------------|
| EN 16931:2017 | UBL **2.1** | CII **D16B** | `urn:cen.eu:en16931:2017` |
| EN 16931:2026 | UBL **2.5** | CII **D25A** | `urn:cen.eu:en16931:2026` |

No cross-edition combinations (no 2.1→D25A, no 2.5→D16B). This is the mirror image of
[en16931-cii2ubl 4.0.0](https://github.com/phax/en16931-cii2ubl), and this plan deliberately follows
`../en16931-cii2ubl/docs/plan-4.0.0.md` step for step, because the same 284 mapping rows are being
implemented in the opposite direction.

Mapping source: `docs/en16931-2026-syntax.md` — CEN/TS 16931-3-2:2026 clause 4.5 Table 10 (UBL
invoice) and clause 4.6 Table 12 (UBL credit note); CEN/TS 16931-3-3:2026 clause 4.5 Table 10 (CII).
Read **right to left** for this project.

## 2. How to use this document

Each action item is sized to fit one working session. Work them in order — later items depend on
earlier ones. At the end of a session:

1. Tick the item's checkbox and fill in its line in the [Progress log](#9-progress-log).
2. Run `mvn clean test`; it must be green before the item counts as done.
3. Commit. One commit per action item, message prefixed `[3.0.0 A<n>]`.

Do **not** re-verify section 4 — those checks were run once against the actual released artifacts.

## 3. Locked decisions

| # | Decision | Rationale |
|---|----------|-----------|
| D1 | Two strict bindings only (2.1→D16B, 2.5→D25A) | Each edition stays inside its own syntax pair, as the standard defines them |
| D2 | Sub-packages per edition (`.en2017` / `.en2026`) | Keeps the two editions from bleeding into each other; 3.0.0 is a major version so import breaks are acceptable |
| D3 | Test strategy: XSD validity **+ XPath assertions per BT** | No Schematron exists for 2026; XSD alone would not catch a BT written into the wrong element |
| D4 | The converters stay **static**, not instance based with a fluent API | Unlike cii2ubl this project has no conversion settings at all; adding an instance API to carry nothing would be ceremony |
| D5 | `UBLToCIIDispatcher` is added, `UBLToCIIConversionHelper` is kept | The dispatcher is the edition aware entry point mirroring `CIIToUBLDispatcher`; the helper stays the stream based convenience facade so existing embedders keep compiling after the package move |
| D6 | The 2017 path stays behaviour-identical to 2.2.x | Enforced by the newly git-tracked `en16931-ubl2cii/generated/` output (A0) |
| D7 | Test corpus for 2026 = cii2ubl's `generated/toubl25/` | 16 git-tracked UBL 2.5 files that cover all 284 rows, and their D25A originals give a round-trip reference |
| D8 | `docs/en16931-2017-syntax.md` and `docs/en16931-2026-syntax.md` are copied into this repo | The coverage guard must be reproducible without a sibling checkout |

## 4. Verified preconditions

Checked 2026-09-07 against the released artifacts. Do not repeat.

### 4.1 Dependencies are available

| Artifact | Version | Source |
|----------|---------|--------|
| `com.helger.cii:ph-cii-d25a` | 4.1.2 | `ph-cii-parent-pom` 4.1.2 — already imported by the parent pom |
| `com.helger.ubl:ph-ubl25` | 10.2.1 | `ph-ubl-parent-pom` 10.2.1 — already imported by the parent pom |
| `com.helger:en16931-basics` | 1.0.0 | released; must be added to `dependencyManagement` |
| `com.helger:en16931-cii2ubl` | 4.0.0 | already a **test** dependency of this project, and already on 4.0.0 |

Marshaller classes confirmed present: `com.helger.cii.d25a.CIID25ACrossIndustryInvoiceTypeMarshaller`
and `com.helger.ubl25.UBL25Marshaller`.

### 4.2 Edition detection: namespace URIs do NOT work

Established in cii2ubl 4.0.0 section 4.2 and unchanged here for the **CII output** side. On the
**UBL input** side the situation is the same: UBL 2.1 and UBL 2.5 declare byte-identical namespace
URIs (`urn:oasis:names:specification:ubl:schema:xsd:Invoice-2` and siblings — OASIS versions the
schema, not the namespace). The only reliable discriminator is **BT-24**
`/Invoice/cbc:CustomizationID`, mandatory 1..1 in BG-2, matched by **prefix** because
customizations append `#compliant#…`.

`en16931-basics` already implements exactly this for both syntaxes:
`SpecificationIdentifierReader` reads BT-24 from UBL *and* CII via SAX, and
`EEN16931Edition.detect (File|Node)` maps it to the edition. Nothing needs to be written here.

### 4.3 Current 2017 coverage is complete

All 182 row identifiers of `docs/en16931-2017-syntax.md` are named in the existing converters, so
the bulk-port approach of A5 starts from a complete 2017 implementation. (Measured by extracting
`B[TG]-<n>(-<n>)*` from the three converter sources and diffing against the document's row set.)

### 4.4 Scope of the change

| Metric | Value |
|--------|-------|
| Distinct row identifiers in `en16931-2026-syntax.md` | 287 |
| — carried over from 2017 | 182 |
| — new in 2026 | 105 (identifiers; 63 BTs + 7 BGs + their sub-identifiers) |
| Existing converter sources | 672 + 675 + 663 = 2010 lines |
| Mutual difference `UBL21InvoiceToCIID16BConverter` vs `UBL21CreditNoteToCIID16BConverter` | ~30 real lines; the rest is `Invoice`/`CreditNote` renaming |
| UBL 2.5 test corpus available | 16 files, 4112 lines |

### 4.5 The two JAXB model pairs are separate Java packages

| Release | Package |
|---------|---------|
| CII D16B | `un.unece.uncefact.data.standard.crossindustryinvoice._100` (+ `…reusableaggregatebusinessinformationentity._100`, `…unqualifieddatatype._100`, `…qualifieddatatype._100`) |
| CII D25A | `un.unece.uncefact.data.standard.cii.d25a` (+ `.rabie`, `.udt`, `.qdt`) |
| UBL 2.1 | `oasis.names.specification.ubl.schema.xsd.*_21` |
| UBL 2.5 | `oasis.names.specification.ubl.schema.xsd.*_25` |

Class names are identical on both axes, packages are not. **Generics cannot bridge this.** The 2026
converter is a port, not a refactor.

### 4.6 API deltas to expect

cii2ubl's section 4.4b enumerated 17 cardinality widenings when porting in the other direction.
Every one of them shows up here too, mirrored — on the UBL side as a *read* change and on the CII
side as a *write* change:

**UBL 2.1 → 2.5, now 0..n (reading):** `AddressType`'s `StreetName`, `AdditionalStreetName`,
`CityName`, `PostalZone`, `CountrySubentity`; `AddressLineType/Line`;
`PartyLegalEntityType/CompanyLegalForm`; `PaymentMeansType/CardAccount`; `ItemType/Name`.

**CII D16B → D25A, now 0..n (writing):** `NoteType/SubjectCode`, `TradeProductType/Description`,
`HeaderTradeSettlementType/InvoiceReferencedDocument`,
`TradeSettlementPaymentMeansType/PayeePartyCreditorFinancialAccount`.

The compiler enumerates them; A5 is where they are worked off.

### 4.7 The `@listID` trap of the CII D25A schema

Carried over verbatim from cii2ubl 4.4c, because it applies to the writing side as well:
`qdt:AllowanceChargeReasonCodeType` declares `default="4465_AllowanceChargeReasonCode"` on `@listID`
and `default="6"` on `@listAgencyID`. JAXB materialises those defaults on read. When *writing*, the
rule is the same in reverse: set `@listID`/`@listAgencyID` **only** for the fixed `5153` of
BT-177-1 / BT-193-1, never as a blanket copy, or the discriminator between BT-105 and BT-177 is
destroyed.

### 4.8 The six real 2017 → 2026 path changes

Established by cii2ubl A6 by diffing all 180 shared rows; 58 differed textually, 52 of those only in
base-path notation. The six real ones, stated for the UBL → CII direction:

| BT | 2017 (UBL 2.1 source) | 2026 (UBL 2.5 source) |
|----|------|------|
| BG-1 (BT-21, BT-22) | `cbc:Note` with BT-21 as a `#code#` prefix that has to be parsed out | `cac:Annotation/cbc:SubjectCode` + `cac:Annotation/cbc:AnnotationContent` — read directly, the prefix hack disappears |
| BT-10 / BT-10-1 | `cbc:BuyerReference` → `ram:BuyerReference` | `cac:BuyerAssignedReference/cbc:BuyerReference` (0..n) + `cbc:BuyerReferenceCode` → `ram:BuyerReferenceID` + `@schemeID` |
| BT-9 (CreditNote) | `cac:PaymentMeans/cbc:PaymentDueDate` | native `/CreditNote/cbc:DueDate` |
| BT-11 (CreditNote) | `cac:AdditionalDocumentReference/cbc:ID` | native `/CreditNote/cac:ProjectReference/cbc:ID` |
| BT-32-2 | `cac:TaxScheme/cbc:ID` = anything except `VAT` | fixed value `LOC`, and the CII side gets `@schemeID='FC'` (BT-32-1) |
| BT-127 | `cbc:Note` | unchanged — it must **not** follow BG-1 into `cac:Annotation` |

### 4.9 One defect in the source mapping table — already resolved

BT-218 "Line-level preceding invoice issue date" was mapped to
`cac:InvoiceDocumentReference/cbc:IssueTime` in the source, which is an `xs:time` and cannot hold a
date. Raised as finding 7 and corrected; the copy of `docs/en16931-2026-syntax.md` in this repo
already reads `cbc:IssueDate`. Read from `cbc:IssueDate` here as well.

## 5. Target structure

```
com.helger.en16931.ubl2cii                    shared — no UBL and no CII types
  AbstractToCIIConverterBase        NEW       the edition independent remainder of today's
                                              AbstractToCIID16BConverter, after the facts of the
                                              standard have moved to en16931-basics
  UBLToCIIDispatcher                NEW       detect edition (BT-24) + document kind -> route
  UBLToCIIConversionHelper                    kept; 2017 methods unchanged, 2026 counterparts added
  UBLToCIIVersion                             unchanged

com.helger.en16931.ubl2cii.en2017             UBL 2.1 -> CII D16B
  AbstractToCIID16BConverter                  moved, UBL-2.1/D16B-typed remainder
  UBL21InvoiceToCIID16BConverter              moved
  UBL21CreditNoteToCIID16BConverter           moved

com.helger.en16931.ubl2cii.en2026             UBL 2.5 -> CII D25A
  AbstractToCIID25AConverter        NEW       same shape, UBL-2.5/D25A-typed
  UBL25InvoiceToCIID25AConverter    NEW
  UBL25CreditNoteToCIID25AConverter NEW
```

Taken from `en16931-basics` instead of being kept locally:

| Today | Replacement |
|-------|-------------|
| `ifNotNull`, `ifNotEmpty` | `ConversionHelper` — kept as `protected static` forwarders, as in cii2ubl, because the mapping code calls them several hundred times |
| `mapDueDateTypeCodeToCII` | `EN16931CodeLists.mapDueDateTypeCodeUBLToCII` |
| `isOriginatorDocumentReferenceTypeCode`, `isValidDocumentReferenceTypeCode` | `EN16931CodeLists` |
| `_getAsVAIfNecessary` | `EN16931CodeLists.mapTaxSchemeCodeUBLToCII` |
| the literal `"916"` | `EN16931CodeLists.DOCUMENT_TYPE_CODE_SUPPORTING_DOCUMENT` |
| the literal `"102"` and `DateTimeFormatter.ofPattern ("yyyyMMdd")` | `EEN16931DateFormatCode.CCYYMMDD` |
| the literal `"50"` of BT-17 | `EN16931CodeLists.DOCUMENT_TYPE_CODE_ORIGINATOR_DOCUMENT` |

## 6. Action items

### Phase 1 — Prepare and restructure

- [x] **A0 — Establish the regression baseline** · ~30 min
  - Remove `generated/` from `.gitignore`, run `mvn clean test`, and commit
    `en16931-ubl2cii/generated/` as it stands today.
  - **Done when:** `git status --short en16931-ubl2cii/generated/` is empty after a full test run.
  - **Why first:** `git diff` on that folder is the proof that A1–A2 do not change 2017 behaviour (D6).

- [x] **A1 — Bump to 3.0.0-SNAPSHOT and adopt `en16931-basics`** · ~2 h
  - `3.0.0-SNAPSHOT` in the parent pom and both module poms; add `en16931-basics` 1.0.0 to
    `dependencyManagement` and to the core module.
  - Replace the seven items of the table in section 5.
  - **Done when:** `mvn clean test` green, `generated/` byte-identical.
  - **Note:** pure substitution. If any *behaviour* changes here, it is a bug — with one expected
    exception to watch for: `mapDueDateTypeCodeUBLToCII` must produce the same three pairs
    (`3→5`, `35→29`, `432→72`) as the deleted local method.

- [x] **A2 — Split the base class and introduce `.en2017`** · ~3 h
  - Create `AbstractToCIIConverterBase` with the edition-independent remainder.
  - Move the rest to `en2017.AbstractToCIID16BConverter`.
  - Move `UBL21InvoiceToCIID16BConverter` and `UBL21CreditNoteToCIID16BConverter` to `.en2017`.
  - Fix the imports in `UBLToCIIConversionHelper`, the CLI and the tests.
  - **Done when:** `mvn clean test` green, `generated/` byte-identical.
  - **Note:** pure move. Count the members in and out; none may be lost or duplicated.

### Phase 2 — 2026 skeleton and test harness

- [x] **A3 — `.en2026` scaffolding + full bulk port** · done · **absorbed A5**
  - Add `ph-cii-d25a` and `ph-ubl25` to the core module pom.
  - Bulk-port all three sources to `.en2026` by package swap and let the compiler enumerate the API
    deltas of section 4.6.
  - **Changed from the original plan:** the plan had A3 hand-write a five-BT skeleton and A5 do the
    bulk port later. cii2ubl recorded that its equivalent skeleton shipped a wrong BT-27 mapping
    that A5 then had to correct, so the skeleton is not just throwaway work, it is a source of
    bugs. Porting everything at once and validating it with the A4 harness is strictly better.

- [x] **A4 — Test corpus + harness** · ~4 h
  - Copy `../en16931-cii2ubl/en16931-cii2ubl/generated/toubl25/*.xml` into
    `src/test/resources/external/ubl25/inv/` and `.../cn/`, and
    `../en16931-cii2ubl/en16931-cii2ubl/src/test/resources/external/cii-d25a/*.xml` into
    `src/test/resources/external/cii-d25a/` (the round-trip reference).
  - `MockD25ASettings` with `assertXPath` / `assertNoXPath` / `assertXPathCount` over the CII
    namespace context, XPaths relative to the document element so they can be pasted from the
    mapping table.
  - Harness: read UBL 2.5 → XSD-validate the input → convert → XSD-validate the CII D25A output via
    the marshaller → run the XPath assertions.
  - **Done when:** both minimal files convert and assert green, and a deliberately wrong expected
    value makes the assertion fail (negative probe).

### Phase 3 — Port the 182 carried-over rows

- [x] **A5 — Bulk port both concrete converters** · **done as part of A3**
  - **Why not hand-port:** the 2017 converters carry accumulated fixes (BT-27/BT-28 party names,
    BT-110/BT-111 zero suppression, BT-149/BT-150 gross vs net base quantity, the BT-9 fallback);
    a hand-port risks reintroducing every one of them.

- [x] **A6 — Apply the six real 2026 path changes** · ~3 h
  - The table of section 4.8, all six.
  - **Done when:** each of the six is covered by an XPath assertion on a header test file.

- [x] **A7 — Carried-over rows: XPath assertions** · ~4 h
  - Assert the 182 carried-over rows against `d25a-full-invoice-ubl.xml` and
    `d25a-full-creditnote-ubl.xml`.
  - Includes the credit-note renames: `cac:CreditNoteLine`, `cbc:CreditedQuantity`,
    `cbc:CreditNoteTypeCode`, `cac:CollectionCreditNoteLine`.

### Phase 4 — The 105 new identifiers

Same slicing as cii2ubl A8–A13, so the two projects can be reviewed against each other.

- [x] **A8 — New header BTs** · 9 rows
  - BT-166 / BT-166-1 (`ram:IssueDateTime` with `@format='208'`), BT-167 / 167-1 / 167-2
    (`ram:InvoiceApplicableTradeCurrencyExchange`), BT-197
    (`ram:DeliveryNoteReferencedDocument`), BT-202 (BG-3 preceding invoice type code),
    BT-215 / BT-216 (BG-19 debited account PSP identifier and name).
  - **Note:** BT-2 + BT-166 collapse into *one* CII element. Writing BT-166 means switching the
    `@format` of `ram:IssueDateTime` from `102` to `208` and emitting date **and** time.

- [x] **A9 — BG-33 / BG-35 / BG-36 payment terms, discount, penalty** · 8 rows
  - BG-35: BT-170, BT-170-1, BT-171, BT-172 → `ram:ApplicableTradePaymentDiscountTerms`.
  - BG-36: BT-181, BT-181-1, BT-182, BT-183 → `ram:ApplicableTradePaymentPenaltyTerms`.
  - **Structural work:** UBL has one 0..n `cac:PaymentTerms` for all three groups and gives no
    explicit discriminator; CII has three distinct containers. Discriminate on the UBL side by
    presence: `cac:SettlementPeriod`/`cbc:SettlementDiscount*` → BG-35,
    `cac:PenaltyPeriod`/`cbc:PenaltyAmount`/`cac:PenaltyInterestRate` → BG-36, otherwise BG-33.
    This is the inverse of cii2ubl A9 and the one place where this direction is genuinely harder.

- [x] **A10 — BG-34 charges on behalf of a third party** · 3 rows
  - UBL `cac:CollectionInvoiceLine` / `cac:CollectionCreditNoteLine` →
    CII `ram:SpecifiedFinancialAdjustment`.
  - BT-179 (`cbc:TaxInclusiveLineExtensionAmount`), BT-180 (`cac:Item/cbc:Description`).
    BT-179-1 is UBL-only — it is **dropped** in this direction, which is the correct inverse of
    cii2ubl synthesising it.

- [x] **A11 — New allowance / charge / VAT-breakdown BTs** · 10 rows
  - BG-20: BT-173, BT-174, BT-213 · BG-21: BT-175, BT-176, BT-177, BT-177-1, BT-214
  - BG-23: BT-184 (`ram:CalculatedAmount/@currencyID`), BT-210
  - **Discriminator:** BT-105 and BT-177 share `cbc:AllowanceChargeReasonCode`; BT-177 is the one
    with `@listID='5153'`. Only that one gets `@listID`/`@listAgencyID` on the CII side — see 4.7.

- [x] **A12 — New line-level document references + BG-39** · 14 rows
  - BG-25: BT-188, BT-200, BT-201, BT-189, BT-190, BT-191, BT-192, BT-198, BT-199
  - BG-39: BT-217, BT-218, BT-218-1, BT-219, BT-220 (from `cac:InvoiceLine/cac:BillingReference`)

- [x] **A13 — BG-37 / BG-38 line delivery + new item and tax BTs** · 19 rows
  - BG-37 (5): BT-185, BT-186, BT-186-1, BT-187, BT-187-1
  - BG-38 (7): BT-203 … BT-209
  - BG-28 (2): BT-193, BT-193-1 · BG-30 (2): BT-194, BT-195 · BG-31 (1): BT-196
  - BG-32 (2): BT-211 (`ram:TypeCode`), BT-212 (`ram:ValueMeasure/@unitCode`)
  - **Discriminator:** BT-161a (`cbc:Value` → `ram:Value`) vs BT-161b (`cbc:ValueQuantity` →
    `ram:ValueMeasure`) — exactly one, per rule CII-SR-504.

### Phase 5 — Detection, CLI, comprehensive tests, docs

- [ ] **A14 — `UBLToCIIDispatcher` + facade** · ~3 h
  - Two-axis routing: the EN 16931 edition from BT-24 via `EEN16931Edition.detect`, and the
    document kind from the document element via `EEN16931SyntaxKind.getFromNodeOrNull`.
  - Explicit edition override; a clear `ErrorList` entry when detection fails, never a silent guess.
  - `UBLToCIIConversionHelper` gains `convertUBL25*ToCIID25A` counterparts of its six existing
    methods.
  - **Done when:** unit tests cover both prefixes, the XRechnung `#compliant#` suffix form, a
    missing BT-24, an unknown identifier and an unsupported document element.

- [ ] **A15 — CLI rework** · ~2 h
  - Default: auto-detect. `--en-version 2017|2026` forces and skips detection.
  - Wire `CIID25ACrossIndustryInvoiceTypeMarshaller` for the 2026 output path.
  - Error text when detection fails:
    `cannot determine EN 16931 edition (BT-24 missing); pass --en-version 2017|2026`.

- [ ] **A16 — Coverage guard and round-trip** · ~3 h
  - `MappingCoverageTest` parsing every row of `docs/en16931-2026-syntax.md`, failing if an
    identifier is never named in the 2026 converters. cii2ubl's A16 found two genuinely
    unimplemented rows this way; expect the same here.
  - Extend `CIIRoundTripTest` with the 2026 leg: D25A → (cii2ubl) → UBL 2.5 → (this) → D25A,
    compared against the original D25A.
  - **Done when:** every one of the 284 mapping rows is asserted by at least one test.

- [ ] **A17 — Documentation** · ~2 h
  - `README.md`: document the two bindings, the new package names, the CLI, `en16931-basics`.
  - `CLAUDE.md`: converter hierarchy, the mapping documents, the test strategy.
  - `README.md` News and noteworthy: `v3.0.0 - work in progress`.

## 7. Estimate

| Phase | Items | Sessions |
|-------|-------|----------|
| 1 Prepare and restructure | A0–A2 | ~1 |
| 2 Skeleton and harness | A3–A4 | ~1 |
| 3 Port carried-over rows | A5–A7 | ~2 |
| 4 New rows | A8–A13 | ~4 |
| 5 Detection, CLI, tests, docs | A14–A17 | ~3 |
| **Total** | **18 items** | **~11 sessions** |

Slightly below cii2ubl's estimate for the same 284 rows, for two reasons: the test corpus already
exists (cii2ubl A4/A7/A16 had to hand-author ~14 CII D25A instances), and the six path changes plus
the `@listID` and BT-218 traps are already known rather than having to be discovered.

## 8. Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| The 2026 binding has weaker test coverage than 2017 | A wrong mapping can ship undetected | XPath assertion per BT (D3); revisit once `phive-rules-en16931` publishes 2026 Schematron |
| The test corpus is cii2ubl's own output | A gap in cii2ubl becomes a blind spot here | cii2ubl's `MappingCoverageTest` and its per-BT assertions make the corpus complete by construction; the A16 round-trip additionally compares against the hand-written D25A originals |
| BG-33/35/36 have no explicit UBL discriminator | Payment terms land in the wrong CII container | Discriminate by element presence (A9) and assert with `assertNoXPath` that the containers never merge |
| A1/A2 silently change 2017 output | Regression for every existing user | `git status` on `generated/` must stay empty (A0) |
| BT-24 detection fails on non-conformant UBL | Dispatcher cannot route | Explicit edition override and an `ErrorList` entry, never a silent guess (A14) |

## 9. Progress log

| Item | Session date | Commit | Notes |
|------|--------------|--------|-------|
| A0 | 2026-09-07 | `[3.0.0 A0]` f94febf | `generated/cii/` turned out to be tracked already; the baseline was green from the start. `.gitignore` hid every *future* file below `generated/`, so new output would have been silently dropped - narrowed to `**/generated/roundtrip/`. |
| A1 | 2026-09-07 | `[3.0.0 A1]` d18f96f | Pure substitution, verified against the enums behind `EN16931CodeLists`: `3->5`, `35->29`, `432->72` and `VAT->VA` are unchanged. `generated/cii/` byte-identical. |
| A2 | 2026-09-07 | `[3.0.0 A2]` 6640114 | Only three members are genuinely edition-independent here, because A1 had already moved the facts of the standard to `en16931-basics` and this project has no settings API. 22 members in, 22 out. `generated/cii/` byte-identical. |
| A3 | 2026-09-07 | `[3.0.0 A3]` 3fb0d97 | Merged with A5 - a hand-written skeleton would have been throwaway work *and* a bug source, per cii2ubl's own A3/A5 experience. 20 compile errors, every one a cardinality widening exactly as predicted in 4.6, no semantic surprise. The eight UBL-side ones collapse into one `getFirstValue` helper. |
| A4 | 2026-09-07 | `[3.0.0 A4]` 048cd84 | All 16 corpus files already convert to XSD-valid CII D25A straight out of the A3 bulk port - the port itself needed no correction. `MockD25ASettings` mirrors cii2ubl's, with the CII namespace context. Negative-probed: a wrong expected value and a non-existent element both fail. 2026 output goes to `generated/cii-d25a/`, tracked like the 2017 one. |
| A6 | 2026-09-07 | `[3.0.0 A6]` 7cb2ab7 | All six applied. **XSD validity alone had hidden a real data loss:** before this item the whole of BG-1 was silently dropped, because the ported converter still read `cbc:Note` while a 2026 document carries `cac:Annotation`. That is the concrete case for D3 - the output was schema valid and simply missing two business terms. `BT-32-2` needs the local `LOC`/`FC` pair; `EEN16931TaxSchemeCode` of en16931-basics only knows `VAT`/`VA`, and cii2ubl holds the same constant locally. |
| A8 | 2026-09-07 | `[3.0.0 A8]` 21cfbf6 | The CII-only `@format` codes and scheme identifiers were implemented all along but not traceable to a mapping row; naming them at their sites is what the coverage guard needs. |
| A9 | 2026-09-07 | `[3.0.0 A9]` 0145770 | The one place this direction is harder than cii2ubl's: UBL has no discriminator between BG-33, BG-35 and BG-36, so they are told apart by the elements they use. **Found a latent bug** - `cac:PaymentTerms` became 0..n in 2026 and BT-9, which is 0..1, was written into every one of them. |
| A10 | 2026-09-07 | `[3.0.0 A10]` 383db6b | BT-179-1 is dropped, the correct inverse of cii2ubl synthesising it. The two UBL collection line types are unrelated Java classes, so the helper takes extracted values. |
| A11 | 2026-09-07 | `[3.0.0 A11]` 262f3e7 | The 4.7 `@listID` trap in reverse. BT-184 needed a judgment call the table does not make - see the commit. BT-193/BT-193-1 and BT-194/BT-195/BT-196 came along for free, as they did in cii2ubl. |
| A12 | 2026-09-07 | `[3.0.0 A12]` 87068d8 | All nine new references share one shape, so one helper covers them - including dropping the `None` placeholder that the UBL binding prescribes for absent line references. |
| A13 | 2026-09-07 | `[3.0.0 A13]` ac92543 | **Second source defect found** - BT-186/BT-186-1 of the line level BG-37 are mapped to the header delivery, recorded as finding 8. Coverage guard now reports 284 of 284. |
| A7 | 2026-09-07 | `[3.0.0 A7]` | ~130 assertions across BG-2 to BG-32 on the two comprehensive files, plus the credit note renames. **All green on the first run** - the A3 bulk port lost nothing, which is the evidence D6's reasoning was right for the 2026 side too. |
