package com.example

import com.example.data.auth.AuthManager
import com.example.data.auth.FirebaseAuthService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FirebaseAuthServiceTest {

    @Test
    fun verifyUserEmail_adminEmail_returnsAdminRole() {
        val context = RuntimeEnvironment.getApplication()
        val authService = FirebaseAuthService.getInstance(context)

        // Case-insensitive & trimmed checks
        val verificationLower = authService.verifyUserEmail("souravbrock@gmail.com")
        assertTrue(verificationLower.isAdmin)
        assertEquals("Store Administrator", verificationLower.roleTitle)

        val verificationMixed = authService.verifyUserEmail("  SouravBrock@GMAIL.com  ")
        assertTrue(verificationMixed.isAdmin)
        assertEquals("Store Administrator", verificationMixed.roleTitle)
    }

    @Test
    fun verifyUserEmail_residentEmail_returnsCustomerRole() {
        val context = RuntimeEnvironment.getApplication()
        val authService = FirebaseAuthService.getInstance(context)

        val verificationCustomer = authService.verifyUserEmail("resident.customer@gmail.com")
        assertFalse(verificationCustomer.isAdmin)
        assertEquals("Resident Customer", verificationCustomer.roleTitle)

        val verificationOther = authService.verifyUserEmail("john.doe@shapoorji.com")
        assertFalse(verificationOther.isAdmin)
        assertEquals("Resident Customer", verificationOther.roleTitle)
    }
}
