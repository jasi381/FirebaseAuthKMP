import UserNotifications
import OneSignalExtension

class NotificationService: UNNotificationServiceExtension {

    var contentHandler: ((UNNotificationContent) -> Void)?
    var receivedRequest: UNNotificationRequest!
    var bestAttemptContent: UNMutableNotificationContent?

    override func didReceive(_ request: UNNotificationRequest, withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void) {
        self.receivedRequest = request
        self.contentHandler = contentHandler
        self.bestAttemptContent = (request.content.mutableCopy() as? UNMutableNotificationContent)

        if let bestAttemptContent = bestAttemptContent {
            let userInfo = bestAttemptContent.userInfo

            // Extract notificationType from the "custom" > "a" dict (OneSignal additional data)
            let notificationType: String? = {
                if let custom = userInfo["custom"] as? [String: Any],
                   let additional = custom["a"] as? [String: Any],
                   let type = additional["notificationType"] as? String {
                    return type
                }
                return nil
            }()

            if notificationType == "incoming_call" {
                let callerEmail: String = {
                    if let custom = userInfo["custom"] as? [String: Any],
                       let a = custom["a"] as? [String: Any],
                       let email = a["callerEmail"] as? String {
                        return email
                    }
                    return "Unknown Caller"
                }()

                let callType: String = {
                    if let custom = userInfo["custom"] as? [String: Any],
                       let a = custom["a"] as? [String: Any],
                       let type = a["callType"] as? String {
                        return type
                    }
                    return "video"
                }()

                let callLabel = callType == "video" ? "Video" : "Audio"
                bestAttemptContent.title = "Incoming \(callLabel) Call"
                bestAttemptContent.body = "\(callerEmail) is calling you"
                bestAttemptContent.sound = UNNotificationSound.defaultCritical
                bestAttemptContent.interruptionLevel = .timeSensitive
                bestAttemptContent.categoryIdentifier = "INCOMING_CALL"

                contentHandler(bestAttemptContent)
                return
            }

            // For all other notifications, let OneSignal process them
            OneSignalExtension.didReceiveNotificationExtensionRequest(self.receivedRequest, with: bestAttemptContent, withContentHandler: self.contentHandler)
        }
    }

    override func serviceExtensionTimeWillExpire() {
        if let contentHandler = contentHandler, let bestAttemptContent = bestAttemptContent {
            OneSignalExtension.serviceExtensionTimeWillExpireRequest(self.receivedRequest, with: self.bestAttemptContent)
            contentHandler(bestAttemptContent)
        }
    }
}
