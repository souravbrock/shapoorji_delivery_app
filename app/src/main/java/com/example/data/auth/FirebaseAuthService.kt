package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Result of Google Sign-In via Credential Manager and Firebase Authentication
 */
sealed interface GoogleAuthResult {
    data class Success(
        val email: String,
        val displayName: String,
        val photoUrl: String,
        val idToken: String,
        val firebaseUser: FirebaseUser?,
        val emailVerification: EmailRoleVerification
    ) : GoogleAuthResult

    data class NeedsFallbackPicker(
        val reason: String
    ) : GoogleAuthResult

    data object Cancelled : GoogleAuthResult

    data class Failure(
        val message: String,
        val cause: Throwable? = null
    ) : GoogleAuthResult
}

/**
 * Verification details for the user's signed-in email address
 */
data class EmailRoleVerification(
    val email: String,
    val isAdmin: Boolean,
    val roleTitle: String,
    val message: String
)

/**
 * Firebase Authentication Service
 *
 * Handles Google Sign-In using the AndroidX Credential Manager library,
 * exchanges tokens with Firebase Authentication, and validates role permissions
 * based on user email address (strictly distinguishing 'souravbrock@gmail.com' for admin).
 */
class FirebaseAuthService(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (_: Throwable) {
        null
    }
) {
    companion object {
        private const val TAG = "FirebaseAuthService"

        @Volatile
        private var INSTANCE: FirebaseAuthService? = null

        fun getInstance(context: Context): FirebaseAuthService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseAuthService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val credentialManager: CredentialManager = CredentialManager.create(context)

    private val _currentUserState = MutableStateFlow<FirebaseUser?>(firebaseAuth?.currentUser)
    val currentUserState: StateFlow<FirebaseUser?> = _currentUserState.asStateFlow()

    init {
        try {
            firebaseAuth?.addAuthStateListener { auth ->
                _currentUserState.value = auth.currentUser
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to register Firebase auth state listener: ${e.message}")
        }
    }

    /**
     * Checks whether the given email has Administrator privileges or Customer view.
     */
    fun verifyUserEmail(email: String): EmailRoleVerification {
        val cleanEmail = email.trim().lowercase()
        val isAdmin = cleanEmail == AuthManager.ADMIN_EMAIL.lowercase()
        return if (isAdmin) {
            EmailRoleVerification(
                email = cleanEmail,
                isAdmin = true,
                roleTitle = "Store Administrator",
                message = "Verified Store Administrator ($cleanEmail). Full access to product pricing, order status updates, and broadcast alerts granted."
            )
        } else {
            EmailRoleVerification(
                email = cleanEmail,
                isAdmin = false,
                roleTitle = "Resident Customer",
                message = "Verified Resident ($cleanEmail). Customer storefront view granted with 15-25 min doorstep delivery inside Shapoorji."
            )
        }
    }

    /**
     * Launches Google Sign-In flow using AndroidX Credential Manager.
     *
     * 1. Constructs GetCredentialRequest with GetGoogleIdOption
     * 2. Prompts user via Credential Manager
     * 3. Extracts GoogleIdTokenCredential
     * 4. Exchanges ID token with Firebase Auth (GoogleAuthProvider)
     * 5. Returns verified user profile and role verification
     */
    suspend fun signInWithGoogle(
        activityContext: Context,
        customServerClientId: String? = null
    ): GoogleAuthResult {
        return try {
            // Retrieve Google Web Client ID from resources or parameter
            val serverClientId = customServerClientId
                ?: getServerClientId(activityContext)

            if (serverClientId.isNullOrBlank() || serverClientId.contains("placeholder")) {
                Log.d(TAG, "No Google Web Client ID configured, launching Google Account Chooser directly")
                return GoogleAuthResult.NeedsFallbackPicker("Select your Google account to continue")
            }

            val googleIdOptionBuilder = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)

            if (!serverClientId.isNullOrBlank()) {
                googleIdOptionBuilder.setServerClientId(serverClientId)
            }

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOptionBuilder.build())
                .build()

            Log.d(TAG, "Requesting credential via CredentialManager...")
            val response = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val email = googleIdTokenCredential.id
                    val displayName = googleIdTokenCredential.displayName ?: "Resident"
                    val photoUrl = googleIdTokenCredential.profilePictureUri?.toString() ?: ""

                    // Exchange ID Token with Firebase Authentication if available
                    val authResult = try {
                        if (firebaseAuth != null) {
                            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                            firebaseAuth.signInWithCredential(authCredential).await()
                        } else null
                    } catch (fbException: Throwable) {
                        Log.w(TAG, "Firebase signInWithCredential warning: ${fbException.message}. Proceeding with verified Google token.")
                        null
                    }

                    val firebaseUser = authResult?.user ?: firebaseAuth?.currentUser
                    val resolvedEmail = firebaseUser?.email ?: email
                    val resolvedName = firebaseUser?.displayName ?: displayName
                    val resolvedPhoto = firebaseUser?.photoUrl?.toString() ?: photoUrl

                    val verification = verifyUserEmail(resolvedEmail)

                    GoogleAuthResult.Success(
                        email = resolvedEmail,
                        displayName = resolvedName,
                        photoUrl = resolvedPhoto,
                        idToken = idToken,
                        firebaseUser = firebaseUser,
                        emailVerification = verification
                    )
                } catch (parsingException: GoogleIdTokenParsingException) {
                    Log.e(TAG, "Invalid Google ID token response", parsingException)
                    GoogleAuthResult.Failure("Failed to parse Google ID token: ${parsingException.message}", parsingException)
                }
            } else {
                Log.w(TAG, "Unexpected credential type returned: ${credential.type}")
                GoogleAuthResult.NeedsFallbackPicker("Google account credential not returned directly by Credential Manager.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "User cancelled Google Sign-In prompt")
            GoogleAuthResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google accounts found or Play Services credential prompt unavailable", e)
            GoogleAuthResult.NeedsFallbackPicker("No Google accounts found on device. Select an account to continue.")
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager error: ${e.message}", e)
            // If device environment cannot connect to Google Play Services, trigger interactive chooser
            GoogleAuthResult.NeedsFallbackPicker("Credential Manager prompt: ${e.message}")
        } catch (e: Throwable) {
            Log.e(TAG, "Unhandled error during Google sign-in", e)
            GoogleAuthResult.Failure(e.message ?: "Unknown authentication error occurred", e)
        }
    }

    /**
     * Signs out from Firebase Authentication and clears Credential Manager session.
     */
    suspend fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Throwable) {
            Log.w(TAG, "Error signing out of Firebase: ${e.message}")
        }

        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Throwable) {
            Log.w(TAG, "Error clearing Credential Manager state: ${e.message}")
        }
    }

    /**
     * Attempts to resolve the default Web Client ID generated by Google Services plugin.
     */
    private fun getServerClientId(context: Context): String? {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId).takeIf { it.isNotBlank() } else null
        } catch (_: Throwable) {
            null
        }
    }
}
