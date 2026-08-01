# Compatibility Matrix

Lighthouse is tested against various versions of Gradle and the Android Gradle Plugin (AGP).

## Gradle Support

| Version | Status | Notes |
| :--- | :--- | :--- |
| **8.0 - 8.x** | ✅ Full Support | Standard target. |
| **9.0+** | ✅ Full Support | Tested for Project Isolation compatibility. |
| **7.x and below** | ❌ Not Supported | Requires Java 17 and Provider API. |

## Android Gradle Plugin (AGP)

| Version | Status | Notes |
| :--- | :--- | :--- |
| **8.0+** | ✅ Full Support | Optimized for modern AGP models. |
| **7.4+** | ✅ Full Support | Minimum recommended version. |
| **Below 7.4** | ⚠️ Partial Support | Some auditors may not find all source sets. |

## Kotlin Support

| Feature | Support |
| :--- | :--- |
| **Kotlin DSL** | ✅ 100% |
| **Groovy DSL** | ✅ 100% |
| **Kotlin Multiplatform** | ✅ Supported |

## Environment

* **Java**: 17, 21+ supported.
* **OS**: Linux, macOS, Windows supported.
