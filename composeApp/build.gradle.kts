import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.google.services)
}

group = "dev.jasmeetsingh"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),             // Real device
        iosSimulatorArm64()     // Apple Silicon simulator
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            linkerOpts("-ld_classic") // Fix for Xcode 16 new linker on Sequoia
        }
    }
    swiftPMDependencies {
        swiftPackage(
            url = url("https://github.com/firebase/firebase-ios-sdk.git"),
            version = from("11.0.0"),
            products = listOf(
                product("FirebaseAuth"),
                product("FirebaseCore"),
                product("FirebaseFirestore"),
                product("FirebaseDatabase")
            )
        )
        swiftPackage(
            url = url("https://github.com/google/GoogleSignIn-iOS.git"),
            version = from("8.0.0"),
            products = listOf(
                product("GoogleSignIn")
            )
        )
        swiftPackage(
            url = url("https://github.com/AgoraIO/AgoraRtcEngine_iOS.git"),
            version = from("4.6.2"),
            products = listOf(product("RtcBasic"))
        )
    }


    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.auth.android)
            implementation(libs.firebase.firestore.android)
            implementation(libs.firebase.database.android)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services)
            implementation(libs.google.id)
            implementation("io.agora.rtc:full-sdk:4.6.2")
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0-beta01")

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "dev.jasmeetsingh.firebaseauth"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.jasmeetsingh.firebaseauth"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

