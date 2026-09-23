# NTSAMAISE project progress

Last updated: 2026-09-23

## Overall status

Early functional research prototype. Three home-screen workflows now have
implementations: Scan Surroundings, Read Text and an OCR-based Identify Money
prototype. Spoken output across these workflows is now translated into Sesotho
prior to TTS playback, with Sesotho text displayed on-screen. Translation is
powered by the Cloud Google Translate API (v2) with in-memory caching and an
offline dictionary fallback for common object classes, currency messages, and
app guidance. Identify Money suggests a possible Lesotho denomination from printed
text; trained visual banknote recognition is not implemented. Device accuracy and
usability remain unverified on a physical phone. Identify Product still has an empty action.

This record reflects inspected code and recorded verification, rather than all
the intended capabilities listed in PROJECT_CONTEXT.md.

## Implemented

| Area | Current implementation |
| --- | --- |
| App foundation | Kotlin, Jetpack Compose, home screen, scanner navigation and camera permission handling. |
| General object recognition | MediaPipe Tasks Vision 1.0.0 with a bundled EfficientDet-Lite0 int8 model supporting 80 COCO classes. Inference runs on-device without a model download. The strongest label above the configured 50% confidence threshold is presented. |
| Camera processing | CameraX preview and frame analysis, background inference, upright frame rotation, stale-frame dropping and resource cleanup when leaving the scanner. |
| Sesotho translation & speech | Google Cloud Translation v2 client (`GoogleCloudTranslator.kt`) translating English text to Sesotho (`st`) before TTS playback. In-memory caching eliminates redundant queries for identical text; built-in local dictionary provides immediate offline translations for common COCO classes, currency readings, and status prompts. The on-screen display shows the Sesotho translation alongside original text. TTS attempts to load an installed Sesotho voice (`st-LS`), falling back to reading the Sesotho text using the installed English TTS voice. |
| Spoken feedback gating | Announcements require results to remain unchanged for 0.8 seconds; new announcements are spaced at least 3 seconds apart, and unchanged results repeat after 8 seconds. Speech stops when leaving or backgrounding the scanner. |
| Accessibility foundation | Large home-screen buttons and descriptive accessibility labels. These have not yet been validated with intended users. |
| Printed-text OCR | Read Text uses bundled ML Kit Latin-script OCR. Capture and read recognizes one frame, translates the passage to Sesotho, displays both translations and reads the Sesotho text aloud. Read again, Stop reading and Cancel capture controls are implemented. Long passages are split into ordered speech chunks. |
| Shared camera and speech | Object recognition and OCR share camera permissions, lifecycle handling, worker cleanup, translation pipeline, and TTS setup. Backgrounding cancels pending capture results and stops speech. |
| Currency OCR prototype | Reuses offline OCR, capture controls, Sesotho translation, and speech. Supports possible 10/20/50/100/200-maloti text matches with bank-name and corroborating-denomination checks. Ambiguous or incomplete text returns retry guidance translated into Sesotho. No visual banknote classifier or authenticity check is implemented. |

The detector replacement is committed as `2f4fe2c` (Replace ML Kit detector with
offline MediaPipe EfficientDet). English speech and project context were added
in `96a96d5`. Text reading and currency OCR were committed as `fc807e9` (Add offline
text reading and Lesotho currency OCR prototype). Cloud Google Translate Sesotho
integration and speech translation were committed in `8da97f8` (Add cloud Google Translate
API Sesotho translation and speech integration).

## Work underway and pending validation

Cloud Google Translate Sesotho integration, debug build, and unit-test verification
are complete. All workflows still need phone testing for recognition quality, response
time, internet connectivity handling, API key configuration, offline fallback behaviour,
and repeated reopening.

## Remaining scope

| Capability | Status and remaining work |
| --- | --- |
| Road-sign detection | User requested YOLO11 training on 2026-09-23. Colab training notebook prepared at `training/road_signs_yolo11_colab.ipynb`. Dataset and target sign classes are pending clarification. No training run, trained road-sign model or Android integration is implemented. |
| Lesotho Loti banknotes | OCR prototype implemented; real-note testing is pending. Collect representative labelled images, train/evaluate a visual model and integrate it for recognition beyond printed-text heuristics. |
| Local products/logos | Button only. Select products, prepare labelled data, train and evaluate a model, and integrate recognition. |
| Printed-text OCR validation | Implementation added. Validate printed passages, long readings, empty images, rotation, cancellation, offline use, and TalkBack on a phone. |
| Sesotho translation validation | Code implemented with Cloud Google Translate API, cache, and offline dictionary fallback. Test on a physical phone with live Google Translate API key, verify Sesotho pronunciation with the phone's TTS engine, and validate offline graceful degradation. |
| Accessibility evaluation | Test TalkBack, large text, contrast, error feedback and navigation with blind and partially sighted users. |
| Research evaluation | Record recognition accuracy, task success, response time and user feedback on representative devices and local examples. |

The bundled general-object model does not implement Loti denomination or local
brand recognition.

## Verification evidence

- Sesotho translation verification on 2026-09-23 passed: `:app:assembleDebug`,
  `:app:assembleDebugAndroidTest`, and `:app:testDebugUnitTest`.
- All 31 unit tests passed: six translator tests (local dictionary match, currency pattern translation, Cloud API response parsing with HTML unescaping, cache deduplication, missing key fallback, and error handling), 14 currency-parser tests, seven speech-chunking/capture-request tests, three announcement-timing tests, and one example test.
- Both debug and Android test APKs assembled without errors.
- Read Text verification on 2026-09-22 passed: `:app:assembleDebug`,
  `:app:testDebugUnitTest` and `:app:assembleDebugAndroidTest`.
- Currency verification on 2026-09-22 passed offline: `:app:assembleDebug`,
  `:app:testDebugUnitTest` and `:app:assembleDebugAndroidTest`.
- No physical Android device was connected during this implementation. Real-device testing is pending.

## Suggested next milestones

These are proposed priorities, not work already started or an agreed schedule.

1. Test the Sesotho translation and speech output on a physical Android device with a configured Google Translate API key.
2. Validate Scan Surroundings, Read Text and currency OCR on a target phone.
3. Evaluate OCR quality and long-passage reading with representative printed material.
4. Develop and evaluate Loti banknote recognition with separate training and evaluation data.
5. Add selected local product recognition.
6. Conduct accessibility and research evaluation sessions with intended users.

## Progress log

- 2026-09-23: Implemented cloud Google Translate API version for Sesotho translation
  before TTS playback per user instruction. Created `GoogleCloudTranslator.kt` with
  self-contained JSON handling (zero extra runtime dependencies), Google Cloud Translation
  v2 endpoint targeting Sesotho (`st`), in-memory query caching, and a built-in offline
  dictionary for common COCO object classes, currency guidance, and UI prompts. Updated
  `SpeechOutput.kt` to prefer an installed Sesotho TTS voice (`st-LS`) before falling
  back to reading Sesotho text with the installed English voice. Updated `CameraScreen.kt`
  and `ReadTextScreen.kt` to display Sesotho translations on-screen and speak them. Added
  `android.permission.INTERNET` and enabled `buildConfig` with `GOOGLE_TRANSLATE_API_KEY`
  support in `app/build.gradle.kts`. Added six unit tests in `GoogleCloudTranslatorTest.kt`.
  All 31 unit tests and both debug/test APK builds passed.

- 2026-09-23: Clarified translation scope: English text must become Sesotho before
  all app-controlled speech; the existing English voice is acceptable. Inspected
  ML Kit language limitations.

- 2026-09-23: Added the user's requested YOLO11 road-sign training scope.
  Prepared a YOLO11n notebook with training, saved checkpoints, environment recording
  and held-out test evaluation.

- 2026-09-22: User selected an OCR prototype as the first currency version.
  Connected Identify Money to shared capture/speech controls and added cautious
  denomination parsing, rejection cases and tests.

- 2026-09-22: Implemented Read Text with bundled Latin-script OCR, capture-on-demand,
  fixed results, repeat/stop/cancel controls and long-passage speech.
