package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val customerName: String,
    val customerEmail: String,
    val rating: Int, // 1 to 5
    val comment: String,
    val createdAt: Long = System.currentTimeMillis()
)
