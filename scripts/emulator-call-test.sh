#!/usr/bin/env bash
# Installs the app on an emulator, makes it the call screening app,
# places a fake incoming call, and checks the app screened it.
set -euo pipefail

pkg="io.github.secnewt.dialer.debug"
adb install -r app-debug.apk

adb shell cmd role add-role-holder android.app.role.CALL_SCREENING "$pkg"
adb shell cmd role get-role-holders android.app.role.CALL_SCREENING | grep -q "$pkg"
echo "App holds the call screening role."

adb logcat -c
adb emu gsm call 5550197731
sleep 10
adb emu gsm cancel 5550197731
sleep 2

adb logcat -d -s SpamScreening:I | tee screening.log
if ! grep -q "Incoming call" screening.log; then
  echo "::error::The app did not screen the incoming call."
  exit 1
fi

# The call must also be saved to the app's recent calls list.
adb shell run-as "$pkg" cat files/screening-log.json | tee recent-calls.json
if grep -q "5550197731" recent-calls.json; then
  echo "Emulator call test passed: the call was screened and saved to recent calls."
else
  echo "::error::The call was screened but is missing from the recent calls list."
  exit 1
fi
