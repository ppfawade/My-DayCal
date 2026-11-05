package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class DayTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "", // Format: YYYY-MM-DD
    val title: String = "",
    val notes: String = "",
    val startMinute: Long = 540L, // 0..1439 (e.g., 540 = 09:00)
    val durationMinutes: Long = 60L, // 15..720
    val category: String = "focus", // focus, work, personal, health, break
    val colorIndex: Long = 0L, // 0..4 fixed per category
    val reminderMinutes: Long = -1L, // -1 = None, 0 = At start, 5, 10, 15, 30, 60
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

val DayTask.endMinute: Int
    get() = (startMinute.toInt() + durationMinutes.toInt()).coerceAtMost(1440)

enum class TaskCategory(
    val key: String,
    val label: String,
    val fixedColorIndex: Int
) {
    FOCUS("focus", "Focus", 0),
    WORK("work", "Work", 1),
    PERSONAL("personal", "Personal", 2),
    HEALTH("health", "Health", 3),
    BREAK("break", "Break", 4);

    companion object {
        fun fromKey(key: String): TaskCategory =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: FOCUS
    }
}

enum class ReminderOption(
    val minutesBefore: Int,
    val label: String
) {
    NONE(-1, "None"),
    AT_START(0, "At start"),
    MIN_5(5, "5m before"),
    MIN_10(10, "10m before"),
    MIN_15(15, "15m before"),
    MIN_30(30, "30m before"),
    HOUR_1(60, "1h before");

    companion object {
        fun fromMinutes(minutes: Int): ReminderOption =
            entries.firstOrNull { it.minutesBefore == minutes } ?: NONE
    }
}
