# Roadmap

Planned work, roughly in priority order. Phases 1 and 2 are shipped.

---

## ✅ Phase 1 — Foundation (v1.x–v2.x)

- [x] 19 auditors covering performance, security, quality, compliance, and module architecture
- [x] `lighthouseAggregate` task for multi-module summary dashboards
- [x] SARIF v2.1.0 and JUnit XML output for CI/CD integration
- [x] Configuration Cache compatible (Gradle 8.5+)
- [x] Isolated Projects compatible (Gradle 9.x)

---

## ✅ Phase 2 — Visualisation & Enforcement (v2.2)

- [x] Interactive Galaxy Graph — canvas-based module dependency visualisation with cycle highlighting
- [x] Historical trend tracking — health score, coupling density, fatal issue count across 30 builds
- [x] Enforcement gates — build fails on cycles, layer violations, or score floor breaches
- [x] Custom YAML rules (`lighthouse-rules.yaml`) for team-specific architectural constraints
- [x] Sandbox Mode — simulate removing a dependency edge and see the effect on score and cycles before touching code
- [x] Gamified rank system (Legacy → Grandmaster Architect)

---

## ✅ Phase 3 — Automation & Adoption (v2.3)

- [x] **Baseline System**: Record and suppress existing technical debt to focus on new issues
- [x] **Deterministic Fixes**: `lighthouseFix` task to automatically apply best practices (caching, parallel, etc.)
- [x] **Remediation Database**: Every issue now links to a deep-dive technical "Recipe" URL
- [x] **Privacy-First Telemetry**: Anonymous usage tracking to prioritize auditor improvements
- [x] **Lighthouse PR Bot**: GitHub Action to comment on PRs with health score deltas and cycle warnings
- [x] **Predictive Dependency Intelligence**: Global data to estimate binary size/startup impact *before* you add an SDK
- [x] **GenAI Remediation**: `./gradlew lighthouseFix --ai` to generate complex refactoring diffs (KAPT to KSP, Version Catalog migration)

---

## 🔲 Phase 4 — Ecosystem & Governance (2027)

- [ ] **Lighthouse CLI**: standalone tool for environments without a Gradle wrapper
- [ ] **IDE Plugin**: Real-time architectural guardrails and health warnings inside Android Studio/IntelliJ
- [ ] **Bytecode & Resource Audit**: Deep-dive into compiled .dex and .apk for unused code paths and asset redundancy
- [ ] **Green Build Sustainability**: Measure and report the CO2/Energy impact of your CI/CD pipeline
- [ ] **Cloud Dashboard**: Cross-repository health monitoring for organisations managing multiple projects

---

## Suggestions

Open an [issue](https://github.com/dev-vikas-soni/gradle-lighthouse/issues) or start a [discussion](https://github.com/dev-vikas-soni/gradle-lighthouse/discussions).
