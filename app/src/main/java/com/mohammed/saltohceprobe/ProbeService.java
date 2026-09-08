package com.mohammed.saltohceprobe;

import android.content.SharedPreferences;
import android.nfc.cardemulation.HostApduService;
import android.nfc.cardemulation.PollingFrame;
import android.os.Bundle;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProbeService extends HostApduService {
    private static final String TAG = "SALTO_PROBE";
    public static final String PREFS = "probe_prefs";
    public static final String KEY_LAST_EVENT = "last_event";
    public static final String KEY_LAST_APDU = "last_apdu";
    public static final String KEY_EVENT_COUNT = "event_count";
    public static final String KEY_APDU_COUNT = "apdu_count";
    public static final String KEY_LAST_TIME = "last_time";
    public static final String KEY_POLL_COUNT = "poll_count";
    public static final String KEY_LAST_POLL = "last_poll";

    private String now() {
        return new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date());
    }

    private void saveEvent(String event, boolean isApdu, boolean isPoll) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int eventCount = prefs.getInt(KEY_EVENT_COUNT, 0) + 1;
        int apduCount = prefs.getInt(KEY_APDU_COUNT, 0) + (isApdu ? 1 : 0);
        int pollCount = prefs.getInt(KEY_POLL_COUNT, 0) + (isPoll ? 1 : 0);
        SharedPreferences.Editor editor = prefs.edit()
                .putString(KEY_LAST_EVENT, event)
                .putString(KEY_LAST_TIME, now())
                .putInt(KEY_EVENT_COUNT, eventCount)
                .putInt(KEY_APDU_COUNT, apduCount)
                .putInt(KEY_POLL_COUNT, pollCount);
        if (isApdu) editor.putString(KEY_LAST_APDU, event.replace("RX APDU: ", ""));
        if (isPoll) editor.putString(KEY_LAST_POLL, event);
        editor.apply();
        Log.i(TAG, event);
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        String hex = toHex(commandApdu);
        saveEvent("RX APDU: " + hex, true, false);
        // Passive probe only: intentionally reject the application request.
        return new byte[] {(byte) 0x6A, (byte) 0x82};
    }

    @Override
    public void processPollingFrames(List<PollingFrame> frames) {
        if (frames == null || frames.isEmpty()) return;
        for (PollingFrame frame : frames) {
            String type;
            switch (frame.getType()) {
                case PollingFrame.POLLING_LOOP_TYPE_A: type = "NFC-A"; break;
                case PollingFrame.POLLING_LOOP_TYPE_B: type = "NFC-B"; break;
                case PollingFrame.POLLING_LOOP_TYPE_F: type = "NFC-F"; break;
                case PollingFrame.POLLING_LOOP_TYPE_ON: type = "FIELD-ON"; break;
                case PollingFrame.POLLING_LOOP_TYPE_OFF: type = "FIELD-OFF"; break;
                default: type = "UNKNOWN(" + frame.getType() + ")"; break;
            }
            String data = toHex(frame.getData());
            String event = "POLL " + type +
                    " data=" + (data.isEmpty() ? "<none>" : data) +
                    " gain=" + frame.getVendorSpecificGain() +
                    " t=" + frame.getTimestamp();
            saveEvent(event, false, true);
        }
    }

    @Override
    public void onDeactivated(int reason) {
        saveEvent("Deactivated (reason=" + reason + ")", false, false);
    }

    private static String toHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format(Locale.US, "%02X", b & 0xFF));
        return sb.toString();
    }
}
