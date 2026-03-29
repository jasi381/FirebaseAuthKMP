package dev.jasmeetsingh.firebaseauth

import com.onesignal.OneSignal

actual class OneSignalNotifs actual constructor() {

    actual fun login(userId: String) {
        OneSignal.login(userId)
        OneSignal.User.pushSubscription.optIn()
    }

    actual fun logout() {
        OneSignal.User.pushSubscription.optOut()
        OneSignal.logout()
    }
}