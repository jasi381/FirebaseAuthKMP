package dev.jasmeetsingh.firebaseauth.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Indigo    = Color(0xFF6366F1)
private val SlateEdge = Color(0xFF334155)
private val SlateText = Color(0xFFE2E8F0)
private val ErrorRed  = Color(0xFFFC8181)

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 14.sp) },
        singleLine = true,
        isError = isError,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword && !passwordVisible)
            PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "🙈" else "👁",
                        fontSize = 16.sp
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor    = Indigo,
            unfocusedBorderColor  = SlateEdge,
            focusedLabelColor     = Indigo,
            unfocusedLabelColor   = Color(0xFF64748B),
            cursorColor           = Indigo,
            focusedTextColor      = SlateText,
            unfocusedTextColor    = SlateText,
            errorBorderColor      = ErrorRed,
            errorLabelColor       = ErrorRed,
            errorCursorColor      = ErrorRed,
        )
    )
}

@Composable
fun AuthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor         = Indigo,
            disabledContainerColor = Indigo.copy(alpha = 0.45f),
            contentColor           = Color.White,
            disabledContentColor   = Color.White.copy(alpha = 0.6f),
        )
    ) {
        AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
            CircularProgressIndicator(
                color       = Color.White,
                strokeWidth = 2.dp,
                modifier    = Modifier.size(20.dp)
            )
        }
        AnimatedVisibility(visible = !isLoading, enter = fadeIn(), exit = fadeOut()) {
            Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SlateEdge),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor         = SlateText,
            disabledContentColor = SlateText.copy(alpha = 0.4f),
        )
    ) {
        Text("G", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF4285F4))
        Text("  Continue with Google", fontSize = 15.sp)
    }
}

@Composable
fun ErrorMessage(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        if (message != null) {
            Text(
                text      = "⚠ $message",
                color     = ErrorRed,
                fontSize  = 13.sp,
            )
        }
    }
}

