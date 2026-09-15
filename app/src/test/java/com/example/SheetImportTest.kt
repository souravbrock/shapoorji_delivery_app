package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Product
import com.example.data.notification.NotificationDispatcher
import com.example.data.repository.GroceryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SheetImportTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: GroceryRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val dispatcher = NotificationDispatcher(context, database.notificationLogDao())
        repository = GroceryRepository(
            productDao = database.productDao(),
            orderDao = database.orderDao(),
            reviewDao = database.reviewDao(),
            favoriteDao = database.favoriteDao(),
            notificationLogDao = database.notificationLogDao(),
            cartDao = database.cartDao(),
            dispatcher = dispatcher
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testSheetImportUpdatesExistingAndAddsFutureProducts() = runBlocking {
        // Insert sample products
        val potato = Product(
            id = 1L,
            name = "Chandramukhi Potato | চন্দ্রমুখী আলু | चंद्रमुखी आलू",
            category = "Vegetables",
            unit = "1 kg",
            price = 38.0,
            mrp = 45.0,
            stockQty = 100,
            description = "Starchy sweet local potato",
            imageUrl = "file:///android_asset/products/chandramukhi_potato.png"
        )
        val onion = Product(
            id = 2L,
            name = "Nasik Onion | নাসিক পেঁয়াজ | नासिक प्याज",
            category = "Vegetables",
            unit = "1 kg",
            price = 42.0,
            mrp = 50.0,
            stockQty = 80,
            description = "Crisp pink Nasik onion",
            imageUrl = "file:///android_asset/products/nasik_onion.png"
        )
        database.productDao().insertAll(listOf(potato, onion))

        val sheetData = """
            Product Name, Image URL, Price, Unit
            Chandramukhi Potato, https://images.unsplash.com/potato-new-photo.jpg
            Broccoli | ব্রোকলি, https://images.unsplash.com/broccoli.jpg, 75, 1 pc
            Dragon Fruit, https://images.unsplash.com/dragonfruit.jpg, 120, 1 pc
        """.trimIndent()

        val result = repository.importSheetData(sheetData)

        assertEquals(1, result.updatedCount)
        assertEquals(2, result.addedCount)
        assertEquals(0, result.skippedCount)

        // Verify existing item's imageUrl was updated
        val updatedPotato = database.productDao().getProductByIdDirect(1L)!!
        assertEquals("https://images.unsplash.com/potato-new-photo.jpg", updatedPotato.imageUrl)

        // Verify future products were added
        val allProducts = database.productDao().getAllProductsDirect()
        assertEquals(4, allProducts.size)

        val broccoli = allProducts.find { it.name.contains("Broccoli") }
        assertTrue(broccoli != null)
        assertEquals("https://images.unsplash.com/broccoli.jpg", broccoli!!.imageUrl)
        assertEquals("Vegetables", broccoli.category)

        val dragonFruit = allProducts.find { it.name.contains("Dragon Fruit") }
        assertTrue(dragonFruit != null)
        assertEquals("https://images.unsplash.com/dragonfruit.jpg", dragonFruit!!.imageUrl)
        assertEquals("Fruits", dragonFruit.category)
    }
}
