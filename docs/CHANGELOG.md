# Change Log
All notable changes to this project will be documented in this file.

## [Unreleased]

## [0.2.4]
* Introduce readArticles and readArticlesPredictive method
* Introduce readData method
* Change behavior
  * Do not search again with lower case.
* Bump versions
  * Tika@2.3.0
  * Gradle git-version@0.13.0
  * Spotless@6.2.1
  * SpotBugs@5.0.5
  * Actions setup-java@2.5.0
  * Actions gradle-build-action@v2
 
## [0.2.3]
* Bump Gradle/gradle-build-action@v2

## [0.2.2]
* Bump versions
  * Jackson@2.13.1
  * JUnit@5.8.2
  * Gradle kotlin@1.6.10
  * actions setup-java@2.4.0
  * BouncyCastle@1.70
  * Gradle@7.3.2

## [0.2.1]
* Support dictionary that use UTF-16(LE) as encoding. 
    * Force endian to LE when UTF-16 is specified even lacking BOM.

## [0.2.0]
* Support MDD file loading
* Test: Apache Tika for dependency
* Improve test

## [0.1.4]
* Bump jackson@2.10.5
* Experimental implementation for .MDD file
* Update and fix v1 parser
* Update and fix dictionary key loading
* Improve tests

## [0.1.3]
* Change jackson version to 2.7.4.

## 0.1.2
* Fix publish configurations
 
## 0.1.1
* First release

## 0.1.0
* First internal release

[Unreleased]: https://github.com/eb4j/mdict4j/compare/v0.2.4...HEAD
[0.2.4]: https://github.com/eb4j/mdict4j/compare/v0.2.3...v0.2.4
[0.2.3]: https://github.com/eb4j/mdict4j/compare/v0.2.2...v0.2.3
[0.2.2]: https://github.com/eb4j/mdict4j/compare/v0.2.1...v0.2.2
[0.2.1]: https://github.com/eb4j/mdict4j/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/eb4j/mdict4j/compare/v0.1.4...v0.2.0
[0.1.4]: https://github.com/eb4j/mdict4j/compare/v0.1.3...v0.1.4
[0.1.3]: https://github.com/eb4j/mdict4j/compare/v0.1.2...v0.1.3
