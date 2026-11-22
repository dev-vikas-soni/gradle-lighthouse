# Compatibility and verification status

Do not treat a version range as a tested compatibility guarantee. Compatibility depends on the plugin release, Gradle, JDK, Android Gradle Plugin (AGP), Kotlin Gradle Plugin, and project layout.

## Declared requirements

The installation guide currently documents Gradle 8+ and JDK 17+ as requirements. Android-specific checks require an Android Gradle Plugin project. These are project requirements, not a claim that every combination in those ranges has been tested.

## Verification matrix

A release-specific matrix should be populated only from successful automated CI/TestKit runs. Until that evidence is published, use the following status:

| Component | What can safely be claimed here |
| --- | --- |
| Gradle | Gradle 8+ is the documented minimum; exact tested versions must be confirmed from release CI. |
| JDK | JDK 17+ is the documented minimum; do not infer all later JDKs are verified. |
| AGP | Android checks depend on AGP models and project configuration; no blanket AGP compatibility guarantee is made here. |
| Kotlin | Kotlin DSL is used in examples. Kotlin plugin version compatibility must be verified per release. |
| Kotlin Multiplatform | Some KMP-specific checks exist, but coverage is partial and not a comprehensive source-set or target analysis. |
| Operating systems | Use the Gradle/JDK-supported environment for your build; report OS details when filing an issue. |

## Gradle performance features

Configuration Cache and Isolated Projects compatibility must be established through automated tests on each supported release matrix. A design goal or absence of known failures is not proof of full compatibility. Check the release's CI results and report any reproducible issue.

## Reporting a compatibility problem

Include:
- Gradle wrapper version and JDK vendor/version.
- AGP and Kotlin plugin versions, if applicable.
- Operating system.
- Whether Configuration Cache or Isolated Projects is enabled.
- A minimal reproducer and full relevant stack trace.

Until a version appears in a published, passing CI matrix, describe it as unverified rather than supported.
