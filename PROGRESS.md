# NTSAMAISE project progress

Last updated: 2026-09-22

## Overall status

Early functional research prototype. Three home-screen workflows now have
implementations: Scan Surroundings, Read Text and an OCR-based Identify Money
prototype. Identify Money suggests a possible Lesotho denomination from printed
text; trained visual banknote recognition is not implemented. Device accuracy and
usability remain unverified. Identify Product still has an empty action.

This record reflects inspected code and recorded verification, rather than all
the intended capabilities listed in PROJECT_CONTEXT.md.

## Implemented

| Area | Current implementation |
| --- | --- |
| App foundation | Kotlin, Jetpack Compose, home screen, scanner navigation and camera permission handling. |
| General object recognition | MediaPipe Tasks Vision 1.0.0 with a bundled EfficientDet-Lite0 int8 model supporting 80 COCO classes. Inference runs on-device without a model download. The strongest label above the configured 50% confidence threshold is presented. |
| Camera processing | CameraX preview and frame analysis, background inference, upright frame rotation, stale-frame dropping and resource cleanup when leaving the scanner. |
| English spoken feedback | Text-to-speech with an installed offline voice preferred. Results must remain unchanged for 0.8 seconds; new announcements are spaced at least 3 seconds apart, and unchanged results repeat after 8 seconds. Speech stops when leaving or backgrounding the scanner. |
| Accessibility foundation | Large home-screen buttons and descriptive accessibility labels. These have not yet been validated with intended users. |
| Printed-text OCR | Read Text uses bundled ML Kit Latin-script OCR. Capture and read recognizes one frame and freezes the displayed result. Read again, Stop reading and Cancel capture controls are implemented. Long passages are split into ordered speech chunks. |
| Shared camera and speech | Object recognition and OCR share camera permissions, lifecycle handling, worker cleanup and English TTS setup. Backgrounding Read Text cancels pending capture results and stops speech. |
| Currency OCR prototype | User-approved first version reuses offline OCR, capture controls and speech. Supports possible 10/20/50/100/200-maloti text matches with bank-name and corroborating-denomination checks. Ambiguous or incomplete text returns retry guidance. No visual banknote classifier or authenticity check is implemented. |

The detector replacement is committed as `2f4fe2c` (Replace ML Kit detector with
offline MediaPipe EfficientDet). English speech and project context were added
in `96a96d5`.

## Work underway and pending validation

Read Text implementation, debug build and unit-test verification are complete.
The currency OCR prototype's debug build and unit-test verification are complete.
All workflows still need phone testing for recognition quality, response time, lighting,
orientation, offline operation, speech behaviour and repeated reopening.

The Android Studio `ComposableFunction1` diagnostic was reported while the build
still worked. Gradle sync and cache invalidation were suggested; clearance of the
editor diagnostic has not been confirmed.

## Remaining scope

| Capability | Status and remaining work |
| --- | --- |
| Lesotho Loti banknotes | OCR prototype implemented; real-note testing is pending. Collect representative labelled images, train/evaluate a visual model and integrate it for recognition beyond printed-text heuristics. |
| Local products/logos | Button only. Select products, prepare labelled data, train and evaluate a model, and integrate recognition. |
| Printed-text OCR validation | Implementation added. Validate printed passages, long readings, empty images, rotation, cancellation, offline use and TalkBack on a phone. |
| Sesotho interaction/audio | A Sesotho subtitle is present. Sesotho speech, recorded prompts and voice commands are not implemented. Define and implement the selected interactions. |
| Accessibility evaluation | Test TalkBack, large text, contrast, error feedback and navigation with blind and partially sighted users. |
| Research evaluation | Record recognition accuracy, task success, response time and user feedback on representative devices and local examples. |

The bundled general-object model does not implement Loti denomination or local
brand recognition.

## Verification evidence

- Read Text verification on 2026-09-22 passed: `:app:assembleDebug`,
  `:app:testDebugUnitTest` and `:app:assembleDebugAndroidTest`.
- All 11 unit tests passed: seven speech-chunking/capture-request tests,
  three announcement-timing tests and one generated example test.
- The Android OCR smoke test using a rendered printed phrase compiled into the
  test APK but has not been executed on a device.
- Dependency downloads initially failed because Java did not trust this machine's
  antivirus HTTPS inspection certificate. Verification succeeded using a temporary
  copy of Java's trust store with the existing Windows-trusted antivirus certificate
  added for the build. Global trust settings and project security settings were not changed.
- No Android device or emulator was connected when checked during this implementation.
- No phone recognition benchmarks or accessibility study results are recorded.
- Currency verification on 2026-09-22 passed offline: `:app:assembleDebug`,
  `:app:testDebugUnitTest` and `:app:assembleDebugAndroidTest`.
- All 25 unit tests passed, including 14 currency-parser tests. The Android
  OCR-to-currency test compiled but has not been run on a device. Its input is
  synthetic text; it does not measure real-note accuracy.
- Passing the build and timing tests does not establish camera accuracy, offline
  speech availability on every phone, or accessibility in practice.

Manual checks for both workflows are documented in README.md.

## Suggested next milestones

These are proposed priorities, not work already started or an agreed schedule.

1. Validate Scan Surroundings, Read Text and currency OCR on a target phone.
2. Evaluate OCR quality and long-passage reading with representative printed material.
3. Develop and evaluate Loti banknote recognition with separate training and evaluation data.
4. Add selected local product recognition and Sesotho feedback.
5. Conduct accessibility and research evaluation sessions with intended users.

## Progress log

- 2026-09-22: User selected an OCR prototype as the first currency version.
  Connected Identify Money to shared capture/speech controls and added cautious
  denomination parsing, rejection cases and tests. Offline debug and Android test
  APK builds passed, along with all 25 unit tests. Real-banknote evaluation and a
  trained visual model remain pending.

- 2026-09-22: Implemented Read Text with bundled Latin-script OCR, capture-on-demand,
  fixed results, repeat/stop/cancel controls and long-passage speech. Extracted
  shared camera and speech components. Debug and Android test APK builds passed,
  along with all 11 unit tests. Device tests remain pending.

- 2026-09-22: Established PROGRESS.md as the project progress record and added a
  persistent instruction in AGENTS.md to maintain it after meaningful changes.
  Recorded the current implementation, outstanding scope and verification limits.
