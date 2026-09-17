package com.example.data.website

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Account on the Shapoorji Delivery website (spdelivery.reddevils.co.in).
 * This is the durable identity: unlike local Room/SharedPreferences data,
 * it survives app reinstalls and is shared with the web storefront.
 */
data class WebsiteUser(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = ""
)

data class WebsiteOrderItem(
    val productId: String,
    val productName: String,
    val quantity: Double,
    val unit: String,
    val price: Double
)

data class WebsiteOrderSummary(
    val id: String,
    val status: String,
    val total: Double,
    val createdAt: String,
    val itemCount: Int = 0
)

sealed interface WebsiteAuthState {
    data object SignedOut : WebsiteAuthState
    data object Loading : WebsiteAuthState
    data class SignedIn(val user: WebsiteUser) : WebsiteAuthState
    data class Error(val message: String) : WebsiteAuthState
}

/**
 * Cookie jar persisted in SharedPreferences so the website session survives
 * app restarts and app updates (re-login after reinstall restores everything
 * server-side: profile + order history).
 */
private class PersistentCookieJar(context: Context) : CookieJar {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("website_session_prefs", Context.MODE_PRIVATE)

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val stored = prefs.getStringSet(KEY_COOKIES, emptySet())!!.toMutableSet()
        for (cookie in cookies) {
            stored.removeAll { it.startsWith(cookie.name + "|") }
            if (!cookie.persistent || cookie.expiresAt > System.currentTimeMillis()) {
                stored.add(
                    "${cookie.name}|${cookie.value}|${cookie.domain}|" +
                        "${cookie.path}|${cookie.secure}|${cookie.hostOnly}|${cookie.expiresAt}"
                )
            }
        }
        prefs.edit().putStringSet(KEY_COOKIES, stored).apply()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val out = mutableListOf<Cookie>()
        for (entry in prefs.getStringSet(KEY_COOKIES, emptySet())!!) {
            try {
                val p = entry.split("|")
                if (p.size != 7) continue
                val expiresAt = p[6].toLong()
                if (expiresAt <= now) continue
                val builder = Cookie.Builder()
                    .name(p[0]).value(p[1])
                    .expiresAt(expiresAt)
                    .path(p[3])
                if (p[5].toBoolean()) builder.hostOnlyDomain(p[2]) else builder.domain(p[2])
                if (p[4].toBoolean()) builder.secure()
                val cookie = builder.build()
                if (cookie.matches(url)) out.add(cookie)
            } catch (_: Exception) {
            }
        }
        return out
    }

    fun clear() {
        prefs.edit().remove(KEY_COOKIES).apply()
    }

    companion object {
        private const val KEY_COOKIES = "website_cookies"
    }
}

/**
 * HTTP client for the website backend (https://reddevils.co.in/api/*).
 * Same API the spdelivery.reddevils.co.in storefront uses: cookie sessions,
 * JSON bodies, no API keys. All calls are best-effort safe (Result-based).
 */
class WebsiteBackend(context: Context) {

    companion object {
        const val BASE_URL = "https://reddevils.co.in"
        const val PRODUCTS_URL = "$BASE_URL/api/products?activeOnly=1&orderBy=name"
        private const val TAG = "WebsiteBackend"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    private val appContext = context.applicationContext
    private val cookieJar = PersistentCookieJar(appContext)
    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun baseUrl(): HttpUrl = BASE_URL.toHttpUrl()

    private fun get(path: String): JSONObject? {
        val request = Request.Builder()
            .url(BASE_URL + path)
            .header("Accept", "application/json")
            .header("User-Agent", "ShapoorjiDelivery-AndroidApp")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: return null
            if (!response.isSuccessful) {
                Log.w(TAG, "GET $path -> HTTP ${response.code}: $body")
                return null
            }
            return try {
                JSONObject(body)
            } catch (e: Exception) {
                Log.w(TAG, "GET $path: not JSON: ${e.message}")
                null
            }
        }
    }

    private fun post(path: String, payload: JSONObject): Pair<Int, JSONObject?> {
        val request = Request.Builder()
            .url(BASE_URL + path)
            .header("Accept", "application/json")
            .header("User-Agent", "ShapoorjiDelivery-AndroidApp")
            .post(payload.toString().toRequestBody(JSON))
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            val json = try {
                if (body.isBlank()) null else JSONObject(body)
            } catch (_: Exception) {
                null
            }
            return response.code to json
        }
    }

    private fun patch(path: String, payload: JSONObject): Pair<Int, JSONObject?> {
        val request = Request.Builder()
            .url(BASE_URL + path)
            .header("Accept", "application/json")
            .header("User-Agent", "ShapoorjiDelivery-AndroidApp")
            .patch(payload.toString().toRequestBody(JSON))
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            val json = try {
                if (body.isBlank()) null else JSONObject(body)
            } catch (_: Exception) {
                null
            }
            return response.code to json
        }
    }

    private fun parseUser(root: JSONObject): WebsiteUser? {
        // Session shape: {user: {...}, profile: {...}}; be liberal in what we accept.
        val userObj = root.optJSONObject("user")
        val profileObj = root.optJSONObject("profile")
        val flatEmail = root.optString("email", "")
        if (userObj == null && profileObj == null && flatEmail.isBlank()) return null

        fun pick(vararg values: String): String = values.firstOrNull { it.isNotBlank() } ?: ""
        val email = pick(
            userObj?.optString("email", "") ?: "",
            profileObj?.optString("email", "") ?: "",
            flatEmail
        )
        if (email.isBlank()) return null
        return WebsiteUser(
            fullName = pick(
                profileObj?.optString("full_name", "") ?: "",
                userObj?.optString("full_name", "") ?: "",
                userObj?.optString("name", "") ?: "",
                root.optString("full_name", "")
            ),
            email = email,
            phone = pick(
                profileObj?.optString("phone", "") ?: "",
                userObj?.optString("phone", "") ?: "",
                root.optString("phone", "")
            ),
            address = pick(
                profileObj?.optString("address", "") ?: "",
                userObj?.optString("address", "") ?: "",
                root.optString("address", "")
            )
        )
    }

    suspend fun fetchSession(): WebsiteUser? = withContext(Dispatchers.IO) {
        try {
            val root = get("/api/auth/session") ?: return@withContext null
            parseUser(root)
        } catch (e: Exception) {
            Log.w(TAG, "fetchSession: ${e.message}")
            null
        }
    }

    suspend fun login(email: String, password: String): Result<WebsiteUser> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put("email", email.trim())
                .put("password", password)
            val (code, json) = post("/api/auth/login", payload)
            if (code in 200..299 && json != null) {
                val user = parseUser(json) ?: fetchSession()
                if (user != null) return@withContext Result.success(user)
                return@withContext Result.failure(IllegalStateException("Signed in but profile unreadable"))
            }
            val err = json?.optString("error", "")?.ifBlank { "Login failed (HTTP $code)" }
                ?: "Login failed (HTTP $code)"
            Result.failure(IllegalStateException(err))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Login error: ${e.message}"))
        }
    }

    suspend fun signup(email: String, password: String): Result<WebsiteUser> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put("email", email.trim())
                .put("password", password)
            val (code, json) = post("/api/auth/signup", payload)
            if (code in 200..299) {
                val user = (json?.let { parseUser(it) }) ?: fetchSession()
                if (user != null) return@withContext Result.success(user)
                return@withContext Result.failure(IllegalStateException("Account created — please sign in"))
            }
            val err = json?.optString("error", "")?.ifBlank { "Signup failed (HTTP $code)" }
                ?: "Signup failed (HTTP $code)"
            Result.failure(IllegalStateException(err))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Signup error: ${e.message}"))
        }
    }

    suspend fun updateProfile(fullName: String, phone: String, address: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject()
                    .put("full_name", fullName)
                    .put("phone", phone)
                    .put("address", address)
                val (code, _) = patch("/api/auth/profile", payload)
                code in 200..299
            } catch (e: Exception) {
                Log.w(TAG, "updateProfile: ${e.message}")
                false
            }
        }

    suspend fun logout(): Unit = withContext(Dispatchers.IO) {
        try {
            post("/api/auth/logout", JSONObject())
        } catch (_: Exception) {
        } finally {
            cookieJar.clear()
        }
    }

    suspend fun hasSession(): Boolean = fetchSession() != null

    /**
     * Submit the order to the website so it appears in the store admin panel
     * and survives app reinstalls. Resolves app products to website product
     * UUIDs by English name (same matching as catalog sync).
     */
    suspend fun placeOrder(
        customerName: String,
        customerPhone: String,
        tower: String,
        flat: String,
        notes: String,
        items: List<WebsiteOrderItem>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put("delivery_address", "$flat, $tower".trim().trim(',').ifBlank { tower })
                .put("customer_name", customerName)
                .put("customer_phone", customerPhone)
                .put("tower", tower)
                .put("flat", flat)
                .put("notes", notes)
                .put("items", JSONArray().apply {
                    for (item in items) {
                        put(
                            JSONObject()
                                .put("product_id", item.productId)
                                .put("product_name", item.productName)
                                .put("quantity", item.quantity)
                                .put("unit", item.unit)
                                .put("price", item.price)
                        )
                    }
                })
            val (code, json) = post("/api/orders", payload)
            if (code in 200..299 && json != null) {
                val id = json.optString("id", "")
                    .ifBlank { json.optJSONObject("order")?.optString("id", "") ?: "" }
                return@withContext Result.success(id)
            }
            val err = json?.optString("error", "")?.ifBlank { "Website order failed (HTTP $code)" }
                ?: "Website order failed (HTTP $code)"
            Result.failure(IllegalStateException(err))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Website order error: ${e.message}"))
        }
    }

    /**
     * Map app cart lines to website product UUIDs using the live website
     * catalog (matched by English product name).
     */
    suspend fun resolveWebsiteItems(
        lines: List<Triple<String, Double, Pair<String, Double>>>
    ): List<WebsiteOrderItem> = withContext(Dispatchers.IO) {
        // lines: (englishName, quantity, (unit, price))
        val idByEnglish = mutableMapOf<String, String>()
        try {
            val request = Request.Builder()
                .url(PRODUCTS_URL)
                .header("Accept", "application/json")
                .header("User-Agent", "ShapoorjiDelivery-AndroidApp")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@use
                if (!response.isSuccessful) return@use
                val arr = JSONArray(body)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val name = obj.optString("name", "")
                    val english = name.split("|").first().trim().lowercase()
                    if (english.isNotBlank()) {
                        idByEnglish.putIfAbsent(english, obj.optString("id", ""))
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "resolveWebsiteItems catalog fetch: ${e.message}")
        }
        lines.mapNotNull { (englishName, quantity, unitPrice) ->
            val uuid = idByEnglish[englishName.trim().lowercase()] ?: return@mapNotNull null
            if (uuid.isBlank()) return@mapNotNull null
            WebsiteOrderItem(
                productId = uuid,
                productName = englishName,
                quantity = quantity,
                unit = unitPrice.first,
                price = unitPrice.second
            )
        }
    }

    suspend fun fetchOrders(): List<WebsiteOrderSummary> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/orders")
                .header("Accept", "application/json")
                .header("User-Agent", "ShapoorjiDelivery-AndroidApp")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@use emptyList()
                if (!response.isSuccessful) {
                    Log.w(TAG, "fetchOrders -> HTTP ${response.code}")
                    return@use emptyList()
                }
                val arr = try {
                    if (body.trim().startsWith("[")) JSONArray(body)
                    else JSONObject(body).optJSONArray("orders") ?: JSONArray()
                } catch (_: Exception) {
                    return@use emptyList()
                }
                val out = mutableListOf<WebsiteOrderSummary>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "")
                    if (id.isBlank()) continue
                    val total = obj.optDouble("grand_total",
                        obj.optDouble("total",
                            obj.optDouble("amount", 0.0)))
                    val items = obj.optJSONArray("items")
                    out.add(
                        WebsiteOrderSummary(
                            id = id,
                            status = obj.optString("status", "received"),
                            total = total,
                            createdAt = obj.optString("created_at",
                                obj.optString("createdAt", "")),
                            itemCount = items?.length() ?: obj.optInt("item_count",
                                obj.optInt("itemCount", 0))
                        )
                    )
                }
                out
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchOrders: ${e.message}")
            emptyList()
        }
    }
}
