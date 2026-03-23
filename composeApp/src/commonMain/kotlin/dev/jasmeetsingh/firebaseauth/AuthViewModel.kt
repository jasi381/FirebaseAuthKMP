package dev.jasmeetsingh.firebaseauth


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading       : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: AuthUser) : AuthState()
    data class Error(val message: String)        : AuthState()
}

class AuthViewModel : ViewModel() {

    // ─── Instantiate expect class directly — no injection needed ─────────────
    private val auth = FirebaseAuth()
    private val db = FirebaseFirestoree()

    // ─── Auth state driven by Firebase's own persistence ─────────────────────

    val authState: StateFlow<AuthState> = auth.currentUser()
        .map { user ->
            if (user != null) AuthState.Authenticated(user)
            else AuthState.Unauthenticated
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthState.Loading
        )

    // ─── Form state ───────────────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ─── Actions ──────────────────────────────────────────────────────────────
    fun signUpWithEmail(email: String, password: String) = launchAuth {
        val user = auth.signUpWithEmail(email, password)
        db.saveUser(
            uid = user.uid,
            data = mapOf("uid" to user.uid, "email" to (user.email ?: ""))
        )
        user
    }

    fun signInWithEmail(email: String, password: String) = launchAuth {
        auth.signInWithEmail(email, password)
    }

    fun signInWithGoogle() = launchAuth {
        val user = auth.signInWithGoogle()
        db.saveUser(
            uid = user.uid,
            data = mapOf("uid" to user.uid, "email" to (user.email ?: ""))
        )
        user
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { auth.signOut() }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { auth.sendPasswordResetEmail(email) }
                .onSuccess  { _errorMessage.value = "Reset email sent!" }
                .onFailure  { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    private val _allUsers = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val allUsers: StateFlow<List<Map<String, Any>>> = _allUsers.asStateFlow()

    fun loadAllUsers(currentUid: String) {
        viewModelScope.launch {
            runCatching { db.getAllUsers() }
                .onSuccess { users ->
                    _allUsers.value = users.filter { it["uid"] != currentUid }
                }
        }
    }

    fun clearError() { _errorMessage.value = null }

    // ─── Helper ───────────────────────────────────────────────────────────────
    private fun launchAuth(block: suspend () -> AuthUser) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            runCatching { block() }
                .onFailure { _errorMessage.value = it.message ?: "Something went wrong" }
            _isLoading.value = false
        }
    }
}