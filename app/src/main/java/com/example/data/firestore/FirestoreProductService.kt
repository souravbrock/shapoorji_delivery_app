package com.example.data.firestore

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.Product
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * State representing the Firestore real-time synchronization status.
 */
sealed interface FirestoreSyncState {
    data object Idle : FirestoreSyncState
    data object Connecting : FirestoreSyncState
    data class Connected(
        val itemCount: Int,
        val isFromCache: Boolean,
        val hasPendingWrites: Boolean,
        val lastUpdate: Long = System.currentTimeMillis()
    ) : FirestoreSyncState
    data class Error(val message: String) : FirestoreSyncState
}

/**
 * Centralized Firestore Database Service for the produce and product catalog.
 *
 * Provides:
 * - Real-time snapshot listening for instant price, stock, and photo synchronization across all customer & admin devices.
 * - Robust batch migration of local products into Firestore.
 * - Direct real-time updates for prices, stock counts, descriptions, and pictures.
 * - Seamless offline caching backed by Firestore's persistent cache.
 * - Dynamic fallback and graceful initialization.
 */
class FirestoreProductService(
    private val context: Context,
    private val customFirestore: FirebaseFirestore? = null
) {
    companion object {
        private const val TAG = "FirestoreProductService"
        const val COLLECTION_PRODUCTS = "products"
        private const val PREFS_NAME = "firestore_catalog_prefs"
        private const val KEY_CUSTOM_PROJECT_ID = "custom_project_id"
        private const val KEY_LAST_SYNC_SUMMARY = "last_sync_summary"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val DEFAULT_PROJECT_ID = "shapoorji-delivery"

        @Volatile
        private var INSTANCE: FirestoreProductService? = null

        fun getInstance(context: Context): FirestoreProductService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirestoreProductService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _syncState = MutableStateFlow<FirestoreSyncState>(FirestoreSyncState.Idle)
    val syncState: StateFlow<FirestoreSyncState> = _syncState.asStateFlow()

    private val _lastSyncSummary = MutableStateFlow(
        prefs.getString(KEY_LAST_SYNC_SUMMARY, "Ready to sync with Firestore") ?: "Ready to sync with Firestore"
    )
    val lastSyncSummary: StateFlow<String> = _lastSyncSummary.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(prefs.getLong(KEY_LAST_SYNC_TIME, 0L))
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    var customProjectId: String
        get() = prefs.getString(KEY_CUSTOM_PROJECT_ID, DEFAULT_PROJECT_ID) ?: DEFAULT_PROJECT_ID
        set(value) {
            prefs.edit().putString(KEY_CUSTOM_PROJECT_ID, value.trim()).apply()
        }

    /**
     * Resiliently obtains a FirebaseFirestore instance, safely initializing FirebaseApp if needed.
     */
    val firestore: FirebaseFirestore? by lazy {
        customFirestore ?: try {
            ensureFirebaseAppInitialized()
            val db = FirebaseFirestore.getInstance()
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
                db.firestoreSettings = settings
            } catch (e: Exception) {
                Log.w(TAG, "Firestore settings configuration: ${e.message}")
            }
            db
        } catch (e: Throwable) {
            Log.e(TAG, "Firestore initialization error: ${e.message}", e)
            _syncState.value = FirestoreSyncState.Error(e.message ?: "Firestore initialization error")
            null
        }
    }

    val isAvailable: Boolean
        get() = firestore != null

    private fun ensureFirebaseAppInitialized() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val projectId = customProjectId.ifBlank { DEFAULT_PROJECT_ID }
                val options = FirebaseOptions.Builder()
                    .setApplicationId(context.packageName)
                    .setProjectId(projectId)
                    .setApiKey("AIzaSyFakeKeyForFirestoreCatalogFallback")
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.i(TAG, "Initialized default FirebaseApp with project: $projectId")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "ensureFirebaseAppInitialized: ${e.message}")
        }
    }

    /**
     * Observes real-time updates from Firestore "products" collection.
     * Whenever any device edits a price, picture, or product, this Flow emits the updated list instantly.
     */
    fun observeProductsRealtime(): Flow<List<Product>> = callbackFlow {
        val db = firestore
        if (db == null) {
            Log.w(TAG, "Firestore unavailable for realtime updates")
            _syncState.value = FirestoreSyncState.Error("Firestore database unavailable")
            close()
            return@callbackFlow
        }

        _syncState.value = FirestoreSyncState.Connecting

        val listener = db.collection(COLLECTION_PRODUCTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore snapshot listener error: ${error.message}", error)
                    _syncState.value = FirestoreSyncState.Error(error.message ?: "Realtime listener error")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val products = snapshot.documents.mapNotNull { doc ->
                        doc.toProduct()
                    }

                    _syncState.value = FirestoreSyncState.Connected(
                        itemCount = products.size,
                        isFromCache = snapshot.metadata.isFromCache,
                        hasPendingWrites = snapshot.metadata.hasPendingWrites()
                    )

                    val summary = "Real-time sync: ${products.size} items active"
                    _lastSyncSummary.value = summary
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                    prefs.edit()
                        .putString(KEY_LAST_SYNC_SUMMARY, summary)
                        .putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())
                        .apply()

                    trySend(products)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * One-shot fetch of all products from Firestore.
     */
    suspend fun fetchAllProductsDirect(): Result<List<Product>> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        try {
            val snapshot = db.collection(COLLECTION_PRODUCTS).get().awaitCompat()
            val products = snapshot.documents.mapNotNull { it.toProduct() }
            Result.success(products)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch products from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Migrate local catalog products to Firestore in batches.
     * This seeds the centralized Firestore database with existing products, images, and prices.
     */
    suspend fun migrateCatalogToFirestore(products: List<Product>): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        try {
            var count = 0
            // Batch write in chunks of 400 (Firestore maximum is 500 per batch)
            products.chunked(400).forEach { chunk ->
                val batch = db.batch()
                for (product in chunk) {
                    val docRef = db.collection(COLLECTION_PRODUCTS).document(product.id.toString())
                    batch.set(docRef, product.toFirestoreMap(), SetOptions.merge())
                    count++
                }
                batch.commit().awaitCompat()
            }

            val summary = "Successfully migrated $count products to Firestore"
            _lastSyncSummary.value = summary
            _lastSyncTimestamp.value = System.currentTimeMillis()
            prefs.edit()
                .putString(KEY_LAST_SYNC_SUMMARY, summary)
                .putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())
                .apply()

            _syncState.value = FirestoreSyncState.Connected(
                itemCount = count,
                isFromCache = false,
                hasPendingWrites = false
            )

            Log.i(TAG, "Migrated $count products to Firestore successfully")
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Migration to Firestore failed: ${e.message}", e)
            _syncState.value = FirestoreSyncState.Error("Migration failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Real-time update: updates daily price for a product in Firestore.
     * Propagates instantly to all customer screens.
     */
    suspend fun updateDailyPrice(productId: Long, newPrice: Double): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        try {
            val docRef = db.collection(COLLECTION_PRODUCTS).document(productId.toString())
            val updates = mapOf(
                "price" to newPrice,
                "updatedAt" to System.currentTimeMillis()
            )
            docRef.set(updates, SetOptions.merge()).awaitCompat()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update price in Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time update: updates stock count for a product in Firestore.
     */
    suspend fun updateStock(productId: Long, stockQty: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        try {
            val docRef = db.collection(COLLECTION_PRODUCTS).document(productId.toString())
            val updates = mapOf(
                "stockQty" to stockQty,
                "isAvailable" to (stockQty > 0),
                "updatedAt" to System.currentTimeMillis()
            )
            docRef.set(updates, SetOptions.merge()).awaitCompat()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update stock in Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time update: updates detailed product information (name, price, image URL, category, etc.).
     */
    suspend fun updateProductDetails(
        productId: Long,
        name: String,
        price: Double,
        description: String,
        imageUrl: String,
        unit: String = "1 kg",
        category: String = "Vegetables",
        allowFractional: Boolean = false,
        fractionStepGrams: Int = 250
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        try {
            val docRef = db.collection(COLLECTION_PRODUCTS).document(productId.toString())
            val updates = mapOf(
                "name" to name,
                "price" to price,
                "description" to description,
                "imageUrl" to imageUrl,
                "unit" to unit,
                "category" to category,
                "allowFractional" to allowFractional,
                "fractionStepGrams" to fractionStepGrams,
                "updatedAt" to System.currentTimeMillis()
            )
            docRef.set(updates, SetOptions.merge()).awaitCompat()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update product details in Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Saves or creates a product in Firestore.
     */
    suspend fun saveProduct(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        try {
            val docRef = db.collection(COLLECTION_PRODUCTS).document(product.id.toString())
            docRef.set(product.toFirestoreMap(), SetOptions.merge()).awaitCompat()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save product in Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a product from Firestore.
     */
    suspend fun deleteProduct(productId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore unavailable"))
        try {
            db.collection(COLLECTION_PRODUCTS).document(productId.toString()).delete().awaitCompat()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete product in Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Checks if Firestore products collection has any data.
     */
    suspend fun isCollectionEmpty(): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext true
        try {
            val snapshot = db.collection(COLLECTION_PRODUCTS).limit(1).get().awaitCompat()
            snapshot.isEmpty
        } catch (e: Exception) {
            Log.w(TAG, "Checking if collection is empty: ${e.message}")
            true
        }
    }
}

/**
 * Extension mapper to convert a Product data class into a Firestore Map.
 */
fun Product.toFirestoreMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "name" to name,
        "category" to category,
        "unit" to unit,
        "price" to price,
        "mrp" to mrp,
        "stockQty" to stockQty,
        "description" to description,
        "imageUrl" to imageUrl,
        "isAvailable" to isAvailable,
        "isDailyEssential" to isDailyEssential,
        "allowFractional" to allowFractional,
        "fractionStepGrams" to fractionStepGrams,
        "averageRating" to averageRating.toDouble(),
        "reviewCount" to reviewCount,
        "updatedAt" to updatedAt
    )
}

/**
 * Extension mapper to convert a Firestore DocumentSnapshot into a Product data class.
 */
fun DocumentSnapshot.toProduct(): Product? {
    val name = getString("name") ?: return null
    val rawId = getLong("id") ?: id.toLongOrNull() ?: 0L

    return Product(
        id = rawId,
        name = name,
        category = getString("category") ?: "Vegetables",
        unit = getString("unit") ?: "1 kg",
        price = getDouble("price") ?: (getLong("price")?.toDouble() ?: 0.0),
        mrp = getDouble("mrp") ?: (getLong("mrp")?.toDouble() ?: 0.0),
        stockQty = getLong("stockQty")?.toInt() ?: 50,
        description = getString("description") ?: "",
        imageUrl = getString("imageUrl") ?: "",
        isAvailable = getBoolean("isAvailable") ?: true,
        isDailyEssential = getBoolean("isDailyEssential") ?: false,
        allowFractional = getBoolean("allowFractional") ?: false,
        fractionStepGrams = getLong("fractionStepGrams")?.toInt() ?: 250,
        averageRating = (getDouble("averageRating") ?: 4.8).toFloat(),
        reviewCount = getLong("reviewCount")?.toInt() ?: 12,
        updatedAt = getLong("updatedAt") ?: System.currentTimeMillis()
    )
}

/**
 * Helper coroutine adapter for Google Play Services Task without external dependency conflicts.
 */
suspend fun <T> Task<T>.awaitCompat(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exception ->
        if (cont.isActive) cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        if (cont.isActive) cont.cancel()
    }
}
