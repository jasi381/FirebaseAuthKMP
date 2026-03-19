import SwiftUI
import FirebaseCore
import GoogleSignIn

@main
struct iOSApp: App {

    init(){
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
