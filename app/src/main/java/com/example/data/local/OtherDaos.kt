package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Favorite
import com.example.data.model.NotificationLog
import com.example.data.model.Review
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE productId = :productId ORDER BY createdAt DESC")
    fun getReviewsForProduct(productId: Long): Flow<List<Review>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review): Long

    @Query("SELECT AVG(rating) FROM reviews WHERE productId = :productId")
    suspend fun getAverageRating(productId: Long): Float?

    @Query("SELECT COUNT(*) FROM reviews WHERE productId = :productId")
    suspend fun getReviewCount(productId: Long): Int
}

@Dao
interface FavoriteDao {
    @Query("SELECT productId FROM favorites")
    fun getAllFavoriteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE productId = :productId")
    suspend fun removeFavorite(productId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE productId = :productId)")
    fun isFavorite(productId: Long): Flow<Boolean>
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<NotificationLog>>

    @Query("SELECT * FROM notification_logs WHERE orderId = :orderId ORDER BY timestamp DESC")
    fun getLogsForOrder(orderId: Long): Flow<List<NotificationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: NotificationLog): Long
}
