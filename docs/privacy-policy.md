---
layout: default
title: Privacy Policy
---

# Privacy Policy

_Last updated: 2026-10-12_

Urgent is an Android app that rings your phone when the same number calls you twice within a few
minutes, even while the phone is silenced. This policy describes what the app reads to do that, and
what it does with it. It is short because the app does very little.

## The app collects nothing

Urgent has no account, no sign-in, and no server. Nothing is transmitted anywhere. There is no
analytics, no crash reporting, no advertising, and no third-party SDK that phones home. The app does
not request internet access at all.

## What the app can access, and why

- **Phone state.** When a call comes in, Android tells the app that the phone is ringing and which
  number is calling. The app uses this only to decide whether to ring.
- **Your call log.** At that moment, the app reads the calls from the last few minutes to see whether
  the same number called before. It does not copy, store or send the call log, and it reads nothing
  older than the window you chose.
- **Notifications**, only to show the *Silence* button while it is ringing.

The app stores two settings — whether it is switched on, and how many minutes count as "again" — and
your theme and language choices.

## The things that happen outside the app

1. **Android's automatic backup.** If it is enabled on your phone, Android backs up the app's
   settings to your own Google Drive. You can turn this off in your phone's settings.
2. **Google Play**, if you installed the app from there. Installing and updating an app is a
   transaction between your phone and Google, with its own privacy policy.

## Children

The app is not directed at children and collects nothing from anyone.

## Deleting your data

Uninstalling the app removes the settings it stored. There is nowhere else to delete anything from,
because nothing went anywhere else.

## Changes

If this policy changes, the date at the top changes with it, and the history of this file is public
in the repository.

## Contact

<https://github.com/srednimax/urgent/issues>
