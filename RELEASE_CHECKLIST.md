# Release Validation Checklist

Use this checklist to verify build quality before publishing a new version to the Gradle Plugin Portal.

## 🏗️ Build & Integrity
- [ ] `./gradlew clean jar` succeeds.
- [ ] All unit tests pass (`./gradlew test`).
- [ ] No regression in `ModernHealthScoreEngineTest`.
- [ ] `BenchmarkCalibrationTest` confirms industry ranges.

## 📊 Benchmarking Engine
- [ ] `./gradlew lighthouseBenchmarkStatus` displays loaded benchmarks.
- [ ] `./gradlew lighthouseExportBenchmark` produces valid `benchmark.json`.
- [ ] Local `benchmarks/*.json` files are correctly loaded and deduplicated.
- [ ] Version mismatch warnings trigger for older snapshots.

## 🎨 Dashboard & Reporting
- [ ] HTML dashboard renders all 9 categories.
- [ ] "Architecture Health Breakdown" shows correct F/E/W counts.
- [ ] "Industry Benchmarking" section shows correct deltas.
- [ ] Galaxy Graph loads with layers and cycle detection.
- [ ] SARIF and JUnit XML outputs contain all findings.

## 📖 Documentation & DX
- [ ] `README.md` features match implementation.
- [ ] `docs/*.md` are up to date.
- [ ] Version number in `gradle.properties` matches `LighthousePlugin.VERSION`.
- [ ] `CHANGELOG.md` entry created for current version.

## 🚀 Publication
- [ ] `./gradlew publishToMavenLocal` succeeds.
- [ ] Verified locally in Signal or NIA project.
