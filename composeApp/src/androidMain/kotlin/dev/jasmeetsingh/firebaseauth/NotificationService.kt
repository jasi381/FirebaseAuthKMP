package dev.jasmeetsingh.firebaseauth

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.onesignal.notifications.INotificationReceivedEvent
import com.onesignal.notifications.INotificationServiceExtension
import org.json.JSONObject

class NotificationService : INotificationServiceExtension {

    private val context: Context by lazy {
        // Get application context safely
        try {
            Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null) as Context
        } catch (e: Exception) {
            throw RuntimeException("Failed to get application context", e)
        }
    }

    override fun onNotificationReceived(event: INotificationReceivedEvent) {
        val notification = event.notification


        Log.d("DEBUG", "=== Notification Debug Info ===")
        Log.d("DEBUG", "Notification title: ${notification.title}")
        Log.d("DEBUG", "Notification body: ${notification.body}")
        Log.d("DEBUG", "Raw Payload : ${notification.rawPayload}")

        // Get user preferences using injected context
        val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

        // Method 1: Check additionalData (most common for SDK 5.x)
        val additionalData = notification.additionalData
        Log.d("DEBUG", "Additional data: $additionalData")

        if (additionalData != null) {
            // List all keys in the additional data
            val keys = additionalData.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = additionalData.get(key)
                Log.d("DEBUG", "Key: $key, Value: $value")
            }

            // Check for notification_category
            if (additionalData.has("notificationType")) {
                val category = additionalData.getString("notificationType")
                Log.d("DEBUG", "Found category: $category")
                handleNotificationByCategory(category, event, sharedPrefs)
                return
            }
        }

        // Method 2: Check raw payload (fallback)
        try {
            val rawPayload = notification.rawPayload
            Log.d("DEBUG", "Raw payload: $rawPayload")

            if (rawPayload != null) {
                val jsonPayload = JSONObject(rawPayload)

                // Check in custom object
                if (jsonPayload.has("custom")) {
                    val custom = jsonPayload.getJSONObject("custom")
                    Log.d("DEBUG", "Custom object: $custom")

                    if (custom.has("a")) {
                        val additionalObject = custom.getJSONObject("a")
                        Log.d("DEBUG", "Additional object in custom.a: $additionalObject")

                        if (additionalObject.has("notificationType")) {
                            val category = additionalObject.getString("notificationType")
                            Log.d("DEBUG", "Found category in custom.a: $category")
                            handleNotificationByCategory(category, event, sharedPrefs)
                            return
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DEBUG", "Error parsing raw payload: ${e.message}")
        }

        Log.d("DEBUG", "No category found, allowing notification")
        // Allow notification if no category found
    }

    private fun handleNotificationByCategory(
        category: String,
        event: INotificationReceivedEvent,
        sharedPrefs: SharedPreferences
    ) {
        Log.d("OneSignal", "Processing notification with category: $category")

        when (category.lowercase()) {
            "message" -> {
                val chatEnabled = sharedPrefs.getBoolean("chat_notifications", false)
                Log.d("OneSignal", "Chat notifications enabled: $chatEnabled")

                if (!chatEnabled) {
                    Log.d("OneSignal", "Blocking chat notification - user disabled")
                    event.preventDefault()
                    return
                } else {
                    Log.d("OneSignal", "Allowing chat notification - user enabled")
                }
            }

            "incoming_call" -> {
                event.preventDefault()

                val additionalData = event.notification.additionalData
                val channelName = additionalData?.optString("channelName", "") ?: ""
                val callType = additionalData?.optString("callType", "video") ?: "video"
                val callerUid = additionalData?.optString("callerUid", "") ?: ""
                val callerEmail =
                    additionalData?.optString("callerEmail", "Unknown Caller") ?: "Unknown Caller"

                val notification = buildCallNotification(
                    callerName = callerEmail,
                    channelName = channelName,
                    callType = callType,
                    callerUid = callerUid,
                )

                val nm =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(CallActionReceiver.NOTIFICATION_ID, notification)
            }

            else -> {
                Log.d("OneSignal", "Allowing notification with category: $category")
            }
        }
    }

    private fun buildCallNotification(
        callerName: String,
        channelName: String,
        callType: String,
        callerUid: String,
    ): Notification {

        // Decline PendingIntent -> broadcasts ACTION_DECLINE_CALL
        val declineIntent = Intent(CallActionReceiver.ACTION_DECLINE_CALL).apply {
            setClass(context, CallActionReceiver::class.java)
            putExtra(CallActionReceiver.EXTRA_CHANNEL_NAME, channelName)
            putExtra(CallActionReceiver.EXTRA_CALL_TYPE, callType)
            putExtra(CallActionReceiver.EXTRA_CALLER_UID, callerUid)
        }
        val declinePi = PendingIntent.getBroadcast(
            context,
            CallActionReceiver.ACTION_DECLINE_CALL.hashCode(),
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Answer PendingIntent -> launches CallActivity directly
        val answerIntent = Intent(context, CallActivity::class.java).apply {
            putExtra("channel", channelName)
            putExtra("isVideo", callType == "video")
            putExtra("fromNotification", true)
            putExtra("callerUid", callerUid)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val answerPi = PendingIntent.getActivity(
            context,
            CallActionReceiver.ACTION_ANSWER_CALL.hashCode(),
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Full screen intent — shows incoming call UI on lock screen
        val fullScreenIntent = Intent(context, IncomingCallActivity::class.java).apply {
            putExtra("channel", channelName)
            putExtra("callType", callType)
            putExtra("callerUid", callerUid)
            putExtra("callerEmail", callerName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val callLabel = if (callType == "video") "Video" else "Audio"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val caller = Person.Builder()
                .setName(callerName)
                .setImportant(true)
                .build()

            return Notification.Builder(context, "incoming_call_channel_v2")
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setStyle(Notification.CallStyle.forIncomingCall(caller, declinePi, answerPi))
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setOngoing(true)
                .setFlag(Notification.FLAG_INSISTENT, true)
                .setCategory(Notification.CATEGORY_CALL)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build()
        }

        // API 26-30 fallback with explicit action buttons
        return NotificationCompat.Builder(context, "incoming_call_channel_v2")
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("Incoming $callLabel Call")
            .setContentText(callerName)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", declinePi)
            .addAction(android.R.drawable.sym_action_call, "Answer", answerPi)
            .build()
    }


}
