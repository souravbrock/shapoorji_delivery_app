package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String, // e.g., "Vegetables", "Fruits", "Dairy & Breakfast", "Staples & Atta", "Snacks & Drinks", "Personal Care"
    val unit: String, // e.g., "1 kg", "500 g", "1 Packet", "1 Litre", "1 bunch", "1 pc"
    val price: Double, // Current daily price in INR (₹)
    val mrp: Double, // Market Retail Price
    val stockQty: Int = 50,
    val description: String = "",
    val imageUrl: String = "",
    val isAvailable: Boolean = true,
    val isDailyEssential: Boolean = false,
    val allowFractional: Boolean = false, // Admin option: enable fractional purchase in grams / liters
    val fractionStepGrams: Int = 250, // 100 for 100g steps (Garlic, Ginger, Chillies), 250 for 250g (Tomato, Potato, etc.)
    val averageRating: Float = 4.8f,
    val reviewCount: Int = 12,
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Checks if the unit can be divided into grams or liters.
     * Per-piece items ("1 pc", "1 piece", "1 bunch", "1 packet") cannot be split into fractions.
     */
    fun isUnitDivisible(): Boolean {
        val lower = unit.lowercase()
        return lower.contains("kg") || lower.contains("gm") || lower.contains("gram") ||
                lower.contains("liter") || lower.contains("litre") || lower.contains("ltr") || lower.contains("ml")
    }

    /**
     * Available portions if fractional purchase is enabled by the admin.
     */
    fun getAvailablePortions(): List<PortionOption> {
        if (!allowFractional || !isUnitDivisible()) {
            return emptyList()
        }
        val isLitre = unit.lowercase().contains("l")
        return if (fractionStepGrams <= 100) {
            listOf(
                PortionOption(fraction = 0.1, label = if (isLitre) "100 ml" else "100g", price = (price * 0.1)),
                PortionOption(fraction = 0.25, label = if (isLitre) "250 ml" else "250g", price = (price * 0.25)),
                PortionOption(fraction = 0.5, label = if (isLitre) "500 ml" else "500g", price = (price * 0.5)),
                PortionOption(fraction = 1.0, label = if (isLitre) "1 Litre" else "1 kg", price = price)
            )
        } else {
            listOf(
                PortionOption(fraction = 0.25, label = if (isLitre) "250 ml" else "250g", price = (price * 0.25)),
                PortionOption(fraction = 0.5, label = if (isLitre) "500 ml" else "500g", price = (price * 0.5)),
                PortionOption(fraction = 0.75, label = if (isLitre) "750 ml" else "750g", price = (price * 0.75)),
                PortionOption(fraction = 1.0, label = if (isLitre) "1 Litre" else "1 kg", price = price),
                PortionOption(fraction = 2.0, label = if (isLitre) "2 Litres" else "2 kg", price = (price * 2.0))
            )
        }
    }

    fun getStep(): Double {
        if (!allowFractional || !isUnitDivisible()) return 1.0
        return (fractionStepGrams / 1000.0).coerceAtLeast(0.1)
    }

    fun formatQuantity(qty: Double): String {
        if (!allowFractional || !isUnitDivisible()) {
            val count = qty.toInt()
            return if (count <= 1) unit else "$count × $unit"
        }
        val isLitre = unit.lowercase().contains("l")
        val grams = kotlin.math.round(qty * 1000).toInt()
        return when {
            isLitre && grams < 1000 -> "$grams ml"
            isLitre && grams % 1000 == 0 -> "${grams / 1000} L"
            isLitre -> String.format(java.util.Locale.US, "%.2f L", qty)
            grams < 1000 -> "${grams}g"
            grams % 1000 == 0 -> "${grams / 1000} kg"
            else -> String.format(java.util.Locale.US, "%.2f kg", qty)
        }
    }
}

data class PortionOption(
    val fraction: Double,
    val label: String,
    val price: Double
)
