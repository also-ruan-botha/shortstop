# Phase 3 detector

## Scope

Phase 3 implements classification only. `ShortsDetector` is pure Kotlin and
depends exclusively on `SanitizedNodeTree`; it does not receive
`AccessibilityNodeInfo`, issue accessibility actions, retain nodes, poll, or
change service behavior. Wiring decisions into the event/action state machine
belongs to Phase 4.

Rule set 1 supports only YouTube `21.35.442` (`1561295275`) and sanitized-tree
schema 1. Both version name and version code must match. This intentionally
avoids assuming that YouTube resource identifiers remain stable across app
updates.

## Results

- `ConfirmedShorts`: exactly one complete compound reel signature exists and
  no ordinary-playback veto exists.
- `PossibleShorts`: some correctly typed reel evidence exists, but its required
  structure is incomplete or more than one complete candidate exists.
- `NotShorts`: a versioned ordinary-playback structure is positively
  recognized.
- `UnknownLayout`: the tree is unrecognized, unsupported, malformed, truncated,
  or otherwise unsafe to classify.

Only `ConfirmedShorts` may authorize a future action. `PossibleShorts` and
`UnknownLayout` fail open and wait for a later eligible event. `NotShorts` is
also non-actionable and may later help Phase 4 reset an encounter.

## Rule set 1

Confirmation requires all of the following:

1. `reel_recycler` is an
   `android.support.v7.widget.RecyclerView` with role `LIST`.
2. `reel_player_page_container` is an `android.widget.FrameLayout` and a direct
   child of that reel list.
3. `reel_playback_loading_spinner` is an `android.widget.ProgressBar` and a
   sibling of that reel list.

Any of these observed ordinary-playback resources vetoes confirmation:

- `next_gen_watch_layout_no_player_fragment_container`
- `watch_panel`
- `watch_player`

The veto is deliberately evaluated before positive evidence. Resource-ID
suffixes are never sufficient alone: the expected class, role where relevant,
and hierarchy must also agree. No visible or localized text fallback is used;
the sanitized model does not collect text, and Phase 2 produced enough
non-content evidence without it.

## Fixture results

| Expected result | Captures |
| --- | --- |
| `ConfirmedShorts` | `youtube-shortscreen.json`, `youtube-other-shortscreen.json` |
| `NotShorts` | regular playback, full-screen playback, ordinary-video loading, short ordinary video, short ordinary video in picture-in-picture |
| `UnknownLayout` | main, subscriptions, channel, account, search autoplay, Shorts loading transition |
| `PossibleShorts` | no full capture; covered by synthetic partial and ambiguous cases |

The ordinary navigation fixtures remain `UnknownLayout` because they provide
no explicit ordinary-playback signature. Treating absence of reel evidence as
proof of `NotShorts` would collapse an important confidence distinction. Every
one of these outcomes is non-confirming, so no ordinary fixture can authorize
an exit.

## Validation and failure behavior

Before applying a rule, the detector checks package identity, schema version,
capture completeness, node-count/depth limits, unique non-negative indexes, one
root, resolvable parents, parent-relative depth, and plausible child counts.
Unsupported or ambiguous rule selection also fails open.

The local tests load the capture files from `captures/` as test resources, so a
fixture change is exercised without maintaining a second copy. Synthetic tests
then alter the structures to cover missing markers, wrong relationships,
conflicting ordinary evidence, duplicate candidates, malformed trees, bounds,
and version selection.
