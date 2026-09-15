package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ShapoorjiGeo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Shapoorji Delivery", appName)
  }

  @Test
  fun `verify shapoorji geofence validation`() {
    // Inside Sukhobristi center
    assertTrue(ShapoorjiGeo.isInsideShapoorji(22.5695, 88.5195))

    // Outside: Sector V Salt Lake
    assertFalse(ShapoorjiGeo.isInsideShapoorji(22.5868, 88.4355))

    // Outside: Kolkata Airport
    assertFalse(ShapoorjiGeo.isInsideShapoorji(22.6547, 88.4467))
  }

  @Test
  fun `verify notification dispatcher defaults and target ids`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = com.example.data.local.AppDatabase.getDatabase(context, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined))
    val dispatcher = com.example.data.notification.NotificationDispatcher(context, database.notificationLogDao())

    assertEquals("order@spdelivery.reddevils.co.in", dispatcher.smtpUsername)
    assertEquals("souravbrock@gmail.com", dispatcher.adminEmail)
    assertEquals("8906839330:AAEOlqOSvXVVrV5A6N0yIdnUuSPzInMjAJ0", dispatcher.telegramBotToken)

    val targetIds = dispatcher.getAllTargetChatIds().map { it.second }
    assertTrue(targetIds.contains("167694312"))
    assertTrue(targetIds.contains("7127777789"))
    assertTrue(targetIds.contains("8924193494"))
    assertTrue(targetIds.contains("9083900751"))
    assertTrue(targetIds.contains("58088380"))
  }

  @Test
  fun `verify admin access is strictly locked to souravbrock@gmail dot com`() {
    val adminUser = com.example.data.model.UserProfile(
        name = "Sourav Brock",
        email = "souravbrock@gmail.com",
        phone = "+91-8442980101"
    )
    assertTrue(adminUser.isAdmin)
    assertEquals("+91-8442980101", adminUser.phone)

    val regularCustomer = com.example.data.model.UserProfile(
        name = "Customer Tester",
        email = "customer@example.com",
        phone = "+91-9830099999"
    )
    assertFalse(regularCustomer.isAdmin)
  }

  @Test
  fun `verify auth manager evaluates admin access and sign in gate correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authManager = com.example.data.auth.AuthManager.getInstance(context)

    // Initially sign out to test unauthenticated gate
    authManager.signOut()
    assertFalse(authManager.currentUser.value.isGoogleSignedIn)
    assertEquals(
        com.example.data.auth.AdminAccessResult.Unauthenticated,
        authManager.evaluateAdminAccess(authManager.currentUser.value)
    )

    // Sign in as customer
    val customer = authManager.signInWithGoogle(
        name = "Rahul Sharma",
        email = "rahul.resident@gmail.com",
        phone = "+91-9830012345",
        tower = "Phase 1 - Tower A4",
        flat = "Flat 502"
    )
    assertTrue(customer.isGoogleSignedIn)
    assertFalse(authManager.isAuthorizedAdmin(customer))
    assertTrue(authManager.evaluateAdminAccess(customer) is com.example.data.auth.AdminAccessResult.Denied)

    // Sign in as store administrator (souravbrock@gmail.com)
    val admin = authManager.signInWithGoogle(
        name = "Sourav Brock",
        email = "  SOURAVBROCK@GMAIL.COM  ",
        phone = "+91-8442980101",
        tower = "Phase 1 - Tower A4",
        flat = "Flat 803"
    )
    assertTrue(admin.isGoogleSignedIn)
    assertTrue(authManager.isAuthorizedAdmin(admin))
    assertTrue(authManager.evaluateAdminAccess(admin) is com.example.data.auth.AdminAccessResult.Granted)
  }
}
