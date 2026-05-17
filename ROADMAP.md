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

## 🔲 Phase 3 — Automation (Q4 2026)

- [ ] **Gradle Build Scan integration**: deep-link from Lighthouse HTML reports into Gradle Build Scans for trace-level analysis
- [ ] **AI-assisted fix suggestions**: LLM-generated diffs for common refactors (KAPT → KSP, `buildSrc` → `build-logic/`)
- [ ] **Predictive dependency impact**: estimate build time delta before adding a new dependency, using past CI data
- [ ] **Migration bots**: CI-ready, commit-ready scripts for high-value but mechanical migrations

---

## 🔲 Phase 4 — Ecosystem (2027)

- [ ] **Lighthouse CLI**: standalone tool for environments without a Gradle wrapper
- [ ] **IDE plugin**: real-time cycle warnings and health score inside Android Studio and IntelliJ IDEA
- [ ] **Cloud Dashboard**: cross-repository health monitoring for organisations managing multiple projects
- [ ] **KMP deep audits**: `expect`/`actual` coverage checks and platform-specific dependency hygiene

---

## Suggestions

Open an [issue](https://github.com/dev-vikas-soni/gradle-lighthouse/issues) or start a [discussion](https://github.com/dev-vikas-soni/gradle-lighthouse/discussions).
