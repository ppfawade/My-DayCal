package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context and verify 20 year calendar horizon`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("My DayCal", appName)

        val horizon = DateUtils.getTwentyYearHorizon()
        val currentYear = DateUtils.currentYear()
        assertEquals(21, horizon.size)
        assertEquals(currentYear - 10, horizon.first())
        assertEquals(currentYear + 10, horizon.last())

        val grid = DateUtils.getMonthGridDays(2026, 10) // Nov 2026
        assertTrue(grid.contains("2026-11-05"))
    }
}
