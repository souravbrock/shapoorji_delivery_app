package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey
    val productId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
