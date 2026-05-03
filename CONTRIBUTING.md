# Contributing to Gradle Lighthouse 🏗️

First off, thank you for considering contributing to Gradle Lighthouse! It's people like you that make Lighthouse a powerful tool for the global Android community.

## 🚀 How to Add a New Auditor

The heart of Lighthouse is its modular **Auditor** system. Adding a new check is as simple as implementing the `Auditor` interface.

### 1. Create your Auditor class
Create a new file in `src/main/kotlin/com/gradlelighthouse/auditors/`:

```kotlin
class MyCustomAuditor : Auditor {
    override val name = "MyCustomCheck"

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()

        // Use the context to inspect the project
        if (context.pluginIds.contains("some-problematic-plugin")) {
            issues.add(AuditIssue(
                category = name,
                severity = Severity.WARNING,
                title = "Problematic Plugin Detected",
                reasoning = "This plugin is known to slow down configuration by 200ms.",
                impactAnalysis = "Slower developer feedback loop.",
                resolution = "Migrate to the modern alternative: 'com.example.modern'.",
                roiAfterFix = "Estimated 15s saved per day per developer."
            ))
        }

        return issues
    }
}
```

### 2. Register it in `LighthouseTask.kt`
Add your auditor to the `buildAuditorList` method:

```kotlin
if ("MyCustomCheck" in enabled) auditors.add(MyCustomAuditor())
```

### 3. Add a toggle in `LighthouseExtension.kt`
Ensure users can opt-out if needed:

```kotlin
val enableMyCustomCheck: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
```

## 🛠️ Development Setup

1.  Clone the repo.
2.  Run `./gradlew build` to verify the environment.
3.  Use `./gradlew publishToMavenLocal` to test your changes in a local sample project.

## ✅ Pull Request Guidelines

- Ensure all new auditors are **Configuration Cache compatible** (only use data from the `AuditContext`).
- Include a brief explanation of the "ROI" of your new check.
- Update the `README.md` features table if you add a major new capability.

---

**Happy coding! Together, we'll build the fastest Android projects on earth.**
