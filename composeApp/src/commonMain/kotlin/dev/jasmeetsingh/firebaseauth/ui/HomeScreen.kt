package dev.jasmeetsingh.firebaseauth.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jasmeetsingh.composeapp.generated.resources.Res
import dev.jasmeetsingh.composeapp.generated.resources.ic_logout
import dev.jasmeetsingh.firebaseauth.AuthUser
import org.jetbrains.compose.resources.vectorResource

private val DarkBg = Color(0xFF0F0F1A)
private val DarkSurface = Color(0xFF1A1A2E)
private val Indigo = Color(0xFF6366F1)
private val Purple = Color(0xFF8B5CF6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    user: AuthUser,
    allUsers: List<Map<String, Any>>,
    onSignOut: () -> Unit,
) {
    val initial = (user.displayName?.firstOrNull() ?: user.email?.firstOrNull() ?: '?')
        .uppercaseChar()

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = Color(0xFFF1F5F9),
                    actionIconContentColor = Color(0xFFF1F5F9),
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Current user avatar
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Indigo, Purple))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                initial.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.width(12.dp))


                            user.email?.let {
                                Text(
                                    it,
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                    }
                },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_logout),
                            contentDescription = "Sign Out"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                "All Users",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF1F5F9),
            )
            Text(
                "${allUsers.size} registered users",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
            )

            Spacer(Modifier.height(12.dp))

            if (allUsers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No other users yet", fontSize = 15.sp, color = Color(0xFF475569))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(allUsers) { userData ->
                        UserCard(userData)
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun UserCard(userData: Map<String, Any>) {
    val email = userData["email"]?.toString() ?: "No email"
    val uid = userData["uid"]?.toString() ?: ""
    val cardInitial = (email.firstOrNull() ?: '?').uppercaseChar()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Indigo.copy(alpha = 0.7f), Purple.copy(alpha = 0.7f)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                cardInitial.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                email,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE2E8F0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                uid,
                fontSize = 11.sp,
                color = Color(0xFF475569),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}