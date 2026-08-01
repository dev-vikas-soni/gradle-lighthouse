# ADR-002: Use of Provider API for Lazy Configuration

## Context
Massive multi-module projects suffer from slow configuration times. If a plugin resolves configurations or walks the filesystem during configuration, it blocks the developer.

## Decision
We decided that every property in the `LighthouseExtension` and task inputs in `LighthouseTask` must use the Gradle **Provider/Property API**.

## Consequences
* **Positive**: Configuration is deferred until the task is actually executed. IDE sync remains responsive.
* **Negative**: The code is slightly more complex due to `.get()`, `.set()`, and `.map()` calls.

## Alternatives Considered
* **Eager String/File types**: *Rejected* because they force calculations to happen early.
