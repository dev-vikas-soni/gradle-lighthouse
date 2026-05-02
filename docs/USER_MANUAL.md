# User Manual: Gradle Lighthouse

Welcome to Gradle Lighthouse. This manual is designed for Android and Kotlin Multiplatform developers who are utilizing the plugin to secure and optimize their codebase.

---

## 1. Installation

### 1.1 Root Configuration
In your root `settings.gradle.kts`, ensure you have access to the JitPack repository:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    resolutionStrategy {
        eachPlugin {
            if (target.id.id == "com.gradlelighthouse.plugin") {
                useModule("com.github.dev-vikas-soni:gradle-lighthouse:${requested.version}")
            }
        }
    }
}
```

In your root `build.gradle.kts`:
```kotlin
plugins {
    id("com.gradlelighthouse.plugin") version "2.0.0"
}
```

### 1.2 Module Configuration
Apply the plugin to the application and library modules you wish to audit:
```kotlin
plugins {
    id("com.android.application")
    id("com.gradlelighthouse.plugin")
}
```

---

## 2. Configuration Options (DSL)

You can customize the strictness and behavior of the audit using the `lighthouse` block in any `build.gradle.kts` file where the plugin is applied.

```kotlin
lighthouse {
    // === Core Toggles ===
    enableBuildSpeed.set(true)        // Audits KAPT usage, caching, jetifier
    enableAppSize.set(true)           // Checks minification, resources
    enableStabilityCheck.set(true)    // R8 missing keep rules, reflection hazards
    enableModernizationCheck.set(true) // XML vs Compose ratios
    enableKmpCheck.set(true)          // Cross-platform structure validation
    enablePlayPolicy.set(true)        // AndroidManifest.xml Play Store compliance
    
    // === CI/CD Integration ===
    failOnSeverity.set("FATAL")       // Options: NONE, INFO, WARNING, ERROR, FATAL
    enableSarifReport.set(true)       // Generate SARIF for GitHub
    enableJunitXmlReport.set(true)    // Generate JUnit for Jenkins/Bitrise
}
```

---

## 3. Running the Audits

### 3.1 Local Module Audit
To audit a specific module, run:
```bash
./gradlew :app:lighthouseAudit
```
**Output:** An HTML report is generated at `app/build/reports/lighthouse/app-index.html`.

### 3.2 Global Dashboard
To audit the entire multi-module project and aggregate the findings into a single executive dashboard:
```bash
./gradlew lighthouseAggregate
```
**Output:** An HTML report is generated at `build/reports/lighthouse/global-dashboard.html`.

---

## 4. CI/CD Integration

### 4.1 GitHub Actions (Security Tab)
You can configure GitHub Actions to read the SARIF report and annotate your Pull Requests with architectural violations:

```yaml
steps:
  - name: Run Gradle Lighthouse
    run: ./gradlew lighthouseAggregate

  - name: Upload SARIF report
    uses: github/codeql-action/upload-sarif@v2
    if: always()
    with:
      sarif_file: build/reports/lighthouse/global-sarif.json
      category: gradle-lighthouse
```

### 4.2 Jenkins / Bitrise (JUnit XML)
Configure your CI test parser to read the generated XML files. Gradle Lighthouse outputs JUnit XML matching the standard Apache Surefire schema.
Path: `build/reports/lighthouse/module-junit.xml`

---

## 5. Understanding the ROI Engine

When viewing the HTML Dashboard, pay close attention to the **Impact Analysis** and **ROI** fields.
Lighthouse doesn't just tell you what's wrong; it tells you *why* fixing it matters:
*   **"Save 150h/year"**: Represents calculated time saved by migrating away from KAPT to KSP based on your module count.
*   **"Crash Prevention"**: Indicates a missing ProGuard rule that will cause a `ClassNotFoundException` in production.
