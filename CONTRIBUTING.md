# Contributing to Gradle Lighthouse

First off, thank you for considering contributing to Gradle Lighthouse. It's people like you that make Gradle Lighthouse such a great tool for the Android and Kotlin Multiplatform community.

## Where do I go from here?

If you've noticed a bug or have a feature request, check our [Issues](https://github.com/dev-vikas-soni/gradle-lighthouse/issues) first. If not, create a new issue!

## Setting up your environment

1. Fork the repo and clone your fork.
2. Ensure you have **JDK 17** installed.
3. Open the project in IntelliJ IDEA or Android Studio.
4. Run `./gradlew build` to ensure everything compiles and tests pass.

## Adding a New Auditor

This is the most common contribution. Follow these steps:

1. Create a class implementing `Auditor` in `src/main/kotlin/com/gradlelighthouse/auditors/`.
2. Your auditor must be **stateless** — all data comes from the `AuditContext` parameter.
3. If new data is required from the Gradle graph:
   - Add a field to `AuditContext`
   - Capture it in `LighthousePlugin.kt` during configuration phase
   - Add a serialized `@Input` property to `LighthouseTask`
4. Add a toggle `Property<Boolean>` in `LighthouseExtension` with `.convention(true)`.
5. Add the mapping in `LighthousePlugin.enabledAuditorNames` provider block.
6. Register the auditor in `LighthouseTask.buildAuditorList()`.
7. Write a test using `GradleRunner` + `@TempDir` in `src/test/kotlin/`.

## Making Changes

1. Create a new branch for your feature/bugfix.
2. Make your changes in the `src/main/kotlin` directory.
3. Run `./gradlew check` to run the test suite.
4. Run `./gradlew validatePlugins` to ensure plugin descriptors are valid.

## Coding Standards

* Follow the standard [Kotlin style guide](https://kotlinlang.org/docs/coding-conventions.html).
* All logic must be **Configuration Cache safe** — never access `Project` inside `@TaskAction` or auditors.
* Keep auditors stateless and pure: `AuditContext → List<AuditIssue>`.
* Use plain text in `AuditIssue` fields — HTML escaping is handled by report generators.
* New categories? Update `JunitXmlReportGenerator` and `HtmlReportGenerator` category lists.

## Submitting a Pull Request

1. Push your branch to your fork.
2. Open a Pull Request against the `main` branch.
3. Fill out the Pull Request template completely.
4. Ensure all CI checks pass (build + validatePlugins).
5. A maintainer will review your code and may request changes.

## Publishing

Only maintainers can publish to Gradle Plugin Portal:

```bash
export GRADLE_PUBLISH_KEY=xxx
export GRADLE_PUBLISH_SECRET=xxx
./gradlew publishPlugins
```

Thank you for your contributions!
