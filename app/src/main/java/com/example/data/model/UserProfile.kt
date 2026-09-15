package com.example.data.model

data class UserProfile(
    val name: String = "Sourav Brock",
    val email: String = "souravbrock@gmail.com",
    val phone: String = "+91-8442980101",
    val tower: String = "Sukhobristi Phase 1 - Tower A4",
    val flatNumber: String = "Flat 803, 8th Floor",
    val isGoogleSignedIn: Boolean = true,
    val photoUrl: String = ""
) {
    val isAdmin: Boolean get() = email.trim().equals("souravbrock@gmail.com", ignoreCase = true)
}

data class CartItem(
    val product: Product,
    val quantity: Double = 1.0,
    val portionLabel: String = ""
) {
    // Convenience constructor for Int quantity
    constructor(product: Product, quantity: Int) : this(product, quantity.toDouble(), "")

    val totalPrice: Double get() = product.price * quantity

    val displayQuantity: String get() {
        if (portionLabel.isNotBlank()) return portionLabel
        return if (quantity == quantity.toLong().toDouble()) {
            "${quantity.toInt()}"
        } else if (quantity == 0.1) {
            "100g"
        } else if (quantity == 0.2) {
            "200g"
        } else if (quantity == 0.25) {
            "250g"
        } else if (quantity == 0.5) {
            "500g"
        } else if (quantity == 0.75) {
            "750g"
        } else {
            String.format(java.util.Locale.US, "%.2f", quantity)
        }
    }
}
