# Changelog

All notable changes to Elemental Ember will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Added
- Opt-in crash diagnostics persistence with local file-backed recovery between launches
- Recovered-diagnostics support in bug reports and plain-text export
- Previous-session outcome metadata for recovered reports

### Changed
- Plain-text report format version advanced from 1 to 2
- `DiagnosticsLogger.clear()` now clears both in-memory current-session entries and recovered persisted diagnostics

### Privacy
- Crash persistence remains local-only and redacted-before-write
- Recovered diagnostics are never exported automatically

## [0.1.0] - 2026-03-24

Initial release of Elemental Ember — a privacy-first, offline-first diagnostics library for Android.

### Added
- Core diagnostics module (`com.elementaldevelopment:diagnostics`)
  - `Diagnostics.create()` factory for single-call initialization
  - `DiagnosticsLogger` with INFO, WARN, ERROR levels and attribute support
  - `DiagnosticsRedactor` with baseline whitespace normalization and composable app-defined redaction
  - `InMemoryDiagnosticsStore` ring buffer with configurable capacity
  - `BugReportBuilder` for assembling metadata, entries, and user notes into a report
  - `PlainTextExporter` producing a plain-text report format (format version 1)
  - `DefaultMetadataProvider` collecting conservative device/app info with per-field opt-out
  - `runCatchingLogged()` extension for try-catch with automatic error logging
  - `ThrowableSummarizer` for sanitized, single-line exception summaries
  - Entry trimming to enforce maximum field lengths
  - Eager redaction at write time — no raw content stored

- Compose UI module (`com.elementaldevelopment:diagnostics-ui`)
  - `DiagnosticsSettingsCard` for toggling report sections
  - `BugReportPreviewScreen` with live preview, user note input, copy, and share
  - `DiagnosticsExportHandler` for clipboard and share sheet integration
  - Deferred log clearing on app resume after share

- Sample app with reference integration for general and high-sensitivity configurations
- Local Maven publishing via `publishLibrariesToMavenLocal`
- Maven Central publishing infrastructure (POM metadata, signing, Sonatype staging)

### Privacy Guarantees
- No data leaves the device automatically
- No unique hardware identifiers collected
- All content redacted before storage
- Logs cleared after successful export

[0.1.0]: https://github.com/Elemental-Development-Engineering/Ember/releases/tag/v0.1.0
