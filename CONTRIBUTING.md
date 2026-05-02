# Contributing to Gradle Lighthouse

First off, thank you for considering contributing to Gradle Lighthouse. It's people like you that make Gradle Lighthouse such a great tool for the Android and Kotlin Multiplatform community.

## Where do I go from here?

If you've noticed a bug or have a feature request, make sure to check our [Issues](https://github.com/dev-vikas-soni/gradle-lighthouse/issues) first to see if someone else has already reported it. If not, go ahead and create a new issue using one of our templates!

## Setting up your environment

1. Fork the repo and clone your fork.
2. Ensure you have JDK 17 installed.
3. Open the project in IntelliJ IDEA or Android Studio.
4. Run `./gradlew build` to ensure everything compiles.

## Making Changes

1. Create a new branch for your feature/bugfix.
2. Make your changes in the `src/main/kotlin` directory.
3. If adding a new Auditor, ensure it implements the `Auditor` interface and is registered in `LighthousePlugin.kt`.
4. Add tests for your new logic in `src/test/kotlin`.
5. Run `./gradlew check` to run Detekt and the test suite. Our pre-commit hooks will also run these checks automatically.

## Submitting a Pull Request

1. Push your branch to your fork.
2. Open a Pull Request against the `main` branch.
3. Fill out the Pull Request template completely.
4. Ensure all CI checks pass.
5. A maintainer will review your code and may request changes.

## Coding Standards

* We follow the standard Kotlin style guide.
* All new logic must be Configuration-Cache safe. Do not access `Project` directly inside Auditor execution phases; use the `AuditContext` snapshot.
* Keep auditors stateless.

Thank you for your contributions!
