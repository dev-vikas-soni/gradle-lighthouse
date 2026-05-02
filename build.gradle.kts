import org.gradle.plugin.compatibility.compatibility
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "2.1.1"
}

group = "io.github.dev-vikas-soni"
version = "2.1.0"

repositories {
    mavenCentral()
}

// Modern Kotlin compiler options (no deprecation warnings)
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll("-Xjvm-default=all")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

gradlePlugin {
    website.set("https://github.com/dev-vikas-soni/gradle-lighthouse")
    vcsUrl.set("https://github.com/dev-vikas-soni/gradle-lighthouse")

    plugins {
        create("lighthouse") {
            id = "io.github.dev-vikas-soni.lighthouse"
            displayName = "Gradle Lighthouse — Build Intelligence for Android & KMP"
            description = "Enterprise-grade Gradle diagnostic engine: health scoring, dependency analysis, " +
                "build performance auditing, module graph visualization, security scanning, " +
                "and 20+ architectural checks for Android and Kotlin Multiplatform projects. " +
                "Zero-config, SARIF + JUnit CI/CD integration, colorful terminal dashboards."
            implementationClass = "com.gradlelighthouse.LighthousePlugin"
            compatibility {
                features {
                    configurationCache.set(true)
                }
            }
            tags.set(listOf(
                "android", "kotlin", "lint", "architecture",
                "build-performance", "dependency-analysis", "health-score",
                "kmp", "multiplatform", "ci-cd", "sarif", "security"
            ))
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnitPlatform()
}
