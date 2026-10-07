# Dialer

A private, Google-free call screener and dialer for GrapheneOS: on-device spam
detection, a simple block list, caller announcement, and a high-contrast,
easy-to-read interface.

See [PLAN.md](PLAN.md) for the full plan and roadmap.

## Look

Tokyo Night colors on a true black background (Tokyo Night Day in light
mode), with every text color at 7:1 contrast or better. Contacts show their
photo, or their initial on a color of their own. Fonts are bundled, nothing
is downloaded: Chakra Petch for screen titles, Atkinson Hyperlegible
(designed for low vision) for names and text, and JetBrains Mono for phone
numbers. Font licenses (SIL OFL) are in `app/src/main/assets/licenses`.

## Status

Phase 3b (first part): the app can be the default phone app. Turn it on in
Settings, "Make this my phone app"; the stock phone app stays installed, and
you can switch back in Android's Settings, Apps, Default apps.

- **Incoming calls:** a full screen with the caller's photo, name and number,
  any "Likely spam" warning, and two big buttons: Decline and Answer. It shows
  over the lock screen. Android still plays your ringtone.
- **In a call:** a running timer and big round buttons with words under them:
  Mute, Speaker (or Sound: Phone, Speaker, Bluetooth when Bluetooth is
  connected), Keypad (sends tones for "press 1 for…" menus), Hold and Add call,
  plus a wide End call button. The screen turns off against your ear.
- **Two calls:** answering a second call puts the first on hold; a card shows
  the held call with a Swap button.
- **Getting back:** a green "Return to call" strip on the main screens, and a
  notification with Answer/Decline or Hang up.
- **Emergency calls:** once this is the phone app, 911 is placed from here like
  any other call (Android keeps its own emergency fallback if the app fails).
- **Dial links:** tel: links and "call" buttons in other apps open the dialpad
  with the number filled in.

Phase 3a. Three tabs at the bottom: Favorites, Recents and Contacts, plus a
dialpad button on every tab.

- **Recents:** the phone's call history (asked for when you open it), with
  missed calls in red, back-to-back calls grouped ("Incoming, 2 calls"),
  "Likely spam" or "Blocked number" labels from spam screening, and a Call
  button on every row. A panel at the top shows spam protection and caller
  announcement, and opens the screened-calls list.
- **Dialpad:** big keys with letters, hold 0 for +, hold delete to clear,
  and matching contacts appear after 3 digits.
- **Calling:** the first call asks to "make and manage phone calls"; after
  that, Call places the call directly. Without it, Call opens the phone app
  with the number filled in. Until this is the phone app, emergency numbers
  (911) go through the stock phone app, which shows the number so one tap
  places the call.

- **Favorites:** a grid of photo tiles. Tap a tile to call, or the corner
  button to open the contact. A card above it
  says what Do Not Disturb lets ring (with a shortcut to its settings if it
  isn't set to "Starred contacts").
- **Contacts:** search by name or number, and tap the star to add or remove a
  favorite. Stars are saved in the phone's own contacts, so Do Not Disturb's
  "Starred contacts" exception and other apps see them. Needs Contacts access
  (read and change), asked for only when you allow it on these tabs. On
  GrapheneOS, give full access rather than Contact Scopes.

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
- **Caller announcement (off by default):** the phone says "Call from Mom",
  "Call from 5 5 5, 0 1 9…" or "Likely spam, from …" while it rings. Choose
  Always or With headphones; it stays quiet on a silenced phone and, by
  default, during Do Not Disturb. Turning it on asks for Phone, Call log and
  Contacts access, used only on the phone. Needs a text-to-speech voice
  (for example RHVoice or eSpeak NG from F-Droid).

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
Android emulator, made the call screening app, and receives a fake call; then
it is made the default phone app and a second call must open its call screen
without crashing.

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
