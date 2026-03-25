package dev.jasmeetsingh.firebaseauth


const val AGORA_APP_ID = "5e4e545a232f41269b29f258e3c1b32c"

expect class CallService() {
    fun startVideoCall(channelName: String)
    fun startAudioCall(channelName: String)
}