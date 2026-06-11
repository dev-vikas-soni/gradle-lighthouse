# Industry Benchmarking

Lighthouse transforms your health score into a relative metric by comparing your project against a registry of "Ground Truth" snapshots from major open-source projects.

---

## The Benchmark Snapshot

A benchmark is a JSON file containing the results of a real Lighthouse audit.

```json
{
  "projectName": "Signal Android",
  "generatedAt": "2026-06-11T10:00:00Z",
  "lighthouseVersion": "2.3.0",
  "overallScore": 87.87,
  "moduleCount": 164,
  "persona": "LARGE_ANDROID_APP",
  "categoryScores": {
    "ARCHITECTURE": 85.0,
    "SECURITY": 95.0
  }
}
```

---

## Registry & Providers

The **BenchmarkRegistry** orchestrates multiple data sources:
1.  **Embedded Provider**: Bundled reference data for projects like *Signal* and *Now in Android*.
2.  **JSON Provider**: Scans a `benchmarks/` directory at your project root for custom snapshots.

---

## Automated Workflow

To maintain trust, benchmark scores are never manually edited. They are refreshed via the **Benchmark Export Pipeline**:

1.  **Clone**: Checkout the target repository (e.g., Signal).
2.  **Audit**: Run `./gradlew lighthouseAudit`.
3.  **Export**: Run `./gradlew lighthouseExportBenchmark`.
4.  **Adopt**: Copy the generated `benchmark.json` into your local `benchmarks/` directory.

---

## Percentile Engine

Lighthouse calculates where your project stands in the global ecosystem:
*   **Overall Percentile**: "Top 28% of all benchmarked projects."
*   **Persona Percentile**: "Top 12% of Modular Monoliths."

---

## Version Compatibility

The scoring engine evolves. To prevent stale comparisons, every snapshot includes a `lighthouseVersion`. If you load a benchmark generated with an older engine, Lighthouse will issue a warning:

`⚠️ [BENCH] Benchmark 'X' was generated with V2.2.0. Current engine is V2.3.0. Re-audit recommended.`

---

## Recommended Benchmarks

| Project | Ideal For |
|---------|-----------|
| **Now in Android** | Gold standard for modular architecture and modern tooling. |
| **Signal Android** | Reference for large-scale, security-first production apps. |
| **Firefox Android** | Reference for massive enterprise-scale repositories. |
