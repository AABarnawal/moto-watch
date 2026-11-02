package dev.lcdclock;

import android.Manifest;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.util.Base64;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

/**
 * What the quick controls panel can switch, and how: the flashlight, an SOS signal with it
 * (SosService), battery saver and Night Light (system settings, once Android's "write secure
 * settings" permission has been given from a computer: canWriteSecure), Digital Wellbeing's Bedtime
 * mode (through Wellbeing's provider), Do Not Disturb (allowed to
 * apps with notification access), the ring mode, media volume, the Home Assistant buttons, a
 * WireGuard tunnel through the WG Tunnel app's remote control, and leaving for Motorola's own
 * screen. Wi-Fi, mobile data and Bluetooth aren't here: Android doesn't let apps switch them.
 * The Home Assistant buttons and WireGuard work only while the phone is unlocked (Lock).
 */
final class QuickControls {
    static final String WIFI = "wifi";
    static final String BLUETOOTH = "bt";
    static final String HOTSPOT = "hotspot";
    static final String AIRPLANE = "airplane";
    static final String TORCH = "torch";
    static final String SOS = "sos";
    static final String DND = "dnd";
    static final String SOUND = "sound";
    static final String VOLUME = "volume";
    static final String SAVER = "saver";
    static final String NIGHT = "night";
    static final String BEDTIME = "bedtime";
    static final String HOME_PREFIX = "ha";
    static final String WIREGUARD = "wireguard";
    static final String MOTOROLA = "moto";
    static String name(Context context, String control) {
        int slot = homeSlot(control);
        if (slot >= 0) {
            String[] slots = context.getResources().getStringArray(R.array.home_slots);
            return slot < slots.length ? context.getString(R.string.quick_home, slots[slot]) : control;
        }
        if (WIFI.equals(control)) return "Wi-Fi";
        if (BLUETOOTH.equals(control)) return "Bluetooth";
        if (HOTSPOT.equals(control)) return "Personal Hotspot";
        if (AIRPLANE.equals(control)) return "Airplane Mode";
        if (TORCH.equals(control)) return "Flashlight";
        if (SOS.equals(control)) return "SOS signal (flashlight Morse code)";
        if (DND.equals(control)) return "Do Not Disturb";
        if (SOUND.equals(control)) return "Sound mode (ring, vibrate, silent)";
        if (VOLUME.equals(control)) return "Media volume";
        if (SAVER.equals(control)) return "Battery saver";
        if (NIGHT.equals(control)) return "Night Light";
        if (BEDTIME.equals(control)) return "Bedtime mode (Digital Wellbeing)";
        if (WIREGUARD.equals(control)) return "WireGuard (WG Tunnel)";
        if (MOTOROLA.equals(control)) return "Motorola's own screen";
        return control;
    }

    /** Every control in its first order. */
    static final List<String> ALL = Arrays.asList(WIFI, BLUETOOTH, TORCH, HOTSPOT, AIRPLANE, SOS, DND, SOUND, VOLUME, SAVER, NIGHT, BEDTIME, HOME_PREFIX + 0, HOME_PREFIX + 1,
            HOME_PREFIX + 2, WIREGUARD, MOTOROLA);

    private static final String WELLBEING = "com.google.android.apps.wellbeing";
    private static final String WG_PACKAGE = "com.zaneschepke.wireguardautotunnel";
    private static final ComponentName WG_REMOTE = new ComponentName(WG_PACKAGE,
            WG_PACKAGE + ".core.broadcast.RemoteControlReceiver");
    /** In wireGuardSecrets. */
    private static final String WG_KEY = "key";
    private static final String WG_SIGNER = "signer";

    private static String torchCamera;
    private static boolean torchOn;
    private static boolean torchWatched;

    private QuickControls() {
    }

    /** A Home Assistant button's slot, or -1. */
    static int homeSlot(String control) {
        return control.startsWith(HOME_PREFIX) ? control.charAt(HOME_PREFIX.length()) - '0' : -1;
    }

    /** The Home Assistant buttons and the VPN switch work only while the phone is unlocked. */
    static boolean needsUnlock(String control) {
        return homeSlot(control) >= 0 || WIREGUARD.equals(control);
    }

    /** Whether a control can be shown: Home Assistant buttons once set up, WireGuard once configured. */
    static boolean usable(Context context, String control) {
        int slot = homeSlot(control);
        if (slot >= 0) {
            return HomeAssistant.isConfigured(context, slot);
        }
        if (SAVER.equals(control) || NIGHT.equals(control)) {
            return canWriteSecure(context);
        }
        if (BEDTIME.equals(control)) {
            return context.getPackageManager().resolveContentProvider(WELLBEING_API.getAuthority(), 0) != null;
        }
        if (WIREGUARD.equals(control)) {
            return !Prefs.get(context).getString(Prefs.WG_TUNNEL, "").isEmpty() && !wgKey(context).isEmpty();
        }
        return true;
    }

    // ---- flashlight ----

    /** The flashlight is on as a light (not blinking SOS). */
    static boolean torchOn(Context context) {
        watchTorch(context);
        return torchOn && !SosService.isRunning();
    }

    /** The camera with the flashlight, or null. */
    static String torchCamera(Context context) {
        watchTorch(context);
        return torchCamera;
    }

    private static void watchTorch(Context context) {
        if (torchWatched) {
            return;
        }
        torchWatched = true;
        CameraManager cameras = context.getSystemService(CameraManager.class);
        try {
            for (String id : cameras.getCameraIdList()) {
                Boolean flash = cameras.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (flash != null && flash) {
                    torchCamera = id;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            Log.w(Binders.TAG, "no flashlight", e);
        }
        cameras.registerTorchCallback(new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(String cameraId, boolean enabled) {
                if (cameraId.equals(torchCamera)) {
                    torchOn = enabled; // the panel polls this
                }
            }
        }, null);
    }

    /** On or off; while SOS is signalling, stops it and leaves the light on. */
    static void toggleTorch(Context context) {
        watchTorch(context);
        if (torchCamera == null) {
            return;
        }
        if (SosService.isRunning()) {
            SosService.stop(context, true);
            return;
        }
        try {
            context.getSystemService(CameraManager.class).setTorchMode(torchCamera, !torchOn);
        } catch (CameraAccessException | IllegalArgumentException e) {
            Log.w(Binders.TAG, "can't switch the flashlight", e);
        }
    }

    // ---- battery saver and Night Light ----

    /**
     * Android lets apps switch battery saver and Night Light only with "write secure settings",
     * which can't be asked for in the app; it's given once from a computer:
     * adb shell pm grant dev.lcdclock android.permission.WRITE_SECURE_SETTINGS
     */
    static boolean canWriteSecure(Context context) {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Battery saver is switched on: its setting, which stays on while charging even though Android
     * holds battery saver back until the phone is unplugged.
     */
    static boolean saverOn(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), "low_power") != 0;
        } catch (Settings.SettingNotFoundException | SecurityException e) {
            return context.getSystemService(PowerManager.class).isPowerSaveMode();
        }
    }

    static void toggleSaver(Context context) {
        putSetting(context, true, "low_power", !saverOn(context));
    }

    static boolean nightOn(Context context) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(), "night_display_activated", 0) != 0;
        } catch (SecurityException e) {
            return false;
        }
    }

    static void toggleNight(Context context) {
        putSetting(context, false, "night_display_activated", !nightOn(context));
    }

    /** A system setting that turns a feature on or off; Global when `global`, else Secure. */
    private static void putSetting(Context context, boolean global, String name, boolean on) {
        try {
            if (global) {
                Settings.Global.putInt(context.getContentResolver(), name, on ? 1 : 0);
            } else {
                Settings.Secure.putInt(context.getContentResolver(), name, on ? 1 : 0);
            }
        } catch (SecurityException e) {
            Log.w(Binders.TAG, "can't switch " + name + ": no write secure settings permission", e);
        }
    }

    // ---- Digital Wellbeing's Bedtime mode ----

    /**
     * Wellbeing's provider: get_wind_down_status (state 1 = on, 0 = off, 2 = paused) and
     * update_wind_down (state 0 off, 1 on) are open to other apps, unlike its other Bedtime calls.
     */
    private static final Uri WELLBEING_API = Uri.parse("content://com.google.android.apps.wellbeing.api");
    private static final long BEDTIME_FRESH_MS = 2000;
    private static boolean bedtimeOn;
    private static long bedtimeCheckedAt;

    /** Bedtime mode is on (checked with Wellbeing at most every two seconds). */
    static boolean bedtimeOn(Context context) {
        long now = SystemClock.elapsedRealtime();
        if (now - bedtimeCheckedAt > BEDTIME_FRESH_MS || bedtimeCheckedAt == 0) {
            bedtimeCheckedAt = now;
            Bundle status = wellbeing(context, "get_wind_down_status", null);
            bedtimeOn = status != null && status.getInt("state", 0) == 1;
        }
        return bedtimeOn;
    }

    /** Turns Bedtime mode off if it's on, else on (also from paused). */
    static void toggleBedtime(Context context) {
        boolean on = !bedtimeOn(context);
        Bundle args = new Bundle();
        args.putInt("state", on ? 1 : 0);
        Bundle result = wellbeing(context, "update_wind_down", args);
        if (result != null && result.getInt("result", -1) == 1) {
            bedtimeOn = on;
            bedtimeCheckedAt = SystemClock.elapsedRealtime();
            Log.d(Binders.TAG, "bedtime: " + (on ? "on" : "off"));
        } else {
            Log.w(Binders.TAG, "bedtime: Wellbeing refused " + result);
        }
    }

    private static Bundle wellbeing(Context context, String method, Bundle args) {
        try {
            return context.getContentResolver().call(WELLBEING_API, method, null, args);
        } catch (RuntimeException e) {
            Log.w(Binders.TAG, "bedtime: can't reach Wellbeing", e);
            return null;
        }
    }

    // ---- SOS ----

    static void toggleSos(Context context) {
        if (SosService.isRunning()) {
            SosService.stop(context, false);
        } else {
            SosService.start(context);
        }
    }

    // ---- Do Not Disturb and ring mode ----

    static boolean dndOn(Context context) {
        return context.getSystemService(NotificationManager.class).getCurrentInterruptionFilter()
                > NotificationManager.INTERRUPTION_FILTER_ALL;
    }

    static void toggleDnd(Context context) {
        int filter = dndOn(context) ? NotificationManager.INTERRUPTION_FILTER_ALL
                : NotificationManager.INTERRUPTION_FILTER_PRIORITY;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm.isNotificationPolicyAccessGranted()) {
            nm.setInterruptionFilter(filter);
        } else if (MediaListener.connected != null) {
            MediaListener.connected.requestInterruptionFilter(filter == NotificationManager.INTERRUPTION_FILTER_ALL
                    ? NotificationListenerService.INTERRUPTION_FILTER_ALL
                    : NotificationListenerService.INTERRUPTION_FILTER_PRIORITY);
        }
    }

    static int ringMode(Context context) {
        return context.getSystemService(AudioManager.class).getRingerMode();
    }

    /** Ring → vibrate → silent → ring. */
    static void nextRingMode(Context context) {
        AudioManager audio = context.getSystemService(AudioManager.class);
        int next = audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL ? AudioManager.RINGER_MODE_VIBRATE
                : audio.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE ? AudioManager.RINGER_MODE_SILENT
                : AudioManager.RINGER_MODE_NORMAL;
        try {
            audio.setRingerMode(next);
        } catch (SecurityException e) {
            // Silent may need Do Not Disturb access; skip it.
            audio.setRingerMode(next == AudioManager.RINGER_MODE_SILENT ? AudioManager.RINGER_MODE_NORMAL : next);
        }
    }

    // ---- media volume ----

    static int volume(Context context) {
        return context.getSystemService(AudioManager.class).getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    static void adjustVolume(Context context, boolean up) {
        context.getSystemService(AudioManager.class).adjustStreamVolume(AudioManager.STREAM_MUSIC,
                up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, 0);
    }

    // ---- WireGuard (WG Tunnel's remote control) ----

    static boolean vpnOn(Context context) {
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        for (Network network : cm.getAllNetworks()) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return true;
            }
        }
        return false;
    }

    /**
     * WG Tunnel's key, and the signing certificate of the WG Tunnel it was saved for, in
     * credential-encrypted storage: unlike Prefs, it can't be read before the first unlock.
     */
    private static SharedPreferences wireGuardSecrets(Context context) {
        return context.getSharedPreferences("wireguard", Context.MODE_PRIVATE);
    }

    /** WG Tunnel's remote control key, or "" before it's set. */
    static String wgKey(Context context) {
        SharedPreferences secrets = wireGuardSecrets(context);
        SharedPreferences old = Prefs.get(context);
        if (old.contains(Prefs.WG_KEY)) {
            if (!secrets.contains(WG_KEY)) {
                secrets.edit().putString(WG_KEY, old.getString(Prefs.WG_KEY, "")).commit();
            }
            old.edit().remove(Prefs.WG_KEY).apply();
        }
        return secrets.getString(WG_KEY, "");
    }

    /** Saves the tunnel and key, for the WG Tunnel installed now. */
    static void setWireGuard(Context context, String tunnel, String key) {
        Prefs.get(context).edit().putString(Prefs.WG_TUNNEL, tunnel).remove(Prefs.WG_KEY).apply();
        wireGuardSecrets(context).edit().putString(WG_KEY, key).putString(WG_SIGNER, wgSigner(context)).apply();
    }

    /** The installed WG Tunnel's signing certificate (SHA-256, Base64), or "" if it isn't installed. */
    private static String wgSigner(Context context) {
        try {
            Signature cert = context.getPackageManager()
                    .getPackageInfo(WG_PACKAGE, PackageManager.GET_SIGNING_CERTIFICATES)
                    .signingInfo.getApkContentsSigners()[0];
            return Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(cert.toByteArray()),
                    Base64.NO_WRAP);
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            return "";
        }
    }

    /**
     * Whether the installed WG Tunnel is signed like the one the key was saved for, so another app
     * under its package name never gets the key. A key saved while WG Tunnel wasn't installed
     * trusts the first WG Tunnel it goes to.
     */
    private static boolean wgTrusted(Context context) {
        SharedPreferences secrets = wireGuardSecrets(context);
        String signer = secrets.getString(WG_SIGNER, "");
        if (signer.isEmpty()) {
            signer = wgSigner(context);
            if (signer.isEmpty()) {
                return false;
            }
            secrets.edit().putString(WG_SIGNER, signer).apply();
        }
        return context.getPackageManager().hasSigningCertificate(WG_PACKAGE,
                Base64.decode(signer, Base64.NO_WRAP), PackageManager.CERT_INPUT_SHA256);
    }

    static void toggleWireGuard(Context context) {
        if (Lock.locked(context)) {
            Log.d(Binders.TAG, "wireguard: locked, not switching");
            return;
        }
        if (!wgTrusted(context)) {
            Log.w(Binders.TAG, "wireguard: WG Tunnel is missing or signed differently since the key was saved,"
                    + " not sending it");
            return;
        }
        String action = WG_PACKAGE + (vpnOn(context) ? ".STOP_TUNNEL" : ".START_TUNNEL");
        context.sendBroadcast(new Intent(action).setComponent(WG_REMOTE)
                .putExtra("key", wgKey(context))
                .putExtra("tunnelName", Prefs.get(context).getString(Prefs.WG_TUNNEL, "")));
    }

    // ---- Network Controls (Wi-Fi, Bluetooth, Airplane, Hotspot) ----

    static boolean wifiOn(Context context) {
        try {
            ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
            if (cm != null) {
                Network active = cm.getActiveNetwork();
                if (active != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(active);
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return true;
                    }
                }
            }
            android.net.wifi.WifiManager wm = context.getSystemService(android.net.wifi.WifiManager.class);
            return wm != null && wm.isWifiEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    static void toggleWifi(Context context) {
        try {
            Intent panel = new Intent(Settings.Panel.ACTION_WIFI);
            panel.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            android.app.ActivityOptions opts = android.app.ActivityOptions.makeBasic();
            opts.setLaunchDisplayId(CoverScreen.DISPLAY_ID);
            context.startActivity(panel, opts.toBundle());
        } catch (Exception e) {
            try {
                Intent panel = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
                panel.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                android.app.ActivityOptions opts = android.app.ActivityOptions.makeBasic();
                opts.setLaunchDisplayId(CoverScreen.DISPLAY_ID);
                context.startActivity(panel, opts.toBundle());
            } catch (Exception ex) {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }

    static boolean bluetoothOn(Context context) {
        try {
            android.bluetooth.BluetoothAdapter ba = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
            return ba != null && ba.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    static void toggleBluetooth(Context context) {
        try {
            android.bluetooth.BluetoothAdapter ba = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
            if (ba != null) {
                if (ba.isEnabled()) {
                    ba.disable();
                } else {
                    ba.enable();
                }
            }
        } catch (Exception e) {
            try {
                Intent panel = new Intent("android.settings.panel.action.BLUETOOTH");
                panel.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                android.app.ActivityOptions opts = android.app.ActivityOptions.makeBasic();
                opts.setLaunchDisplayId(CoverScreen.DISPLAY_ID);
                context.startActivity(panel, opts.toBundle());
            } catch (Exception ex) {
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }

    static boolean airplaneOn(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        } catch (Exception e) {
            return false;
        }
    }

    static void toggleAirplane(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            android.app.ActivityOptions opts = android.app.ActivityOptions.makeBasic();
            opts.setLaunchDisplayId(CoverScreen.DISPLAY_ID);
            context.startActivity(intent, opts.toBundle());
        } catch (Exception e) {
            Log.w(Binders.TAG, "can't open airplane settings", e);
        }
    }

    static boolean hotspotOn(Context context) {
        return false;
    }

    static void toggleHotspot(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            android.app.ActivityOptions opts = android.app.ActivityOptions.makeBasic();
            opts.setLaunchDisplayId(CoverScreen.DISPLAY_ID);
            context.startActivity(intent, opts.toBundle());
        } catch (Exception e) {
            Log.w(Binders.TAG, "can't open hotspot settings", e);
        }
    }
}
