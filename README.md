# ntsamaise

Android assistive research prototype for blind and partially sighted users in Lesotho.
See [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) for the intended research scope.

## Spoken object detection

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

Build and unit tests: `./gradlew :app:assembleDebug :app:testDebugUnitTest`.
