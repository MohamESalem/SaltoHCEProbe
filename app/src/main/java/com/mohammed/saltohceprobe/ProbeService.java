package com.mohammed.saltohceprobe;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

public class ProbeService extends HostApduService {
    private static final String TAG = "SALTO_PROBE";

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        StringBuilder sb = new StringBuilder();
        for (byte b : commandApdu) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        Log.i(TAG, "RX: " + sb);

        // 6A82 = File/application not found.
        // We intentionally reject the request: this is a passive probe only.
        return new byte[] {(byte) 0x6A, (byte) 0x82};
    }

    @Override
    public void onDeactivated(int reason) {
        Log.i(TAG, "Deactivated: " + reason);
    }
}
