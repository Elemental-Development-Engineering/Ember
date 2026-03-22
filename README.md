# Ember — Elemental Diagnostics Library

A privacy-first, offline-first diagnostics library for Android apps. Ember lets apps record lightweight diagnostic events locally on-device, build human-readable bug reports, and export them only when the user explicitly chooses to.

## Privacy Philosophy

- **No silent data export** — nothing is sent off-device automatically
- **User control** — reports are generated locally; the user reviews before sharing
- **Minimal data collection** — only technical diagnostics needed for debugging
- **App-specific redaction** — each app can sanitize or omit sensitive values
- **No internet permission** — the library does not require network access

## Module Breakdown

| Module | Purpose |
|--------|---------|
| `:diagnostics` | Core domain logic — logging, storage, redaction, report building, export |
| `:diagnostics-ui` | Jetpack Compose UI — settings card, report preview screen, share/copy actions |
| `:sample-app` | Reference integration demonstrating full usage |

## Installation

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":diagnostics"))
    implementation(project(":diagnostics-ui"))  // optional, for Compose UI
}
```

## Testing Locally In Another App

For day-to-day local testing against a real app before publishing, prefer a composite build.
It gives you the fastest edit-sync-debug loop and does not require publishing artifacts first.

In the consuming app's `settings.gradle.kts`:

```kotlin
includeBuild("../elemental-ember") {
    dependencySubstitution {
        substitute(module("com.elementaldevelopment:diagnostics"))
            .using(project(":diagnostics"))
        substitute(module("com.elementaldevelopment:diagnostics-ui"))
            .using(project(":diagnostics-ui"))
    }
}
```

Then in the consuming app module:

```kotlin
dependencies {
    implementation("com.elementaldevelopment:diagnostics:0.1.0")
    implementation("com.elementaldevelopment:diagnostics-ui:0.1.0") // optional
}
```

If you want to test the exact artifact-based install flow before publishing to a remote Maven repository,
you can also publish locally to `mavenLocal()` from this repo:

```bash
./gradlew publishLibrariesToMavenLocal
```

Then add `mavenLocal()` ahead of `mavenCentral()` in the consuming app's repository list:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
}
```

And consume the same coordinates:

```kotlin
dependencies {
    implementation("com.elementaldevelopment:diagnostics:0.1.0")
    implementation("com.elementaldevelopment:diagnostics-ui:0.1.0") // optional
}
```

## Initialization

Initialize diagnostics once during app startup:

```kotlin
val diagnostics = Diagnostics.create(
    context = applicationContext,
    config = MyAppDiagnosticsConfig(),
)
```

A session-start entry is automatically logged, and an ephemeral session ID is generated for the current app process.

## Configuration

Implement `DiagnosticsConfig` for your app:

```kotlin
class MyAppConfig : DiagnosticsConfig {
    override val appName = "MyApp"
    override val appId = "com.example.myapp"
    override val supportEmail = "support@example.com"
    override val maxStoredEntries = 300
    override val includeDeviceModelByDefault = true
    override val includeOsVersionByDefault = true

    override val redactor = DiagnosticsRedactor { input ->
        input.replace(Regex("/storage/emulated/0/[^\\s]+"), "[REDACTED_PATH]")
    }

    override fun additionalMetadata(): Map<String, String> = emptyMap()
}
```

## Logging

```kotlin
// Simple info log (defaults to INFO level)
diagnostics.logger.log("Parser", "Document opened")

// Explicit severity
diagnostics.logger.log(DiagnosticLevel.WARN, "Parser", "Unsupported syntax")

// Error with optional throwable
diagnostics.logger.logError("Parser", "Render failed", exception)

// Safe execution wrapper
diagnostics.logger.runCatchingLogged("Timer", "schedule") {
    scheduleNotification()
}
```

## Bug Report Preview Integration

Add the settings card to your Settings screen:

```kotlin
DiagnosticsSettingsCard(
    onSendBugReport = { showPreview = true },
)
```

Show the preview screen:

```kotlin
BugReportPreviewScreen(
    diagnostics = diagnostics,
    onCopy = { text -> exportHandler.copyToClipboard(text) },
    onShare = { text -> exportHandler.shareReport(text) },
)
```

## Redaction Examples

**General utility app** (PlainMark, PlainTimers):
```kotlin
val redactor = DiagnosticsRedactor { input ->
    input.replace(Regex("/storage/emulated/0/[^\\s]+"), "[REDACTED_PATH]")
}
```

**Highly sensitive app** (Private Vault):
```kotlin
val redactor = DiagnosticsRedactor { input ->
    input
        .replace(Regex("/storage/emulated/0/[^\\s]+"), "[REDACTED_PATH]")
        .replace(Regex("\"[^\"]+\""), "\"[REDACTED_NAME]\"")
        .replace(Regex("(?i)(note|title|name|content):\\s*\\S+"), "$1: [REDACTED]")
}
```

## Report Format

Reports are plain text with a stable layout:

```text
Elemental Diagnostics Report
Format Version: 1
Library Version: 0.1.0

App
- Name: MyApp
- App ID: com.example.myapp
- Version: 1.0.0 (1)

Environment
- Android: 15 / API 35
- Device: Google Pixel 8
- Time: 2026-03-19T17:10:00-07:00
- Session: a1b2c3d4-...

User Note
App freezes when opening a large file.

Recent Diagnostics
[2026-03-19T17:08:15-07:00] INFO System: Diagnostics initialized
[2026-03-19T17:08:16-07:00] INFO Parser: parse started
[2026-03-19T17:08:18-07:00] ERROR Parser: OutOfMemoryError while rendering
  Exception: OutOfMemoryError: Java heap space
```

## Testing

Run core unit tests:
```bash
./gradlew :diagnostics:test
```

Run UI tests (requires emulator or device):
```bash
./gradlew :diagnostics-ui:connectedAndroidTest
```

## v1 Non-Goals

These are explicitly out of scope for v1:

- No persistent storage (in-memory ring buffer only)
- No coroutines in the core module
- No automatic remote crash reporting
- No full stack traces by default
- No analytics or telemetry
- No screenshot capture
- No Firebase/Crashlytics integration

## Versioning

- `0.x` while APIs are evolving
- `1.0.0` once integration patterns are stable
- Reports include `formatVersion` for forward compatibility

## Release Checklist

Before release, verify:

- [ ] No internet permission introduced by library
- [ ] No telemetry SDKs in dependency tree
- [ ] No forbidden metadata collected (Android ID, IMEI, etc.)
- [ ] All unit tests pass
- [ ] Sample app builds and runs correctly
- [ ] End-to-end redaction test passes
- [ ] Public API reviewed for stability
- [ ] All implementation classes marked `internal`
- [ ] Report preview works before export
- [ ] Logs clear automatically after successful export
