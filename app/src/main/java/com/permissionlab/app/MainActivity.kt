package com.permissionlab.app

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.permissionlab.app.model.PermissionItem
import com.permissionlab.app.model.PermissionStatus
import com.permissionlab.app.model.PermissionType
import com.permissionlab.app.permission.PermissionChecker
import com.permissionlab.app.permission.PermissionRequester
import com.permissionlab.app.permission.PermissionTester
import com.permissionlab.app.ui.PermissionAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var rvPermissions: RecyclerView
    private lateinit var tvSummaryCount: TextView
    private lateinit var btnRefreshAll: MaterialButton
    private lateinit var btnAdbDialog: MaterialButton

    private lateinit var adapter: PermissionAdapter
    private val permissionItems = mutableListOf<PermissionItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvPermissions = findViewById(R.id.rvPermissions)
        tvSummaryCount = findViewById(R.id.tvSummaryCount)
        btnRefreshAll = findViewById(R.id.btnRefreshAll)
        btnAdbDialog = findViewById(R.id.btnAdbDialog)

        setupPermissionList()

        adapter = PermissionAdapter(
            context = this,
            items = permissionItems,
            onToggleClick = { item, _ ->
                if (item.status != PermissionStatus.GRANTED) {
                    Toast.makeText(this, "Requesting ${item.title} permission...", Toast.LENGTH_SHORT).show()
                    PermissionRequester.requestPermission(this, item.type)
                } else {
                    // Permission already granted; open settings if user wishes to revoke/modify
                    Toast.makeText(this, "Opening settings to modify ${item.title}...", Toast.LENGTH_SHORT).show()
                    PermissionRequester.requestPermission(this, item.type)
                }
            },
            onConfigureClick = { item ->
                PermissionRequester.requestPermission(this, item.type)
            },
            onTestClick = { item, position ->
                PermissionTester.runTest(this, item.type) { result ->
                    item.testResult = result
                    // Update status dynamically if test reveals granted state
                    item.status = PermissionChecker.checkPermission(this, item.type)
                    adapter.notifyItemChanged(position)
                    updateSummary()
                }
            }
        )

        rvPermissions.layoutManager = LinearLayoutManager(this)
        rvPermissions.adapter = adapter

        btnRefreshAll.setOnClickListener {
            refreshPermissions()
            Toast.makeText(this, "Permissions refreshed", Toast.LENGTH_SHORT).show()
        }

        btnAdbDialog.setOnClickListener {
            showAdbHelperDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun setupPermissionList() {
        permissionItems.clear()
        permissionItems.addAll(
            listOf(
                PermissionItem(
                    type = PermissionType.INSTALL_UNKNOWN_SOURCES,
                    title = "Install Unknown Sources",
                    category = "PACKAGE_INSTALL • API 26+",
                    description = "Allows this app to initiate installations of unknown APK packages. Checked via PackageManager.canRequestPackageInstalls()."
                ),
                PermissionItem(
                    type = PermissionType.USAGE_STATS,
                    title = "App Usage Data / Stats",
                    category = "APP_OPS • API 21+",
                    description = "Access device app usage history, foreground times, and screen time statistics via UsageStatsManager."
                ),
                PermissionItem(
                    type = PermissionType.ACCESSIBILITY_SERVICE,
                    title = "Accessibility Settings & Service",
                    category = "ACCESSIBILITY • ALL APIS",
                    description = "Enables LabAccessibilityService to inspect UI elements, listen to window events, and dispatch interaction gestures."
                ),
                PermissionItem(
                    type = PermissionType.WRITE_SECURE_SETTINGS,
                    title = "Write Secure Settings",
                    category = "PRIVILEGED • ADB / ROOT REQUIRED",
                    description = "Allows modifying system Settings.Secure. Requires ADB grant or Superuser Root access.",
                    requiresAdb = true,
                    adbCommand = "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
                ),
                PermissionItem(
                    type = PermissionType.RESTRICTED_SETTINGS,
                    title = "Restricted Settings (Android 13+)",
                    category = "APP_OPS (119) • API 33+",
                    description = "Android 13+ blocks sideloaded apps from enabling Accessibility and Notification Listeners until allowed in App Info ⋮ menu."
                ),
                PermissionItem(
                    type = PermissionType.NOTIFICATION_LISTENER,
                    title = "Notification Read & Listen",
                    category = "NOTIFICATION • API 18+",
                    description = "Inspect incoming and active system notifications, package names, titles, texts, and embedded action buttons."
                ),
                PermissionItem(
                    type = PermissionType.NOTIFICATION_POST,
                    title = "Post Notifications",
                    category = "RUNTIME • API 33+",
                    description = "Post notifications to status bar. Required to test notification channels, action buttons, and direct reply."
                ),
                PermissionItem(
                    type = PermissionType.NOTIFICATION_REPLY_CONTROL,
                    title = "Notification Reply & Dismiss Control",
                    category = "NOTIFICATION_CONTROL • API 20+",
                    description = "Dismiss active notifications or send inline direct replies using RemoteInput action PendingIntents."
                ),
                PermissionItem(
                    type = PermissionType.SYSTEM_ALERT_WINDOW,
                    title = "Overlay / Draw Over Other Apps",
                    category = "SPECIAL_ACCESS • API 23+",
                    description = "Display floating windows or views over other running applications via WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY."
                ),
                PermissionItem(
                    type = PermissionType.WRITE_SETTINGS,
                    title = "Modify System Settings",
                    category = "SETTINGS • API 23+",
                    description = "Modify system settings such as screen brightness mode, screen timeout, and sound profiles."
                ),
                PermissionItem(
                    type = PermissionType.MANAGE_EXTERNAL_STORAGE,
                    title = "All Files Access",
                    category = "STORAGE • API 30+",
                    description = "Full broad access to external shared storage (/sdcard) via Environment.isExternalStorageManager()."
                ),
                PermissionItem(
                    type = PermissionType.IGNORE_BATTERY_OPTIMIZATIONS,
                    title = "Battery Optimization Whitelist",
                    category = "POWER • API 23+",
                    description = "Whitelists app from Doze mode and aggressive background process termination."
                ),
                PermissionItem(
                    type = PermissionType.SCHEDULE_EXACT_ALARM,
                    title = "Schedule Exact Alarms",
                    category = "ALARM • API 31+",
                    description = "Allows scheduling precise alarms via AlarmManager.setExactAndAllowWhileIdle()."
                ),
                PermissionItem(
                    type = PermissionType.DEVICE_ADMIN,
                    title = "Device Administration",
                    category = "ADMIN • ALL APIS",
                    description = "Device administration capabilities like lock screen policy and wipe data checks."
                )
            )
        )
    }

    private fun refreshPermissions() {
        for (item in permissionItems) {
            item.status = PermissionChecker.checkPermission(this, item.type)
        }
        adapter.notifyDataSetChanged()
        updateSummary()
    }

    private fun updateSummary() {
        val grantedCount = permissionItems.count { it.status == PermissionStatus.GRANTED }
        val totalCount = permissionItems.size
        tvSummaryCount.text = "Granted: $grantedCount / $totalCount"
    }

    private fun showAdbHelperDialog() {
        val message = """
            Run these commands via ADB terminal or Shizuku/rish to grant privileged permissions:

            1. Write Secure Settings:
            adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS

            2. Unblock Android 13+ Restricted Settings:
            adb shell appops set $packageName ACCESS_RESTRICTED_SETTINGS allow

            3. Grant Usage Stats directly:
            adb shell appops set $packageName GET_USAGE_STATS allow

            4. Grant Notification Access directly:
            adb shell cmd notification allow_listener $packageName/com.permissionlab.app.services.LabNotificationListenerService

            5. Grant Accessibility Service directly:
            adb shell settings put secure enabled_accessibility_services $packageName/com.permissionlab.app.services.LabAccessibilityService
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("Privileged / ADB Grant Commands")
            .setMessage(message)
            .setPositiveButton("Grant via Root (su)") { _, _ ->
                executeRootGrant()
            }
            .setNegativeButton("OK", null)
            .setNeutralButton("Copy Commands") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("ADB Commands", message)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Commands copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun executeRootGrant() {
        Toast.makeText(this, "Requesting Superuser (su) grant...", Toast.LENGTH_SHORT).show()
        kotlin.concurrent.thread {
            try {
                val p = Runtime.getRuntime().exec("su")
                val os = java.io.DataOutputStream(p.outputStream)
                val script = """
                    pm grant $packageName android.permission.WRITE_SECURE_SETTINGS
                    pm grant $packageName android.permission.POST_NOTIFICATIONS
                    appops set $packageName GET_USAGE_STATS allow
                    appops set $packageName ACCESS_RESTRICTED_SETTINGS allow
                    appops set $packageName SYSTEM_ALERT_WINDOW allow
                    appops set $packageName WRITE_SETTINGS allow
                    appops set $packageName MANAGE_EXTERNAL_STORAGE allow
                    cmd notification allow_listener $packageName/com.permissionlab.app.services.LabNotificationListenerService
                    settings put secure enabled_accessibility_services $packageName/com.permissionlab.app.services.LabAccessibilityService
                    settings put secure accessibility_enabled 1
                    cmd device_idle whitelist +$packageName
                    exit
                """.trimIndent()
                os.writeBytes(script + "\n")
                os.flush()
                p.waitFor()
                runOnUiThread {
                    refreshPermissions()
                    Toast.makeText(this, "All permissions granted via Superuser root!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Root execution failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
