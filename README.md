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

## Custom Dependency Injection

SDK currently uses an in-house DI container in `core.di` (no Hilt/Koin/Dagger).

### Core pieces

- `SdkDi`: service locator + registry (supports `single` and `factory` scopes)
- `module { ... }`: module DSL for dependency registration
- `PromotionContainer`: SDK-level bootstrap and access point for config and resolved dependencies
- `ComponentRegistry`: internal storage/resolution engine for DI keys/providers

### Startup flow

When host app calls `PromotionSDK.init(...)`:

1. `PromotionContainer.init(context, config)` is invoked
2. `SdkDi.start(context, config, ...)` registers base dependencies (`Context`, `PromotionSDKConfig`)
3. DI modules are loaded (e.g. `NetworkModule.module`, `RepositoryModule.module`)
4. Features resolve dependencies from container/DI instead of creating them manually

`PromotionSDKConfig.baseUrl` convention in SDK:

- Pass host root URL (e.g. `https://domain.com/`)
- Keep API path segments in `PromotionApiService` annotations
- This avoids double-path when host app config changes

### Registration pattern

Use modules under `core.di` to register dependencies:

```kotlin
object NetworkModule {
    internal val module = module {
        single<PromotionApiService> { RetrofitClient.promotionApiService() }
        single<PromotionRemoteDataSource> { PromotionRemoteDataSource(get()) }
    }
}

object RepositoryModule {
    internal val module = module {
        single<PromotionRepository> { PromotionRepositoryImpl(get()) }
    }
}
```

### Usage rules

- Register infra dependencies in modules (`ApiService`, `RemoteDataSource`, `Repository`, factories)
- Resolve dependencies through container/DI (`get<T>()`, `inject<T>()`, or `PromotionContainer` providers)
- Avoid manual creation of managed dependencies in UI/business code
  - avoid patterns like `PromotionRepositoryImpl(...)` or `RetrofitClient.promotionApiService()` in Fragment/ViewModel
- Keep DI scope consistent:
  - `single` for shared stateless/network/repository objects
  - `factory` for short-lived objects when needed
- Clear DI state on SDK release via `PromotionContainer.clear()`

### Architectural note

Current codebase allows service-locator style resolution in some places, but preferred direction is:

- constructor injection for classes with dependencies
- centralized creation via custom DI modules
- minimal direct dependency construction outside DI bootstrap

This keeps feature implementations aligned with SDK architecture and makes testing/replacement easier.

## Detailed Package Structure

The trees below reflect **`vds-promotion/src/main/java/com/ttcn/promotionsdk/`** as of this repository. **Core** and **UI** are **Kotlin/Java package directories inside the same `vds-promotion` Gradle module**; they are not separate Gradle modules or separate AARs.

### Core package structure

```text
com.ttcn.promotionsdk.core/
├── config/                         # Library-facing configuration types (e.g. environment, API settings)
├── data/
│   ├── dto/                        # Data transfer objects and mappers for API/cache layers
│   ├── local/                      # Room DAOs, database, shared preferences–backed storage
│   ├── remote/                     # Retrofit services, client, interceptors
│   └── repository/                 # Repository implementations bridging remote + local sources
├── di/                             # SDK container, module registration, and service resolution
│   └── internal/                   # Component registry and DI key types
├── domain/
│   ├── exception/                  # Domain/network/feature-flag oriented errors
│   ├── model/                      # Domain models (promotion config, feature flags, …)
│   ├── repository/                 # Repository interfaces (contracts for data access)
│   └── usecase/                    # Application use cases (get promotions, submit, flags, …)
└── utils/                          # Coroutine and date helpers shared by core layers
```

### UI package structure

```text
com.ttcn.promotionsdk.ui/
├── base/                           # Activity/Fragment base classes shared by SDK screens
├── di/                             # UI-scoped dependency wiring (e.g. ViewModel module)
├── entry/                          # Public SDK façade: init options, callbacks, navigation helpers
├── feature/
│   ├── featureflag/                # Feature-flag screen logic (ViewModel, UI state/actions)
│   └── promotion/                  # Promotion flows: shared ViewModel + feature subpackages
│       ├── choosepromotion/        # Voucher selection / payment-integrate flows
│       │   ├── adapter/            # RecyclerView adapters and list item types
│       │   └── searchmypromotion/  # Search-my-promotion screen (fragment, ViewModel, contract)
│       ├── mypromotion/            # “My promotions” list screen (fragment, ViewModel, contract)
│       └── promotiondetail/        # Promotion detail screen (fragment, ViewModel, contract)
├── theme/                          # SDK theming tokens and theme resolution
└── utils/
    ├── enum/                       # UI enumerations (button size/type, search type, …)
    ├── extension/                  # Kotlin extensions (keyboard, resources, RecyclerView, …)
    └── view/                       # Custom views, buttons, inputs, promotion-specific widgets
        └── itf/                    # Small view/input interfaces used by composite controls
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

## Theming

The SDK applies optional **component tokens** (button, search field, list row, tab chip, tab underline, discount badge). Hosts pass them when initializing the SDK and may update them at runtime.

### Initialization

```kotlin
import com.ttcn.promotionsdk.ui.entry.PromotionSDK
import com.ttcn.promotionsdk.ui.entry.PromotionSDKOptions
import com.ttcn.promotionsdk.ui.entry.PromotionTheme
import com.ttcn.promotionsdk.ui.theme.PromotionSDKTheme
import com.ttcn.promotionsdk.ui.theme.PromotionThemeConfig
import com.ttcn.promotionsdk.ui.theme.ButtonToken
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig

PromotionSDK.init(
    context,
    PromotionSDKOptions(
        config = PromotionSDKConfig(apiKey = "…", baseUrl = "…"),
        theme = PromotionSDKTheme(
            config = PromotionThemeConfig(
                buttonToken = ButtonToken(
                    backgroundColor = 0xFFE00029.toInt(),
                    textColor = 0xFFFFFFFF.toInt(),
                ),
                // … other tokens optional (null = use SDK defaults from resources)
            ),
        ),
    ),
)
```

### Runtime updates

`PromotionTheme.configure(PromotionThemeConfig)` updates the internal theme registry **and** keeps `PromotionSDK.getTheme()` in sync. Use `PromotionTheme.clear()` to reset tokens to defaults (empty config). `PromotionSDK.release()` also clears theme state.

### Persistence (optional)

`PromotionThemeJson` in the library can serialize/deserialize `PromotionThemeConfig` with Gson for simple storage. Hosts may use their own format as long as they rebuild `PromotionThemeConfig` before calling `init` or `PromotionTheme.configure`.

### Migration note (older `PromotionSDKTheme` shape)

Earlier snapshots modeled `PromotionSDKTheme` with separate `colors`, `fonts`, `icons`, and `borders` properties. That shape was **replaced** by a single `config: PromotionThemeConfig` aggregating the **token** types above. Hosts that integrated against the old fields need to map their styling into the new token properties (or rely on SDK defaults by omitting tokens).

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
