# TTCN Promotion Android SDK

## Overview

**TTCN Promotion Android SDK** (`ttcn-promotion-android-sdk`) is an Android library that helps host applications add **promotion-related** capabilities (configuration, data access, feature logic, and user-facing promotion screens).

The SDK is delivered as a **single Android Archive (AAR)** built from the **`vds-promotion`** Gradle module. That artifact bundles **both** non-UI foundation code and UI code: they live in separate **Java/Kotlin package trees** inside the same module, not in separate library modules.

> This README describes the current SDK planning and structure. It may be updated as the SDK public APIs, integration method, and release process are finalized.

---

## Project structure

Repository layout (Gradle / tooling; `build/` omitted):

```text
ttcn-promotion-android-sdk/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
├── app/                              # Sample Android application (local dev / demo)
│   └── build.gradle.kts
└── vds-promotion/                    # Android library module — SDK source & AAR output
    ├── build.gradle.kts
    ├── consumer-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/ttcn/promotionsdk/
        │   ├── core/                 # Foundation layer (packages only; not a separate module)
        │   └── ui/                   # UI layer (packages only; not a separate module)
        └── res/
```

**Gradle modules (verified in `settings.gradle.kts`):**

| Module           | Type        | Role |
|-----------------|-------------|------|
| `vds-promotion` | Android library | **SDK implementation**; produces the **AAR** consumed by host apps. |
| `app`           | Android application | **Optional** in-repo sample host; not part of the shipped SDK artifact. |

The **promotion “Core” vs “UI” split** is expressed only as **`com.ttcn.promotionsdk.core`** and **`com.ttcn.promotionsdk.ui`** under `vds-promotion/src/main/java/`.

---

## Module planning: `vds-promotion`

- **Single library module:** All SDK Kotlin/Java sources and `res/` for the SDK live under `vds-promotion/`.
- **Build output:** Android Gradle Plugin builds **`vds-promotion` → `*.aar`** (e.g. under `vds-promotion/build/outputs/aar/` after `assembleRelease` / `assembleDebug`, depending on your build type).
- **Manifest:** The library manifest is minimal (`vds-promotion/src/main/AndroidManifest.xml`); host apps supply the application `Activity` / theme context.

---

## Package responsibilities

The following **logical layers** map to package roots inside **`vds-promotion`** only (not separate Gradle artifacts).

### promotion-sdk-core → `com.ttcn.promotionsdk.core`

| Responsibility | Examples (from current tree) |
|----------------|------------------------------|
| Configuration & bootstrap | `core.config`, `core.di` (e.g. container / modules) |
| Data & persistence | `core.data` (remote, local, DTOs, repository implementations) |
| Domain rules | `core.domain` (models, repository contracts, use cases, exceptions) |
| Shared non-UI utilities | `core.utils` |

**Rule:** This layer should **not** depend on Android UI implementation details (screens, view binding, fragments as public contracts). It may be used **by** the UI package internally.

### promotion-sdk-ui → `com.ttcn.promotionsdk.ui`

| Responsibility | Examples (from current tree) |
|----------------|------------------------------|
| Screens & navigation hosts | `ui.feature.*` (Fragments, ViewModels, UI state) |
| Reusable widgets / styling | `ui.utils.view`, `ui.theme` |
| SDK-facing entry API | `ui.entry` (e.g. `PromotionSDK` and related options/callback types) |
| UI-oriented DI helpers | `ui.di` |

**Dependency direction (intended):**

```text
ui  ──depends on──>  core
core  ──must not──>  ui
```

---

## Build output

- **Artifact:** One **Android Library AAR** per variant from **`vds-promotion`**.
- **Contents:** Compiled **Core** and **UI** packages, merged resources, and transitive dependency metadata as declared in `vds-promotion/build.gradle.kts` (consumer rules in `consumer-rules.pro` where applicable).

There is **no** separate AAR for “core only” in this repository layout.

---

## Integration concept

- Host applications integrate **one** SDK artifact: the **AAR produced from `vds-promotion`** (or the same module via `implementation(project(":vds-promotion"))` in a composite build).
- **Maven Central / internal Maven coordinates** are **not** configured in this repository at present; publishing is **out of scope** for this README unless you add a publishing plugin and documented coordinates later.

**Illustrative dependency patterns (placeholders):**

```kotlin
// Composite / monorepo style (example only)
dependencies {
    implementation(project(":vds-promotion"))
}
```

```kotlin
// Local AAR file (example only — adjust path and variant name)
dependencies {
    implementation(files("libs/vds-promotion-release.aar"))
}
```

Public entry types for UI integration are under **`com.ttcn.promotionsdk.ui.entry`** (see source files such as `PromotionSDK.kt`); exact initialization and navigation contracts should be taken from the current source until a stable external doc is published.

---

## Development guidelines

| Guideline | Detail |
|-----------|--------|
| **Core** | Keep configuration, networking, persistence, domain rules, and repositories in `com.ttcn.promotionsdk.core`. |
| **UI** | Keep Activities, Fragments, custom views, adapters, ViewModels, and resources in `com.ttcn.promotionsdk.ui`. |
| **Dependencies** | Avoid **circular** references between `core` and `ui`; keep **`ui → core`** only. |
| **Public API** | Treat types intended for hosts (e.g. under `ui.entry`) as **stable**; avoid breaking changes without versioning policy. |
| **Host-specific logic** | Keep app branding, unrelated navigation, and business rules that belong to the host **outside** the SDK. |

---

## Tooling snapshot (verified)

| Item | Value / source |
|------|----------------|
| Root project name | `ttcn-promotion-android-sdk` (`settings.gradle.kts`) |
| Android Gradle Plugin | `8.2.0` (root `build.gradle.kts`) |
| Kotlin | `2.2.0` (root `build.gradle.kts`) |
| Library `namespace` | `com.ttcn.promotionsdk` (`vds-promotion/build.gradle.kts`) |
| `minSdk` (library) | `24` (`vds-promotion/build.gradle.kts`) |
| `compileSdk` (library) | `35` (`vds-promotion/build.gradle.kts`) |
