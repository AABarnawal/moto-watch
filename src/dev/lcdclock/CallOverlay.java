package dev.lcdclock;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

/**
 * The CALL panel (CallView) as an accessibility overlay on the outside screen. While a call rings,
 * Motorola's own call screen sits in a system window above every app, CoverActivity included;
 * only an accessibility service's overlay goes over it. Shown while the call lasts, gone when it
 * ends or the phone opens. Needs ButtonService running; Calls falls back to CoverActivity without.
 */
final class CallOverlay {
    private static CallView view;
    private static WindowManager windows;
    private static DisplayManager displays;

    /** The phone opening: the call carries on inside. */
    private static final DisplayManager.DisplayListener innerScreen = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }

        @Override
        public void onDisplayChanged(int displayId) {
            if (displayId == Display.DEFAULT_DISPLAY && displays != null) {
                Display inner = displays.getDisplay(Display.DEFAULT_DISPLAY);
                if (inner != null && inner.getState() != Display.STATE_OFF) {
                    hide();
                }
            }
        }
    };

    private CallOverlay() {
    }

    static boolean isShowing() {
        return view != null;
    }

    /** Shows the panel over the outside screen; false if it can't (the service is off). */
    static boolean show() {
        return false;
    }

    /** Turns the outside screen on for the ring; the window's flag keeps it on from there. */
    @SuppressWarnings("deprecation")
    private static void wake(Context context) {
        PowerManager.WakeLock lock = context.getSystemService(PowerManager.class).newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "LcdClock:call");
        lock.acquire(3000);
    }

    static void hide() {
        if (view == null) {
            return;
        }
        try {
            windows.removeView(view);
        } catch (RuntimeException e) {
            Log.w(Binders.TAG, "call: can't remove the overlay", e);
        }
        displays.unregisterDisplayListener(innerScreen);
        view = null;
        Log.d(Binders.TAG, "call: overlay gone");
    }
}
