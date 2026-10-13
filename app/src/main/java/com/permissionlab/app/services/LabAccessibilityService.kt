package com.permissionlab.app.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi

class LabAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "LabAccessibility"
        var isConnected: Boolean = false
            private set
        var instance: LabAccessibilityService? = null
            private set
        var lastEventSummary: String = "No events received yet."
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isConnected = true
        instance = this
        Log.i(TAG, "LabAccessibilityService connected successfully.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: "unknown"
        val type = AccessibilityEvent.eventTypeToString(event.eventType)
        lastEventSummary = "Event: $type from pkg: $pkg at ${System.currentTimeMillis()}"
        Log.d(TAG, lastEventSummary)
    }

    override fun onInterrupt() {
        Log.w(TAG, "LabAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        isConnected = false
        instance = null
        Log.i(TAG, "LabAccessibilityService destroyed.")
    }

    /**
     * Live test: Simulates a harmless tap gesture using AccessibilityService API.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun testPerformClickGesture(x: Float, y: Float, onComplete: (Boolean) -> Unit) {
        val clickPath = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(clickPath, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Test gesture completed at ($x, $y)")
                onComplete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "Test gesture cancelled.")
                onComplete(false)
            }
        }, null)
    }
}
