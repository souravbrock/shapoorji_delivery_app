package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String, // e.g., "Vegetables", "Fruits", "Dairy & Breakfast", "Staples & Atta", "Snacks & Drinks", "Personal Care"
    val unit: String, // e.g., "1 kg", "500 g", "1 Packet", "1 Litre", "1 bunch"
    val price: Double, // Current daily price in INR (₹)
    val mrp: Double, // Market Retail Price
    val stockQty: Int = 50,
    val description: String = "",
    val imageUrl: String = "",
    val isAvailable: Boolean = true,
    val isDailyEssential: Boolean = false,
    val averageRating: Float = 4.8f,
    val reviewCount: Int = 12,
    val updatedAt: Long = System.currentTimeMillis()
)
