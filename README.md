# Dialer

A private, Google-free call screener and dialer for GrapheneOS: on-device spam
detection, a simple block list, caller announcement, and a high-contrast,
easy-to-read interface.

See [PLAN.md](PLAN.md) for the full plan and roadmap.

## Status

Phase 1. The app takes the "Caller ID & spam app" role and checks calls from
unknown numbers before they ring:

- **Spam settings:** a protection level (Off, Balanced, Strict) and three plain
  choices (Ring, Silence or Block) for likely spam, hidden numbers and copycat
  numbers.
- **Block list:** exact numbers or "numbers starting with", added from the
  settings or straight from Recent calls.
- **Observe only (on by default):** every call rings, and Recent calls show
  what would have been silenced or blocked, and why. Turn it off in the
  settings once the labels look right.
- **Recent calls:** time, number, carrier verification and the outcome.

Calls from contacts are never sent to the app, so they always ring.

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

## Installing

Every change merged into `main` that passes all checks is published as a
signed release. Open the repo's **Releases** page (on a phone: the **Code**
tab, then scroll to **Releases**), tap the newest one, and tap the
`dialer-0.1.N.apk` file to install it. Each release installs over the
previous one, and the app's data is kept.

Test builds from pull requests are named **Dialer (test)** and install
alongside the real app. They're under the **Actions** tab, in the run's
**Artifacts** section, as a zip containing the APK.

## Release signing

Releases are signed with a private key that lives only in two GitHub
secrets (**Settings → Secrets and variables → Actions**):

| Secret | Contents |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | The keystore file (`release.p12`), base64-encoded |
| `SIGNING_PASSWORD` | Its password (the key alias is `dialer`) |

Keep an offline backup of the keystore and password. Android only installs
an update when it's signed with the same key, so if the key is lost, the
app has to be uninstalled and reinstalled once with a new one. Without
these secrets, the build still runs and skips publishing a release.
