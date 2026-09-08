package com.mohammed.saltohceprobe;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView text = new TextView(this);
        text.setPadding(48, 64, 48, 64);
        text.setTextSize(18f);
        text.setText(
            "SALTO HCE Probe\n\n" +
            "1) Enable NFC.\n" +
            "2) Keep this app installed.\n" +
            "3) Run: adb logcat | findstr SALTO_PROBE\n" +
            "4) Hold the phone against the lock reader.\n\n" +
            "This app only logs APDUs sent to the registered HCE AID. " +
            "It does not attempt to unlock the door."
        );
        setContentView(text);
    }
}
