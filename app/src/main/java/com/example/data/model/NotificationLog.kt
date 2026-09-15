package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NotificationType {
    EMAIL,
    TELEGRAM
}

@Entity(tableName = "notification_logs")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: Long,
    val orderNumber: String,
    val type: NotificationType,
    val sender: String, // e.g., "order@spdelivery.reddevils.co.in"
    val recipient: String, // e.g., "customer@gmail.com, souravbrock@gmail.com" or "Admin & Staff Bot"
    val title: String,
    val content: String,
    val status: String = "DELIVERED", // "DELIVERED", "QUEUED", "FAILED"
    val timestamp: Long = System.currentTimeMillis()
)
