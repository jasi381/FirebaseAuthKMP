// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_internal_linkage_SwiftPMImport",
  platforms: [
    .iOS("15.0")
  ],
  products: [
    .library(
      name: "_internal_linkage_SwiftPMImport",
      type: .none,
      targets: ["_internal_linkage_SwiftPMImport"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/firebase/firebase-ios-sdk.git",
      from: "11.0.0",
    ),
    .package(
      url: "https://github.com/google/GoogleSignIn-iOS.git",
      from: "8.0.0",
    ),
    .package(
      url: "https://github.com/AgoraIO/AgoraRtcEngine_iOS.git",
      from: "4.6.2",
    )
  ],
  targets: [
    .target(
      name: "_internal_linkage_SwiftPMImport",
      dependencies: [
        .product(
          name: "FirebaseAuth",
          package: "firebase-ios-sdk",
        ),
        .product(
          name: "FirebaseCore",
          package: "firebase-ios-sdk",
        ),
        .product(
          name: "FirebaseFirestore",
          package: "firebase-ios-sdk",
        ),
        .product(
          name: "GoogleSignIn",
          package: "GoogleSignIn-iOS",
        ),
        .product(
          name: "RtcBasic",
          package: "AgoraRtcEngine_iOS",
        )
      ]
    )
  ]
)
