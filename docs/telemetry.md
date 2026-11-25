# Usage telemetry and privacy

Gradle Lighthouse includes anonymous usage telemetry to help understand plugin adoption and Gradle version compatibility across the ecosystem.

## Event contract

An audit-completed event contains strictly four fields:

- `event`: the fixed value `"audit_completed"`
- `installationId`: a randomly generated UUID persisted in the Gradle User Home directory
- `pluginVersion`: the running Gradle Lighthouse plugin version (e.g., `"2.4.0"`)
- `gradleVersion`: the running Gradle runtime version (e.g., `"9.0"`)

The installation identifier is pseudonymous and represents the Gradle installation environment. It is stored under the user's Gradle User Home (`$GRADLE_USER_HOME/caches/gradle-lighthouse/installation-id`) and reused across projects and invocations.

The event strictly excludes project names, module paths, filesystem paths, dependency coordinates, source code, repository URLs, audit findings, health scores, and build metadata.

## Opt-out controls

Telemetry is enabled by default. You can disable telemetry using any of the following mechanisms:

1. **Gradle Command-line Property:**
   ```bash
   ./gradlew lighthouseAudit -PgradleLighthouseTelemetry=false
   ```

2. **Environment Variable:**
   ```bash
   export GRADLE_LIGHTHOUSE_TELEMETRY=false
   ```

3. **Gradle Build Script DSL:**
   ```kotlin
   lighthouse {
       enableTelemetry.set(false)
   }
   ```

When telemetry is disabled, no network requests occur, no background threads are created, and no installation ID is generated or persisted.

## Production endpoint

The default production telemetry endpoint is:

`https://asia-south1-gradle-lighthouse-telemetry.cloudfunctions.net/telemetry`

Enterprise users running custom internal telemetry collectors can override the default endpoint in `build.gradle.kts`:

```kotlin
lighthouse {
    telemetryEndpoint.set("https://your-custom-endpoint.example/telemetry")
}
```

Telemetry delivery is best-effort and non-blocking: network failures or server errors never affect task outcomes or build execution.
