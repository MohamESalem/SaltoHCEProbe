package com.mohammed.saltohceprobe;

import android.content.SharedPreferences;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProbeService extends HostApduService {
    private static final String TAG = "SALTO_PROBE";
    public static final String PREFS = "probe_prefs";
    public static final String KEY_LAST_EVENT = "last_event";
    public static final String KEY_LAST_APDU = "last_apdu";
    public static final String KEY_EVENT_COUNT = "event_count";
    public static final String KEY_APDU_COUNT = "apdu_count";
    public static final String KEY_LAST_TIME = "last_time";

    private String now() {
        return new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date());
    }

    private void saveEvent(String event, boolean isApdu) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int eventCount = prefs.getInt(KEY_EVENT_COUNT, 0) + 1;
        int apduCount = prefs.getInt(KEY_APDU_COUNT, 0) + (isApdu ? 1 : 0);
        SharedPreferences.Editor editor = prefs.edit()
                .putString(KEY_LAST_EVENT, event)
                .putString(KEY_LAST_TIME, now())
                .putInt(KEY_EVENT_COUNT, eventCount)
                .putInt(KEY_APDU_COUNT, apduCount);
        if (isApdu) {
            editor.putString(KEY_LAST_APDU, event.replace("RX: ", ""));
        }
        editor.apply();
        Log.i(TAG, event);
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        StringBuilder sb = new StringBuilder();
        for (byte b : commandApdu) {
            sb.append(String.format(Locale.US, "%02X", b & 0xFF));
        }
        saveEvent("RX: " + sb, true);

        // Passive probe only. 6A82 = application/file not found.
        return new byte[] {(byte) 0x6A, (byte) 0x82};
    }

    @Override
    public void onDeactivated(int reason) {
        saveEvent("Deactivated (reason=" + reason + ")", false);
    }
}
