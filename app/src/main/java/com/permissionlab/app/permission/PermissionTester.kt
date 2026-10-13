package com.permissionlab.app.permission

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.permissionlab.app.R
import com.permissionlab.app.model.PermissionType
import com.permissionlab.app.receiver.LabAlarmReceiver
import com.permissionlab.app.receiver.LabNotificationReplyReceiver
import com.permissionlab.app.services.LabAccessibilityService
import com.permissionlab.app.services.LabNotificationListenerService
import java.io.File
import java.util.Calendar

object PermissionTester {

    const val CHANNEL_ID = "permission_lab_test_channel"
    const val NOTIF_ID = 1001

    fun runTest(context: Context, type: PermissionType, onResult: (String) -> Unit) {
        when (type) {
            PermissionType.INSTALL_UNKNOWN_SOURCES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val canInstall = context.packageManager.canRequestPackageInstalls()
                    if (canInstall) {
                        onResult("[SUCCESS] App has permission to install packages directly from unknown sources.")
                    } else {
                        onResult("[FAILED] Permission denied. Enable in Settings -> Install Unknown Apps.")
                    }
                } else {
                    onResult("[SUCCESS] Permission implicitly granted on Android < 8.0.")
                }
            }

            PermissionType.USAGE_STATS -> {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                if (usm == null) {
                    onResult("[FAILED] UsageStatsManager unavailable.")
                    return
                }

                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                val stats = usm.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    cal.timeInMillis,
                    System.currentTimeMillis()
                )

                if (stats.isNullOrEmpty()) {
                    onResult("[FAILED] No usage data returned. Please grant Usage Access in Settings.")
                } else {
                    val top3 = stats.sortedByDescending { it.totalTimeInForeground }
                        .take(3)
                        .joinToString("\n") {
                            "• ${it.packageName.substringAfterLast('.')}: ${it.totalTimeInForeground / 1000}s"
                        }
                    onResult("[SUCCESS] Usage Stats retrieved successfully!\nTop apps:\n$top3")
                }
            }

            PermissionType.ACCESSIBILITY_SERVICE -> {
                if (LabAccessibilityService.isConnected) {
                    val service = LabAccessibilityService.instance
                    val eventInfo = LabAccessibilityService.lastEventSummary
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && service != null) {
                        service.testPerformClickGesture(200f, 200f) { success ->
                            val gestureStatus = if (success) "Gesture dispatch passed." else "Gesture dispatch cancelled."
                            onResult("[SUCCESS] Accessibility Service ACTIVE!\n$eventInfo\n$gestureStatus")
                        }
                    } else {
                        onResult("[SUCCESS] Accessibility Service ACTIVE!\n$eventInfo")
                    }
                } else {
                    onResult("[FAILED] LabAccessibilityService is NOT enabled or bound by the system.")
                }
            }

            PermissionType.WRITE_SECURE_SETTINGS -> {
                try {
                    val testVal = "test_${System.currentTimeMillis()}"
                    val success = Settings.Secure.putString(
                        context.contentResolver,
                        "permissionlab_test_key",
                        testVal
                    )
                    val readBack = Settings.Secure.getString(
                        context.contentResolver,
                        "permissionlab_test_key"
                    )
                    if (success && readBack == testVal) {
                        onResult("[SUCCESS] Successfully wrote and verified Secure Setting! (key: permissionlab_test_key)")
                    } else {
                        onResult("[FAILED] Write reported false or read mismatch.")
                    }
                } catch (e: SecurityException) {
                    onResult("[FAILED] SecurityException: Requires WRITE_SECURE_SETTINGS.\nGrant via ADB:\nadb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS")
                } catch (e: Exception) {
                    onResult("[FAILED] Error: ${e.message}")
                }
            }

            PermissionType.RESTRICTED_SETTINGS -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    onResult("[INFO] Restricted Settings feature was introduced in Android 13+. Not restricted on this device.")
                } else {
                    val status = PermissionChecker.checkPermission(context, PermissionType.RESTRICTED_SETTINGS)
                    if (status == com.permissionlab.app.model.PermissionStatus.RESTRICTED) {
                        onResult("[RESTRICTED] Android 13+ has restricted this sideloaded app from enabling Accessibility or Notification Listeners.\nFix: Go to App Info -> Tap ⋮ (top right) -> 'Allow restricted settings', or run:\nadb shell appops set ${context.packageName} ACCESS_RESTRICTED_SETTINGS allow")
                    } else {
                        onResult("[SUCCESS] Restricted settings are UNBLOCKED! App is allowed to configure sensitive services.")
                    }
                }
            }

            PermissionType.NOTIFICATION_POST -> {
                createNotificationChannel(context)
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val replyIntent = Intent(context, LabNotificationReplyReceiver::class.java).apply {
                    action = LabNotificationReplyReceiver.ACTION_REPLY
                }
                val replyPendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    replyIntent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val remoteInput = RemoteInput.Builder(LabNotificationReplyReceiver.KEY_TEXT_REPLY)
                    .setLabel("Type test reply...")
                    .build()

                val action = NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_send,
                    "Direct Reply",
                    replyPendingIntent
                ).addRemoteInput(remoteInput).build()

                val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("PermissionLab Test Notification")
                    .setContentText("Tap 'Direct Reply' to test inline reply capability.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .addAction(action)
                    .setAutoCancel(true)
                    .build()

                try {
                    nm.notify(NOTIF_ID, notif)
                    onResult("[SUCCESS] Test notification #$NOTIF_ID posted! Check your status bar to test Direct Reply.")
                } catch (e: Exception) {
                    onResult("[FAILED] Failed to post notification: ${e.message}")
                }
            }

            PermissionType.NOTIFICATION_LISTENER -> {
                val service = LabNotificationListenerService.instance
                if (service != null && LabNotificationListenerService.isConnected) {
                    val active = service.refreshActiveNotifications()
                    val details = active.take(3).joinToString("\n") {
                        "• [${it.packageName.substringAfterLast('.')}] ${it.title}: ${it.text.take(25)}"
                    }
                    onResult("[SUCCESS] Notification Listener ACTIVE! Found ${active.size} active notifications:\n$details")
                } else {
                    onResult("[FAILED] Notification Listener Service is not connected. Enable in Notification Access settings.")
                }
            }

            PermissionType.NOTIFICATION_REPLY_CONTROL -> {
                val service = LabNotificationListenerService.instance
                if (service != null && LabNotificationListenerService.isConnected) {
                    val active = service.refreshActiveNotifications()
                    val replyable = active.firstOrNull { it.canReply }

                    if (replyable != null) {
                        val replySent = service.testReplyToNotification(
                            replyable.key,
                            "Auto reply from PermissionLab test!"
                        )
                        if (replySent) {
                            onResult("[SUCCESS] Dispatched RemoteInput reply to: ${replyable.title} (${replyable.packageName})")
                        } else {
                            onResult("[FAILED] Could not send reply intent to notification.")
                        }
                    } else {
                        // Dismiss test if no replyable notification
                        val target = active.firstOrNull()
                        if (target != null) {
                            val dismissed = service.testDismissNotification(target.key)
                            onResult("[SUCCESS] Dismissed notification: ${target.title} (Key: ${target.key.take(15)}...) Result: $dismissed")
                        } else {
                            onResult("[INFO] No notifications found to reply or dismiss. Run 'Post Notification' test first!")
                        }
                    }
                } else {
                    onResult("[FAILED] Notification Listener Service not connected.")
                }
            }

            PermissionType.SYSTEM_ALERT_WINDOW -> {
                if (Settings.canDrawOverlays(context)) {
                    showTestOverlay(context)
                    onResult("[SUCCESS] Overlay permission granted! Spawning 4-second floating test overlay on screen.")
                } else {
                    onResult("[FAILED] Overlay permission denied.")
                }
            }

            PermissionType.WRITE_SETTINGS -> {
                if (Settings.System.canWrite(context)) {
                    try {
                        val mode = Settings.System.getInt(
                            context.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            0
                        )
                        Settings.System.putInt(
                            context.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            mode
                        )
                        onResult("[SUCCESS] Successfully verified Settings.System.putInt! Brightness mode: $mode")
                    } catch (e: Exception) {
                        onResult("[FAILED] Error writing system setting: ${e.message}")
                    }
                } else {
                    onResult("[FAILED] Permission denied in Settings -> Modify System Settings.")
                }
            }

            PermissionType.MANAGE_EXTERNAL_STORAGE -> {
                val isManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    true
                }

                if (isManager) {
                    val sdcard = Environment.getExternalStorageDirectory()
                    val files = sdcard.listFiles()?.take(4)?.map { it.name } ?: emptyList()
                    onResult("[SUCCESS] Full storage access verified! Root directories: ${files.joinToString(", ")}")
                } else {
                    onResult("[FAILED] All files access permission not granted.")
                }
            }

            PermissionType.IGNORE_BATTERY_OPTIMIZATIONS -> {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val isIgnoring = pm?.isIgnoringBatteryOptimizations(context.packageName) == true
                if (isIgnoring) {
                    onResult("[SUCCESS] App is whitelisted from battery optimization Doze mode.")
                } else {
                    onResult("[FAILED] App is subject to battery optimizations and background restrictions.")
                }
            }

            PermissionType.SCHEDULE_EXACT_ALARM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    if (am?.canScheduleExactAlarms() == true) {
                        val intent = Intent(context, LabAlarmReceiver::class.java)
                        val pi = PendingIntent.getBroadcast(
                            context,
                            2001,
                            intent,
                            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                        am.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + 3000,
                            pi
                        )
                        onResult("[SUCCESS] Exact alarm scheduled! Triggering in 3 seconds (check Toast).")
                    } else {
                        onResult("[FAILED] SCHEDULE_EXACT_ALARM permission denied.")
                    }
                } else {
                    onResult("[SUCCESS] Exact alarms permitted on Android < 12.")
                }
            }

            PermissionType.DEVICE_ADMIN -> {
                val status = PermissionChecker.checkPermission(context, PermissionType.DEVICE_ADMIN)
                if (status == com.permissionlab.app.model.PermissionStatus.GRANTED) {
                    onResult("[SUCCESS] Device Admin is ACTIVE for PermissionLab.")
                } else {
                    onResult("[FAILED] Device Admin is INACTIVE. Tap Configure to activate.")
                }
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PermissionLab Tests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for PermissionLab test notifications"
                enableLights(true)
                lightColor = Color.BLUE
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun showTestOverlay(context: Context) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val overlayView = TextView(context).apply {
            text = "⚡ PermissionLab Floating Test Overlay ⚡"
            setBackgroundColor(Color.parseColor("#DD1A73E8"))
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(32, 20, 32, 20)
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 120
        }

        try {
            wm.addView(overlayView, params)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    wm.removeView(overlayView)
                } catch (e: Exception) {
                    // Ignore removal if already dismissed
                }
            }, 4000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
