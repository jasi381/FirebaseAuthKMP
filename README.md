# Firebase Auth + Calling — KMP

A **Kotlin Multiplatform** app with **Firebase Authentication**, **Firestore**, **Realtime Database** signaling, and **Agora video/audio calling** — all from a shared codebase using [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) and the **experimental SwiftPM integration** for iOS.

> **Experimental Notice**
> This project uses several cutting-edge features:
> - **Kotlin SwiftPM Import** — Consuming Swift Package Manager dependencies directly from Kotlin/Native ([experimental](https://kotlinlang.org/docs/multiplatform/multiplatform-spm-import.html))
> - **Kotlin 2.3.x (Titan)** — Pre-release Kotlin build (`2.3.20-titan-222`)

> This is a **proof-of-concept** and **learning resource** — not a production template.

## Features

- Email/password sign up & sign in
- Google Sign-In (Credential Manager on Android, Google Sign-In SDK on iOS)
- Password reset via email
- Auth state persistence across app launches
- User listing via Cloud Firestore
- **Video & audio calling** via Agora RTC
- **In-app call signaling** via Firebase Realtime Database (no push notifications needed)
- Incoming call overlay with accept/reject
- Auto-timeout on unanswered calls (30s)
- Shared ViewModel & UI across platforms
- Dark-themed UI with animations

## Architecture


<p align="center">
  <img src="https://github.com/user-attachments/assets/3a0742de-16c5-484f-8a7c-81f4dcb80c93" width="600"/>
  <br/>
</p>

### How Calling Works

```
1. Caller taps call button
   └─► Writes invite to /calls/{receiverUid} in Realtime DB
   └─► Joins Agora channel

2. Receiver's app has real-time listener on /calls/{myUid}
   └─► Shows full-screen IncomingCallOverlay (accept / reject)

3. Accept → Joins same Agora channel → Both connected
   Reject → Clears invite from Realtime DB
   Timeout → Auto-clears after 30 seconds

4. Channel name = sorted(callerUid, receiverUid).joinToString("_")
   └─► Deterministic — same channel regardless of who calls whom
```

### expect/actual Pattern

All platform-specific code uses Kotlin's `expect`/`actual` mechanism:

| Common (expect) | Android (actual) | iOS (actual) |
|---|---|---|
| `FirebaseAuth` | Firebase SDK + Credential Manager | FIRAuth + GIDSignIn via SwiftPM |
| `FirebaseFirestoree` | FirebaseFirestore SDK | FIRFirestore via SwiftPM |
| `CallSignaling` | Firebase Realtime Database SDK | FIRDatabase via SwiftPM |
| `CallService` | CallActivity with Agora RTC | UIViewController with Agora RTC |

## Tech Stack

| Layer | Technology |
|---|---|
| Shared UI | Compose Multiplatform 1.10.0 + Material3 |
| Shared Logic | Kotlin 2.3.x, Coroutines, ViewModel, StateFlow |
| Auth | Firebase Auth (Android SDK / iOS SwiftPM) |
| Database | Cloud Firestore (user data) + Realtime DB (call signaling) |
| Calling | Agora RTC SDK 4.6.2 (video + audio) |
| iOS Deps | SwiftPM (Firebase, GoogleSignIn, Agora) |
| Architecture | expect/actual, callbackFlow, StateFlow |

## Project Structure

```
composeApp/src/
├── commonMain/                    # Shared code
│   ├── App.kt                    # Navigation + global incoming call overlay
│   ├── AuthViewModel.kt          # Auth state management (StateFlow)
│   ├── AuthUser.kt               # User data class
│   ├── FirebaseAuth.kt           # expect — auth interface
│   ├── FirebaseFirestoree.kt     # expect — Firestore interface
│   ├── CallService.kt            # expect — start video/audio calls
│   ├── CallSignaling.kt          # expect — Realtime DB signaling + CallInvite model
│   └── ui/
│       ├── LoginScreen.kt
│       ├── SignUpScreen.kt
│       ├── HomeScreen.kt         # User list with call buttons
│       ├── IncomingCallOverlay.kt # Full-screen accept/reject UI
│       └── AuthComponent.kt
├── androidMain/                   # Android implementations
│   ├── FirebaseAuth.kt           # Credential Manager + Firebase Auth
│   ├── FirebaseFirestoree.kt     # FirebaseFirestore SDK
│   ├── CallSignaling.kt          # Firebase Realtime Database
│   ├── CallService.kt            # Launches CallActivity
│   ├── CallActivity.kt           # Agora RTC + programmatic UI
│   └── MainActivity.kt
└── iosMain/                       # iOS implementations
    ├── FirebaseAuth.kt           # GIDSignIn + FIRAuth via SwiftPM
    ├── FirebaseFireStoree.kt     # FIRFirestore via SwiftPM
    ├── CallSignaling.kt          # FIRDatabase via SwiftPM
    ├── CallService.kt            # Agora RTC + AgoraRtcEngineDelegate
    └── MainViewController.kt

iosApp/                            # Xcode project entry point
├── iosApp/
│   ├── iOSApp.swift              # FirebaseApp.configure() + Google Sign-In URL handler
│   ├── ContentView.swift         # Compose-to-UIViewController bridge
│   └── Info.plist                # Permissions (camera, mic) + Google client ID
└── _internal_linkage_SwiftPMImport/  # Auto-generated SwiftPM linkage
```

## Prerequisites

- Android Studio (with KMP plugin)
- Xcode 16+
- A Firebase project with:
  - Android app registered (package: `dev.jasmeetsingh.firebaseauth`)
  - iOS app registered
  - **Authentication > Email/Password** enabled
  - **Authentication > Google** enabled
  - **Cloud Firestore** database created
  - **Realtime Database** created (for call signaling)
- An [Agora](https://www.agora.io/) account with an App ID

## Setup (Fresh Clone)

### Step 1 — Clone & open

```shell
git clone <repo-url>
```

Open the project in **Android Studio** (with KMP plugin installed).

### Step 2 — Create a Firebase project

1. Go to [Firebase Console](https://console.firebase.google.com/) → **Create a project**
2. **Register an Android app** with package name `dev.jasmeetsingh.firebaseauth`
   - Download `google-services.json` → place in `composeApp/`
3. **Register an iOS app** with bundle ID `dev.jasmeetsingh.firebaseauth.FirebaseAuthApp`
   - Download `GoogleService-Info.plist` → place in `iosApp/iosApp/`
4. Enable these in Firebase Console:
   - **Authentication → Sign-in method → Email/Password** ✅
   - **Authentication → Sign-in method → Google** ✅
   - **Cloud Firestore → Create database** (test mode is fine to start)
   - **Realtime Database → Create database** → use these rules:

```json
{
  "rules": {
    "calls": {
      "$uid": {
        ".read": "auth != null && auth.uid == $uid",
        ".write": "auth != null"
      }
    }
  }
}
```

### Step 3 — Create an Agora account

1. Go to [Agora Console](https://console.agora.io/) → Create a project
2. Choose **"Testing mode: App ID"** (no certificate)
3. Copy the **App ID** and update it in:

```
composeApp/src/commonMain/kotlin/.../CallService.kt
```

```kotlin
const val AGORA_APP_ID = "your-agora-app-id-here"
```

### Step 4 — iOS configuration

**4a. Update Info.plist** (`iosApp/iosApp/Info.plist`):

Open your downloaded `GoogleService-Info.plist` and copy these values:

| Info.plist key | Value from GoogleService-Info.plist |
|---|---|
| `GIDClientID` | `CLIENT_ID` |
| `CFBundleURLSchemes` | `REVERSED_CLIENT_ID` |

Camera & mic permissions are already in the template.

**4b. Set your Team ID** in `iosApp/Configuration/Config.xcconfig`:

```
TEAM_ID=YOUR_APPLE_TEAM_ID
```

(Find it in Xcode → Settings → Accounts → your team)

**4c. SwiftPM linkage** (one-time, after first Gradle sync):

```shell
XCODEPROJ_PATH='iosApp/iosApp.xcodeproj' ./gradlew :composeApp:integrateLinkagePackage
```

Then resolve packages:

```shell
xcodebuild -resolvePackageDependencies -project iosApp/iosApp.xcodeproj -scheme iosApp
```

### Step 5 — Build & Run

**Android:**

Select the Android run configuration in Android Studio → Run, or:

```shell
./gradlew :composeApp:assembleDebug
```

**iOS:**

Open `iosApp/iosApp.xcodeproj` in Xcode → select your real device → ⌘R

Or use the iOS run configuration in Android Studio.

> **Note:** iOS simulator on Intel Macs is not supported with SwiftPM dependencies. Use a real device.

## Gotchas

| Problem | Fix |
|---|---|
| `PRODUCT_NAME` collision with Firebase module names | Rename in `Config.xcconfig` to `FirebaseAuthApp` (already done) |
| `Missing package product '_internal_linkage_SwiftPMImport'` | Run `xcodebuild -resolvePackageDependencies` |
| `integrateLinkagePackage` needed | Run after adding/changing SwiftPM deps in `build.gradle.kts` |
| Intel Mac — `iosX64()` fails with SwiftPM | Don't add `iosX64()`. Use a real iOS device instead |
| Agora "no token" mode | Fine for testing. For production, enable token auth in Agora Console |
| `xcrun: error: missing DEVELOPER_DIR` | Run `sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer` |
| iOS buttons not working after a while | Kotlin/Native GC — ensure strong references to handlers (already handled) |

## Demo

https://github.com/user-attachments/assets/94bcb620-966d-4923-8185-a07a03b90a1a
