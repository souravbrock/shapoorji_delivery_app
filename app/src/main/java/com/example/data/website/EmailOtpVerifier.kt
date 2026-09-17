package com.example.data.website

import android.content.Context
import android.util.Log
import com.example.data.notification.SmtpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom

sealed interface EmailOtpState {
    data object Idle : EmailOtpState
    data object Sending : EmailOtpState
    data class CodeSent(val email: String, val resendAtMillis: Long) : EmailOtpState
    data class Verified(val email: String) : EmailOtpState
    data class Failed(val message: String) : EmailOtpState
}

/**
 * Email ownership check for new Store Accounts: sends a 6-digit code from
 * order@spdelivery.reddevils.co.in (existing SMTP) that must be entered
 * before the website account is created. The website backend itself has no
 * verification flow, so this is enforced app-side at registration.
 */
class EmailOtpVerifier(context: Context) {

    companion object {
        private const val TAG = "EmailOtpVerifier"
        private const val PREFS = "website_email_verify_prefs"
        private const val KEY_VERIFIED_PREFIX = "verified_email_"
        private const val OTP_TTL_MILLIS = 10 * 60 * 1000L
        private const val MAX_ATTEMPTS = 5
        private const val SENDER = "order@spdelivery.reddevils.co.in"
    }

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val smtp = SmtpClient()
    private val random = SecureRandom()

    private var pendingEmail: String? = null
    private var pendingCode: String? = null
    private var pendingExpiry: Long = 0L
    private var attempts: Int = 0

    fun isVerified(email: String): Boolean =
        prefs.getBoolean(KEY_VERIFIED_PREFIX + email.trim().lowercase(), false)

    private fun markVerified(email: String) {
        prefs.edit().putBoolean(KEY_VERIFIED_PREFIX + email.trim().lowercase(), true).apply()
    }

    suspend fun sendCode(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val clean = email.trim()
        if (!clean.contains("@")) {
            return@withContext Result.failure(IllegalStateException("Enter a valid email address"))
        }
        val code = String.format("%06d", random.nextInt(1_000_000))
        val subject = "Your Shapoorji Delivery verification code: $code"
        val body = buildString {
            appendLine("Hello,")
            appendLine()
            appendLine("Your email verification code for Shapoorji Delivery (spdelivery.reddevils.co.in) is:")
            appendLine()
            appendLine("    $code")
            appendLine()
            appendLine("Enter this code in the app to finish creating your Store Account.")
            appendLine("The code expires in 10 minutes. If you did not request this, ignore this email.")
        }
        try {
            val result = smtp.sendEmail(
                from = SENDER,
                recipients = listOf(clean),
                subject = subject,
                bodyText = body
            )
            if (!result.success) {
                return@withContext Result.failure(IllegalStateException("Could not send code: ${result.message}"))
            }
            pendingEmail = clean.lowercase()
            pendingCode = code
            pendingExpiry = System.currentTimeMillis() + OTP_TTL_MILLIS
            attempts = 0
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "sendCode: ${e.message}")
            Result.failure(IllegalStateException("Could not send code: ${e.message}"))
        }
    }

    fun confirmCode(email: String, code: String): Result<Unit> {
        val clean = email.trim().lowercase()
        val expected = pendingCode
        if (pendingEmail == null || expected == null || clean != pendingEmail) {
            return Result.failure(IllegalStateException("Request a fresh code for this email first"))
        }
        if (System.currentTimeMillis() > pendingExpiry) {
            clearPending()
            return Result.failure(IllegalStateException("Code expired — request a new one"))
        }
        attempts++
        if (attempts > MAX_ATTEMPTS) {
            clearPending()
            return Result.failure(IllegalStateException("Too many attempts — request a new code"))
        }
        if (code.trim() != expected) {
            return Result.failure(IllegalStateException("Wrong code (${MAX_ATTEMPTS - attempts + 1} tries left)"))
        }
        markVerified(clean)
        clearPending()
        return Result.success(Unit)
    }

    fun canResend(): Boolean = pendingEmail == null

    private fun clearPending() {
        pendingEmail = null
        pendingCode = null
        pendingExpiry = 0L
        attempts = 0
    }
}
