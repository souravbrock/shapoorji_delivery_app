package com.example.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.viewmodel.GroceryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GitHubCloudSyncDialog(
    viewModel: GroceryViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val isSyncing by viewModel.isSyncingCentral.collectAsState()
    val isPublishing by viewModel.isPublishingToGitHub.collectAsState()
    val syncResult by viewModel.centralSyncResult.collectAsState()
    val publishResult by viewModel.gitHubPublishResult.collectAsState()

    val currentSyncUrl by viewModel.centralSyncUrl.collectAsState()
    val lastSummary by viewModel.lastSyncSummary.collectAsState()
    val isAutoSync by viewModel.isAutoSyncEnabled.collectAsState()

    // Form states
    var syncUrlInput by remember(currentSyncUrl) { mutableStateOf(currentSyncUrl) }
    var autoSyncInput by remember(isAutoSync) { mutableStateOf(isAutoSync) }

    var publishMode by remember { mutableIntStateOf(0) } // 0: Gist, 1: Repo
    var tokenInput by remember { mutableStateOf(viewModel.centralSyncManager.githubToken) }
    var showToken by remember { mutableStateOf(false) }
    var gistIdInput by remember { mutableStateOf(viewModel.centralSyncManager.githubGistId) }
    var repoInput by remember { mutableStateOf(viewModel.centralSyncManager.githubRepo) }
    var pathInput by remember { mutableStateOf(viewModel.centralSyncManager.githubFilePath) }

    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = EmeraldGreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Central Cloud & GitHub Sync",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = EmeraldContainer.copy(alpha = 0.5f),
                    contentColor = EmeraldGreenDark
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Pull & Sync", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Push to GitHub", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Export Data", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // -------------------------------------------------------------
                        // TAB 0: SYNC FROM CENTRAL REPOSITORY
                        // -------------------------------------------------------------
                        Card(
                            colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Central Sync Overview",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreenDark
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "When you sync centrally, all customer apps automatically download your updated produce pictures, prices, and new future products directly from GitHub or your published sheet without needing a new APK.",
                                    fontSize = 11.sp,
                                    color = EmeraldGreenDark
                                )
                            }
                        }

                        // Last Sync Status Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                val lastTime = viewModel.centralSyncManager.lastSyncTimestamp
                                val dateStr = if (lastTime > 0) {
                                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(lastTime))
                                } else "Never"

                                Text(text = "Last Synced: $dateStr", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "Status: $lastSummary", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        OutlinedTextField(
                            value = syncUrlInput,
                            onValueChange = { syncUrlInput = it },
                            label = { Text("Central Sync URL (GitHub Raw / Gist / Google Sheet CSV)") },
                            placeholder = { Text("https://raw.githubusercontent.com/.../catalog.json") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            singleLine = false,
                            maxLines = 3,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-sync on customer launch", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Customers automatically receive new pictures & future items on app start", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = autoSyncInput,
                                onCheckedChange = { autoSyncInput = it }
                            )
                        }

                        if (syncResult != null) {
                            val isSuccess = syncResult?.success == true
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (isSuccess) "✓ Central Sync Succeeded" else "⚠ Central Sync Failed",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSuccess) EmeraldGreenDark else Color(0xFFC62828)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = syncResult?.message ?: "",
                                        fontSize = 11.sp,
                                        color = if (isSuccess) EmeraldGreenDark else Color(0xFFC62828)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.saveCentralSyncSettings(syncUrlInput, autoSyncInput)
                                    viewModel.syncWithCentralCloud(syncUrlInput)
                                },
                                enabled = !isSyncing && syncUrlInput.isNotBlank(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Syncing...", fontSize = 12.sp)
                                } else {
                                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync from Central Now", fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.saveCentralSyncSettings(syncUrlInput, autoSyncInput)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save URL", fontSize = 12.sp)
                            }
                        }
                    }

                    1 -> {
                        // -------------------------------------------------------------
                        // TAB 1: PUSH / PUBLISH TO GITHUB
                        // -------------------------------------------------------------
                        Card(
                            colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Push Live Catalog to GitHub",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreenDark
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Pushes all 37+ produce items, freshly imported Sheet 2 picture URLs, and future items into GitHub. Once pushed, it sets the central sync URL so all customers sync with it!",
                                    fontSize = 11.sp,
                                    color = EmeraldGreenDark
                                )
                            }
                        }

                        // Target Selector: Gist vs Repo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { publishMode = 0 }
                            ) {
                                RadioButton(selected = publishMode == 0, onClick = { publishMode = 0 })
                                Text("GitHub Gist (Quick)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { publishMode = 1 }
                            ) {
                                RadioButton(selected = publishMode == 1, onClick = { publishMode = 1 })
                                Text("GitHub Repo", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        // GitHub Personal Access Token (PAT)
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = { Text("GitHub Token (PAT)") },
                            placeholder = { Text("ghp_... (with 'gist' or 'repo' scope)") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            singleLine = true,
                            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showToken = !showToken }) {
                                    Icon(
                                        imageVector = if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle token visibility"
                                    )
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        )

                        if (publishMode == 0) {
                            // Gist ID (Optional)
                            OutlinedTextField(
                                value = gistIdInput,
                                onValueChange = { gistIdInput = it },
                                label = { Text("Gist ID (Optional)") },
                                placeholder = { Text("Leave blank to create a new Gist automatically") },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else {
                            // Repo fields
                            OutlinedTextField(
                                value = repoInput,
                                onValueChange = { repoInput = it },
                                label = { Text("Repository (owner/repo)") },
                                placeholder = { Text("souravbrock/shapoorji-delivery") },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = pathInput,
                                onValueChange = { pathInput = it },
                                label = { Text("File Path in Repo") },
                                placeholder = { Text("catalog.json") },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        if (publishResult != null) {
                            val isSuccess = publishResult?.success == true
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (isSuccess) "✓ Published to GitHub!" else "⚠ Publish Failed",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSuccess) EmeraldGreenDark else Color(0xFFC62828)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = publishResult?.message ?: "",
                                        fontSize = 11.sp,
                                        color = if (isSuccess) EmeraldGreenDark else Color(0xFFC62828)
                                    )

                                    if (isSuccess && publishResult?.rawSyncUrl?.isNotBlank() == true) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedButton(
                                            onClick = {
                                                val raw = publishResult?.rawSyncUrl ?: ""
                                                syncUrlInput = raw
                                                viewModel.saveCentralSyncSettings(raw, autoSyncInput)
                                                Toast.makeText(context, "Set as Central Sync URL!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Set as Active Sync URL", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.saveCentralSyncSettings(
                                    url = syncUrlInput,
                                    autoSync = autoSyncInput,
                                    gistId = gistIdInput,
                                    repo = repoInput,
                                    path = pathInput,
                                    token = tokenInput
                                )
                                if (publishMode == 0) {
                                    viewModel.pushCatalogToGitHubGist(tokenInput, gistIdInput) { res ->
                                        if (res.success && res.rawSyncUrl.isNotBlank()) {
                                            syncUrlInput = res.rawSyncUrl
                                        }
                                    }
                                } else {
                                    viewModel.pushCatalogToGitHubRepo(tokenInput, repoInput, pathInput) { res ->
                                        if (res.success && res.rawSyncUrl.isNotBlank()) {
                                            syncUrlInput = res.rawSyncUrl
                                        }
                                    }
                                }
                            },
                            enabled = !isPublishing && tokenInput.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isPublishing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Publishing to GitHub...", fontSize = 12.sp)
                            } else {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Push Catalog & Pictures to GitHub", fontSize = 12.sp)
                            }
                        }
                    }

                    2 -> {
                        // -------------------------------------------------------------
                        // TAB 2: MANUAL EXPORT & COPY
                        // -------------------------------------------------------------
                        Text(
                            text = "If you prefer not to enter a GitHub Token, you can copy the full JSON file or Sheet CSV right here and commit it directly to your GitHub repository or Gist in your browser.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val json = viewModel.exportCatalogJson()
                                    clipboard.setPrimaryClip(ClipData.newPlainText("catalog.json", json))
                                    Toast.makeText(context, "Copied catalog.json (${viewModel.allProducts.value.size} items) to clipboard!", Toast.LENGTH_LONG).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy JSON for GitHub", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val csv = viewModel.exportCatalogCsv()
                                    clipboard.setPrimaryClip(ClipData.newPlainText("catalog.csv", csv))
                                    Toast.makeText(context, "Copied Sheet CSV to clipboard!", Toast.LENGTH_LONG).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Sheet CSV", fontSize = 11.sp)
                            }
                        }

                        val sampleJson = remember { viewModel.exportCatalogJson() }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Preview (catalog.json):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sampleJson.take(1200) + if (sampleJson.length > 1200) "\n... (+${sampleJson.length - 1200} characters)" else "",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 12
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
            ) {
                Text("Done")
            }
        }
    )
}
