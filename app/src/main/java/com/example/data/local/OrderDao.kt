package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE id = :id")
    fun getOrderById(id: Long): Flow<Order?>

    @Query("SELECT * FROM orders WHERE customerEmail = :email ORDER BY createdAt DESC")
    fun getOrdersByCustomerEmail(email: String): Flow<List<Order>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

    @Query("UPDATE orders SET status = :status, packedAt = CASE WHEN :status = 'ORDER_PACKED' THEN :timestamp ELSE packedAt END, outForDeliveryAt = CASE WHEN :status = 'OUT_FOR_DELIVERY' THEN :timestamp ELSE outForDeliveryAt END, deliveredAt = CASE WHEN :status = 'DELIVERED' THEN :timestamp ELSE deliveredAt END WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, status: OrderStatus, timestamp: Long = System.currentTimeMillis())
}
