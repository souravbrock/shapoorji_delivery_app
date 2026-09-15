package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Product
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object (DAO) for managing the product catalog and inventory.
 */
@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY isDailyEssential DESC, name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products")
    suspend fun getAllProductsDirect(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id")
    fun getProductById(id: Long): Flow<Product?>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductByIdDirect(id: Long): Product?

    @Query("SELECT * FROM products WHERE category = :category ORDER BY name ASC")
    fun getProductsByCategory(category: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE stockQty <= :threshold ORDER BY stockQty ASC")
    fun getLowStockProducts(threshold: Int = 20): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET stockQty = :stockQty, isAvailable = :isAvailable, updatedAt = :updatedAt WHERE id = :productId")
    suspend fun updateStock(
        productId: Long,
        stockQty: Int,
        isAvailable: Boolean = stockQty > 0,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE products SET price = :price, updatedAt = :updatedAt WHERE id = :productId")
    suspend fun updateDailyPrice(
        productId: Long,
        price: Double,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE products 
        SET name = :name, price = :price, description = :description, imageUrl = :imageUrl, unit = :unit, category = :category, 
            allowFractional = :allowFractional, fractionStepGrams = :fractionStepGrams, updatedAt = :updatedAt 
        WHERE id = :productId
    """)
    suspend fun updateProductDetails(
        productId: Long,
        name: String,
        price: Double,
        description: String,
        imageUrl: String,
        unit: String,
        category: String,
        allowFractional: Boolean = false,
        fractionStepGrams: Int = 250,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Long)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int
}
