# Permissions Guide: Motorola Razr 40 External Display OS

This document explains every Android permission declared, why it is necessary, how it is granted, and its security profile.

---

## 1. Runtime Permissions (User Prompted)

| Permission | Purpose | How Granted | Fallback if Denied |
| :--- | :--- | :--- | :--- |
| `android.permission.RECORD_AUDIO` | Voice Recorder page and Gemini AI Assistant tap-to-talk speech recognition. | In-app dialog or `./install.sh` over ADB. | Voice Recorder and Assistant show a permission alert card. |
| `android.permission.POST_NOTIFICATIONS` | Timer ringing notifications and voice recorder ongoing foreground notification. | In-app dialog or `./install.sh` over ADB. | Alarm and Timer notifications do not post status alerts. |
| `android.permission.READ_CALENDAR` | Reading next 7 days of upcoming events for the Calendar cover page. | In-app dialog or `./install.sh` over ADB. | Calendar page displays "Calendar permission needed". |
| `android.permission.ACCESS_COARSE_LOCATION` | Determining city coordinates for Open-Meteo weather forecasts. | In-app dialog or `./install.sh` over ADB. | Weather page displays "Location permission needed". |
| `android.permission.READ_MEDIA_AUDIO` | Listing past voice recordings in the Recordings activity. | In-app dialog or `./install.sh` over ADB. | Recordings list shows only local app files. |

---

## 2. Privileged & ADB-Granted Permissions

| Permission | Purpose | How Granted | Fallback if Denied |
| :--- | :--- | :--- | :--- |
| `android.permission.WRITE_SECURE_SETTINGS` | 1. Toggling Battery Saver and Night Light in Quick Controls.<br>2. Enabling and disabling `ButtonService` on hinge fold/unfold (`ButtonPause`). | `adb shell pm grant dev.lcdclock android.permission.WRITE_SECURE_SETTINGS` | Battery saver/Night light tiles show setup link; Cover button remains permanently on or off. |
| `android.permission.PACKAGE_USAGE_STATS` | Checking when banking applications were last used to avoid triggering anti-malware RASP tripwires. | `adb shell appops set dev.lcdclock GET_USAGE_STATS allow` | Banking app detection relies only on current foreground activity. |
| `android.permission.DUMP` | Inspecting `ApplicationExitInfo` to verify when a banking app process has died after being swiped from Recents. | `adb shell pm grant dev.lcdclock android.permission.DUMP` | Banking app in Recents might cause `ButtonPause` to stay paused longer. |

---

## 3. Special Device Capabilities & Services

| Service / Capability | Component | Purpose |
| :--- | :--- | :--- |
| `BIND_ACCESSIBILITY_SERVICE` | `ButtonService` | Intercepting volume holds with phone folded and launching cover activities on external display. |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | `MediaListener` | Reading active media session playback controls and call style notifications. |
| `BIND_QUICK_SETTINGS_TILE` | `ButtonTile` | Quick Settings tile indicating cover button pause status. |
| `BIND_CLOCK_FACE_SERVICE` | `LcdClockService`, `LcdSessionService` | Proprietary Motorola Clock style plugin integration. |
| `READ_CLOCK_FACE_OPTIONS` | `LcdOptionsProvider` | Exposing color choices to Motorola's settings picker. |

---

## 4. Automated Permission Setup via ADB

The provided [`install.sh`](file:///Users/ankitkr/logi/razr-lcd-clock/install.sh) grants all runtime and privileged permissions automatically during installation:
```sh
adb shell pm grant dev.lcdclock android.permission.POST_NOTIFICATIONS
adb shell pm grant dev.lcdclock android.permission.RECORD_AUDIO
adb shell pm grant dev.lcdclock android.permission.READ_CALENDAR
adb shell pm grant dev.lcdclock android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant dev.lcdclock android.permission.READ_MEDIA_AUDIO
adb shell pm grant dev.lcdclock android.permission.WRITE_SECURE_SETTINGS
adb shell pm grant dev.lcdclock android.permission.DUMP
adb shell appops set dev.lcdclock GET_USAGE_STATS allow
```
