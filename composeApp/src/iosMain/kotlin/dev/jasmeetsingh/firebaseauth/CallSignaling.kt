package dev.jasmeetsingh.firebaseauth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import swiftPMImport.dev.jasmeetsingh.composeApp.FIRDatabase
import swiftPMImport.dev.jasmeetsingh.composeApp.FIRDataEventType
import swiftPMImport.dev.jasmeetsingh.composeApp.FIRDatabaseHandle
import swiftPMImport.dev.jasmeetsingh.composeApp.FIRDatabaseReference
import swiftPMImport.dev.jasmeetsingh.composeApp.FIRDataSnapshot

@OptIn(ExperimentalForeignApi::class)
actual class CallSignaling actual constructor() {

    private val db: FIRDatabaseReference = FIRDatabase.database().reference().child("calls")

    actual fun sendCallInvite(
        targetUid: String,
        callerUid: String,
        callerEmail: String,
        channelName: String,
        callType: String,
    ) {
        val invite = mapOf<Any?, Any?>(
            "callerUid" to callerUid,
            "callerEmail" to callerEmail,
            "channelName" to channelName,
            "callType" to callType,
            "timestamp" to (NSDate().timeIntervalSince1970 * 1000).toLong(),
            "status" to "ringing",
        )
        db.child(targetUid).setValue(invite)
    }

    actual fun observeIncomingCall(myUid: String): Flow<CallInvite?> = callbackFlow {
        val ref = db.child(myUid)
        val handle: FIRDatabaseHandle = ref.observeEventType(
            eventType = FIRDataEventType.FIRDataEventTypeValue,
            withBlock = { snapshot ->
                val snap = snapshot as? FIRDataSnapshot
                val data = snap?.value() as? Map<*, *>
                if (data == null) {
                    trySend(null)
                    return@observeEventType
                }
                val status = data["status"]?.toString() ?: ""
                if (status == "ringing") {
                    val invite = CallInvite(
                        callerUid = data["callerUid"]?.toString() ?: "",
                        callerEmail = data["callerEmail"]?.toString() ?: "",
                        channelName = data["channelName"]?.toString() ?: "",
                        callType = data["callType"]?.toString() ?: "video",
                        timestamp = (data["timestamp"] as? Long) ?: 0L,
                    )
                    trySend(invite)
                } else {
                    trySend(null)
                }
            }
        )
        awaitClose { ref.removeObserverWithHandle(handle) }
    }

    actual fun acceptCall(myUid: String) {
        db.child(myUid).child("status").setValue("accepted")
    }

    actual fun clearCall(uid: String) {
        db.child(uid).removeValue()
    }
}
