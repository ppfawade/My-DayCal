package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.TaskCategory
import com.example.util.DateUtils
import com.example.util.ReminderNotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `verify app name, short date format, categories, and notification channels`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("My DayCal", appName)

        // Verify MMM XX top bar format
        assertEquals("Nov 05", DateUtils.formatShortMonthDay("2026-11-05"))

        // Verify supported year range baked into date picker
        val yearRange = DateUtils.getSupportedYearRange()
        val currentYear = DateUtils.currentYear()
        assertEquals(currentYear - 10, yearRange.first)
        assertEquals(currentYear + 10, yearRange.last)

        // Verify exact 5 categories [Focus, Work, Personal, Health, Break]
        val labels = TaskCategory.entries.map { it.label }
        assertEquals(listOf("Focus", "Work", "Personal", "Health", "Break"), labels)

        // Verify notification channels are registered
        ReminderNotificationManager.createNotificationChannels(context)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val taskChannel =
            notificationManager.getNotificationChannel(ReminderNotificationManager.CHANNEL_TASK_REMINDERS)
        assertNotNull(taskChannel)
        assertTrue(taskChannel.importance >= NotificationManager.IMPORTANCE_HIGH)
    }
}
