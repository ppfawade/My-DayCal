package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.DayTask
import com.example.data.model.TaskCategory

object ReminderNotificationManager {
    const val CHANNEL_TASK_REMINDERS = "daycal_task_reminders"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_TASK_TIME = "extra_task_time"
    const val EXTRA_TASK_CATEGORY = "extra_task_category"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val taskChannel = NotificationChannel(
                CHANNEL_TASK_REMINDERS,
                "Task & Tile Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts before your scheduled timeline tiles begin"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(taskChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun scheduleTaskReminder(context: Context, task: DayTask) {
        val offsetMins = task.reminderMinutes.toInt()
        if (offsetMins < 0 || task.completed) {
            cancelTaskReminder(context, task.id)
            return
        }

        createNotificationChannels(context)

        val cal = DateUtils.parseDateKey(task.dateKey)
        val startHour = (task.startMinute.toInt() / 60).coerceIn(0, 23)
        val startMin = (task.startMinute.toInt() % 60).coerceIn(0, 59)
        cal.set(java.util.Calendar.HOUR_OF_DAY, startHour)
        cal.set(java.util.Calendar.MINUTE, startMin)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)

        val triggerAtMillis = cal.timeInMillis - (offsetMins * 60_000L)
        val now = System.currentTimeMillis()

        val timeRange = "${DateUtils.formatMinutesToTime(task.startMinute.toInt())} (${DateUtils.formatDuration(task.durationMinutes.toInt())})"
        val categoryLabel = TaskCategory.fromKey(task.category).label

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
            putExtra(EXTRA_TASK_TIME, timeRange)
            putExtra(EXTRA_TASK_CATEGORY, categoryLabel)
        }

        val requestCode = task.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (triggerAtMillis > now && alarmManager != null) {
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else if (triggerAtMillis in (now - 60_000L)..now) {
            showReminderNotification(
                context = context,
                taskId = task.id,
                title = task.title,
                timeLabel = timeRange,
                categoryLabel = categoryLabel
            )
        }
    }

    fun cancelTaskReminder(context: Context, taskId: String) {
        if (taskId.isBlank()) return
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun showReminderNotification(
        context: Context,
        taskId: String,
        title: String,
        timeLabel: String,
        categoryLabel: String
    ) {
        createNotificationChannels(context)
        if (!hasNotificationPermission(context)) return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TASK_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$categoryLabel • $title")
            .setContentText("Scheduled at $timeLabel")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
        } catch (ignored: SecurityException) {
        }
    }
}

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ReminderNotificationManager.EXTRA_TASK_ID) ?: return
        val title = intent.getStringExtra(ReminderNotificationManager.EXTRA_TASK_TITLE) ?: "Time Tile"
        val timeLabel = intent.getStringExtra(ReminderNotificationManager.EXTRA_TASK_TIME) ?: ""
        val categoryLabel = intent.getStringExtra(ReminderNotificationManager.EXTRA_TASK_CATEGORY) ?: "Focus"

        ReminderNotificationManager.showReminderNotification(
            context = context,
            taskId = taskId,
            title = title,
            timeLabel = timeLabel,
            categoryLabel = categoryLabel
        )
    }
}
