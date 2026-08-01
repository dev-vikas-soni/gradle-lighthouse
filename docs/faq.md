# Frequently Asked Questions

## Is Lighthouse a replacement for Detekt or Android Lint?
No. Detekt and Android Lint focus on **code-level** issues (syntax, formatting, local bugs). Lighthouse focuses on **architectural** issues (module coupling, build performance, structural hygiene). They are complementary.

## Does Lighthouse support Kotlin Multiplatform (KMP)?
Yes. Lighthouse includes a specific `KmpStructureAuditor` that verifies source set alignment and targets.

## Will Lighthouse make my build slower?
Minimal impact. Most of the heavy lifting happens in the background or during CI. The configuration phase is near-zero cost due to our Snapshot Architecture.

## Can I use Lighthouse with Gradle 7?
No. Lighthouse targets Gradle 8.0 and higher to leverage the full power of Configuration Cache and the Provider API.

## Why did my score drop suddenly?
Check the "Trend Tracking" section in your report. Usually, this happens when a new "FATAL" issue is introduced, such as a circular dependency.
