# ShortStop

ShortStop is a native Android prototype that uses an `AccessibilityService` to detect the Shorts interface in the official YouTube app and leave it, while leaving ordinary YouTube playback untouched.

Phase 2 is complete for tested YouTube version `21.35.442`. Its debug-build-only,
explicitly armed discovery tool produced a sanitized 13-capture evidence set.
The selected structural fields distinguish completed Shorts viewers from the
tested ordinary surfaces while safely leaving a Shorts-loading transition
unknown. Release builds contain no inspector or export UI, and no build performs
automatic detection or navigation yet.

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
- Deterministic, high-confidence detection followed by Android's Back action.
- Entirely on-device processing with no screenshots or captured screen content.
- Fail open when the YouTube interface cannot be classified safely.

## AI Notice

This repository is a way for me to gain some experience - if you can call it that - with vibe coding. The majority of the code and documentation is written by Codex.
