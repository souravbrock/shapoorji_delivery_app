package com.example.data.model

data class UserProfile(
    val name: String = "Sourav Brock",
    val email: String = "souravbrock@gmail.com",
    val phone: String = "+91 98765 43210",
    val tower: String = "Sukhobristi Phase 1 - Tower A4",
    val flatNumber: String = "Flat 803, 8th Floor",
    val isGoogleSignedIn: Boolean = true,
    val photoUrl: String = ""
)

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val totalPrice: Double get() = product.price * quantity
}
