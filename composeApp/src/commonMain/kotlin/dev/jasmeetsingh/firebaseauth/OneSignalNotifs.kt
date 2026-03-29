package dev.jasmeetsingh.firebaseauth

expect class OneSignalNotifs() {

    fun login(userId: String)
    fun logout()
}