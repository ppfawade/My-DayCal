package com.example.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.MainActivity
import com.example.R

class DayCalWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return DayCalWidgetViewsFactory(applicationContext)
    }
}

class DayCalWidgetViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var taskList: List<WidgetTaskData> = emptyList()

    override fun onCreate() {
        // Initial setup
    }

    override fun onDataSetChanged() {
        // Called on a background binder thread when notifyAppWidgetViewDataChanged is triggered
        taskList = DayCalWidgetDataManager.getWeekTasks(context)
    }

    override fun onDestroy() {
        taskList = emptyList()
    }

    override fun getCount(): Int = taskList.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position !in taskList.indices) return null
        val item = taskList[position]

        val views = RemoteViews(context.packageName, R.layout.widget_task_item)

        // 1. Date Badge
        views.setTextViewText(R.id.widget_item_date_badge, item.dayBadge)
        if (item.isToday) {
            views.setInt(R.id.widget_item_date_badge, "setBackgroundResource", R.drawable.widget_today_badge_bg)
            views.setTextColor(R.id.widget_item_date_badge, android.graphics.Color.parseColor("#111319"))
        } else {
            views.setInt(R.id.widget_item_date_badge, "setBackgroundResource", R.drawable.widget_badge_bg)
            views.setTextColor(R.id.widget_item_date_badge, android.graphics.Color.parseColor("#F59E0B"))
        }

        // 2. Time & Duration
        views.setTextViewText(R.id.widget_item_time, item.timeRange)
        views.setTextViewText(R.id.widget_item_duration, item.durationText)

        // 3. Category Tag & Strip
        views.setTextViewText(R.id.widget_item_category, item.categoryLabel)
        val (stripRes, catTextColor) = when (item.categoryRaw) {
            "work" -> Pair(R.drawable.widget_strip_work, "#00A6E0")
            "personal" -> Pair(R.drawable.widget_strip_personal, "#30C88F")
            "health" -> Pair(R.drawable.widget_strip_health, "#FF5252")
            "break" -> Pair(R.drawable.widget_strip_break, "#AB47BC")
            else -> Pair(R.drawable.widget_strip_focus, "#FFC174")
        }
        views.setImageViewResource(R.id.widget_item_strip, stripRes)
        views.setTextColor(R.id.widget_item_category, android.graphics.Color.parseColor(catTextColor))

        // 4. Title & Strikethrough / Completion
        views.setTextViewText(R.id.widget_item_title, item.title)
        if (item.completed) {
            views.setViewVisibility(R.id.widget_item_check, View.VISIBLE)
            views.setTextColor(R.id.widget_item_title, android.graphics.Color.parseColor("#787C87"))
        } else {
            views.setViewVisibility(R.id.widget_item_check, View.GONE)
            views.setTextColor(R.id.widget_item_title, android.graphics.Color.parseColor("#E2E2EB"))
        }

        // 5. Notes
        if (item.notes.isNotBlank()) {
            views.setTextViewText(R.id.widget_item_notes, item.notes)
            views.setViewVisibility(R.id.widget_item_notes, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_item_notes, View.GONE)
        }

        // 6. Click fillInIntent (opens MainActivity directly to this date and task)
        val fillInIntent = Intent().apply {
            putExtra(MainActivity.EXTRA_DATE_KEY, item.dateKey)
            putExtra(MainActivity.EXTRA_TASK_ID, item.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
