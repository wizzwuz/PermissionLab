package com.permissionlab.app.receiver

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

class LabNotificationReplyReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REPLY = "com.permissionlab.app.ACTION_REPLY"
        const val KEY_TEXT_REPLY = "key_text_reply"
        var lastReceivedReply: String? = null
            private set
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REPLY) {
            val remoteInputBundle = RemoteInput.getResultsFromIntent(intent)
            if (remoteInputBundle != null) {
                val replyText = remoteInputBundle.getCharSequence(KEY_TEXT_REPLY)?.toString()
                lastReceivedReply = replyText
                Log.i("LabReplyReceiver", "Received direct reply: $replyText")

                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Direct Reply Received: \"$replyText\"",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
