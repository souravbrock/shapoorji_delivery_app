package com.example.data.notification

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class SmtpResult(
    val success: Boolean,
    val message: String,
    val serverLog: String = ""
)

class SmtpClient(
    val host: String = "spdelivery.reddevils.co.in",
    val port: Int = 465,
    val username: String = "order@spdelivery.reddevils.co.in",
    val password: String = "K?@f{Ntv2lX]APW0"
) {
    companion object {
        private const val TAG = "SmtpClient"
        private const val TIMEOUT_MS = 10000
    }

    private fun base64Encode(str: String): String {
        return java.util.Base64.getEncoder().encodeToString(str.toByteArray(StandardCharsets.UTF_8))
    }

    suspend fun sendEmail(
        from: String,
        recipients: List<String>,
        subject: String,
        bodyText: String
    ): SmtpResult = withContext(Dispatchers.IO) {
        val cleanPassword = password.trim().removeSurrounding("\"")
        val cleanUsername = username.trim()
        val validRecipients = recipients.map { it.trim() }.filter { it.contains("@") }.distinct()

        if (validRecipients.isEmpty()) {
            return@withContext SmtpResult(false, "No valid email recipients specified")
        }

        val logBuilder = StringBuilder()
        var socket: SSLSocket? = null
        try {
            logBuilder.appendLine("Connecting to $host:$port via SSL...")
            val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val rawSocket = sslFactory.createSocket(host, port) as SSLSocket
            rawSocket.soTimeout = TIMEOUT_MS
            rawSocket.startHandshake()
            socket = rawSocket

            val reader = BufferedReader(InputStreamReader(socket.inputStream, StandardCharsets.UTF_8))
            val writer = BufferedWriter(OutputStreamWriter(socket.outputStream, StandardCharsets.UTF_8))

            fun readReply(): Pair<Int, String> {
                val sb = StringBuilder()
                var code = 0
                while (true) {
                    val line = reader.readLine() ?: break
                    sb.appendLine(line)
                    if (line.length >= 3) {
                        val parsedCode = line.substring(0, 3).toIntOrNull()
                        if (parsedCode != null) {
                            code = parsedCode
                            if (line.length == 3 || line[3] == ' ') {
                                break
                            }
                        }
                    }
                }
                val full = sb.toString().trim()
                logBuilder.appendLine("<< $full")
                return code to full
            }

            fun sendCmd(cmd: String, redact: Boolean = false): Pair<Int, String> {
                if (redact) {
                    logBuilder.appendLine(">> [AUTH REDACTED]")
                } else {
                    logBuilder.appendLine(">> $cmd")
                }
                writer.write(cmd + "\r\n")
                writer.flush()
                return readReply()
            }

            // 1. Read greeting
            val (bannerCode, banner) = readReply()
            if (bannerCode != 220) {
                return@withContext SmtpResult(false, "Server greeting failed: $banner", logBuilder.toString())
            }

            // 2. EHLO
            val (ehloCode, ehloResp) = sendCmd("EHLO localhost")
            if (ehloCode != 250) {
                return@withContext SmtpResult(false, "EHLO failed: $ehloResp", logBuilder.toString())
            }

            // 3. AUTH LOGIN
            val (authCode, authResp) = sendCmd("AUTH LOGIN")
            if (authCode != 334) {
                return@withContext SmtpResult(false, "AUTH LOGIN initiation failed: $authResp", logBuilder.toString())
            }

            // Send username
            val (uCode, uResp) = sendCmd(base64Encode(cleanUsername), redact = false)
            if (uCode != 334) {
                return@withContext SmtpResult(false, "Username challenge failed: $uResp", logBuilder.toString())
            }

            // Send password
            var (pCode, pResp) = sendCmd(base64Encode(cleanPassword), redact = true)
            // If failed, try with quoted password just in case
            if (pCode != 235 && cleanPassword != password) {
                // Already tried without quotes; attempt with raw if different
            }

            if (pCode != 235) {
                return@withContext SmtpResult(
                    success = false,
                    message = "SMTP Auth Rejected: $pResp (Please verify password in Admin Settings)",
                    serverLog = logBuilder.toString()
                )
            }

            // 4. MAIL FROM
            val (mailCode, mailResp) = sendCmd("MAIL FROM:<$from>")
            if (mailCode != 250) {
                return@withContext SmtpResult(false, "MAIL FROM rejected: $mailResp", logBuilder.toString())
            }

            // 5. RCPT TO for each recipient
            var acceptedRecipients = 0
            for (rcpt in validRecipients) {
                val (rcptCode, rcptResp) = sendCmd("RCPT TO:<$rcpt>")
                if (rcptCode == 250 || rcptCode == 251) {
                    acceptedRecipients++
                } else {
                    logBuilder.appendLine("Warning: Recipient $rcpt rejected with $rcptResp")
                }
            }

            if (acceptedRecipients == 0) {
                return@withContext SmtpResult(false, "All recipients were rejected by server", logBuilder.toString())
            }

            // 6. DATA
            val (dataCode, dataResp) = sendCmd("DATA")
            if (dataCode != 354) {
                return@withContext SmtpResult(false, "DATA command rejected: $dataResp", logBuilder.toString())
            }

            // 7. Write message content
            val dateStr = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).format(Date())
            val msgBuilder = StringBuilder().apply {
                appendLine("From: \"Shapoorji Delivery\" <$from>")
                appendLine("To: ${validRecipients.joinToString(", ")}")
                appendLine("Subject: $subject")
                appendLine("Date: $dateStr")
                appendLine("MIME-Version: 1.0")
                appendLine("Content-Type: text/plain; charset=UTF-8")
                appendLine("Content-Transfer-Encoding: 8bit")
                appendLine()
                appendLine(bodyText)
                appendLine(".")
            }

            writer.write(msgBuilder.toString())
            writer.flush()

            val (finalCode, finalResp) = readReply()
            sendCmd("QUIT")

            if (finalCode == 250) {
                Log.i(TAG, "Email successfully sent to $validRecipients")
                SmtpResult(
                    success = true,
                    message = "Email dispatched successfully to ${validRecipients.size} recipients via SMTP: $finalResp",
                    serverLog = logBuilder.toString()
                )
            } else {
                SmtpResult(
                    success = false,
                    message = "Email data send returned non-250 code: $finalResp",
                    serverLog = logBuilder.toString()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "SMTP error: ${e.message}", e)
            logBuilder.appendLine("Exception: ${e.message}")
            SmtpResult(
                success = false,
                message = "SMTP Network Error: ${e.message ?: "Unknown error"}",
                serverLog = logBuilder.toString()
            )
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }
}
