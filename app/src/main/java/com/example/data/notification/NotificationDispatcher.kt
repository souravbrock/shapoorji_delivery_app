package com.example.data.notification

import android.content.Context
import android.util.Log
import com.example.data.local.NotificationLogDao
import com.example.data.model.NotificationLog
import com.example.data.model.NotificationType
import com.example.data.model.Order
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
            appendLine("Location Status: Verified Inside Shapoorji Sukhobristi")
            if (order.deliveryNotes.isNotBlank()) {
                appendLine("Notes         : ${order.deliveryNotes}")
            }
            appendLine("--------------------------------------------------")
            appendLine("ORDER ITEMS & PRICING:")
            appendLine("Items Count   : ${order.itemCount} items")
            appendLine("Subtotal      : ₹${String.format(Locale.US, "%.2f", order.subtotal)}")
            appendLine("Delivery Fee  : ₹${String.format(Locale.US, "%.2f", order.deliveryFee)} (Free inside Shapoorji)")
            appendLine("Grand Total   : ₹${String.format(Locale.US, "%.2f", order.grandTotal)}")
            appendLine("Payment Method: ${order.paymentMethod}")
            appendLine("==================================================")
            appendLine("Delivered exclusively inside Shapoorji Sukhobristi, Action Area III, Kolkata.")
            appendLine("Store Contact: order@spdelivery.reddevils.co.in | +91 98765 43210")
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

        // 3. Prepare Telegram Bot Message (using HTML to avoid Markdown parsing bugs)
        val telegramHtml = buildString {
            appendLine(if (isNewOrder) "🚨 <b>NEW ORDER RECEIVED - SHAPOORJI DELIVERY</b>" else "🚚 <b>ORDER STATUS CHANGED</b>")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("📌 <b>Status:</b> ${order.status.label}")
            appendLine("🆔 <b>Order:</b> <code>${order.orderNumber}</code>")
            appendLine("📄 <b>Invoice:</b> <code>${order.invoiceNumber}</code>")
            appendLine("👤 <b>Customer:</b> ${order.customerName} (${order.customerPhone})")
            appendLine("📍 <b>Address:</b> ${order.towerName}, ${order.flatNumber}")
            appendLine("💰 <b>Amount:</b> ₹${String.format(Locale.US, "%.2f", order.grandTotal)} (${order.paymentMethod})")
            appendLine("📦 <b>Total Items:</b> ${order.itemCount}")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("🔔 <i>Alert sent to Store Admin, Managers & Staff</i>")
        }

        // 4. Real Telegram Dispatch to all roles
        val report = sendTelegramBroadcast(telegramHtml)

        val telegramLog = NotificationLog(
            orderId = order.id,
            orderNumber = order.orderNumber,
            type = NotificationType.TELEGRAM,
            sender = "Shapoorji Orders Bot (@ShapoorjiOrdersBot)",
            recipient = "Admin (${parseChatIds(adminChatIds).size}), Managers (${parseChatIds(managerChatIds).size}), Staff (${parseChatIds(staffChatIds).size})",
            title = "Telegram Order Alert #${order.orderNumber} (${order.status.label})",
            content = "${report.successCount}/${report.totalCount} delivered.\n${report.details.joinToString("\n")}",
            status = if (report.successCount > 0) "DELIVERED (${report.successCount}/${report.totalCount})" else "FAILED",
            timestamp = System.currentTimeMillis()
        )
        logDao.insertLog(telegramLog)
        Log.i(TAG, "Telegram broadcast finished: ${report.successCount}/${report.totalCount} sent")
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
                    put("parse_mode", "HTML")
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
            Store Admin, Managers & Staff will receive instant alerts for every new order placed and status change inside Shapoorji Sukhobristi.
            
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
