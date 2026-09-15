package com.example.ui.auth

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AdminAccessResult
import com.example.data.auth.AuthManager
import com.example.data.model.UserProfile
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary

/**
 * Authentication Logic Guard
 *
 * Verifies if the current signed-in user strictly matches 'souravbrock@gmail.com'
 * before rendering the admin dashboard component.
 *
 * If unauthorized, the admin dashboard component is NEVER rendered. Instead,
 * an Unauthorized Access Security Screen is displayed with a return-to-store option.
 */
@Composable
fun AdminAuthGuard(
    currentUser: UserProfile,
    onNavigateBackToCustomer: () -> Unit,
    adminContent: @Composable () -> Unit
) {
    val accessResult = when {
        !currentUser.isGoogleSignedIn -> AdminAccessResult.Unauthenticated
        currentUser.email.trim().equals(AuthManager.ADMIN_EMAIL, ignoreCase = true) -> AdminAccessResult.Granted(currentUser)
        else -> AdminAccessResult.Denied(
            userEmail = currentUser.email,
            message = "Your signed-in account (${currentUser.email}) does not have administrative privileges."
        )
    }

    when (accessResult) {
        is AdminAccessResult.Granted -> {
            // Strictly verified: user is souravbrock@gmail.com -> render the Admin Dashboard component
            adminContent()
        }
        is AdminAccessResult.Denied -> {
            // Refuse rendering admin dashboard component; display security barrier
            UnauthorizedAdminScreen(
                attemptedEmail = accessResult.userEmail,
                onReturnToStore = onNavigateBackToCustomer
            )
        }
        is AdminAccessResult.Unauthenticated -> {
            // Refuse rendering admin dashboard component
            UnauthenticatedAdminScreen(
                onReturnToStore = onNavigateBackToCustomer
            )
        }
    }
}

@Composable
private fun UnauthorizedAdminScreen(
    attemptedEmail: String,
    onReturnToStore: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Access Locked",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "Admin Access Restricted",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color(0xFFD32F2F)
                    )

                    Text(
                        text = "The Store Admin Console is protected by strict role-based authentication.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Security Audit Verification",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Text(
                                text = "Current Signed-In Email: $attemptedEmail",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFBF360C)
                            )
                            Text(
                                text = "Authorized Admin Email: ${AuthManager.ADMIN_EMAIL}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Text(
                        text = "Customers and residents can freely browse the catalog, place orders, and track deliveries. Admin privileges (price modifications, stock updates, notification dispatch) are reserved exclusively for the store owner.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onReturnToStore,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Return to Customer Storefront", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun UnauthenticatedAdminScreen(
    onReturnToStore: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEDE7F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Sign In Required",
                            tint = Color(0xFF512DA8),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "Sign-In Required",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                    )

                    Text(
                        text = "Please sign in with your Google Account (${AuthManager.ADMIN_EMAIL}) to access the admin dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onReturnToStore,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Go to Sign-In", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
