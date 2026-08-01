# The Baseline System

The Baseline System allows teams to adopt Gradle Lighthouse in large, existing codebases without being overwhelmed by technical debt.

## The Problem: Legacy Noise
When applying Lighthouse to a mature project, you might see hundreds of "Warnings" or "Errors" related to years of historical debt. This "noise" can make it difficult for developers to see **new** architectural regressions in their Pull Requests.

## The Solution: Record and Suppress
Lighthouse allows you to record all current findings into a baseline file. Once recorded, these specific instances of issues are suppressed from terminal output and CI build gates.

### 1. Create a baseline
Run the following task to record the current state:

```bash
./gradlew lighthouseRecordBaseline
```

This generates a `lighthouse-baseline.txt` file in your root project (or module directory).

### 2. Commit the baseline
Add the baseline file to your Version Control (Git). This ensures every developer on the team shares the same "clean slate."

### 3. Focus on the Delta
Future runs of `./gradlew lighthouseAudit` will only flag **new** issues introduced after the baseline was created.

## How it works: Fingerprinting
Lighthouse identifies issues using a stable fingerprinting algorithm:
`Category + Title + FilePath (Relative)`

* If you fix a baselined issue, it is automatically removed from the report.
* If you move a file, the fingerprint changes, and the issue will reappear (promoting "Clean Refactoring").
* If you introduce the same type of issue in a new file, it will be flagged as a new violation.

## Strategy: Ratcheting Quality
We recommend a "Rachet" strategy:
1. Baseline all existing debt today.
2. Enforce "Zero New Issues" in your CI pipeline using `failOnSeverity`.
3. Periodically run a "Baseline Cleanup" sprint to resolve debt and re-record the baseline.
