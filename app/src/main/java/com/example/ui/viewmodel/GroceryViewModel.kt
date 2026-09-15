package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.model.ShapoorjiGeo
import com.example.data.model.UserProfile
import com.example.data.notification.NotificationDispatcher
import com.example.data.repository.GroceryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GroceryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val dispatcher = NotificationDispatcher(application, database.notificationLogDao())
    val repository = GroceryRepository(
        productDao = database.productDao(),
        orderDao = database.orderDao(),
        reviewDao = database.reviewDao(),
        favoriteDao = database.favoriteDao(),
        notificationLogDao = database.notificationLogDao(),
        dispatcher = dispatcher
    )

    // User Profile / Google Sign-in
    private val _currentUser = MutableStateFlow(
        UserProfile(
            name = "Sourav Brock",
            email = "souravbrock@gmail.com",
            phone = "+91 98765 43210",
            tower = "Sukhobristi Phase 1 - Tower A4",
            flatNumber = "Flat 803, 8th Floor",
            isGoogleSignedIn = true
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    // Mode: Customer vs Admin
    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    // Search and Category
    val selectedCategory = MutableStateFlow("All")
    val searchQuery = MutableStateFlow("")

    // Raw Products
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Products
    val filteredProducts: StateFlow<List<Product>> = combine(
        allProducts,
        selectedCategory,
        searchQuery
    ) { products, category, query ->
        products.filter { product ->
            val matchesCategory = (category == "All" || product.category.equals(category, ignoreCase = true))
            val matchesQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.description.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart items: productId -> quantity
    private val _cartMap = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val cartMap: StateFlow<Map<Long, Int>> = _cartMap.asStateFlow()

    val cartItems: StateFlow<List<CartItem>> = combine(allProducts, _cartMap) { products, cart ->
        val productMap = products.associateBy { it.id }
        cart.mapNotNull { (productId, qty) ->
            productMap[productId]?.let { CartItem(it, qty) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartSubtotal: StateFlow<Double> = cartItems.combine(_cartMap) { items, _ ->
        items.sumOf { it.totalPrice }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val deliveryFee: StateFlow<Double> = cartSubtotal.combine(_currentUser) { subtotal, _ ->
        // Free delivery inside Shapoorji on orders above ₹199, else nominal ₹20
        if (subtotal >= 199.0 || subtotal == 0.0) 0.0 else 20.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartTotal: StateFlow<Double> = combine(cartSubtotal, deliveryFee) { sub, fee ->
        if (sub > 0) sub + fee else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Favorites
    val favoriteIds: StateFlow<List<Long>> = repository.favoriteProductIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Orders
    val allOrders: StateFlow<List<Order>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customer's Orders
    val customerOrders: StateFlow<List<Order>> = combine(allOrders, _currentUser) { orders, user ->
        orders.filter { it.customerEmail.equals(user.email, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notification Logs
    val notificationLogs = repository.allNotificationLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Shapoorji Location Verification State
    val selectedTower = MutableStateFlow("Sukhobristi Phase 1 - Tower A4")
    val flatInput = MutableStateFlow("Flat 803, 8th Floor")
    val deliveryNotesInput = MutableStateFlow("Please leave at door / ring bell")
    val currentLatitude = MutableStateFlow(22.5695)
    val currentLongitude = MutableStateFlow(88.5195)
    val isLocationInsideShapoorji = MutableStateFlow(true)
    val locationVerificationMessage = MutableStateFlow("✓ Verified: Inside Shapoorji Sukhobristi Delivery Zone")

    // UI Feedback
    val toastMessage = MutableStateFlow<String?>(null)

    fun toggleAdminMode() {
        _isAdminMode.value = !_isAdminMode.value
    }

    fun setAdminMode(enabled: Boolean) {
        _isAdminMode.value = enabled
    }

    // Google Sign-In Simulation
    fun signInWithGoogle(email: String, name: String) {
        _currentUser.value = _currentUser.value.copy(
            name = name.ifBlank { "Sourav Brock" },
            email = email.ifBlank { "souravbrock@gmail.com" },
            isGoogleSignedIn = true
        )
        toastMessage.value = "Signed in as ${_currentUser.value.email}"
    }

    fun signOut() {
        _currentUser.value = _currentUser.value.copy(isGoogleSignedIn = false)
        toastMessage.value = "Signed out of Google account"
    }

    // Cart operations
    fun addToCart(productId: Long) {
        val current = _cartMap.value.toMutableMap()
        val count = current.getOrDefault(productId, 0)
        current[productId] = count + 1
        _cartMap.value = current
    }

    fun removeFromCart(productId: Long) {
        val current = _cartMap.value.toMutableMap()
        val count = current.getOrDefault(productId, 0)
        if (count <= 1) {
            current.remove(productId)
        } else {
            current[productId] = count - 1
        }
        _cartMap.value = current
    }

    fun clearCart() {
        _cartMap.value = emptyMap()
    }

    // Favorites
    fun toggleFavorite(productId: Long) {
        viewModelScope.launch {
            val isFav = favoriteIds.value.contains(productId)
            repository.toggleFavorite(productId, isFav)
        }
    }

    // Location Verification for Shapoorji
    fun updateCoordinates(lat: Double, lng: Double) {
        currentLatitude.value = lat
        currentLongitude.value = lng
        val inside = ShapoorjiGeo.isInsideShapoorji(lat, lng)
        isLocationInsideShapoorji.value = inside
        if (inside) {
            val dist = ShapoorjiGeo.distanceInMeters(lat, lng, ShapoorjiGeo.CENTER_LATITUDE, ShapoorjiGeo.CENTER_LONGITUDE).toInt()
            locationVerificationMessage.value = "✓ Verified Inside Shapoorji (Offset: ${dist}m from Sukhobristi center)"
        } else {
            locationVerificationMessage.value = "❌ Outside Delivery Zone! Shapoorji Delivery only serves within Shapoorji Sukhobristi."
        }
    }

    fun setLocationToPreset(isInside: Boolean) {
        if (isInside) {
            updateCoordinates(22.5695, 88.5195)
        } else {
            // Outside Shapoorji: e.g. Sector V / Salt Lake (22.5868, 88.4355)
            updateCoordinates(22.5868, 88.4355)
        }
    }

    // Order Placement
    fun placeOrder(onSuccess: (Order) -> Unit, onError: (String) -> Unit) {
        if (!isLocationInsideShapoorji.value) {
            onError("Orders cannot be placed outside Shapoorji! Please verify your location inside Shapoorji Sukhobristi.")
            return
        }

        val items = cartItems.value
        if (items.isEmpty()) {
            onError("Your grocery cart is empty!")
            return
        }

        viewModelScope.launch {
            try {
                val orderItems = items.map {
                    OrderItem(
                        productId = it.product.id,
                        productName = it.product.name,
                        unit = it.product.unit,
                        price = it.product.price,
                        quantity = it.quantity
                    )
                }

                val order = repository.placeOrder(
                    customerName = _currentUser.value.name,
                    customerEmail = _currentUser.value.email,
                    customerPhone = _currentUser.value.phone,
                    towerName = selectedTower.value,
                    flatNumber = flatInput.value,
                    deliveryNotes = deliveryNotesInput.value,
                    latitude = currentLatitude.value,
                    longitude = currentLongitude.value,
                    isLocationVerified = isLocationInsideShapoorji.value,
                    items = orderItems,
                    subtotal = cartSubtotal.value,
                    deliveryFee = deliveryFee.value,
                    discount = 0.0,
                    grandTotal = cartTotal.value,
                    paymentMethod = "Pay on Delivery (Cash / UPI QR)"
                )

                clearCart()
                toastMessage.value = "Order #${order.orderNumber} placed! Email sent from order@spdelivery.reddevils.co.in"
                onSuccess(order)
            } catch (e: Exception) {
                onError("Failed to place order: ${e.message}")
            }
        }
    }

    // Admin: Advance order status
    fun advanceOrderStatus(order: Order) {
        val next = order.status.nextStatus() ?: return
        viewModelScope.launch {
            repository.updateOrderStatus(order, next)
            toastMessage.value = "Order #${order.orderNumber} updated to ${next.label}! Email & Telegram alerts sent."
        }
    }

    // Admin: Update Daily Price
    fun updateProductDailyPrice(productId: Long, newPrice: Double) {
        viewModelScope.launch {
            repository.updateDailyPrice(productId, newPrice)
            toastMessage.value = "Updated daily price to ₹$newPrice"
        }
    }

    // Admin: Add Product
    fun addProduct(
        name: String,
        category: String,
        unit: String,
        price: Double,
        mrp: Double,
        stockQty: Int,
        description: String,
        imageUrl: String,
        isDailyEssential: Boolean
    ) {
        viewModelScope.launch {
            val product = Product(
                name = name,
                category = category,
                unit = unit,
                price = price,
                mrp = mrp,
                stockQty = stockQty,
                description = description,
                imageUrl = imageUrl.ifBlank { "https://images.unsplash.com/photo-1542838132-92c53300491e?w=400" },
                isDailyEssential = isDailyEssential
            )
            repository.insertProduct(product)
            toastMessage.value = "Added product: $name"
        }
    }

    // Admin: Delete Product
    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            toastMessage.value = "Deleted product: ${product.name}"
        }
    }

    // Customer: Submit Review & Rating
    fun submitReview(productId: Long, rating: Int, comment: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.addReview(
                productId = productId,
                customerName = _currentUser.value.name,
                customerEmail = _currentUser.value.email,
                rating = rating,
                comment = comment
            )
            toastMessage.value = "Thank you for rating this product!"
            onSuccess()
        }
    }

    // Telegram Bot Settings
    fun updateTelegramSettings(token: String, adminChat: String, managerChat: String, staffChat: String) {
        dispatcher.telegramBotToken = token
        dispatcher.adminChatId = adminChat
        dispatcher.managerChatId = managerChat
        dispatcher.staffChatId = staffChat
        toastMessage.value = "Telegram Bot alerts configured"
    }

    fun sendTestTelegramAlert(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = dispatcher.sendTestTelegramPing(
                dispatcher.telegramBotToken,
                dispatcher.adminChatId
            )
            onResult(success)
        }
    }
}
