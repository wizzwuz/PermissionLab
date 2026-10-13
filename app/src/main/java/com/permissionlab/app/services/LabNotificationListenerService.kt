package com.permissionlab.app.services

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class LabNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "LabNotifListener"
        var isConnected: Boolean = false
            private set
        var instance: LabNotificationListenerService? = null
            private set

        val recentNotifications = mutableListOf<NotificationSnapshot>()
    }

    data class NotificationSnapshot(
        val key: String,
        val packageName: String,
        val title: String,
        val text: String,
        val postTime: Long,
        val canReply: Boolean
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        instance = this
        Log.i(TAG, "LabNotificationListenerService connected.")
        refreshActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        instance = null
        Log.i(TAG, "LabNotificationListenerService disconnected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "No Title"
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "No Content"
        val canReply = findReplyAction(sbn.notification) != null

        synchronized(recentNotifications) {
            recentNotifications.removeAll { it.key == sbn.key }
            recentNotifications.add(
                0,
                NotificationSnapshot(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    title = title,
                    text = text,
                    postTime = sbn.postTime,
                    canReply = canReply
                )
            )
            if (recentNotifications.size > 20) {
                recentNotifications.removeAt(recentNotifications.lastIndex)
            }
        }
        Log.d(TAG, "Notification posted from: ${sbn.packageName} - $title: $text")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        synchronized(recentNotifications) {
            recentNotifications.removeAll { it.key == sbn.key }
        }
    }

    fun refreshActiveNotifications(): List<NotificationSnapshot> {
        return try {
            val active = activeNotifications ?: emptyArray()
            val list = active.map { sbn ->
                val extras = sbn.notification.extras
                NotificationSnapshot(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "No Title",
                    text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "No Content",
                    postTime = sbn.postTime,
                    canReply = findReplyAction(sbn.notification) != null
                )
            }
            synchronized(recentNotifications) {
                recentNotifications.clear()
                recentNotifications.addAll(list)
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching active notifications", e)
            emptyList()
        }
    }

    /**
     * Find an action containing a RemoteInput (Direct Reply)
     */
    fun findReplyAction(notification: Notification): Notification.Action? {
        val actions = notification.actions ?: return null
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (ri in remoteInputs) {
                if (ri.allowFreeFormInput) {
                    return action
                }
            }
        }
        return null
    }

    /**
     * Live Test: Sends an automated reply back to a notification using its RemoteInput PendingIntent
     */
    fun testReplyToNotification(key: String, replyMessage: String): Boolean {
        try {
            val active = activeNotifications ?: return false
            val target = active.firstOrNull { it.key == key } ?: return false
            val action = findReplyAction(target.notification) ?: return false

            val remoteInput = action.remoteInputs?.firstOrNull { it.allowFreeFormInput } ?: return false
            val bundle = Bundle()
            bundle.putCharSequence(remoteInput.resultKey, replyMessage)

            val replyIntent = Intent()
            RemoteInput.addResultsToIntent(arrayOf(remoteInput), replyIntent, bundle)

            action.actionIntent.send(this, 0, replyIntent)
            Log.i(TAG, "Successfully sent direct reply to: $key with text: $replyMessage")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reply to notification: $key", e)
            return false
        }
    }

    /**
     * Live Test: Dismisses / Cancels a notification by key
     */
    fun testDismissNotification(key: String): Boolean {
        return try {
            cancelNotification(key)
            Log.i(TAG, "Successfully requested cancellation for: $key")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel notification: $key", e)
            false
        }
    }
}
