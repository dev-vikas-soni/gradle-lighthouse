# Adding a New Auditor

Follow this step-by-step guide to implement a new diagnostic check in Gradle Lighthouse.

## 1. Create the Auditor Class

Create a new file in `com.gradlelighthouse.auditors`.

```kotlin
class MyNewAuditor : Auditor {
    override val name: String = "MyCheck"

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()

        // Use context.dependencies, context.sourceSets, etc.
        // DO NOT use the filesystem or project APIs.

        if (someViolationFound) {
            issues.add(AuditIssue(
                category = LighthouseCategory.ARCHITECTURE,
                severity = Severity.WARNING,
                title = "Violation Found",
                reasoning = "...",
                impactAnalysis = "...",
                resolution = "...",
                roiAfterFix = "..."
            ))
        }

        return issues
    }
}
```

## 2. Register the Auditor

Add your new auditor to the registry in `LighthouseTask.kt`.

```kotlin
// LighthouseTask.kt
protected fun buildAuditorList(enabled: Set<String>): List<Auditor> {
    val auditors = mutableListOf<Auditor>()
    // ...
    if ("MyNewCheck" in enabled) auditors.add(MyNewAuditor())
    return auditors
}
```

## 3. (Optional) Add a DSL Toggle

If you want users to be able to disable your check, add a property to `LighthouseExtension.kt`.

```kotlin
// LighthouseExtension.kt
abstract val enableMyNewCheck: Property<Boolean>

init {
    enableMyNewCheck.convention(true)
}
```

Update `LighthousePlugin` to wire the property to the task.

## 4. Test Your Auditor

1. **Unit Test**: Create a test in `src/test` using a mock `AuditContext`.
2. **Functional Test**: Add a failing case to the `example/` project and run `./gradlew lighthouseAudit`.

## 5. Document

Add your check to the [Auditor Catalog](../reference/auditor-catalog.md).
