package com.example.data.repository

import com.example.data.local.FavoriteDao
import com.example.data.local.NotificationLogDao
import com.example.data.local.OrderDao
import com.example.data.local.ProductDao
import com.example.data.local.ReviewDao
import com.example.data.model.Favorite
import com.example.data.model.NotificationLog
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
    private val dispatcher: NotificationDispatcher
) {
    // Products
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    fun getProductById(id: Long): Flow<Product?> = productDao.getProductById(id)

    fun getProductsByCategory(category: String): Flow<List<Product>> =
        if (category == "All") productDao.getAllProducts() else productDao.getProductsByCategory(category)

    fun searchProducts(query: String): Flow<List<Product>> = productDao.searchProducts(query)

    suspend fun insertProduct(product: Product): Long = productDao.insertProduct(product)

    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)

    suspend fun updateDailyPrice(productId: Long, newPrice: Double) =
        productDao.updateDailyPrice(productId, newPrice)

    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)

    suspend fun deleteProductById(id: Long) = productDao.deleteProductById(id)

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
            itemCount = items.sumOf { it.quantity },
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
                            quantity = obj.optInt("quantity", 1)
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
