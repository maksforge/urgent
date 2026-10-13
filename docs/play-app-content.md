# Play Console — App content answers

The answers this app gave in the Console's **App content** pages, what each one rests on, and **the
fact that would change it**. The Console shows what was answered but never why, and Play asks again:
when the app changes, when the questionnaire changes, and once a year. A re-declaration made from
memory a year from now is how a true answer turns false.

<!--
    Every section below is a page the Console will make you answer before a closed release, and the
    answers are pre-filled for Urgent: no network, no account, no ads, two settings on the device.
    Date every answer the day you give it in the Console — the reasons are in front of you then, and
    gone a week later.
-->

Two rules keep this file honest:

- **The privacy policy and the Data safety form describe the same facts to two audiences**, and Play
  cross-checks them. [`privacy-policy.md`](privacy-policy.md) and this file move in the same commit.
  A change to one without the other is the drift this file exists to stop.
- **A claim about permissions is read off the built artifact, never off the manifest.** A dependency
  can merge a permission the source never declared, and that is invisible from `AndroidManifest.xml`.
  `scripts/aab-permissions.py` is the reader, it runs before every upload, and its `EXPECTED` and
  `FORBIDDEN` tables are this section's twin — edit them together.

## What the artifact carries

Read at **versionCode _, versionName _** by `publish-play.yml`'s verify step on _<date>_:

> _paste the summary line `aab-permissions.py` prints_

| Permission | Whose | What it is for | Console consequence |
| --- | --- | --- | --- |
| `READ_PHONE_STATE` | ours | the ringing broadcast that tells the app a call has arrived | none: runtime, asked when the feature is switched on |
| `READ_CALL_LOG` | ours | the caller's number, and whether they called in the last few minutes | **Restricted**: Play's SMS and Call Log policy allows it only for default dialers and listed exceptions. The Permissions Declaration Form decides whether this app can be on Play at all |
| `POST_NOTIFICATIONS` | ours | the ringing notification's Silence button | none |
| `VIBRATE` | ours | vibrating with the ring | none: normal, install-time |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | ours | `RingThroughService` plays the ringtone for up to a minute | **Foreground service declaration**: the type and a short video of the ring are asked for in App content |
| `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX | its own non-exported receivers, signature-level | none: not user-visible |

**Forbidden, and asserted absent**: `INTERNET`, `AD_ID`, `QUERY_ALL_PACKAGES`,
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `ACCESS_NOTIFICATION_POLICY`, `USE_EXACT_ALARM`. The first
two are what the Data safety and advertising-ID answers rest on; `ACCESS_NOTIFICATION_POLICY` is
the house rule that Urgent never changes Do Not Disturb, held on the artifact.

**Two asks are not permissions and appear on no Play list**: the battery-optimisation exemption and
Xiaomi's autostart. Both are hand-offs to Settings. They need no declaration, and the privacy policy
names them anyway, because a user sent to two Settings screens should find both explained.

---

## Ads

**No.** Answered _<date>_.

Would change: any ad SDK, ever. `AD_ID` absent from the artifact is the check that would catch one
arriving through a dependency.

## App access

**All functionality is available without special access.** Answered _<date>_.

There is no account and no sign-in. Would change: a feature behind a login.

## Content rating

**The IARC questionnaire.** Answered _<date>_, category _<category>_, rating contact _<email>_.

Would change: user-generated content, communication between users, or a link out to anything that
has either. A support `mailto:` hand-off is not user-to-user communication — it opens the user's own
mail app, addressed to the developer — and a rate row opens Play's own listing, whose reviews are
Play's surface rather than the app's.

## Target audience and content

**18 and over** _(or your answer)_. Answered _<date>_.

Would change: lowering it. Any age band under 18 brings in the Families policy and its design and
SDK requirements.

## Government, financial and health features

**No to all three** _(or your answer)_. Answered _<date>_.

Health is the one to guard, for an app anywhere near wellbeing. **Keep health claims out of the
listing, the site and the tags** — Play's enforcement has treated a linked page as part of the
listing. Would change: copy or a tag that sells the app as good for you.

## Data safety

**No data collected. No data shared.** Answered _<date>_ against versionCode _.

Play's definitions, from its Data safety help page (support.google.com, answer 10787469) — re-read
them at each re-declaration rather than trusting this quote:

> "'Collect' means transmitting data from your app off a user's device."
>
> "User data accessed by your app that is only processed locally on the user's device and not sent
> off device does **not** need to be disclosed."

The answer rests on one fact per route data could take:

1. **The app cannot transmit anything.** `INTERNET` is absent from the artifact, and
   `aab-permissions.py` fails the upload if it ever appears. This one fact is most of the answer.
2. **The call log is read only on the device.** A few minutes of it, at the moment a call rings, to
   compare one number. Nothing from it is stored or sent. Still disclose it if the form asks about
   *access* rather than collection: the Permissions Declaration Form does.
3. **Auto Backup is still "no data collected".** The platform moves the user's backup to the user's
   own Google Drive; the developer never sees it. That is Android transmitting the user's backup, not
   the app transmitting to the developer. ⚠ Every file the backup carries lands on the user's *next*
   phone too — and it is only two settings and a theme here (ADR-0005's amendment).
4. **A support mail is still "no data collected", and the reason is the definition rather than an
   exemption.** A `mailto:` hands a draft to **another app**; the user reads it, can edit every line,
   and sends it from their own mail client, or does not. ⚠ **Do not write "Play exempts support
   flows" in a re-declaration.** The help page has no such exemption; the claim is true because of
   what "collect" means, and that is the reason to give.

Would change any of it:

- **`INTERNET` on the artifact**, from our code or a dependency. Every answer here becomes a question.
- **Anything the app sends by itself**, such as crash reporting or analytics. There is none today.
- **Storing call-log rows or numbers** in the app's own storage, where a backup would carry them.

## Advertising ID

**No.** The artifact carries no `com.google.android.gms.permission.AD_ID`, and `aab-permissions.py`
forbids it. Answered _<date>_.

## Foreground service permissions

**`mediaPlayback`**, for `call/RingThroughService.kt`. Answered _<date>_.

- **What the user sees:** the ringtone playing on the alarm stream while a repeated call rings, and
  a notification with a *Silence* button. It stops when the call is answered, declined or missed, or
  after a minute at most.
- **Why it cannot wait:** it exists only while the phone is ringing. Deferred, it rings after the
  call is over, which is no use to anyone.
- **Why this type:** the service's whole job is playing a sound the user hears. `phoneCall` is for
  the app that owns the call — a default dialer or one holding `MANAGE_OWN_CALLS` — and Urgent is
  neither. `specialUse` would claim no named type fits, and one does.
- **The video:** two calls from another phone to this one on silent, the second ringing and the
  notification visible. Link: _<url>_.

Would change: a second foreground service, a change of type, or the ringing outliving the call.

## Store settings (not App content, recorded here because it is asked for the same reason)

- **Category:** _<category>_, and why not the obvious neighbour.
- **Tags:** _<tags>_.
- **Contact email:** _<address>_, public on the listing.
- **Privacy policy:** `https://<owner>.github.io/<repo>/privacy-policy.html`, served from `docs/` by
  Pages. It updates on a merge to `main`, with no release, which is why it has to be right before the
  app it describes ships rather than after.

## Production access — the application, and the answers it was given

**Submitted _<date>_.** Asked once, from the Console's Dashboard, after the closed test's 14 days.
Record the answers **verbatim**, not summarised: the Console asks the same set again for the next app
on this account, and an answer re-read a year later has to be the one that was given.

**About your closed test**

- *How did you recruit users for your closed test?* — _<answer>_
- *How easy was it to recruit testers?* — _<answer>_
- *Describe the engagement you received from testers* — _<answer>_
- *Provide a summary of the feedback that you received* — _<answer>_

**About your app**

- *Who is the intended audience?* — _<answer>_
- *Describe how your app provides value to users* — _<answer>_
- *How many installs do you expect in your first year?* — _<answer>_

**Your production readiness**

- *What changes did you make based on what you learned during your closed test?* — _<answer>_
- *How did you decide that your app is ready for production?* — _<answer>_

**Three things the first app built from this template kept deliberate, so a later answer does not undo
them:**

1. **The audience answer agrees with *Target audience* above.** A reviewer reads the two side by side.
   A mismatch is a question rather than a rejection, but it can cost days.
2. **Engagement is answered from what the app can know.** With no `INTERNET` permission there is no
   telemetry, so *"I do not know how many used X"* is the true answer. A guessed figure would
   contradict *Data safety* in this same file.
3. **The changes answer says where the changes came from.** If the testers reported nothing, say so and
   name what came from your own use instead. Presenting your own work as tester-driven is an easy
   dishonest sentence, and the application does not need one.

## A tip (Payments policy — not a question Play asks, but one a reviewer answers)

**Default: no tip on any surface the release controls.** Not in the app, and not on the pages the
listing links to. Read this before adding one, because the policy text and its enforcement disagree.

- **The text allows it.** Payments policy §3.2 treats a tip as a peer-to-peer payment, outside Play
  Billing, when **100% reaches the creator** and **nothing unlocks** — no badge, no theme, no
  thanks-screen. §4's anti-steering rule excepts §3. So: one personal payment link, worded as a *tip to
  a person*, never a *donation to a project*.
- **Enforcement has rejected apps anyway.** StreetComplete (February 2022) was rejected for in-app
  Patreon, Liberapay and GitHub Sponsors links — and **for a link to its own home page, because that
  page carried donation information**. A Buy Me a Coffee link in the app this template was extracted
  from was flagged the same way. None of those is the §3.2 shape (tiers unlock things, a platform takes
  a cut, a project is not a person), but the distinction has to survive a reviewer skimming for
  "external payment link", and a pre-launch rejection costs the closed-test window.
- **The reach is one level out.** The first app built from this template moved its tip from the app to
  the repository and the Pages site, then noticed that the listing's *Website* field and the last line
  of its full description point at exactly those pages. So "nothing the app links to" includes what
  the **listing** links to.
- **When to revisit:** once the app is live, with a track record to appeal from. The cheapest shape
  then is a tip on the repository alone, with the listing's Website field and full description kept
  off it. A Play Billing consumable is the unambiguous alternative, at the cost of a billing
  dependency, Google's cut, and an *in-app purchases* badge on the listing.

Region carve-outs (US external links since October 2025, the EEA's external-offers program) do not
help: both are fee programs with reporting obligations, which is not a serious route for occasional
tips. A sentence promising a *future* tip, with nothing to click, is a statement about pricing and is
fine anywhere.

---

## When Play asks again

1. Run `python3 scripts/aab-permissions.py` against the newest bundle, and compare its table with the
   one above. A new row is a new question.
2. Read `docs/privacy-policy.md` against the answers here. If one has moved, both move, in one commit.
3. Copy any value this file does not hold while the Console is open — a category, a date, a
   declaration's exact wording — so the next re-declaration can compare against it.
4. Re-date the sections you re-answered.
