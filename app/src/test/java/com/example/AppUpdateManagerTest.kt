package com.example

import com.example.data.update.AppUpdateManager
import com.example.data.update.UpdateStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppUpdateManagerTest {

    @Test
    fun versionConfiguration_isIncrementedForSeamlessUpdates() {
        val context = RuntimeEnvironment.getApplication()
        val updateManager = AppUpdateManager(context)

        // Version Code must be >= 2 for the in-place upgrade path
        assertTrue("versionCode must be >= 2", updateManager.currentVersionCode >= 2)
        assertEquals("1.1.0", updateManager.currentVersionName)
    }

    @Test
    fun checkUpdates_returnsUpToDateForCurrentRelease() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val updateManager = AppUpdateManager(context)

        val info = updateManager.checkForUpdates(forcedCheck = true)
        assertEquals(updateManager.currentVersionCode, info.currentVersionCode)
        assertFalse(info.isUpdateAvailable)

        val status = updateManager.updateStatus.value
        assertTrue(status is UpdateStatus.UpToDate)
        if (status is UpdateStatus.UpToDate) {
            assertEquals("1.1.0", status.versionName)
        }
    }

    @Test
    fun simulateUpgradeAvailable_transitionsToAvailableWithReleaseNotes() {
        val context = RuntimeEnvironment.getApplication()
        val updateManager = AppUpdateManager(context)

        updateManager.simulateUpdateAvailable(
            newVersionName = "1.2.0",
            newVersionCode = 3,
            notes = listOf("In-place update supported", "Preserved user data")
        )

        val status = updateManager.updateStatus.value
        assertTrue(status is UpdateStatus.Available)
        if (status is UpdateStatus.Available) {
            assertEquals("1.2.0", status.info.latestVersionName)
            assertEquals(3, status.info.latestVersionCode)
            assertTrue(status.info.isUpdateAvailable)
            assertEquals(2, status.info.releaseNotes.size)
        }

        updateManager.dismissUpdate()
        assertTrue(updateManager.updateStatus.value is UpdateStatus.Idle)
    }
}
