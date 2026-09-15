package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a product in the customer's persistent shopping cart.
 * Supports whole units (1, 2, 3...) and fractional purchases (0.1 for 100g, 0.25 for 250g, 0.5 for 500g).
 */
@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val quantity: Double = 1.0,
    val portionLabel: String = "", // e.g., "100g", "250g", "500g", "1 kg", "1 pc"
    val addedAt: Long = System.currentTimeMillis()
)
