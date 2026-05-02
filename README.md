# Gradle Lighthouse: 360° Architectural Intelligence for Android & KMP

![Version: 1.0.0](https://img.shields.io/badge/Version-1.0.0--Enterprise-orange.svg)
![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=flat&logo=kotlin&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-8.x--Ready-green.svg)

**Gradle Lighthouse** is an enterprise-grade Gradle diagnostic engine designed to eliminate the **Silent Killers** of modern Android and Kotlin Multiplatform development. Optimized for 100+ module ecosystems, it provides **Hierarchical Dashboarding**, **SARIF/JUnit Integration**, and **Executive ROI Prescriptions**.

---

## 🚀 Installation (via JitPack)

### 1. Root Configuration
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
    id("com.gradlelighthouse.plugin") version "1.0.0"
}
```

### 2. Module Configuration
Apply the plugin to all sub-modules you wish to audit. For example, in a module's `build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application") // or com.android.library / org.jetbrains.kotlin.multiplatform
    id("com.gradlelighthouse.plugin")
}
```

---

## 🛠️ Configuration Options (DSL)

Customize the strictness and behavior of the audit using the `lighthouse` block in any `build.gradle.kts` file where the plugin is applied:

```kotlin
lighthouse {
    // === Core Toggles ===
    enableBuildSpeed.set(true)         // Audits KAPT usage, caching, jetifier
    enableAppSize.set(true)            // Checks minification, resources
    enableStabilityCheck.set(true)     // R8 missing keep rules, reflection hazards
    enableModernizationCheck.set(true) // XML vs Compose ratios
    enableKmpCheck.set(true)           // Cross-platform structure validation
    enablePlayPolicy.set(true)         // AndroidManifest.xml Play Store compliance
    
    // === CI/CD Integration ===
    failOnSeverity.set("FATAL")        // Options: NONE, INFO, WARNING, ERROR, FATAL
    enableSarifReport.set(true)        // Generate SARIF for GitHub Security Tab
    enableJunitXmlReport.set(true)     // Generate JUnit for Jenkins/Bitrise Test Tabs
}
```

---

## 🏛️ Intelligence Capabilities

### 1. 🚀 Build Speed & Engineering Efficiency
*   **KSP ROI Engine**: Scans your dependencies for libraries (Room, Hilt, Glide, etc.) still using legacy KAPT and calculates the exact **"Hours Saved/Year"** if you migrate to KSP.
*   **Gradle Best Practices**: Validates your `gradle.properties` for critical performance flags like `org.gradle.caching`, `org.gradle.parallel`, and warns if the expensive **Jetifier** is enabled.
*   **Parallelism Engineering**: Quantifies the potential for parallel builds across massive multi-module graphs.

### 2. 🛡️ Stability & "Invisible Landmine" Detection
*   **R8/ProGuard Safety**: Detects if reflection-heavy libraries (Gson, Retrofit, Moshi) are missing their required `-keep` rules, which would cause silent crashes in signed release builds.
*   **Manifest Safety (API 31+)**: Scans `AndroidManifest.xml` to ensure all components with intent-filters explicitly declare `android:exported`, preventing immediate crashes on Android 12+ devices.
*   **Conflict Intelligence**: Detects **"Silent Major Version Jumps"** where Gradle's conflict resolution forces a binary-incompatible version of a library into your APK.

### 3. 🎨 Modernization & Technical Debt
*   **Modernization Index**: Calculates the ratio of **Jetpack Compose vs. Legacy XML** layouts. It flags "XML Monoliths" that increase maintenance costs.
*   **Catalog Migration**: Audits your project for readiness to move from hardcoded dependencies to **Gradle Version Catalogs (TOML)**.
*   **KMP Ready**: Fully supports Kotlin Multiplatform project structures, auditing `commonMain` and native source sets.

### 4. 📦 App Size & Resource Optimization
*   **Shrinking Validation**: Verifies if `isMinifyEnabled` and `isShrinkResources` are active for release builds.
*   **Resource Audit**: Flags "Legacy Drawable Overuse" (bitmaps in default folders) and detects unoptimized assets over 2MB.

### 5. ⚡ Startup Performance (TTI Killers)
*   **Heavy SDK Detection**: Identifies libraries (Firebase, AdMob, Segment) that perform synchronous I/O or network calls during app startup.
*   **ContentProvider Audit**: Scans for "magic" auto-initialization providers that bloat the **Main Thread** before the user even sees the first frame.

### 6. 🏛️ Compliance & Play Store Readiness
*   **Dangerous Permission Scanner**: Flags permissions like `READ_SMS` or `MANAGE_EXTERNAL_STORAGE` that trigger strict Google Play manual reviews or automated rejections.
*   **Target SDK Compliance**: Ensures the project meets the latest Play Store requirements.

---

## 📊 Reporting Interface

| Command | Output | Purpose |
| :--- | :--- | :--- |
| `./gradlew lighthouseAudit` | `module-index.html` | Detailed per-module architectural audit. |
| `./gradlew lighthouseAggregate` | `project-dashboard.html` | **The Global Dashboard**: Aggregated "Intelligence Map" with ROI prescriptions. |

### 🔒 CI/CD & Security Integration
V1.0.0 introduces industry-standard reporting for automation:
*   **SARIF v2.1.0**: Native integration with **GitHub Security Tab** for inline PR annotations.
*   **JUnit XML**: View architectural violations as test failures in Jenkins, GitLab, or Azure DevOps.

---

## 🏛️ Professional Support & Licensing
Developed by the **Gradle Lighthouse** team for the global Android Architect community. Distributed under the MIT License.

> "The difference between a legacy app and a scaleable one is 50ms of build time compounded over 100 modules."
