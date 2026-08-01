plugins {
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
    kotlin("jvm") version "1.9.22"
    id("io.github.dev-vikas-soni.lighthouse") version "2.3.2"
    // id("kotlin-kapt") removed by Lighthouse AI
}

// 1. DANGEROUS REPO: Should trigger JCenter Sunset Error
repositories {
    mavenCentral()
}

dependencies {
    // 2. HEAVY LIBRARY: Should trigger the modern Kotlin conversion warning
    implementation(libs.com.google.guava.guava)

    // 3. API LEAKAGE: Should trigger the Gradle build speed incremental leakage warning
    api(libs.org.apache.commons.commons.lang3)

    // 4. DYNAMIC VERSIONING: Should trigger the non-deterministic build warning
    implementation(libs.com.squareup.okhttp3.okhttp)

    // 5. KAPT dependencies for AI migration test
    ksp(libs.androidx.room.room.compiler)

    // 6. Predictive Intelligence Test (Heavy SDK)
    implementation("com.facebook.android:facebook-android-sdk:16.0.0")
}

lighthouse {
    enablePlayPolicy.set(true)
    enableDependencyHealth.set(true)
    enableCatalogMigration.set(true)
    useAi.set(true)
    enablePredictiveIntelligence.set(true)
    enableTelemetry.set(true)
}
