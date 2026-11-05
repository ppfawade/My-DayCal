package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DayTask
import com.example.data.model.TaskCategory
import com.example.ui.viewmodel.DaytileViewModel
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
    fun `verify app name, short date format, categories, overlap detection, and notification channels`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("My DayCal", appName)

        // Verify MMM XX date format
        assertEquals("Nov 05", DateUtils.formatShortMonthDay("2026-11-05"))

        // Verify supported year range baked into date picker
        val yearRange = DateUtils.getSupportedYearRange()
        val currentYear = DateUtils.currentYear()
        assertEquals(currentYear - 10, yearRange.first)
        assertEquals(currentYear + 10, yearRange.last)

        // Verify exact 5 categories [Focus, Work, Personal, Health, Break]
        val labels = TaskCategory.entries.map { it.label }
        assertEquals(listOf("Focus", "Work", "Personal", "Health", "Break"), labels)

        // Verify overlap detection between colliding time blocks
        val tasks = listOf(
            DayTask(id = "t1", startMinute = 540, durationMinutes = 90), // 09:00 - 10:30
            DayTask(id = "t2", startMinute = 600, durationMinutes = 60), // 10:00 - 11:00 (overlaps t1)
            DayTask(id = "t3", startMinute = 720, durationMinutes = 45)  // 12:00 - 12:45 (no overlap)
        )
        val overlaps = DaytileViewModel.computeOverlappingTaskIds(tasks)
        assertEquals(setOf("t1", "t2"), overlaps)

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
