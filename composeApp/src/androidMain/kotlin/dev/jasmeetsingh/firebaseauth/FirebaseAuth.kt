package dev.jasmeetsingh.firebaseauth




import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

actual class FirebaseAuth actual constructor() {

    private val auth = FirebaseAuth.getInstance()

    actual fun currentUser(): Flow<AuthUser?> = callbackFlow {
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toAuthUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    actual suspend fun signUpWithEmail(email: String, password: String): AuthUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user?.toAuthUser() ?: error("No user returned after sign up")
    }

    actual suspend fun signInWithEmail(email: String, password: String): AuthUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user?.toAuthUser() ?: error("No user returned after sign in")
    }

    actual suspend fun signOut() {
        auth.signOut()
    }

    actual suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
        isEmailVerified = isEmailVerified
    )
}