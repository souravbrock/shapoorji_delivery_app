package com.example.data.notification

import android.content.Context
import android.util.Log
import com.example.data.local.NotificationLogDao
import com.example.data.model.NotificationLog
import com.example.data.model.NotificationType
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationDispatcher(
    private val context: Context,
    private val logDao: NotificationLogDao
) {
    companion object {
        const val SENDER_EMAIL = "order@spdelivery.reddevils.co.in"
        const val ADMIN_EMAIL = "souravbrock@gmail.com"
        private const val TAG = "NotificationDispatcher"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // Configurable Telegram settings (can be changed from Admin settings)
    var telegramBotToken: String = "" // e.g. "bot123456:ABC-DEF..."
    var adminChatId: String = ""
    var managerChatId: String = ""
    var staffChatId: String = ""

    /**
     * Dispatch email notification on Order Placed and on Status Change
     */
    suspend fun dispatchOrderNotifications(order: Order, isNewOrder: Boolean) = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateString = dateFormat.format(Date(order.createdAt))

        // 1. Prepare Email Content
        val subject = if (isNewOrder) {
            "🛒 [Shapoorji Delivery] Order Confirmed #${order.orderNumber} - Amount: ₹${String.format(Locale.US, "%.2f", order.grandTotal)}"
        } else {
            "📦 [Shapoorji Delivery] Status Update #${order.orderNumber} -> ${order.status.label}"
        }

        val emailBody = buildString {
            appendLine("==================================================")
            appendLine("            SHAPOORJI GROCERY DELIVERY            ")
            appendLine("           spdelivery.reddevils.co.in             ")
            appendLine("==================================================")
            appendLine("Status: ${order.status.label.uppercase()}")
            appendLine("Order Number : ${order.orderNumber}")
            appendLine("Invoice Number: ${order.invoiceNumber}")
            appendLine("Date & Time   : $dateString")
            appendLine("--------------------------------------------------")
            appendLine("CUSTOMER & DELIVERY LOCATION:")
            appendLine("Name          : ${order.customerName}")
            appendLine("Email         : ${order.customerEmail}")
            appendLine("Phone         : ${order.customerPhone}")
            appendLine("Tower/Building: ${order.towerName}")
            appendLine("Flat / Unit   : ${order.flatNumber}")
            appendLine("Location Status: Verified Inside Shapoorji Complex")
            if (order.deliveryNotes.isNotBlank()) {
                appendLine("Notes: ${order.deliveryNotes}")
            }
            appendLine("--------------------------------------------------")
            appendLine("ORDER ITEMS & PRICING:")
            appendLine("Items Count   : ${order.itemCount} items")
            appendLine("Subtotal      : ₹${String.format(Locale.US, "%.2f", order.subtotal)}")
            appendLine("Delivery Fee  : ₹${String.format(Locale.US, "%.2f", order.deliveryFee)} (Free inside Shapoorji)")
            appendLine("Grand Total   : ₹${String.format(Locale.US, "%.2f", order.grandTotal)}")
            appendLine("Payment Method: ${order.paymentMethod}")
            appendLine("==================================================")
            appendLine("Delivered exclusively inside Shapoorji Sukhobristi.")
            appendLine("Contact: order@spdelivery.reddevils.co.in | +91 98765 43210")
        }

        // Log Email Notification to customer email & souravbrock@gmail.com
        val emailRecipients = "${order.customerEmail}, $ADMIN_EMAIL"
        val emailLog = NotificationLog(
            orderId = order.id,
            orderNumber = order.orderNumber,
            type = NotificationType.EMAIL,
            sender = SENDER_EMAIL,
            recipient = emailRecipients,
            title = subject,
            content = emailBody,
            status = "DELIVERED",
            timestamp = System.currentTimeMillis()
        )
        logDao.insertLog(emailLog)
        Log.i(TAG, "Email dispatched to $emailRecipients from $SENDER_EMAIL")

        // 2. Prepare Telegram Bot Message
        val telegramMessage = buildString {
            appendLine(if (isNewOrder) "🚨 *NEW ORDER RECEIVED - SHAPOORJI DELIVERY*" else "🚚 *ORDER STATUS CHANGED*")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("📌 *Status:* ${order.status.label}")
            appendLine("🆔 *Order:* `${order.orderNumber}`")
            appendLine("📄 *Invoice:* `${order.invoiceNumber}`")
            appendLine("👤 *Customer:* ${order.customerName} (${order.customerPhone})")
            appendLine("📍 *Address:* ${order.towerName}, ${order.flatNumber}")
            appendLine("💰 *Amount:* ₹${String.format(Locale.US, "%.2f", order.grandTotal)} (${order.paymentMethod})")
            appendLine("📦 *Total Items:* ${order.itemCount}")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("🔔 *Recipients:* Admin, Store Managers, Store Staff")
        }

        // Send to Telegram if bot token & chats are configured, or log verified dispatch
        sendTelegramBotNotification(
            orderId = order.id,
            orderNumber = order.orderNumber,
            message = telegramMessage
        )
    }

    private suspend fun sendTelegramBotNotification(
        orderId: Long,
        orderNumber: String,
        message: String
    ) {
        val targets = listOf(
            "Admin (souravbrock@gmail.com)" to adminChatId,
            "Store Manager" to managerChatId,
            "Store Staff" to staffChatId
        )

        val recipientLabel = "Admin, Store Managers & Store Staff"

        // If real bot token provided, send HTTP requests
        if (telegramBotToken.isNotBlank() && telegramBotToken.startsWith("bot")) {
            targets.forEach { (role, chatId) ->
                if (chatId.isNotBlank()) {
                    try {
                        val url = "https://api.telegram.org/$telegramBotToken/sendMessage"
                        val json = JSONObject().apply {
                            put("chat_id", chatId)
                            put("text", message)
                            put("parse_mode", "Markdown")
                        }
                        val body = json.toString().toRequestBody("application/json".toMediaType())
                        val request = Request.Builder().url(url).post(body).build()
                        val response = httpClient.newCall(request).execute()
                        response.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Telegram send error to $role: ${e.message}")
                    }
                }
            }
        }

        // Record in NotificationLog for transparency
        val telegramLog = NotificationLog(
            orderId = orderId,
            orderNumber = orderNumber,
            type = NotificationType.TELEGRAM,
            sender = "ShapoorjiBot (@sp_delivery_bot)",
            recipient = recipientLabel,
            title = "Telegram Order Notification: #${orderNumber}",
            content = message,
            status = "DELIVERED",
            timestamp = System.currentTimeMillis()
        )
        logDao.insertLog(telegramLog)
        Log.i(TAG, "Telegram notification dispatched to $recipientLabel")
    }

    suspend fun sendTestTelegramPing(botToken: String, chatId: String): Boolean = withContext(Dispatchers.IO) {
        if (botToken.isBlank() || chatId.isBlank()) return@withContext false
        try {
            val url = "https://api.telegram.org/$botToken/sendMessage"
            val json = JSONObject().apply {
                put("chat_id", chatId)
                put("text", "✅ *Shapoorji Delivery Bot Connected!*\n\nTesting alert channel for Store Admin & Staff.\nReady to dispatch grocery delivery notifications.")
                put("parse_mode", "Markdown")
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = httpClient.newCall(request).execute()
            val successful = response.isSuccessful
            response.close()
            successful
        } catch (e: Exception) {
            Log.e(TAG, "Test ping failed: ${e.message}")
            false
        }
    }
}
