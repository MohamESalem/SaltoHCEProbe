# SALTO HCE Probe

Minimal Android HCE diagnostic app for an authorized access-control test.

## What it does
- Registers the AID `A000000743CC843413925E20C59B0100`.
- Logs any APDU sent by an NFC reader to that AID.
- Always answers `6A82`; it does **not** attempt to unlock anything.

## Build with Android Studio
1. Install Android Studio.
2. Open this folder as a project.
3. Let Android Studio install Android SDK 35 if prompted.
4. Wait for Gradle Sync.
5. Build > Build App Bundle(s) / APK(s) > Build APK(s).
6. APK path: `app/build/outputs/apk/debug/app-debug.apk`.

## Install with ADB
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Test
1. Enable NFC on the phone.
2. Run:
```powershell
adb logcat -c
adb logcat | findstr SALTO_PROBE
```
3. Hold the phone at the reader.
4. If the reader selects the registered AID, you should see `SALTO_PROBE RX: ...`.
