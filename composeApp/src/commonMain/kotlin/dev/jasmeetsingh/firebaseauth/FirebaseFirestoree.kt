package dev.jasmeetsingh.firebaseauth

import dev.jasmeetsingh.composeapp.generated.resources.Res

expect  class FirebaseFirestoree (){
    suspend fun saveUser(uid:String, data:Map<String, Any>)
    suspend fun getAllUsers():List<Map<String,Any>>
}