# Configuration Cache guide

Gradle's Configuration Cache can reuse the configured task graph between builds when the build and participating plugins satisfy Gradle's requirements.

Gradle Lighthouse is designed to keep task execution inputs explicit and avoid unnecessary access to Gradle project models from task actions. This design intent is not, by itself, proof of compatibility across every supported Gradle release or every plugin configuration.

## Verify in your build

Run the audit twice with Configuration Cache enabled:

```bash
./gradlew lighthouseAudit --configuration-cache
./gradlew lighthouseAudit --configuration-cache
```

Inspect Gradle's output for cache storage/reuse messages and any problems report. Repeat for aggregate tasks and the project configurations you actually use. Also test with the exact Gradle, JDK, AGP, and Kotlin plugin versions in your build.

## Troubleshooting

If Gradle reports a Configuration Cache problem:
1. Capture the complete Gradle output and problems report.
2. Check whether the issue is attributed to Lighthouse, another plugin, or build logic.
3. Create a minimal reproducer.
4. Report the plugin version and environment versions in an issue.

Do not assume a cache problem is external to Lighthouse without evidence.

## Performance

Configuration Cache reuse can reduce configuration work, but actual timings depend on the project and environment. This guide makes no universal speedup or audit-overhead guarantee.
