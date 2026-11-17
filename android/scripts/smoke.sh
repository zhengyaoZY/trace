#!/usr/bin/env bash
set -uo pipefail
cd "$(dirname "$0")/.."
test_status=0
./gradlew -PtraceAbis=x86_64 connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true || test_status=$?
mkdir -p app/build/reports/androidTests/screenshots
artifact_status=0
adb pull /sdcard/Android/data/com.zhengyaozy.trace/files/screenshots/. app/build/reports/androidTests/screenshots/ || artifact_status=$?
if [ "$test_status" -ne 0 ]; then
    exit "$test_status"
fi
exit "$artifact_status"
