# Template sources

Each template that ships inside the APK Builder app is a small, **pre-compiled**
Android APK whose runtime code reads `assets/config.json` (and any optional
binary assets) and renders accordingly. The APK Builder app patches in the
user's customisation, strips ABI-specific native libs based on the user's
selection, and re-signs the APK on the device.

## Why pre-compiled templates?

Android does not allow regular apps to invoke `javac` / `d8` / `aapt2` on a
device (no JDK, no SDK build tools, blocked by SELinux and the verifier).
Every working "no-code APK builder" on the Play Store (Sketchware, AIDE,
Apk Editor Pro, etc.) uses the same template-patching pattern that this
project does.

## How to author a template

A template is a normal Android Studio module that:

1. Reads its configuration on startup:
   ```kotlin
   val cfg = JSONObject(assets.open("config.json").bufferedReader().readText())
   val primary = cfg.optInt("primaryColor", 0xFF3D5AFE.toInt())
   ```
2. Loads optional branding from `assets/branding/` if present
   (`icon.png`, `splash.png`, `audio.mp3`).
3. Targets `minSdk 21` and includes both `armeabi-v7a` + `arm64-v8a`
   (and ideally `x86` + `x86_64`) in its `ndk.abiFilters` so the builder
   can strip whichever the user doesn't want.

After building the template module, copy the resulting APK to:

```
app/src/main/assets/templates/<template-id>/template.apk
```

The APK Builder will pick it up automatically the next time it runs.

## Bundled stubs

This repo ships per-template `manifest.json` files but **no `template.apk`**
binaries — those are produced by the developer (you) once and reused by
every end-user. Build them once with Android Studio's "Build APK" command
and drop them into the matching folder above. After that, the project is
fully self-contained: end users never need a server or extra setup.

The `ApkAssembler` will throw a clear `FileNotFoundException` if a template
APK is missing, so missing templates are easy to spot during development.
