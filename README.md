# AI Photo & Video Enhancer

An offline, bilingual (English + Turkish) Android app built with Jetpack Compose that enhances photos and videos on-device. All processing is local—no internet or external AI services are required. Drop-in TensorFlow Lite models are supported via `app/src/main/assets/models/` and the app gracefully falls back to handcrafted image/video filters when models are absent.

## Features
- Home hub with quick actions for photo and video enhancement.
- Photo enhancement: before/after slider, sharpness, denoise, brightness, and contrast controls, offline processing pipeline, save/share via MediaStore.
- Video enhancement: selectable profiles (Soft Clean, Strong Sharpen, Night Boost), frame-by-frame filtering with progress and ETA, audio preserved, MP4 export.
- Settings: language (System/English/Türkçe), theme (System/Light/Dark), About + privacy notice.
- Dynamic locale switching at runtime without restart; Material 3 theming with light/dark support.

## Project Structure
- `app/` – Single Android module (package `com.ilhanakd.aiphotovideoenhancer`).
  - `ui/` – Compose screens and shared components.
  - `domain/` – Models and use cases.
  - `data/` – Preferences (DataStore) and processing helpers.
  - `ml/` – Model runner stubs for future TensorFlow Lite models with offline fallbacks.
  - `assets/models/` – Place `photo_enhancer.tflite` or `video_enhancer.tflite` here to enable on-device inference.

## Build & Run
1. Generate the Gradle wrapper JAR (the repository omits binary files): either let Android Studio sync once or run `gradle wrapper --gradle-version 8.2` with a locally installed Gradle.
2. Open the project in Android Studio (Giraffe+).
3. Ensure the Android SDK 34 and Kotlin 17 toolchain are installed.
4. Select the `app` run configuration and build/run on an emulator or device (minSdk 26).

## Offline AI & Privacy
- No network permission is requested; all enhancement happens locally on the device.
- Media stays on-device. Outputs are saved to `Pictures/AIPhotoVideoEnhancer/` and `Movies/AIPhotoVideoEnhancer/` via MediaStore.

## Localization
- Default strings in `app/src/main/res/values/strings.xml` (English).
- Turkish translations in `app/src/main/res/values-tr/strings.xml`.
- Switch languages at runtime from Settings (System/English/Türkçe).
