# NTSAMAISE Android Prototype — Codex Project Context

## 1. Project Overview

**Application Name:** NTSAMAISE
**Package:** `com.moeketsifaso.ntsamaise`
**Platform:** Android
**Language:** Kotlin
**UI:** Jetpack Compose
**IDE:** Android Studio

NTSAMAISE is an AI-based Android assistive application prototype designed for blind and partially sighted users in Lesotho.

The application uses the smartphone camera, computer vision, OCR, and speech technologies to assist users with selected everyday visual tasks.

The application is a research prototype rather than a production/commercial application.

---

## 2. Main Prototype Goal

Develop an accessible Android application capable of providing:

1. General object recognition
2. Lesotho Loti banknote recognition
3. Selected local product/logo recognition
4. Printed-text recognition using OCR
5. Text-to-Speech (TTS) feedback
6. Selected Sesotho voice interaction/audio feedback
7. Dominant colour recognition for the object selected by Scan Surroundings

### Colour recognition scope added on 2026-09-30

The user requested implementation of detected-object colour recognition, including
displayed and spoken object/colour results. The initial approach uses on-device
image-region analysis without training another model, with stable readings and
uncertain-colour handling. Lighting and bounding-box background can affect accuracy.

On 2026-09-30 the user reported that colour recognition works but is not effective
enough, and accepted approximate guesses with low precision/confidence. Prefer
returning a plausible sampled colour with explicit uncertainty over suppressing
mixed or weak readings. Keep colour confidence distinct from object confidence.

Local context is an important requirement.

The prototype should eventually recognize information relevant to users in Lesotho rather than relying entirely on generic international datasets.

Prefer **offline/on-device processing** where technically feasible.

### Language choice updated on 2026-10-04

The user requested an English/Sesotho selection screen to accommodate people who
are not Basotho or do not understand Sesotho. The selected language controls app
content and voice output. Save the choice and allow changing it from the home
screen. This supersedes the earlier requirement to always translate into Sesotho.
English app results use English speech; Sesotho retains local/cloud translation
and the installed Sesotho voice with English-voice fallback.

### Speech and translation requirements updated on 2026-09-23

The user directed implementation of the cloud Google Translate API version for
translating English text to Sesotho (`st`) before TTS playback. Translation uses
Google Cloud Translation v2 with in-memory caching to eliminate redundant calls and
a local offline dictionary fallback for common app guidance and object classes.
Speech output attempts to use an installed Sesotho voice if available on device,
falling back to the installed English TTS voice to read the Sesotho text.
Cloud translation requires internet access and an API key configured via
`local.properties` (`GOOGLE_TRANSLATE_API_KEY`) or `GoogleCloudTranslator.customApiKey`.

---

## 3. Current Android Configuration

Current project configuration:

- `compileSdk`: 37
- `targetSdk`: 37
- `minSdk`: 24
- Java compatibility: Java 11
- Jetpack Compose enabled
- Kotlin
- AndroidX

Current general Gradle configuration:

```kotlin
android {
    namespace = "com.moeketsifaso.ntsamaise"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.moeketsifaso.ntsamaise"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}
```
