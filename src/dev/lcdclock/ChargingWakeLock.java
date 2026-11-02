package dev.lcdclock;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

/**
 * Manages a screen wake lock to keep the Motorola Razr 40 external display
 * continuously awake while charging, even when rendered inside SurfaceControlViewHost.
 */
public final class ChargingWakeLock {
    private static final String TAG = "ChargingWakeLock";
    private static PowerManager.WakeLock wakeLock;

    private ChargingWakeLock() {
    }

    public static synchronized void update(Context context, boolean active) {
        try {
            if (active) {
                if (wakeLock == null) {
                    PowerManager pm = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
                    if (pm != null) {
                        wakeLock = pm.newWakeLock(
                                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                                "dev.lcdclock:charging_awake"
                        );
                        wakeLock.setReferenceCounted(false);
                    }
                }
                if (wakeLock != null && !wakeLock.isHeld()) {
                    wakeLock.acquire();
                    Log.d(TAG, "Screen wake lock acquired (keeping display awake while charging)");
                }
            } else {
                if (wakeLock != null && wakeLock.isHeld()) {
                    wakeLock.release();
                    Log.d(TAG, "Screen wake lock released");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to update wake lock state", e);
        }
    }
}
