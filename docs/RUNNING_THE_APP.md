# Build, install, and run ShortStop

This guide uses Android CLI, the Gradle wrapper, and `adb`; Android Studio is
not required.

## What the Phase 0 app does

The current app is an installation-ready shell. When opened, it displays
**ShortStop**, **Phase 0 ready**, and a notice that Shorts blocking is not active
yet. It does not declare an accessibility service, inspect YouTube, or perform
Back actions. Those behaviors begin in later implementation phases.

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

The test launches the activity and verifies the Phase 0 text using Compose Test
and UI Automator. Emulator startup is not managed by this task; start or connect
the device first.

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

Uninstalling is destructive to the app's local settings. The current Phase 0
shell has no user settings yet.

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
