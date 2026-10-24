# NTSAMAISE project progress

Last updated: 2026-10-24

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
| Banknote visual recognition (YOLO11s) | Bundled on-device TensorFlow Lite model (`currency_yolo11s.tflite`, 37.9 MB) trained on 10 banknote classes (10/20/50/100/200 Maloti and 10/20/50/100/200 Rand). Includes NCHW 640x640 letterbox preprocessing, anchor decoding across 8,400 candidates, confidence filtering, and IoU-based Non-Maximum Suppression (NMS). Live CameraX preview renders visual bounding boxes and confidence percentage on-screen. Spoken results are rate-limited via `rememberSpokenDetection` and localized to English or Sesotho with offline dictionary translations. Legacy OCR text heuristic preserved in `LotiCurrencyReader.kt`. No banknote authenticity check. |

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
| Lesotho Loti banknotes | YOLO11s visual detector integrated and verified offline (78 unit tests and APK assembly pass). Real-device validation of detection accuracy across lighting conditions, folded/worn notes, angles, and Sesotho speech pronunciation remains pending. |
| Local products/logos | Button only. Select products, prepare labelled data, train and evaluate a model, and integrate recognition. |
| Printed-text OCR validation | Implemented; user reports all device tests pass. Formal OCR quality measurements and detailed test coverage are not recorded. |
| Sesotho translation validation | Implemented; user reports all device tests pass. Detailed translation/pronunciation evaluation results are not recorded; source-inspected fallback labelling and untranslated feedback gaps remain. |
| Accessibility evaluation | Test TalkBack, large text, contrast, error feedback and navigation with blind and partially sighted users. |
| Research evaluation | Record recognition accuracy, task success, response time and user feedback on representative devices and local examples. |

The bundled general-object model does not implement Loti denomination or local
brand recognition.

## Verification evidence

- YOLO11s banknote detector verification on 2026-10-24: offline `:app:assembleDebug`,
  `:app:testDebugUnitTest`, and `:app:assembleDebugAndroidTest` passed cleanly. All
  78 unit tests passed with zero failures/errors/skips. Ten new unit tests in
  `CurrencyDetectorTest.kt` cover class index alignment, IoU calculations (identical,
  disjoint, partial overlap), NMS box pruning, 640x640 NCHW letterbox decoding and
  un-letterboxed coordinate conversion, confidence threshold filtering, feedback
  formatting, and offline Sesotho translation of all 10 banknote classes and percentages.
  Real-device execution and physical note validation are pending.

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

- 2026-10-24: Fixed installation failure bug. Identified that `buildTypes.release` in
  `app/build.gradle.kts` lacked a `signingConfig`, generating an unsigned APK (`app-release-unsigned.apk`)
  that Android rejects on installation with `INSTALL_PARSE_FAILED_NO_CERTIFICATES` ("App not installed").
  Added `signingConfig = signingConfigs.getByName("debug")` to `release` so packaged release APKs
  (`app-release.apk`) are signed and installable. Also identified that the host system `JAVA_HOME`
  environment variable points to an invalid directory (`C:\Program Files\Java\jre1.8.0_461`), causing
  Gradle CLI invocations (`./gradlew installDebug`) to fail unless overridden with the Android Studio
  bundled JBR (`C:\Program Files\Android\Android Studio\jbr`). Verified on-device installation and launch
  on physical device Samsung SM-A032F (API 33) for both debug and release builds; all 78 unit tests pass.

- 2026-10-24: Integrated trained YOLO11s currency model (`currency_yolo11s.tflite`,
  37.9 MB) into Android app assets and added `org.tensorflow:tensorflow-lite:2.16.1`
  with `noCompress` rule in `app/build.gradle.kts`. Implemented `CurrencyDetector.kt`
  with NCHW 640x640 letterbox scaling, TFLite inference, coordinate un-letterboxing,
  and NMS suppression. Replaced OCR capture screen with dedicated live `CurrencyScreen.kt`
  featuring real-time bounding box overlay, bilingual status card, and debounced speech output.
  Added offline Sesotho banknote dictionary translations and percentage matching in
  `GoogleCloudTranslator.kt` and `AppLanguage.kt`. Added 10 unit tests in
  `CurrencyDetectorTest.kt`. All 78 unit tests, debug APK, and androidTest APK
  assembled with zero failures.

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

## Currency detector code review (2026-10-24)

Reviewed the current TensorFlow Lite currency detector and live camera integration after
user reported failed detection. The shipped asset `app/src/main/assets/currency_yolo11s.tflite`
was SHA-256 identical to `model/best.tflite`. The recorded 60-epoch validation results
reach precision 0.629, recall 0.613, mAP50 0.603 and mAP50-95 0.379; model quality can
therefore still cause missed notes, but is not by itself evidence of zero detection.

Likely implementation defects: the interpreter buffers and postprocessor hard-code a
`[1,14,8400]` output and channel-major indexing without inspecting the model tensor
shape. Ultralytics detection exports commonly use `[1,8400,14]`, in which case the
current decode is invalid. The output buffer is passed to `Interpreter.run` as a
FloatBuffer while populated/consumed as NCHW, also relying on assumed output shape.
The app likewise hard-codes `[1,3,640,640]`, float input, RGB planar layout and 0..1
normalization without checking the model input tensor metadata. Confirm actual input/output
tensor shapes and types on device or with a local TFLite interpreter before applying a
shape-specific fix.

Camera integration risk: `CameraPreview` requests `RGBA_8888`, while both currency and
surroundings processors call `ImageProxy.toBitmap()` without defining that conversion in
project sources or explicit plane handling. The dependency-resolved extension/behavior
needs confirmation; incorrect RGBA plane conversion can corrupt all pixels before inference.
Currency overlay boxes are also drawn directly in the composable viewport despite
`PreviewView.FIT_CENTER`, so box display can be misaligned even if detection succeeds.

Verification limitation: no attached Android device/adb or local TensorFlow Lite model
inspection runtime was available during this review. Unit tests currently validate synthetic
channel-major decoding and class labels but not the shipped model tensor metadata, real
model output, camera pixel conversion, or end-to-end detection. No detection fix has been
applied pending confirmation of tensor metadata.

## Currency detector fixes (2026-10-24)

Implemented a source-level repair pass after review of the TensorFlow Lite currency
path. The detector now reads runtime input/output tensor shapes and types, supports
NHWC/NCHW input and channel-major/anchor-major YOLO output, handles float32 and
int8/uint8 quantized tensors, and validates batch, spatial dimensions and ten-class
output before inference. Preprocessing and box unletterboxing use model tensor size
rather than fixed 640 assumptions. Added anchor-major decoder unit coverage.

CameraX RGBA output now has an explicit row/pixel-stride-aware conversion to ARGB
bitmaps, shared with surroundings scanning. The camera host installs its analyzer
once both the camera is bound and processor initialization has completed, addressing
a startup ordering race. Currency box overlays are mapped using CameraX's frame-to-
PreviewView transform so FIT_CENTER scaling/rotation/crop is applied.

Verification limitation: Gradle compilation could not run in this environment. The
wrapper attempted a network download due cache location mismatch (certificate failure);
using cached Gradle then failed initializing Windows native services. No real device
is attached, so model tensor metadata, camera conversion and live recognition remain
to be validated on-device. The training report still indicates moderate validation
accuracy, so missed detections can remain after implementation fixes.

## Currency capture workflow update (2026-10-24)

Changed Identify Money from continuous analysis to explicit still capture. The camera
binds a live preview with CameraX ImageCapture and runs the currency model only on the
captured ImageProxy. Captured YUV_420_888 data is converted using its plane row/pixel
strides. An accessible on-screen button and Volume Up key-up invoke the same capture
action; repeated capture is disabled while inference runs, and the last result remains
visible until the next capture. Bounding boxes keep the CameraX preview transform.
Capture prompts/status have offline Sesotho copy. This update has not been compiled or
device-verified due the Gradle environment issue recorded above.

## Desktop model test harness (2026-10-24)

Added `model/test_currency_model.py`, a command-line desktop runner for a local photo.
It uses the bundled TFLite model, runtime tensor metadata, app-matching letterbox
preprocessing and YOLO decoding/NMS, prints predicted classes/scores, and writes an
annotated image. Added Windows setup instructions and pinned LiteRT/Pillow/NumPy
requirements in `model/README_TEST.md` and `model/requirements-test.txt`.

The harness syntax check passed. Runtime inference was not validated because this
environment's PyPI connection reset during the LiteRT runtime download. No banknote
sample image was available. On the laptop, install the dependencies and pass a photo
to the script as documented; the first dependency installation requires internet.

2026-10-24 follow-up: Ran the supplied `IMG_8424` photo locally after installing
LiteRT. Runtime confirmed `[1,3,640,640]` float input and `[1,14,8400]` float output.
Raw box coordinates are normalized 0..1, not model pixels; both desktop and Android
decoders had treated them as pixel coordinates, collapsing boxes and yielding no
annotations. Added auto-scaling for normalized coordinates, changed default confidence
threshold to 0.25 (the photo's top candidate score is 0.400), printed the top raw score,
and added a normalized-coordinate regression test. Rerun now emits a 200 Rand candidate
at 40.0%, but its box covers almost the whole photo and the denomination is likely
wrong. The sample is dim, folded and occluded; model quality/fit remains inadequate
for this image. Annotated output: `model/debug-result.jpg`. Android build/device test
and the unit test suite were not run in this session.

2026-10-24 rerun: Ran the available `34599ce9-IMG_E8379` photo on the laptop CPU.
Inference again confirmed `[1,3,640,640]` float input and `[1,14,8400]` float output.
The top result was `100 Loti` at 46.7%, while the pictured note is marked 100 Rand;
the predicted box spans much of the frame and is poorly localized. Annotation drawing
works, but this sample confirms the model still misclassifies and localizes poorly.
Annotated output: `model/34599ce9-IMG_E8379_JPG.rf.e325206d0e84dfad364fac99248cb12a_currency_result.jpg`.
