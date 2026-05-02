plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

// group and version are picked up from gradle.properties for JitPack compatibility

repositories {
    mavenCentral()
}

// Ensure Kotlin compiles with modern settings
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

gradlePlugin {
    plugins {
        create("lighthouse") {
            id = "com.gradlelighthouse.plugin"
            displayName = "Gradle Lighthouse: 360° Architectural Intelligence"
            description = "Enterprise-grade Gradle diagnostic engine for Android & KMP projects. " +
                "Detects R8 landmines, manifest hazards, dependency conflicts, and build bottlenecks."
            implementationClass = "com.gradlelighthouse.LighthousePlugin"
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnitPlatform()
}
