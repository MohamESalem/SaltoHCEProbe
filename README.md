# SALTO HCE Probe V3

Passive Android NFC diagnostic probe for an authorized SALTO lock test.

V3 adds Android 15+ Observe Mode and polling-frame capture. It also keeps the SALTO JustIN Mobile AID probe and broadens routing to the SALTO RID prefix when the phone supports AID-prefix registration.

## Build without Android Studio
Push this folder to GitHub. The included GitHub Actions workflow builds `app-debug.apk` and uploads it as `SaltoHCEProbeV3-debug`.

## Test
1. Turn NFC on.
2. Open the app and keep it in the foreground.
3. Confirm `Preferred HCE service: YES` and, if available, `Observe Mode active: YES`.
4. Clear results.
5. Hold the phone at the SALTO reader for 5–10 seconds.
6. Screenshot the result, especially `Last polling frame` and `Last APDU`.

This build is diagnostic only. It does not contain a valid access credential and intentionally rejects APDU application requests.
