package com.example.data.repository

import com.example.data.local.CartDao
import com.example.data.local.FavoriteDao
import com.example.data.local.NotificationLogDao
import com.example.data.local.OrderDao
import com.example.data.local.ProductDao
import com.example.data.local.ReviewDao
import com.example.data.model.CartItemEntity
import com.example.data.model.Favorite
import com.example.data.model.NotificationLog
import com.example.data.model.OfficialCatalog
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.notification.NotificationDispatcher
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.random.Random

class GroceryRepository(
    private val productDao: ProductDao,
    private val orderDao: OrderDao,
    private val reviewDao: ReviewDao,
    private val favoriteDao: FavoriteDao,
    private val notificationLogDao: NotificationLogDao,
    private val cartDao: CartDao,
    private val dispatcher: NotificationDispatcher
) {
    // Products
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    suspend fun syncOfficialCatalog() {
        productDao.insertAll(OfficialCatalog.INITIAL_PRODUCTS)
    }

    suspend fun resetToOfficialCatalog() {
        productDao.deleteAllProducts()
        productDao.insertAll(OfficialCatalog.INITIAL_PRODUCTS)
    }

    fun getProductById(id: Long): Flow<Product?> = productDao.getProductById(id)

    fun getProductsByCategory(category: String): Flow<List<Product>> =
        if (category == "All") productDao.getAllProducts() else productDao.getProductsByCategory(category)

    fun searchProducts(query: String): Flow<List<Product>> = productDao.searchProducts(query)

    suspend fun insertProduct(product: Product): Long = productDao.insertProduct(product)

    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)

    suspend fun updateStock(productId: Long, stockQty: Int) =
        productDao.updateStock(productId, stockQty)

    suspend fun updateProductDetails(
        productId: Long,
        name: String,
        price: Double,
        description: String,
        imageUrl: String,
        unit: String = "1 kg",
        category: String = "Vegetables",
        allowFractional: Boolean = false,
        fractionStepGrams: Int = 250
    ) = productDao.updateProductDetails(productId, name, price, description, imageUrl, unit, category, allowFractional, fractionStepGrams)

    suspend fun updateDailyPrice(productId: Long, newPrice: Double) =
        productDao.updateDailyPrice(productId, newPrice)

    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)

    suspend fun deleteProductById(id: Long) = productDao.deleteProductById(id)

    // Shopping Cart (Room-backed)
    val allCartEntities: Flow<List<CartItemEntity>> = cartDao.getAllCartItems()
    val cartCount: Flow<Int> = cartDao.getCartCount()

    suspend fun addToCart(productId: Long, quantity: Double = 1.0, portionLabel: String = "") {
        val existing = cartDao.getCartItemByProductId(productId)
        if (existing != null) {
            val updatedQty = existing.quantity + quantity
            val label = if (portionLabel.isNotBlank()) portionLabel else existing.portionLabel
            cartDao.updateQuantity(productId, updatedQty, label)
        } else {
            cartDao.insertCartItem(
                CartItemEntity(
                    productId = productId,
                    quantity = quantity,
                    portionLabel = portionLabel
                )
            )
        }
    }

    suspend fun setPortionInCart(productId: Long, fraction: Double, portionLabel: String) {
        val existing = cartDao.getCartItemByProductId(productId)
        if (existing != null) {
            cartDao.updateQuantity(productId, fraction, portionLabel)
        } else {
            cartDao.insertCartItem(
                CartItemEntity(
                    productId = productId,
                    quantity = fraction,
                    portionLabel = portionLabel
                )
            )
        }
    }

    suspend fun removeFromCart(productId: Long, fractionStep: Double = 1.0) {
        val existing = cartDao.getCartItemByProductId(productId)
        if (existing != null) {
            val newQty = existing.quantity - fractionStep
            if (newQty <= 0.001) {
                cartDao.deleteCartItem(productId)
            } else {
                val label = if (existing.portionLabel.isNotBlank() && (newQty != existing.quantity)) {
                    // Update label or keep formatted
                    formatPortionLabel(newQty)
                } else existing.portionLabel
                cartDao.updateQuantity(productId, newQty, label)
            }
        }
    }

    suspend fun deleteCartItem(productId: Long) {
        cartDao.deleteCartItem(productId)
    }

    suspend fun clearCart() {
        cartDao.clearCart()
    }

    private fun formatPortionLabel(qty: Double): String {
        return if (qty == qty.toLong().toDouble()) {
            "${qty.toInt()}"
        } else if (qty == 0.1) "100g"
        else if (qty == 0.2) "200g"
        else if (qty == 0.25) "250g"
        else if (qty == 0.5) "500g"
        else if (qty == 0.75) "750g"
        else "${(qty * 1000).toInt()}g"
    }

    // Orders
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()

    fun getOrdersForCustomer(email: String): Flow<List<Order>> =
        orderDao.getOrdersByCustomerEmail(email)

    fun getOrderById(id: Long): Flow<Order?> = orderDao.getOrderById(id)

    suspend fun placeOrder(
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        towerName: String,
        flatNumber: String,
        deliveryNotes: String,
        latitude: Double,
        longitude: Double,
        isLocationVerified: Boolean,
        items: List<OrderItem>,
        subtotal: Double,
        deliveryFee: Double,
        discount: Double,
        grandTotal: Double,
        paymentMethod: String
    ): Order {
        val randSuffix = Random.nextInt(1000, 9999)
        val orderNumber = "SPD-2026-$randSuffix"
        val invoiceNumber = "INV-SPD-${Random.nextInt(10000, 99999)}"

        val jsonArray = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("productId", item.productId)
                put("productName", item.productName)
                put("unit", item.unit)
                put("price", item.price)
                put("quantity", item.quantity)
                put("portionLabel", item.portionLabel)
            }
            jsonArray.put(obj)
        }

        val order = Order(
            orderNumber = orderNumber,
            invoiceNumber = invoiceNumber,
            customerName = customerName,
            customerEmail = customerEmail,
            customerPhone = customerPhone,
            towerName = towerName,
            flatNumber = flatNumber,
            deliveryNotes = deliveryNotes,
            latitude = latitude,
            longitude = longitude,
            isLocationVerifiedInsideShapoorji = isLocationVerified,
            itemsJson = jsonArray.toString(),
            itemCount = items.size,
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            discount = discount,
            grandTotal = grandTotal,
            paymentMethod = paymentMethod,
            status = OrderStatus.ORDER_RECEIVED
        )

        val insertedId = orderDao.insertOrder(order)
        val savedOrder = order.copy(id = insertedId)

        // Trigger notifications (Email to customer + souravbrock@gmail.com, and Telegram bot)
        dispatcher.dispatchOrderNotifications(savedOrder, isNewOrder = true)

        return savedOrder
    }

    suspend fun updateOrderStatus(order: Order, newStatus: OrderStatus) {
        orderDao.updateOrderStatus(order.id, newStatus)
        val updatedOrder = order.copy(status = newStatus)
        // Trigger notifications on status update
        dispatcher.dispatchOrderNotifications(updatedOrder, isNewOrder = false)
    }

    // Reviews & Ratings
    fun getReviewsForProduct(productId: Long): Flow<List<Review>> =
        reviewDao.getReviewsForProduct(productId)

    suspend fun addReview(
        productId: Long,
        customerName: String,
        customerEmail: String,
        rating: Int,
        comment: String
    ) {
        val review = Review(
            productId = productId,
            customerName = customerName,
            customerEmail = customerEmail,
            rating = rating,
            comment = comment
        )
        reviewDao.insertReview(review)

        // Recalculate average rating for product
        val avg = reviewDao.getAverageRating(productId) ?: rating.toFloat()
        val count = reviewDao.getReviewCount(productId)
        // Update product rating cache if present
        productDao.getProductById(productId)
    }

    // Favorites
    val favoriteProductIds: Flow<List<Long>> = favoriteDao.getAllFavoriteIds()

    suspend fun toggleFavorite(productId: Long, isCurrentlyFavorite: Boolean) {
        if (isCurrentlyFavorite) {
            favoriteDao.removeFavorite(productId)
        } else {
            favoriteDao.addFavorite(Favorite(productId))
        }
    }

    // Notification Logs
    val allNotificationLogs: Flow<List<NotificationLog>> = notificationLogDao.getAllLogs()

    fun getLogsForOrder(orderId: Long): Flow<List<NotificationLog>> =
        notificationLogDao.getLogsForOrder(orderId)

    val notificationDispatcher: NotificationDispatcher get() = dispatcher

    companion object {
        fun parseOrderItems(jsonString: String): List<OrderItem> {
            val list = mutableListOf<OrderItem>()
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        OrderItem(
                            productId = obj.optLong("productId", 0L),
                            productName = obj.optString("productName", ""),
                            unit = obj.optString("unit", ""),
                            price = obj.optDouble("price", 0.0),
                            quantity = obj.optDouble("quantity", 1.0),
                            portionLabel = obj.optString("portionLabel", "")
                        )
                    )
                }
            } catch (e: Exception) {
                // Return empty list on parse error
            }
            return list
        }
    }
}
