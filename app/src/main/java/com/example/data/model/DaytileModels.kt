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
    val category: String = "focus", // focus, work, personal, health, routine, creative
    val colorIndex: Long = 0L, // 0..5
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

val DayTask.endMinute: Int
    get() = (startMinute.toInt() + durationMinutes.toInt()).coerceAtMost(1440)

@IgnoreExtraProperties
data class DayJournal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "", // Format: YYYY-MM-DD
    val headline: String = "",
    val content: String = "",
    val moodScore: Long = 3L, // 1..5
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class TaskCategory(
    val key: String,
    val label: String,
    val defaultColorIndex: Int
) {
    FOCUS("focus", "Deep Focus", 0),
    WORK("work", "Work", 1),
    PERSONAL("personal", "Personal", 2),
    HEALTH("health", "Health & Move", 3),
    ROUTINE("routine", "Daily Ritual", 4),
    CREATIVE("creative", "Creative", 5);

    companion object {
        fun fromKey(key: String): TaskCategory =
            entries.firstOrNull { it.key == key } ?: FOCUS
    }
}
