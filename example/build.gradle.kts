plugins {
    id("java-library")
    id("com.gradlelighthouse.plugin") version "1.0.0"
}

// 1. DANGEROUS REPO: Should trigger JCenter Sunset Error
repositories {
    jcenter()
}

dependencies {
    // 2. HEAVY LIBRARY: Should trigger the modern Kotlin conversion warning
    implementation("com.google.guava:guava:31.1-jre")

    // 3. API LEAKAGE: Should trigger the Gradle build speed incremental leakage warning
    api("org.apache.commons:commons-lang3:3.12.0")

    // 4. DYNAMIC VERSIONING: Should trigger the non-deterministic build warning
    implementation("com.squareup.okhttp3:okhttp:4.+")
}

lighthouse {
    enablePlayPolicy.set(true)
    enableDependencyHealth.set(true)
    enableCatalogMigration.set(true)
}
