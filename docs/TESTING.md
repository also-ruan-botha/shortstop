# Testing strategy

False exits from normal YouTube are the highest-severity functional defect.
Tests must cover ordinary viewing more broadly than the happy-path Shorts case.

## Test layers

### Local unit tests

- Tree sanitization and traversal limits
- Exact, partial, conflicting, and absent detector signals
- Signature schema/version behavior
- Detection confidence rules
- Ejection-state transitions, retry limit, and cooldown
- Pause/resume behavior
- Malformed, excessively deep, and excessively wide trees

Use sanitized fixtures captured through the approved debug workflow. Every new
positive fixture requires nearby negative fixtures from visually similar
ordinary YouTube screens.

### Instrumented tests

- Accessibility service lifecycle and settings-state detection
- Package gating
- DataStore persistence
- Overlay display and dismissal
- Process recreation and activity lifecycle
- Material 3 screen behavior in light and dark themes
- Scalable text, semantic labels, minimum touch targets, edge-to-edge insets,
  and adaptive layouts at supported display sizes

The Phase 1 automated UI test covers the first-run disclosure and disabled
acknowledgement control. Its manual device gate additionally covers the system
Accessibility Settings handoff, enabled-state refresh on foreground return,
pause/resume persistence, disable instructions, theme variants, large text,
and rotation. System accessibility consent must remain a manual user action;
tests must not enable the service through shell commands or hidden APIs.

UI Automator may drive controlled navigation, but tests must tolerate normal
network loading and must report the installed YouTube version.

### Manual device matrix

At minimum, test:

- the current stable Android release and the oldest supported API where
  practical;
- a phone with the current stable YouTube app;
- at least one additional YouTube version retained on a separate test device;
- compact and large display/font settings;
- portrait and landscape;
- English plus one non-English device locale;
- gesture and three-button system navigation.

Use official Play-distributed YouTube builds. Do not add downloaded or modified
YouTube APKs to this repository.

## Shorts entry cases

- Tap the Shorts navigation tab.
- Open a Short from Home.
- Open a Short returned by YouTube search.
- Open a `youtube.com/shorts/...` deep link.
- Resume YouTube while it was previously displaying Shorts.
- Move rapidly between Shorts and ordinary screens.

Each case must exit within one second after a usable event, avoid repeated Back
actions, and leave the user in a predictable previous screen or manual-exit
state.

## Ordinary YouTube false-positive suite

Run at least 100 scripted actions spanning:

- Home and subscriptions feeds
- Search and search filters
- Ordinary portrait and landscape videos
- Full-screen playback and player controls
- Very short ordinary videos
- Live streams and premieres
- Channels, playlists, comments, descriptions, and settings
- Picture-in-picture entry and return
- Notifications and external ordinary-video links
- Loading, offline, unavailable, age-gated, and error screens

The release candidate requires zero automated exits in this suite.

## Reliability and resource checks

- Service enabled, disabled, paused, and resumed
- Permission revoked while the app is running
- YouTube force-stop and update
- ShortStop process death and restart
- Device reboot
- Repeated accessibility-event bursts
- Missing/partial node roots
- Thirty-minute ordinary playback session with event/action counts recorded
- Android battery and CPU inspection confirming no polling loop

## Release evidence

Record for each candidate:

- ShortStop commit and build number
- Android version and device model
- YouTube version
- Detector rule-set version
- Positive-case pass rate
- Ordinary-suite false exits
- Median and worst observed response time
- Known unsupported layouts

Any false exit, unbounded retry, collected content, or unexplained background
activity blocks release.
