package dev.jasmeetsingh.firebaseauth

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class CallActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ANSWER_CALL = "dev.jasmeetsingh.firebaseauth.ACTION_ANSWER_CALL"
        const val ACTION_DECLINE_CALL = "dev.jasmeetsingh.firebaseauth.ACTION_DECLINE_CALL"
        const val EXTRA_CHANNEL_NAME = "extra_channel_name"
        const val EXTRA_CALL_TYPE = "extra_call_type"
        const val EXTRA_CALLER_UID = "extra_caller_uid"
        const val NOTIFICATION_ID = 101097
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DECLINE_CALL) return

        Log.d("CallActionReceiver", "Decline pressed")
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)

        val channel = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: return
        val callerUid = intent.getStringExtra(EXTRA_CALLER_UID) ?: ""
        val parts = channel.split("_")
        val myUid = parts.firstOrNull { it != callerUid } ?: ""
        if (myUid.isNotEmpty()) {
            CallSignaling().clearCall(myUid)
        }
    }
}
