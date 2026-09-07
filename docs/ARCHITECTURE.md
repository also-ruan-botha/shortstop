# Architecture

## System overview

```text
Android accessibility events
          |
          v
 YouTubeEventGate ---------> ignored non-YouTube/paused events
          |
          v
 AccessibilityTreeReader
          |
          v
 SanitizedNodeTree
          |
          v
 ShortsDetector + versioned rules
          |
          v
 DetectionDecision
          |
          v
 EjectionStateMachine -----> Back action / cooldown / user overlay
```

The Compose application is a configuration and status surface. The Android
system owns the lifetime of the accessibility service; the service must not
depend on a running activity or Compose runtime.

## Components

### Application UI

Responsibilities:

- prominent disclosure and onboarding;
- deep link to Android Accessibility Settings;
- service enabled/disabled status;
- pause and resume controls;
- privacy explanation and troubleshooting;
- debug tooling in debug builds only.

It persists only settings such as paused state and whether onboarding was
acknowledged.

All application UI follows Material Design 3 guidelines. Screens use Material
3 components and design tokens for typography, color, shape, spacing, and
motion. They must support light and dark themes, scalable text, accessible
semantics and touch targets, edge-to-edge presentation, and adaptive layouts
for the supported phone configurations. A custom control may replace an
equivalent Material 3 component only when its rationale and accessibility
behavior are documented.

### `ShortStopAccessibilityService`

Responsibilities:

- accept only relevant events from `com.google.android.youtube`;
- ignore events while paused or cooling down;
- obtain the active root safely;
- pass framework nodes to the tree reader;
- execute decisions through Android accessibility actions;
- clear state when the active package changes.

It must never retain an `AccessibilityNodeInfo` beyond processing the event.
Recycle nodes where required by the supported Android API behavior.

### Sanitized node model

The pure model contains only properties necessary for structural detection:

- normalized resource-ID suffix;
- Android class or semantic role;
- selected, clickable, scrollable, and visible flags;
- tree depth, child count, and structural relationships;
- optional coarse bounds/layout facts only if proven necessary.

Text and content descriptions are excluded from stored fixtures. If a later
fallback temporarily inspects a content description in memory, it must be
documented, localized, never logged, and unable to confirm Shorts by itself.

### Detector

The detector is a pure Kotlin component with no Android service dependency. It
evaluates a bounded tree against versioned signatures and returns a decision
plus non-sensitive reason codes.

Confirmation requires multiple independent markers. Rules may recognize a
known layout but must not infer Shorts merely because a video is vertical,
brief, full-screen, or swipeable.

### Ejection state machine

The state machine controls timing and prevents loops. One confirmed encounter
permits at most two Back actions. A cooldown absorbs the burst of accessibility
events produced by navigation.

If Back cannot exit, automation stops for that encounter. A user-controlled
accessibility overlay may then offer a manual exit or temporary pause. The
overlay must not mimic YouTube or cover unrelated applications.

### Rule repository

Prototype rules ship inside the application and are immutable at runtime. Each
rule set records:

- internal schema version;
- YouTube versions on which it was observed;
- required and supporting signals;
- fixtures covering matching and non-matching layouts.

Remote rule updates are deferred. If later introduced, they must be
declarative, signed, schema-validated, rollback-capable, and unable to request
new data or actions.

## Data and privacy flow

Accessibility trees remain in process and in memory. Only user settings and
non-content state are persisted. Release builds have no Internet permission,
which makes the no-transmission boundary enforceable at the manifest level.

Debug fixtures are manually generated, sanitized structural data. They must be
reviewed before being committed to ensure no user-visible content or account
information remains.

## Failure behavior

- Missing root: ignore the event.
- Partial tree: return `UnknownLayout` unless high-confidence requirements are
  still satisfied.
- Unknown YouTube version: apply proven generic structural rules, otherwise
  fail open.
- Back action rejected: retry only after reclassification and never more than
  twice.
- Rapid repeated events: coalesce and enforce cooldown.
- Permission removed: clear state and show disabled status next time the app
  opens.
- Process restarted: restore pause preference; do not restore an in-progress
  ejection encounter.

## iOS limitation

iOS does not expose another application's accessibility hierarchy or permit a
third-party app to issue an equivalent cross-application Back action. The core
behavior is therefore Android-specific. Any future iOS product would need a
different, more limited definition, such as Safari URL blocking or whole-app
Screen Time controls.
