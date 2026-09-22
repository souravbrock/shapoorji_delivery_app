package com.example.ui.auth

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.auth.AuthManager
import com.example.data.model.OfficialCatalog
import com.example.data.model.UserProfile
import com.example.data.website.EmailOtpState
import com.example.data.website.WebsiteAuthState
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import kotlinx.coroutines.launch

/**
 * Production Welcome & Store Account Sign-In Screen
 *
 * Resident-focused design showcasing daily fresh vegetables and essentials.
 * Entry is gated on the email-verified Store Account (spdelivery.reddevils.co.in).
 */
@Composable
fun WelcomeScreen(
    onGoogleSignIn: (name: String, email: String, phone: String, tower: String, flat: String) -> Unit,
    websiteAuthState: WebsiteAuthState = WebsiteAuthState.SignedOut,
    emailOtpState: EmailOtpState = EmailOtpState.Idle,
    onWebsiteLogin: (email: String, password: String) -> Unit = { _, _ -> },
    onRequestSignupOtp: (email: String) -> Unit = {},
    onConfirmSignupOtp: (
        name: String, email: String, password: String, phone: String,
        tower: String, flat: String, code: String
    ) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    val authManager = remember(context) { AuthManager.getInstance(context) }

    var lastUser by remember { mutableStateOf(authManager.getLastRegisteredUser()) }
    var registeredUsers by remember { mutableStateOf(authManager.getAllRegisteredUsers()) }
    var showResidentLoginDialog by remember { mutableStateOf(false) }
    var showGoogleAccountChooser by remember { mutableStateOf(false) }
    var prefillEmail by remember { mutableStateOf("") }
    var prefillName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Top decorative gradient header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                EmeraldGreenDark,
                                EmeraldGreenPrimary,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                // Brand Header Badge & Title
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .shadow(6.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(EmeraldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Shapoorji Delivery",
                            tint = EmeraldGreenDark,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Shapoorji Shukhobrishti",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Daily Fresh Groceries & Kitchen Essentials",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.95f)
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Delivery Zone Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Doorstep Delivery • Phase 1 & Phase 2",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Visual Grocery Item Cards Showcase
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Today's Fresh Mandi Arrivals",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Fresh Daily",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreenPrimary
                        )
                    }

                    // Horizontal showcase of fresh vegetables with photos & prices
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        items(OfficialCatalog.INITIAL_PRODUCTS.take(6)) { product ->
                            GroceryShowcaseCard(product = product)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Resident Sign-In Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Welcome to Shukhobrishti Grocery",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sign in to order fresh morning vegetables, track 15-25 min delivery to your flat, and view daily mandi prices.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }

                        // Feature Value Highlights
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ResidentFeatureItem(
                                icon = Icons.Default.DeliveryDining,
                                color = Color(0xFFE65100),
                                title = "15-25 Min Doorstep Delivery",
                                subtitle = "Delivered directly to your flat door in Phase 1 & 2"
                            )
                            ResidentFeatureItem(
                                icon = Icons.Default.Eco,
                                color = EmeraldGreenDark,
                                title = "Morning Mandi Fresh Vegetables",
                                subtitle = "Cleaned, sorted and weighed with daily morning rates"
                            )
                            ResidentFeatureItem(
                                icon = Icons.Default.Payments,
                                color = Color(0xFF1976D2),
                                title = "Cash or UPI on Delivery",
                                subtitle = "Pay easily after inspecting your fresh produce"
                            )
                        }

                        // Error Banner if sign in failed
                        errorMessage?.let { error ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFEBEE),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = error,
                                    color = Color(0xFFC62828),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // WELCOME BACK PERSISTENT 1-TAP LOGIN (For returning residents who registered and logged out)
                        lastUser?.let { user ->
                            if (user.email.isNotBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = EmeraldContainer.copy(alpha = 0.45f),
                                    border = BorderStroke(1.5.dp, EmeraldGreenPrimary.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(if (user.isAdmin) Color(0xFFE65100) else Color(0xFF1A73E8)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = user.name.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "Welcome Back, ${user.name}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (user.isAdmin) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0xFFFFF3E0)
                                                        ) {
                                                            Text(
                                                                text = "ADMIN",
                                                                color = Color(0xFFE65100),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = user.email,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "${user.flatNumber} • ${user.tower.substringAfterLast("-").trim()}",
                                                    fontSize = 10.sp,
                                                    color = EmeraldGreenDark,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                onGoogleSignIn(
                                                    user.name,
                                                    user.email,
                                                    user.phone,
                                                    user.tower,
                                                    user.flatNumber
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (user.isAdmin) Color(0xFFE65100) else EmeraldGreenDark
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Continue as ${user.name.split(" ").firstOrNull() ?: "Resident"}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // STORE ACCOUNT SIGN-IN (spdelivery.reddevils.co.in) —
                        // the only way in. Email is verified by code at registration.
                        Button(
                            onClick = {
                                errorMessage = null
                                prefillEmail = ""
                                prefillName = ""
                                showResidentLoginDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreenPrimary
                            )
                        ) {
                            Text(
                                text = "Sign In / Create Store Account",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Resident flat sign-in alternative
                        OutlinedButton(
                            onClick = { showResidentLoginDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = EmeraldGreenDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enter Email & Flat Details",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldGreenDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Store Helpline
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = EmeraldGreenDark,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Store Helpline & WhatsApp: +91-8442980101",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Google Account Chooser Dialog (1-Tap Selection for known/registered accounts)
    if (showGoogleAccountChooser) {
        GoogleAccountChooserDialog(
            registeredUsers = registeredUsers,
            onSelectAccount = { selectedUser ->
                showGoogleAccountChooser = false
                onGoogleSignIn(
                    selectedUser.name,
                    selectedUser.email,
                    selectedUser.phone,
                    selectedUser.tower,
                    selectedUser.flatNumber
                )
            },
            onUseAnotherAccount = {
                showGoogleAccountChooser = false
                prefillEmail = ""
                prefillName = ""
                showResidentLoginDialog = true
            },
            onDismiss = { showGoogleAccountChooser = false }
        )
    }

    // Production Resident Account Sign-in Dialog (email-verified Store Account required)
    if (showResidentLoginDialog) {
        ResidentGoogleLoginDialog(
            authManager = authManager,
            initialEmail = prefillEmail,
            initialName = prefillName,
            onDismiss = {
                showResidentLoginDialog = false
                prefillEmail = ""
                prefillName = ""
            },
            onLoginComplete = { name, email, phone, tower, flat ->
                showResidentLoginDialog = false
                prefillEmail = ""
                prefillName = ""
                onGoogleSignIn(name, email, phone, tower, flat)
            },
            websiteAuthState = websiteAuthState,
            emailOtpState = emailOtpState,
            onWebsiteLogin = onWebsiteLogin,
            onRequestSignupOtp = onRequestSignupOtp,
            onConfirmSignupOtp = onConfirmSignupOtp
        )
    }
}

/**
 * Visual Showcase Card displaying a real grocery item with photo, name, and live price
 */
@Composable
private fun GroceryShowcaseCard(
    product: com.example.data.model.Product
) {
    Card(
        modifier = Modifier
            .width(135.dp)
            .height(180.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(Color(0xFFF1F3F4))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Fresh badge
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(EmeraldGreenDark.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Fresh",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val displayName = product.name.split("|").firstOrNull()?.trim() ?: product.name
                Text(
                    text = displayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = product.unit,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "₹${product.price.toInt()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldGreenDark
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "20m",
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResidentFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Account Chooser Dialog (1-Tap Selection for known/registered accounts)
 */
@Composable
fun GoogleAccountChooserDialog(
    registeredUsers: List<UserProfile>,
    onSelectAccount: (UserProfile) -> Unit,
    onUseAnotherAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4285F4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Choose Store Account",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Choose an account to continue",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select an account to sign in directly without re-entering your details:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                registeredUsers.forEach { user ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAccount(user) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(
                            1.dp,
                            if (user.isAdmin) Color(0xFFFFB74D) else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (user.isAdmin) Color(0xFFE65100) else Color(0xFF1A73E8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    if (user.isAdmin) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFFFF3E0)
                                        ) {
                                            Text(
                                                text = "ADMIN",
                                                color = Color(0xFFE65100),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = user.email,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${user.flatNumber} • ${user.tower.substringAfterLast("-").trim()}",
                                    fontSize = 10.sp,
                                    color = EmeraldGreenDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Sign In",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Add or use another account
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUseAnotherAccount() },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Use another account",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Sign in with a different email or flat",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

/**
 * Production Resident Login & Flat Details Dialog (email-verified Store Account gate)
 */
@Composable
fun ResidentGoogleLoginDialog(
    authManager: AuthManager = AuthManager.getInstance(LocalContext.current),
    initialEmail: String = "",
    initialName: String = "",
    onDismiss: () -> Unit,
    onLoginComplete: (name: String, email: String, phone: String, tower: String, flat: String) -> Unit,
    websiteAuthState: WebsiteAuthState = WebsiteAuthState.SignedOut,
    emailOtpState: EmailOtpState = EmailOtpState.Idle,
    onWebsiteLogin: (email: String, password: String) -> Unit = { _, _ -> },
    onRequestSignupOtp: (email: String) -> Unit = {},
    onConfirmSignupOtp: (
        name: String, email: String, password: String, phone: String,
        tower: String, flat: String, code: String
    ) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    val registeredUsers = remember { authManager.getAllRegisteredUsers() }

    var email by remember { mutableStateOf(initialEmail) }
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf("+91-") }
    var selectedTower by remember { mutableStateOf("Shukhobrishti Phase 1 - Tower A1") }
    var flatNumber by remember { mutableStateOf("") }
    var storePassword by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    // Set when the user explicitly starts a website sign-in/signup, so a
    // pre-existing session for another email can't auto-admit them.
    var webActionArmed by remember { mutableStateOf(false) }

    val linkedEmail = (websiteAuthState as? WebsiteAuthState.SignedIn)?.user?.email
    val isLinkedForEnteredEmail = !linkedEmail.isNullOrBlank() &&
        linkedEmail.equals(email.trim(), ignoreCase = true)

    // No one proceeds past this dialog without a verified Store Account.
    LaunchedEffect(linkedEmail, webActionArmed, email) {
        if (webActionArmed && isLinkedForEnteredEmail) {
            webActionArmed = false
            val finalEmail = email.trim()
            val finalName = name.trim().ifBlank {
                finalEmail.substringBefore("@").replace(".", " ")
                    .split(" ").joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
            }
            val finalPhone = if (phone.length > 5) phone.trim() else "+91-8442980101"
            onLoginComplete(
                finalName,
                finalEmail,
                finalPhone,
                selectedTower.trim().ifBlank { "Shukhobrishti Phase 1 - Tower A1" },
                flatNumber.trim()
            )
        }
    }

    androidx.compose.runtime.LaunchedEffect(initialEmail, initialName) {
        if (initialEmail.isNotBlank()) {
            email = initialEmail
        }
        if (initialName.isNotBlank()) {
            name = initialName
        } else if (initialEmail.contains("@") && name.isBlank()) {
            name = initialEmail.substringBefore("@")
                .replace(".", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
        }
    }

    val existingProfile = remember(email) {
        val trimmed = email.trim().lowercase()
        if (trimmed.contains("@") && trimmed.contains(".")) {
            authManager.getRegisteredUser(trimmed)
        } else null
    }

    // When an existing profile is found, pre-populate
    androidx.compose.runtime.LaunchedEffect(existingProfile) {
        existingProfile?.let { prof ->
            name = prof.name
            phone = prof.phone
            selectedTower = prof.tower
            flatNumber = prof.flatNumber
        }
    }

    val isRegistered = existingProfile != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRegistered) "Resident Sign In" else "Resident Registration",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick chips for registered users
                if (registeredUsers.isNotEmpty()) {
                    Text(
                        text = "Registered Residents on Device:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(registeredUsers) { regUser ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (email.equals(regUser.email, ignoreCase = true)) EmeraldGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    email = regUser.email
                                    name = regUser.name
                                    phone = regUser.phone
                                    selectedTower = regUser.tower
                                    flatNumber = regUser.flatNumber
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = regUser.name.split(" ").firstOrNull() ?: regUser.email,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (email.equals(regUser.email, ignoreCase = true)) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "(${regUser.flatNumber})",
                                        fontSize = 10.sp,
                                        color = if (email.equals(regUser.email, ignoreCase = true)) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Banner when existing profile recognized
                if (isRegistered && existingProfile != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = EmeraldGreenDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Registered Resident found: ${existingProfile.name} (${existingProfile.flatNumber})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldGreenDark
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Enter your email and flat details. New emails must verify with a mailed code; your Store Account is shared with spdelivery.reddevils.co.in.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address *") },
                    placeholder = { Text("e.g. resident@gmail.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    placeholder = { Text("e.g. Anirban Roy") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number (for Delivery OTP) *") },
                    placeholder = { Text("+91-9830012345") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = selectedTower,
                    onValueChange = { selectedTower = it },
                    label = { Text("Shapoorji Tower Name *") },
                    placeholder = { Text("e.g. Shukhobrishti Phase 1 - Tower A4") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = flatNumber,
                    onValueChange = { flatNumber = it },
                    label = { Text("Flat & Floor Number *") },
                    placeholder = { Text("e.g. Flat 803, 8th Floor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = storePassword,
                    onValueChange = { storePassword = it },
                    label = { Text("Store Password *") },
                    placeholder = { Text("Min 6 characters") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Email verification gate: a code is mailed before any new
                // account may proceed.
                val otpSent = emailOtpState is EmailOtpState.CodeSent ||
                    emailOtpState is EmailOtpState.Failed
                if (emailOtpState is EmailOtpState.Failed) {
                    Text(
                        text = emailOtpState.message,
                        fontSize = 12.sp,
                        color = Color(0xFFC62828)
                    )
                }
                if (otpSent && emailOtpState !is EmailOtpState.Failed) {
                    Text(
                        text = "Verification code sent to ${email.trim()} — check inbox/spam (valid 10 min).",
                        fontSize = 12.sp,
                        color = EmeraldGreenDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (otpSent) {
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it },
                        label = { Text("6-digit Verification Code *") },
                        placeholder = { Text("123456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                } else if (websiteAuthState is WebsiteAuthState.Error) {
                    Text(
                        text = websiteAuthState.message,
                        fontSize = 12.sp,
                        color = Color(0xFFC62828)
                    )
                }
                if (websiteAuthState is WebsiteAuthState.Loading ||
                    emailOtpState is EmailOtpState.Sending
                ) {
                    Text(
                        text = "Contacting store...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            val busy = websiteAuthState is WebsiteAuthState.Loading ||
                emailOtpState is EmailOtpState.Sending
            val emailOk = email.contains("@") && email.contains(".")
            val otpSent = emailOtpState is EmailOtpState.CodeSent ||
                emailOtpState is EmailOtpState.Failed
            if (isLinkedForEnteredEmail) {
                Button(
                    onClick = {
                        val finalEmail = email.trim()
                        val finalName = name.trim().ifBlank {
                            finalEmail.substringBefore("@").replace(".", " ")
                                .split(" ").joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
                        }
                        val finalPhone = if (phone.length > 5) phone.trim() else "+91-8442980101"
                        onLoginComplete(
                            finalName,
                            finalEmail,
                            finalPhone,
                            selectedTower.trim().ifBlank { "Shukhobrishti Phase 1 - Tower A1" },
                            flatNumber.trim()
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Continue to Store", fontWeight = FontWeight.Bold)
                }
            } else if (otpSent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            webActionArmed = true
                            onConfirmSignupOtp(
                                name.trim(),
                                email.trim(),
                                storePassword,
                                phone.trim(),
                                selectedTower.trim(),
                                flatNumber.trim(),
                                otpCode.trim()
                            )
                        },
                        enabled = emailOk && otpCode.trim().length >= 4 && !busy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Verify & Create", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = { onRequestSignupOtp(email.trim()) },
                        enabled = emailOk && !busy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Resend Code", fontSize = 13.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            webActionArmed = true
                            onWebsiteLogin(email.trim(), storePassword)
                        },
                        enabled = emailOk && storePassword.isNotBlank() && !busy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Sign In", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = { onRequestSignupOtp(email.trim()) },
                        enabled = emailOk && !busy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Create Account", fontSize = 13.sp)
                    }
                }
            }
        },
        dismissButton = {}
    )
}
