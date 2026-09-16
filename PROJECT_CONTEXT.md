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

Local context is an important requirement.

The prototype should eventually recognize information relevant to users in Lesotho rather than relying entirely on generic international datasets.

Prefer **offline/on-device processing** where technically feasible.

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
