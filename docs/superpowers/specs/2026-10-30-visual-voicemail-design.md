# Visual voicemail — design

Oct 30, 2026

## Why

Most of my voicemails aren't important. Today I have to call voicemail and
listen through menus to find that out. I want to **read** a message, delete
it, and move on. When a message does matter, I want to copy a callback number
or a bit of the text and use it somewhere else. I had visual voicemail before
and miss it.

## What success looks like

- A new voicemail shows up in Newt Phone on its own, already transcribed.
- I can read it, tap the number in it, copy any part of it, and delete it in
  a few seconds, without calling in.
- Deleting in Newt Phone deletes it at Ting too.

## Goals

- Download voicemails from Ting's visual voicemail server.
- Transcribe each one **on the phone**.
- A Voicemail tab with a list, and a screen for one message with the
  transcript, playback, Call back, Text and Delete.

## Non-goals (for now)

- Changing the greeting or the voicemail PIN. The "hold 1" call to voicemail
  still covers those.
- A transcript or Delete button inside the notification. I'd rather read it
  in the app.
- Deleting straight from the list (swipe or hold). Opening a message first is
  fine.
- Spam labels on voicemails, backup or export of voicemails, sharing a
  voicemail count with Newt Home.

## Anti-goals (a failure even if it works)

- Voicemail audio or transcripts going to anyone besides Ting. That rules out
  cloud transcription, YouMail and similar services.
- Internet use by anything besides the voicemail download.
- Requiring Google Play Services. The phone runs GrapheneOS.
- Text that's hard to read: the transcript must follow `NEWT_STYLE.md`
  (7:1 contrast, works at 200% text size, works with a screen reader).

## Facts this rests on

- **Ting supports it.** Ting has no app of its own and relies on the phone's
  built-in visual voicemail ([help.ting.com](https://help.ting.com/mobile-articles/visual-voicemail)).
  I'm on legacy Ting, and **I've confirmed visual voicemail is turned on in my
  account** (Oct 30, 2026). The network itself isn't verified yet (see Step 1).
- **On Android, "built-in" means the Phone app does the work.** The stock
  Phone app runs the visual voicemail client: it activates by SMS, then
  downloads messages over IMAP on cellular data. Since Newt Phone is the
  default phone app, it has to do this itself.
- **Internet permission: approved by me (Oct 29, 2026), for voicemail only.**
  This changes the "never gets the internet permission" line in `PLAN.md`.

## How it works

### Getting the messages

- Newt Phone, as the default phone app, provides Android's
  `VisualVoicemailService`. It reads the SIM's settings from
  `CarrierConfigManager` (visual voicemail type, destination number, port),
  sends the activation SMS, and gets the server details back by SMS.
- The download uses **cellular data only**: a cellular network requested
  through `ConnectivityManager`, never Wi-Fi. That matches what Ting expects
  and keeps the traffic between the phone and Ting.
- The AOSP Dialer's visual voicemail code (`com.android.voicemail.impl`,
  Apache 2.0) is the reference implementation. **Builder's call** whether to
  port pieces of it or use a mail library. Either way it can't pull in Play
  Services, and its license must allow use in this app.
- Deleting a message deletes it on the server. Ting says deleted voicemails
  can't be restored, so the server delete waits until the Undo window
  (below) has passed.

### Permissions and the build check

- Add `INTERNET` (and whatever cellular network request needs, such as
  `CHANGE_NETWORK_STATE`) to `config/allowed-permissions.txt`, with a comment
  saying it's for visual voicemail only. Add any voicemail provider
  permissions the same way.
- Add a check that **only the voicemail package opens network connections**,
  for example a lint rule or a test that fails if networking classes are used
  outside it. The exact check is the builder's call. The goal is that the
  internet permission can't quietly spread to other features.
- **My call (from `NEWT_STYLE.md`: "off by default and explained"):** visual
  voicemail starts **off**. When it's off, the Voicemail tab shows one plain
  sentence ("Download your voicemails and read them here. Uses mobile data to
  reach Ting, nothing else.") and a Turn on button. There's a matching row in
  settings.

### Transcription

- Transcripts are made **on the phone with a model bundled in the app**.
  GrapheneOS has no built-in on-device speech recognizer to use, and the
  model must never be downloaded at runtime.
- **Builder's call:** the engine and model size. Candidates are Vosk (small
  English model, about 40 MB) and whisper.cpp (`tiny.en` or `base.en`).
  Pick based on accuracy on real phone-quality audio and APK size, then
  record the choice in `PLAN.md`. Visual voicemail audio is usually AMR, so
  decode it to PCM with `MediaExtractor`/`MediaCodec` first.
- Transcription runs in the background after download. Until it finishes,
  the row shows "Transcribing…". If it fails, the row shows "No transcript.
  Tap to listen."

### Storage

- **My call:** store messages in Android's voicemail provider
  (`VoicemailContract`) under Newt Phone's own source. Recents already
  understands the voicemail call type (`CallLogRepository.kt`), and this is
  how the stock dialer does it. If the provider's permissions get in the
  way, an app-private database is fine.
- Transcripts are stored with the message (the provider's transcription
  field, or the app database). Deleting a message removes its audio and
  transcript from the phone.

## What I see

Mock-ups were reviewed in chat on Oct 30, 2026.

### Voicemail tab

- A fourth tab, **between Recents and Contacts**. Swipe and tap work like
  the other tabs, and the tab bar shows an icon and the word "Voicemail".
- One card per message, newest first:
  - the contact name, or the number in JetBrains Mono
  - the time
  - a word badge **NEW** for unheard and unread messages (not just a color)
  - the first two or three lines of the transcript
  - the length of the recording
- Short messages can be read from the list without opening them.
- When the list is empty: "No voicemails."

### One message

- Name or number large at the top, then the date, time and length.
- **The transcript is the biggest text on the screen.** It's selectable, so
  holding it copies any part.
- Phone numbers inside the transcript are links. Tapping one opens the same
  call / text / copy / add-to-contacts menu used in Recents
  (`NumberActions.kt`).
- Playback: a large Play/Pause button, a progress bar, and the time. It
  plays through the earpiece by default with a Speaker toggle, like the
  in-call screen. Playback is secondary to the transcript.
- Buttons, at least 56 dp tall, with words: **Call back** (green), **Text**,
  **Delete** (red).
- Delete has **no confirmation dialog**. It returns to the list and shows
  "Voicemail deleted · Undo" for a few seconds. The server delete happens
  after that.
- Opening a message marks it read (no more NEW) and tells the server it
  was read.

### Notification

- "New voicemail from (name or number)". Tapping it opens that message. No
  transcript or action buttons in the notification.
- Opening the message clears the notification, the same way Recents clears
  missed calls.

### Settings

- A "Visual voicemail" row: On/Off, with one plain sentence.
- When it's on and something is wrong, a status line in plain words.
  Examples: "Ting hasn't answered the activation text yet," "Voicemail is
  full. Delete some messages so new ones can be saved," "Can't reach Ting.
  Will try again on mobile data."

## Order of work

1. **Prove it on my phone first.** Build the activation and IMAP login
   behind the setting, with a temporary status line showing each step
   (carrier settings found → activation SMS sent → reply received → logged
   in → N messages found). I install it, turn it on, and report what it
   says. If Ting's network doesn't do standard visual voicemail, stop here.
   Nothing else gets built.
2. Download and store messages; list and message screens with playback,
   delete with Undo, and the notification. No transcripts yet.
3. On-device transcription.
4. Clean up the status messages and record the outcome in `PLAN.md`: move
   visual voicemail out of "stretch ideas", update the internet line under
   Offline spam list, and close or answer the related open questions.

## Testing

- Logic tests: activation SMS parsing, number detection in transcripts,
  Undo timing, the "networking only in the voicemail package" check.
- Screenshot tests for the tab and the message screen in light, dark and
  200% text, plus accessibility checks, as on other screens.
- Transcription checked against a few sample AMR files in the test
  resources.
- The real test is my phone on Ting (Step 1). The emulator can't reach a
  real voicemail server.

## Left to the builder

The IMAP approach, the transcription engine and model, the exact
network-use check, the database layout if the provider doesn't work out,
and wording beyond what's written here (in plain words, per
`NEWT_STYLE.md`).
