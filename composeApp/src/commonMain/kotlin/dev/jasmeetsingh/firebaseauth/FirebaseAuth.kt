package dev.jasmeetsingh.firebaseauth



import kotlinx.coroutines.flow.Flow

expect class FirebaseAuth() {

    /** Emits current user on auth state changes. Persists across app launches. */
    fun currentUser(): Flow<AuthUser?>

    suspend fun signUpWithEmail(email: String, password: String): AuthUser

    suspend fun signInWithEmail(email: String, password: String): AuthUser

    suspend fun signOut()

    suspend fun sendPasswordResetEmail(email: String)
}