# PermissionLab: Android Privileged & Special Permissions Tester

A comprehensive, production-grade Android diagnostic application designed to check, request, test, and verify all special, restricted, and privileged Android permissions across Android 8.0 (API 26) through Android 14/15 (API 34+).

---

## 📱 Features & Permissions Tested

| # | Permission / Feature | API Level | Check Method | Request Mechanism | Live Functional Test |
|---|---|---|---|---|---|
| 1 | **Install Unknown Sources** | 26+ | `PackageManager.canRequestPackageInstalls()` | `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` | Tests APK package installation dispatch |
| 2 | **App Usage Data / Stats** | 21+ | `AppOpsManager.unsafeCheckOpNoThrow(OPSTR_GET_USAGE_STATS)` | `Settings.ACTION_USAGE_ACCESS_SETTINGS` | Queries top 3 used applications and screen time for past 24h |
| 3 | **Accessibility Settings & Service** | All | `AccessibilityManager` & Settings check | `Settings.ACTION_ACCESSIBILITY_SETTINGS` | Verifies active service binding and tests `dispatchGesture` tap simulation |
| 4 | **Write Secure Settings** | All | `context.checkSelfPermission(WRITE_SECURE_SETTINGS)` | ADB Shell or Superuser (`su`) | Tests writing and reading back `Settings.Secure.putString(...)` |
| 5 | **Restricted Settings (Android 13+)** | 33+ | AppOps op 119 (`ACCESS_RESTRICTED_SETTINGS`) | App Details ⋮ Menu or ADB bypass | Detects whether OS blocks accessibility/notification listener for sideloaded app |
| 6 | **Notification Read & Listen** | 18+ | `NotificationManagerCompat.getEnabledListenerPackages()` | `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` | Live reads status bar notifications, package names, titles, and text |
| 7 | **Notification Reply & Dismiss Control** | 20+ | Active listener service status | `NotificationListenerService` internal API | Automatically extracts `RemoteInput` and sends an inline reply or dismisses notification |
| 8 | **Post Notifications** | 33+ | `ContextCompat.checkSelfPermission(POST_NOTIFICATIONS)` | Runtime dialog / App notifications | Posts test notification with Direct Reply input field |
| 9 | **Overlay / Draw Over Other Apps** | 23+ | `Settings.canDrawOverlays(context)` | `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` | Spawns a floating banner overlay on screen for 4 seconds |
| 10 | **Modify System Settings** | 23+ | `Settings.System.canWrite(context)` | `Settings.ACTION_MANAGE_WRITE_SETTINGS` | Reads and writes `Settings.System.SCREEN_BRIGHTNESS_MODE` |
| 11 | **All Files Access** | 30+ | `Environment.isExternalStorageManager()` | `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` | Verifies full directory traversal in `/sdcard` |
| 12 | **Battery Optimization Whitelist** | 23+ | `PowerManager.isIgnoringBatteryOptimizations()` | `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Confirms app immunity from Doze mode |
| 13 | **Schedule Exact Alarms** | 31+ | `AlarmManager.canScheduleExactAlarms()` | `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` | Schedules 3-second test alarm with broadcast receiver callback |
| 14 | **Device Administration** | All | `DevicePolicyManager.isAdminActive()` | `DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN` | Tests device admin receiver binding |

---

## 🛠️ Project Structure

```
PermissionLab/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/permissionlab/app/
│       │   ├── MainActivity.kt
│       │   ├── model/
│       │   │   ├── PermissionType.kt
│       │   │   ├── PermissionStatus.kt
│       │   │   └── PermissionItem.kt
│       │   ├── permission/
│       │   │   ├── PermissionChecker.kt
│       │   │   ├── PermissionRequester.kt
│       │   │   └── PermissionTester.kt
│       │   ├── services/
│       │   │   ├── LabAccessibilityService.kt
│       │   │   └── LabNotificationListenerService.kt
│       │   ├── receiver/
│       │   │   ├── LabNotificationReplyReceiver.kt
│       │   │   ├── LabAlarmReceiver.kt
│       │   │   └── LabDeviceAdminReceiver.kt
│       │   └── ui/
│       │       └── PermissionAdapter.kt
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   └── item_permission_card.xml
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── xml/
│               ├── accessibility_service_config.xml
│               └── device_admin.xml
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 💻 Privileged / ADB Shell Commands

For permissions that cannot be granted via standard user prompts:

### 1. Grant Write Secure Settings
```bash
adb shell pm grant com.permissionlab.app android.permission.WRITE_SECURE_SETTINGS
```

### 2. Unblock Android 13+ Restricted Settings
```bash
adb shell appops set com.permissionlab.app ACCESS_RESTRICTED_SETTINGS allow
```

### 3. Grant Usage Stats directly
```bash
adb shell appops set com.permissionlab.app GET_USAGE_STATS allow
```

### 4. Enable Notification Listener via Shell
```bash
adb shell cmd notification allow_listener com.permissionlab.app/com.permissionlab.app.services.LabNotificationListenerService
```

### 5. Enable Accessibility Service via Shell
```bash
adb shell settings put secure enabled_accessibility_services com.permissionlab.app/com.permissionlab.app.services.LabAccessibilityService
```

---

## 🚀 Building the Project

Open the `PermissionLab` directory in Android Studio or compile with Gradle:
```bash
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`
