package dev.jasmeetsingh.firebaseauth

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

object OneSignalApi {

    private const val TAG = "OneSignalApi"
    private const val APP_ID = "778b2d15-8416-423e-bb23-1fc7778445e8"
    private const val API_KEY =
        "Key os_v2_app_o6fs2fmeczbd5ozdd7dxpbcf5awsjv4r7nvus3uw5efu5hod5v3hqnrtz6mum6c5n7b43t4tybhhnaty77gmbloyw67vx2alkkmtzla"
    private const val URL = "https://api.onesignal.com/notifications"

    private val client = HttpClient()

    suspend fun sendCallPush(
        targetUid: String,
        callerUid: String,
        callerEmail: String,
        channelName: String,
        callType: String,
    ) {
        val body = """
            {
                "app_id": "$APP_ID",
                "include_aliases": {
                    "external_id": ["$targetUid"]
                },
                "target_channel": "push",
                "headings": {"en": "Incoming Call"},
                "contents": {"en": "$callerEmail is calling you"},
                "data": {
                    "notificationType": "incoming_call",
                    "channelName": "$channelName",
                    "callType": "$callType",
                    "callerUid": "$callerUid",
                    "callerEmail": "$callerEmail"
                },
                "content_available": true,
                "mutable_content": true
            }
        """.trimIndent()

        Napier.d(tag = TAG, message = "=== OneSignal Push Request ===")
        Napier.d(tag = TAG, message = "URL: $URL")
        Napier.d(tag = TAG, message = "Target UID: $targetUid")
        Napier.d(tag = TAG, message = "Request Body:\n$body")

        try {
            val response = client.post(URL) {
                contentType(ContentType.Application.Json)
                header("Authorization", API_KEY)
                setBody(body)
            }

            val responseBody = response.bodyAsText()
            Napier.d(tag = TAG, message = "=== OneSignal Push Response ===")
            Napier.d(tag = TAG, message = "Status: ${response.status}")
            Napier.d(tag = TAG, message = "Response Body:\n$responseBody")
        } catch (e: Exception) {
            Napier.e(tag = TAG, message = "=== OneSignal Push FAILED ===")
            Napier.e(tag = TAG, message = "Error: ${e.message}", throwable = e)
        }
    }
}
