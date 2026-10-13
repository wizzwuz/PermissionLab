package com.permissionlab.app.permission

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.permissionlab.app.model.PermissionType
import com.permissionlab.app.receiver.LabDeviceAdminReceiver

object PermissionRequester {

    fun requestPermission(activity: Activity, type: PermissionType) {
        try {
            when (type) {
                PermissionType.INSTALL_UNKNOWN_SOURCES -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${activity.packageName}")
                        )
                        activity.startActivity(intent)
                    } else {
                        Toast.makeText(activity, "Not required on Android < 8.0", Toast.LENGTH_SHORT).show()
                    }
                }

                PermissionType.USAGE_STATS -> {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    if (intent.resolveActivity(activity.packageManager) == null) {
                        intent.data = null
                    }
                    activity.startActivity(intent)
                }

                PermissionType.ACCESSIBILITY_SERVICE -> {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    activity.startActivity(intent)
                    Toast.makeText(
                        activity,
                        "Enable 'PermissionLab Accessibility Inspector'",
                        Toast.LENGTH_LONG
                    ).show()
                }

                PermissionType.WRITE_SECURE_SETTINGS -> {
                    // Requires ADB or Root. Show instruction prompt.
                    Toast.makeText(
                        activity,
                        "Grant via ADB: adb shell pm grant ${activity.packageName} android.permission.WRITE_SECURE_SETTINGS",
                        Toast.LENGTH_LONG
                    ).show()
                }

                PermissionType.RESTRICTED_SETTINGS -> {
                    // Open App Details where user can tap 3-dots -> Allow restricted settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivity(intent)
                    Toast.makeText(
                        activity,
                        "In App Info: Tap ⋮ (top right) -> 'Allow restricted settings'",
                        Toast.LENGTH_LONG
                    ).show()
                }

                PermissionType.NOTIFICATION_LISTENER,
                PermissionType.NOTIFICATION_REPLY_CONTROL -> {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    activity.startActivity(intent)
                    Toast.makeText(
                        activity,
                        "Enable 'PermissionLab Notification Listener'",
                        Toast.LENGTH_LONG
                    ).show()
                }

                PermissionType.NOTIFICATION_POST -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            1001
                        )
                    } else {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                        }
                        activity.startActivity(intent)
                    }
                }

                PermissionType.SYSTEM_ALERT_WINDOW -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${activity.packageName}")
                        )
                        activity.startActivity(intent)
                    }
                }

                PermissionType.WRITE_SETTINGS -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_WRITE_SETTINGS,
                            Uri.parse("package:${activity.packageName}")
                        )
                        activity.startActivity(intent)
                    }
                }

                PermissionType.MANAGE_EXTERNAL_STORAGE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:${activity.packageName}")
                        )
                        activity.startActivity(intent)
                    } else {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            1002
                        )
                    }
                }

                PermissionType.IGNORE_BATTERY_OPTIMIZATIONS -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${activity.packageName}")
                        )
                        activity.startActivity(intent)
                    }
                }

                PermissionType.SCHEDULE_EXACT_ALARM -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:${activity.packageName}")
                        )
                        activity.startActivity(intent)
                    }
                }

                PermissionType.DEVICE_ADMIN -> {
                    val compName = ComponentName(activity, LabDeviceAdminReceiver::class.java)
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "PermissionLab needs this to test Device Admin APIs."
                        )
                    }
                    activity.startActivity(intent)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(activity, "Error opening settings: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
