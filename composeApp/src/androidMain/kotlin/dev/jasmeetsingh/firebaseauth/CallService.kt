package dev.jasmeetsingh.firebaseauth

import android.content.Intent

actual class CallService actual constructor() {

    actual fun startVideoCall(channelName :String){
        val activity = ActivityProvider.activity ?:return
        activity.startActivity(Intent(activity, CallActivity::class.java).apply {
            putExtra("channel", channelName)
            putExtra("isVideo", true)
        })
    }

    actual fun startAudioCall(channelName: String) {
        val activity = ActivityProvider.activity ?: return
        activity.startActivity(Intent(activity, CallActivity::class.java).apply {
            putExtra("channel", channelName)
            putExtra("isVideo", false)
        })
    }
}