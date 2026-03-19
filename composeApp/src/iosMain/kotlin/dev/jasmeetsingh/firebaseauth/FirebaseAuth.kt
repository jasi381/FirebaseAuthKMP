package dev.jasmeetsingh.firebaseauth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIApplication
import swiftPMImport.dev.jasmeetsingh.composeApp.FIRAuth
import swiftPMImport.dev.jasmeetsingh.composeApp.FIRGoogleAuthProvider
import swiftPMImport.dev.jasmeetsingh.composeApp.FIRUser
import swiftPMImport.dev.jasmeetsingh.composeApp.GIDSignIn
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
actual class FirebaseAuth actual constructor() {

    private val auth = FIRAuth.auth()

    actual fun currentUser(): Flow<AuthUser?> = callbackFlow {
        val handle = auth.addAuthStateDidChangeListener { _, user ->
            trySend(user?.toAuthUser())
        }
        awaitClose { auth.removeAuthStateDidChangeListener(handle) }
    }

    actual suspend fun signUpWithEmail(email: String, password: String): AuthUser =
        suspendCancellableCoroutine { cont ->
            auth.createUserWithEmail(email, password = password) { result, error ->
                if (error != null) cont.resumeWithException(Exception(error.localizedDescription))
                else cont.resume(result!!.user.toAuthUser())
            }
        }

    actual suspend fun signInWithEmail(email: String, password: String): AuthUser =
        suspendCancellableCoroutine { cont ->
            auth.signInWithEmail(email, password = password) { result, error ->
                if (error != null) cont.resumeWithException(Exception(error.localizedDescription))
                else cont.resume(result!!.user.toAuthUser())
            }
        }

    @Suppress("DEPRECATION")
    actual suspend fun signInWithGoogle(): AuthUser =
        suspendCancellableCoroutine { cont ->
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (rootViewController == null) {
                cont.resumeWithException(Exception("No root view controller found"))
                return@suspendCancellableCoroutine
            }

            GIDSignIn.sharedInstance.signInWithPresentingViewController(rootViewController) { signInResult, error ->
                if (error != null) {
                    cont.resumeWithException(Exception(error.localizedDescription))
                    return@signInWithPresentingViewController
                }

                val googleUser = signInResult?.user
                val idToken = googleUser?.idToken?.tokenString
                val accessToken = googleUser?.accessToken?.tokenString

                if (idToken == null) {
                    cont.resumeWithException(Exception("Failed to get Google ID token"))
                    return@signInWithPresentingViewController
                }

                val credential = FIRGoogleAuthProvider.credentialWithIDToken(
                    idToken,
                    accessToken = accessToken ?: ""
                )

                auth.signInWithCredential(credential) { authResult, authError ->
                    if (authError != null) {
                        cont.resumeWithException(Exception(authError.localizedDescription))
                    } else {
                        cont.resume(authResult!!.user.toAuthUser())
                    }
                }
            }
        }

    actual suspend fun signOut() {
        auth.signOut(null)
    }

    actual suspend fun sendPasswordResetEmail(email: String): Unit =
        suspendCancellableCoroutine { cont ->
            auth.sendPasswordResetWithEmail(email) { error ->
                if (error != null) cont.resumeWithException(Exception(error.localizedDescription))
                else cont.resume(Unit)
            }
        }

    private fun FIRUser.toAuthUser() = AuthUser(
        uid             = uid,
        email           = email,
        displayName     = displayName,
        isEmailVerified = isEmailVerified
    )
}
