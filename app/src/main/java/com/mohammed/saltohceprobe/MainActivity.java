package com.mohammed.saltohceprobe;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private TextView nfcStatus, observeStatus, status, details;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private NfcAdapter adapter;
    private CardEmulation cardEmulation;
    private ComponentName service;
    private boolean preferredSet = false;
    private boolean observeEnabled = false;

    private final Runnable refresher = new Runnable() {
        @Override public void run() {
            refreshStatus();
            handler.postDelayed(this, 300);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter != null) {
            cardEmulation = CardEmulation.getInstance(adapter);
            service = new ComponentName(this, ProbeService.class);
        }

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(44, 56, 44, 56);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("SALTO HCE Probe V3");
        title.setTextSize(27f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("\nPassive polling-loop + HCE probe. Phone only.\n");
        subtitle.setTextSize(16f);
        root.addView(subtitle);

        nfcStatus = new TextView(this);
        nfcStatus.setTextSize(17f);
        nfcStatus.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(nfcStatus);

        observeStatus = new TextView(this);
        observeStatus.setTextSize(15f);
        root.addView(observeStatus);

        TextView steps = new TextView(this);
        steps.setText(
                "\n1. Turn NFC on and keep this app in the foreground.\n" +
                "2. Press CLEAR RESULT.\n" +
                "3. Hold the phone on the black SALTO reader for 5–10 seconds.\n" +
                "4. If the lock gives the short red pulse, look for POLLING DETECTED below.\n"
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
        details.setTextSize(14f);
        details.setTypeface(Typeface.MONOSPACE);
        details.setTextIsSelectable(true);
        root.addView(details);

        Button copy = new Button(this);
        copy.setText("Copy last event");
        copy.setOnClickListener(v -> copyLastEvent());
        root.addView(copy);

        Button clear = new Button(this);
        clear.setText("Clear result");
        clear.setOnClickListener(v -> {
            getSharedPreferences(ProbeService.PREFS, MODE_PRIVATE).edit().clear().apply();
            refreshStatus();
        });
        root.addView(clear);

        TextView note = new TextView(this);
        note.setText("\nThis build is passive. When Observe Mode is active, Android listens to the reader's NFC polling loop and does not allow the NFC transaction to continue, so an APDU count of 0 is expected. If Observe Mode is unavailable, the app still probes the registered SALTO AID/prefix and intentionally rejects received APDUs.");
        note.setTextSize(13f);
        root.addView(note);

        setContentView(scroll);
    }

    @Override protected void onResume() {
        super.onResume();
        configureProbe();
        handler.post(refresher);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(refresher);
        if (adapter != null && Build.VERSION.SDK_INT >= 35 && observeEnabled) {
            try { adapter.setObserveModeEnabled(false); } catch (Throwable ignored) {}
        }
        if (cardEmulation != null && preferredSet) {
            try { cardEmulation.unsetPreferredService(this); } catch (Throwable ignored) {}
        }
        preferredSet = false;
        observeEnabled = false;
        super.onPause();
    }

    private void configureProbe() {
        if (adapter == null || cardEmulation == null || service == null) return;
        try {
            preferredSet = cardEmulation.setPreferredService(this, service);
        } catch (Throwable t) {
            preferredSet = false;
        }

        // Broaden AID routing to SALTO's registered RID when the device supports prefix AIDs.
        try {
            List<String> aids = new ArrayList<>();
            aids.add("A000000743CC843413925E20C59B0100");
            if (cardEmulation.supportsAidPrefixRegistration()) {
                aids.add("A000000743*");
            }
            cardEmulation.registerAidsForService(service, CardEmulation.CATEGORY_OTHER, aids);
        } catch (Throwable ignored) {}

        if (Build.VERSION.SDK_INT >= 35) {
            try {
                if (adapter.isObserveModeSupported() && preferredSet) {
                    observeEnabled = adapter.setObserveModeEnabled(true);
                }
            } catch (Throwable t) {
                observeEnabled = false;
            }
        }
    }

    private void refreshStatus() {
        if (adapter == null) {
            nfcStatus.setText("NFC: NOT SUPPORTED");
        } else if (!adapter.isEnabled()) {
            nfcStatus.setText("NFC: OFF — turn it on first");
        } else {
            nfcStatus.setText("NFC: ON ✓");
        }

        String observe;
        if (Build.VERSION.SDK_INT < 35) {
            observe = "Observe Mode: unavailable (Android 15+ required)";
        } else if (adapter == null) {
            observe = "Observe Mode: unavailable";
        } else {
            boolean supported = false;
            try { supported = adapter.isObserveModeSupported(); } catch (Throwable ignored) {}
            observe = "Preferred HCE service: " + (preferredSet ? "YES ✓" : "NO") +
                    "\nObserve Mode supported: " + (supported ? "YES ✓" : "NO") +
                    "\nObserve Mode active: " + (observeEnabled ? "YES ✓" : "NO");
        }
        observeStatus.setText(observe);

        SharedPreferences prefs = getSharedPreferences(ProbeService.PREFS, MODE_PRIVATE);
        int apduCount = prefs.getInt(ProbeService.KEY_APDU_COUNT, 0);
        int pollCount = prefs.getInt(ProbeService.KEY_POLL_COUNT, 0);
        int eventCount = prefs.getInt(ProbeService.KEY_EVENT_COUNT, 0);
        String event = prefs.getString(ProbeService.KEY_LAST_EVENT, "");
        String poll = prefs.getString(ProbeService.KEY_LAST_POLL, "");
        String apdu = prefs.getString(ProbeService.KEY_LAST_APDU, "");
        String time = prefs.getString(ProbeService.KEY_LAST_TIME, "");

        if (pollCount > 0) {
            status.setText("NFC POLLING DETECTED ✓");
        } else if (apduCount > 0) {
            status.setText("HCE APDU DETECTED ✓");
        } else {
            status.setText("WAITING — NO POLLING/APDU YET");
        }

        details.setText(
                "Polling frames: " + pollCount + "\n" +
                "APDUs: " + apduCount + "\n" +
                "Events: " + eventCount + "\n" +
                "Last time: " + (time.isEmpty() ? "-" : time) + "\n\n" +
                "Last polling frame:\n" + (poll.isEmpty() ? "-" : poll) + "\n\n" +
                "Last APDU:\n" + (apdu.isEmpty() ? "-" : apdu) + "\n\n" +
                "Last event:\n" + (event.isEmpty() ? "-" : event)
        );
    }

    private void copyLastEvent() {
        SharedPreferences prefs = getSharedPreferences(ProbeService.PREFS, MODE_PRIVATE);
        String event = prefs.getString(ProbeService.KEY_LAST_EVENT, "");
        if (event.isEmpty()) {
            Toast.makeText(this, "No event received yet", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("SALTO NFC event", event));
        Toast.makeText(this, "Last event copied", Toast.LENGTH_SHORT).show();
    }
}
