# en16931-ubl2cii

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.helger/en16931-ubl2cii/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.helger/en16931-ubl2cii) 
[![javadoc](https://javadoc.io/badge2/com.helger/en16931-ubl2cii/javadoc.svg)](https://javadoc.io/doc/com.helger/en16931-ubl2cii)

Converter library for EN 16931:2017 invoices from UBL 2.1 to CII D16B

This version is based on the version of @VartikaG02: https://github.com/VartikaG02/en16931-ubl2cii - it was extended by adding CII conformance to the EN.
 Additionally UBL Credit Notes are supported and a commandline client was added.

This is a Java 11+ library that converts a Universal Business Language (UBL) 2.1 into a Cross Industry Invoice (CII) D16B document following the rules of the European Norm (EN) 16931 that defines a common semantic data model for electronic invoices in Europe.

This is the counterpart to https://github.com/phax/en16931-cii2ubl which can be used to convert CII D16B invoices to different UBL versions.

This library is licensed under the Apache License Version 2.0.

# News and noteworthy

* v1.1.2 - 2025-05-31
    * Only pushing to Maven Central
* v1.1.1 - 2025-03-20
    * Improved mapping for BT-6, BT-7, BT-8, BT-9, BT-10, BT-11 and BT-111
* v1.1.0 - 2025-02-22
    * Added a simple command line client
    * The created CII documents are now compliant to the EN 16931:2017 validation artefacts
    * Added support to convert UBL 2.1 CreditNotes as well
    * Initial fork from https://github.com/VartikaG02/en16931-ubl2cii

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.
