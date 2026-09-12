# Phase 4 action state machine

## Status

Implementation updated on 12 September 2026. The automated quality gates are
documented with the implementation handoff; the physical-device exit gate must
still be verified by the project owner before Phase 4 is considered complete.

## Event and action flow

The accessibility-service metadata and callback continue to accept only
window-state and window-content events from `com.google.android.youtube`.
Eligible events are coalesced with a one-shot 150 ms delay. The service then
obtains the current root, verifies its package, converts it immediately through
`AccessibilityTreeReader`, and passes only the sanitized tree to
`ShortsDetector`. The detector evaluates its structural rule regardless of the
installed YouTube version name or code.

Before acting, `YoutubeHomeActionExecutor` obtains a fresh YouTube root and
requires the Shorts detector to confirm that fresh tree again. The separate
pure `YoutubeHomeTargetResolver` then requires the structure observed in both
positive fixtures: one `pivot_bar`, one direct five-item tab row, and an
enabled, visible, clickable button as the row's first item. The executor finds
that exact `pivot_bar` in the framework tree, repeats the structural checks,
and invokes `ACTION_CLICK` on its first tab. It does not inspect the tab's text
or content description. A missing, duplicated, disabled, or changed target
fails open without any navigation action.

The state machine uses these states:

1. `Monitoring`: wait for an eligible YouTube event.
2. `Suspected`: coalesce the event burst and schedule one fresh classification.
3. `Ejecting`: click YouTube's own bottom-navigation Home tab after
   `ConfirmedShorts` only.
4. `Verifying`: schedule one fresh classification 400 ms after the click; later
   events may request another classification only when none is pending.
5. `Cooldown`: suppress events for 1.5 seconds after an attempted exit reaches
   positively recognized `NotShorts` playback.

The timers are bounded, one-shot coroutine delays. There is no continuous
polling. Every automatic navigation action is an `ACTION_CLICK` on the
structurally verified YouTube Home tab; the state machine has no Android Home
or Back directive and no action-count ceiling.

## Encounter boundary and fail-open behavior

An encounter resets when verification positively recognizes `NotShorts`, when
the fresh active root no longer belongs to YouTube, or when the service pauses
or stops.

`PossibleShorts`, `UnknownLayout`, a missing root, and a failed read cause no
action. An eligible YouTube event that arrives while the one verification read
is pending is remembered. If that fresh verification still confirms Shorts,
the state machine permits one immediate bounded Home-tab retry. This prevents a
rapid user re-entry from being discarded. It cannot chain indefinitely: after
the one retry, the state machine schedules nothing further and waits for a new
eligible event. A rejected click follows the same event-driven retry path.

The main ShortStop screen mirrors `UnsupportedLayout` runtime status without
persisting a viewing history or detector details.

## Automated coverage

Pure state-machine tests cover rapid event coalescing, accepted and rejected
YouTube Home-tab handling, verification scheduling, repeated confirmed
encounters without an action ceiling, unknown verification, ordinary-layout
cooldown, the single bounded rapid-entry retry, reset, and stale/irrelevant
transitions. Resolver tests prove both
positive fixtures identify the first pivot button and that a missing or unsafe
target fails open. Existing detector tests continue to prove that only the two
positive fixtures can reach confirmation. A Compose device test checks the
persistent compatibility warning.

## Physical-device exit gate

Verify first on the evidence version YouTube `21.35.442`, then repeat on the
currently installed YouTube version:

- Shorts tab, Home, search, deep link, resume-to-Shorts, and rapid navigation;
- no Home-tab click on all ordinary fixtures and the broader false-positive
  matrix;
- pause suppresses classification and actions;
- successful YouTube Home-tab click and verification paths from every Shorts
  entry point;
- repeated Shorts encounters continue to trigger the Home tab without an
  action-count ceiling, while inconclusive states do not spin without new
  events; and
- service disable/destruction removes pending timers.

Do not proceed to hardening or call Phase 4 complete until this device gate has
been recorded.
