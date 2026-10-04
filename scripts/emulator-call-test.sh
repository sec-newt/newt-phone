#!/usr/bin/env bash
# Installs the app on an emulator, makes it the call screening app, turns on
# caller announcement, places a fake incoming call, and checks the app
# screened it, saved it to recent calls, and announced it.
set -euo pipefail

pkg="io.github.secnewt.dialer.debug"
adb install -r app-debug.apk

adb shell cmd role add-role-holder android.app.role.CALL_SCREENING "$pkg"
adb shell cmd role get-role-holders android.app.role.CALL_SCREENING | grep -q "$pkg"
echo "App holds the call screening role."

# Turn on caller announcement the way the settings screen would.
for permission in READ_PHONE_STATE READ_CALL_LOG READ_CONTACTS; do
  adb shell pm grant "$pkg" "android.permission.$permission"
done
printf '%s\n' \
  "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>" \
  '<map>' \
  '    <string name="announce_mode">ALWAYS</string>' \
  '    <boolean name="quiet_during_dnd" value="false" />' \
  '</map>' \
  | adb shell "run-as $pkg sh -c 'mkdir -p shared_prefs && cat > shared_prefs/spam_settings.xml'"
echo "Caller announcement turned on."

adb logcat -c
adb emu gsm call 5550197731
sleep 10
adb emu gsm cancel 5550197731
sleep 2

adb logcat -d -s SpamScreening:I CallAnnouncer:I | tee screening.log
if ! grep -q "Incoming call" screening.log; then
  echo "::error::The app did not screen the incoming call."
  exit 1
fi

# The call must also be saved to the app's recent calls list.
adb shell run-as "$pkg" cat files/screening-log.json | tee recent-calls.json
if ! grep -q "5550197731" recent-calls.json; then
  echo "::error::The call was screened but is missing from the recent calls list."
  exit 1
fi

if ! grep -q "Announcing incoming call" screening.log; then
  echo "::error::Caller announcement is on, but the ringing call was not announced."
  exit 1
fi
echo "Emulator call test passed: the call was screened, saved to recent calls and announced."
