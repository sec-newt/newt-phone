# Dialer

A private, Google-free call screener and dialer for GrapheneOS: on-device spam
detection, a simple block list, caller announcement, and a high-contrast,
easy-to-read interface.

See [PLAN.md](PLAN.md) for the full plan and roadmap.

## Status

Phase 1 skeleton. The app can take the "Caller ID & spam app" role and logs
whether the carrier verified each incoming call. It does not block anything yet.

## Automatic checks

Every push runs these on GitHub before an APK is published:

| Check | What it catches |
| --- | --- |
| Logic tests | Mistakes in number matching, block rules and spam scoring |
| Screenshots | Every screen in light, dark and at the largest font size (download the `screenshots` artifact to look) |
| Accessibility | Low contrast, small touch targets, missing screen-reader labels |
| Lint | Common Android bugs and security problems |
| Permission audit | Any permission not listed in `config/allowed-permissions.txt` (e.g. internet access) |
| Secret scan | Passwords or keys accidentally committed, across the whole history |

Pull requests also run an **emulator call test**: the app is installed on an
Android emulator, made the call screening app, and receives a fake call.

Dependabot opens pull requests for dependency updates and security fixes; they
go through the same checks.

## Getting the APK

Every push builds a debug APK on GitHub Actions, but only if all checks pass. Open the **Actions** tab, pick
the latest **Build APK** run, and download `dialer-debug-apk` from the
**Artifacts** section. Unzip it and install the `.apk` on the phone.

Until a permanent signing key is set up, each build may be signed with a
different debug key, so you may need to uninstall the old version before
installing a new one.
