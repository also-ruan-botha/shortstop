# Policy, privacy, and release requirements

This is engineering risk guidance, not legal advice. Policies can change and
must be reviewed again immediately before a public release.

## Google Play Accessibility API requirements

ShortStop is not a disability accessibility tool, so it must not claim that
classification merely to simplify review. A public release should:

- declare Accessibility API usage accurately in Play Console;
- provide a prominent in-app disclosure before opening system settings;
- obtain affirmative acknowledgement of the disclosure;
- explain that the service reads the structural interface of YouTube and
  selects YouTube's own Home tab when it recognizes the Shorts screen and remains active
  for future encounters until paused or disabled;
- state that processing is local and screen content is not collected or
  transmitted;
- demonstrate the deterministic, narrow, user-understood behavior in the
  review materials;
- provide straightforward instructions for pausing and disabling the service;
- maintain an accurate Data safety form and privacy policy.

Google Play approval is not guaranteed. The primary references are:

- [Google Play Accessibility API policy](https://support.google.com/googleplay/android-developer/answer/10964491)
- [Android AccessibilityService reference](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [Build an accessibility service](https://developer.android.com/guide/topics/ui/accessibility/service)

## Draft prominent disclosure

The final copy requires policy/legal review, but the implementation should be
designed around language equivalent to:

> ShortStop uses Android Accessibility access to recognize when the official
> YouTube app is displaying Shorts and to select YouTube's own Home tab automatically. It examines
> YouTube's on-screen interface structure only while YouTube is active. It does
> not capture screenshots, record what you watch or type, or send accessibility
> data off your device. You can pause ShortStop here or disable its access at
> any time in Android Settings.

The implemented disclosure additionally explains that monitoring continues
until the user pauses ShortStop or disables its accessibility access. The tab is
performed only after the detector confirms the full structural Shorts
signature; partial and unknown layouts remain non-actionable.

The status screen also carries a persistent compatibility warning. It explains
that YouTube interface changes may stop blocking or interfere with normal use,
and tells the user to report the bug and disable ShortStop in Accessibility
settings if that happens. This warning reflects the deliberate decision to
attempt the structural rule across unobserved YouTube versions rather than
disabling functionality solely because version metadata changed.

Place this disclosure immediately before the affirmative control that opens
Accessibility Settings. Do not hide it only in a privacy policy.

The Phase 1 onboarding implements this placement with an unchecked, explicit
acknowledgement. The system-settings button remains disabled until the user
selects it. The acknowledgement is then stored locally so returning users can
open settings from the status screen without repeating onboarding.

Application backup and device-transfer extraction are disabled, so the local
acknowledgement and pause preference are not copied off the device by Android's
backup mechanisms.

## Data inventory

Persisted data is limited to:

- onboarding acknowledgement;
- pause preference (Android owns the enabled/disabled service state);
- non-sensitive settings;
- optional local counters only if later approved and documented.

The prototype must not persist or transmit:

- accessibility-node text or content descriptions;
- video titles, channel names, search terms, comments, or URLs;
- account names, identifiers, cookies, or credentials;
- screenshots, audio, or video;
- a history of Shorts encounters.

Do not add the Internet permission to the release manifest. Debug fixture export
must be manual, sanitized, and absent from release builds.

The Phase 2 inspector is compiled only into debug builds and captures exactly
one bounded tree after a developer arms it and returns to YouTube. Export is a
separate developer action and writes only to app-private local storage. Fixtures
contain structural identifiers, framework class/role, boolean capabilities,
tree relationships, capture time, YouTube version, and Android version. The
reader never accesses node text or content descriptions, and it records no
bounds, screenshots, device identifiers, or account data.

## YouTube terms and branding

Review the [YouTube Terms of Service](https://www.youtube.com/t/terms) before
distribution. ShortStop must not scrape or reproduce content, call private
YouTube services, bypass security, interfere with advertising or measurement,
or misrepresent itself as affiliated with YouTube.

Use factual compatibility wording such as “blocks the Shorts screen in the
YouTube app.” Do not use YouTube logos or trade dress in the icon, screenshots,
or store listing. Obtain legal review before commercial distribution because
the Terms' restrictions concerning interference are broad.

## Release checklist

- Re-read current Google Play policies and YouTube Terms.
- Confirm the accessibility declaration matches actual behavior.
- Confirm disclosure and consent appear before the system settings link.
- Confirm the manifest has no unnecessary permissions.
- Inspect the release bundle for analytics or networking dependencies.
- Confirm release logging contains no screen-derived values.
- Publish a privacy policy matching the final binary.
- Prepare a reviewer video showing onboarding, enablement, blocking, pausing,
  and disabling.
- Document tested Android and YouTube versions and known limitations.
- Complete legal review of name, branding, terms, and disclosure language.
