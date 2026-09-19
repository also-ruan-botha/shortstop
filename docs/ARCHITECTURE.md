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
 EjectionStateMachine -----> YouTube Home-tab click / verification / cooldown
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

`MainActivity` refreshes the system-owned service-enabled state in
`onResume`, so returning from Accessibility Settings immediately updates the
status surface. `UserPreferencesRepository` stores only the onboarding
acknowledgement and pause state in Preferences DataStore. The disabled state
takes precedence over paused, and paused takes precedence over detector status.

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

The manifest and service metadata limit delivery to window-state and
window-content events from `com.google.android.youtube`; the callback repeats
the package and pause gates. In debug builds, an explicitly armed discovery
bridge may consume the next eligible event and read one tree. In release builds
that bridge is a no-op. Phase 4 is the first phase permitted to add an
automatic navigation action.

### Debug discovery flow

The inspector cannot capture when ShortStop is foregrounded because the active
root would belong to ShortStop. The developer therefore arms a one-shot capture
in the debug UI and then switches to the intended YouTube surface. The next
eligible YouTube event reads one active tree; there is no polling or continuous
recording.

Traversal is bounded to 1,000 nodes, depth 40, and 100 children per node. A
capture records whether a limit was reached, recycles framework nodes where the
Android API requires it, and converts them immediately into the pure sanitized
model. The framework tree is never retained.

Export is a second explicit action. It writes JSON only to the debug app's
private `files/debug-fixtures` directory. The debug implementation and UI live
under the `debug` source set; the `release` source set supplies only a no-op
bridge and renders no discovery panel.

### Sanitized node model

The pure model contains only properties necessary for structural detection:

- normalized resource-ID suffix;
- Android class or semantic role;
- selected, clickable, scrollable, and visible flags;
- tree depth, child count, and structural relationships;
- optional coarse bounds/layout facts only if proven necessary.

The Phase 2 representation is a flat node list. Stable capture-local indexes
and `parentIndex` preserve relationships without retaining Android objects.
Metadata records capture time, package identity, installed YouTube version,
Android release, and SDK level. It contains no device or account identifier.

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

Rule set 1 was observed on YouTube version name `21.35.442` and version code
`1561295275`, but those values are diagnostic evidence rather than an
applicability gate. The rule is evaluated for every installed official YouTube
version. It requires a typed reel list and its direct typed player-page child.
The typed loading-spinner sibling is recorded as supporting evidence but is not
required because device testing showed that transient state could cause missed
detections. Recognized watch-layout resources veto confirmation. Exactly one
complete core reel structure is required; duplicates are ambiguous and
non-actionable.

`NotShorts` means an ordinary-playback structure was positively recognized. A
tree with neither ordinary nor reel evidence is `UnknownLayout`, not
`NotShorts`. `PossibleShorts` records partial or ambiguous reel evidence for
later reconsideration but cannot authorize an action.

### Ejection state machine

The pure `EjectionStateMachine` controls timing and prevents loops. A 150 ms
one-shot delay coalesces event bursts before reading one fresh tree. A confirmed
screen permits one click on YouTube's Home tab followed by one 400 ms
verification read. A 1.5 second cooldown follows positively recognized ordinary
playback after an attempted exit.

There is no lifetime or encounter action-count ceiling: ShortStop remains
available for later confirmed Shorts until the user pauses it or disables the
service. If an eligible event arrives during the 400 ms verification window and
the fresh verification still confirms Shorts, the state machine permits one
immediate bounded Home-tab retry. This closes the event-loss race caused by
rapid re-entry. After that retry, or when no event was observed, it schedules no
continuous work and waits for a later eligible YouTube event. Every later click
still requires a fresh confirmed tree. This preserves indefinite operation
without a timer-driven action loop.

### YouTube Home action

`YoutubeHomeActionExecutor` is separate from both the detector and the state
machine. Immediately before a click, it obtains a fresh root, verifies that the
active package is YouTube, sanitizes and confirms that tree again, and asks the
pure `YoutubeHomeTargetResolver` for an unambiguous target.

The first action rule is based on both positive Phase 2 fixtures. It requires
one `pivot_bar` horizontal scroll container with one direct five-item linear tab
row. The first item must be an enabled, visible, clickable Android button. The
framework executor repeats these structural checks and performs `ACTION_CLICK`
on that first item. It never reads navigation labels, text, or content
descriptions. If any target condition changes, the action fails open.

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
- Partial or truncated tree: return `UnknownLayout`; a structurally complete
  but partial reel signature returns non-actionable `PossibleShorts`.
- Unknown or newly patched YouTube version: evaluate the bundled structural
  rule normally. Version metadata alone never changes a classification.
- Changed YouTube structure: return `PossibleShorts` or `UnknownLayout` unless
  the complete core signature still matches.
- YouTube Home-tab click rejected or inconclusive: retry only after a later eligible event
  and fresh confirmation; do not poll continuously.
- Rapid repeated events: coalesce and enforce cooldown.
- Permission removed: clear state and show disabled status next time the app
  opens.
- Process restarted: restore pause preference; do not restore an in-progress
  ejection encounter.

## iOS limitation

iOS does not expose another application's accessibility hierarchy or permit a
third-party app to issue an equivalent cross-application navigation action. The core
behavior is therefore Android-specific. Any future iOS product would need a
different, more limited definition, such as Safari URL blocking or whole-app
Screen Time controls.
