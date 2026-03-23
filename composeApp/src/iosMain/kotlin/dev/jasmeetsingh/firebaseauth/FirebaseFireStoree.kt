package dev.jasmeetsingh.firebaseauth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import swiftPMImport.dev.jasmeetsingh.composeApp.FIRFirestore
import swiftPMImport.dev.jasmeetsingh.composeApp.FIRQueryDocumentSnapshot
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
actual class FirebaseFirestoree actual constructor() {

    private val db = FIRFirestore.firestore()

    actual suspend fun saveUser(uid:String,data:Map<String,Any>) = suspendCancellableCoroutine { cont ->
        db.collectionWithPath("users")
            .documentWithPath(uid)
            .setData(
                data as Map<Any?, *>,

                completion = { error ->
                    if (error != null) cont.resumeWithException(Exception(error.localizedDescription))
                    else cont.resume(Unit)
                }
            )
    }
    actual suspend fun getAllUsers():List<Map<String, Any>> = suspendCancellableCoroutine { cont ->
        db.collectionWithPath("users")
            .getDocumentsWithCompletion { snapshot, error ->
                if(error != null) cont.resumeWithException(Exception(error.localizedDescription))
                else{
                    val users = snapshot?.documents?.mapNotNull {

                        (it as? FIRQueryDocumentSnapshot)?.data() as? Map<String,Any>
                    }?: emptyList()
                        cont.resume(users)
                }
            }
    }

}


