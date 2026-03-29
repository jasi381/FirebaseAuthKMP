package dev.jasmeetsingh.firebaseauth

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class IncomingCallActivity : ComponentActivity() {

    private lateinit var channelName: String
    private var callType: String = "video"
    private var callerUid: String = ""
    private var callerEmail: String = "Unknown"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show on lock screen + turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        channelName = intent.getStringExtra("channel") ?: run { finish(); return }
        callType = intent.getStringExtra("callType") ?: "video"
        callerUid = intent.getStringExtra("callerUid") ?: ""
        callerEmail = intent.getStringExtra("callerEmail") ?: "Unknown"

        setContent {
            IncomingCallScreen(
                callerName = callerEmail,
                callType = callType,
                onAnswer = { answerCall() },
                onDecline = { declineCall() },
            )
        }
    }

    private fun answerCall() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(CallActionReceiver.NOTIFICATION_ID)

        startActivity(Intent(this, CallActivity::class.java).apply {
            putExtra("channel", channelName)
            putExtra("isVideo", callType == "video")
            putExtra("fromNotification", true)
            putExtra("callerUid", callerUid)
        })
        finish()
    }

    private fun declineCall() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(CallActionReceiver.NOTIFICATION_ID)

        val parts = channelName.split("_")
        val myUid = parts.firstOrNull { it != callerUid } ?: ""
        if (myUid.isNotEmpty()) {
            CallSignaling().clearCall(myUid)
        }
        finish()
    }
}

@Composable
fun IncomingCallScreen(
    callerName: String,
    callType: String,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2D2D44)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = callerName.take(1).uppercase(),
                    fontSize = 40.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = callerName,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            val label =
                if (callType == "video") "Incoming video call..." else "Incoming audio call..."
            Text(
                text = label,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallActionButton(
                    icon = painterResource(R.drawable.ic_call_end),
                    label = "Decline",
                    containerColor = Color(0xFFE53935),
                    onClick = onDecline
                )

                CallActionButton(
                    icon = painterResource(R.drawable.ic_call_answer),
                    label = "Answer",
                    containerColor = Color(0xFF43A047),
                    onClick = onAnswer
                )
            }
        }
    }
}

@Composable
fun CallActionButton(
    icon: Painter,
    label: String,
    containerColor: Color,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(containerColor)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
