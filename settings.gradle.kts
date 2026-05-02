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
rootProject.name = "gradle-lighthouse"