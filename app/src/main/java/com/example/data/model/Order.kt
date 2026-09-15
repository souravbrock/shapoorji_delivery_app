package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OrderStatus(val label: String) {
    ORDER_RECEIVED("Order Received"),
    ORDER_PACKED("Order Packed"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERED("Delivered");

    fun nextStatus(): OrderStatus? = when (this) {
        ORDER_RECEIVED -> ORDER_PACKED
        ORDER_PACKED -> OUT_FOR_DELIVERY
        OUT_FOR_DELIVERY -> DELIVERED
        DELIVERED -> null
    }

    fun stepIndex(): Int = when (this) {
        ORDER_RECEIVED -> 0
        ORDER_PACKED -> 1
        OUT_FOR_DELIVERY -> 2
        DELIVERED -> 3
    }
}

data class OrderItem(
    val productId: Long,
    val productName: String,
    val unit: String,
    val price: Double,
    val quantity: Double = 1.0,
    val portionLabel: String = ""
) {
    val total: Double get() = price * quantity
    val displayQuantity: String
        get() = if (portionLabel.isNotBlank()) portionLabel else if (quantity == quantity.toLong().toDouble()) "${quantity.toInt()}" else "$quantity"
}

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: String, // e.g., "SPD-2026-8812"
    val invoiceNumber: String, // e.g., "INV-SPD-9402"
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val towerName: String, // e.g., "Tower A8", "Sukhobristi Phase 1 - Tower B14"
    val flatNumber: String, // e.g., "Flat 402, 4th Floor"
    val deliveryNotes: String = "",
    val latitude: Double = 22.5695,
    val longitude: Double = 88.5195,
    val isLocationVerifiedInsideShapoorji: Boolean = true,
    val itemsJson: String, // Serialized list of items
    val itemCount: Int,
    val subtotal: Double,
    val deliveryFee: Double = 0.0,
    val discount: Double = 0.0,
    val grandTotal: Double,
    val paymentMethod: String = "Pay on Delivery (Cash / UPI QR)",
    val status: OrderStatus = OrderStatus.ORDER_RECEIVED,
    val createdAt: Long = System.currentTimeMillis(),
    val packedAt: Long? = null,
    val outForDeliveryAt: Long? = null,
    val deliveredAt: Long? = null
)
