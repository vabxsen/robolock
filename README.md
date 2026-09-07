# Robolock

**Block the rabbit holes. Keep the apps.**

An Android digital-wellbeing app that helps you spend less time in the apps that take it, without
giving up the parts you actually want. Same apps. Less distractions.

Local-first: no account, no server, no analytics. Everything Robolock records stays on the device.

---

## The defining constraint: no AccessibilityService

Robolock **does not use Android's AccessibilityService**, anywhere. No service declaration, no
accessibility permission, no `AccessibilityNodeInfo`, no accessibility events, no
`performGlobalAction`, no UIAutomator substitute. It also does no TLS interception, uses no custom
certificates, no VPN packet inspection, no private APIs, and never records the screen.

That is a deliberate trade, and it has a direct consequence worth stating plainly:

> Android will tell Robolock **which app** is in the foreground.
> It will not tell Robolock **which screen inside that app** you are looking at.

So Robolock knows *"Instagram is open."* It does not know *"you are watching Reels."* An app that
could tell the difference would have to read the contents of your screen — including your messages
and anything else on it. Robolock does not do that, and it does not pretend to.

### What follows from that

Robolock is built to be excellent at what Android legitimately offers:

- session limits (longest single sitting)
- daily budgets
- schedules, including windows that cross midnight
- Focus Mode, in three tiers
- launch friction
- timed bypasses with a grace period
- local statistics derived only from what it actually observed

### The honesty guarantee

This is the most important correctness property in the codebase, so it is enforced by the type
system rather than by discipline. In `:core-rules`:

```kotlin
sealed interface DistractingSurface { … }
sealed interface AppLevelSurface   : DistractingSurface   // UsageStats reports it
sealed interface DeepLinkSurface   : DistractingSurface   // a URI was routed to us
sealed interface UnsupportedSurface: DistractingSurface   // not observable, and we say so
```

Evidence is the only route into the database. `AppForegroundEvidence` accepts only an
`AppLevelSurface`; `DeepLinkEvidence` accepts only a `DeepLinkSurface`; and
`ObservedIntervention` — the only thing the statistics layer will store — has an `internal`
constructor reachable solely from `DetectionEvidence.toIntervention()`.

**There is therefore no constructor anywhere that accepts an `UnsupportedSurface`.** Writing a
fabricated "Reels blocked" statistic is a compile error, not a promise. Statistics for such a
surface return `SurfaceStats.NoData`, which is a distinct type from a zero count, because "we
never saw this" and "this happened zero times" are different claims.

The single exception where surface-level knowledge *is* honest: when you tap or share a link that
clearly points at a Short or a Reel, Android hands that URL to Robolock, so it genuinely knows
where you were heading.

---

## Known limitations

Stated here, in the code, and once in the app's own "What Robolock can detect" screen.

1. **Detection latency is roughly 2–5 seconds.** You may see a moment of the app before a limit
   appears. That delay is what keeps battery use low; the copy never claims "instantly."
2. **No in-app surface detection.** Reels/Shorts/Status *after* launch is app-level only.
3. **WhatsApp Status cannot be detected at all.** It is modelled as `UnsupportedSurface`: you can
   still say you want less of it, which shapes reminders, but it can never produce a statistic.
4. **Force-stop and aggressive OEM battery managers** (Xiaomi, Samsung, OnePlus) can kill the
   monitor. Gaps are detected via a heartbeat and surfaced rather than hidden.
5. **Foreground-service restart after a background kill throws on API 31+**, so Robolock degrades
   to a notification asking you to reopen it.
6. **Permission revocation can be noticed, never prevented.** If Usage Access is switched off,
   Robolock says so — in the service notification, in a system notification, and as a banner on
   the dashboard. It never shows itself as protecting something it cannot see.

---

## Architecture

Two Gradle modules. `:core-rules` is a **plain JVM library, not an Android one**, so an accidental
`import android.*` in domain logic is a compile error rather than a code-review catch.

```
:core-rules   Pure Kotlin, no Android. Surfaces and capability types, the rule engine,
              schedule windows, session segmentation, bypass expiry maths, deep-link
              patterns, the time-reclaimed heuristic. 92 unit tests.

:app          presentation/ (Compose screens, ViewModels)
              monitoring/   (ForegroundAppMonitor, MonitorService, receivers, notifications)
              blocking/     (OverlayController, OverlayHost, intervention UI, link intercept)
              data/         (Room, DataStore, repositories)
              permissions/  (real permission state, never cached optimistically)
              ui/           (theme tokens + component library)
```

### Foreground monitoring

`queryEvents`, never `queryUsageStats` — the latter returns bucketed aggregates and cannot say
*when* a transition happened. UsageStats flushes events in batches and can surface an event older
than one already seen, so query windows deliberately **overlap by 10s** and duplicates are filtered
by identity in a bounded ring. Advancing a cursor straight to `now` silently drops transitions.

Cadence adapts rather than polling at a fixed rate:

| State | Cadence |
|---|---|
| Screen off | suspended entirely |
| Screen on, nothing watched in front | 3 s |
| A watched app in front | 1 s |
| Protection off | service stops itself |

The host is a foreground service typed **`specialUse`**, not `dataSync` — on API 35 `dataSync` is
capped at six cumulative hours a day, which would silently stop enforcing limits in the evening.

### The intervention overlay

A `TYPE_APPLICATION_OVERLAY` window, not an Activity. From Android 10 a background app cannot
reliably start an Activity, and an app in this position is by definition in the background; an
overlay is not an activity launch, so the restriction does not apply. If the overlay cannot be
drawn, Robolock falls back to a high-priority notification whose tap is itself a valid launch.

The overlay is always dismissible, never imitates a system dialog, takes no text input, and is
suppressed outright over Settings and the package installer so it can never cover a permission
dialog.

### Bypass expiry

A "continue anyway" grant stores both a wall-clock deadline and a monotonic
(`elapsedRealtime`) one, plus the boot session that produced it. While on the same boot the
**earlier of the two wins**, so winding the system clock backwards cannot extend a grant. After a
reboot the monotonic reading is meaningless and the wall clock stands alone.

---

## Building

Requires JDK 17+ and the Android SDK (compileSdk 36).

```bash
./gradlew :core-rules:test :app:testDebugUnitTest   # unit tests
./gradlew :app:assembleDebug :app:lintDebug          # build + lint
./gradlew :app:assembleRelease                       # needs keystore.properties
```

### Testing enforcement on an emulator

Emulator images ship without Instagram, YouTube or WhatsApp, so the debug build's
`SupportedAppRegistry` additionally watches stand-ins that do exist (Chrome, Clock). This runs the
real pipeline — real `UsageEvents`, real overlay, real limits and bypass — rather than a stub, and
none of it reaches a release build.

```bash
adb shell appops set com.robolock.app.debug GET_USAGE_STATS allow
adb shell appops set com.robolock.app.debug SYSTEM_ALERT_WINDOW allow

# Seed an 8-second limit on Chrome and start the monitor
adb shell am broadcast -a com.robolock.app.debug.SEED \
  -e pkg com.android.chrome --ei sessionSeconds 8 \
  -n com.robolock.app.debug/com.robolock.app.debug.DebugControlReceiver

adb logcat -s Robolock:V
```

`…debug.DUMP`, `…debug.STOP` and `…debug.RESET` are also available. The receiver exists only in
the debug source set.

---

## Privacy

Robolock does not read other apps' view hierarchies, messages, passwords or screen contents; does
not screenshot or record the display; does not log keystrokes; does not decrypt HTTPS; and uploads
nothing. It stores only what it needs to do its job — your settings, your sessions, and its own
interventions — and Settings › Data & Privacy clears all of it.

Package visibility is scoped: Robolock declares the specific packages it supports in `<queries>`
rather than requesting `QUERY_ALL_PACKAGES`.

Link interception is **off by default**. The catcher ships as an `activity-alias` that is disabled
until you opt in, with `autoVerify="false"` set explicitly — Robolock must never quietly become the
default handler for your Instagram or YouTube links. The patterns match `/shorts/` and `/reel/`
paths only, never a bare host, and never `/p/` (an ordinary Instagram post).
