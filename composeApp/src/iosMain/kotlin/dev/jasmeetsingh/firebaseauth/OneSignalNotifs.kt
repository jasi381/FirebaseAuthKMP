package dev.jasmeetsingh.firebaseauth


// iosMain
actual class OneSignalNotifs actual constructor() {

    /**
     * TODO: Implement push notifications for iOS.
     * Requires a paid Apple Developer account ($99/year) for APN (Apple Push Notification) setup.
     * Once available:
     * 1. Configure APN certificates in Apple Developer Console
     * 2. Add OneSignal iOS SDK via SPM (github.com/OneSignal/OneSignal-XCFramework)
     * 3. Call OneSignal.initialize("<ONESIGNAL_APP_ID>") in AppDelegate
     * 4. Login with: OneSignal.login(userId)
     */
    actual fun login(userId: String) {
        // no-op — APN setup required for iOS push notifications
    }

    /**
     * TODO: Implement logout for iOS.
     * Once APN is configured, call OneSignal.logout() here
     * to stop sending push notifications to this device.
     */
    actual fun logout() {
        // no-op — APN setup required for iOS push notifications
    }
}