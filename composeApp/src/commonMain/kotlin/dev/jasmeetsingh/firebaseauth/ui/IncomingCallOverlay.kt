package dev.jasmeetsingh.firebaseauth.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jasmeetsingh.composeapp.generated.resources.Res
import dev.jasmeetsingh.composeapp.generated.resources.ic_check
import dev.jasmeetsingh.composeapp.generated.resources.ic_close
import dev.jasmeetsingh.firebaseauth.CallInvite
import org.jetbrains.compose.resources.vectorResource

private val DarkBg = Color(0xFF0F0F1A)
private val Green = Color(0xFF30D158)  // iOS system green
private val Red = Color(0xFFFF3B30)    // iOS system red

@Composable
fun IncomingCallOverlay(
    invite: CallInvite,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Pulsing avatar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                val initial = (invite.callerEmail.firstOrNull() ?: '?').uppercaseChar()
                Text(
                    initial.toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                invite.callerEmail,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                if (invite.callType == "video") "Incoming Video Call..." else "Incoming Audio Call...",
                fontSize = 15.sp,
                color = Color(0xFF94A3B8),
            )

            Spacer(Modifier.height(48.dp))

            // Accept / Reject buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                // Reject
                Button(
                    onClick = onReject,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Red),
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_close),
                        contentDescription = "Decline",
                        modifier = Modifier.size(32.dp),
                        tint = Color.White,
                    )
                }

                Spacer(Modifier.width(48.dp))

                // Accept
                Button(
                    onClick = onAccept,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_check),
                        contentDescription = "Accept",
                        modifier = Modifier.size(32.dp),
                        tint = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("Decline", fontSize = 13.sp, color = Red)
                Spacer(Modifier.width(72.dp))
                Text("Accept", fontSize = 13.sp, color = Green)
            }
        }
    }
}
