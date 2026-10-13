package com.permissionlab.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

class LabAlarmReceiver : BroadcastReceiver() {

    companion object {
        var lastAlarmFiredTime: Long = 0L
            private set
    }

    override fun onReceive(context: Context, intent: Intent) {
        lastAlarmFiredTime = System.currentTimeMillis()
        Log.i("LabAlarmReceiver", "Exact alarm fired at $lastAlarmFiredTime")
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "Exact Alarm Fired Successfully! (SCHEDULE_EXACT_ALARM working)",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
