package dev.jasmeetsingh.firebaseauth

import kotlinx.cinterop.ExperimentalForeignApi
import swiftPMImport.dev.jasmeetsingh.composeApp.OneSignal

@OptIn(ExperimentalForeignApi::class)
actual class OneSignalNotifs actual constructor() {

    actual fun login(userId: String) {
        OneSignal.login(userId)
        OneSignal.User()?.pushSubscription?.optIn()
    }

    actual fun logout() {
        OneSignal.User()?.pushSubscription?.optOut()
        OneSignal.logout()
    }
}