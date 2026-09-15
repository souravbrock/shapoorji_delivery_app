package com.example.ui.components

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.update.AppUpdateInfo
import com.example.data.update.UpdateStatus
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary

@Composable
fun AppUpdateDialog(
    status: UpdateStatus,
    onDismiss: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onInstallUpdate: () -> Unit,
    onRequestInstallPermission: () -> Unit,
    canInstallPackages: Boolean
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(EmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "System Update",
                        tint = EmeraldGreenDark,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "App Updates & Upgrades",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Seamless in-place updates without uninstalling",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (status) {
                    is UpdateStatus.Checking -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = EmeraldGreenPrimary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Checking for official updates...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    is UpdateStatus.UpToDate -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(EmeraldContainer)
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Latest Version",
                                        tint = EmeraldGreenDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "You're on the latest version!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = EmeraldGreenDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Installed: v${status.versionName} (Build ${status.versionCode})",
                                    fontSize = 12.sp,
                                    color = EmeraldGreenDark.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Future updates will install automatically as direct in-place upgrades. Your account data, cart, and addresses remain securely preserved.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = EmeraldGreenDark.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    is UpdateStatus.Available -> {
                        val info = status.info
                        UpdateAvailableContent(
                            info = info,
                            canInstallPackages = canInstallPackages,
                            onRequestInstallPermission = onRequestInstallPermission
                        )
                    }

                    is UpdateStatus.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Update Check Notice",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = status.message,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Version info",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "In-Place Upgrade System",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "When new versions or daily fresh updates are released, you can install the upgrade directly on top of this build without uninstalling.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", fontSize = 13.sp)
                    }

                    if (status is UpdateStatus.Available) {
                        Button(
                            onClick = {
                                if (!canInstallPackages) {
                                    onRequestInstallPermission()
                                } else {
                                    onInstallUpdate()
                                }
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreenPrimary
                            )
                        ) {
                            Text(
                                text = if (!canInstallPackages) "Grant Install Permission" else "Upgrade Now",
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = onCheckForUpdates,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreenPrimary
                            )
                        ) {
                            Text("Check Updates", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateAvailableContent(
    info: AppUpdateInfo,
    canInstallPackages: Boolean,
    onRequestInstallPermission: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(EmeraldContainer)
                .padding(14.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "New Update Available: v${info.latestVersionName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = EmeraldGreenDark
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Current: v${info.currentVersionName} (Build ${info.currentVersionCode}) → Latest: v${info.latestVersionName} (Build ${info.latestVersionCode})",
                    fontSize = 11.sp,
                    color = EmeraldGreenDark.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "What's New:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = EmeraldGreenDark
                )
                info.releaseNotes.forEach { note ->
                    Text(
                        text = "• $note",
                        fontSize = 11.sp,
                        color = EmeraldGreenDark.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Upgrade guarantee banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE8F5E9))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Safe Upgrade",
                tint = EmeraldGreenDark,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "In-Place Upgrade: Installing will update the app directly. You DO NOT need to uninstall the current version.",
                fontSize = 11.sp,
                color = EmeraldGreenDark,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (!canInstallPackages) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⚠️ Android requires permission to install APK updates directly from within this app. Tap below to enable.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error,
                lineHeight = 15.sp
            )
        }
    }
}
