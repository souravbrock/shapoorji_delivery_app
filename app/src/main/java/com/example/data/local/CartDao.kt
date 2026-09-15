package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CartItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object (DAO) for the persistent shopping cart.
 */
@Dao
interface CartDao {

    @Query("SELECT * FROM cart_items ORDER BY addedAt ASC")
    fun getAllCartItems(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE productId = :productId LIMIT 1")
    suspend fun getCartItemByProductId(productId: Long): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItemEntity): Long

    @Update
    suspend fun updateCartItem(item: CartItemEntity)

    @Query("UPDATE cart_items SET quantity = :quantity, portionLabel = :portionLabel WHERE productId = :productId")
    suspend fun updateQuantity(productId: Long, quantity: Double, portionLabel: String)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun deleteCartItem(productId: Long)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    @Query("SELECT COUNT(*) FROM cart_items")
    fun getCartCount(): Flow<Int>
}
