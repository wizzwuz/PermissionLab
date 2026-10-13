package com.permissionlab.app.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class LabDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "LabDeviceAdmin"
        var isDeviceAdminActive: Boolean = false
            private set
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        isDeviceAdminActive = true
        Log.i(TAG, "Device Admin enabled.")
        Toast.makeText(context, "PermissionLab Device Admin Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        isDeviceAdminActive = false
        Log.i(TAG, "Device Admin disabled.")
        Toast.makeText(context, "PermissionLab Device Admin Disabled", Toast.LENGTH_SHORT).show()
    }
}
