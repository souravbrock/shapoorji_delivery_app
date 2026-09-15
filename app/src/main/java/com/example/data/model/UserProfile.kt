package com.example.data.model

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val tower: String = "",
    val flatNumber: String = "",
    val isGoogleSignedIn: Boolean = false,
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
