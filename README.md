# APK Builder

A no-code Android app builder that runs entirely on the device. Users pick a
template, customise it (colours, header, tabs, top/bottom bars, window size,
icon, splash, audio), preview the result, and tap **Build APK** to generate
an installable APK with a selectable ABI target (32-bit / 64-bit / Universal).

The APK Builder app itself supports `armeabi-v7a`, `arm64-v8a`, `x86`, and
`x86_64`, so it runs on every Android phone from 5.0 (Lollipop) up.

## How to build

### Android Studio

1. `File → Open…` and pick the `android-apk-builder/` folder.
2. Let Gradle sync (Android Gradle Plugin 8.2.2, Kotlin 1.9.22, Compose).
3. Click **Run ▶**.

### AIDE (on-device)

1. Copy the `android-apk-builder/` folder to your phone.
2. Open AIDE, choose **Open existing project** and select the folder.
3. AIDE will detect the Gradle structure; tap **Run / Build APK**.

> AIDE Pro is required for Kotlin + Gradle projects. If you don't have AIDE Pro,
> open the project in Android Studio first, run a debug build to populate
> `app/build/outputs/apk/`, then copy that APK to your device.

## Project layout

```
android-apk-builder/
├── settings.gradle / build.gradle / gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── app/
│   ├── build.gradle              # AGP 8.2.2, Compose, apksig, Bouncy Castle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/                  # themes, colours, launcher icon, file_paths
│       ├── assets/templates/     # per-template manifest.json + (your) template.apk
│       └── java/com/apkbuilder/app/
│           ├── MainActivity.kt
│           ├── theme/Theme.kt
│           ├── model/            # Template, ProjectConfig, AbiTarget
│           ├── builder/          # ApkAssembler, ApkInstaller, KeystoreManager, ProjectStore
│           └── ui/               # AppRoot + Templates / Editor / Preview / Build / Projects / Settings
└── template-sources/README.md    # how to author a template APK
```

## How the on-device build actually works

Real on-device Java→DEX compilation is not possible from a sandboxed Android
app — there is no JDK, no `aapt2`, and SELinux blocks `dex2oat` from arbitrary
paths. The honest, working approach (used by Sketchware, Apk Editor, etc.) is
**template patching**:

1. Each template is a pre-compiled APK shipped in `assets/templates/<id>/template.apk`.
   Its runtime reads `assets/config.json` and `assets/branding/*` to render the
   user's chosen colours, text, images, and audio.
2. When the user taps **Build APK**, `ApkAssembler`:
   * copies the template APK to the cache;
   * streams its zip entries into a new APK, replacing `assets/config.json`
     and any branding files with the user's customisations, and dropping
     `lib/<abi>/*.so` entries that don't match the chosen 32/64-bit target;
   * signs the result with a self-signed key (v1 + v2 schemes for maximum
     install compatibility) generated and persisted by `KeystoreManager`.
3. The output APK is written to `filesDir/builds/` and handed to the system
   installer via a `FileProvider`.

## What you (the developer) need to add once

The repo ships one `manifest.json` per template but **no `template.apk` binary**
— you build those yourself in Android Studio once, then drop them into
`app/src/main/assets/templates/<id>/template.apk`. See
`template-sources/README.md` for the contract every template runtime must
follow (`assets/config.json`, optional `assets/branding/*`).

After that one-time step the project is fully self-contained: end users
install the APK Builder, pick a template, customise, build — no server,
no extra setup.

## Adding more templates

1. Build a new template APK that follows the contract in `template-sources/README.md`.
2. Drop it at `app/src/main/assets/templates/<your-id>/template.apk`.
3. Add a matching entry to `app/src/main/assets/templates/manifest.json` and to
   `Template.BUILT_IN` in `model/Template.kt`.

The drag-and-drop editor and the build pipeline pick the new template up
automatically.
