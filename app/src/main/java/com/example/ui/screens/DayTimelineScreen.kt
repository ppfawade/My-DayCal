package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.ui.theme.getTilePalette
import com.example.ui.viewmodel.DaytileUiState
import com.example.util.DateUtils
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun DayTimelineScreen(
    uiState: DaytileUiState,
    showFull24HourGrid: Boolean,
    onSwitchTo24h: () -> Unit,
    onSelectDate: (String) -> Unit,
    onStepWeek: (Int) -> Unit = {},
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
    val tasks = uiState.selectedDayTasks
    val overlappingTaskIds = uiState.overlappingTaskIds
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
        // 1. CW (Calendar Week) + Smooth Sequential All-Day Scrubber & Progress Bar
        item(key = "day_header") {
            SingleDayHeaderSection(
                selectedDateKey = selectedDate,
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
                    overlappingTaskIds = overlappingTaskIds,
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
                            hasOverlap = task.id in overlappingTaskIds,
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

/**
 * Smooth All-Day Sequential Week Scrubber:
 * - Includes all 7 days of the week sequentially (Sun, Mon, Tue, Wed, Thu, Fri, Sat) in a continuous
 *   snapping horizontal slider so Sunday is always visible in sequence when sliding.
 * - Calendar Week (`CW`) dynamically depends on the first visible date while sliding or the selected date,
 *   and immediately updates with a vertical slide animation when a Sunday (start of week) is selected or scrolled to.
 */
@Composable
private fun SingleDayHeaderSection(
    selectedDateKey: String,
    tasksByDate: Map<String, List<DayTask>>,
    totalCount: Int,
    totalMinutes: Int,
    progress: Float,
    onSelectDate: (String) -> Unit,
    onOpenDatePicker: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val visibleDayCount = 6

    // Recenter the 361-day sequential window only if selectedDateKey jumps outside the buffered range
    var windowCenterKey by rememberSaveable { mutableStateOf(selectedDateKey) }
    val sequentialDays = remember(windowCenterKey) {
        DateUtils.getSequentialDays(windowCenterKey, radiusDays = 180)
    }

    LaunchedEffect(selectedDateKey) {
        val idx = sequentialDays.indexOfFirst { it.dateKey == selectedDateKey }
        if (idx < 21 || idx > sequentialDays.size - 21) {
            windowCenterKey = selectedDateKey
        }
    }

    // Initial scroll position: start of the Sunday week containing selectedDateKey
    val initialScrollIndex = remember(windowCenterKey) {
        val sundayStartKey = DateUtils.getStartOfSundayWeek(selectedDateKey)
        val sundayIdx = sequentialDays.indexOfFirst { it.dateKey == sundayStartKey }
        val selIdx = sequentialDays.indexOfFirst { it.dateKey == selectedDateKey }
        when {
            sundayIdx >= 0 && selIdx >= 0 && (selIdx - sundayIdx) < visibleDayCount -> sundayIdx
            selIdx >= 0 -> (selIdx - 2).coerceAtLeast(0)
            else -> 180
        }
    }

    val dayStripState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = dayStripState)

    // Smoothly animate the strip when selectedDateKey changes (especially when Sunday is selected as new start of week)
    LaunchedEffect(selectedDateKey, sequentialDays) {
        val selectedIdx = sequentialDays.indexOfFirst { it.dateKey == selectedDateKey }
        if (selectedIdx < 0) return@LaunchedEffect

        val firstVis = dayStripState.firstVisibleItemIndex
        val lastVis = firstVis + visibleDayCount - 1

        if (DateUtils.isSunday(selectedDateKey)) {
            // When Sunday (new start of week) is selected, smoothly slide so Sunday is the first visible date
            if (firstVis != selectedIdx) {
                dayStripState.animateScrollToItem(selectedIdx)
            }
        } else if (selectedIdx < firstVis || selectedIdx > lastVis) {
            // If selected date is outside the current viewport (e.g., from DatePicker), smoothly slide to its Sunday week start
            val sundayStartKey = DateUtils.getStartOfSundayWeek(selectedDateKey)
            val sundayIdx = sequentialDays.indexOfFirst { it.dateKey == sundayStartKey }
            val targetIdx = if (sundayIdx >= 0 && (selectedIdx - sundayIdx) < visibleDayCount) {
                sundayIdx
            } else {
                selectedIdx
            }
            if (abs(targetIdx - firstVis) > 14) {
                dayStripState.scrollToItem((targetIdx - 6).coerceAtLeast(0))
            }
            dayStripState.animateScrollToItem(targetIdx)
        }
    }

    // Track first visible date key from the sequential strip
    val firstVisibleDateKey by remember(sequentialDays) {
        derivedStateOf {
            sequentialDays.getOrNull(dayStripState.firstVisibleItemIndex)?.dateKey ?: selectedDateKey
        }
    }

    val isSelectedDateVisible by remember(sequentialDays, selectedDateKey) {
        derivedStateOf {
            val selIdx = sequentialDays.indexOfFirst { it.dateKey == selectedDateKey }
            val firstVis = dayStripState.firstVisibleItemIndex
            selIdx in firstVis until (firstVis + visibleDayCount)
        }
    }

    // Calendar Week (CW) depends on the first visible date (when sliding or when selected date is scrolled out of view)
    // or the selected date (when visible / tapped)
    val displayedCalendarWeek by remember(selectedDateKey, firstVisibleDateKey, isSelectedDateVisible) {
        derivedStateOf {
            val referenceDateKey = if (dayStripState.isScrollInProgress || !isSelectedDateVisible) {
                firstVisibleDateKey
            } else {
                selectedDateKey
            }
            DateUtils.getCalendarWeekNumber(referenceDateKey)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (totalCount > 0) progress.coerceIn(0.04f, 1f) else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "progress_bar_anim"
    )

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
            // CW Badge with animated week transition
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surfaceContainerLowest,
                border = BorderStroke(1.dp, colors.surfaceContainerHighest),
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
                        color = colors.outline
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AnimatedContent(
                        targetState = displayedCalendarWeek,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInVertically { height -> height } + fadeIn(tween(220)))
                                    .togetherWith(slideOutVertically { height -> -height } + fadeOut(tween(180)))
                            } else {
                                (slideInVertically { height -> -height } + fadeIn(tween(220)))
                                    .togetherWith(slideOutVertically { height -> height } + fadeOut(tween(180)))
                            }
                        },
                        label = "cw_number_anim"
                    ) { weekNum ->
                        Text(
                            text = String.format(java.util.Locale.US, "%02d", weekNum),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Smooth All-Day Sequential Strip (including Sunday in natural chronological order)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
            ) {
                val spacing = 6.dp
                val chipWidth = ((maxWidth - spacing * (visibleDayCount - 1)) / visibleDayCount)
                    .coerceAtLeast(40.dp)

                LazyRow(
                    state = dayStripState,
                    flingBehavior = snapFlingBehavior,
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("sequential_day_ribbon")
                ) {
                    itemsIndexed(
                        items = sequentialDays,
                        key = { _, dayInfo -> dayInfo.dateKey }
                    ) { index, dayInfo ->
                        val isSelected = dayInfo.dateKey == selectedDateKey
                        val isSundayChip = dayInfo.dayOfWeekShort.equals("Sun", ignoreCase = true)
                        val hasTasks = (tasksByDate[dayInfo.dateKey]?.isNotEmpty() == true)

                        val chipBgColor by animateColorAsState(
                            targetValue = if (isSelected) colors.primaryContainer else colors.surfaceContainerLow,
                            animationSpec = tween(durationMillis = 220),
                            label = "chip_bg_anim"
                        )
                        val dayLabelColor by animateColorAsState(
                            targetValue = when {
                                isSelected -> colors.onPrimaryContainer
                                isSundayChip -> colors.primary
                                else -> colors.onSurfaceVariant.copy(alpha = 0.75f)
                            },
                            animationSpec = tween(durationMillis = 220),
                            label = "day_label_anim"
                        )
                        val dayNumberColor by animateColorAsState(
                            targetValue = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                            animationSpec = tween(durationMillis = 220),
                            label = "day_num_anim"
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = chipBgColor,
                            border = if (!isSelected && isSundayChip) {
                                BorderStroke(1.dp, colors.primaryContainer.copy(alpha = 0.28f))
                            } else {
                                null
                            },
                            modifier = Modifier
                                .width(chipWidth)
                                .height(58.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    if (isSelected) {
                                        onOpenDatePicker()
                                    } else {
                                        onSelectDate(dayInfo.dateKey)
                                        if (isSundayChip) {
                                            scope.launch {
                                                dayStripState.animateScrollToItem(index)
                                            }
                                        }
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
                                    fontWeight = if (isSelected || isSundayChip) FontWeight.Bold else FontWeight.Medium,
                                    color = dayLabelColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dayInfo.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = dayNumberColor
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> colors.onPrimaryContainer
                                                hasTasks -> colors.primaryContainer
                                                else -> Color.Transparent
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.surfaceContainerLow,
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
                            color = colors.primary
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(colors.outlineVariant)
                        )
                        Text(
                            text = "${DateUtils.formatDuration(totalMinutes)} scheduled",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${(progress * 100).toInt()}% DONE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        color = colors.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHighest),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (totalCount > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(colors.primaryContainer)
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
    val colors = MaterialTheme.colorScheme
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
                color = colors.primaryContainer
            )
            Text(
                text = "NOW",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = colors.primaryContainer
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
                    .background(colors.primaryContainer)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(colors.primaryContainer.copy(alpha = 0.45f))
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
    val colors = MaterialTheme.colorScheme
    val gapText = DateUtils.formatDuration(gapMinutes)
    val suffix = if (isFirstGap) "unallocated focus" else "open space"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(54.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colors.surfaceContainerLowest.copy(alpha = 0.85f),
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
                    color = colors.onSurfaceVariant.copy(alpha = 0.85f)
                )
                Text(
                    text = "+ Schedule",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = colors.primary
                )
            }
        }
    }
}

@Composable
fun DayTaskTileCard(
    task: DayTask,
    hasOverlap: Boolean = false,
    spanByDuration: Boolean = false,
    onToggleTask: () -> Unit,
    onEditTask: () -> Unit,
    onAdjustTaskSchedule: (newStartMinute: Int, newDurationMinutes: Int) -> Unit,
    onDeleteTask: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val categoryObj = TaskCategory.fromKey(task.category)
    val startStr = DateUtils.formatMinutesToTime(task.startMinute.toInt())
    val endStr = DateUtils.formatMinutesToTime(task.endMinute)
    val durationMins = task.durationMinutes.toInt().coerceAtLeast(15)

    val minCardHeight: Dp = if (spanByDuration) {
        (28 + (durationMins * 4) / 3).dp.coerceIn(66.dp, 340.dp)
    } else {
        64.dp
    }

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
                    colors.outline
                } else if (categoryObj == TaskCategory.FOCUS) {
                    colors.primary
                } else {
                    colors.onSurface
                }
            )

            intermediateHours.forEach { midHourLabel ->
                Text(
                    text = midHourLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = colors.onSurfaceVariant.copy(alpha = 0.45f)
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
                color = colors.onSurfaceVariant.copy(alpha = 0.65f)
            )
        }

        TaskEntryCardBody(
            task = task,
            hasOverlap = hasOverlap,
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
    hasOverlap: Boolean,
    minCardHeight: Dp,
    spanByDuration: Boolean,
    onToggleTask: () -> Unit,
    onEditTask: () -> Unit,
    onAdjustTaskSchedule: (newStartMinute: Int, newDurationMinutes: Int) -> Unit,
    onDeleteTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val categoryObj = TaskCategory.fromKey(task.category)
    val palette = getTilePalette(categoryObj.fixedColorIndex)
    var isExpanded by rememberSaveable(task.id) { mutableStateOf(false) }
    val durationStr = DateUtils.formatDuration(task.durationMinutes.toInt())
    val reminderOpt = ReminderOption.fromMinutes(task.reminderMinutes.toInt())

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) colors.surfaceContainerHigh else colors.surfaceContainer
        ),
        border = if (hasOverlap) {
            BorderStroke(1.dp, colors.error.copy(alpha = 0.55f))
        } else {
            BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.22f))
        },
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
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(palette.accent)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .padding(top = if (spanByDuration && task.durationMinutes >= 60) 2.dp else 0.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (task.completed) colors.tertiaryContainer else colors.surfaceContainerHighest
                        )
                        .border(
                            width = 1.dp,
                            color = if (task.completed) colors.tertiaryContainer else colors.outlineVariant.copy(alpha = 0.7f),
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
                            tint = colors.onTertiaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = if (task.completed) colors.outline else colors.onSurface,
                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.surfaceContainerHighest
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

                        if (hasOverlap) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.errorContainer,
                                modifier = Modifier.testTag("overlap_badge_${task.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = "Time overlap conflict",
                                        tint = colors.onErrorContainer,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "OVERLAP",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                        color = colors.onErrorContainer
                                    )
                                }
                            }
                        }

                        if (reminderOpt != ReminderOption.NONE) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Reminder: ${reminderOpt.label}",
                                tint = colors.primary,
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
                            color = if (task.completed) colors.outline else colors.onSurfaceVariant,
                            maxLines = if (isExpanded || (spanByDuration && task.durationMinutes >= 90)) 4 else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHighest.copy(alpha = 0.70f))
                        .clickable { onEditTask() }
                        .testTag("edit_task_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit task tile",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    HorizontalDivider(color = colors.surfaceContainerHighest)
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
                                .background(colors.surfaceContainerLowest)
                                .clickable { onDeleteTask() }
                                .testTag("delete_task_${task.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete task tile",
                                tint = colors.error,
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
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(50),
        color = colors.surfaceContainerHighest,
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
                color = colors.onSurface
            )
        }
    }
}

@Composable
private fun HourAxisRow(
    hour: Int,
    isToday: Boolean,
    tasksStartingInHour: List<DayTask>,
    overlappingTaskIds: Set<String>,
    onTapEmptySlot: () -> Unit,
    onToggleTask: (DayTask) -> Unit,
    onEditTask: (DayTask) -> Unit,
    onAdjustTaskSchedule: (DayTask, Int, Int) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
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
                        color = if (isCurrentHour) colors.primaryContainer else colors.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceContainerLowest.copy(alpha = 0.75f),
                    border = BorderStroke(
                        1.dp,
                        if (isCurrentHour) colors.primaryContainer.copy(alpha = 0.45f) else colors.surfaceContainerHighest.copy(alpha = 0.55f)
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
                            color = colors.onSurfaceVariant.copy(alpha = 0.70f)
                        )
                        Text(
                            text = "+ Schedule",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary
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
                        hasOverlap = task.id in overlappingTaskIds,
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
    val colors = MaterialTheme.colorScheme
    val quickHours = listOf(7, 9, 11, 13, 15, 17, 19, 21)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Text(
            text = "QUICK-ADD TIME SLOT",
            style = MaterialTheme.typography.labelSmall,
            color = colors.outline,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickHours, key = { it }) { hour ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.surfaceContainerLow,
                    border = BorderStroke(1.dp, colors.surfaceContainerHighest),
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
                            color = colors.primary
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%02d:00", hour),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurface
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
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceContainer
        ),
        border = BorderStroke(1.dp, colors.surfaceContainerHighest),
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
                color = colors.primaryContainer.copy(alpha = 0.16f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(id = R.string.empty_day_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(id = R.string.empty_day_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
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
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer
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
                    border = BorderStroke(1.dp, colors.surfaceContainerHighest),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("populate_sample_day_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.btn_populate_starter),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onSwitchTo24h,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, colors.surfaceContainerHighest),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Open 24h Hourly Grid",
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}
