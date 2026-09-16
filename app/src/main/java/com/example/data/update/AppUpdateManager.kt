package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val latestVersionCode: Int = BuildConfig.VERSION_CODE,
    val latestVersionName: String = BuildConfig.VERSION_NAME,
    val releaseNotes: List<String> = emptyList(),
    val isUpdateAvailable: Boolean = false,
    val isMandatory: Boolean = false,
    val apkDownloadUrl: String? = null
)

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data class Available(val info: AppUpdateInfo) : UpdateStatus
    data class UpToDate(val versionName: String, val versionCode: Int) : UpdateStatus
    data class Downloading(val progressPercent: Int) : UpdateStatus
    data class ReadyToInstall(val apkFile: File, val info: AppUpdateInfo) : UpdateStatus
    data class Error(val message: String) : UpdateStatus
}

class AppUpdateManager(private val context: Context) {

    companion object {
        const val GITHUB_OWNER = "souravbrock"
        const val GITHUB_REPO = "shapoorji_delivery_app"
        // Primary feed: latest.json attached to every GitHub Release by
        // .github/workflows/build-apk.yml. Mirror the same file at
        // https://spd.reddevils.co.in/latest.json for subdomain hosting.
        const val SUBDOMAIN_LATEST_URL = "https://spd.reddevils.co.in/latest.json"
        fun githubLatestJsonUrl(owner: String = GITHUB_OWNER, repo: String = GITHUB_REPO): String =
            "https://github.com/$owner/$repo/releases/latest/download/latest.json"
        fun githubApiLatestUrl(owner: String = GITHUB_OWNER, repo: String = GITHUB_REPO): String =
            "https://api.github.com/repos/$owner/$repo/releases/latest"
    }

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    val currentVersionCode: Int get() = BuildConfig.VERSION_CODE
    val currentVersionName: String get() = BuildConfig.VERSION_NAME

    /**
     * Check if the app has permission to trigger package installs on Android 8.0+
     */
    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Opens system Settings so user can enable "Install unknown apps" for this app
     */
    fun openInstallPermissionSettings(activityContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${activityContext.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activityContext.startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activityContext.startActivity(fallbackIntent)
            }
        }
    }

    /**
     * Check for updates.
     * @param updateUrl optional latest.json URL (GitHub Release asset or
     * subdomain mirror). When null, reports UpToDate locally so the existing
     * offline behavior/tests keep working. Pass a URL for a real network check.
     */
    suspend fun checkForUpdates(
        forcedCheck: Boolean = false,
        updateUrl: String? = null
    ): AppUpdateInfo = withContext(Dispatchers.IO) {
        _updateStatus.value = UpdateStatus.Checking
        delay(800) // smooth check feedback

        if (updateUrl != null) {
            return@withContext checkForUpdatesFromUrl(updateUrl)
        }

        // Offline fallback: no remote feed configured.
        val updateInfo = AppUpdateInfo(
            currentVersionCode = BuildConfig.VERSION_CODE,
            currentVersionName = BuildConfig.VERSION_NAME,
            latestVersionCode = BuildConfig.VERSION_CODE,
            latestVersionName = BuildConfig.VERSION_NAME,
            releaseNotes = listOf(
                "✓ Full support for in-place seamless updates without uninstalling",
                "✓ Preserved local user sessions, cart, and delivery addresses",
                "✓ Shukhobrishti township address verification & real produce visuals",
                "✓ Optimized Room database migration engine"
            ),
            isUpdateAvailable = false
        )

        _updateStatus.value = UpdateStatus.UpToDate(currentVersionName, currentVersionCode)
        updateInfo
    }

    /**
     * Real network check against a latest.json feed generated by the release
     * workflow (GitHub Releases asset + spd.reddevils.co.in mirror).
     * Falls back to UpToDate/Error states — never crashes callers.
     */
    suspend fun checkForUpdatesFromUrl(url: String): AppUpdateInfo = withContext(Dispatchers.IO) {
        _updateStatus.value = UpdateStatus.Checking
        try {
            val json = httpGet(url, timeoutMs = 15_000)
            val obj = JSONObject(json)
            val latestCode = obj.optInt("versionCode", BuildConfig.VERSION_CODE)
            val latestName = obj.optString("versionName", BuildConfig.VERSION_NAME)
            val apkUrl = obj.optString("apkUrl", "").ifBlank { null }
            val notes = mutableListOf<String>()
            val arr = obj.optJSONArray("notes")
            if (arr != null) {
                for (i in 0 until arr.length()) notes += arr.optString(i)
            }
            // GitHub API fallback shape: tag_name + body.
            if (latestCode == BuildConfig.VERSION_CODE && obj.has("tag_name")) {
                val tag = obj.optString("tag_name", latestName).removePrefix("v")
                return@withContext applyRemoteResult(tag, latestCode, apkUrl, notes.ifEmpty {
                    listOf(obj.optString("body", "Bug fixes and improvements").take(500))
                })
            }
            return@withContext applyRemoteResult(latestName, latestCode, apkUrl, notes)
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error("Couldn't reach update server: ${e.message}")
            AppUpdateInfo(isUpdateAvailable = false)
        }
    }

    /**
     * Convenience: try GitHub latest.json asset, then subdomain mirror,
     * then GitHub API. First success wins.
     */
    suspend fun checkForUpdatesFromGitHub(
        owner: String = GITHUB_OWNER,
        repo: String = GITHUB_REPO
    ): AppUpdateInfo {
        val candidates = listOf(
            githubLatestJsonUrl(owner, repo),
            SUBDOMAIN_LATEST_URL,
            githubApiLatestUrl(owner, repo)
        )
        var last: AppUpdateInfo = AppUpdateInfo(isUpdateAvailable = false)
        for (url in candidates) {
            last = checkForUpdatesFromUrl(url)
            val s = _updateStatus.value
            if (s is UpdateStatus.Available || s is UpdateStatus.UpToDate) return last
        }
        return last
    }

    private fun applyRemoteResult(
        latestName: String,
        latestCode: Int,
        apkUrl: String?,
        notes: List<String>
    ): AppUpdateInfo {
        val available = latestCode > BuildConfig.VERSION_CODE
        val info = AppUpdateInfo(
            currentVersionCode = BuildConfig.VERSION_CODE,
            currentVersionName = BuildConfig.VERSION_NAME,
            latestVersionCode = latestCode,
            latestVersionName = latestName,
            releaseNotes = notes.ifEmpty {
                listOf("Bug fixes and improvements. Install as in-place upgrade — no uninstall needed.")
            },
            isUpdateAvailable = available,
            apkDownloadUrl = apkUrl
        )
        _updateStatus.value = if (available) UpdateStatus.Available(info)
        else UpdateStatus.UpToDate(currentVersionName, currentVersionCode)
        return info
    }

    private fun httpGet(url: String, timeoutMs: Int): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "ShapoorjiDelivery-App")
            instanceFollowRedirects = true
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code for $url")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Download an APK from [apkUrl] into the app cache with progress updates.
     * Same signing key across releases => system performs in-place upgrade.
     */
    suspend fun downloadUpdateApk(apkUrl: String, destName: String = "update.apk"): File? =
        withContext(Dispatchers.IO) {
            try {
                _updateStatus.value = UpdateStatus.Downloading(0)
                val conn = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                }
                val total = conn.contentLengthLong.takeIf { it > 0 }
                val out = File(context.cacheDir, destName)
                conn.inputStream.use { input ->
                    out.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        var read: Int
                        var done = 0L
                        while (input.read(buf).also { read = it } != -1) {
                            output.write(buf, 0, read)
                            done += read
                            if (total != null) {
                                _updateStatus.value =
                                    UpdateStatus.Downloading(((done * 100) / total).toInt().coerceIn(0, 100))
                            }
                        }
                    }
                }
                val current = _updateStatus.value
                val info = (current as? UpdateStatus.Available)?.info ?: AppUpdateInfo(
                    apkDownloadUrl = apkUrl
                )
                _updateStatus.value = UpdateStatus.ReadyToInstall(out, info)
                out
            } catch (e: Exception) {
                _updateStatus.value = UpdateStatus.Error("Download failed: ${e.message}")
                null
            }
        }

    /**
     * Triggers an in-place upgrade test or preview to verify that future updates
     * can be installed cleanly without uninstalling previous versions.
     */
    fun simulateUpdateAvailable(
        newVersionName: String = "1.2.0",
        newVersionCode: Int = BuildConfig.VERSION_CODE + 1,
        notes: List<String> = listOf(
            "✓ Instant doorstep delivery status enhancements",
            "✓ Enhanced Bengali vegetable quality certificates",
            "✓ Seamless update engine - no uninstallation needed"
        )
    ) {
        val info = AppUpdateInfo(
            currentVersionCode = BuildConfig.VERSION_CODE,
            currentVersionName = BuildConfig.VERSION_NAME,
            latestVersionCode = newVersionCode,
            latestVersionName = newVersionName,
            releaseNotes = notes,
            isUpdateAvailable = true
        )
        _updateStatus.value = UpdateStatus.Available(info)
    }

    fun dismissUpdate() {
        _updateStatus.value = UpdateStatus.Idle
    }

    /**
     * Launch the Android system package installer for an APK file using FileProvider.
     * This performs an in-place upgrade preserving existing user data.
     */
    fun installApk(activityContext: Context, apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists()) {
                return Result.failure(IllegalStateException("Update package file not found at: ${apkFile.absolutePath}"))
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                activityContext,
                "${activityContext.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activityContext.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
