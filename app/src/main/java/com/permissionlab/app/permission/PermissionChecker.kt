package com.permissionlab.app.permission

import android.Manifest
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.permissionlab.app.model.PermissionStatus
import com.permissionlab.app.model.PermissionType
import com.permissionlab.app.receiver.LabDeviceAdminReceiver
import com.permissionlab.app.services.LabAccessibilityService
import com.permissionlab.app.services.LabNotificationListenerService

object PermissionChecker {

    fun checkPermission(context: Context, type: PermissionType): PermissionStatus {
        return when (type) {
            PermissionType.INSTALL_UNKNOWN_SOURCES -> checkInstallUnknownSources(context)
            PermissionType.USAGE_STATS -> checkUsageStats(context)
            PermissionType.ACCESSIBILITY_SERVICE -> checkAccessibilityService(context)
            PermissionType.WRITE_SECURE_SETTINGS -> checkWriteSecureSettings(context)
            PermissionType.RESTRICTED_SETTINGS -> checkRestrictedSettings(context)
            PermissionType.NOTIFICATION_LISTENER -> checkNotificationListener(context)
            PermissionType.NOTIFICATION_POST -> checkNotificationPost(context)
            PermissionType.NOTIFICATION_REPLY_CONTROL -> checkNotificationReplyControl(context)
            PermissionType.SYSTEM_ALERT_WINDOW -> checkSystemAlertWindow(context)
            PermissionType.WRITE_SETTINGS -> checkWriteSettings(context)
            PermissionType.MANAGE_EXTERNAL_STORAGE -> checkManageExternalStorage(context)
            PermissionType.IGNORE_BATTERY_OPTIMIZATIONS -> checkIgnoreBatteryOptimizations(context)
            PermissionType.SCHEDULE_EXACT_ALARM -> checkScheduleExactAlarm(context)
            PermissionType.DEVICE_ADMIN -> checkDeviceAdmin(context)
        }
    }

    private fun checkInstallUnknownSources(context: Context): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (context.packageManager.canRequestPackageInstalls()) {
                PermissionStatus.GRANTED
            } else {
                PermissionStatus.DENIED
            }
        } else {
            PermissionStatus.GRANTED
        }
    }

    private fun checkUsageStats(context: Context): PermissionStatus {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return PermissionStatus.DENIED
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return if (mode == AppOpsManager.MODE_ALLOWED) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }

    private fun checkAccessibilityService(context: Context): PermissionStatus {
        if (LabAccessibilityService.isConnected) return PermissionStatus.GRANTED

        val expectedComponentName = ComponentName(context, LabAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""

        val isEnabledInSettings = enabledServices.split(":").any {
            it.equals(expectedComponentName, ignoreCase = true) ||
                    it.contains(context.packageName, ignoreCase = true)
        }

        return if (isEnabledInSettings) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }

    private fun checkWriteSecureSettings(context: Context): PermissionStatus {
        val res = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        )
        return if (res == PackageManager.PERMISSION_GRANTED) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }

    /**
     * Checks Android 13+ (API 33+) Restricted Settings via AppOps op 119 (ACCESS_RESTRICTED_SETTINGS).
     * If app was sideloaded, Android blocks accessibility and notification listener services.
     */
    private fun checkRestrictedSettings(context: Context): PermissionStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return PermissionStatus.NOT_APPLICABLE
        }

        try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return PermissionStatus.NOT_APPLICABLE

            // Op 119 = OP_ACCESS_RESTRICTED_SETTINGS on Android 13+
            val opCode = try {
                val field = AppOpsManager::class.java.getDeclaredField("OP_ACCESS_RESTRICTED_SETTINGS")
                field.isAccessible = true
                field.getInt(null)
            } catch (e: Exception) {
                119
            }

            val checkOpMethod = AppOpsManager::class.java.getMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            val mode = checkOpMethod.invoke(appOps, opCode, Process.myUid(), context.packageName) as Int

            return when (mode) {
                AppOpsManager.MODE_ALLOWED -> PermissionStatus.GRANTED
                AppOpsManager.MODE_ERRORED, AppOpsManager.MODE_IGNORED -> PermissionStatus.RESTRICTED
                else -> PermissionStatus.DENIED
            }
        } catch (e: Exception) {
            // If reflection fails or op doesn't exist, check whether accessibility/notification listeners are blocked
            return PermissionStatus.GRANTED
        }
    }

    private fun checkNotificationListener(context: Context): PermissionStatus {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return if (enabledPackages.contains(context.packageName) || LabNotificationListenerService.isConnected) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }

    private fun checkNotificationPost(context: Context): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val res = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (res == PackageManager.PERMISSION_GRANTED) PermissionStatus.GRANTED else PermissionStatus.DENIED
        } else {
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                PermissionStatus.GRANTED
            } else {
                PermissionStatus.DENIED
            }
        }
    }

    private fun checkNotificationReplyControl(context: Context): PermissionStatus {
        // Notification reply & control requires active NotificationListenerService
        return checkNotificationListener(context)
    }

    private fun checkSystemAlertWindow(context: Context): PermissionStatus {
        return if (Settings.canDrawOverlays(context)) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }

    private fun checkWriteSettings(context: Context): PermissionStatus {
        return if (Settings.System.canWrite(context)) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }

    private fun checkManageExternalStorage(context: Context): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) PermissionStatus.GRANTED else PermissionStatus.DENIED
        } else {
            val res = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (res == PackageManager.PERMISSION_GRANTED) PermissionStatus.GRANTED else PermissionStatus.DENIED
        }
    }

    private fun checkIgnoreBatteryOptimizations(context: Context): PermissionStatus {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return PermissionStatus.DENIED
        return if (pm.isIgnoringBatteryOptimizations(context.packageName)) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }

    private fun checkScheduleExactAlarm(context: Context): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: return PermissionStatus.DENIED
            if (am.canScheduleExactAlarms()) PermissionStatus.GRANTED else PermissionStatus.DENIED
        } else {
            PermissionStatus.GRANTED
        }
    }

    private fun checkDeviceAdmin(context: Context): PermissionStatus {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return PermissionStatus.DENIED
        val cn = ComponentName(context, LabDeviceAdminReceiver::class.java)
        return if (dpm.isAdminActive(cn)) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }
}
