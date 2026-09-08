package com.mohammed.saltohceprobe;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TextView nfcStatus;
    private TextView status;
    private TextView details;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable refresher = new Runnable() {
        @Override public void run() {
            refreshStatus();
            handler.postDelayed(this, 300);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(44, 56, 44, 56);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("SALTO HCE Probe V2");
        title.setTextSize(27f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("\nPhone-only reader test. No laptop or ADB needed.\n");
        subtitle.setTextSize(16f);
        root.addView(subtitle);

        nfcStatus = new TextView(this);
        nfcStatus.setTextSize(17f);
        nfcStatus.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(nfcStatus);

        TextView steps = new TextView(this);
        steps.setText(
                "\n1. Turn NFC on.\n" +
                "2. Keep the phone unlocked and this screen open.\n" +
                "3. Put the phone's NFC area directly on the black SALTO reader for 5–10 seconds.\n" +
                "4. Move it slowly a few cm if needed.\n"
        );
        steps.setTextSize(16f);
        root.addView(steps);

        status = new TextView(this);
        status.setGravity(Gravity.CENTER);
        status.setPadding(22, 34, 22, 34);
        status.setTextSize(21f);
        status.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(status);

        details = new TextView(this);
        details.setPadding(0, 24, 0, 24);
        details.setTextSize(15f);
        details.setTypeface(Typeface.MONOSPACE);
        details.setTextIsSelectable(true);
        root.addView(details);

        Button copy = new Button(this);
        copy.setText("Copy last APDU");
        copy.setOnClickListener(v -> copyLastApdu());
        root.addView(copy);

        Button clear = new Button(this);
        clear.setText("Clear result");
        clear.setOnClickListener(v -> {
            getSharedPreferences(ProbeService.PREFS, MODE_PRIVATE).edit().clear().apply();
            refreshStatus();
        });
        root.addView(clear);

        TextView note = new TextView(this);
        note.setText("\nThis app only detects APDUs addressed to its registered HCE application and intentionally rejects them. It does not unlock or modify the lock.");
        note.setTextSize(13f);
        root.addView(note);

        setContentView(scroll);
        refreshStatus();
    }

    @Override protected void onResume() {
        super.onResume();
        handler.post(refresher);
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refresher);
    }

    private void refreshStatus() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter == null) {
            nfcStatus.setText("NFC: NOT SUPPORTED");
        } else if (!adapter.isEnabled()) {
            nfcStatus.setText("NFC: OFF — turn it on first");
        } else {
            nfcStatus.setText("NFC: ON ✓");
        }

        SharedPreferences prefs = getSharedPreferences(ProbeService.PREFS, MODE_PRIVATE);
        int apduCount = prefs.getInt(ProbeService.KEY_APDU_COUNT, 0);
        int eventCount = prefs.getInt(ProbeService.KEY_EVENT_COUNT, 0);
        String event = prefs.getString(ProbeService.KEY_LAST_EVENT, "");
        String time = prefs.getString(ProbeService.KEY_LAST_TIME, "");
        String apdu = prefs.getString(ProbeService.KEY_LAST_APDU, "");

        if (apduCount == 0) {
            status.setText("WAITING — NO HCE APDU DETECTED");
            details.setText(
                    "No reader command has reached the app yet.\n\n" +
                    "Events: " + eventCount +
                    (event.isEmpty() ? "" : "\nLast event: " + event)
            );
        } else {
            status.setText("HCE COMMUNICATION DETECTED ✓");
            details.setText(
                    "APDUs received: " + apduCount + "\n" +
                    "Events: " + eventCount + "\n" +
                    "Last time: " + time + "\n\n" +
                    "Last APDU:\n" + apdu + "\n\n" +
                    "Last event:\n" + event + "\n\n" +
                    "Take a screenshot of this screen."
            );
        }
    }

    private void copyLastApdu() {
        SharedPreferences prefs = getSharedPreferences(ProbeService.PREFS, MODE_PRIVATE);
        String apdu = prefs.getString(ProbeService.KEY_LAST_APDU, "");
        if (apdu.isEmpty()) {
            Toast.makeText(this, "No APDU received yet", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("SALTO APDU", apdu));
        Toast.makeText(this, "APDU copied", Toast.LENGTH_SHORT).show();
    }
}
