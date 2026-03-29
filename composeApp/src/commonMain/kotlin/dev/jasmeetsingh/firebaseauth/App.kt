package dev.jasmeetsingh.firebaseauth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jasmeetsingh.firebaseauth.ui.HomeScreen
import dev.jasmeetsingh.firebaseauth.ui.IncomingCallOverlay
import dev.jasmeetsingh.firebaseauth.ui.LoginScreen
import dev.jasmeetsingh.firebaseauth.ui.SignUpScreen
import kotlinx.coroutines.launch

enum class Screen { LOGIN, SIGNUP, HOME }

@Composable
fun App() {
    val viewModel: AuthViewModel = viewModel { AuthViewModel() }
    val authState    by viewModel.authState.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val callService = remember { CallService() }
    val callSignaling = remember { CallSignaling() }
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(Screen.LOGIN) }

    // Incoming call state — observed globally
    var incomingCall by remember { mutableStateOf<CallInvite?>(null) }

    // Start listening for incoming calls when authenticated
    val currentUser = (authState as? AuthState.Authenticated)?.user
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        callSignaling.observeIncomingCall(uid).collect { invite ->
            incomingCall = invite
        }
    }

    // Auto-timeout: clear invite after 30 seconds
    LaunchedEffect(incomingCall) {
        val invite = incomingCall ?: return@LaunchedEffect
        kotlinx.coroutines.delay(30_000)
        // If still the same invite after 30s, auto-reject
        if (incomingCall?.timestamp == invite.timestamp) {
            callSignaling.clearCall(currentUser?.uid ?: "")
            incomingCall = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Main app content
        when (authState) {
            is AuthState.Authenticated -> {
                val user = (authState as AuthState.Authenticated).user
                val allUsers by viewModel.allUsers.collectAsState()

                LaunchedEffect(user.uid) {
                    viewModel.loadAllUsers(user.uid)
                }

                HomeScreen(
                    user = user,
                    allUsers = allUsers,
                    onSignOut = { viewModel.signOut() },
                    onVideoCall = { targetUid ->
                        val channel = listOf(user.uid, targetUid).sorted().joinToString("_")
                        callSignaling.sendCallInvite(
                            targetUid = targetUid,
                            callerUid = user.uid,
                            callerEmail = user.email ?: "",
                            channelName = channel,
                            callType = "video",
                        )
                        callService.startVideoCall(channel)
                        scope.launch {
                            OneSignalApi.sendCallPush(
                                targetUid,
                                user.uid,
                                user.email ?: "",
                                channel,
                                "video"
                            )
                        }
                    },
                    onAudioCall = { targetUid ->
                        val channel = listOf(user.uid, targetUid).sorted().joinToString("_")
                        callSignaling.sendCallInvite(
                            targetUid = targetUid,
                            callerUid = user.uid,
                            callerEmail = user.email ?: "",
                            channelName = channel,
                            callType = "audio",
                        )
                        callService.startAudioCall(channel)
                        scope.launch {
                            OneSignalApi.sendCallPush(
                                targetUid,
                                user.uid,
                                user.email ?: "",
                                channel,
                                "audio"
                            )
                        }
                    },
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
                        onGoogleSignIn     = { viewModel.signInWithGoogle() }
                    )

                    Screen.SIGNUP -> SignUpScreen(
                        isLoading          = isLoading,
                        errorMessage       = errorMessage,
                        onSignUp           = { email, password -> viewModel.signUpWithEmail(email, password) },
                        onNavigateToLogin  = { currentScreen = Screen.LOGIN },
                        onClearError       = { viewModel.clearError() }
                    )

                    Screen.HOME -> Unit
                }
            }
        }

        // Global incoming call overlay — renders ON TOP of everything
        incomingCall?.let { invite ->
            IncomingCallOverlay(
                invite = invite,
                onAccept = {
                    val myUid = currentUser?.uid ?: return@IncomingCallOverlay
                    callSignaling.acceptCall(myUid)
                    incomingCall = null
                    // Join the same Agora channel
                    if (invite.callType == "video") {
                        callService.startVideoCall(invite.channelName)
                    } else {
                        callService.startAudioCall(invite.channelName)
                    }
                    // Clean up after a short delay to let caller see "accepted"
                    callSignaling.clearCall(myUid)
                },
                onReject = {
                    val myUid = currentUser?.uid ?: return@IncomingCallOverlay
                    callSignaling.clearCall(myUid)
                    incomingCall = null
                },
            )
        }
    }
}
