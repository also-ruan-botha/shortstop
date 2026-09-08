# Implementation plan

## Goal and success criteria

Build a native Android prototype that detects the Shorts viewing interface in
the official YouTube application and leaves it promptly, without interrupting
ordinary videos or collecting user content.

The prototype succeeds when it:

- detects Shorts opened from the Shorts tab, Home, search, or a deep link;
- exits a confirmed Shorts screen within one second of a usable accessibility
  event;
- causes zero false exits in the defined 100-action ordinary-YouTube test run;
- avoids action loops and stops after two unsuccessful Back actions;
- performs all classification locally and requests only accessibility access;
- fails open after an unrecognized YouTube layout change.

## Fixed technical decisions

- Application ID: `com.shortstop.blocker`
- Display name: `ShortStop`
- Language/UI: Kotlin and Jetpack Compose; all user-facing UI must follow
  Material Design 3 guidelines
- Build: Gradle Kotlin DSL and version catalog
- Minimum Android version: API 26
- Compile/target version: latest stable SDK supported by the chosen stable
  Android Gradle Plugin at implementation time
- Persistence: Preferences DataStore for pause state and user settings
- Dependency injection: manual constructor injection for the prototype
- Network access: none
- Analytics/crash reporting: none

## Phase 0: environment and scaffold

Status as of 7 September 2026:

- complete: single-module Compose scaffold, version catalog, debug/release
  build types, local and instrumented test source sets, Gradle wrapper,
  formatting, lint, test, build, installation, and launch commands; and
- verified by the project owner: the app builds, installs, launches, and passes
  the Phase 0 local and instrumented test gate.

1. Complete [the Android development setup guide](ANDROID_SETUP_UBUNTU.md)
   using the local or remote command-line path appropriate for the host.
2. Confirm a physical Android device can be reached with `adb`. Use an emulator
   only if KVM becomes available.
3. Create a single-module Android application with Compose enabled.
4. Add debug and release build types and standard unit/instrumentation source
   sets. Do not add product flavors initially.
5. Add formatting, Android lint, unit-test, and connected-test commands to the
   contributor README.

Exit gate: the empty app builds, installs, launches, and runs one local and one
instrumented test.

## Phase 1: onboarding and service shell

Status as of 7 September 2026: complete and manually verified by the project
owner. The service accepts only configured YouTube events, observes the
persisted pause preference, and performs no automatic accessibility action.

1. Add an onboarding screen explaining:
   - what ShortStop observes;
   - that it reacts only inside YouTube;
   - that it presses Back after detecting Shorts;
   - that no screen content leaves the device;
   - how to disable access.
2. Require an explicit acknowledgement before opening Android Accessibility
   Settings.
3. Declare an accessibility service protected by
   `android.permission.BIND_ACCESSIBILITY_SERVICE`.
4. Configure it for the YouTube package, window-state/window-content events,
   view-ID reporting, and window-content retrieval.
5. Show live enabled/disabled status when the app returns to the foreground.
6. Provide enabled, paused, and unsupported-layout states. Pausing prevents
   actions without attempting to disable the Android service.
7. Implement every user-visible state with Material 3 components and design
   tokens, including light/dark themes, scalable text, accessible semantics and
   touch targets, edge-to-edge presentation, and adaptive phone layouts.

Exit gate: the user can understand, enable, pause, resume, and disable the
service, but it performs no navigation action yet. The UI also passes Material
3 and accessibility review for the supported phone configurations.

Device verification covered the first-run acknowledgement gate, return from
Accessibility Settings, live enabled/disabled refresh, pause persistence, and
the service's no-navigation behavior.

## Phase 2: safe discovery tooling

Status as of 8 September 2026: complete for tested YouTube version `21.35.442`.
The debug-only, one-shot inspector and local JSON export are implemented. Two
required non-textual signal groups distinguish both completed Shorts captures
from ten negative captures. A separate Shorts-loading capture correctly lacks
the signature and establishes the required fail-open transition behavior.

The evidence and deliberately rejected signals are recorded in
[PHASE_2_CAPTURE_ANALYSIS.md](PHASE_2_CAPTURE_ANALYSIS.md). Compatibility is not
inferred beyond the tested YouTube version, and the negative suite must keep
expanding before action-capable phases can pass their gates.

1. Build a debug-only inspector that traverses the current YouTube
   accessibility tree after an explicit developer action.
2. Convert framework nodes into a sanitized structural representation:
   resource-ID suffix, class/role, boolean capabilities, child count, depth,
   and selection state.
3. Do not store visible text, content descriptions, bounds screenshots, video
   names, or account information.
4. Export fixtures only through an explicit debug action and keep them local.
5. Collect fixtures from Shorts and ordinary YouTube surfaces, recording the
   YouTube app version and Android version alongside each fixture.

Exit gate: at least two independent non-textual signals distinguish the tested
Shorts layouts from normal playback. If this cannot be demonstrated across the
available test versions, stop rather than implementing speculative actions.

## Phase 3: detector engine

1. Define the sanitized node model and these results:
   `NotShorts`, `PossibleShorts`, `ConfirmedShorts`, and `UnknownLayout`.
2. Implement versioned declarative signatures using resource IDs, roles,
   hierarchy, selection state, and other non-content properties.
3. Require multiple high-confidence signals for `ConfirmedShorts`.
4. Keep optional localized-text fallbacks isolated and insufficient on their
   own to trigger an action.
5. Create fixtures for every observed Shorts entry path and representative
   ordinary surfaces.
6. Unit-test traversal limits, malformed trees, missing nodes, partial trees,
   ambiguous matches, and signature-version selection.

Exit gate: all fixtures classify as expected, and no ordinary fixture reaches
`ConfirmedShorts`.

## Phase 4: action state machine

Implement these states:

1. `Monitoring`: inspect eligible YouTube events.
2. `Suspected`: coalesce rapid events and request one fresh tree.
3. `Ejecting`: issue one `GLOBAL_ACTION_BACK` for a confirmed screen.
4. `Verifying`: wait briefly for the resulting window event and reclassify.
5. `Cooldown`: suppress duplicate actions for the same encounter.
6. `NeedsUserExit`: after two unsuccessful Back actions, stop automation and
   offer an accessibility overlay with Leave YouTube and Pause controls.

Do not issue Home automatically. Reset encounter state after YouTube leaves the
foreground or a non-Shorts layout is confirmed.

Exit gate: all defined entry paths exit correctly without loops or unrelated
navigation.

## Phase 5: hardening and validation

1. Execute the matrix in [TESTING.md](TESTING.md) on a physical device.
2. Measure detection latency and event volume.
3. Use Android battery tools to confirm there is no polling or unexpected
   background CPU use.
4. Test process death, reboot, orientation changes, YouTube restarts, rapid
   navigation, and permission removal.
5. Remove or compile out debug inspection and fixture-export behavior from the
   release build.
6. Complete the disclosure and Play Console preparation in
   [POLICY_AND_PRIVACY.md](POLICY_AND_PRIVACY.md).

Exit gate: all acceptance criteria pass on the supported test matrix. A failure
to maintain low false positives blocks public distribution.

## Phase 6: later target applications

Instagram or Facebook support is not part of the YouTube prototype. After the
YouTube detector is proven:

1. Add a `TargetAdapter` abstraction for package identity, signatures,
   cooldown, and exit behavior.
2. Research each application's accessibility tree independently.
3. Require the same privacy, fail-open, fixture, and false-positive gates.
4. Update the accessibility disclosure and store declaration before release.

## Deliberate exclusions

- Flutter
- Replacement YouTube player or YouTube API integration
- VPN/DNS filtering or TLS interception
- Screenshots, OCR, or image recognition
- Remote rule delivery in the prototype
- User accounts, cloud sync, analytics, and advertising
- Automatic Home action or attempts to disable parts of YouTube's UI
- iOS parity claims
