package dev.jasmeetsingh.firebaseauth

data class AuthUser(
    val uid:String,
    val email: String?,
    val displayName :String?,
    val isEmailVerified:Boolean = false,
)
