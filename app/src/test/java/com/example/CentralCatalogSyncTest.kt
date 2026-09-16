package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Product
import com.example.data.sync.CentralCatalogSyncManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CentralCatalogSyncTest {

    private lateinit var database: AppDatabase
    private lateinit var syncManager: CentralCatalogSyncManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        syncManager = CentralCatalogSyncManager(context, database.productDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testParseAndApplyCatalogJsonUpdatesProduceAndAddsFutureItems() = runBlocking {
        // Seed initial local products
        database.productDao().insertProduct(
            Product(
                id = 1L,
                name = "Chandramukhi Potato | চন্দ্রমুখী আলু | चंद्रमुखी आलू",
                category = "Vegetables",
                unit = "1 kg",
                price = 38.0,
                mrp = 45.0,
                stockQty = 100,
                description = "Local potato",
                imageUrl = "file:///android_asset/products/chandramukhi_potato.png"
            )
        )

        // Remote JSON representing GitHub sync data
        val remoteJson = """
            [
              {
                "id": 1,
                "name": "Chandramukhi Potato | চন্দ্রমুখী আলু | चंद्रमुखी आलू",
                "price": 40.0,
                "imageUrl": "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=800",
                "category": "Vegetables",
                "unit": "1 kg"
              },
              {
                "name": "Organic Dragon Fruit | ড্রাগন ফল",
                "price": 140.0,
                "mrp": 160.0,
                "imageUrl": "https://images.unsplash.com/photo-1527325678964-54921661f888?w=800",
                "category": "Fruits",
                "unit": "500 g"
              }
            ]
        """.trimIndent()

        val result = syncManager.parseAndApplyCatalog(remoteJson, "GitHub Raw Test")

        assertTrue(result.success)
        assertEquals(1, result.updatedCount)
        assertEquals(1, result.addedCount)

        // Verify local Room database was updated
        val allProducts = database.productDao().getAllProductsDirect()
        assertEquals(2, allProducts.size)

        val updatedPotato = allProducts.firstOrNull { it.id == 1L }
        assertNotNull(updatedPotato)
        assertEquals("https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=800", updatedPotato?.imageUrl)
        assertEquals(40.0, updatedPotato?.price ?: 0.0, 0.01)

        val dragonFruit = allProducts.firstOrNull { it.name.contains("Dragon Fruit") }
        assertNotNull(dragonFruit)
        assertEquals("Fruits", dragonFruit?.category)
        assertEquals("500 g", dragonFruit?.unit)
        assertEquals("https://images.unsplash.com/photo-1527325678964-54921661f888?w=800", dragonFruit?.imageUrl)
    }

    @Test
    fun testExportCatalogJsonAndCsv() = runBlocking {
        val testProducts = listOf(
            Product(
                id = 10L,
                name = "Fresh Broccoli | ব্রকলি",
                category = "Vegetables",
                unit = "500 g",
                price = 55.5,
                mrp = 70.0,
                stockQty = 25,
                description = "Fresh green broccoli",
                imageUrl = "https://example.com/broccoli.jpg"
            )
        )

        val json = syncManager.exportCatalogAsJson(testProducts)
        assertTrue(json.contains("Fresh Broccoli"))
        assertTrue(json.contains("https://example.com/broccoli.jpg"))
        assertTrue(json.contains("55.5"))

        val csv = syncManager.exportCatalogAsCsv(testProducts)
        assertTrue(csv.contains("Fresh Broccoli | ব্রকলি"))
        assertTrue(csv.contains("https://example.com/broccoli.jpg"))
        assertTrue(csv.contains("55.5"))
    }

    @Test
    fun testSettingsPersistence() {
        syncManager.centralSyncUrl = "https://raw.githubusercontent.com/test/repo/main/catalog.json"
        syncManager.isAutoSyncEnabled = true
        syncManager.githubToken = "ghp_mock_token_123"
        syncManager.githubRepo = "user/repo"
        syncManager.githubGistId = "gist_abc"

        assertEquals("https://raw.githubusercontent.com/test/repo/main/catalog.json", syncManager.centralSyncUrl)
        assertTrue(syncManager.isAutoSyncEnabled)
        assertEquals("ghp_mock_token_123", syncManager.githubToken)
        assertEquals("user/repo", syncManager.githubRepo)
        assertEquals("gist_abc", syncManager.githubGistId)
    }
}
