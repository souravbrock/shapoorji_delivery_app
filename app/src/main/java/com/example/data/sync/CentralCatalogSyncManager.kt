package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.example.data.local.ProductDao
import com.example.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Result of syncing the product catalog from a central remote endpoint (GitHub Raw, Gist, or Sheet).
 */
data class CentralSyncResult(
    val success: Boolean,
    val updatedCount: Int = 0,
    val addedCount: Int = 0,
    val totalCount: Int = 0,
    val sourceUrl: String = "",
    val message: String = ""
)

/**
 * Result of pushing the product catalog and photos to GitHub.
 */
data class GitHubPublishResult(
    val success: Boolean,
    val rawSyncUrl: String = "",
    val gistId: String? = null,
    val message: String = ""
)

/**
 * Central catalog synchronization engine.
 * 
 * Synchronizes produce photos, pricing, descriptions, and new future products
 * between the local Room SQLite database and a central remote source
 * (GitHub repository, GitHub Gist, or published Google Sheets CSV).
 */
class CentralCatalogSyncManager(
    private val context: Context,
    private val productDao: ProductDao
) {
    companion object {
        private const val TAG = "CentralCatalogSync"
        private const val PREFS_NAME = "shapoorji_central_sync_prefs"
        private const val KEY_SYNC_URL = "central_sync_url"
        private const val KEY_GITHUB_TOKEN = "github_token"
        private const val KEY_GITHUB_GIST_ID = "github_gist_id"
        private const val KEY_GITHUB_REPO = "github_repo"
        private const val KEY_GITHUB_PATH = "github_file_path"
        private const val KEY_LAST_SYNC_TIME = "last_sync_timestamp"
        private const val KEY_LAST_SYNC_SUMMARY = "last_sync_summary"
        private const val KEY_AUTO_SYNC = "auto_sync_on_launch"

        // Default GitHub Raw catalog URL (can be customized by admin)
        const val DEFAULT_CENTRAL_SYNC_URL = "https://raw.githubusercontent.com/souravbrock/shapoorji-delivery/main/catalog.json"

        // Website catalog (source of truth): spdelivery.reddevils.co.in storefront,
        // served by the reddevils.co.in backend API. Product edits on the website
        // flow into the app through fetchAndSyncFromWebsite().
        const val WEBSITE_PRODUCTS_URL = "https://reddevils.co.in/api/products?activeOnly=1&orderBy=name"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    var centralSyncUrl: String
        get() = prefs.getString(KEY_SYNC_URL, DEFAULT_CENTRAL_SYNC_URL) ?: DEFAULT_CENTRAL_SYNC_URL
        set(value) = prefs.edit().putString(KEY_SYNC_URL, value.trim()).apply()

    var githubToken: String
        get() = prefs.getString(KEY_GITHUB_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GITHUB_TOKEN, value.trim()).apply()

    var githubGistId: String
        get() = prefs.getString(KEY_GITHUB_GIST_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GITHUB_GIST_ID, value.trim()).apply()

    var githubRepo: String
        get() = prefs.getString(KEY_GITHUB_REPO, "souravbrock/shapoorji-delivery") ?: "souravbrock/shapoorji-delivery"
        set(value) = prefs.edit().putString(KEY_GITHUB_REPO, value.trim()).apply()

    var githubFilePath: String
        get() = prefs.getString(KEY_GITHUB_PATH, "catalog.json") ?: "catalog.json"
        set(value) = prefs.edit().putString(KEY_GITHUB_PATH, value.trim()).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    var lastSyncSummary: String
        get() = prefs.getString(KEY_LAST_SYNC_SUMMARY, "Not synced yet") ?: "Not synced yet"
        set(value) = prefs.edit().putString(KEY_LAST_SYNC_SUMMARY, value).apply()

    var isAutoSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SYNC, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SYNC, value).apply()

    /**
     * Fetch central catalog from GitHub or remote URL and sync into local Room database.
     * Supports both JSON and CSV/TSV (Sheet 2 format).
     */
    suspend fun fetchAndSyncCatalog(targetUrl: String? = null): CentralSyncResult = withContext(Dispatchers.IO) {
        val url = (targetUrl ?: centralSyncUrl).trim()
        if (url.isBlank()) {
            return@withContext CentralSyncResult(
                success = false,
                message = "No Central Sync URL configured."
            )
        }

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ShapoorjiDelivery-AndroidApp")
                .header("Accept", "application/json, text/plain, text/csv, */*")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errMsg = "HTTP ${response.code}: ${response.message}"
                    return@withContext CentralSyncResult(
                        success = false,
                        sourceUrl = url,
                        message = "Could not fetch central catalog: $errMsg"
                    )
                }

                val rawContent = response.body?.string() ?: ""
                if (rawContent.isBlank()) {
                    return@withContext CentralSyncResult(
                        success = false,
                        sourceUrl = url,
                        message = "Central catalog response was empty."
                    )
                }

                val trimmed = rawContent.trim()
                parseAndApplyCatalog(trimmed, url)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing central catalog: ${e.message}", e)
            CentralSyncResult(
                success = false,
                sourceUrl = url,
                message = "Central sync error: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * Pull the live website catalog (spdelivery.reddevils.co.in via the
     * reddevils.co.in backend API) and merge it into the local Room database.
     *
     * Matching is by product name (English segment), so local row IDs — and with
     * them carts, favorites, orders and reviews — stay stable across syncs.
     * Only website-managed fields are overwritten (name, category, unit, price,
     * stock, description, image); app-side fields (ratings, Daily Essential flag,
     * fractional settings) are preserved.
     */
    suspend fun fetchAndSyncFromWebsite(targetUrl: String = WEBSITE_PRODUCTS_URL): CentralSyncResult =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "ShapoorjiDelivery-AndroidApp")
                    .header("Accept", "application/json")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext CentralSyncResult(
                            success = false,
                            sourceUrl = targetUrl,
                            message = "Website catalog unreachable: HTTP ${response.code}"
                        )
                    }
                    val raw = response.body?.string() ?: ""
                    if (raw.isBlank()) {
                        return@withContext CentralSyncResult(
                            success = false,
                            sourceUrl = targetUrl,
                            message = "Website catalog response was empty."
                        )
                    }
                    applyWebsiteCatalog(JSONArray(raw), targetUrl)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Website catalog sync failed: ${e.message}", e)
                CentralSyncResult(
                    success = false,
                    sourceUrl = targetUrl,
                    message = "Website sync error: ${e.localizedMessage ?: e.message}"
                )
            }
        }

    private suspend fun applyWebsiteCatalog(jsonArray: JSONArray, sourceUrl: String): CentralSyncResult {
        val existing = productDao.getAllProductsDirect().toMutableList()
        var updated = 0
        var added = 0

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(i) ?: continue
            val name = item.optString("name", "").trim()
            if (name.isBlank()) continue

            val price = item.optDouble("price", -1.0)
            val unit = normalizeWebsiteUnit(item.optString("unit", "1 kg"))
            val imageUrl = item.optString("image_url", "").trim()
            val stock = item.optInt("stock", -1)
            val description = item.optString("description", "").trim().ifBlank { name }
            val category = normalizeWebsiteCategory(
                item.optJSONObject("category")?.optString("name", "") ?: ""
            )

            val englishCandidate = name.split("|").first().trim().lowercase()
            val matched = existing.find { prod ->
                prod.name.equals(name, ignoreCase = true) ||
                prod.name.split("|").first().trim().equals(englishCandidate, ignoreCase = true)
            }

            if (matched != null) {
                val merged = matched.copy(
                    name = name,
                    category = category.ifBlank { matched.category },
                    unit = unit,
                    price = if (price > 0.0) price else matched.price,
                    mrp = if (price > 0.0) price else matched.price,
                    stockQty = if (stock >= 0) stock else matched.stockQty,
                    description = description,
                    imageUrl = imageUrl.ifBlank { matched.imageUrl },
                    isAvailable = if (stock >= 0) stock > 0 else matched.isAvailable,
                    updatedAt = System.currentTimeMillis()
                )
                if (merged != matched) {
                    productDao.updateProduct(merged)
                    val idx = existing.indexOfFirst { it.id == matched.id }
                    if (idx >= 0) existing[idx] = merged
                    updated++
                }
            } else {
                val lowerUnit = unit.lowercase()
                val divisible = lowerUnit.contains("kg") || lowerUnit.contains("gm") ||
                        lowerUnit.contains("gram") || lowerUnit.contains("l")
                val fine = name.contains("Garlic", ignoreCase = true) ||
                        name.contains("Ginger", ignoreCase = true) ||
                        name.contains("Chilli", ignoreCase = true)
                val newProduct = Product(
                    name = name,
                    category = category.ifBlank { "Vegetables" },
                    unit = unit,
                    price = if (price > 0.0) price else 60.0,
                    mrp = if (price > 0.0) price else 60.0,
                    stockQty = if (stock >= 0) stock else 50,
                    description = description,
                    imageUrl = imageUrl,
                    isAvailable = if (stock >= 0) stock > 0 else true,
                    allowFractional = divisible,
                    fractionStepGrams = if (fine) 100 else 250,
                    updatedAt = System.currentTimeMillis()
                )
                val newId = productDao.insertProduct(newProduct)
                existing.add(newProduct.copy(id = newId))
                added++
            }
        }

        if (updated > 0 || added > 0) {
            lastSyncTimestamp = System.currentTimeMillis()
            lastSyncSummary = "Website sync: $updated updated, $added added (${existing.size} total)"
        }
        return CentralSyncResult(
            success = true,
            updatedCount = updated,
            addedCount = added,
            totalCount = existing.size,
            sourceUrl = sourceUrl,
            message = "Website catalog synced ($updated updated, $added added)."
        )
    }

    private fun normalizeWebsiteUnit(raw: String): String {
        val t = raw.trim()
        return when (t.lowercase()) {
            "1kg" -> "1 kg"
            "100gms", "100gm", "100g" -> "100 g"
            "1pc", "1pcs", "1piece" -> "1 pc"
            "1bunch" -> "1 bunch"
            else -> t.ifBlank { "1 kg" }
        }
    }

    private fun normalizeWebsiteCategory(raw: String): String {
        val t = raw.trim().lowercase()
        return when (t) {
            "vegetables", "fruits", "dairy", "rice", "grocery", "bakery", "beverages" ->
                t.replaceFirstChar { it.uppercase() }
            else -> raw.trim()
        }
    }

    /**
     * Parse raw content (JSON array or Sheet CSV/TSV) and apply to local database.
     */
    suspend fun parseAndApplyCatalog(rawContent: String, sourceUrl: String = "local"): CentralSyncResult {
        val trimmed = rawContent.trim()
        val result = if (trimmed.startsWith("[") || (trimmed.startsWith("{") && !trimmed.contains("\n") && !trimmed.contains(","))) {
            parseAndApplyJsonCatalog(trimmed, sourceUrl)
        } else if (trimmed.startsWith("{") && (trimmed.contains("\"products\"") || trimmed.contains("\"items\"") || trimmed.contains("\"catalog\""))) {
            parseAndApplyJsonCatalog(trimmed, sourceUrl)
        } else {
            // Parse as CSV or TSV (Sheet 2 format)
            parseAndApplyCsvCatalog(trimmed, sourceUrl)
        }

        if (result.success) {
            lastSyncTimestamp = System.currentTimeMillis()
            lastSyncSummary = "Synced ${result.updatedCount} photos, ${result.addedCount} items (${result.totalCount} total)"
        }
        return result
    }

    /**
     * Parse JSON catalog array and update local Room database.
     */
    private suspend fun parseAndApplyJsonCatalog(jsonStr: String, sourceUrl: String): CentralSyncResult {
        val jsonArray = try {
            if (jsonStr.startsWith("[")) {
                JSONArray(jsonStr)
            } else {
                val rootObj = JSONObject(jsonStr)
                when {
                    rootObj.has("products") -> rootObj.getJSONArray("products")
                    rootObj.has("items") -> rootObj.getJSONArray("items")
                    rootObj.has("catalog") -> rootObj.getJSONArray("catalog")
                    else -> null
                }
            }
        } catch (e: Exception) {
            null
        }

        if (jsonArray == null || jsonArray.length() == 0) {
            return parseAndApplyCsvCatalog(jsonStr, sourceUrl)
        }

        val existingProducts = productDao.getAllProductsDirect().toMutableList()
        var updated = 0
        var added = 0

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(i) ?: continue
            val id = item.optLong("id", -1L)
            val name = item.optString("name", "").trim()
            val imageUrl = item.optString("imageUrl", item.optString("image", item.optString("photo", ""))).trim()
            val price = item.optDouble("price", -1.0)
            val mrp = item.optDouble("mrp", -1.0)
            val unit = item.optString("unit", "1 kg")
            val category = item.optString("category", "Vegetables")
            val stockQty = item.optInt("stockQty", 50)
            val description = item.optString("description", "")
            val isDailyEssential = item.optBoolean("isDailyEssential", false)
            val allowFractional = item.optBoolean("allowFractional", true)
            val fractionStepGrams = item.optInt("fractionStepGrams", 250)

            if (name.isBlank() && id <= 0L) continue

            val englishCandidate = name.split("|").first().trim().lowercase()

            val matched = existingProducts.find { existing ->
                (id > 0L && existing.id == id) ||
                existing.name.equals(name, ignoreCase = true) ||
                existing.name.split("|").first().trim().equals(englishCandidate, ignoreCase = true)
            }

            if (matched != null) {
                var modified = false
                var newImage = matched.imageUrl
                var newPrice = matched.price
                var newStock = matched.stockQty
                var newDesc = matched.description

                if (imageUrl.isNotBlank() && imageUrl != matched.imageUrl) {
                    newImage = imageUrl
                    modified = true
                }
                if (price > 0.0 && price != matched.price) {
                    newPrice = price
                    modified = true
                }
                if (stockQty >= 0 && stockQty != matched.stockQty) {
                    newStock = stockQty
                    modified = true
                }
                if (description.isNotBlank() && description != matched.description) {
                    newDesc = description
                    modified = true
                }

                if (modified) {
                    val updatedProduct = matched.copy(
                        imageUrl = newImage,
                        price = newPrice,
                        stockQty = newStock,
                        description = newDesc,
                        isAvailable = newStock > 0,
                        updatedAt = System.currentTimeMillis()
                    )
                    productDao.updateProduct(updatedProduct)
                    val idx = existingProducts.indexOfFirst { it.id == matched.id }
                    if (idx >= 0) existingProducts[idx] = updatedProduct
                    updated++
                }
            } else if (name.isNotBlank()) {
                val newProduct = Product(
                    name = name,
                    category = category,
                    unit = unit,
                    price = if (price > 0.0) price else 60.0,
                    mrp = if (mrp > 0.0) mrp else ((if (price > 0.0) price else 60.0) * 1.15).toInt().toDouble(),
                    stockQty = stockQty,
                    description = description.ifBlank { "Fresh produce sourced daily for Shukhobrishti residents." },
                    imageUrl = imageUrl,
                    isDailyEssential = isDailyEssential,
                    isAvailable = stockQty > 0,
                    allowFractional = allowFractional,
                    fractionStepGrams = fractionStepGrams,
                    updatedAt = System.currentTimeMillis()
                )
                val newId = productDao.insertProduct(newProduct)
                existingProducts.add(newProduct.copy(id = newId))
                added++
            }
        }

        return CentralSyncResult(
            success = true,
            updatedCount = updated,
            addedCount = added,
            totalCount = existingProducts.size,
            sourceUrl = sourceUrl,
            message = "Successfully synced central catalog ($updated updated, $added added)."
        )
    }

    /**
     * Parse CSV / TSV text (Sheet 2 format) and update local Room database.
     */
    private suspend fun parseAndApplyCsvCatalog(csvText: String, sourceUrl: String): CentralSyncResult {
        val existingProducts = productDao.getAllProductsDirect().toMutableList()
        var updated = 0
        var added = 0
        val urlRegex = Regex("""(https?://[^\s,"']+|file://[^\s,"']+)""")

        val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
        for (line in lines) {
            val lowerLine = line.lowercase()
            if (lowerLine.contains("product") && (lowerLine.contains("url") || lowerLine.contains("image") || lowerLine.contains("picture") || lowerLine.contains("photo") || lowerLine.contains("price") || lowerLine.contains("link"))) {
                continue
            }

            val urlMatch = urlRegex.find(line) ?: continue
            val url = urlMatch.value.trim()
            val beforeUrl = line.substring(0, urlMatch.range.first).trim().trimEnd(',', '\t', ';')
            val afterUrl = line.substring(urlMatch.range.last + 1).trim().trimStart(',', '\t', ';')

            val rawName = if (beforeUrl.isNotBlank()) beforeUrl else {
                afterUrl.split(',', '\t').firstOrNull()?.trim() ?: ""
            }

            val cleanName = rawName.trim('"', '\'').trim()
            if (cleanName.isBlank()) continue

            val englishCandidate = cleanName.split("|").first().trim().lowercase()
            val matchedProduct = existingProducts.find { existing ->
                val existingLower = existing.name.lowercase()
                val existingEng = existing.name.split("|").first().trim().lowercase()
                existingLower == cleanName.lowercase() ||
                existingEng == englishCandidate ||
                (englishCandidate.length >= 4 && (existingLower.contains(englishCandidate) || englishCandidate.contains(existingEng)))
            }

            if (matchedProduct != null) {
                val updatedProd = matchedProduct.copy(
                    imageUrl = url,
                    updatedAt = System.currentTimeMillis()
                )
                productDao.updateProduct(updatedProd)
                val index = existingProducts.indexOfFirst { it.id == matchedProduct.id }
                if (index >= 0) existingProducts[index] = updatedProd
                updated++
            } else {
                val isFruit = cleanName.contains("apple", ignoreCase = true) ||
                        cleanName.contains("grape", ignoreCase = true) ||
                        cleanName.contains("banana", ignoreCase = true) ||
                        cleanName.contains("orange", ignoreCase = true) ||
                        cleanName.contains("mango", ignoreCase = true) ||
                        cleanName.contains("melon", ignoreCase = true) ||
                        cleanName.contains("papaya", ignoreCase = true) ||
                        cleanName.contains("coconut", ignoreCase = true) ||
                        cleanName.contains("pineapple", ignoreCase = true) ||
                        cleanName.contains("fruit", ignoreCase = true) ||
                        cleanName.contains("ফল", ignoreCase = true)

                var parsedPrice = 60.0
                val priceNumberMatch = Regex("""\d+(\.\d+)?""").find(afterUrl)
                if (priceNumberMatch != null) {
                    parsedPrice = priceNumberMatch.value.toDoubleOrNull() ?: 60.0
                }

                val newProduct = Product(
                    name = cleanName,
                    category = if (isFruit) "Fruits" else "Vegetables",
                    unit = "1 kg",
                    price = parsedPrice,
                    mrp = (parsedPrice * 1.15).toInt().toDouble(),
                    stockQty = 50,
                    description = "Freshly sourced premium quality produce delivered straight to your Shapoorji doorstep.",
                    imageUrl = url,
                    isDailyEssential = false,
                    isAvailable = true,
                    allowFractional = true,
                    fractionStepGrams = 250,
                    updatedAt = System.currentTimeMillis()
                )
                val newId = productDao.insertProduct(newProduct)
                existingProducts.add(newProduct.copy(id = newId))
                added++
            }
        }

        return CentralSyncResult(
            success = true,
            updatedCount = updated,
            addedCount = added,
            totalCount = existingProducts.size,
            sourceUrl = sourceUrl,
            message = "Synced sheet ($updated photos updated, $added future products added)."
        )
    }

    /**
     * Publish catalog directly to a GitHub Gist using GitHub REST API.
     */
    suspend fun pushToGitHubGist(
        token: String,
        gistIdInput: String?,
        products: List<Product>,
        description: String = "Shapoorji Delivery Live Produce Catalog & Photos"
    ): GitHubPublishResult = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            return@withContext GitHubPublishResult(
                success = false,
                message = "GitHub Personal Access Token is required to push to GitHub."
            )
        }

        try {
            val jsonContent = exportCatalogAsJson(products)
            val cleanGistId = gistIdInput?.trim()?.ifBlank { null } ?: githubGistId.ifBlank { null }

            val payload = JSONObject().apply {
                put("description", description)
                put("public", true)
                val filesObj = JSONObject().apply {
                    val fileObj = JSONObject().apply {
                        put("content", jsonContent)
                    }
                    put("catalog.json", fileObj)
                }
                put("files", filesObj)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payload.toString().toRequestBody(mediaType)

            val request = if (cleanGistId.isNullOrBlank()) {
                Request.Builder()
                    .url("https://api.github.com/gists")
                    .header("Authorization", "Bearer $cleanToken")
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .post(requestBody)
                    .build()
            } else {
                Request.Builder()
                    .url("https://api.github.com/gists/$cleanGistId")
                    .header("Authorization", "Bearer $cleanToken")
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .patch(requestBody)
                    .build()
            }

            httpClient.newCall(request).execute().use { response ->
                val respBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext GitHubPublishResult(
                        success = false,
                        message = "GitHub API Error ${response.code}: $respBody"
                    )
                }

                val respObj = JSONObject(respBody)
                val returnedId = respObj.optString("id", cleanGistId ?: "")
                val filesObj = respObj.optJSONObject("files")
                val catalogFile = filesObj?.optJSONObject("catalog.json")
                val rawUrl = catalogFile?.optString("raw_url", "") ?: "https://gist.githubusercontent.com/raw/$returnedId/catalog.json"

                // Save configuration
                githubToken = cleanToken
                if (returnedId.isNotBlank()) githubGistId = returnedId
                if (rawUrl.isNotBlank()) centralSyncUrl = rawUrl

                GitHubPublishResult(
                    success = true,
                    rawSyncUrl = rawUrl,
                    gistId = returnedId,
                    message = "Successfully pushed to GitHub Gist! Raw sync URL configured: $rawUrl"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed pushing to GitHub Gist: ${e.message}", e)
            GitHubPublishResult(
                success = false,
                message = "GitHub Gist Push Failed: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * Push catalog directly to a GitHub repository file using GitHub Contents API.
     */
    suspend fun pushToGitHubRepo(
        token: String,
        ownerRepoInput: String,
        pathInput: String = "catalog.json",
        branchInput: String = "main",
        products: List<Product>
    ): GitHubPublishResult = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val cleanOwnerRepo = ownerRepoInput.trim()
        val cleanPath = pathInput.trim().trimStart('/')
        val cleanBranch = branchInput.trim().ifBlank { "main" }

        if (cleanToken.isBlank() || cleanOwnerRepo.isBlank()) {
            return@withContext GitHubPublishResult(
                success = false,
                message = "GitHub Token and Repository (owner/repo) are required."
            )
        }

        try {
            // Check if file already exists to get its SHA
            val checkUrl = "https://api.github.com/repos/$cleanOwnerRepo/contents/$cleanPath?ref=$cleanBranch"
            val checkRequest = Request.Builder()
                .url(checkUrl)
                .header("Authorization", "Bearer $cleanToken")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .get()
                .build()

            var existingSha: String? = null
            try {
                httpClient.newCall(checkRequest).execute().use { checkResp ->
                    if (checkResp.isSuccessful) {
                        val body = checkResp.body?.string() ?: ""
                        val checkObj = JSONObject(body)
                        existingSha = if (checkObj.has("sha")) checkObj.getString("sha") else null
                    }
                }
            } catch (_: Exception) {}

            val jsonContent = exportCatalogAsJson(products)
            val base64Content = Base64.encodeToString(jsonContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            val putPayload = JSONObject().apply {
                put("message", "Update Shapoorji produce catalog and photos [Central Sync]")
                put("content", base64Content)
                put("branch", cleanBranch)
                if (!existingSha.isNullOrBlank()) {
                    put("sha", existingSha)
                }
            }

            val putUrl = "https://api.github.com/repos/$cleanOwnerRepo/contents/$cleanPath"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val putBody = putPayload.toString().toRequestBody(mediaType)

            val putRequest = Request.Builder()
                .url(putUrl)
                .header("Authorization", "Bearer $cleanToken")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .put(putBody)
                .build()

            httpClient.newCall(putRequest).execute().use { putResp ->
                val putRespBody = putResp.body?.string() ?: ""
                if (!putResp.isSuccessful) {
                    return@withContext GitHubPublishResult(
                        success = false,
                        message = "GitHub API Error ${putResp.code}: $putRespBody"
                    )
                }

                val rawUrl = "https://raw.githubusercontent.com/$cleanOwnerRepo/$cleanBranch/$cleanPath"

                // Save configuration
                githubToken = cleanToken
                githubRepo = cleanOwnerRepo
                githubFilePath = cleanPath
                centralSyncUrl = rawUrl

                GitHubPublishResult(
                    success = true,
                    rawSyncUrl = rawUrl,
                    message = "Successfully committed to GitHub ($cleanOwnerRepo/$cleanPath)!\nRaw sync URL set to: $rawUrl"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed pushing to GitHub Repo: ${e.message}", e)
            GitHubPublishResult(
                success = false,
                message = "GitHub Repo Push Failed: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * Export all products as formatted JSON string.
     */
    fun exportCatalogAsJson(products: List<Product>): String {
        val array = JSONArray()
        for (prod in products) {
            val obj = JSONObject().apply {
                put("id", prod.id)
                put("name", prod.name)
                put("category", prod.category)
                put("unit", prod.unit)
                put("price", prod.price)
                put("mrp", prod.mrp)
                put("stockQty", prod.stockQty)
                put("description", prod.description)
                put("imageUrl", prod.imageUrl)
                put("isDailyEssential", prod.isDailyEssential)
                put("isAvailable", prod.isAvailable)
                put("allowFractional", prod.allowFractional)
                put("fractionStepGrams", prod.fractionStepGrams)
                put("updatedAt", prod.updatedAt)
            }
            array.put(obj)
        }
        return array.toString(2).replace("\\/", "/")
    }

    /**
     * Export all products as Sheet 2 compatible CSV string.
     */
    fun exportCatalogAsCsv(products: List<Product>): String {
        val sb = StringBuilder()
        sb.append("Product Name,Image URL,Price,Unit,Category\n")
        for (p in products) {
            val cleanName = p.name.replace("\"", "\"\"")
            val cleanUrl = p.imageUrl.replace("\"", "\"\"")
            sb.append("\"$cleanName\",\"$cleanUrl\",${p.price},\"${p.unit}\",\"${p.category}\"\n")
        }
        return sb.toString()
    }
}
