# ntsamaise

Android assistive research prototype for blind and partially sighted users in Lesotho.
See [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) for the intended research scope.

## Spoken object detection

The scanner uses MediaPipe Tasks Vision 1.0.0 with a bundled EfficientDet-Lite0
int8 model. It recognizes 80 COCO object classes (such as people, chairs, bottles,
and dogs) on-device, without a first-run model download. This replaces ML Kit's
five broad default categories. Model details and trade-offs are documented in
[Google's object detection guide](https://developers.google.com/edge/mediapipe/solutions/vision/object_detector).

Inference runs on a single camera worker with CameraX dropping stale frames.
Frames are rotated upright before inference. Results below 50% confidence are
discarded; the highest-confidence remaining label is displayed and spoken.
"No object recognized" means no supported class passed that threshold, not that
the surroundings are empty. Loti notes and local product brands require separately
trained models; this model does not provide those capabilities.

The bundled model is `app/src/main/assets/efficientdet_lite0.tflite` (4.6 MB).
Source: [Google model storage, version 1](https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/int8/1/efficientdet_lite0.tflite).
SHA-256: `0720bf247bd76e6594ea28fa9c6f7c5242be774818997dbbeffc4da460c723bb`.

Open **Scan Surroundings** and allow camera access. Stable detection results are
read aloud in English using the phone's text-to-speech engine. An installed offline
English voice is preferred; availability depends on the phone's speech engine.
Confidence percentages remain on screen only.

- Results must remain unchanged for 0.8 seconds before speech is considered.
- New announcements are spaced at least 3 seconds apart.
- An unchanged result is repeated after 8 seconds.
- Speech stops when leaving the scanner or putting the app in the background.
- If speech initialization fails, the scanner displays guidance about phone TTS settings.

### Phone verification

1. Turn up media volume, open the scanner, and hold the camera steady on an object.
2. Confirm the spoken label matches the displayed result, without reading percentages.
3. Keep the same scene in view and check that speech does not repeat on every frame.
4. Point at a different object and confirm the announcement changes.
5. Press Back, then test backgrounding the app while scanning; speech should stop.
6. With an offline English voice installed, repeat the speech check in airplane mode.
7. Test a person, chair, bottle, and cup in portrait and landscape orientations.
   Confirm specific labels and check responsiveness on the intended phone.
8. Leave and reopen the scanner repeatedly; confirm preview and detection recover.

Build and unit tests: `./gradlew :app:assembleDebug :app:testDebugUnitTest`.
