#!/usr/bin/env bash
# Installs the app on an emulator, makes it the call screening app,
# places a fake incoming call, and checks the app screened it.
set -euo pipefail

pkg="io.github.secnewt.dialer"
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
if grep -q "Incoming call" screening.log; then
  echo "Emulator call test passed: the incoming call was screened."
else
  echo "::error::The app did not screen the incoming call."
  exit 1
fi
