package dev.jasmeetsingh.firebaseauth

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FireBaseAuthApp : Application() {

    override fun onCreate() {
        super.onCreate()


        Napier.base(DebugAntilog())

        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        OneSignal.initWithContext(this, "778b2d15-8416-423e-bb23-1fc7778445e8")

        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(false)
        }


        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)   // ← treated as a call ring
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val incomingCallChannel = NotificationChannel(
            "incoming_call_channel_v2",
            "Incoming Call Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Used for incoming call notifications"
            enableLights(true)
            enableVibration(true)
            lightColor = Color.Green.toArgb()
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(ringtoneUri, audioAttributes)                    // ← ringtone
            vibrationPattern = longArrayOf(0, 1000, 500, 1000)   // ← buzz pattern
            setBypassDnd(true)                                        // ← rings even in DND
        }

        val nm = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(incomingCallChannel)

    }
}