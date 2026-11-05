package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DayTask
import com.example.data.model.ReminderOption
import com.example.data.model.TaskCategory
import com.example.data.model.endMinute
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.ObsidianCanvasLowest
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.getTilePalette
import com.example.ui.viewmodel.DaytileUiState
import com.example.util.DateUtils

private val SurfaceLow = Color(0xFF191B22)
private val SurfaceContainer = Color(0xFF1E1F26)
private val SurfaceContainerHigh = Color(0xFF282A30)
private val SurfaceContainerHighest = Color(0xFF33343B)
private val SurfaceLowest = Color(0xFF0C0E14)
private val PrimaryAmberLight = Color(0xFFFFC174)
private val PrimaryAmberContainer = Color(0xFFF59E0B)
private val OnPrimaryContainerDark = Color(0xFF2A1700)
private val WarmMuted = Color(0xFFD8C3AD)
private val WarmOutline = Color(0xFFA08E7A)
private val WarmOutlineVariant = Color(0xFF534434)

@Composable
fun DayTimelineScreen(
    uiState: DaytileUiState,
    showFull24HourGrid: Boolean,
    onSwitchTo24h: () -> Unit,
    onSelectDate: (String) -> Unit,
    onOpenDatePicker: () -> Unit,
    onOpenCreateTask: (presetHourMinute: Int) -> Unit,
    onEditTask: (DayTask) -> Unit,
    onToggleTask: (DayTask) -> Unit,
    onAdjustTaskSchedule: (DayTask, Int, Int) -> Unit,
    onDeleteTask: (String) -> Unit,
    onPopulateStarterDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDate = uiState.selectedDateKey
    val calendarWeek = remember(selectedDate) { DateUtils.getCalendarWeekNumber(selectedDate) }
    val workWeekDays = remember(selectedDate) { DateUtils.getWorkWeekDays(selectedDate) }

    val tasks = uiState.selectedDayTasks
    val completedCount = tasks.count { it.completed }
    val totalMinutes = tasks.sumOf { it.durationMinutes.toInt() }
    val progress = if (tasks.isNotEmpty()) completedCount.toFloat() / tasks.size.toFloat() else 0f
    val listState = rememberLazyListState()
    val isToday = selectedDate == uiState.todayDateKey
    val currentMinuteOfDay = remember(selectedDate, tasks) { DateUtils.currentMinuteOfDay() }

    // Precompute which hours are covered by a multi-hour task that started in an earlier hour
    val coveredHoursIn24h = remember(tasks) {
        val covered = mutableSetOf<Int>()
        tasks.forEach { task ->
            val startHour = (task.startMinute.toInt() / 60).coerceIn(0, 23)
            val endMin = task.endMinute
            for (h in (startHour + 1)..23) {
                val hStart = h * 60
                val hEnd = (h + 1) * 60
                val hasTaskStartingInH = tasks.any {
                    it.startMinute.toInt() in hStart until hEnd
                }
                // If an earlier task spans across at least half of hour h (or covers hour h completely)
                // and no new task starts in hour h, absorb hour h into the spanning card above
                if (!hasTaskStartingInH && endMin >= hStart + 30) {
                    covered.add(h)
                }
            }
        }
        covered
    }

    val visibleHoursIn24h = remember(coveredHoursIn24h) {
        (0..23).filter { it !in coveredHoursIn24h }
    }

    LaunchedEffect(showFull24HourGrid, selectedDate) {
        if (showFull24HourGrid) {
            val earliestTaskHour = tasks.minOfOrNull { (it.startMinute.toInt() / 60).coerceIn(0, 23) }
            val targetHour = if (earliestTaskHour != null) minOf(7, earliestTaskHour) else 7
            val targetIdx = visibleHoursIn24h.indexOfFirst { it >= targetHour }.coerceAtLeast(0)
            listState.scrollToItem(index = 1 + targetIdx)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("day_timeline_list"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // 1. CW (Calendar Week) + Week Scrubber & Minimalist Amber Progress Bar
        item(key = "day_header") {
            SingleDayHeaderSection(
                selectedDateKey = selectedDate,
                calendarWeek = calendarWeek,
                workWeekDays = workWeekDays,
                tasksByDate = uiState.tasksByDate,
                totalCount = tasks.size,
                totalMinutes = totalMinutes,
                progress = progress,
                onSelectDate = onSelectDate,
                onOpenDatePicker = onOpenDatePicker
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 2. 24-Hour Hourly Timeline Mode OR Fast Day Timeline Stream
        if (showFull24HourGrid) {
            items(
                items = visibleHoursIn24h,
                key = { hour -> "hour_slot_$hour" }
            ) { hour ->
                val hourStartMin = hour * 60
                val hourEndMin = (hour + 1) * 60
                val tasksInHour = tasks.filter {
                    val start = it.startMinute.toInt()
                    start in hourStartMin until hourEndMin
                }
                HourAxisRow(
                    hour = hour,
                    isToday = isToday,
                    tasksStartingInHour = tasksInHour,
                    onTapEmptySlot = { onOpenCreateTask(hour * 60) },
                    onToggleTask = onToggleTask,
                    onEditTask = onEditTask,
                    onAdjustTaskSchedule = onAdjustTaskSchedule,
                    onDeleteTask = onDeleteTask
                )
            }
        } else if (tasks.isEmpty()) {
            if (isToday) {
                item(key = "now_indicator_empty") {
                    NowTimelineIndicator(
                        currentMinute = currentMinuteOfDay,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            item(key = "empty_day_state") {
                EmptyDayTileCard(
                    onOpenCreateTask = { onOpenCreateTask(9 * 60) },
                    onPopulateStarterDay = onPopulateStarterDay,
                    onSwitchTo24h = onSwitchTo24h
                )
            }
        } else {
            val firstTaskStart = tasks.first().startMinute.toInt()
            if (isToday && currentMinuteOfDay < firstTaskStart) {
                item(key = "now_before_all") {
                    NowTimelineIndicator(
                        currentMinute = currentMinuteOfDay,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            tasks.forEachIndexed { index, task ->
                item(key = task.id.ifBlank { "${task.startMinute}_${task.title}_$index" }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        DayTaskTileCard(
                            task = task,
                            spanByDuration = false,
                            onToggleTask = { onToggleTask(task) },
                            onEditTask = { onEditTask(task) },
                            onAdjustTaskSchedule = { newStart, newDur ->
                                onAdjustTaskSchedule(task, newStart, newDur)
                            },
                            onDeleteTask = { onDeleteTask(task.id) }
                        )

                        val taskEnd = task.endMinute
                        val nextTask = tasks.getOrNull(index + 1)
                        val nextStart = nextTask?.startMinute?.toInt()

                        if (isToday) {
                            val inThisTask = currentMinuteOfDay in task.startMinute.toInt()..taskEnd
                            val inGapAfter = nextStart != null && currentMinuteOfDay in (taskEnd + 1) until nextStart
                            val afterLastTask = nextStart == null && currentMinuteOfDay > taskEnd
                            if (inThisTask || inGapAfter || afterLastTask) {
                                Spacer(modifier = Modifier.height(10.dp))
                                NowTimelineIndicator(currentMinute = currentMinuteOfDay)
                            }
                        }

                        if (nextStart != null && nextStart - taskEnd >= 30) {
                            val gapMinutes = nextStart - taskEnd
                            Spacer(modifier = Modifier.height(10.dp))
                            UnallocatedGapRow(
                                gapMinutes = gapMinutes,
                                isFirstGap = index == 0,
                                onScheduleGap = { onOpenCreateTask(taskEnd) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            item(key = "quick_hour_slots") {
                QuickHourLauncherRow(
                    onPickHour = { hour -> onOpenCreateTask(hour * 60) }
                )
            }
        }
    }
}

@Composable
private fun SingleDayHeaderSection(
    selectedDateKey: String,
    calendarWeek: Int,
    workWeekDays: List<com.example.util.DayChipInfo>,
    tasksByDate: Map<String, List<DayTask>>,
    totalCount: Int,
    totalMinutes: Int,
    progress: Float,
    onSelectDate: (String) -> Unit,
    onOpenDatePicker: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceLowest,
                border = BorderStroke(1.dp, SurfaceContainerHighest),
                modifier = Modifier
                    .width(46.dp)
                    .height(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenDatePicker() }
                    .testTag("calendar_week_badge")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "CW",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WarmOutline
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(java.util.Locale.US, "%02d", calendarWeek),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary,
                        fontSize = 15.sp
                    )
                }
            }

            workWeekDays.forEach { dayInfo ->
                val isSelected = dayInfo.dateKey == selectedDateKey
                val hasTasks = (tasksByDate[dayInfo.dateKey]?.isNotEmpty() == true)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) PrimaryAmberContainer else SurfaceLow,
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (isSelected) {
                                onOpenDatePicker()
                            } else {
                                onSelectDate(dayInfo.dateKey)
                            }
                        }
                        .testTag("day_ribbon_${dayInfo.dateKey}")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = dayInfo.dayOfWeekShort.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                OnPrimaryContainerDark
                            } else {
                                WarmMuted.copy(alpha = 0.65f)
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dayInfo.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = if (isSelected) {
                                OnPrimaryContainerDark
                            } else {
                                WarmMuted
                            }
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> OnPrimaryContainerDark
                                        hasTasks -> PrimaryAmberContainer
                                        else -> Color.Transparent
                                    }
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$totalCount ${if (totalCount == 1) "task" else "tasks"}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = PrimaryAmberLight
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(WarmOutlineVariant)
                        )
                        Text(
                            text = "${DateUtils.formatDuration(totalMinutes)} scheduled",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = WarmMuted
                        )
                    }
                    Text(
                        text = "${(progress * 100).toInt()}% DONE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        color = PrimaryAmberLight
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHighest),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (totalCount > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0.04f, 1f))
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(PrimaryAmberContainer)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NowTimelineIndicator(
    currentMinute: Int,
    modifier: Modifier = Modifier
) {
    val nowTimeStr = DateUtils.formatMinutesToTime(currentMinute)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .width(54.dp)
                .padding(end = 10.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = nowTimeStr,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = PrimaryAmberContainer
            )
            Text(
                text = "NOW",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = PrimaryAmberContainer
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(PrimaryAmberContainer)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(PrimaryAmberContainer.copy(alpha = 0.45f))
            )
        }
    }
}

@Composable
private fun UnallocatedGapRow(
    gapMinutes: Int,
    isFirstGap: Boolean,
    onScheduleGap: () -> Unit
) {
    val gapText = DateUtils.formatDuration(gapMinutes)
    val suffix = if (isFirstGap) "unallocated focus" else "open space"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(54.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceLowest.copy(alpha = 0.75f),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onScheduleGap() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$gapText $suffix",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.sp,
                    color = WarmMuted.copy(alpha = 0.85f)
                )
                Text(
                    text = "+ Schedule",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = PrimaryAmberLight
                )
            }
        }
    }
}

/**
 * Unified Task Row & Card used across BOTH Timeline Mode and 24h Mode:
 * - Left column (`54.dp` wide, right-aligned): Start time (`08:30`), spanned intermediate hour ticks when in 24h mode, and End time (`10:00`).
 * - Right column: `TaskEntryCardBody` (`#1E1F26`, `RoundedCornerShape(18.dp)`) with left-edge `4.dp` category bar,
 *   circular completion button, Title + Category pill (`FOCUS`, `WORK`, `PERSONAL`, `HEALTH`, `BREAK`) + notes,
 *   and crisp `32.dp` circular edit button.
 * - When `spanByDuration = true` (in 24h mode), the card height scales proportionally with `task.durationMinutes`.
 */
@Composable
fun DayTaskTileCard(
    task: DayTask,
    spanByDuration: Boolean = false,
    onToggleTask: () -> Unit,
    onEditTask: () -> Unit,
    onAdjustTaskSchedule: (newStartMinute: Int, newDurationMinutes: Int) -> Unit,
    onDeleteTask: () -> Unit
) {
    val categoryObj = TaskCategory.fromKey(task.category)
    val startStr = DateUtils.formatMinutesToTime(task.startMinute.toInt())
    val endStr = DateUtils.formatMinutesToTime(task.endMinute)
    val durationMins = task.durationMinutes.toInt().coerceAtLeast(15)

    // Proportional height when spanning in 24h mode (e.g., 30m -> 68dp, 45m -> 88dp, 60m -> 108dp, 90m -> 148dp, 120m -> 188dp)
    val minCardHeight: Dp = if (spanByDuration) {
        (28 + (durationMins * 4) / 3).dp.coerceIn(66.dp, 340.dp)
    } else {
        64.dp
    }

    // Intermediate hour labels when spanning multiple hours in 24h mode
    val intermediateHours = remember(task.startMinute, task.durationMinutes, spanByDuration) {
        if (!spanByDuration) {
            emptyList()
        } else {
            val startH = (task.startMinute.toInt() / 60) + 1
            val endH = (task.endMinute - 1) / 60
            if (endH >= startH) {
                (startH..endH).map { String.format(java.util.Locale.US, "%02d:00", it) }
            } else {
                emptyList()
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        // Left Time Column (`08:30` .. intermediate hours .. `10:00`)
        Column(
            modifier = Modifier
                .width(54.dp)
                .fillMaxHeight()
                .padding(top = 12.dp, bottom = 12.dp, end = 10.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = if (spanByDuration && durationMins >= 60) {
                Arrangement.SpaceBetween
            } else {
                Arrangement.Top
            }
        ) {
            Text(
                text = startStr,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (task.completed) {
                    SlateTextMuted
                } else if (categoryObj == TaskCategory.FOCUS) {
                    PrimaryAmberLight
                } else {
                    SlateTextPrimary
                }
            )

            intermediateHours.forEach { midHourLabel ->
                Text(
                    text = midHourLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = WarmMuted.copy(alpha = 0.40f)
                )
            }

            if (!spanByDuration || durationMins < 60) {
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = endStr,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                color = WarmMuted.copy(alpha = 0.6f)
            )
        }

        // Right Task Card (identical design in both Timeline & 24h views, spanning height in 24h view)
        TaskEntryCardBody(
            task = task,
            minCardHeight = minCardHeight,
            spanByDuration = spanByDuration,
            onToggleTask = onToggleTask,
            onEditTask = onEditTask,
            onAdjustTaskSchedule = onAdjustTaskSchedule,
            onDeleteTask = onDeleteTask,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TaskEntryCardBody(
    task: DayTask,
    minCardHeight: Dp,
    spanByDuration: Boolean,
    onToggleTask: () -> Unit,
    onEditTask: () -> Unit,
    onAdjustTaskSchedule: (newStartMinute: Int, newDurationMinutes: Int) -> Unit,
    onDeleteTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryObj = TaskCategory.fromKey(task.category)
    val palette = getTilePalette(categoryObj.fixedColorIndex)
    var isExpanded by rememberSaveable(task.id) { mutableStateOf(false) }
    val durationStr = DateUtils.formatDuration(task.durationMinutes.toInt())
    val reminderOpt = ReminderOption.fromMinutes(task.reminderMinutes.toInt())

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) SurfaceContainerHigh else SurfaceContainer
        ),
        modifier = modifier
            .heightIn(min = minCardHeight)
            .alpha(if (task.completed) 0.78f else 1.0f)
            .clip(RoundedCornerShape(18.dp))
            .clickable { isExpanded = !isExpanded }
            .testTag("task_tile_${task.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minCardHeight),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = spanByDuration)
                    .height(IntrinsicSize.Min)
                    .padding(end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = if (spanByDuration && task.durationMinutes >= 60) {
                    Alignment.Top
                } else {
                    Alignment.CenterVertically
                }
            ) {
                // Left-edge 4dp glowing category accent strip spanning the full card height
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(palette.accent)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Completion Circle Checkbox
                Box(
                    modifier = Modifier
                        .padding(top = if (spanByDuration && task.durationMinutes >= 60) 2.dp else 0.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (task.completed) CyberTeal else SurfaceContainerHighest
                        )
                        .border(
                            width = 1.dp,
                            color = if (task.completed) CyberTeal else WarmOutlineVariant.copy(alpha = 0.6f),
                            shape = CircleShape
                        )
                        .clickable { onToggleTask() }
                        .testTag("toggle_task_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.completed) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Mark incomplete",
                            tint = ObsidianCanvasLowest,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Center: Title + Category Pill + Notes
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = if (task.completed) SlateTextMuted else SlateTextPrimary,
                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Compact Uppercase Category Badge (`FOCUS`, `WORK`, `PERSONAL`, `HEALTH`, `BREAK`)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceContainerHighest
                        ) {
                            Text(
                                text = if (spanByDuration) {
                                    "${categoryObj.label.uppercase()} • $durationStr"
                                } else {
                                    categoryObj.label.uppercase()
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 9.sp,
                                color = palette.accent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (reminderOpt != ReminderOption.NONE) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Reminder: ${reminderOpt.label}",
                                tint = PrimaryAmberLight,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    if (task.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = if (task.completed) SlateTextMuted else WarmMuted,
                            maxLines = if (isExpanded || (spanByDuration && task.durationMinutes >= 90)) 4 else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right Circular Edit Button (exact 32dp circle, no 48dp IconButton overflow)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHighest.copy(alpha = 0.65f))
                        .clickable { onEditTask() }
                        .testTag("edit_task_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit task tile",
                        tint = WarmMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Expandable speed-action bar (Reschedule, Duration, Delete)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    HorizontalDivider(color = SurfaceContainerHighest)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            QuickTileActionChip(
                                label = "◀ -30m",
                                onClick = {
                                    onAdjustTaskSchedule(
                                        task.startMinute.toInt() - 30,
                                        task.durationMinutes.toInt()
                                    )
                                }
                            )
                            QuickTileActionChip(
                                label = "+30m ▶",
                                onClick = {
                                    onAdjustTaskSchedule(
                                        task.startMinute.toInt() + 30,
                                        task.durationMinutes.toInt()
                                    )
                                }
                            )
                            QuickTileActionChip(
                                label = "-15m len",
                                onClick = {
                                    onAdjustTaskSchedule(
                                        task.startMinute.toInt(),
                                        task.durationMinutes.toInt() - 15
                                    )
                                }
                            )
                            QuickTileActionChip(
                                label = "+15m len",
                                onClick = {
                                    onAdjustTaskSchedule(
                                        task.startMinute.toInt(),
                                        task.durationMinutes.toInt() + 15
                                    )
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SurfaceLowest)
                                .clickable { onDeleteTask() }
                                .testTag("delete_task_${task.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete task tile",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTileActionChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = SurfaceContainerHighest,
        modifier = Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = SlateTextPrimary
            )
        }
    }
}

/**
 * 24-Hour Axis Row:
 * - When empty: shows `HH:00` on the left and `Tap to block HH:00 + Schedule` on the right.
 * - When tasks start in this hour: renders `DayTaskTileCard(spanByDuration = true)` so the card looks
 *   identical to Timeline mode and spans vertically according to its duration.
 */
@Composable
private fun HourAxisRow(
    hour: Int,
    isToday: Boolean,
    tasksStartingInHour: List<DayTask>,
    onTapEmptySlot: () -> Unit,
    onToggleTask: (DayTask) -> Unit,
    onEditTask: (DayTask) -> Unit,
    onAdjustTaskSchedule: (DayTask, Int, Int) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    val currentMinute = DateUtils.currentMinuteOfDay()
    val isCurrentHour = isToday && (currentMinute / 60 == hour)
    val hourLabel = String.format(java.util.Locale.US, "%02d:00", hour)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
    ) {
        if (isCurrentHour) {
            NowTimelineIndicator(
                currentMinute = currentMinute,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        if (tasksStartingInHour.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .width(54.dp)
                        .padding(end = 10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = hourLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isCurrentHour) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = if (isCurrentHour) PrimaryAmberContainer else WarmMuted.copy(alpha = 0.7f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceLowest.copy(alpha = 0.65f),
                    border = BorderStroke(
                        1.dp,
                        if (isCurrentHour) PrimaryAmberContainer.copy(alpha = 0.35f) else SurfaceContainerHighest.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTapEmptySlot() }
                        .testTag("hour_slot_$hour")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tap to block $hourLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = WarmMuted.copy(alpha = 0.65f)
                        )
                        Text(
                            text = "+ Schedule",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryAmberLight.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tasksStartingInHour.forEach { task ->
                    DayTaskTileCard(
                        task = task,
                        spanByDuration = true,
                        onToggleTask = { onToggleTask(task) },
                        onEditTask = { onEditTask(task) },
                        onAdjustTaskSchedule = { newStart, newDur ->
                            onAdjustTaskSchedule(task, newStart, newDur)
                        },
                        onDeleteTask = { onDeleteTask(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickHourLauncherRow(
    onPickHour: (Int) -> Unit
) {
    val quickHours = listOf(7, 9, 11, 13, 15, 17, 19, 21)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Text(
            text = "QUICK-ADD TIME SLOT",
            style = MaterialTheme.typography.labelSmall,
            color = WarmOutline,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickHours, key = { it }) { hour ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SurfaceLow,
                    border = BorderStroke(1.dp, SurfaceContainerHighest),
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { onPickHour(hour) }
                        .testTag("quick_hour_chip_$hour")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryAmberLight
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%02d:00", hour),
                            style = MaterialTheme.typography.labelLarge,
                            color = SlateTextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDayTileCard(
    onOpenCreateTask: () -> Unit,
    onPopulateStarterDay: () -> Unit,
    onSwitchTo24h: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceContainer
        ),
        border = BorderStroke(1.dp, SurfaceContainerHighest),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = PrimaryAmberContainer.copy(alpha = 0.16f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = PrimaryAmberLight,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(id = R.string.empty_day_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = SlateTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(id = R.string.empty_day_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = WarmMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenCreateTask,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryAmberContainer,
                        contentColor = OnPrimaryContainerDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("empty_state_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.btn_add_tile),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "New Entry",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onPopulateStarterDay,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, SurfaceContainerHighest),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("populate_sample_day_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PrimaryAmberLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.btn_populate_starter),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = SlateTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onSwitchTo24h,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, SurfaceContainerHighest),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Open 24h Hourly Grid",
                    color = WarmMuted
                )
            }
        }
    }
}
