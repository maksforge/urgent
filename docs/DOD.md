# Definition of done — what is still open

The **live checklist**. Keep it short: when an item closes, tick it, write the *result* into whatever
long-form record you keep, and delete the detail from here. A session should be able to pick up the
work by reading this file alone.

## The standing schema gate — parked

Urgent has no database (ADR-0001's amendment). The gate's five points come back verbatim from the
template's `docs/DOD.md` the day a feature adds a table.

## Prove it on a real call

- [ ] **Two calls from another phone, this one on silent.** The first stays silent, the second rings
      at alarm volume and stops when it is answered or declined. Then again on vibrate, and once with
      Do Not Disturb on. `PHONE_STATE` cannot be faked over adb, so this is the only end-to-end test.
- [ ] **Decide whether Urgent can be on Google Play at all.** `READ_CALL_LOG` is restricted to default
      dialers and a short list of exceptions; the Permissions Declaration Form is where that is
      argued, and a refusal means distributing outside Play.

## Before the first upload

- [x] **Delete what the app does not need** — Room, photos, backup and reminders are gone.
- [x] **Decide `minSdk` deliberately.** 26, kept on 2026-10-13; the reason is beside it in `app/build.gradle.kts`.

- [ ] **Replace the placeholder mark.** `art/mark.py`, then `python3 art/make-launcher-icon.py` and
      `make-feature-graphic.py`. Commit the concept image and record its provenance in
      `art/README.md` — where the art came from is the thing most likely to block a first upload, and
      it is discovered late.
- [x] **Replace the placeholder domain** with `call/` and `ui/home/`.
- [x] **Rewrite `scripts/edge-to-edge.py`'s SCENES** against Home, Settings, Support and Licences.
- [x] **Make the artifact allowlists yours.** All four `aab-*.py` pass on a local `bundleRelease`.
- [ ] **Choose the palette.** Four seeds in `scripts/gen_scheme.py`, regenerate `theme/Color.kt`,
      read the contrast report it prints on stderr.
- [x] **Put a real support address in `ui/support/SupportHandoff.kt`** — `urgent.support@gmail.com`.

- [ ] **Write the listing.** `docs/store-listing.md` — every heading in it is parsed by a script.
- [ ] **Write the privacy policy** and confirm GitHub Pages is serving `docs/`. Play requires a
      *hosted* URL, and an offline app has no server of its own.
- [ ] **Answer the Console's App content pages and write [`play-app-content.md`](play-app-content.md)
      in the same sitting** — the reasons are in front of you then and gone a week later. It moves in
      the same commit as the privacy policy, every time.
- [x] **`LICENSE` and `README.md`** — all rights reserved, source-available, as Gloam.

- [ ] **Back up the upload keystore.** `~/.keystores/urgent-upload.jks` and its password in
      `local.properties` exist only on this machine. Losing them means never updating the app on Play
      again. Fill in *Backed up to* in `RELEASING.md` the same day.

- [ ] **Set up the GitHub repository** — `python3 scripts/repo-setup.py` does the ruleset, the
      merge setting and Pages; the release-please PAT is the one step it cannot do, and without it
      no release PR is ever opened. *Setting up a new repository* in [`RELEASING.md`](RELEASING.md).
      **None of this is inherited from the template** — GitHub copies files, never settings — and
      the build stays green while it is all still undone.
- [ ] **Set the five Play secrets and create the service account** (`docs/RELEASING.md`) — the
      service account is the one step CI cannot do for itself. No account permissions; two app
      permissions, on this app only; no expiry date.
- [ ] **Plan the closed test.** A new personal developer account reaches production only after a
      closed test — at the time of writing 12 testers opted in for 14 continuous days, then an access
      application. Recruit the testers before the build is ready, not after; `publish-play-closed.yml`
      moves each build to them.
- [ ] **Decide the `applicationId` deliberately.** It is fixed the moment the Play entry is created —
      not renameable, not transferable without losing every install and review.

## After the closed test

- [ ] **The production-access application is in, with its answers recorded verbatim** in
      [`play-app-content.md`](play-app-content.md). Until it is granted the production track does not
      exist. The app this template was extracted from waited one day; that is one datapoint, not a
      promise. **If it comes back rejected, read the reason before changing any artifact.** Changing
      the build first leaves you arguing the reason against a different build.
- [ ] **Decide the first production number.** release-please never jumps to `1.0.0` by itself (with
      the major at 0 even `feat!:` bumps the minor), so it takes a `Release-As: 1.0.0` footer
      ([`RELEASING.md`](RELEASING.md)), or a deliberate decision to stay on 0.x. In order: production
      access granted, *then* that version's release notes in every locale on an ordinary branch (never
      on release-please's own, which it force-pushes), *then* the AAB and the listing go up together.

## Standing checks that never close

- [ ] **Every release: run the artifact checks on the built AAB**, not on the source.
      `aab-permissions.py` is the one that finds what a *dependency* merged into your manifest —
      a permission you never declared, or a `uses-feature` that quietly filters the app off devices.
- [ ] **Every release: read the release notes gate's output** rather than trusting it passed.
- [ ] **Every dependency bump: re-run `licensee`.** The build fails on an unallowed licence, which is
      the point — but the fix is two things, the allowlist *and* the bundled text.
- [ ] **Test the release-shaped build on a device.** A missing R8 keep rule does not crash; it makes
      a feature silently stop working.
