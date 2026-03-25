package dev.jasmeetsingh.firebaseauth

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual class CallSignaling actual constructor() {

    private val db = FirebaseDatabase.getInstance().reference.child("calls")

    actual fun sendCallInvite(
        targetUid: String,
        callerUid: String,
        callerEmail: String,
        channelName: String,
        callType: String,
    ) {
        val invite = mapOf(
            "callerUid" to callerUid,
            "callerEmail" to callerEmail,
            "channelName" to channelName,
            "callType" to callType,
            "timestamp" to System.currentTimeMillis(),
            "status" to "ringing",
        )
        db.child(targetUid).setValue(invite)
    }

    actual fun observeIncomingCall(myUid: String): Flow<CallInvite?> = callbackFlow {
        val ref = db.child(myUid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }
                val data = snapshot.value as? Map<*, *> ?: run { trySend(null); return }
                val invite = CallInvite(
                    callerUid = data["callerUid"]?.toString() ?: "",
                    callerEmail = data["callerEmail"]?.toString() ?: "",
                    channelName = data["channelName"]?.toString() ?: "",
                    callType = data["callType"]?.toString() ?: "video",
                    timestamp = (data["timestamp"] as? Long) ?: 0L,
                )
                val status = data["status"]?.toString() ?: ""
                if (status == "ringing") {
                    trySend(invite)
                } else {
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    actual fun acceptCall(myUid: String) {
        db.child(myUid).child("status").setValue("accepted")
    }

    actual fun clearCall(uid: String) {
        db.child(uid).removeValue()
    }
}
