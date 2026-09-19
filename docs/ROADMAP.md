# ShortStop roadmap

This roadmap records work intentionally deferred until after the minimum viable
product (MVP). Items listed here are not part of current application behavior
and must pass their own design, privacy, detector, and physical-device review
before implementation.

## MVP

The MVP blocks confidently detected YouTube Shorts by selecting YouTube's own
Home tab. Users who intentionally want to watch Shorts can temporarily use the
existing **Pause ShortStop** control and resume blocking afterward.

MVP completion remains focused on detector reliability, ordinary-playback
safety, event-driven retry behavior, accessibility, and physical-device
validation.

## Post-MVP: controlled Shorts allowance

A future release may let users configure a daily number of Shorts and request a
single intentional Short without disabling blocking for an entire session.
This work is deferred because reliable accounting across YouTube entry paths,
reel swipes, process restarts, and accessibility-event variations has not yet
been demonstrated.

Before implementation, the feature requires:

1. Captured and sanitized evidence of how supported YouTube versions signal
   every transition between Shorts.
2. A counting design that does not inspect or retain video content, titles,
   account data, or viewing history.
3. Tests proving that opening YouTube, stale accessibility roots, repeated
   content-change events, and ordinary scrolling do not consume an allowance.
4. Physical-device tests proving that each actual Short is counted exactly
   once, including rapid swipes and app re-entry.
5. Clear recovery behavior when the counter cannot determine a transition with
   confidence. Unknown layouts must continue to fail open.
6. Updated disclosure, privacy, architecture, testing, and user documentation.

Possible controls include a configurable daily allowance and a non-stacking
**View one short** action. Their exact behavior remains undecided until the
counting model is validated.
