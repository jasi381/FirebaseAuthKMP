package dev.jasmeetsingh.firebaseauth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jasmeetsingh.composeapp.generated.resources.Res
import dev.jasmeetsingh.composeapp.generated.resources.compose_multiplatform
import dev.jasmeetsingh.firebaseauth.ui.HomeScreen
import dev.jasmeetsingh.firebaseauth.ui.LoginScreen
import dev.jasmeetsingh.firebaseauth.ui.SignUpScreen
import org.jetbrains.compose.resources.painterResource

//@Composable
//@Preview
//fun App() {
//    MaterialTheme {
//        var showContent by remember { mutableStateOf(false) }
//        Column(
//            modifier = Modifier
//                .background(MaterialTheme.colorScheme.primaryContainer)
//                .safeContentPadding()
//                .fillMaxSize(),
//            horizontalAlignment = Alignment.CenterHorizontally,
//        ) {
//            Button(onClick = { showContent = !showContent }) {
//                Text("Click me!")
//            }
//            AnimatedVisibility(showContent) {
//                val greeting = remember { Greeting().greet() }
//                Column(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                ) {
//                    Image(painterResource(Res.drawable.compose_multiplatform), null)
//                    Text("Compose: $greeting")
//                }
//            }
//        }
//    }
//}

enum class Screen { LOGIN, SIGNUP, HOME }

@Composable
fun App() {
    val viewModel: AuthViewModel = viewModel { AuthViewModel() }
    val authState    by viewModel.authState.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.LOGIN) }

    when (authState) {
        is AuthState.Authenticated -> {
            HomeScreen(
                user = (authState as AuthState.Authenticated).user,
                onSignOut = { viewModel.signOut() }
            )
        }

        is AuthState.Unauthenticated, is AuthState.Error, is AuthState.Loading -> {
            when (currentScreen) {
                Screen.LOGIN -> LoginScreen(
                    isLoading          = isLoading,
                    errorMessage       = errorMessage,
                    onSignIn           = { email, password -> viewModel.signInWithEmail(email, password) },
                    onForgotPassword   = { email -> viewModel.sendPasswordResetEmail(email) },
                    onNavigateToSignUp = { currentScreen = Screen.SIGNUP },
                    onClearError       = { viewModel.clearError() },
                    onGoogleSignIn = { viewModel.signInWithGoogle() }
                )

                Screen.SIGNUP -> SignUpScreen(
                    isLoading          = isLoading,
                    errorMessage       = errorMessage,
                    onSignUp           = { email, password -> viewModel.signUpWithEmail(email, password) },
                    onNavigateToLogin  = { currentScreen = Screen.LOGIN },
                    onClearError       = { viewModel.clearError() }
                )

                Screen.HOME -> Unit // unreachable
            }
        }
    }
}
