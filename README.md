# Firebase Auth KMP

A **Kotlin Multiplatform** sample app demonstrating Firebase Authentication on both **Android** and **iOS** from a shared codebase — including **Google Sign-In** — using [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) for the UI and the **experimental SwiftPM integration** for iOS Firebase dependencies.

> **Experimental Notice**
> This project uses several cutting-edge features that are still in experimental/beta stages:
> - **Kotlin SwiftPM Import** — The ability to consume Swift Package Manager dependencies directly from Kotlin/Native is [experimental](https://kotlinlang.org/docs/multiplatform/multiplatform-spm-import.html). APIs and Gradle DSL may change in future Kotlin releases.
> - **Kotlin 2.3.x (Titan)** — This project uses a pre-release Kotlin build (`2.3.20-titan-222`) from JetBrains. It is not a stable release and may contain bugs or breaking changes.

> This project is meant as a **proof-of-concept** and **learning resource** — not a production template. Things may break as these APIs evolve.

## Features

- Email/password sign up & sign in
- Google Sign-In (Credential Manager on Android, Google Sign-In SDK on iOS)
- Password reset via email
- Auth state persistence across app launches
- Shared ViewModel & UI across platforms
- Dark-themed UI with animations

## Tech Stack

| Layer | Technology |
|---|---|
| Shared UI | Compose Multiplatform 1.10.0 + Material3 |
| Shared Logic | Kotlin 2.3.x, Coroutines, ViewModel |
| Android Auth | Firebase Auth + Credential Manager |
| iOS Auth | Firebase iOS SDK (SwiftPM) + Google Sign-In SDK (SwiftPM) |
| Architecture | expect/actual pattern, StateFlow, callbackFlow |

## Project Structure

```
composeApp/src/
├── commonMain/          # Shared code
│   ├── FirebaseAuth.kt  # expect class — platform-agnostic auth interface
│   ├── AuthUser.kt      # Data class for authenticated user
│   ├── AuthViewModel.kt # ViewModel with auth state management
│   ├── App.kt           # Navigation & screen routing
│   └── ui/
│       ├── LoginScreen.kt
│       ├── SignUpScreen.kt
│       ├── HomeScreen.kt
│       └── AuthComponent.kt
├── androidMain/         # Android actual implementations
│   ├── FirebaseAuth.kt  # Credential Manager + Firebase Auth
│   └── MainActivity.kt
└── iosMain/             # iOS actual implementations
    └── FirebaseAuth.kt  # GIDSignIn + FIRAuth via SwiftPM

iosApp/                  # Xcode project entry point
├── iOSApp.swift         # FirebaseApp.configure() + Google Sign-In URL handler
└── ContentView.swift    # Compose-to-UIViewController bridge
```

## Prerequisites

- Android Studio (with KMP plugin)
- Xcode 16+
- A Firebase project with:
  - Android app registered (package: `dev.jasmeetsingh.firebaseauth`)
  - iOS app registered
  - **Authentication > Sign-in method > Email/Password** enabled
  - **Authentication > Sign-in method > Google** enabled

## Setup

### 1. Clone & open in Android Studio

```shell
git clone <repo-url>
```

Open the project in Android Studio.

### 2. Firebase configuration

- Place your `google-services.json` in `composeApp/`
- Place your `GoogleService-Info.plist` in `iosApp/iosApp/`

> Make sure Google Sign-In is enabled in Firebase Console so these files contain the required OAuth client IDs.

### 3. iOS — SPM linkage (one-time)

After the first Gradle sync, run:

```shell
XCODEPROJ_PATH='iosApp/iosApp.xcodeproj' ./gradlew :composeApp:integrateLinkagePackage
```

Then resolve packages:

```shell
xcodebuild -resolvePackageDependencies -project iosApp/iosApp.xcodeproj -scheme iosApp
```

### 4. iOS — Info.plist

Ensure `iosApp/iosApp/Info.plist` contains:

- `GIDClientID` — the `CLIENT_ID` from your `GoogleService-Info.plist`
- `CFBundleURLSchemes` — the `REVERSED_CLIENT_ID` from your `GoogleService-Info.plist`

## Build & Run

### Android

Use the Android run configuration in Android Studio, or:

```shell
./gradlew :composeApp:assembleDebug
```

### iOS

Use the iOS run configuration in Android Studio, or open `iosApp/` in Xcode and run from there.

## Gotchas

**`PRODUCT_NAME` collision**: If your Xcode product name matches a Firebase module (e.g. `FirebaseAuth`), you'll get "Multiple commands produce" errors. Rename it in `Config.xcconfig` to something like `FirebaseAuthApp`.

**`integrateLinkagePackage`**: Required after adding/changing SwiftPM dependencies in `build.gradle.kts`. The error message tells you the exact command to run.

**`Missing package product '_internal_linkage_SwiftPMImport'`**: Run `xcodebuild -resolvePackageDependencies` after integrating the linkage package.


# Demo
https://github.com/user-attachments/assets/94bcb620-966d-4923-8185-a07a03b90a1a


