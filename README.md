# Urgent

Rings your Android phone when the same number calls twice within a few minutes — even while the
phone is on silent, on vibrate or in Do Not Disturb. Someone who calls straight back probably needs
you.

Android only lets repeated calls through inside Do Not Disturb; silent and vibrate mode mute them
like any other call. Urgent hears the call, checks the call log for an earlier one from the same
number, and plays your ringtone at alarm volume. It never changes your phone's sound settings.

Free, no ads, no server, no account. Nothing leaves the phone.

## Status

Working on a phone, not yet released. Not on Google Play: reading the call log is a restricted
permission there.

## Setup on the phone

1. Turn on **Ring for repeated calls** and allow the phone and call log permissions.
2. Set Urgent's battery use to **unrestricted** — Android 12+ will not let it ring from the
   background otherwise.
3. On Xiaomi, turn on **autostart** for Urgent.

## Contributing

Urgent is source-available, not open source: read it, verify it, file issues. Pull requests are not
accepted — see [LICENSE](LICENSE).

## Build

```bash
./gradlew assembleDebug          # build
./gradlew installDebug           # build + install on the connected phone
./gradlew test                   # JVM unit tests
./gradlew connectedAndroidTest   # instrumented tests — needs a device
./gradlew spotlessApply          # format (the CI gate is spotlessCheck)
python3 scripts/project.py       # what the toolchain thinks this app is called
```

Commit subjects are [Conventional Commits](https://www.conventionalcommits.org); release-please
derives the version and `CHANGELOG.md` from them. See [docs/RELEASING.md](docs/RELEASING.md).
