# Phase 2 capture analysis

## Dataset

Analysis date: 8 September 2026

All 13 captures use schema version 1, were recorded on Android 16 / API 36,
target YouTube `21.35.442` (`1561295275`), and report `truncated: false`.

Positive captures:

- `captures/youtube-shortscreen.json`
- `captures/youtube-other-shortscreen.json`

Negative captures:

- `captures/youtube-main-screen.json`
- `captures/youtube-subscription-screen.json`
- `captures/youtube-channel-screen.json`
- `captures/youtube-account-screen.json`
- `captures/youtube-regular-playback.json`
- `captures/youtube-fullscreen-playback.json`
- `captures/youtube-ordinary-video-loading.json`
- `captures/youtube-search-autoplay.json`
- `captures/youtube-short-ordinary-video.json`
- `captures/youtube-short-ordinary-video-picture-in-picture.json`

Transitional capture expected to fail open:

- `captures/youtube-short-loading.json`

## Candidate structural signature

The following non-textual structure occurs in both positive captures and none
of the ten negative captures or the transitional loading capture:

1. `reel_recycler` is an `android.support.v7.widget.RecyclerView` with the
   normalized `LIST` role.
2. `reel_player_page_container` is a direct child of `reel_recycler`.
3. `reel_playback_loading_spinner` is a sibling of `reel_recycler` under the
   same `browse_fragment_layout_coordinator_layout` node.

| Capture | Reel list | Direct player-page child | Loading-spinner sibling |
| --- | --- | --- | --- |
| Shorts screen | Yes | Yes | Yes |
| Other Shorts screen | Yes | Yes | Yes |
| Main screen | No | No | No |
| Subscriptions screen | No | No | No |
| Channel screen | No | No | No |
| Account screen | No | No | No |
| Regular playback | No | No | No |
| Full-screen playback | No | No | No |
| Ordinary video loading | No | No | No |
| Search autoplay | No | No | No |
| Short ordinary video | No | No | No |
| Short ordinary video in picture-in-picture | No | No | No |
| Shorts loading transition | No | No | No |

The narrowed high-confidence candidate consists of two required signal groups:

1. **Reel paging structure:** `reel_recycler` has the RecyclerView/List type and
   directly owns `reel_player_page_container`, which is a FrameLayout.
2. **Reel loading context:** `reel_playback_loading_spinner` is a ProgressBar
   sibling of that reel list under the same coordinator.

Phase 2 initially proposed requiring both groups, meaning all three named nodes
and both structural relationships had to agree. No individual resource
identifier was sufficient on its own.

The regular, full-screen, and loading ordinary-playback captures contain
`next_gen_watch_layout_no_player_fragment_container`. Regular playback contains
`watch_panel` with a `watch_list` RecyclerView, while full-screen playback
contains `watch_player`. These are strong ordinary-playback markers and can be
used as an explicit veto before considering a Shorts match.

The short ordinary-video and picture-in-picture captures also use
`watch_panel` with `watch_list` and contain no reel-player structure. This is
important negative evidence against treating video duration, portrait-like
presentation, or picture-in-picture transitions as Shorts.

`youtube-short-loading.json` represents the navigation/loading state before the
Shorts viewer structure exists. It deliberately does not match. The detector
must return an unknown or possible state without acting, then reconsider a
later usable accessibility event after the viewer has been constructed.

## Rejected or provisional differences

- `reel_time_bar` is not a Shorts marker; it occurs in all 13 captures,
  including regular playback, full-screen playback, picture-in-picture, and
  both loading states.
- Total node count is layout- and content-dependent and must not be a rule.
- The Shorts captures make `browse_fragment_layout_coordinator_layout`
  non-scrollable while the ordinary-navigation captures make it scrollable.
  Ordinary playback uses a different root, so this remains unnecessary and
  unsafe as a positive rule.
- `nerd_stats_container`, generic button counts, toolbar shape, and selected
  bottom-navigation state are too likely to vary or occur in ordinary playback.

## Phase 2 conclusion

The expanded dataset satisfies the Phase 2 requirement to distinguish two
independent non-textual signal groups from normal playback for tested YouTube
version `21.35.442`. It covers ordinary navigation, regular and full-screen
playback, a short ordinary video, picture-in-picture, search autoplay, and
loading states. The Phase 2 evidence gate is therefore complete for this
version.

Phase 3 rules must remain versioned, require a compound structural signature,
and classify the captured Shorts-loading state without action. Continue adding
negative fixtures whenever new look-alike surfaces are found. A second
available YouTube version must be evaluated independently; these resource
identifiers are not assumed to be stable across versions.

Product decision recorded 12 September 2026: the rule remains internally
versioned and its observed YouTube version remains recorded, but the installed
YouTube version is no longer an applicability gate. Phase 3 now attempts the
same compound structural rule on every YouTube version. This prioritizes
continued functionality across minor patches while retaining all structural
confirmation requirements and ordinary-playback vetoes.

Product decision recorded 12 September 2026 after rapid-entry device testing:
the stable typed reel-list/direct-player relationship is the required core
signature. The loading-spinner sibling is transient and is now supporting,
rather than mandatory, evidence. All captured ordinary surfaces still lack the
core relationship, and the ordinary-playback vetoes remain unchanged.
