package com.example.data.notification

import android.content.Context
import android.util.Log
import com.example.data.local.NotificationLogDao
import com.example.data.model.NotificationLog
import com.example.data.model.NotificationType
import com.example.data.model.Order
import com.example.data.model.OrderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class TelegramDispatchReport(
    val totalCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val details: List<String>
)

class NotificationDispatcher(
    private val context: Context,
    private val logDao: NotificationLogDao
) {
    companion object {
        const val DEFAULT_SENDER_EMAIL = "order@spdelivery.reddevils.co.in"
        const val DEFAULT_ADMIN_EMAIL = "souravbrock@gmail.com"
        const val DEFAULT_TELEGRAM_BOT_TOKEN = "8906839330:AAEOlqOSvXVVrV5A6N0yIdnUuSPzInMjAJ0"
        const val DEFAULT_ADMIN_CHAT_IDS = "167694312, 7127777789"
        const val DEFAULT_MANAGER_CHAT_IDS = "8924193494, 9083900751"
        const val DEFAULT_STAFF_CHAT_IDS = "58088380"
        private const val TAG = "NotificationDispatcher"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Active Telegram settings
    var telegramBotToken: String = DEFAULT_TELEGRAM_BOT_TOKEN
    var adminChatIds: String = DEFAULT_ADMIN_CHAT_IDS
    var managerChatIds: String = DEFAULT_MANAGER_CHAT_IDS
    var staffChatIds: String = DEFAULT_STAFF_CHAT_IDS

    // Active SMTP Email settings
    var smtpHost: String = "spdelivery.reddevils.co.in"
    var smtpPort: Int = 465
    var smtpUsername: String = DEFAULT_SENDER_EMAIL
    var smtpPassword: String = "K?@f{Ntv2lX]APW0"
    var senderEmail: String = DEFAULT_SENDER_EMAIL
    var adminEmail: String = DEFAULT_ADMIN_EMAIL

    private fun parseChatIds(raw: String): List<String> {
        return raw.split(",", ";", "\n", " ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun getAllTargetChatIds(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        parseChatIds(adminChatIds).forEach { list.add("Store Admin" to it) }
        parseChatIds(managerChatIds).forEach { list.add("Store Manager" to it) }
        parseChatIds(staffChatIds).forEach { list.add("Store Staff" to it) }
        return list.distinctBy { it.second }
    }

    private fun formatQuantity(quantity: Double): String {
        return if (quantity % 1.0 == 0.0) {
            quantity.toLong().toString()
        } else {
            val formatted = String.format(Locale.US, "%.2f", quantity)
            if (formatted.endsWith("0")) formatted.trimEnd('0').trimEnd('.') else formatted
        }
    }

    private fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", amount)
        }
    }

    private fun formatTelegramAddress(towerName: String, flatNumber: String): String {
        val cleanTower = towerName.trim()
        val cleanFlat = flatNumber.trim()

        val base = when {
            cleanFlat.isNotBlank() && cleanTower.isNotBlank() -> {
                if (cleanTower.contains(cleanFlat, ignoreCase = true)) {
                    cleanTower
                } else {
                    "$cleanFlat, $cleanTower"
                }
            }
            cleanTower.isNotBlank() -> cleanTower
            cleanFlat.isNotBlank() -> cleanFlat
            else -> "Shapoorji Shukhobrishti"
        }

        return if (!base.contains("Shapoorji", ignoreCase = true) && !base.contains("Shukhobrishti", ignoreCase = true) && !base.contains("Sukhobristi", ignoreCase = true)) {
            "$base Shapoorji"
        } else {
            base
        }
    }

    private fun parseItems(jsonString: String): List<OrderItem> {
        val list = mutableListOf<OrderItem>()
        if (jsonString.isBlank()) return list
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
        } catch (_: Exception) {}
        return list
    }

    /**
     * Dispatch email notification on Order Placed and on Status Change
     */
    suspend fun dispatchOrderNotifications(order: Order, isNewOrder: Boolean) = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.ENGLISH)
        val rawDate = dateFormat.format(Date(order.createdAt))
        val dateString = rawDate
            .replace("Sep ", "Sept ")
            .replace("AM", "am")
            .replace("PM", "pm")

        val orderItems = parseItems(order.itemsJson)
        val telegramAddress = formatTelegramAddress(order.towerName, order.flatNumber)

        // 1. Prepare Email Content matching requested format
        val subject = if (isNewOrder) {
            "🛒 [Shapoorji Delivery] Order Confirmed #${order.orderNumber} - Amount: ₹${String.format(Locale.US, "%.2f", order.grandTotal)}"
        } else {
            "📦 [Shapoorji Delivery] Status Update #${order.orderNumber} -> ${order.status.label}"
        }

        val emailBody = buildString {
            appendLine("==================================================")
            appendLine("            SHAPOORJI GROCERY DELIVERY           ")
            appendLine("           spdelivery.reddevils.co.in             ")
            appendLine("==================================================")
            appendLine("Status: ${order.status.label.uppercase()}")
            appendLine("Order Number  : ${order.orderNumber}")
            appendLine("Invoice Number: ${order.invoiceNumber}")
            appendLine("Date & Time   : $dateString")
            appendLine("--------------------------------------------------")
            appendLine("CUSTOMER & DELIVERY LOCATION:")
            appendLine("Name          : ${order.customerName}")
            appendLine("Email         : ${order.customerEmail}")
            appendLine("Phone         : ${order.customerPhone}")
            appendLine("Tower/Building: ${order.towerName}")
            appendLine("Flat / Unit   : ${order.flatNumber}")
            appendLine("Location Status: Verified Inside Shapoorji Shukhobrishti")
            if (order.deliveryNotes.isNotBlank()) {
                appendLine("Notes         : ${order.deliveryNotes}")
            }
            appendLine("--------------------------------------------------")
            appendLine("ORDER ITEMS & PRICING:")
            appendLine("Items Count   : ${order.itemCount} ${if (order.itemCount == 1) "item" else "items"}")
            appendLine("Subtotal      : ₹${String.format(Locale.US, "%.2f", order.subtotal)}")
            appendLine("Delivery Fee  : ₹${String.format(Locale.US, "%.2f", order.deliveryFee)} (Free inside Shapoorji)")
            appendLine("Grand Total   : ₹${String.format(Locale.US, "%.2f", order.grandTotal)}")
            appendLine("Payment Method: ${order.paymentMethod}")
            appendLine("--------------------------------------------------")
            appendLine("🛍️ Items Billed:")
            orderItems.forEach { item ->
                val qtyStr = formatQuantity(item.quantity)
                val priceStr = formatAmount(item.total)
                appendLine("- ${item.productName} (x$qtyStr) : ₹$priceStr")
            }
            appendLine("==================================================")
            appendLine("Delivered exclusively inside Shapoorji Shukhobrishti, Action Area III, Kolkata.")
            append("Store Contact: order@spdelivery.reddevils.co.in | +91-8442980101")
        }

        // 2. Real SMTP Email Dispatch
        val emailRecipients = listOfNotNull(
            order.customerEmail.takeIf { it.isNotBlank() },
            adminEmail.takeIf { it.isNotBlank() },
            smtpUsername.takeIf { it.isNotBlank() }
        ).distinct()

        val smtpClient = SmtpClient(
            host = smtpHost,
            port = smtpPort,
            username = smtpUsername,
            password = smtpPassword
        )

        val emailResult = smtpClient.sendEmail(
            from = senderEmail,
            recipients = emailRecipients,
            subject = subject,
            bodyText = emailBody
        )

        // Log Email in Room Database
        val emailLog = NotificationLog(
            orderId = order.id,
            orderNumber = order.orderNumber,
            type = NotificationType.EMAIL,
            sender = senderEmail,
            recipient = emailRecipients.joinToString(", "),
            title = subject,
            content = emailBody,
            status = if (emailResult.success) "DELIVERED" else "FAILED: ${emailResult.message}",
            timestamp = System.currentTimeMillis()
        )
        logDao.insertLog(emailLog)
        Log.i(TAG, "Email dispatch result: ${emailResult.success} - ${emailResult.message}")

        // 3. Prepare Role-specific Telegram Bot Messages
        val statusPrefix = if (!isNewOrder) "📦 Status: ${order.status.label.uppercase()}\n" else ""

        val adminTelegramMessage = buildString {
            if (statusPrefix.isNotEmpty()) append(statusPrefix)
            appendLine("👤 Name: ${order.customerName}")
            appendLine("📞 Phone: ${order.customerPhone}")
            appendLine("🏠 Address: $telegramAddress")
            appendLine("📧 Email: ${order.customerEmail}")
            appendLine()
            appendLine("🛍️ Items Billed:")
            orderItems.forEach { item ->
                val qtyStr = formatQuantity(item.quantity)
                val priceStr = formatAmount(item.total)
                appendLine("- ${item.productName} (x$qtyStr) : ₹$priceStr")
            }
            appendLine()
            append("💰 Total: ₹${formatAmount(order.grandTotal)}")
        }

        val managerTelegramMessage = buildString {
            if (statusPrefix.isNotEmpty()) append(statusPrefix)
            appendLine("👤 Name: ${order.customerName}")
            appendLine("🏠 Address: $telegramAddress")
            appendLine()
            appendLine()
            appendLine("🛍️ Items Billed:")
            orderItems.forEach { item ->
                val qtyStr = formatQuantity(item.quantity)
                val priceStr = formatAmount(item.total)
                appendLine("- ${item.productName} (x$qtyStr) : ₹$priceStr")
            }
            appendLine()
            append("💰 Total: ₹${formatAmount(order.grandTotal)}")
        }

        val staffTelegramMessage = buildString {
            if (statusPrefix.isNotEmpty()) append(statusPrefix)
            appendLine("👤 Name: ${order.customerName}")
            appendLine("🏠 Address: $telegramAddress")
            append("💰 Total: ₹${formatAmount(order.grandTotal)}")
        }

        // 4. Real Role-Based Telegram Dispatch
        val report = sendRoleBasedTelegramBroadcast(
            adminMessage = adminTelegramMessage,
            managerMessage = managerTelegramMessage,
            staffMessage = staffTelegramMessage
        )

        val telegramLog = NotificationLog(
            orderId = order.id,
            orderNumber = order.orderNumber,
            type = NotificationType.TELEGRAM,
            sender = "Shapoorji Orders Bot (@ShapoorjiOrdersBot)",
            recipient = "Admin (${parseChatIds(adminChatIds).size}), Managers (${parseChatIds(managerChatIds).size}), Staff (${parseChatIds(staffChatIds).size})",
            title = "Telegram Order Alert #${order.orderNumber} (${order.status.label})",
            content = "Role-based dispatch: ${report.successCount}/${report.totalCount} delivered.\n${report.details.joinToString("\n")}\n\n[Admin Message Preview]\n$adminTelegramMessage",
            status = if (report.successCount > 0) "DELIVERED (${report.successCount}/${report.totalCount})" else "FAILED",
            timestamp = System.currentTimeMillis()
        )
        logDao.insertLog(telegramLog)
        Log.i(TAG, "Telegram broadcast finished: ${report.successCount}/${report.totalCount} sent")
    }

    suspend fun sendRoleBasedTelegramBroadcast(
        adminMessage: String,
        managerMessage: String,
        staffMessage: String
    ): TelegramDispatchReport = withContext(Dispatchers.IO) {
        if (telegramBotToken.isBlank()) {
            return@withContext TelegramDispatchReport(0, 0, 0, listOf("No bot token configured"))
        }

        val cleanToken = telegramBotToken.trim().removePrefix("bot")
        val url = "https://api.telegram.org/bot$cleanToken/sendMessage"

        var successCount = 0
        var failCount = 0
        val details = mutableListOf<String>()

        val adminIds = parseChatIds(adminChatIds)
        val managerIds = parseChatIds(managerChatIds).filterNot { adminIds.contains(it) }
        val staffIds = parseChatIds(staffChatIds).filterNot { adminIds.contains(it) || managerIds.contains(it) }

        val dispatchList = mutableListOf<Triple<String, String, String>>()
        adminIds.forEach { dispatchList.add(Triple("Store Admin", it, adminMessage)) }
        managerIds.forEach { dispatchList.add(Triple("Store Manager", it, managerMessage)) }
        staffIds.forEach { dispatchList.add(Triple("Store Staff", it, staffMessage)) }

        if (dispatchList.isEmpty()) {
            return@withContext TelegramDispatchReport(0, 0, 0, listOf("No target chat IDs configured"))
        }

        dispatchList.forEach { (role, chatId, messageText) ->
            try {
                val json = JSONObject().apply {
                    put("chat_id", chatId)
                    put("text", messageText)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).post(body).build()
                val response = httpClient.newCall(request).execute()
                val code = response.code
                val respStr = response.body?.string() ?: ""
                response.close()

                if (code == 200) {
                    successCount++
                    details.add("✓ $role ($chatId): Delivered")
                } else {
                    failCount++
                    val desc = try {
                        JSONObject(respStr).optString("description", "Error $code")
                    } catch (_: Exception) {
                        "Error $code"
                    }
                    if (desc.contains("chat not found", ignoreCase = true)) {
                        details.add("✗ $role ($chatId): Chat not found (User must tap /start on @ShapoorjiOrdersBot)")
                    } else {
                        details.add("✗ $role ($chatId): $desc")
                    }
                }
            } catch (e: Exception) {
                failCount++
                details.add("✗ $role ($chatId): ${e.message}")
            }
        }

        TelegramDispatchReport(
            totalCount = dispatchList.size,
            successCount = successCount,
            failureCount = failCount,
            details = details
        )
    }

    suspend fun sendTelegramBroadcast(messageHtml: String): TelegramDispatchReport = withContext(Dispatchers.IO) {
        val targets = getAllTargetChatIds()
        if (targets.isEmpty() || telegramBotToken.isBlank()) {
            return@withContext TelegramDispatchReport(0, 0, 0, listOf("No chat IDs or bot token configured"))
        }

        val cleanToken = telegramBotToken.trim().removePrefix("bot")
        val url = "https://api.telegram.org/bot$cleanToken/sendMessage"

        var successCount = 0
        var failCount = 0
        val details = mutableListOf<String>()

        targets.forEach { (role, chatId) ->
            try {
                val json = JSONObject().apply {
                    put("chat_id", chatId)
                    put("text", messageHtml)
                    if (messageHtml.contains("<b>") || messageHtml.contains("<i>") || messageHtml.contains("<code>")) {
                        put("parse_mode", "HTML")
                    }
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).post(body).build()
                val response = httpClient.newCall(request).execute()
                val code = response.code
                val respStr = response.body?.string() ?: ""
                response.close()

                if (code == 200) {
                    successCount++
                    details.add("✓ $role ($chatId): Delivered")
                } else {
                    failCount++
                    val desc = try {
                        JSONObject(respStr).optString("description", "Error $code")
                    } catch (_: Exception) {
                        "Error $code"
                    }
                    if (desc.contains("chat not found", ignoreCase = true)) {
                        details.add("✗ $role ($chatId): Chat not found (User must tap /start on @ShapoorjiOrdersBot)")
                    } else {
                        details.add("✗ $role ($chatId): $desc")
                    }
                }
            } catch (e: Exception) {
                failCount++
                details.add("✗ $role ($chatId): ${e.message}")
            }
        }

        TelegramDispatchReport(
            totalCount = targets.size,
            successCount = successCount,
            failureCount = failCount,
            details = details
        )
    }

    suspend fun sendTestTelegramPing(): TelegramDispatchReport = withContext(Dispatchers.IO) {
        val testHtml = """
            ✅ <b>Shapoorji Delivery Bot Connection Test</b>
            
            🔔 Notification Dispatch System is operational!
            Store Admin, Managers & Staff will receive instant alerts for every new order placed and status change inside Shapoorji Shukhobrishti.
            
            ⏰ <i>Tested at: ${SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date())}</i>
        """.trimIndent()
        sendTelegramBroadcast(testHtml)
    }

    suspend fun sendTestEmailPing(): SmtpResult = withContext(Dispatchers.IO) {
        val client = SmtpClient(
            host = smtpHost,
            port = smtpPort,
            username = smtpUsername,
            password = smtpPassword
        )
        val recipients = listOf(adminEmail, smtpUsername).distinct()
        val subject = "✅ [Shapoorji Delivery] SMTP Dispatch Test"
        val body = """
            Hello Store Administrator,
            
            This is an automated SMTP connection test from Shapoorji Delivery App.
            Outgoing Server: $smtpHost (Port $smtpPort - SSL)
            Sender: $senderEmail
            
            If you received this message, the email notification engine is operating normally.
            Customer order confirmations and status changes will be automatically delivered to this address.
        """.trimIndent()

        val result = client.sendEmail(
            from = senderEmail,
            recipients = recipients,
            subject = subject,
            bodyText = body
        )

        // Insert log
        val log = NotificationLog(
            orderId = 0,
            orderNumber = "TEST-EMAIL",
            type = NotificationType.EMAIL,
            sender = senderEmail,
            recipient = recipients.joinToString(", "),
            title = subject,
            content = body,
            status = if (result.success) "DELIVERED" else "FAILED: ${result.message}",
            timestamp = System.currentTimeMillis()
        )
        logDao.insertLog(log)

        result
    }
}
