# Build, install, and run ShortStop

This guide uses Android CLI, the Gradle wrapper, and `adb`; Android Studio is
not required.

## What the Phase 4 app does

The current app presents the accessibility disclosure and requires an explicit
acknowledgement before it can open Android Accessibility Settings. After setup,
it reports whether the service is disabled, enabled, or paused, and provides a
persistent pause control plus instructions for disabling access.

The declared service receives only window-state and window-content events from
the official YouTube package. It coalesces events, sanitizes one fresh tree,
and clicks YouTube's own Home tab only after the structural detector confirms
Shorts.
YouTube version metadata is retained for diagnostics but does not disable the
rule. The service verifies the result and remains active for future confirmed
encounters until paused or disabled. Inconclusive retries wait for later
YouTube events, so continued operation does not introduce continuous polling.

The status screen warns that YouTube updates can stop blocking or interfere
with normal YouTube use. If that occurs, report the bug and disable ShortStop in
Android Accessibility settings.

Debug builds also provide the explicitly armed, one-shot structural inspector.
It cannot read text or content descriptions and does not record bounds,
screenshots, media, or user activity history. Release builds do not contain the
inspector or export UI.

## 1. Verify the toolchain and device

From the repository root:

```bash
java -version
android -V
android info
adb --version
adb devices -l
```

The target device or running emulator must appear with state `device`. A state
of `unauthorized` means its debugging authorization prompt still needs to be
accepted.

The project currently compiles against API 37. If it is not installed:

```bash
android sdk install platforms/android-37 build-tools/36.0.0
```

## 2. Build and test locally

```bash
./gradlew spotlessCheck lint test
./gradlew assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 3. Install the debug build

The simplest path builds and installs in one command:

```bash
./gradlew installDebug
```

Alternatively, install an already-built APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug application ID is `com.shortstop.blocker.debug`. It is intentionally
different from the future release ID so both builds can coexist during testing.

## 4. Open the installed app

Tap **ShortStop** in the device launcher, or start it from the terminal:

```bash
adb shell am start -n com.shortstop.blocker.debug/com.shortstop.blocker.MainActivity
```

Android CLI can also install and open the APK:

```bash
android run \
  --apks=app/build/outputs/apk/debug/app-debug.apk \
  --activity=com.shortstop.blocker.MainActivity
```

If more than one device is connected, pass `-s <serial>` to `adb`, or
`--device=<serial>` to `android run`. Obtain serials from `adb devices -l`.

## 5. Run the device test

```bash
./gradlew connectedDebugAndroidTest
```

The test launches the activity and verifies that the prominent disclosure is
visible and the Accessibility Settings button is disabled until acknowledgement.
It also confirms the application is foregrounded using UI Automator. Emulator
startup is not managed by this task; start or connect the device first.

## 6. Verify the Phase 4 flow manually

1. Start from a clean install and confirm the settings button is initially
   disabled.
2. Read and select the acknowledgement, then open Accessibility Settings.
3. Enable ShortStop manually and return to the app. Confirm the status changes
   to **ShortStop is enabled**.
4. Turn on **Pause ShortStop**, restart the app, and confirm the paused state is
   retained. Resume it and confirm enabled status returns.
5. Open Accessibility Settings from the status screen, disable ShortStop, and
   return. Confirm the app reports that access is off.
6. Repeat the UI review in light and dark theme, portrait and landscape, and at
   the largest supported display and font settings.
7. With ShortStop resumed, open each completed Shorts entry path documented in
   `TESTING.md`. Confirm that YouTube's Home tab is selected promptly only after
   the completed Shorts viewer appears.
8. Exercise regular playback, full-screen playback, a short ordinary video,
   picture-in-picture, search autoplay, loading screens, and ordinary
   navigation. Confirm none causes a Home-tab click.
9. Pause ShortStop and confirm a completed Shorts screen causes no action.
10. Repeat confirmed Shorts entry many times and confirm each encounter returns
    to YouTube's Home tab; ShortStop must not stop after any fixed number of
    actions.
11. Exercise a persistent or rapidly repeated Shorts case. Confirm retries occur
    only after later YouTube events and fresh confirmation, with no continuous
    navigation loop, and confirm the cooldown does not eject ordinary screens.

## Useful commands

```bash
# Apply formatting
./gradlew spotlessApply

# Check formatting, Android lint, and local tests
./gradlew spotlessCheck lint test

# Stop the app
adb shell am force-stop com.shortstop.blocker.debug

# Remove the debug app and its local data
adb uninstall com.shortstop.blocker.debug
```

Uninstalling is destructive to the saved onboarding acknowledgement and pause
state. Android may retain or clear the separately managed accessibility-service
setting depending on device behavior; verify it after reinstalling.

## Capture a sanitized Phase 2 fixture

This workflow is available only in a debug build:

1. Enable the ShortStop accessibility service and make sure it is not paused.
2. In ShortStop, select **Arm one capture** under **Debug discovery**.
3. Switch to the exact YouTube surface being documented. The next eligible
   YouTube event captures one bounded structural tree.
4. Return to ShortStop, review the reported node count and truncation state,
   then select **Export sanitized fixture**.
5. List the app-private exports:

   ```bash
   adb shell run-as com.shortstop.blocker.debug \
     ls files/debug-fixtures
   ```

6. Copy a named fixture to the local development machine for review:

   ```bash
   adb exec-out run-as com.shortstop.blocker.debug \
     cat files/debug-fixtures/youtube-EXACT_TIMESTAMP.json \
     > youtube-EXACT_TIMESTAMP.json
   ```

Before retaining a fixture, inspect it and confirm it contains only the fields
documented in `ARCHITECTURE.md`. Record whether the surface was Shorts or an
ordinary YouTube surface separately; the export deliberately does not infer or
store that label. Capture comparable negative surfaces near every positive
surface. Never automate accessibility-service enablement for this workflow.

## Troubleshooting

- **`adb` is missing:** install it with
  `android sdk install platform-tools`, then add the managed SDK's
  `platform-tools` directory to `PATH`.
- **`SDK location not found`:** create an untracked `local.properties` file
  containing `sdk.dir=/exact/path/reported/by/android-info`, or set
  `ANDROID_HOME` to that same path.
- **API 37 is missing:** run the `android sdk install` command in step 1.
- **The device is unauthorized:** unlock it and accept the USB debugging
  fingerprint, then rerun `adb devices -l`.
- **No device is found:** start the emulator or connect a physical phone. On
  Ubuntu, also verify the `plugdev` group and Android udev rules.
