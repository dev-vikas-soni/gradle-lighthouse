# ADR-004: String-Pipe Serialization for Task Inputs

## Context
We need to pass complex dependency data from the configuration phase to the execution phase. Gradle task inputs must be serializable.

## Decision
We decided to use a **String-Pipe Serialization** format (`"group|name|version"`) passed as a `List<String>`.

## Consequences
* **Positive**: Simple, no dependencies on serialization libraries like Jackson. Fast and memory efficient for Gradle's internal serialization.
* **Negative**: Brittle if library coordinates contain the pipe character (extremely rare). Requires manual parsing logic in the task.

## Alternatives Considered
* **Kotlin Serialization**: *Rejected* because it adds a heavy runtime dependency.
* **Java Serializable POJOs**: *Rejected* because they are harder to maintain across Gradle versions.
