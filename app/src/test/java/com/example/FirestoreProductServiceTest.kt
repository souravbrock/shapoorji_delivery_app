package com.example

import com.example.data.firestore.toFirestoreMap
import com.example.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FirestoreProductServiceTest {

    @Test
    fun testProductToFirestoreMapSerialization() {
        val product = Product(
            id = 42L,
            name = "Fresh Ridge Gourd | ঝিঙে | तोरी",
            category = "Vegetables",
            unit = "1 kg",
            price = 55.0,
            mrp = 65.0,
            stockQty = 30,
            description = "Crisp farm-fresh ridge gourd harvested daily.",
            imageUrl = "https://images.unsplash.com/photo-ridge-gourd",
            isDailyEssential = true,
            isAvailable = true,
            allowFractional = true,
            fractionStepGrams = 250,
            averageRating = 4.9f,
            reviewCount = 18,
            updatedAt = 1726000000000L
        )

        val map = product.toFirestoreMap()

        assertEquals(42L, map["id"])
        assertEquals("Fresh Ridge Gourd | ঝিঙে | तोरी", map["name"])
        assertEquals("Vegetables", map["category"])
        assertEquals("1 kg", map["unit"])
        assertEquals(55.0, map["price"])
        assertEquals(65.0, map["mrp"])
        assertEquals(30, map["stockQty"])
        assertEquals("Crisp farm-fresh ridge gourd harvested daily.", map["description"])
        assertEquals("https://images.unsplash.com/photo-ridge-gourd", map["imageUrl"])
        assertEquals(true, map["isDailyEssential"])
        assertEquals(true, map["isAvailable"])
        assertEquals(true, map["allowFractional"])
        assertEquals(250, map["fractionStepGrams"])
        assertEquals(4.9, map["averageRating"] as Double, 0.01)
        assertEquals(18, map["reviewCount"])
        assertEquals(1726000000000L, map["updatedAt"])
    }

    @Test
    fun testFirestoreFieldCompleteness() {
        val testProduct = Product(
            id = 101L,
            name = "Shimla Apple | আপেল | सेब",
            category = "Fruits",
            unit = "1 kg",
            price = 180.0,
            mrp = 210.0,
            stockQty = 25,
            description = "Crisp Himalayan Shimla apples",
            imageUrl = "https://images.unsplash.com/photo-apple",
            isDailyEssential = false,
            isAvailable = true
        )

        val map = testProduct.toFirestoreMap()
        val requiredKeys = listOf(
            "id", "name", "category", "unit", "price", "mrp", "stockQty",
            "description", "imageUrl", "isAvailable", "isDailyEssential",
            "allowFractional", "fractionStepGrams", "averageRating", "reviewCount", "updatedAt"
        )

        for (key in requiredKeys) {
            assertTrue("Map should contain key '$key'", map.containsKey(key))
            assertNotNull("Value for '$key' should not be null", map[key])
        }
    }
}
