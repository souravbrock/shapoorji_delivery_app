package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary

@Composable
fun GoogleAccountDialog(
    currentUser: UserProfile,
    onDismiss: () -> Unit,
    onSaveProfile: (name: String, email: String, phone: String, tower: String, flat: String) -> Unit,
    onSignOut: () -> Unit
) {
    var email by remember { mutableStateOf(currentUser.email) }
    var name by remember { mutableStateOf(currentUser.name) }
    var phone by remember { mutableStateOf(currentUser.phone) }
    var tower by remember { mutableStateOf(currentUser.tower) }
    var flat by remember { mutableStateOf(currentUser.flatNumber) }

    val isAdminEmail = email.trim().equals("souravbrock@gmail.com", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isAdminEmail) Color(0xFFE65100) else Color(0xFF4285F4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAdminEmail) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isAdminEmail) "Store Admin Account" else "Customer Account Registration",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (isAdminEmail) "Authorized Administrator" else "Verified Customer Identity",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isAdminEmail) Color(0xFFE65100) else EmeraldGreenPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Role badge and security banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAdminEmail) Color(0xFFFFF3E0) else EmeraldContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAdminEmail) Icons.Default.Security else Icons.Default.VerifiedUser,
                            contentDescription = "Role",
                            tint = if (isAdminEmail) Color(0xFFE65100) else EmeraldGreenDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAdminEmail) {
                                "Store Administrator account: souravbrock@gmail.com"
                            } else {
                                "Resident Account: Orders will be delivered to your verified Sukhobristi flat."
                            },
                            fontSize = 11.sp,
                            color = if (isAdminEmail) Color(0xFFBF360C) else EmeraldGreenDark,
                            lineHeight = 15.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    placeholder = { Text("customer@example.com") },
                    supportingText = {
                        if (email.trim().equals("souravbrock@gmail.com", ignoreCase = true)) {
                            Text("✓ Admin privileges active for this email", color = Color(0xFF2E7D32), fontSize = 11.sp)
                        } else {
                            Text("Customer-only access (Admin panel locked)", color = Color.Gray, fontSize = 11.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+91-8442980101") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = tower,
                    onValueChange = { tower = it },
                    label = { Text("Tower / Building (Shapoorji)") },
                    placeholder = { Text("Sukhobristi Phase 1 - Tower A4") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = flat,
                    onValueChange = { flat = it },
                    label = { Text("Flat / Unit Number") },
                    placeholder = { Text("Flat 803, 8th Floor") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text(
                    text = "Order confirmation emails and status updates will be dispatched from order@spdelivery.reddevils.co.in to this address and souravbrock@gmail.com.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveProfile(name.trim(), email.trim(), phone.trim(), tower.trim(), flat.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAdminEmail) Color(0xFFE65100) else EmeraldGreenPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isAdminEmail) "Save & Login as Admin" else "Save & Register as Customer")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = {
                        onSignOut()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sign Out", color = Color.Red, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close", fontSize = 12.sp)
                }
            }
        }
    )
}
