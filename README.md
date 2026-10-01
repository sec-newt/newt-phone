# Dialer

A private, Google-free call screener and dialer for GrapheneOS: on-device spam
detection, a simple block list, caller announcement, and a high-contrast,
easy-to-read interface.

See [PLAN.md](PLAN.md) for the full plan and roadmap.

## Status

Phase 1 skeleton. The app can take the "Caller ID & spam app" role and logs
whether the carrier verified each incoming call. It does not block anything yet.

## Getting the APK

Every push builds a debug APK on GitHub Actions. Open the **Actions** tab, pick
the latest **Build APK** run, and download `dialer-debug-apk` from the
**Artifacts** section. Unzip it and install the `.apk` on the phone.

Until a permanent signing key is set up, each build may be signed with a
different debug key, so you may need to uninstall the old version before
installing a new one.
