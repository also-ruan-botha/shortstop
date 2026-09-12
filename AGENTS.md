# ShortStop repository instructions

These instructions apply to the entire repository.

## Current phase gate

The repository is documentation-only. Do not scaffold an Android project,
write application code, add binary assets, or install dependencies unless the
user explicitly approves implementation. Documentation may be corrected or
refined when requested.

## Source-control boundary

Codex or any other LLM must never run `git add`, `git commit`, `git push`, or `git pull` in this
repository. Staging, committing, and synchronizing with remotes are always the
project owner's responsibility.

## Product boundary

ShortStop is an Android accessibility-based blocker for YouTube Shorts. It
must preserve normal use of the official YouTube application. Do not introduce
a replacement YouTube client, local VPN, DNS filter, TLS interception, root
requirement, modified YouTube APK, private YouTube API, or Flutter runtime.

## Required implementation stack

When implementation is approved, use:

- Kotlin
- Jetpack Compose and Material 3
- Gradle Kotlin DSL with a version catalog
- AndroidX `AccessibilityService`, DataStore, Lifecycle, and Test APIs
- JUnit for local tests and AndroidX Test/UI Automator for device tests

Use `com.shortstop.blocker` as the prototype application ID, `ShortStop` as the
display name, API 26 as `minSdk`, and the latest stable SDK available at
implementation time for `compileSdk` and `targetSdk`. Use a JDK version
supported by the selected Android Gradle Plugin; Android Studio's bundled JDK
is optional, not required.

## Safety and privacy invariants

- Restrict accessibility events and node inspection to
  `com.google.android.youtube`.
- Never capture screenshots, audio, video, typed text, account data, search
  terms, video titles, comments, or browsing history.
- Never transmit accessibility data or add analytics to the prototype.
- Do not request Internet, VPN, notification-listener, usage-access,
  device-administrator, or root privileges.
- The app must not enable its own accessibility service or obstruct disabling
  or uninstalling it.
- Detection is deterministic and rule-based. Do not add OCR, machine learning,
  packet inspection, or remote executable rules.
- A low-confidence or unknown layout must fail open.
- Click YouTube's own Home tab, never Android Home or Back, for a confirmed
  Shorts encounter. Keep the service available for future encounters without a
  fixed action-count limit, and require a later eligible event plus fresh
  confirmation before retrying so no polling or action loop is created.
- Release builds must not log accessibility-node text or content descriptions.

## Architecture constraints

- Keep Android framework traversal separate from the pure detection engine.
- Convert `AccessibilityNodeInfo` instances into a minimized, sanitized tree
  model before detection.
- Treat rules as versioned declarative data so YouTube-specific signatures do
  not leak throughout the service code.
- Keep action execution separate from detection decisions.
- Any future Instagram or Facebook support must be a separate target adapter
  with its own rules and tests.

## UI design requirements

- All user-facing UI must follow Material Design 3 guidelines.
- Use Material 3 components, typography, color, shape, spacing, and motion
  patterns instead of recreating equivalent controls without a documented
  reason.
- Support light and dark themes, scalable text, accessible semantics, minimum
  touch-target sizes, edge-to-edge layouts, and adaptive behavior across the
  supported phone configurations.
- Treat Material 3 compliance and accessibility as review requirements for
  every user-visible screen change.

## Quality gates

- Add tests with every detector-rule change.
- Test normal YouTube surfaces as aggressively as Shorts surfaces; false exits
  are the most serious functional failure.
- Do not assert guessed YouTube resource IDs as facts. Discover them on a test
  device, record the tested YouTube version, and keep sanitized fixtures.
- Keep the service event-driven. Do not add continuous polling.
- Run formatting, lint, unit tests, and relevant device tests before declaring
  implementation work complete.
- Recheck current Google Play Accessibility API policy before each release.

## Documentation discipline

Keep architectural decisions and user-visible behavior synchronized with the
documents under `docs/`. Record policy-sensitive changes explicitly. Never
claim that Google Play approval is guaranteed.
