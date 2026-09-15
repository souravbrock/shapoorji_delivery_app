package com.example

import com.example.data.auth.AuthManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AuthManagerTest {

    @Test
    fun residentRegistration_persistsAfterSignOut() {
        val context = RuntimeEnvironment.getApplication()
        val authManager = AuthManager.getInstance(context)

        // 1. Register a customer
        val registeredUser = authManager.signInWithGoogle(
            name = "Anirban Sen",
            email = "anirban.sen@gmail.com",
            phone = "+91-9830099999",
            tower = "Sukhobristi Phase 1 - Tower A3",
            flat = "Flat 502, 5th Floor"
        )

        assertTrue(registeredUser.isGoogleSignedIn)
        assertEquals("Anirban Sen", registeredUser.name)
        assertEquals("anirban.sen@gmail.com", registeredUser.email)
        assertEquals("Flat 502, 5th Floor", registeredUser.flatNumber)

        // Verify registry contains user
        assertTrue(authManager.isEmailRegistered("anirban.sen@gmail.com"))

        // 2. Sign out
        authManager.signOut()

        // Active session is signed out
        assertFalse(authManager.currentUser.value.isGoogleSignedIn)

        // BUT registered user profile is STILL preserved!
        assertTrue(authManager.isEmailRegistered("anirban.sen@gmail.com"))
        val cachedUser = authManager.getRegisteredUser("anirban.sen@gmail.com")
        assertNotNull(cachedUser)
        assertEquals("Anirban Sen", cachedUser?.name)
        assertEquals("Flat 502, 5th Floor", cachedUser?.flatNumber)
        assertEquals("Sukhobristi Phase 1 - Tower A3", cachedUser?.tower)

        // 3. User logs back in (via quickSignIn / Google account chooser)
        val reLoggedInUser = authManager.quickSignIn("anirban.sen@gmail.com")
        assertNotNull(reLoggedInUser)
        assertTrue(reLoggedInUser!!.isGoogleSignedIn)
        assertEquals("Anirban Sen", reLoggedInUser.name)
        assertEquals("Flat 502, 5th Floor", reLoggedInUser.flatNumber)
        assertEquals("Sukhobristi Phase 1 - Tower A3", reLoggedInUser.tower)
    }

    @Test
    fun adminUser_souravbrock_hasAdminPrivilege() {
        val context = RuntimeEnvironment.getApplication()
        val authManager = AuthManager.getInstance(context)

        val adminUser = authManager.signInWithGoogle(
            name = "Sourav Brock",
            email = "souravbrock@gmail.com",
            phone = "+91-8442980101",
            tower = "Sukhobristi Phase 1 - Tower A4",
            flat = "Flat 803"
        )

        assertTrue(adminUser.isAdmin)
        assertTrue(authManager.isAuthorizedAdmin(adminUser))
    }
}
