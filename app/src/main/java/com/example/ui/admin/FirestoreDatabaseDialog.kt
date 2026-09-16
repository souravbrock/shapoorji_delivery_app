package com.example.ui.admin

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.data.firestore.FirestoreSyncState
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.viewmodel.GroceryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FirestoreDatabaseDialog(
    viewModel: GroceryViewModel,
    onDismiss: () -> Unit
) {
    val syncState by viewModel.firestoreSyncState.collectAsState()
    val lastSummary by viewModel.firestoreLastSyncSummary.collectAsState()
    val lastTimestamp by viewModel.firestoreLastSyncTimestamp.collectAsState()
    val isMigrating by viewModel.isFirestoreMigrating.collectAsState()
    val actionMessage by viewModel.firestoreActionMessage.collectAsState()
    val currentProjectId by viewModel.firestoreProjectId.collectAsState()

    var projectIdInput by remember(currentProjectId) { mutableStateOf(currentProjectId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Firestore",
                        tint = EmeraldGreenDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Firebase Firestore Database",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Central Real-Time Product Catalog",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Realtime Sync Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (syncState) {
                            is FirestoreSyncState.Connected -> Color(0xFFE8F5E9)
                            is FirestoreSyncState.Connecting -> Color(0xFFFFF8E1)
                            is FirestoreSyncState.Error -> Color(0xFFFFEBEE)
                            else -> EmeraldContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val statusDotColor = when (syncState) {
                                    is FirestoreSyncState.Connected -> Color(0xFF2E7D32)
                                    is FirestoreSyncState.Connecting -> Color(0xFFF57C00)
                                    is FirestoreSyncState.Error -> Color(0xFFC62828)
                                    else -> EmeraldGreenPrimary
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(statusDotColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (syncState) {
                                        is FirestoreSyncState.Connected -> "Live Real-Time Active"
                                        is FirestoreSyncState.Connecting -> "Connecting to Firestore..."
                                        is FirestoreSyncState.Error -> "Sync Alert"
                                        else -> "Ready to Connect"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = when (syncState) {
                                        is FirestoreSyncState.Connected -> Color(0xFF1B5E20)
                                        is FirestoreSyncState.Connecting -> Color(0xFFE65100)
                                        is FirestoreSyncState.Error -> Color(0xFFB71C1C)
                                        else -> EmeraldGreenDark
                                    }
                                )
                            }

                            if (syncState is FirestoreSyncState.Connected) {
                                val connected = syncState as FirestoreSyncState.Connected
                                Text(
                                    text = "${connected.itemCount} items synced",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = lastSummary,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (lastTimestamp > 0) {
                            val formattedTime = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault()).format(Date(lastTimestamp))
                            Text(
                                text = "Last updated: $formattedTime",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (actionMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF1565C0),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = actionMessage ?: "",
                                fontSize = 11.sp,
                                color = Color(0xFF0D47A1),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Description
                Text(
                    text = "Centralized Firestore database acts as the single source of truth for all produce photos, daily rates, and stock. Every update made here is broadcast in real-time across all user devices with instant offline caching.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                HorizontalDivider()

                // Actions Section
                Text(
                    text = "Cloud Synchronization Actions",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = { viewModel.migrateAllProductsToFirestore() },
                    enabled = !isMigrating,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isMigrating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Migrating to Firestore...", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Migrate / Seed Catalog to Firestore", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.syncFromFirestore() },
                    enabled = !isMigrating,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = EmeraldGreenDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pull & Refresh from Firestore", fontSize = 12.sp, color = EmeraldGreenDark)
                }

                HorizontalDivider()

                // Configuration Section
                Text(
                    text = "Firebase Configuration",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = projectIdInput,
                    onValueChange = { projectIdInput = it },
                    label = { Text("Firebase Project ID", fontSize = 11.sp) },
                    placeholder = { Text("e.g. shapoorji-delivery") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (projectIdInput != currentProjectId) {
                    Button(
                        onClick = { viewModel.updateFirestoreProjectId(projectIdInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Project ID", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.clearFirestoreActionMessage()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
            ) {
                Text("Close")
            }
        }
    )
}
