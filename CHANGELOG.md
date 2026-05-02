# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-05-02

### Added
- **Global Rebranding**: Officially rebranded to `Gradle Lighthouse`.
- **Enterprise Capabilities**: Added `lighthouseAggregate` task to generate a single 360° dashboard for 100+ module ecosystems.
- **Reporting Engines**: Added native SARIF v2.1.0 and JUnit XML generators for deep CI/CD integration.
- **Auditor Extensibility**: Re-architected core engine to support isolated `Auditor` interfaces.
- **Play Policy Auditor**: New compliance scanner to detect restricted permissions and target SDK mismatches.
- **Configuration Cache Safety**: Reworked `AuditContext` to strictly serialize inputs, guaranteeing zero cache invalidations in Gradle 8+.

### Changed
- Refactored package from `com.droidunplugged` to `com.gradlelighthouse`.
- DSL Configuration block renamed to `lighthouse { }`.
- Audit tasks renamed from `depAudit` to `lighthouseAudit`.

### Removed
- Removed legacy tightly-coupled monolithic execution patterns.


