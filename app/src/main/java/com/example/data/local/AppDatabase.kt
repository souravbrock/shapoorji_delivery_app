package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Favorite
import com.example.data.model.NotificationLog
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
        NotificationLog::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun reviewDao(): ReviewDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun notificationLogDao(): NotificationLogDao

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
            val sampleProducts = listOf(
                Product(
                    name = "Fresh Farm Tomatoes (Desi)",
                    category = "Vegetables",
                    unit = "1 kg",
                    price = 42.0,
                    mrp = 55.0,
                    stockQty = 85,
                    description = "Freshly harvested, naturally vine-ripened red desi tomatoes from Bengal farms.",
                    imageUrl = "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?w=400",
                    isDailyEssential = true,
                    averageRating = 4.8f,
                    reviewCount = 28
                ),
                Product(
                    name = "Farm Fresh Potatoes (Jyoti)",
                    category = "Vegetables",
                    unit = "2 kg",
                    price = 58.0,
                    mrp = 70.0,
                    stockQty = 120,
                    description = "Premium cleaned Jyoti potatoes, perfect for daily curries and aloo posta.",
                    imageUrl = "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=400",
                    isDailyEssential = true,
                    averageRating = 4.7f,
                    reviewCount = 35
                ),
                Product(
                    name = "Fresh Red Onions (Nashik)",
                    category = "Vegetables",
                    unit = "1 kg",
                    price = 48.0,
                    mrp = 60.0,
                    stockQty = 90,
                    description = "Crunchy, pungent pinkish-red onions, essential for every Indian kitchen.",
                    imageUrl = "https://images.unsplash.com/photo-1618512496248-a07fe83aa8cb?w=400",
                    isDailyEssential = true,
                    averageRating = 4.6f,
                    reviewCount = 19
                ),
                Product(
                    name = "Amul Taaza Homogenised Toned Milk",
                    category = "Dairy & Breakfast",
                    unit = "1 Litre (Pouch)",
                    price = 56.0,
                    mrp = 56.0,
                    stockQty = 150,
                    description = "Fresh toned pasteurized milk delivered cold every morning to your tower doorstep.",
                    imageUrl = "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400",
                    isDailyEssential = true,
                    averageRating = 4.9f,
                    reviewCount = 64
                ),
                Product(
                    name = "Amul Salted Butter",
                    category = "Dairy & Breakfast",
                    unit = "500 g",
                    price = 285.0,
                    mrp = 295.0,
                    stockQty = 40,
                    description = "Utterly butterly delicious classic butter block.",
                    imageUrl = "https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d?w=400",
                    isDailyEssential = false,
                    averageRating = 4.9f,
                    reviewCount = 42
                ),
                Product(
                    name = "Fresh Green Cauliflower (Phoolgobhi)",
                    category = "Vegetables",
                    unit = "1 piece (~600g)",
                    price = 35.0,
                    mrp = 45.0,
                    stockQty = 45,
                    description = "Firm white florets with crisp green leaves, fresh from Barasat mandi.",
                    imageUrl = "https://images.unsplash.com/photo-1568584711075-3d021a7c3ca3?w=400",
                    isDailyEssential = false,
                    averageRating = 4.5f,
                    reviewCount = 14
                ),
                Product(
                    name = "Fresh Shimla Green Apples",
                    category = "Fruits",
                    unit = "1 kg (4-5 pcs)",
                    price = 180.0,
                    mrp = 220.0,
                    stockQty = 30,
                    description = "Crisp, sweet and juicy hill apples directly sourced from Himachal orchards.",
                    imageUrl = "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=400",
                    isDailyEssential = false,
                    averageRating = 4.7f,
                    reviewCount = 22
                ),
                Product(
                    name = "Ripe Robusta Bananas",
                    category = "Fruits",
                    unit = "1 Dozen (12 pcs)",
                    price = 65.0,
                    mrp = 80.0,
                    stockQty = 60,
                    description = "Naturally ripened golden Robusta bananas rich in potassium and energy.",
                    imageUrl = "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=400",
                    isDailyEssential = true,
                    averageRating = 4.8f,
                    reviewCount = 31
                ),
                Product(
                    name = "Aashirvaad Shudh Chakki Atta",
                    category = "Staples & Atta",
                    unit = "5 kg",
                    price = 245.0,
                    mrp = 275.0,
                    stockQty = 50,
                    description = "100% pure whole wheat flour ground to perfection for soft rotis.",
                    imageUrl = "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=400",
                    isDailyEssential = true,
                    averageRating = 4.9f,
                    reviewCount = 58
                ),
                Product(
                    name = "Fortune Sunlite Refined Sunflower Oil",
                    category = "Staples & Atta",
                    unit = "1 Litre Pouch",
                    price = 142.0,
                    mrp = 165.0,
                    stockQty = 70,
                    description = "Light and healthy cooking oil enriched with vitamins A & D.",
                    imageUrl = "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400",
                    isDailyEssential = true,
                    averageRating = 4.7f,
                    reviewCount = 18
                ),
                Product(
                    name = "Tata Salt Iodized Crystal Salt",
                    category = "Staples & Atta",
                    unit = "1 kg",
                    price = 28.0,
                    mrp = 30.0,
                    stockQty = 100,
                    description = "Desh ka namak, vacuum evaporated iodized salt.",
                    imageUrl = "https://images.unsplash.com/photo-1518843875459-f738682238a6?w=400",
                    isDailyEssential = true,
                    averageRating = 4.9f,
                    reviewCount = 47
                ),
                Product(
                    name = "Fresh Coriander Leaves (Dhania)",
                    category = "Vegetables",
                    unit = "100 g Bunch",
                    price = 15.0,
                    mrp = 20.0,
                    stockQty = 75,
                    description = "Aromatic freshly plucked coriander bunch for daily garnishing.",
                    imageUrl = "https://images.unsplash.com/photo-1526318896980-cf78c088247c?w=400",
                    isDailyEssential = true,
                    averageRating = 4.6f,
                    reviewCount = 15
                )
            )

            productDao.insertAll(sampleProducts)

            // Seed a few sample reviews
            val sampleReviews = listOf(
                Review(productId = 1, customerName = "Priya Sen", customerEmail = "priya.sen@gmail.com", rating = 5, comment = "Delivered to Sukhobristi Tower A4 in just 20 minutes! Super fresh desi tomatoes."),
                Review(productId = 1, customerName = "Amitabh Guha", customerEmail = "amitabh.g@gmail.com", rating = 5, comment = "Very fresh, better quality than nearby local market and delivered to the flat."),
                Review(productId = 4, customerName = "Sourav Brock", customerEmail = "souravbrock@gmail.com", rating = 5, comment = "Best morning milk delivery service inside Shapoorji complex!"),
                Review(productId = 9, customerName = "Debashis M", customerEmail = "debashis@gmail.com", rating = 5, comment = "Authentic pack, neatly packed with invoice.")
            )
            sampleReviews.forEach { reviewDao.insertReview(it) }
        }
    }
}
