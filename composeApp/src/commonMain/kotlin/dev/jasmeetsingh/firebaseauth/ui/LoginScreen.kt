package dev.jasmeetsingh.firebaseauth.ui


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onSignIn: (email: String, password: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onClearError: () -> Unit,
) {
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0F0F1A), Color(0xFF1A1A2E)))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))

            // ── Header ────────────────────────────────────────────────────────
            AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically { -40 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔐", fontSize = 52.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Welcome back", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                    Spacer(Modifier.height(6.dp))
                    Text("Sign in to continue", fontSize = 15.sp, color = Color(0xFF64748B))
                }
            }

            Spacer(Modifier.height(44.dp))

            // ── Form ──────────────────────────────────────────────────────────
            AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically { 60 }) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    AuthTextField(
                        value         = email,
                        onValueChange = { email = it; onClearError() },
                        label         = "Email",
                        keyboardType  = KeyboardType.Email,
                        isError       = errorMessage != null,
                    )

                    AuthTextField(
                        value         = password,
                        onValueChange = { password = it; onClearError() },
                        label         = "Password",
                        isPassword    = true,
                        isError       = errorMessage != null,
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            text     = "Forgot password?",
                            color    = Color(0xFF6366F1),
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { if (email.isNotBlank()) onForgotPassword(email) }
                        )
                    }

                    ErrorMessage(errorMessage)

                    AuthButton(
                        text      = "Sign In",
                        onClick   = { onSignIn(email, password) },
                        isLoading = isLoading,
                        enabled   = email.isNotBlank() && password.isNotBlank(),
                    )

                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        HorizontalDivider(Modifier.weight(1f), color = Color(0xFF1E293B))
                        Text("or", fontSize = 13.sp, color = Color(0xFF475569))
                        HorizontalDivider(Modifier.weight(1f), color = Color(0xFF1E293B))
                    }

                    GoogleSignInButton(onClick = onGoogleSignIn, isLoading = isLoading)
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Footer ────────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Don't have an account?", color = Color(0xFF64748B), fontSize = 14.sp)
                TextButton(onClick = onNavigateToSignUp) {
                    Text("Sign Up", color = Color(0xFF6366F1), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}