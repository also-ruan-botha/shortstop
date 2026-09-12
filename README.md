# ShortStop

ShortStop is a native Android prototype that uses an `AccessibilityService` to detect the Shorts interface in the official YouTube app and leave it, while leaving ordinary YouTube playback untouched.

Phase 4 is implemented and awaiting manual device verification. The event-driven
service coalesces YouTube events, classifies one fresh sanitized tree, presses
YouTube's own Home tab only for confirmed Shorts, and verifies the result. It remains
active for future confirmed encounters until the user pauses it or disables
its accessibility access. Retries are driven by later YouTube events rather
than a polling loop. Unsupported and partial layouts remain non-actionable.
YouTube version metadata is recorded for diagnostics but does not gate
detection: the structural rule is attempted on every installed official
YouTube version.

The behavior and verification matrix are documented in
[`docs/PHASE_4_ACTION_STATE_MACHINE.md`](docs/PHASE_4_ACTION_STATE_MACHINE.md).
Deferred post-MVP ideas are tracked in [`docs/ROADMAP.md`](docs/ROADMAP.md).

## Development commands

From the repository root:

```bash
./gradlew spotlessCheck lint test
./gradlew assembleDebug
./gradlew installDebug
./gradlew connectedDebugAndroidTest
```

## Direction

- Native Kotlin and Jetpack Compose, with all user-facing UI following Material Design 3 guidelines.
- Android first, until Apple starts giving a shit about open source development, or I start giving a shit about Apple.
- Event-driven accessibility inspection, scoped to the YouTube package. Privacy is of utmost importance and that should be clear to the user
- Deterministic, high-confidence detection followed by a click on YouTube's
  own Home tab.
- Entirely on-device processing with no screenshots or captured screen content.
- Fail open when the YouTube interface cannot be classified safely.

## AI Notice

This repository is a way for me to gain some experience - if you can call it that - with vibe coding. The majority of the code and documentation is written by Codex.
