# SALTO HCE Probe V2

A phone-only passive HCE probe for an authorized SALTO lock test.

## What changed in V2
- Shows NFC ON/OFF status.
- Shows `HCE COMMUNICATION DETECTED` directly on the phone.
- Displays the last APDU and timestamp.
- Copy Last APDU button.
- Clear Result button.
- No ADB/logcat required.

The probe intentionally replies `6A82`; it does not unlock or modify the lock.

## Build without Android Studio
Push this project to GitHub. GitHub Actions builds the debug APK automatically.
Open **Actions → Build APK → latest successful run → Artifacts → SaltoHCEProbeV2-debug**.
