# Android development without Android Studio

## Recommended ShortStop workflow

Android Studio is convenient, but it is not required to build, install, or test
an Android application. For ShortStop, the preferred alternative is:

- VS Code, VSCodium, Cursor, or another LSP-capable editor;
- JetBrains' official Kotlin language server;
- a JDK compatible with the selected Android Gradle Plugin (AGP);
- Android CLI, which manages Android SDK packages;
- Android SDK Platform-Tools, which supplies `adb`;
- the project's Gradle wrapper for builds, lint, and tests;
- a physical Android device with the official YouTube app for accessibility
  discovery and end-to-end testing; and
- optionally, `scrcpy` for viewing and controlling that device from the desktop.

Implementation through the Phase 1 service shell has been approved. Dependency
installation and later implementation phases still require their corresponding
project approval and safety gates, as described in [AGENTS.md](../AGENTS.md)
and the [implementation plan](IMPLEMENTATION_PLAN.md).

## Choose a path for the machine

Check the operating system and CPU architecture:

```bash
uname -m
cat /etc/os-release
```

### Supported x86_64 Linux host

Use the complete local command-line workflow below. An emulator is optional;
ShortStop still needs final testing on physical hardware because its behavior
depends on the accessibility tree of the official YouTube app.

### ARM64 Linux host

Google currently states that Android Studio on Linux does not support ARM-based
CPUs. Do not try to work around that by running untrusted repackaged IDE or SDK
binaries.

Use the editor locally, but run builds and SDK tools on a supported x86_64
Linux machine reached through SSH or a remote-editor connection. That machine
can be a small workstation, VM, or CI runner. Keep the repository as the source
of truth and use the Gradle wrapper there. A physical phone may be connected to
the remote builder if USB forwarding is reliable, but it is usually simpler to
build remotely, download the debug APK, and install it from a machine that has
working `adb` access.

Firebase Studio is not a good new-project fallback: new workspace creation was
disabled in June 2026 and the service is scheduled to shut down in March 2027.

## Editor choices

### 1. VS Code family (recommended)

Install the official **Kotlin by JetBrains** extension. It supplies completion,
diagnostics, navigation, refactoring, and formatting. Its Android Gradle Plugin
support is currently experimental, so Gradle and lint output remain the
authoritative correctness checks.

Useful companion extensions include an XML editor and Gradle task integration.
Avoid extensions that claim to replace AGP, the Android SDK, or `adb`.

Trade-offs compared with Android Studio:

- no mature Compose Preview or Layout Inspector workflow;
- fewer Android-specific refactorings and manifest/resource inspections;
- device deployment is command-driven; and
- some Kotlin/AGP project import behavior may still be rough.

These limitations are acceptable for ShortStop because its detector and state
machine should be kept as pure Kotlin and tested heavily with local unit tests.

### 2. IntelliJ IDEA with Android plugins

IntelliJ IDEA can use JetBrains' **Android** and **Android Design Tools**
plugins. This offers a more IDE-like Kotlin experience, but the plugins are not
bundled and their versions must match the IntelliJ build. It can also be nearly
as resource-intensive as Android Studio. Treat this as a second choice, not as
a guaranteed workaround for an unsupported CPU architecture.

### 3. Terminal editor with Kotlin LSP

Neovim, Emacs, Helix, and similar editors can use an LSP client with JetBrains'
Kotlin language server. This is viable for an experienced terminal user, with
the same experimental AGP caveat as VS Code. All Android operations still run
through Gradle and SDK commands.

## Install the command-line toolchain on a supported host

The following is a host setup reference for when implementation is approved.
Use an actively supported JDK version required by the selected stable AGP; do
not install a separate Kotlin compiler or system Gradle.

Install basic prerequisites on Ubuntu:

```bash
sudo apt update
sudo apt install git curl unzip zip openjdk-17-jdk \
  android-sdk-platform-tools-common
sudo usermod -aG plugdev "$USER"
```

Confirm the selected AGP still supports JDK 17 before installation. If its
official compatibility table requires a newer JDK, install that version
instead. Log out and back in after the `plugdev` group change.

Install Google's current [Android
CLI](https://developer.android.com/tools/agents/android-cli/download). Prefer
the official apt repository instructions on that page when system-wide package
management is desired, or its local installer for a user-only installation.
After installation, update it and inspect the SDK location it selected:

```bash
android update
android -V
android info
```

`android info` prints the SDK directory currently managed by the CLI. If a
different directory is required, pass `--sdk=/exact/path` or add that setting
to `~/.androidrc` as documented by Android CLI. Do not configure one SDK path
in `ANDROID_HOME` and a different path in `.androidrc`.

Install Platform-Tools first. It is a separate SDK package and contains `adb`:

```bash
android sdk list '^platform-tools$' --all
android sdk install platform-tools
```

Add the `platform-tools` directory under the SDK path reported by
`android info` to `PATH`. Replace the example path with that exact path:

```bash
export ANDROID_HOME="${HOME}/Android/Sdk"
export PATH="${ANDROID_HOME}/platform-tools:${PATH}"
hash -r
"${ANDROID_HOME}/platform-tools/adb" --version
adb --version
```

Persist those two exports in the shell's startup file after the direct-path
check succeeds. If the direct path works but plain `adb` does not, installation
is complete and only `PATH` is wrong.

Use `android sdk list <pattern> --all` to identify the current stable platform
and exact package names. Install the platform selected for `compileSdk` and
matching build tools. For example, after substituting the versions selected at
implementation time:

```bash
android sdk list '^(platforms|build-tools)/' --all
android sdk install platforms/android-XX build-tools/XX.Y.Z
java -version
```

Review and accept Google's license prompts during installation. Install the
emulator and a Google Play system image only when the host supports accelerated
emulation.

After scaffolding is approved, always build through the checked-in wrapper:

```bash
./gradlew assembleDebug
./gradlew lint test
```

## Physical-device development (preferred)

Accessibility behavior must ultimately be tested against the official YouTube
app on physical hardware.

1. On the device, enable Developer options and USB debugging.
2. Connect it with a data-capable USB cable.
3. Accept the computer's debugging fingerprint.
4. Verify that `adb devices -l` reports the state `device`, not
   `unauthorized`.

Android 11 and later also support wireless debugging with `adb pair` and
`adb connect`. Use it only on a trusted network.

Once an APK exists, a complete terminal loop looks like:

```bash
./gradlew installDebug
adb shell am start -n com.shortstop.blocker/.MainActivity
adb logcat
./gradlew connectedAndroidTest
```

The exact activity name may change during implementation. Do not enable the
accessibility service with hidden commands: onboarding must send the user to
Android Accessibility Settings for explicit consent.

[`scrcpy`](https://github.com/Genymobile/scrcpy) is an optional open-source
desktop mirror and controller that works through `adb` without root. It is
useful for demonstrations and manual test runs but is not a test framework.
Install it only from the operating-system package source or the project's
official GitHub repository.

Keep at least one test device on a controlled YouTube version, with automatic
updates disabled, so detector changes can be validated against more than one
version. Do not store YouTube APKs in this repository.

## Optional command-line emulator

On x86_64 Linux, check KVM availability before downloading a system image:

```bash
lscpu | grep -E 'Architecture|Virtualization'
ls -l /dev/kvm
emulator -accel-check
```

If KVM is available, locate and install the Android Emulator and a stable
Google Play system image with `android sdk list` and `android sdk install`.
Create, list, start, and stop virtual devices with `android emulator`, as
described in the [official command-line emulator
guide](https://developer.android.com/studio/run/emulator-commandline).

A Google Play image is required for the official Play-distributed YouTube app.
An AOSP-only emulator is useful for ShortStop onboarding and lifecycle tests,
but it cannot validate the core YouTube detector.

## Automated and cloud testing

All routine checks work without an IDE:

```bash
./gradlew test
./gradlew lint
./gradlew connectedAndroidTest
```

AGP can also provision Gradle-managed virtual devices from the command line.
Use those for repeatable app-owned UI, lifecycle, and API-level tests when an
accelerated emulator is available. They do not replace the real-device YouTube
matrix.

[Firebase Test Lab](https://firebase.google.com/docs/test-lab) can run uploaded
instrumentation APKs on hosted physical and virtual devices through `gcloud`.
It is useful as a secondary compatibility matrix. Before relying on it for an
end-to-end Shorts case, prove that the chosen device model exposes the required
official YouTube version and that the test can complete the user-mediated
accessibility setup. Otherwise, reserve it for ShortStop-owned screens and
components.

A CI service on a supported x86_64 runner should at minimum run local unit
tests and lint for each change. Emulator and connected-device jobs may be added
later, but must not be reported as coverage of the manual YouTube matrix.

## Pre-implementation verification

Before scaffolding the application, verify:

```bash
java -version
android -V
android info
adb --version
adb devices -l
```

The selected workflow is ready when a minimal future project can build through
the Gradle wrapper and a physical device is visible to `adb`. Do not create that
project merely to complete this guide while the documentation phase gate is
active.

## Troubleshooting

- **Linux reports `aarch64` or `arm64`:** use a supported remote x86_64 builder;
  switching editors does not make Google's unsupported Linux Android toolchain
  supported.
- **Gradle reports an unsupported Java version:** compare the selected AGP
  release notes with `java -version`, then select a compatible JDK.
- **`sdkmanager` prints a deprecation warning:** use `android sdk` instead. The
  old command may be a compatibility shim and does not need to be invoked.
- **`adb` is missing:** run `android sdk install platform-tools`, then include
  the managed SDK's `platform-tools` directory in `PATH`.
- **Device is unauthorized:** revoke USB debugging authorizations on the phone,
  reconnect, and accept the new prompt.
- **USB device is absent on Ubuntu:** confirm `plugdev` membership, udev rules,
  the cable, and the USB port.
- **No `/dev/kvm`:** use a physical device or a remote/cloud test host; software
  emulation is too slow for the primary development loop.
- **Editor analysis disagrees with Gradle:** trust Gradle, lint, and tests, and
  report a minimized issue to the Kotlin language-server project.

## Safely retire a legacy SDK installation

Do not remove anything merely because the `sdkmanager` command exists. Android
CLI distributions can include it as a deprecated compatibility shim.

First inventory every command and identify the active SDK:

```bash
type -a android
type -a sdkmanager
type -a adb
android info
```

If `sdkmanager` is under the SDK reported by `android info`, leave it alone and
simply stop using it. If it resolves to a genuinely separate, older SDK root:

1. Record its exact path from `type -a sdkmanager`.
2. Remove only that old root's `cmdline-tools`, `tools`, and `platform-tools`
   entries from the shell startup file. Keep the active SDK entry.
3. Start a new shell and repeat the inventory commands.
4. Rename or move the exact old SDK directory to a clearly dated backup rather
   than immediately deleting it.
5. Build and connect a device successfully using the active SDK. Delete the
   backup later through the file manager only after confirming it is unused.

Packages within the active SDK should be removed through Android CLI:

```bash
android sdk remove <exact-package-name>
```

If an old SDK package was installed by Ubuntu apt rather than extracted
manually, inspect the package database before removal:

```bash
dpkg -l | grep -E 'android|platform-tools|adb'
```

Use `sudo apt remove <exact-installed-package>` only for a package shown by that
command and no longer needed. The shell suggestion to install `adb` does not
mean either suggested apt package is currently installed.

## Sources checked

This recommendation was checked on 6 September 2026 against the official
[Android CLI](https://developer.android.com/tools/agents/android-cli),
[ADB](https://developer.android.com/tools/adb),
[command-line build](https://developer.android.com/build/building-cmdline),
[command-line test](https://developer.android.com/studio/test/command-line),
[hardware-device](https://developer.android.com/studio/run/device), and
[Android Studio system-requirements](https://developer.android.com/studio/install)
documentation; JetBrains' [Kotlin
language-server](https://kotlinlang.org/docs/kotlin-lsp.html) and [IntelliJ
Android](https://www.jetbrains.com/help/idea/create-your-first-android-application.html)
documentation; and the Firebase Test Lab documentation linked above.
