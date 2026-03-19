package dev.jasmeetsingh.firebaseauth.ui



import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jasmeetsingh.firebaseauth.AuthUser


@Composable
fun HomeScreen(
    user: AuthUser,
    onSignOut: () -> Unit,
) {
    val initial = (user.displayName?.firstOrNull() ?: user.email?.firstOrNull() ?: '?')
        .uppercaseChar()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F0F1A), Color(0xFF1A1A2E)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ── Avatar ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(initial.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(24.dp))

            Text("Welcome! 👋", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))

            Spacer(Modifier.height(8.dp))

            user.displayName?.let {
                Text(it, fontSize = 18.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
            }

            user.email?.let {
                Text(it, fontSize = 14.sp, color = Color(0xFF64748B))
            }

            Spacer(Modifier.height(16.dp))

            // ── Email verification badge ──────────────────────────────────────
            if (user.isEmailVerified) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF0F3326))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text("✓ Email verified", fontSize = 12.sp, color = Color(0xFF10B981))
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF2D1B00))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text("⚠ Email not verified", fontSize = 12.sp, color = Color(0xFFF59E0B))
                }
            }

            Spacer(Modifier.height(48.dp))

            AuthButton(text = "Sign Out", onClick = onSignOut)
        }
    }
}