package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CartItemEntity
import com.example.data.model.Favorite
import com.example.data.model.NotificationLog
import com.example.data.model.OfficialCatalog
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.Review
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Product::class,
        Order::class,
        Review::class,
        Favorite::class,
        NotificationLog::class,
        CartItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun reviewDao(): ReviewDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun cartDao(): CartDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shapoorji_delivery_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialGroceries(database.productDao(), database.reviewDao())
                    }
                }
            }
        }

        suspend fun populateInitialGroceries(productDao: ProductDao, reviewDao: ReviewDao) {
            productDao.insertAll(OfficialCatalog.INITIAL_PRODUCTS)

            // Seed sample resident reviews for official produce
            val sampleReviews = listOf(
                Review(productId = 1, customerName = "Priya Sen", customerEmail = "priya.sen@gmail.com", rating = 5, comment = "Chandramukhi potatoes delivered to Tower A4 in 18 minutes! Super creamy and fresh."),
                Review(productId = 2, customerName = "Amitabh Guha", customerEmail = "amitabh.g@gmail.com", rating = 5, comment = "Himalini potatoes are clean and high quality. Perfect mandi price."),
                Review(productId = 4, customerName = "Sourav Brock", customerEmail = "souravbrock@gmail.com", rating = 5, comment = "Crisp pink onions at ₹60/kg delivered to doorstep. Best grocery service in Sukhobristi!"),
                Review(productId = 22, customerName = "Debashis M", customerEmail = "debashis@gmail.com", rating = 5, comment = "Spinach bunch was crisp, green and dirt-free. Delivered fresh early morning.")
            )
            sampleReviews.forEach { reviewDao.insertReview(it) }
        }

        suspend fun syncOfficialCatalog(productDao: ProductDao) {
            val count = productDao.getProductCount()
            if (count == 0 || count != OfficialCatalog.INITIAL_PRODUCTS.size) {
                // Synchronize official catalog
                productDao.insertAll(OfficialCatalog.INITIAL_PRODUCTS)
            }
        }
    }
}
