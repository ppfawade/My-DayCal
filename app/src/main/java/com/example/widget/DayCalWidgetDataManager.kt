package com.example.widget

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.LocalDaytileDatabase
import com.example.data.model.DayTask
import com.example.util.DateUtils
import org.json.JSONArray
import org.json.JSONObject

data class WidgetTaskData(
    val id: String,
    val dateKey: String,
    val dayBadge: String,
    val isToday: Boolean,
    val timeRange: String,
    val durationText: String,
    val categoryLabel: String,
    val categoryRaw: String,
    val title: String,
    val notes: String,
    val completed: Boolean,
    val isCurrentOrUpcoming: Boolean
)

object DayCalWidgetDataManager {
    private const val PREFS_NAME = "daycal_widget_prefs"
    private const val KEY_CACHED_TASKS_JSON = "cached_tasks_json"

    fun saveTasksCache(context: Context, tasks: Collection<DayTask>) {
        try {
            val jsonArray = JSONArray()
            tasks.forEach { task ->
                val obj = JSONObject().apply {
                    put("id", task.id)
                    put("userId", task.userId)
                    put("dateKey", task.dateKey)
                    put("title", task.title)
                    put("notes", task.notes)
                    put("startMinute", task.startMinute)
                    put("durationMinutes", task.durationMinutes)
                    put("category", task.category)
                    put("colorIndex", task.colorIndex)
                    put("completed", task.completed)
                }
                jsonArray.put(obj)
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CACHED_TASKS_JSON, jsonArray.toString()).apply()
        } catch (_: Exception) {
        }
    }

    fun getCachedTasks(context: Context): List<DayTask> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_CACHED_TASKS_JSON, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<DayTask>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DayTask(
                        id = obj.optString("id"),
                        userId = obj.optString("userId"),
                        dateKey = obj.optString("dateKey"),
                        title = obj.optString("title"),
                        notes = obj.optString("notes"),
                        startMinute = obj.optLong("startMinute", 540L),
                        durationMinutes = obj.optLong("durationMinutes", 60L),
                        category = obj.optString("category", "focus"),
                        colorIndex = obj.optLong("colorIndex", 0L),
                        completed = obj.optBoolean("completed", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Loads and prepares tasks for the 7-day week starting today (today through today + 6 days).
     * Merges Room SQLite tasks with cached Cloud Firestore tasks.
     */
    fun getWeekTasks(context: Context): List<WidgetTaskData> {
        val today = DateUtils.todayDateKey()
        val endOfWeek = DateUtils.addDays(today, 6)
        val currentMinute = DateUtils.currentMinuteOfDay()

        // 1. Gather Room SQLite local tasks
        val roomTasks = try {
            LocalDaytileDatabase.getInstance(context)
                .daytileDao()
                .getAllTasksSync()
                .map { it.toDomainModel() }
        } catch (_: Exception) {
            emptyList()
        }

        // 2. Gather Cached Cloud tasks
        val cachedTasks = getCachedTasks(context)

        // 3. Deduplicate by ID
        val combinedMap = mutableMapOf<String, DayTask>()
        roomTasks.forEach { combinedMap[it.id] = it }
        cachedTasks.forEach { combinedMap[it.id] = it }

        // 4. Filter for tasks within the 7-day window [today .. endOfWeek]
        val weekTasks = combinedMap.values.filter { task ->
            task.dateKey in today..endOfWeek
        }.sortedWith(
            compareBy<DayTask> { it.dateKey }
                .thenBy { it.startMinute }
        )

        // 5. Map to WidgetTaskData
        return weekTasks.map { task ->
            val isToday = task.dateKey == today
            val endMin = (task.startMinute + task.durationMinutes).toInt()
            val isCurrentOrUpcoming = isToday && (endMin >= currentMinute)

            val dayBadge = when (task.dateKey) {
                today -> "TODAY"
                DateUtils.addDays(today, 1) -> "TOMORROW"
                else -> {
                    val cal = DateUtils.parseDateKey(task.dateKey)
                    val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
                    val dowName = when (dow) {
                        java.util.Calendar.SUNDAY -> "SUN"
                        java.util.Calendar.MONDAY -> "MON"
                        java.util.Calendar.TUESDAY -> "TUE"
                        java.util.Calendar.WEDNESDAY -> "WED"
                        java.util.Calendar.THURSDAY -> "THU"
                        java.util.Calendar.FRIDAY -> "FRI"
                        else -> "SAT"
                    }
                    "$dowName, ${DateUtils.formatShortMonthDay(task.dateKey)}"
                }
            }

            val startTimeStr = DateUtils.formatMinutesToTime(task.startMinute.toInt())
            val endTimeStr = DateUtils.formatMinutesToTime(endMin)
            val timeRange = "$startTimeStr - $endTimeStr"
            val durationText = DateUtils.formatDuration(task.durationMinutes.toInt())

            val catLabel = task.category.uppercase()

            WidgetTaskData(
                id = task.id,
                dateKey = task.dateKey,
                dayBadge = dayBadge,
                isToday = isToday,
                timeRange = timeRange,
                durationText = durationText,
                categoryLabel = catLabel,
                categoryRaw = task.category.lowercase(),
                title = task.title,
                notes = task.notes,
                completed = task.completed,
                isCurrentOrUpcoming = isCurrentOrUpcoming
            )
        }
    }
}
