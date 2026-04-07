import SwiftUI
import FirebaseCore
import GoogleSignIn
import OneSignalFramework
import ComposeApp
import UserNotifications
import CallKit
import AVFoundation

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}

// MARK: - CallKit Manager
class CallManager: NSObject, CXProviderDelegate {

    static let shared = CallManager()

    private let provider: CXProvider
    private let callController = CXCallController()

    // Store call info so we can use it on answer/decline
    var activeCallUUID: UUID?
    var activeChannelName: String = ""
    var activeCallType: String = "video"
    var activeCallerUid: String = ""

    private override init() {
        let config = CXProviderConfiguration()
        config.supportsVideo = true
        config.maximumCallsPerCallGroup = 1
        config.supportedHandleTypes = [.generic]
        config.ringtoneSound = nil // Use default iOS ringtone
        provider = CXProvider(configuration: config)
        super.init()
        provider.setDelegate(self, queue: nil)
    }

    // Shared logic: accept call and launch the Agora call screen
    static func answerCall(channelName: String, callType: String, callerUid: String) {
        // Derive my UID from channelName (format: "sortedUid1_sortedUid2")
        let parts = channelName.split(separator: "_")
        let myUid = parts.first(where: { String($0) != callerUid }).map(String.init) ?? ""

        let signaling = CallSignaling()
        if !myUid.isEmpty {
            signaling.acceptCall(myUid: myUid)
        }

        // Launch the call screen
        DispatchQueue.main.async {
            let callService = CallService()
            if callType == "video" {
                callService.startVideoCall(channelName: channelName)
            } else {
                callService.startAudioCall(channelName: channelName)
            }

            // Clean up Firebase after a short delay
            if !myUid.isEmpty {
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                    signaling.clearCall(uid: myUid)
                }
            }
        }
    }

    func reportIncomingCall(
        callerEmail: String,
        channelName: String,
        callType: String,
        callerUid: String,
        completion: @escaping (Error?) -> Void
    ) {
        let uuid = UUID()
        activeCallUUID = uuid
        activeChannelName = channelName
        activeCallType = callType
        activeCallerUid = callerUid

        let update = CXCallUpdate()
        update.remoteHandle = CXHandle(type: .generic, value: callerEmail)
        update.localizedCallerName = callerEmail
        update.hasVideo = (callType == "video")
        update.supportsHolding = false
        update.supportsGrouping = false
        update.supportsUngrouping = false
        update.supportsDTMF = false

        provider.reportNewIncomingCall(with: uuid, update: update) { error in
            if let error = error {
                print("CallKit reportNewIncomingCall error: \(error.localizedDescription)")
                self.activeCallUUID = nil
            }
            completion(error)
        }
    }

    func endCall() {
        guard let uuid = activeCallUUID else { return }
        let endAction = CXEndCallAction(call: uuid)
        let transaction = CXTransaction(action: endAction)
        callController.request(transaction) { error in
            if let error = error {
                print("CallKit endCall error: \(error.localizedDescription)")
            }
        }
    }

    // MARK: - CXProviderDelegate

    func providerDidReset(_ provider: CXProvider) {
        activeCallUUID = nil
    }

    // User tapped "Answer" on the CallKit screen
    func provider(_ provider: CXProvider, perform action: CXAnswerCallAction) {
        CallManager.answerCall(
            channelName: activeChannelName,
            callType: activeCallType,
            callerUid: activeCallerUid
        )
        action.fulfill()
    }

    // User tapped "Decline" on the CallKit screen
    func provider(_ provider: CXProvider, perform action: CXEndCallAction) {
        let parts = activeChannelName.split(separator: "_")
        let myUid = parts.first(where: { String($0) != activeCallerUid }).map(String.init) ?? ""
        if !myUid.isEmpty {
            CallSignaling().clearCall(uid: myUid)
        }
        activeCallUUID = nil
        action.fulfill()
    }

    func provider(_ provider: CXProvider, didActivate audioSession: AVAudioSession) {}
    func provider(_ provider: CXProvider, didDeactivate audioSession: AVAudioSession) {}
}

// MARK: - AppDelegate
class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {

        FirebaseApp.configure()

        // Initialize CallManager early
        _ = CallManager.shared

        OneSignal.initialize("778b2d15-8416-423e-bb23-1fc7778445e8", withLaunchOptions: launchOptions)
        OneSignal.Notifications.requestPermission({ _ in }, fallbackToSettings: false)

        UNUserNotificationCenter.current().delegate = self

        // Register for remote notifications (needed for content-available background wake)
        application.registerForRemoteNotifications()

        // Register incoming call notification category with Answer/Decline buttons
        // These are used as FALLBACK when app is killed (CallKit can't be triggered)
        let answerAction = UNNotificationAction(
            identifier: "ANSWER_ACTION",
            title: "Answer",
            options: [.foreground]
        )
        let declineAction = UNNotificationAction(
            identifier: "DECLINE_ACTION",
            title: "Decline",
            options: [.destructive]
        )
        let callCategory = UNNotificationCategory(
            identifier: "INCOMING_CALL",
            actions: [answerAction, declineAction],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )
        UNUserNotificationCenter.current().setNotificationCategories([callCategory])

        // Register for OneSignal notification events to intercept incoming calls
        OneSignal.Notifications.addClickListener(self)
        OneSignal.Notifications.addForegroundLifecycleListener(self)

        return true
    }

    // Background wake-up: called when push arrives with content-available (even when app is in background)
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        // Extract additional data from OneSignal payload
        let additionalData: [String: Any]? = {
            if let custom = userInfo["custom"] as? [String: Any],
               let a = custom["a"] as? [String: Any] {
                return a
            }
            return nil
        }()

        let notificationType = additionalData?["notificationType"] as? String

        if notificationType == "incoming_call" {
            let callerEmail = additionalData?["callerEmail"] as? String ?? "Unknown Caller"
            let channelName = additionalData?["channelName"] as? String ?? ""
            let callType = additionalData?["callType"] as? String ?? "video"
            let callerUid = additionalData?["callerUid"] as? String ?? ""

            // Remove any banner notification the NSE may have shown
            UNUserNotificationCenter.current().removeAllDeliveredNotifications()

            CallManager.shared.reportIncomingCall(
                callerEmail: callerEmail,
                channelName: channelName,
                callType: callType,
                callerUid: callerUid
            ) { _ in
                completionHandler(.newData)
            }
            return
        }

        completionHandler(.noData)
    }

    // Show non-call notifications in foreground
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }

    // Handle Answer/Decline taps on the notification (fallback when app was killed)
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        let additionalData: [String: Any]? = {
            if let custom = userInfo["custom"] as? [String: Any],
               let a = custom["a"] as? [String: Any] {
                return a
            }
            return nil
        }()

        guard response.notification.request.content.categoryIdentifier == "INCOMING_CALL" else {
            completionHandler()
            return
        }

        let channelName = additionalData?["channelName"] as? String ?? ""
        let callerUid = additionalData?["callerUid"] as? String ?? ""
        let callType = additionalData?["callType"] as? String ?? "video"

        switch response.actionIdentifier {
        case "ANSWER_ACTION", UNNotificationDefaultActionIdentifier:
            // User tapped Answer or tapped the notification itself → launch call screen directly
            CallManager.answerCall(
                channelName: channelName,
                callType: callType,
                callerUid: callerUid
            )

        case "DECLINE_ACTION", UNNotificationDismissActionIdentifier:
            // User tapped Decline or swiped away → clear call from Firebase
            let parts = channelName.split(separator: "_")
            let myUid = parts.first(where: { String($0) != callerUid }).map(String.init) ?? ""
            if !myUid.isEmpty {
                CallSignaling().clearCall(uid: myUid)
            }

        default:
            break
        }

        completionHandler()
    }
}

// MARK: - OneSignal Foreground Lifecycle Listener
extension AppDelegate: OSNotificationLifecycleListener {

    func onWillDisplay(event: OSNotificationWillDisplayEvent) {
        let additionalData = event.notification.additionalData
        let notificationType = additionalData?["notificationType"] as? String

        if notificationType == "incoming_call" {
            // Prevent OneSignal from showing the banner — we'll use CallKit instead
            event.preventDefault()

            let callerEmail = additionalData?["callerEmail"] as? String ?? "Unknown Caller"
            let channelName = additionalData?["channelName"] as? String ?? ""
            let callType = additionalData?["callType"] as? String ?? "video"
            let callerUid = additionalData?["callerUid"] as? String ?? ""

            CallManager.shared.reportIncomingCall(
                callerEmail: callerEmail,
                channelName: channelName,
                callType: callType,
                callerUid: callerUid
            ) { error in
                if error != nil {
                    // Fallback: show as regular notification if CallKit fails
                    event.notification.display()
                }
            }
        }
        // Non-call notifications display normally
    }
}

// MARK: - OneSignal Click Listener
extension AppDelegate: OSNotificationClickListener {

    func onClick(event: OSNotificationClickEvent) {
        let additionalData = event.notification.additionalData
        let notificationType = additionalData?["notificationType"] as? String

        if notificationType == "incoming_call" {
            let channelName = additionalData?["channelName"] as? String ?? ""
            let callType = additionalData?["callType"] as? String ?? "video"
            let callerUid = additionalData?["callerUid"] as? String ?? ""

            CallManager.answerCall(
                channelName: channelName,
                callType: callType,
                callerUid: callerUid
            )
        }
    }
}
