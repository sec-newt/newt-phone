# Dialer App Plan

Living copy (with diagrams and mock-ups): https://claude.ai/code/artifact/551ffb50-2187-4868-87ca-1e6cef35b2d4 · Snapshot: Oct 1, 2026

## Overview

Build a private, Google-free call app for GrapheneOS that blocks and labels spam better than the stock dialer, entirely on the phone. No call data leaves the device unless a feature is explicitly turned on.

- **Phone:** GrapheneOS, which ships the open-source AOSP dialer. It has no Google spam labels, so we can't borrow Google's spam detection and have to build our own.
- **Carrier:** Ting, running on the T-Mobile network. That matters for STIR/SHAKEN caller verification (see Spam detection).
- **No Google dependencies:** no Play Services, Firebase or Google APIs. It should work with or without sandboxed Google Play installed.
- **Distribution:** just for me. Install the APK directly or track GitHub releases with Obtainium, so there's no Play Store review.
- **Stack:** Kotlin + Jetpack Compose. Each push builds an APK on GitHub Actions, so no local Android setup is needed.

## Spam detection

Spam detection runs on the phone itself: it weighs signals Android already provides plus rules I control, with no Google or cloud lookups. The app holds the "Caller ID & spam" role, so it sees every incoming call before the phone rings.

_(Diagram in the living copy: call screening flow · 3 checks, 4 outcomes)_

Contacts and starred numbers skip scoring entirely, so a real contact is never silenced by a rule.

- **Carrier verification (STIR/SHAKEN):** on Android 11+, the screening app sees whether the network verified the caller ID. T-Mobile signs and checks calls, but whether Ting passes the result through needs a real-world test (see Open questions).
- **Neighbor spoofing:** scammers often fake numbers that start like mine. Unknown callers matching my first 6 digits get a high score.
- **Hidden or unknown numbers:** a separate rule with its own action, since doctors and pharmacies sometimes call this way.
- **Optional offline spam list:** a periodically downloaded list stored on the phone. Candidate source: public complaint data such as the FTC's Do Not Call reports. Off by default, so the app needs no internet.
- **My own reports:** marking a call as spam adds it to the block list, and the blocked-calls log lets me undo mistakes.
- **Paid databases** (Hiya, Truecaller, Nomorobo): skipped. They cost money and send callers' numbers to a third party, which goes against the privacy goal.

### Spam settings screen

The spam screen is one short page with no rule builder. Choose a level, and optionally change any of three plain choices.

_(Diagram in the living copy: spam settings mock-up · main screen and one choice)_

- **One level control:** Off, Balanced or Strict, with a sentence saying what that level does. The levels just preset the rows below. Changing a row switches the level to "Custom."
- **Three rows, three words:** each type of unknown caller gets Ring, Silence or Block. There's no rule order, no conditions and no syntax.
- **The scoring stays out of sight:** carrier verification, copycat checks and the spam list combine into "Likely spam" automatically. The log shows why each call was flagged, so I never have to tune weights.
- **Patterns stay simple:** "Add to block list" asks one question, either "This number" or "Numbers starting with…", and picks up the number from recents when possible.
- **Mistakes are easy to undo:** every blocked or silenced call in the log has an "Allow this number" button.

## Features

Everything here works with standard Android APIs on GrapheneOS, and none of it needs Google services.

| Feature | What it does | Android piece | Effort |
| --- | --- | --- | --- |
| Personal block list | Exact numbers plus patterns (prefix, area code, hidden or unknown). Per rule: reject, silence or voicemail | Call screening role | Low |
| Spam rules | Score each unknown call and then block, silence or label it (see Spam detection) | Call screening role | Medium |
| Blocked-calls log | Review what was stopped and why, and allow a number back with one tap | App database | Low |
| Announce caller | Speaks the contact name, the number or "likely spam" while the phone rings. Modes: always, headset only, contacts only | Text-to-speech | Medium |
| Starred contacts / DND | Quick starring and a Favorites tab. The system's DND exceptions honor stars automatically | Contacts provider | Low |
| Dialpad, call log, contacts | The standard dialer screens | Default phone app role | Medium |
| In-call screen | Mute, speaker, hold, keypad, Bluetooth, merge or swap | In-call service | High |
| System block list | Shares blocks with the OS, so they carry over if I switch dialers | Blocked-numbers provider (default dialer only) | Low |

**Stretch ideas:** let starred contacts ring through silent mode; let a number through DND if it calls twice within 3 minutes; quiet hours separate from DND; visual voicemail (depends on the carrier, and probably unsupported on Ting).

## Look and readability

The goal is clean, modern Material 3 styling where high contrast, clear type and roomy layouts make everything easy to read without looking oversized.

**Readability (non-negotiable)**

- **Follows my system text and display size.** All text scales with Android's font size setting. Layouts reflow instead of clipping or truncating names at the largest sizes.
- **High contrast:** at least 7:1 for body text and important labels (WCAG AAA), and never light grey on white. A true-black dark theme and a light theme, both checked for contrast.
- **Clear type:** one clean sans-serif (the system font or Inter) in regular and medium weights, never thin weights. Names and numbers are the largest text on each screen.
- **Color is never the only signal:** spam, blocked and missed calls each get an icon and a word as well as a color.
- **Big touch targets:** at least 48dp everywhere, larger for answer, decline and end call. Spacing keeps buttons from being hit by accident.
- **Labeled icons:** bottom navigation shows icon plus text. Every control has a screen-reader label in case I use TalkBack or Select to Speak.

**Sleek and modern**

- Material 3 components, rounded cards, generous whitespace and one accent color. The accent is checked for contrast, so wallpaper-based dynamic colors are used only if they pass.
- Short, subtle animations that turn off when Android's "Remove animations" is on.
- Calm screens: one main action per screen, details tucked one tap away.

**Key screens**

- **Incoming call:** caller name or number very large and centered, a clear spam badge if flagged, and wide answer/decline buttons near the bottom.
- **Dialpad:** large, bold digits with the typed number shown big at the top.
- **Recents and contacts:** one call per row with name, time and an icon for incoming, outgoing, missed or blocked. Favorites at the top.

_(Diagram in the living copy: rough mock-ups · incoming call, likely spam, dialpad)_

The caller is always the biggest thing on screen, spam is flagged with a word and an icon as well as color, and the answer and decline buttons are wide enough to hit without looking closely.

- [ ] Test every screen at the largest font and display size, in both themes.

## Roadmap

Start with a screening app that works alongside the GrapheneOS dialer. It delivers the blocking and spam features first, without risking missed calls.

1. **Phase 1: Call screener.** Block list, spam rules, blocked-calls log and caller announcement. Runs next to the stock dialer as the call screening app.
   - Done when: a week of normal use with no wrongly blocked real calls, and spam that previously rang is silenced or labeled.
2. **Phase 2: Contacts and favorites.** Quick starring, a Favorites tab and a contact view that's friendly to DND exceptions.
   - Done when: starring from the app shows up in the system DND exceptions.
3. **Phase 3: Full dialer (optional).** Split in two: 3a adds the dialpad, Recents from the call log and direct calling while the stock app still handles live calls; 3b adds the in-call and incoming-call screens, the system block list and the default phone app role (done: the role, call screens, notification, dial links, the system block list and conference calls). Dialpad, call log, in-call screen, and the system block list. The app becomes the default phone app.
   - Done when: incoming, outgoing and emergency calls are tested, plus Bluetooth/car and a call while another is active. Keep the stock dialer installed as a fallback.

- [x] Create the GitHub repo for the app
- [ ] Set up the project skeleton and an APK build on GitHub Actions
- [ ] Phase 1 build

## GrapheneOS notes

GrapheneOS supports everything this app needs. A few of its privacy features need the right settings.

- **Contact Scopes:** if the app only gets scoped contacts, it can't tell contacts from unknown callers. Give it full contacts access.
- **Text-to-speech voice:** caller announcement needs a TTS engine. If none is installed, a FOSS one from F-Droid works, such as RHVoice or eSpeak NG. Check Settings → Accessibility → Text-to-speech output.
- **Roles:** Settings → Apps → Default apps → Caller ID & spam app (screening, Phase 1) and Phone app (Phase 3).
- **Network permission:** the app doesn't need internet for Phase 1. Leave GrapheneOS's Network permission off unless an optional downloaded spam list is turned on.
- **Installing:** allow "install unknown apps" for the browser or Obtainium. Updates stay signed with the same key, so they install over the previous build.
- **Prior art to study:** Fossify Phone (open-source dialer) and Yet Another Call Blocker (open-source, offline spam lists), both on F-Droid.

## Later: editing contacts

Contacts sync to a Radicale server (CardDAV). When contact editing is added, new contacts must be saved to that sync account, not a phone-only one, and a `.vcf` export comes first so nothing can be lost.

## Open questions

- [ ] Which Pixel model and GrapheneOS build? This sets the minimum Android version.
- [ ] Does Ting pass STIR/SHAKEN verification through to the phone? Test it with a Phase 1 debug screen that logs each call's verification status.
- [ ] Does Ting offer carrier-side scam blocking on its T-Mobile service? If so, turn it on as a first filter.
- [ ] Default action for "likely spam": silence and label, or reject outright?
- [ ] Should caller announcement speak during DND, or stay quiet?
- [ ] App name.
