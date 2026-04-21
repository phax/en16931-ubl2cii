# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

EN 16931 UBL 2.1 to CII D16B converter library. Converts Universal Business Language (UBL) 2.1 invoices and credit notes into Cross Industry Invoice (CII) D16B format, following the European Norm (EN) 16931 semantic data model. Java 17+ required. Counterpart to [en16931-cii2ubl](https://github.com/phax/en16931-cii2ubl).

## Build Commands

```bash
# Full build with tests
mvn clean install

# Build specific module
mvn clean install -pl en16931-ubl2cii
mvn clean install -pl en16931-ubl2cii-cli

# Run all tests
mvn test

# Run single test class
mvn test -pl en16931-ubl2cii -Dtest=UBL21InvoiceToCIID16BConverterTest

# Run single test method
mvn test -pl en16931-ubl2cii -Dtest=UBL21InvoiceToCIID16BConverterTest#testConvertAndValidateAllInvoices

# Build CLI fat JAR
mvn clean package -pl en16931-ubl2cii-cli

# Run CLI
java -jar en16931-ubl2cii-cli/target/en16931-ubl2cii-cli-full.jar -t ./output invoice.xml
```

## Modules

- **en16931-ubl2cii** — Core conversion library
- **en16931-ubl2cii-cli** — Picocli-based command-line tool (shade plugin produces fat JAR)

## Architecture

All converter code is in package `com.helger.en16931.ubl2cii`.

**Conversion chain:** UBL XML → `UBL21Marshaller` (parse) → `InvoiceType`/`CreditNoteType` → Converter → `CrossIndustryInvoiceType` → `CIID16BCrossIndustryInvoiceTypeMarshaller` (serialize) → CII XML

**Key classes:**
- `AbstractToCIID16BConverter` — Base class with shared helper methods (`convertAddress`, `convertParty`, `convertAmount`, `convertDate`, `convertApplicableTradeTax`, etc.)
- `UBL21InvoiceToCIID16BConverter` — Invoice conversion. Entry point: `convertToCrossIndustryInvoice(InvoiceType, ErrorList)`
- `UBL21CreditNoteToCIID16BConverter` — Credit note conversion. Same pattern.
- `UBLToCIIConversionHelper` — Static facade with stream-based convenience methods and auto-detection of invoice vs credit note

The converters map UBL elements to CII using EN 16931 Business Term (BT) codes (BT-1 through BT-125+). Comments in converter code reference these BT codes.

## Testing

- JUnit 4
- Tests convert sample UBL files from `src/test/resources/external/ubl21/` and validate the resulting CII against EN 16931 rules using the phive validation framework
- Generated CII output goes to `generated/cii/` (gitignored)

## Key Dependencies

- `com.helger.ubl:ph-ubl21` — UBL 2.1 JAXB models
- `com.helger.cii:ph-cii-d16b` — CII D16B JAXB models
- `com.helger.commons:ph-commons` — Helger utility library (collections, XML, etc.)
- `com.helger.phive.rules:phive-rules-en16931` — EN 16931 validation rules (test scope)
