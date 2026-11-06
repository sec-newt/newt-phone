# Newt Phone UI refresh, part 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every Newt Phone screen meets the UI refresh bar on newt-ui 2.0.0, including the incoming-call redesign (mockup B) and the Calls/spam redesign (mockup 3).

**Architecture:** Move to newt-ui 2.0.0 and run every screen test through the shared `renderScreen` helper, so all of them get the 16sp check. Then rebuild the incoming call, the in-call controls, Calls, the Block list and the dialpad/contacts rows from the shared pieces. New logic (context lines, last talked) lives in small pure functions with unit tests.

**Tech Stack:** Kotlin, Compose, newt-ui/newt-test 2.0.0, Robolectric + Roborazzi.

**Spec:** `newt-android-lib/docs/superpowers/specs/2026-11-04-ui-refresh-design.md` (Phone rows of "Per-app work" and "Feature changes"); mockups `round1-screens.html` B and `round2-menus.html` 3.

## Global Constraints

- No text under 16sp at 100%; nothing clipped, ellipsized or split mid-word at 200%.
- Never color alone: stripes and chips always carry a word.
- No new permissions. Text back uses the default SMS app (`ACTION_SENDTO smsto:`).
- Danger actions are outlined red, never filled, and never next to Answer.
- Local builds: `./gradlew --include-build ../newt-android-lib …` (the GitHub token can't read packages).
- Merge on green CI (owner's standing instruction for this rollout).

## Review Focus

1. **A hidden number rings:** there's no number to text back. Text back must not show; Decline, Silence and Answer still work. Task 2 tests it.
2. **Silence, then the call keeps ringing silently:** Answer and Decline must still work afterwards, and Silence becomes "Silenced" (disabled) so a second tap does nothing confusing. Task 2 tests it.
3. **No call history or no contacts permission:** the context lines simply don't show; there's no empty card. Task 2 tests `incomingContext` with nulls.
4. **A 200% font with a long name on the incoming screen:** Answer must stay on screen without scrolling. Task 2 has a 200% screenshot with a long name, and asserts Answer is displayed.
5. **Calls list with Block on a number that's already blocked:** show "Blocked" instead of Block. Task 4 tests it.

---

### Task 1: newt-ui 2.0.0, and the 16sp check on every screen

**Files:** `gradle/libs.versions.toml`; `MainActivity.kt` (drop `NewtReadingText`); `ui/*.kt` (text under 16sp: dialpad letters 16sp, matching-contact rows 21sp, any 12–15sp literals); tests `InCallScreensTest`, `ContactsScreensTest`, `SpamProtectionScreenTest` and the dialpad tests switch their screenshot helper to `renderScreen`; `NewtScreenTests.kt` drops `redesigned` and `NewtReadingText`.

- [ ] Step 1: Change the test helpers first, then run the suite. Expected: 16sp failures listing the offenders.
- [ ] Step 2: Fix each offender, in the theme's styles rather than new literals.
- [ ] Step 3: Run `testDebugUnitTest`. Expected: all pass. Commit "Use newt-ui 2.0.0; 16sp check on every screen".

### Task 2: Incoming call (mockup B)

**Files:** `calls/LiveCall.kt` (add `numberType: String? = null`, `lastTalkedMillis: Long? = null`, `lastTalkedSeconds: Long? = null`; `starred` already comes from `CallerInfo`, so carry it as `starred: Boolean = false`); `incall/CallerLookup.kt` (fill them: `PhoneLookup.TYPE/LABEL` → `Phone.getTypeLabel`, the latest answered call from `CallLog.Calls`); `incall/CallManager.kt` (`silence(context)`: stop the announcer and RingThrough, then `telecom.silenceRinger()`); `incall/InCallActivity.kt` (wire `onSilence` and `onTextBack`); `ui/IncomingContext.kt` (new); `ui/InCallScreens.kt`.

**Interfaces:**
- `fun incomingContext(call: LiveCall, nowMillis: Long): List<String>` gives "★ Favorite · mobile" (or just "mobile"), then "Last talked yesterday, 4 min" / "Last talked today, under a minute" / "Last talked Tuesday, 12 min" / "Last talked Oct 3, 2 min".
- `CallActions.onSilence: (String) -> Unit`, `CallActions.onTextBack: (String) -> Unit` (decline, then open `smsto:`).

- [ ] Step 1: Write `IncomingContextTest`: favorite plus type; type only; nothing for a hidden number with no data; the four "last talked" day forms; under a minute.
- [ ] Step 2: Write `InCallScreensTest` checks:
  - Text back declines and reports the id.
  - Silence reports the id and becomes "Silenced", disabled.
  - There's no Text back for a hidden number.
  - Answer is displayed at 200% with a long name.
  - Answer and Decline are two separate rows, not side by side.
- [ ] Step 3: Run them. Expected: FAIL.
- [ ] Step 4: Build the screen. Top: "Incoming call", the caller, the spam warning, and the context lines as two outlined info rows. Bottom third: Text back and Silence as a two-button row (`NewtButton`), Decline full width outlined red (`NewtPrimaryButton`, Danger tone), then Answer full width, filled green, large (the biggest thing).
- [ ] Step 5: Run the tests. Expected: PASS. Commit "Incoming call: context, Text back, Silence, Answer biggest".

### Task 3: In-call controls

**Files:** `ui/InCallScreens.kt` (`ControlButton`).

- [ ] Step 1: Test `controlLabelsSayTheirState`: muted shows "Muted" (filled), unmuted shows "Mute" (outlined); the label is 16sp or more (covered by renderScreen).
- [ ] Step 2: Implement it: the label is under the icon at 16sp, the button is filled with the gradient while on, and its word changes ("Muted", "Speaker on", "On hold").
- [ ] Step 3: Run the tests. Commit "In-call: worded controls, filled while on".

### Task 4: Calls / spam protection (mockup 3)

**Files:** `ui/SpamProtectionScreen.kt`.

- [ ] Step 1: Tests:
  - The status is a `NewtSummary`: "Spam protection · Watching" plus one sentence.
  - The rows are grouped under "Today"/"Yesterday"/dates in `NewtSettingsCard`s.
  - Each row has a verdict chip (Danger "Would silence"/"Silenced"/"Blocked", Good "Verified") and a Block button that calls `onBlock(number)`.
  - A blocked number shows "Blocked" with no button.
  - A hidden number has no Block button.
- [ ] Step 2: Run them. Expected: FAIL.
- [ ] Step 3: Implement it: the stripe is red for would-silence/blocked rows and green for verified, and the gear opens Settings.
- [ ] Step 4: Run the tests. Commit "Calls: verdict chips, Block buttons, day cards".

### Task 5: Block list and the deferred Phone minors

**Files:** `ui/BlockListScreen.kt`, `ui/SpamSettingsScreen.kt`, `ui/UnknownCallersScreen.kt`, `ui/RecentsScreen.kt`.

- [ ] Step 1: Tests:
  - The Block list row's value reads "2 numbers" (or "1 number", "None").
  - Settings shows "These open Android's own settings." under Phone.
  - Unknown callers in Watch mode shows the summary "Watching: these rules are shown, not enforced."
  - Recents says "Headphones" (not "With headphones").
  - The Block list screen uses cards, and each entry's Remove sits behind ⋯ (M6).
- [ ] Step 2: Run them. Expected: FAIL.
- [ ] Step 3: Implement.
- [ ] Step 4: Run the tests. Commit "Block list by the rules; settings wording fixes".

### Task 6: Contacts and favorites by the rules

**Files:** `ui/ContactsScreens.kt`.

- [ ] Step 1: Tests: contact names are 21sp (`titleLarge`); the contact page groups numbers in a `NewtSettingsCard`; Block is behind the ⋯ menu.
- [ ] Step 2: Implement with 2dp lifted cards.
- [ ] Step 3: Run the tests. Commit "Contacts: bigger names, cards, danger behind ⋯".

### Task 7: Pull request

- [ ] Run `testDebugUnitTest lint assembleDebug` with the included build. Expected: BUILD SUCCESSFUL.
- [ ] Push `ui-refresh-2` and open the PR with before/after screenshots (CI artifact). Link it, wait for green, merge.
