# Usage telemetry and privacy

Gradle Lighthouse usage telemetry is **disabled by default**. It is sent only when a project owner explicitly sets:

```kotlin
lighthouse {
    enableTelemetry.set(true)
}
```

## Event contract

An audit-completed event contains only:

- `event`: the fixed value `audit_completed`
- `installationId`: a randomly generated UUID persisted in the Gradle User Home directory
- `pluginVersion`: the running Gradle Lighthouse version
- `gradleVersion`: the running Gradle version

The identifier is pseudonymous, not a claim of perfect anonymity. It is shared by Gradle builds using the same Gradle User Home, so it represents an opted-in installation environment rather than a person or a unique repository.

The event deliberately excludes project and module names, filesystem paths, dependency coordinates, source code, repository URLs, audit findings, health scores, and build performance measurements.

## Storage and opt-out

The identifier is stored at:

`$GRADLE_USER_HOME/caches/gradle-lighthouse/installation-id`

Delete this file to generate a new identifier. To disable telemetry, leave `enableTelemetry` unset (the default) or set it to `false`. No event is sent when telemetry is disabled.

## Endpoint

No telemetry collector is bundled with the plugin. Configure an HTTPS endpoint operated by your organization or a service you trust:

```kotlin
lighthouse {
    enableTelemetry.set(true)
    telemetryEndpoint.set("https://your-telemetry.example/v1/events")
}
```

The endpoint defaults to an empty value, so opting in without configuring an endpoint sends no event. Only HTTPS endpoints are accepted. The endpoint operator is responsible for publishing its retention, access, and deletion policies. Lighthouse treats telemetry as best-effort and does not fail an audit when the endpoint is unavailable.

## Interpreting counts

The installation identifier supports counting distinct opted-in Gradle User Home environments and identifying repeat activity. It does not measure total downloads, all plugin users, unique people, or all repositories. Users who disable telemetry are not counted.
