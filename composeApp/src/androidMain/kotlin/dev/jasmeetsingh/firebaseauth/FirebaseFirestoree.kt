package dev.jasmeetsingh.firebaseauth

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

actual class FirebaseFirestoree actual constructor(){

    private val db = FirebaseFirestore.getInstance()

    actual suspend fun  saveUser(uid:String,data:Map<String,Any>){
        db.collection("users").document(uid).set(data).await()

    }

    actual suspend fun getAllUsers():List<Map<String,Any>> {
        val snapshot = db.collection("users").get().await()
        return snapshot.documents.mapNotNull { it.data }
    }


}