# Contributing

Contributions are welcome — bug fixes, new auditors, documentation improvements. This guide explains the development workflow and the conventions you need to follow.

---

## Table of Contents

1. [Development setup](#1-development-setup)
2. [Adding a new auditor](#2-adding-a-new-auditor)
3. [Pull request checklist](#3-pull-request-checklist)
4. [Code conventions](#4-code-conventions)

---

## 1. Development setup

```bash
git clone https://github.com/dev-vikas-soni/gradle-lighthouse.git
cd gradle-lighthouse

# Build and run all tests
./gradlew build

# Publish to Maven Local for end-to-end testing
./gradlew publishToMavenLocal
```

To test your changes against a real project, add `mavenLocal()` to the `pluginManagement` block in `example/settings.gradle.kts`, then run `./gradlew lighthouseAudit lighthouseAggregate` from the `example/` folder.

---

## 2. Adding a new auditor

There are 19 auditors today. Adding one means touching a small number of files in a specific order.

### Step 1 — Implement the `Auditor` interface

```kotlin
// src/main/kotlin/com/gradlelighthouse/auditors/MyCustomAuditor.kt

class MyCustomAuditor : Auditor {
    override val name = "MyCustomCheck"

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()

        if (context.pluginIds.contains("some-problematic-plugin")) {
            issues.add(AuditIssue(
                category = name,
                severity = Severity.WARNING,
                title = "Problematic Plugin Detected",
                reasoning = "This plugin performs eager configuration resolution, adding ~200ms to every build.",
                impactAnalysis = "Slower local builds and Configuration Cache misses.",
                resolution = "Replace with `id(\"com.example.modern\")` — a lazy equivalent that resolves at execution time.",
                roiAfterFix = "~15 seconds saved per build."
            ))
        }

        return issues
    }
}
```

**Important**: Auditors must not access the filesystem, network, or any Gradle API directly. Use only data already in `AuditContext`. This is what keeps the plugin Configuration Cache–safe.

### Step 2 — Register in `LighthouseTask.kt`

In `buildAuditorList()`:

```kotlin
if ("MyCustomCheck" in enabled) auditors.add(MyCustomAuditor())
```

### Step 3 — Add an extension toggle

In `LighthouseExtension.kt`:

```kotlin
val enableMyCustomCheck: Property<Boolean> =
    objects.property(Boolean::class.java).convention(true)
```

### Step 4 — Wire the toggle in `LighthousePlugin.kt`

Inside the `enabledAuditorNames` provider builder:

```kotlin
if (ext.enableMyCustomCheck.get()) add("MyCustomCheck")
```

### Step 5 — Add context data (if needed)

If your auditor needs something not already in `AuditContext`:

1. Add a `Serializable` field to `AuditContext.kt`
2. Capture it as a pipe-delimited `Provider<List<String>>` in `LighthousePlugin.kt`
3. Declare a corresponding `@Input` or `@InputFiles` on `LighthouseTask`
4. Reconstruct typed data from the string in `LighthouseTask.buildAuditContext()`

### Step 6 — Write tests

Tests live in `src/test/kotlin/com/gradlelighthouse/` and use `GradleRunner`:

```kotlin
@Test
fun `MyCustomAuditor detects problematic plugin`() {
    // Arrange: write a build.gradle.kts that applies the problematic plugin
    // Act: run lighthouseAudit via GradleRunner
    // Assert: output contains the expected issue title
}
```

Cover both the "issue found" and "no issue" code paths. Untested auditors won't be merged.

### Step 7 — Update docs

- Add a row to the [auditor registry table](docs/LLD.md#2-auditor-registry) in `LLD.md`
- Add a bullet to the "What it checks" section in `README.md`
- Document the new `lighthouse {}` toggle in `docs/USER_MANUAL.md` Section 3

---

## 3. Pull request checklist

- **Configuration Cache compatibility**: auditors must use `AuditContext` only. PRs that reference `Project` inside a `@TaskAction` will not be merged.
- **ROI field**: every `AuditIssue` needs a meaningful `roiAfterFix`. Vague values like "improves performance" aren't acceptable.
- **All 7 steps completed**: missing toggle wiring, tests, or doc updates will require revision before merge.
- **One concern per auditor**: if a check spans unrelated domains, split it.
- **`reasoning` field**: assume the reader knows Gradle well. Explain the root cause technically, not just the symptom.

---

## 4. Code conventions

- Follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- All four `AuditIssue` text fields (`reasoning`, `impactAnalysis`, `resolution`, `roiAfterFix`) must be non-empty strings — no placeholder text.
- No external dependencies. The plugin has zero runtime dependencies. Use only Kotlin stdlib and the Gradle API.
- No class-level mutable state in auditors.
