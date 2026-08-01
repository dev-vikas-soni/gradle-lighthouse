# Troubleshooting

Common issues and their solutions.

## 1. "Configuration resolved during configuration time"
Lighthouse itself uses the Provider API to avoid this. If you see this error, ensure that your `lighthouse { }` block is not accessing configurations eagerly (e.g. `configurations.implementation.get()`).

## 2. Report collisions in multi-module projects
If two modules have the same simple name (e.g., `:feature:search:ui` and `:feature:auth:ui`), older versions of Lighthouse might overwrite reports.
**Fix**: Upgrade to v2.1.1+, which uses path-sanitized subdirectories for module reports.

## 3. Configuration Cache serialization errors
If Lighthouse fails to cache the task graph:
1. Ensure you are on Gradle 8.5+.
2. Run with `--configuration-cache-problems=warn` to identify the specific capturing object.
3. If the object is within Lighthouse, please [open an issue](https://github.com/dev-vikas-soni/gradle-lighthouse/issues).

## 4. Galaxy Graph is empty or missing links
Aggregation requires `lighthouseAggregate` to be run **with** or **after** `lighthouseAudit`.
**Fix**: Run `./gradlew lighthouseAudit lighthouseAggregate`.

## 5. Baselines are not suppressing issues
Check that the fingerprints match exactly. Moving a file or changing an auditor version can change the fingerprint.
**Fix**: Re-record the baseline using `./gradlew lighthouseRecordBaseline`.
