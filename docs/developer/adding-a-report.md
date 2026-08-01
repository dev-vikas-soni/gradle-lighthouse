# Adding a New Report Format

Lighthouse is designed to be extensible, allowing you to output architectural intelligence in any format required by your CI/CD tools.

## 1. Create a Generator

Create a new object in `com.gradlelighthouse.reporting`.

```kotlin
object MyReportGenerator {
    fun generate(
        moduleName: String,
        allIssues: List<AuditIssue>
    ): String {
        // Build your report string (CSV, XML, Markdown, etc.)
        return "..."
    }
}
```

## 2. Wire it to the Task

Update the `LighthouseTask.execute()` function.

```kotlin
// LighthouseTask.kt
@TaskAction
override fun execute() {
    // ... audit logic ...

    val myContent = MyReportGenerator.generate(name, allIssues)
    val myFile = File(outputDir, "${name}-report.txt")
    myFile.writeText(myContent)
}
```

## 3. Configuration

If the report should be optional, add a toggle to `LighthouseExtension.kt` (e.g., `enableMyReport`).

## Design Guidelines

* **Portability**: The report should not depend on absolute file paths if it's meant to be shared across environments. Use paths relative to `context.rootDir`.
* **Escaping**: If your format is XML or HTML, use the `esc()` helper to prevent injection or breakage.
* **Performance**: Avoid deep object traversals during report generation. Use the already-calculated `ScoringResult` if you need scores or ranks.
