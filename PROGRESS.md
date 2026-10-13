# NTSAMAISE project progress

Last updated: 2026-10-04

## Language selection update (2026-10-04)

Implemented a bilingual first-launch language screen, a saved English/Sesotho
preference, and a home-screen language-change action. App controls, guidance,
permission/error messages and accessibility labels use offline interface copy.
English detection/OCR/currency speech bypasses translation and selects English
TTS. Sesotho retains the local/cloud pipeline and installed-Sesotho/English-voice
fallback. Language changes dispose screen speech state. OCR and scanner translation
failures display an explicit fallback notice. Original OCR text remains available.
Older statements below describing always-Sesotho output are historical and are
superseded by this selection. Error/no-text speech localization and stale README
claims noted in the September inspection have been addressed.

Verification underway: debug app/test APK builds and unit tests. New tests cover
language codes, English translation bypass even after Sesotho caching, offline
Sesotho errors/guidance and interface translations. A Compose test covers switching
both ways. No new device testing has been performed. Real-device persistence,
TalkBack, large text, all workflows, TTS voices and fluent Sesotho review remain
pending. No additional feature scope is being implemented during this verification.

## Overall status

Early functional research prototype. Four home-screen workflows now have
implementations: Scan Surroundings, Read Text and an OCR-based Identify Money
prototype, plus experimental Detect Obstacles. Scan Surroundings now includes
on-device dominant-colour estimates with displayed and spoken colour/uncertainty
feedback. The user reports that colour recognition works but is not very effective;
device details and measured results were not supplied. It now offers immediate
best guesses with low/moderate heuristic confidence; this revision awaits device
validation. Obstacle detection uses the bundled
object model with image-space bounding-box heuristics and offline directional
Sesotho alerts, plus user-calibrated approximate distance estimates for the same
object. It does not provide sensor depth or establish a safe route. This new
feature has not been device-tested. Recognized results across these workflows now follow the saved English/Sesotho
selection for display and TTS. English bypasses translation; Sesotho uses local
and cloud translation, with an explicit notice when original text is returned.
App-controlled camera/OCR error and no-text speech uses offline localized copy. Translation is
powered by the Cloud Google Translate API (v2) with in-memory caching and an
offline dictionary fallback for common object classes, currency messages, and
app guidance. Identify Money suggests a possible Lesotho denomination from printed
text; trained visual banknote recognition is not implemented. On 2026-09-26, the
user confirmed that all device tests pass on a real device. Formal accuracy
measurement and usability evaluation with intended users remain outstanding.
Identify Product still has an empty action.

This record reflects inspected code and recorded verification, rather than all
the intended capabilities listed in PROJECT_CONTEXT.md.

## Implemented

| Area | Current implementation |
| --- | --- |
| App foundation | Kotlin, Jetpack Compose, home screen, scanner navigation and camera permission handling. |
| General object recognition | MediaPipe Tasks Vision 1.0.0 with a bundled EfficientDet-Lite0 int8 model supporting 80 COCO classes. Inference runs on-device without a model download. The strongest label above the configured 50% confidence threshold is presented. |
| Object colour recognition | Scan Surroundings samples the central 60% of the selected object's visible upright bounding box using at most 576 pixels and 11 HSV colour buckets. Returns the leading colour immediately, including mixed/partially clipped samples, with low/moderate heuristic confidence shown separately from object confidence. Smooths up to five frames within 1.2 seconds, retaining the previous guess on ties. Displays and speaks "Possible colour" through existing translation, with offline Sesotho composition for dictionary objects. Only unusable regions return colour unclear. Resets on selection/geometry/gap/session changes and silences stale camera results. No segmentation, identity tracking, illumination correction or measured probability/accuracy. |
| Experimental obstacle alerts | Separate Detect Obstacles screen with left/ahead/right alerts, center-overlap and image-area prioritization, invalid/small-box filtering, local Sesotho prompts, pause/resume, stale-result timeout and Back navigation. Reuses the bundled detector and shared camera/speech lifecycle. No sensor depth, safe-path assurance, steps/potholes model or device accuracy validation. |
| Calibrated distance estimate | User enters a known camera-to-object distance; subsequent normalized box heights estimate distance to the same object. Stable, confident, fully visible observations only; unsupported readings are unavailable. Displays metres and speaks approximate distances with offline Sesotho translation. Session calibration resets on camera stops/errors, stale frames, geometry or selected-class changes. No physical-object identity tracking or depth sensor integration. |
| Camera processing | CameraX preview and frame analysis, background inference, upright frame rotation, stale-frame dropping and resource cleanup when leaving the scanner. |
| Selected-language translation & speech | Google Cloud Translation v2 client (`GoogleCloudTranslator.kt`) translating English text to Sesotho (`st`) before TTS playback when Sesotho is selected. English mode bypasses translation. In-memory caching eliminates redundant queries for identical text; built-in local dictionary provides immediate offline translations for common COCO classes, currency readings, and status prompts. The interface follows the saved language. Original OCR text remains available separately. TTS attempts to load an installed Sesotho voice (`st-LS`), falling back to reading the Sesotho text using the installed English TTS voice. |
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

Initial colour implementation passed 61 unit tests and debug app/test APK builds.
On 2026-09-30 the user reported it works but is not effective enough and accepted
low-confidence guesses. The permissive guessing revision and automated verification
are complete. Device colour accuracy, lighting/background robustness, latency and
Sesotho wording/pronunciation remain pending. No additional model training is underway.

Cloud Google Translate Sesotho integration is implemented, with debug build and
unit-test success recorded on 2026-09-23. The user confirmed on 2026-09-26 that all
device tests pass on a real device. This supersedes the earlier pending-device-test
status. Device model, Android version, individual test cases and measured results
were not supplied. That report predates obstacle detection and does not validate it.
Obstacle implementation and automated verification completed on 2026-09-27.
Calibrated distance estimation and its automated checks also completed on 2026-09-27.
Device testing, threshold calibration and Sesotho wording/pronunciation evaluation
remain pending, alongside formal research evaluation and the source-inspection gaps below.

### Source inspection findings (2026-09-24)

- Translation checks the cache, then the local dictionary, then the cloud API.
  The dictionary covers selected object labels, not all 80 detector classes.
  Unmapped text is returned unchanged when the key is missing or translation fails.
- Read Text and currency screens do not display `TranslationResult.error` or its
  source. They can label original English text as a Sesotho translation after a
  fallback. OCR/camera errors and Read Text's empty-capture message are passed
  directly to speech without translation. Fully Sesotho app-controlled speech
  therefore remains incomplete.
- Older README sections still claim English-only speech and no translation,
  contradicting the newer Sesotho setup section and current code. Documentation
  cleanup remains pending.
- Identify Product remains an empty navigation action. Road signs have a Colab
  training notebook only; no trained road-sign model is bundled or integrated.
- Existing local test reports confirm 31 tests with zero failures/errors dated
  2026-09-23. Tests/builds were not rerun for this inspection; live translation,
  pronunciation, camera behaviour and accessibility were not verified.

## Remaining scope

| Capability | Status and remaining work |
| --- | --- |
| Object colour validation | Implementation added on 2026-09-30. Measure colour accuracy on representative objects/lighting/backgrounds and verify selection, stability, lifecycle, accessibility and Sesotho speech on-device. README includes a device checklist. |
| Obstacle detection | Experimental object-box heuristic and calibrated approximate distance implemented on 2026-09-27. Validate direction, distance error against measured references, latency, false positives/misses, lifecycle, offline speech and accessibility on-device. Sensor depth and dedicated hazard recognition remain unimplemented. |
| Road-sign detection | User requested YOLO11 training on 2026-09-23. Colab training notebook prepared at `training/road_signs_yolo11_colab.ipynb`. Dataset and target sign classes are pending clarification. No training run, trained road-sign model or Android integration is implemented. |
| Lesotho Loti banknotes | OCR prototype implemented; user reports all device tests pass. Collect representative labelled images, train/evaluate a visual model and integrate it for recognition beyond printed-text heuristics. |
| Local products/logos | Button only. Select products, prepare labelled data, train and evaluate a model, and integrate recognition. |
| Printed-text OCR validation | Implemented; user reports all device tests pass. Formal OCR quality measurements and detailed test coverage are not recorded. |
| Sesotho translation validation | Implemented; user reports all device tests pass. Detailed translation/pronunciation evaluation results are not recorded; source-inspected fallback labelling and untranslated feedback gaps remain. |
| Accessibility evaluation | Test TalkBack, large text, contrast, error feedback and navigation with blind and partially sighted users. |
| Research evaluation | Record recognition accuracy, task success, response time and user feedback on representative devices and local examples. |

The bundled general-object model does not implement Loti denomination or local
brand recognition.

## Verification evidence

- Colour guessing revision on 2026-09-30: offline `:app:assembleDebug`,
  `:app:testDebugUnitTest` and `:app:assembleDebugAndroidTest` passed. All 64 unit
  tests passed with zero failures/errors/skips; `git diff --check` passed. Revised tests
  cover immediate low-confidence output, mixed and clipped samples, smaller
  regions, temporal voting/ties/expiry, selection resets, display confidence and
  translated uncertainty. Device retesting of this revision is pending.

- Object colour verification on 2026-09-30 passed offline:
  `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:assembleDebugAndroidTest`.
  All 61 unit tests passed with zero failures/errors/skips. Eleven new tests cover
  the 11 representative colour buckets, selected-box centre sampling, sampling
  limits, mixed regions/highlights, invalid/clipped/tiny/transparent regions,
  frame stability and resets, confidence-free speech and offline colour/uncertainty
  translation. `git diff --check` passed. Used the cached JDK 25 with a command-line
  Java-home override; no project build configuration was changed. No instrumented
  tests were executed on a device; colour accuracy, camera latency and native
  Sesotho wording/pronunciation remain unverified.

- Calibrated distance verification on 2026-09-27 passed offline:
  `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:assembleDebugAndroidTest`.
  Nine new tests cover inverse-height scaling, geometry/class mismatch, invalid
  calibration and bounds, supported range, observation stability/reset and offline
  distance speech. All 50 unit tests passed with zero failures/errors. No device
  distance measurements or instrumented test execution were performed.

- Obstacle detection verification on 2026-09-27 passed offline:
  `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:assembleDebugAndroidTest`.
  All 41 unit tests passed with zero failures/errors. The ten new tests cover
  directions, central overlap, candidate priority,
  invalid/clipped bounds, no-detection semantics, stable directional speech keys,
  local Sesotho translations and announcement intervals. Android test APK assembly
  is a build check, not execution on a device. Obstacle camera behaviour, latency,
  accuracy and pronunciation have not been tested on-device.

- Sesotho translation verification on 2026-09-23 passed: `:app:assembleDebug`,
  `:app:assembleDebugAndroidTest`, and `:app:testDebugUnitTest`.
- All 31 unit tests passed: six translator tests (local dictionary match, currency pattern translation, Cloud API response parsing with HTML unescaping, cache deduplication, missing key fallback, and error handling), 14 currency-parser tests, seven speech-chunking/capture-request tests, three announcement-timing tests, and one example test.
- Both debug and Android test APKs assembled without errors.
- Read Text verification on 2026-09-22 passed: `:app:assembleDebug`,
  `:app:testDebugUnitTest` and `:app:assembleDebugAndroidTest`.
- Currency verification on 2026-09-22 passed offline: `:app:assembleDebug`,
  `:app:testDebugUnitTest` and `:app:assembleDebugAndroidTest`.
- On 2026-09-26, the user reported: "Device tests all pass on real device".
  Recorded as user-confirmed real-device verification. Device details and individual
  test results were not supplied; Codex did not independently rerun device tests.

## Suggested next milestones

These are proposed priorities, not work already started or an agreed schedule.

1. Validate language selection, persistence, localized controls and both speech modes on-device.
2. Record device/test details and evaluate Sesotho wording and pronunciation with fluent speakers.
3. Evaluate OCR quality and long-passage reading with representative printed material.
4. Develop and evaluate Loti banknote recognition with separate training and evaluation data.
5. Add selected local product recognition.
6. Conduct accessibility and research evaluation sessions with intended users.

## Progress log

- 2026-10-04: Added saved English/Sesotho selection at first launch and from home.
  Localized controls, guidance and app-controlled error/no-text speech. Connected
  object, obstacle, OCR and currency output and TTS voice setup to the preference.
  Added explicit translation fallback notices and retained original OCR text.
  Added language-policy unit checks and a Compose language-switch test. Updated
  README and project scope. Automated verification is underway; device validation
  and fluent-speaker review are pending.


- 2026-09-30: User reported the original colour feature works but is not very
  effective, and accepted guesses with low precision/confidence. Replaced majority
  rejection and the 600 ms startup gate with immediate guesses and five-frame
  smoothing. Sample partially clipped and smaller visible regions at low
  confidence; keep unusable-region rejection. Display low/moderate heuristic colour
  confidence separately from object percentages and say "Possible colour" in the
  existing English/Sesotho feedback pipeline. Updated tests, README and project
  scope. Offline builds and unit tests passed; revised device behaviour remains
  unverified. No further implementation is actively underway.

- 2026-09-30: Implemented the user's requested detected-object colour feature in
  Scan Surroundings. Retained the winning detection's box, sampled its upright
  central region on-device, added 11 colour buckets with majority/uncertainty
  handling and temporal stability. Added displayed/spoken colour results, offline
  Sesotho composition for existing dictionary objects, camera-stop/error/stale
  result clearing and documentation with a device evaluation checklist. Updated
  PROJECT_CONTEXT.md with the added scope. All 61 unit tests and both debug APK
  builds passed offline; real-device evaluation is pending. No additional
  implementation work is currently underway in this change.

- 2026-09-27: User selected approximate camera distances with calibration. Added
  reference-distance entry, calibration/clear controls, inverse-box-height distance
  calculation, stability/confidence/visibility/range checks and session reset rules.
  Added displayed metre estimates and half-metre-step offline Sesotho speech.
  Same-object/pose assumptions and a tape-measure evaluation checklist are documented.
  Debug app/test APK builds and all 50 unit tests passed offline; real-device
  distance accuracy is unverified.

- 2026-09-27: Added Detect Obstacles navigation and a scrollable home screen. Reused
  upright MediaPipe detections with normalized bounds; prioritized middle-third
  overlap, then box area (minimum 4%, confidence at least 50%, up to 20 results).
  Added local Sesotho direction prompts, 400 ms stability, 1.5 s change/5 s repeat
  intervals, pause/resume and stale-result silencing. Added policy, malformed-box,
  translation and timing tests plus a README device checklist. Preserved existing
  progress-file changes. Debug app/test APK builds and unit tests passed offline;
  no obstacle device testing has been performed.

- 2026-09-26: User confirmed all device tests pass on a real device. Updated
  current status, verification evidence and next milestones to remove the pending
  real-device-testing status. Formal research/user evaluation and known source
  gaps remain separate. Documentation-only update; no tests rerun by Codex.

- 2026-09-26: Rechecked project context, current navigation, object detector,
  currency parser, translation/speech call sites, bundled model and local unit-test
  reports for a project status report. Three workflows remain implemented;
  Identify Product is still an empty action. Existing reports show 31 passing
  unit tests from 2026-09-23. No builds or tests were rerun and no device behaviour
  was verified during this review. Translation feedback gaps and remaining scope
  above remain outstanding; no new implementation work was started. Preserved
  the progress-file changes already present at review start.

- 2026-09-24: Reviewed project context, progress, tracked application source,
  tests, build configuration, resources, recent commits and the YOLO11 notebook.
  Working tree was clean at review start. Recorded translation fallback and
  untranslated feedback gaps, stale README statements and existing test-report
  evidence. Updated this progress record only; application code was not changed.

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
