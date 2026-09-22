package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Authentication Logic Layer (local device profile + Store Account).
 *
 * No Google/Firebase: identity lives in SharedPreferences on this device and
 * durably in the store backend (spdelivery.reddevils.co.in) via WebsiteBackend.
 * Enforces role-based security across the Shapoorji Delivery platform:
 * 1. Checks customer sign-in status (required to enter the app)
 * 2. Stores the resident profile locally on this device
 * 3. Strictly restricts Store Admin Console access to 'souravbrock@gmail.com'
 */
class AuthManager(private val context: Context) {

    companion object {
        private const val TAG = "AuthManager"
        const val ADMIN_EMAIL = "souravbrock@gmail.com"
        private const val PREFS_NAME = "shapoorji_auth_prefs"
        private const val KEY_IS_SIGNED_IN = "is_signed_in"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_TOWER = "user_tower"
        private const val KEY_USER_FLAT = "user_flat"
        private const val KEY_USER_PHOTO = "user_photo"
        private const val KEY_LAST_ACTIVE_EMAIL = "last_active_email"
        private const val KEY_REGISTERED_EMAILS = "registered_emails_set"

        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow(loadInitialUser())
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private fun loadInitialUser(): UserProfile {
        val isSignedIn = prefs.getBoolean(KEY_IS_SIGNED_IN, false)
        val activeEmail = prefs.getString(KEY_USER_EMAIL, null)
        return if (isSignedIn && !activeEmail.isNullOrBlank()) {
            val registered = getRegisteredUser(activeEmail)
            registered ?: UserProfile(
                name = prefs.getString(KEY_USER_NAME, "Resident") ?: "Resident",
                email = activeEmail,
                phone = prefs.getString(KEY_USER_PHONE, "") ?: "",
                tower = prefs.getString(KEY_USER_TOWER, "") ?: "",
                flatNumber = prefs.getString(KEY_USER_FLAT, "") ?: "",
                isGoogleSignedIn = true,
                photoUrl = prefs.getString(KEY_USER_PHOTO, "") ?: ""
            )
        } else {
            UserProfile(
                name = "",
                email = "",
                phone = "",
                tower = "",
                flatNumber = "",
                isGoogleSignedIn = false,
                photoUrl = ""
            )
        }
    }

    /**
     * Gets all registered emails remembered on this device.
     */
    fun getRegisteredEmails(): Set<String> {
        return prefs.getStringSet(KEY_REGISTERED_EMAILS, emptySet()) ?: emptySet()
    }

    /**
     * Retrieves saved profile for a registered email on this device.
     */
    fun getRegisteredUser(email: String): UserProfile? {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return null
        
        val nameKey = "user_${cleanEmail}_name"
        val savedName = prefs.getString(nameKey, null) ?: return null

        val phone = prefs.getString("user_${cleanEmail}_phone", "") ?: ""
        val tower = prefs.getString("user_${cleanEmail}_tower", "Shukhobrishti Phase 1 - Tower A1") ?: "Shukhobrishti Phase 1 - Tower A1"
        val flat = prefs.getString("user_${cleanEmail}_flat", "") ?: ""
        val photo = prefs.getString("user_${cleanEmail}_photo", "") ?: ""

        return UserProfile(
            name = savedName,
            email = cleanEmail,
            phone = phone,
            tower = tower,
            flatNumber = flat,
            isGoogleSignedIn = true,
            photoUrl = photo
        )
    }

    /**
     * Returns all registered profiles stored on this device.
     */
    fun getAllRegisteredUsers(): List<UserProfile> {
        val emails = getRegisteredEmails()
        return emails.mapNotNull { getRegisteredUser(it) }
    }

    /**
     * Returns the last active registered user profile on this device, or null if fresh install.
     */
    fun getLastRegisteredUser(): UserProfile? {
        val lastEmail = prefs.getString(KEY_LAST_ACTIVE_EMAIL, null)
        if (!lastEmail.isNullOrBlank()) {
            val user = getRegisteredUser(lastEmail)
            if (user != null) return user
        }
        return null
    }

    /**
     * Checks if this email has already registered flat delivery details.
     */
    fun isEmailRegistered(email: String): Boolean {
        val clean = email.trim().lowercase()
        if (clean.isBlank()) return false
        return getRegisteredEmails().contains(clean)
    }

    /**
     * 1-Tap Sign-In for an already registered user.
     */
    fun quickSignIn(email: String): UserProfile? {
        val existing = getRegisteredUser(email) ?: return null
        return signInWithGoogle(
            name = existing.name,
            email = existing.email,
            phone = existing.phone,
            tower = existing.tower,
            flat = existing.flatNumber,
            photoUrl = existing.photoUrl
        )
    }

    /**
     * Verifies whether the provided user is strictly authorized for Admin Privileges.
     * Only 'souravbrock@gmail.com' (case-insensitive) matches.
     */
    fun isAuthorizedAdmin(user: UserProfile?): Boolean {
        if (user == null || !user.isGoogleSignedIn) return false
        return user.email.trim().equals(ADMIN_EMAIL, ignoreCase = true)
    }

    /**
     * Verifies whether the provided user is strictly authorized for Admin Privileges.
     */
    fun isAuthorizedAdmin(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        return email.trim().equals(ADMIN_EMAIL, ignoreCase = true)
    }

    /**
     * Evaluates access result before rendering the admin dashboard component.
     */
    fun evaluateAdminAccess(user: UserProfile): AdminAccessResult {
        return when {
            !user.isGoogleSignedIn -> AdminAccessResult.Unauthenticated
            isAuthorizedAdmin(user) -> AdminAccessResult.Granted(user)
            else -> AdminAccessResult.Denied(
                userEmail = user.email,
                message = "Access Restricted: Device email '${user.email}' does not have administrator privileges. Admin access is strictly reserved for $ADMIN_EMAIL."
            )
        }
    }

    /**
     * Signs a resident in with the local device profile (name/email/phone/tower/flat).
     * Durable identity is the Store Account on spdelivery.reddevils.co.in (see WebsiteBackend).
     */
    fun signInWithGoogle(
        name: String,
        email: String,
        phone: String,
        tower: String,
        flat: String,
        photoUrl: String = ""
    ): UserProfile {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim().ifBlank {
            cleanEmail.substringBefore("@")
                .replace(".", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
        }
        val cleanPhone = phone.trim()
        val cleanTower = tower.trim().ifBlank { "Shukhobrishti Phase 1 - Tower A1" }
        val cleanFlat = flat.trim()

        val profile = UserProfile(
            name = cleanName,
            email = cleanEmail,
            phone = cleanPhone,
            tower = cleanTower,
            flatNumber = cleanFlat,
            isGoogleSignedIn = true,
            photoUrl = photoUrl
        )

        // Save to persistent user registry so account is never forgotten
        val existingEmails = prefs.getStringSet(KEY_REGISTERED_EMAILS, emptySet())?.toMutableSet() ?: mutableSetOf()
        existingEmails.add(cleanEmail)

        prefs.edit()
            .putBoolean(KEY_IS_SIGNED_IN, true)
            .putString(KEY_USER_NAME, cleanName)
            .putString(KEY_USER_EMAIL, cleanEmail)
            .putString(KEY_USER_PHONE, cleanPhone)
            .putString(KEY_USER_TOWER, cleanTower)
            .putString(KEY_USER_FLAT, cleanFlat)
            .putString(KEY_USER_PHOTO, photoUrl)
            .putString(KEY_LAST_ACTIVE_EMAIL, cleanEmail)
            .putStringSet(KEY_REGISTERED_EMAILS, existingEmails)
            .putString("user_${cleanEmail}_name", cleanName)
            .putString("user_${cleanEmail}_phone", cleanPhone)
            .putString("user_${cleanEmail}_tower", cleanTower)
            .putString("user_${cleanEmail}_flat", cleanFlat)
            .putString("user_${cleanEmail}_photo", photoUrl)
            .apply()

        _currentUser.value = profile
        return profile
    }

    /**
     * Signs out the user, clearing the local session.
     */
    fun signOut(): UserProfile {
        val lastEmail = _currentUser.value.email.ifBlank {
            prefs.getString(KEY_USER_EMAIL, "") ?: ""
        }

        prefs.edit()
            .putBoolean(KEY_IS_SIGNED_IN, false)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_PHONE)
            .remove(KEY_USER_TOWER)
            .remove(KEY_USER_FLAT)
            .remove(KEY_USER_PHOTO)
            .putString(KEY_LAST_ACTIVE_EMAIL, lastEmail)
            .apply()

        val guestUser = UserProfile(
            name = "",
            email = "",
            phone = "",
            tower = "",
            flatNumber = "",
            isGoogleSignedIn = false,
            photoUrl = ""
        )
        _currentUser.value = guestUser
        return guestUser
    }
}

/**
 * Result of authentication check for admin access
 */
sealed interface AdminAccessResult {
    data class Granted(val user: UserProfile) : AdminAccessResult
    data object Unauthenticated : AdminAccessResult
    data class Denied(val userEmail: String, val message: String) : AdminAccessResult
}
