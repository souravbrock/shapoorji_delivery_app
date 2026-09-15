package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.NotificationType
import com.example.data.model.OrderStatus

class Converters {
    @TypeConverter
    fun fromOrderStatus(value: OrderStatus): String = value.name

    @TypeConverter
    fun toOrderStatus(value: String): OrderStatus = try {
        OrderStatus.valueOf(value)
    } catch (e: Exception) {
        OrderStatus.ORDER_RECEIVED
    }

    @TypeConverter
    fun fromNotificationType(value: NotificationType): String = value.name

    @TypeConverter
    fun toNotificationType(value: String): NotificationType = try {
        NotificationType.valueOf(value)
    } catch (e: Exception) {
        NotificationType.EMAIL
    }
}
