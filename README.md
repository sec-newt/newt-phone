# Newt Phone

A private, Google-free phone app for Android (built for GrapheneOS), designed to
be easy to see and use with low vision: large text, high contrast, big buttons
with words, and a voice that says who is calling.

> **A personal project, in beta.** I made this for my own phone and share it in
> case it helps someone else. It works for me every day, but there's no
> guarantee it will work for you. Keep your phone's stock dialer installed so
> you can switch back at any time. Android also falls back to its own dialer for
> emergency calls if this app ever fails.

## What it does

- **Phone app:** dialpad, Recents, Favorites and Contacts, plus its own
  incoming-call and in-call screens.
- **Spam screening, on the phone:** labels or silences likely spam, hidden and
  copycat numbers, and keeps a block list. No lookup services, no internet.
- **Caller announcement:** says "Call from Mom" (or the number) while it rings.
- **Ringing rules:** favorites and repeat callers ring even on silent, and
  optional quiet hours.
- **Easy to see:** Tokyo Night colors on true black, every text color at 7:1
  contrast or better, and Atkinson Hyperlegible, a font designed for low vision.

## Privacy

- **The app has no internet permission.** It can't send anything anywhere, and
  the build fails if that permission is ever added (see `config/allowed-permissions.txt`).
- Call history, contacts and settings stay on the phone, in the app's private
  storage, and are excluded from backups.
- No Google Play Services, Firebase, analytics or ads.
- The app's own logs never contain phone numbers or names.

## Install

The easiest way is [Obtainium](https://github.com/ImranR98/Obtainium), which
installs and updates apps straight from GitHub releases:

1. In Obtainium, tap **Add app** and paste this repository's link.
2. Install **Newt Phone**. Obtainium will offer updates as new releases appear.
3. Open Newt Phone, then **Settings → Make this my phone app**.

If Android says the app **"was denied access to be default Phone app"**, that's
Android's restricted settings, which apply to apps installed from a downloaded
file: open **Settings → Apps → Newt Phone**, tap the **⋮** menu, choose **Allow
restricted settings**, and try again. To switch back to the stock dialer, use
**Settings → Apps → Default apps → Phone app**.

Requirements: Android 11 or newer. Built and tested on GrapheneOS; other
Android versions should work but aren't tested.

## Permissions, and why

| Permission | Why | When it's asked |
| --- | --- | --- |
| Phone (read phone state) | Know when the phone rings, to announce the caller | When you turn on caller announcement |
| Call log | Show Recents; spot repeat callers | When you open Recents |
| Contacts (read and write) | Show names and photos; star favorites | When you open Favorites or Contacts |
| Make phone calls | Place calls directly | On your first call |
| Notifications | The incoming-call and in-call notification | When you make it your phone app |
| Full-screen alerts | Show the incoming-call screen over the lock screen | Granted with the phone app role |
| Keep awake | Turn the screen off against your ear during a call | No prompt (harmless) |
| Vibrate | Vibrate for a call while the ringtone takes turns with the announcement | No prompt (harmless) |

Becoming the **default phone app** and the **caller ID & spam app** are roles
you grant in Android's settings; you can take them back there at any time.

## Features in detail

### Phone app and calls

- **Ringing rules** (Settings, Ringing; need Newt Phone to be the phone app):
  starred contacts and anyone calling again within 3 minutes ring even on
  silent or vibrate (at alarm volume; never for likely spam), and optional
  quiet hours when everyone else's calls show silently.
- **Swipe between tabs:** slide left or right to move between Favorites,
  Recents and Contacts, or tap a tab at the bottom.
- **Tap-and-hold menus:** tap or hold a call in Recents to copy the number,
  add it to contacts, send a text, edit it before calling, or block it. Tap
  the number on the dialpad to paste a number in (or copy, save or text it).
  Hold a contact or a Favorites tile to edit it in your contacts app, or copy
  or text its number; a contact's page has an "Edit in Contacts" button, and
  tapping one of its numbers gives the same number menu.
- **Keypad sounds:** a short tone for each dialpad key (on by default, quiet
  when the phone is on silent or vibrate), in Settings, Phone settings.
- **Phone settings:** shortcuts to Android's own screens for ringtone and
  volume, calls and SIM (forwarding, call waiting, caller ID, Wi-Fi calling),
  voicemail, and hearing aids/TTY, plus the text-to-speech voice used for
  announcements.

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
  notification with Answer/Decline or Hang up. While the call screen is open,
  the notification waits quietly in the shade instead of popping up over it.
- **Emergency calls:** once this is the phone app, 911 is placed from here like
  any other call (Android keeps its own emergency fallback if the app fails).
- **Conference calls:** with one call on hold, Merge joins everyone into one
  call (when the carrier allows it); the screen lists who's on it.
- **Android's block list:** as the phone app, exact numbers on your block list
  are shared with Android's own block list, both ways, so they're turned
  away before ringing and their texts are blocked too. Numbers you blocked
  earlier in the stock phone app show up here. "Starting with" rules stay in
  this app. In observe-only mode nothing is sent to Android, since those calls
  are meant to keep ringing.
- **Dial links:** tel: links and "call" buttons in other apps open the dialpad
  with the number filled in.

### Main screens

Three tabs at the bottom: Favorites, Recents and Contacts, plus a dialpad
button on every tab.

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

### Spam screening

The app takes the "Caller ID & spam app" role and checks calls from
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
  Always or With headphones, and by default it repeats every few seconds
  until the call is answered or stops ringing ("Repeat until answered").
  When Newt Phone is your phone app, the ringtone and the voice take turns:
  the ring pauses while the caller is announced, so the voice is never
  drowned out (vibration keeps going if "Vibrate for calls" is on). It
  stays quiet on a silenced phone and, by default, during Do Not Disturb. Turning it on asks for Phone, Call log and
  Contacts access, used only on the phone. Needs a text-to-speech voice
  (for example RHVoice or eSpeak NG from F-Droid).

Calls from contacts are never treated as spam, so they always ring.

## How this was made

Newt Phone is my idea and my design decisions: what it should do, how it should
look, and what matters for someone with low vision. The code was written by AI
(Claude, by Anthropic, using Claude Code), working from my requests. I
reviewed each change, tested it on my own phone, and decided what got merged.
Every change also has to pass the automatic checks below before it can be
released.

## Reporting problems

- **Bugs and ideas:** open an issue. This is a personal project, so I may be
  slow to respond and can't promise fixes.
- **Security problems:** please report privately; see [SECURITY.md](SECURITY.md).

The plan and roadmap are in [PLAN.md](PLAN.md); the shared look for future Newt
apps is in [NEWT_STYLE.md](NEWT_STYLE.md).

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

## Installing a release by hand

Every change merged into `main` that passes all checks is published as a
signed release. Open the repo's **Releases** page (on a phone: the **Code**
tab, then scroll to **Releases**), tap the newest one, and tap the
`dialer-0.1.N.apk` file to install it. Each release installs over the
previous one, and the app's data is kept.

Test builds from pull requests are named **Newt Phone (test)** and install
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

## License

Newt Phone is free software under the [GNU General Public License v3.0](LICENSE):
you may use, study, share and change it, and versions you share must stay under
the same license. Bundled fonts are under the SIL Open Font License; their
license files are in `app/src/main/assets/licenses`.
