package dev.jasmeetsingh.firebaseauth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import swiftPMImport.dev.jasmeetsingh.composeApp.FIRAuth
import swiftPMImport.dev.jasmeetsingh.composeApp.FIRUser
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