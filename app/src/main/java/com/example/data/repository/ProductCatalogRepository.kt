package com.example.data.repository

import com.example.data.local.ProductDao
import com.example.data.model.OfficialCatalog
import com.example.data.model.Product
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository class to manage the product catalog and inventory in Room database.
 *
 * Provides reactive [Flow] data streams and suspending functions for inventory management,
 * with full support for product name, price, description, image URL, category, unit,
 * and live stock counts.
 */
class ProductCatalogRepository(
    private val productDao: ProductDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Reactive stream of all products in the catalog, ordered by essentials and name.
     */
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    /**
     * Get a single product by ID as a reactive Flow.
     */
    fun getProductById(id: Long): Flow<Product?> = productDao.getProductById(id)

    /**
     * Direct one-shot retrieval of a product by ID.
     */
    suspend fun getProductDirect(id: Long): Product? = withContext(dispatcher) {
        productDao.getProductByIdDirect(id)
    }

    /**
     * Filter products by category (e.g., "Vegetables", "Fruits").
     */
    fun getProductsByCategory(category: String): Flow<List<Product>> =
        productDao.getProductsByCategory(category)

    /**
     * Reactive stream of low-stock items requiring replenishment by the admin.
     */
    fun getLowStockProducts(threshold: Int = 20): Flow<List<Product>> =
        productDao.getLowStockProducts(threshold)

    /**
     * Search products by name, category, or description.
     */
    fun searchProducts(query: String): Flow<List<Product>> =
        productDao.searchProducts(query)

    /**
     * Add a new product into the catalog with full details.
     */
    suspend fun addProduct(
        name: String,
        price: Double,
        description: String,
        imageUrl: String,
        category: String = "Vegetables",
        unit: String = "1 kg",
        stockQty: Int = 50,
        mrp: Double = price * 1.15,
        isDailyEssential: Boolean = false,
        allowFractional: Boolean = false,
        fractionStepGrams: Int = 250
    ): Long = withContext(dispatcher) {
        val newProduct = Product(
            name = name,
            category = category,
            unit = unit,
            price = price,
            mrp = mrp,
            stockQty = stockQty,
            description = description,
            imageUrl = imageUrl,
            isAvailable = stockQty > 0,
            isDailyEssential = isDailyEssential,
            allowFractional = allowFractional,
            fractionStepGrams = fractionStepGrams,
            updatedAt = System.currentTimeMillis()
        )
        productDao.insertProduct(newProduct)
    }

    /**
     * Insert or replace an existing Product object.
     */
    suspend fun insertProduct(product: Product): Long = withContext(dispatcher) {
        productDao.insertProduct(product)
    }

    /**
     * Insert a batch list of products.
     */
    suspend fun insertProducts(products: List<Product>) = withContext(dispatcher) {
        productDao.insertAll(products)
    }

    /**
     * Update full product record in inventory.
     */
    suspend fun updateProduct(product: Product) = withContext(dispatcher) {
        productDao.updateProduct(product.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * Update product details (name, price, description, imageUrl, unit, category, fractional settings).
     */
    suspend fun updateProductDetails(
        productId: Long,
        name: String,
        price: Double,
        description: String,
        imageUrl: String,
        unit: String,
        category: String,
        allowFractional: Boolean = false,
        fractionStepGrams: Int = 250
    ) = withContext(dispatcher) {
        productDao.updateProductDetails(
            productId = productId,
            name = name,
            price = price,
            description = description,
            imageUrl = imageUrl,
            unit = unit,
            category = category,
            allowFractional = allowFractional,
            fractionStepGrams = fractionStepGrams,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Admin: Update daily mandi rate / price for a product.
     */
    suspend fun updateProductPrice(productId: Long, newPrice: Double) = withContext(dispatcher) {
        productDao.updateDailyPrice(
            productId = productId,
            price = newPrice,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Admin: Update inventory stock quantity and availability status.
     */
    suspend fun updateInventoryStock(productId: Long, newStockQty: Int) = withContext(dispatcher) {
        productDao.updateStock(
            productId = productId,
            stockQty = newStockQty,
            isAvailable = newStockQty > 0,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Admin: Remove a product from inventory.
     */
    suspend fun deleteProduct(product: Product) = withContext(dispatcher) {
        productDao.deleteProduct(product)
    }

    /**
     * Admin: Remove a product by ID.
     */
    suspend fun deleteProductById(id: Long) = withContext(dispatcher) {
        productDao.deleteProductById(id)
    }

    /**
     * Total number of products in the database.
     */
    suspend fun getProductCount(): Int = withContext(dispatcher) {
        productDao.getProductCount()
    }

    /**
     * Sync official catalog if database is empty or outdated.
     */
    suspend fun syncOfficialCatalog() = withContext(dispatcher) {
        productDao.insertAll(OfficialCatalog.INITIAL_PRODUCTS)
    }

    /**
     * Admin: Reset catalog back to the official 37 produce items.
     */
    suspend fun resetToOfficialCatalog() = withContext(dispatcher) {
        productDao.deleteAllProducts()
        productDao.insertAll(OfficialCatalog.INITIAL_PRODUCTS)
    }
}
