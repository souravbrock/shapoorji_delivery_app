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
}
