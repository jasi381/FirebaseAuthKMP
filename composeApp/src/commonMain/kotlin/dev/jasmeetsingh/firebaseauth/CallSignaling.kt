package dev.jasmeetsingh.firebaseauth

import kotlinx.coroutines.flow.Flow

data class CallInvite(
    val callerUid: String = "",
    val callerEmail: String = "",
    val channelName: String = "",
    val callType: String = "video", // "video" or "audio"
    val timestamp: Long = 0L,
)

expect class CallSignaling() {
    /** Send a call invite to [targetUid]. */
    fun sendCallInvite(
        targetUid: String,
        callerUid: String,
        callerEmail: String,
        channelName: String,
        callType: String,
    )

    /** Listen for incoming calls for [myUid]. Emits null when no active invite. */
    fun observeIncomingCall(myUid: String): Flow<CallInvite?>

    /** Receiver accepted — update status so caller knows. */
    fun acceptCall(myUid: String)

    /** Reject / cancel / cleanup the invite for [uid]. */
    fun clearCall(uid: String)
}
