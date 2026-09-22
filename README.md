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

## Read Text

Open **Read Text**, allow camera access, point at printed text, then tap
**Capture and read**. The app recognizes one camera frame, displays the passage
and reads it aloud once. The result stays fixed as the camera moves. Use
**Read again** to restart the passage, **Stop reading** to silence it, or
**Capture and read** for another passage. **Cancel capture** discards a pending
capture; cancelled results cannot replace a later capture.

The app bundles ML Kit's Latin-script text recognition model
(`com.google.mlkit:text-recognition:16.0.1`) for on-device OCR without a first-run
model download. See [the Android OCR guide](https://developers.google.com/ml-kit/vision/text-recognition/v2/android).
Reading aloud uses the shared English TTS engine and prefers an installed offline
voice. Long passages are split into ordered chunks within the engine's input
limit. Camera frames are not saved as image files. Recognized text is kept in
screen state, including restoration across rotation; there is no reading-history
feature. OCR does not translate text, and Sesotho speech is not implemented.

Both scanning modes share camera permission handling, lifecycle binding and
worker cleanup. Backgrounding or leaving Read Text cancels pending captures and
stops speech. Returning does not automatically restart a passage; use Read again.

### Read Text phone verification

1. Open Read Text, deny camera permission, and confirm the permission guidance and
   Allow camera button. Grant permission and confirm the preview appears.
2. Capture a clear, well-lit printed paragraph. Compare the displayed and spoken
   words with the original. Move the camera and confirm the result stays fixed.
3. Capture a blank surface. Confirm the no-text guidance and try again.
4. Test portrait and landscape captures, small print and several lighting levels.
5. Test Read again, Stop reading and Cancel capture. Start a new capture after
   cancellation and confirm an older result cannot replace it.
6. Read a passage longer than 4,000 characters. Confirm it continues through the
   complete passage and that Stop reading clears the queued speech.
7. Press Back and background the app during recognition and speech. Confirm speech
   stops and does not restart by itself on returning.
8. With an installed offline English voice, repeat in airplane mode, including a
   fresh app launch. Test TalkBack navigation and enlarged system text.
9. Recheck Scan Surroundings after using Read Text to verify shared camera and
   speech resources are released correctly.

## Identify Money: OCR prototype

Open **Identify Money**, show one flat Lesotho banknote in good light and tap
**Scan banknote**. Keep the whole side with the bank name visible. A result is
displayed and spoken once; use **Repeat result**, **Stop reading**, or scan again.
Capture cancellation, backgrounding, rotation and camera permissions reuse the
Read Text workflow. Only the result message is retained in currency screen state,
not the raw OCR text or serial number.

This is an offline text-based prototype, not a trained visual banknote model.
Its supported denominations are 10, 20, 50, 100 and 200 maloti, matching the
[Central Bank of Lesotho currency page](https://centralbank.org.ls/currency/).
The issuer phrases are **Central Bank of Lesotho** and **Banka E Kholo Ea Lesotho**,
also shown in the [CBL 20-maloti information leaflet](https://centralbank.org.ls/wp-content/uploads/LSO_20_Maloti-min.pdf).

A possible match requires the issuer phrase, the word **maloti**, and either two
matching standalone denomination numerals or one numeral plus the matching
English amount (for example, **TWENTY MALOTI**). Missing or conflicting evidence
produces retry guidance. The parser does not correct guessed digits, extract
denominations from serial numbers, or assign a confidence percentage.

Results always say **Possible … maloti**. OCR can misread a note and can also match
text on a leaflet, receipt or screen; this feature does not establish that the
camera is viewing a banknote or that a note is genuine. It does not count notes,
sum money, recognize coins, translate speech into Sesotho or identify other
currencies. A side without readable issuer text may fail; try the other side.
Real-banknote accuracy has not been measured. A trained visual model and a
representative evaluation dataset remain future work.

### Currency phone verification

1. Test each supported denomination with different physical notes and both sides.
   Record correct suggestions, incorrect suggestions and rejected captures separately.
2. Test folded/worn notes, glare, low light, distance, orientation and partial views.
3. Test blank paper, ordinary numbers, receipts, printed currency information,
   foreign notes and mixed denominations. Record false suggestions; matching
   banknote text on other media is a known limitation of this prototype.
4. Test Repeat result, Stop reading, Cancel capture, switching modes, backgrounding
   and rotation. Old results must not replace a new capture or restart speech.
5. Repeat in airplane mode with an installed offline English voice, and with TalkBack.

Unit tests cover the parsing rules and rejection cases. The Android integration
test uses synthetic printed text, not a real banknote dataset, and does not measure
banknote recognition accuracy.

Build and unit tests: `./gradlew :app:assembleDebug :app:testDebugUnitTest`.
The Android OCR smoke test renders a known printed phrase and checks recognition;
run it on a connected device with `./gradlew :app:connectedDebugAndroidTest`.
