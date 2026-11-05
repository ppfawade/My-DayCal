package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DayJournal
import com.example.data.model.DayTask
import com.example.data.model.ReminderOption
import com.example.data.model.TaskCategory
import com.example.data.model.endMinute
import com.example.ui.theme.getTilePalette
import com.example.ui.viewmodel.DaytileUiState
import com.example.util.DateUtils

@Composable
fun DayTimelineScreen(
    uiState: DaytileUiState,
    showFull24HourGrid: Boolean,
    onSwitchTo24h: () -> Unit,
    onSelectDate: (String) -> Unit,
    onOpenCreateTask: (presetHourMinute: Int) -> Unit,
    onEditTask: (DayTask) -> Unit,
    onToggleTask: (DayTask) -> Unit,
    onAdjustTaskSchedule: (DayTask, Int, Int) -> Unit,
    onDeleteTask: (String) -> Unit,
    onSaveJournal: (String, String, Int) -> Unit,
    onPopulateStarterDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDate = uiState.selectedDateKey
    val calendarWeek = remember(selectedDate) { DateUtils.getCalendarWeekNumber(selectedDate) }
    val workWeekDays = remember(selectedDate) { DateUtils.getWorkWeekDays(selectedDate) }
    var isJournalExpanded by rememberSaveable { mutableStateOf(true) }

    val tasks = uiState.selectedDayTasks
    val completedCount = tasks.count { it.completed }
    val totalMinutes = tasks.sumOf { it.durationMinutes.toInt() }
    val progress = if (tasks.isNotEmpty()) completedCount.toFloat() / tasks.size.toFloat() else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("day_timeline_list"),
        contentPadding = PaddingValues(bottom = 104.dp)
    ) {
        // 1. CW (Calendar Week) + Mon to Fri Workweek Bar & Completion Progress
        item(key = "day_header") {
            SingleDayHeaderSection(
                selectedDateKey = selectedDate,
                calendarWeek = calendarWeek,
                workWeekDays = workWeekDays,
                tasksByDate = uiState.tasksByDate,
                journalsByDate = uiState.journalsByDate,
                completedCount = completedCount,
                totalCount = tasks.size,
                totalMinutes = totalMinutes,
                progress = progress,
                onSelectDate = onSelectDate
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 2. Tasks or Empty Day Canvas
        if (tasks.isEmpty() && !showFull24HourGrid) {
            item(key = "empty_day_state") {
                EmptyDayTileCard(
                    onOpenCreateTask = { onOpenCreateTask(9 * 60) },
                    onPopulateStarterDay = onPopulateStarterDay,
                    onSwitchTo24h = onSwitchTo24h
                )
            }
        } else if (showFull24HourGrid) {
            items(
                items = (0..23).toList(),
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
                    isToday = selectedDate == uiState.todayDateKey,
                    tasksStartingInHour = tasksInHour,
                    onTapEmptySlot = { onOpenCreateTask(hour * 60) },
                    onToggleTask = onToggleTask,
                    onEditTask = onEditTask,
                    onAdjustTaskSchedule = onAdjustTaskSchedule,
                    onDeleteTask = onDeleteTask
                )
            }
        } else {
            items(
                items = tasks,
                key = { task -> task.id.ifBlank { "${task.startMinute}_${task.title}" } }
            ) { task ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    DayTaskTileCard(
                        task = task,
                        onToggleTask = { onToggleTask(task) },
                        onEditTask = { onEditTask(task) },
                        onAdjustTaskSchedule = { newStart, newDur ->
                            onAdjustTaskSchedule(task, newStart, newDur)
                        },
                        onDeleteTask = { onDeleteTask(task.id) }
                    )
                }
            }

            // Quick Hour Slot Strip at bottom of focused stream
            item(key = "quick_hour_slots") {
                QuickHourLauncherRow(
                    onPickHour = { hour -> onOpenCreateTask(hour * 60) }
                )
            }
        }

        // 3. Daily Journal & Reflection Card
        item(key = "daily_journal_card") {
            Spacer(modifier = Modifier.height(12.dp))
            DailyJournalSection(
                dateKey = selectedDate,
                existingJournal = uiState.selectedDayJournal,
                isExpanded = isJournalExpanded,
                onToggleExpand = { isJournalExpanded = !isJournalExpanded },
                onSaveJournal = onSaveJournal
            )
        }
    }
}

@Composable
private fun SingleDayHeaderSection(
    selectedDateKey: String,
    calendarWeek: Int,
    workWeekDays: List<com.example.util.DayChipInfo>,
    tasksByDate: Map<String, List<DayTask>>,
    journalsByDate: Map<String, DayJournal>,
    completedCount: Int,
    totalCount: Int,
    totalMinutes: Int,
    progress: Float,
    onSelectDate: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 14.dp)
        ) {
            // Fixed single-row bar: CW (Calendar Week) badge on left + Mon to Fri chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // CW (Calendar Week) indicator
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.testTag("calendar_week_badge")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CW",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%02d", calendarWeek),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(9.dp))
                    }
                }

                // Mon to Fri (5 workdays)
                workWeekDays.forEach { dayInfo ->
                    val isSelected = dayInfo.dateKey == selectedDateKey
                    val hasTasks = (tasksByDate[dayInfo.dateKey]?.isNotEmpty() == true)
                    val hasJournal = journalsByDate.containsKey(dayInfo.dateKey)

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            dayInfo.isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onSelectDate(dayInfo.dateKey) }
                            .testTag("day_ribbon_${dayInfo.dateKey}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayInfo.dayOfWeekShort.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dayInfo.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                !hasTasks -> Color.Transparent
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                )
                                if (hasJournal) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.secondary
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Daily summary bar
            if (totalCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$completedCount of $totalCount tiles completed",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${DateUtils.formatDuration(totalMinutes)} scheduled",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DayTaskTileCard(
    task: DayTask,
    onToggleTask: () -> Unit,
    onEditTask: () -> Unit,
    onAdjustTaskSchedule: (newStartMinute: Int, newDurationMinutes: Int) -> Unit,
    onDeleteTask: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val categoryObj = TaskCategory.fromKey(task.category)
    val palette = getTilePalette(categoryObj.fixedColorIndex)
    val containerColor = if (isDark) palette.containerDark else palette.containerLight
    val contentColor = if (isDark) palette.onContainerDark else palette.onContainerLight
    var isExpanded by rememberSaveable(task.id) { mutableStateOf(false) }

    val startStr = DateUtils.formatMinutesToTime(task.startMinute.toInt())
    val endStr = DateUtils.formatMinutesToTime(task.endMinute)
    val durationStr = DateUtils.formatDuration(task.durationMinutes.toInt())
    val reminderOpt = ReminderOption.fromMinutes(task.reminderMinutes.toInt())

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            } else {
                containerColor
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = if (task.completed) Color.Transparent else palette.accent.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium
            )
            .clip(MaterialTheme.shapes.medium)
            .clickable { isExpanded = !isExpanded }
            .testTag("task_tile_${task.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Interactive completion check circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (task.completed) palette.accent
                            else palette.accent.copy(alpha = 0.16f)
                        )
                        .border(2.dp, palette.accent, CircleShape)
                        .clickable { onToggleTask() }
                        .testTag("toggle_task_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.completed) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Mark incomplete",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$startStr – $endStr",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (task.completed) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                palette.accent
                            }
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = palette.accent.copy(alpha = 0.18f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(palette.accent)
                                )
                                Text(
                                    text = "$durationStr • ${categoryObj.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = contentColor
                                )
                            }
                        }
                        if (reminderOpt != ReminderOption.NONE) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Reminder: ${reminderOpt.label}",
                                tint = palette.accent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (task.completed) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            contentColor
                        },
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                    )

                    if (task.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.8f),
                            maxLines = if (isExpanded) 6 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onEditTask,
                    modifier = Modifier.testTag("edit_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit task tile",
                        tint = contentColor.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Tactile Daytile Quick-Resize & Time-Shift Bar when expanded
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = palette.accent.copy(alpha = 0.25f))
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

                        IconButton(
                            onClick = onDeleteTask,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("delete_task_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete task tile",
                                tint = MaterialTheme.colorScheme.error
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
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left Time Column
        Column(
            modifier = Modifier
                .width(58.dp)
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = hourLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isCurrentHour) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrentHour) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (isCurrentHour) {
                Text(
                    text = "NOW",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Timeline Slot Content
        Column(modifier = Modifier.weight(1f)) {
            if (tasksStartingInHour.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isCurrentHour) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onTapEmptySlot() }
                        .testTag("empty_hour_slot_$hour")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tap to block $hourLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add task at $hourLabel",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasksStartingInHour.forEach { task ->
                        DayTaskTileCard(
                            task = task,
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
}

@Composable
private fun QuickHourLauncherRow(
    onPickHour: (Int) -> Unit
) {
    val quickHours = listOf(7, 9, 11, 13, 15, 17, 19, 21)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
    ) {
        Text(
            text = "QUICK-ADD TIME SLOT",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickHours, key = { it }) { hour ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPickHour(hour) }
                        .testTag("quick_slot_$hour")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%02d:00", hour),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(id = R.string.empty_day_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No time tiles scheduled yet. Block your hours intentionally or switch to the 24h view.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenCreateTask,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("empty_state_add_tile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add First Tile")
                }

                OutlinedButton(
                    onClick = onPopulateStarterDay,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("populate_sample_day_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Starter Template")
                }
            }
            TextButton(
                onClick = onSwitchTo24h,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("Or view full 24-hour timeline slots")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyJournalSection(
    dateKey: String,
    existingJournal: DayJournal?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSaveJournal: (headline: String, content: String, moodScore: Int) -> Unit
) {
    var headline by remember(dateKey, existingJournal?.headline) {
        mutableStateOf(existingJournal?.headline ?: "")
    }
    var content by remember(dateKey, existingJournal?.content) {
        mutableStateOf(existingJournal?.content ?: "")
    }
    var moodScore by remember(dateKey, existingJournal?.moodScore) {
        mutableIntStateOf((existingJournal?.moodScore ?: 4L).toInt())
    }

    LaunchedEffect(dateKey, existingJournal) {
        headline = existingJournal?.headline ?: ""
        content = existingJournal?.content ?: ""
        moodScore = (existingJournal?.moodScore ?: 4L).toInt()
    }

    val moodLabels = listOf(
        1 to "1 • Drained",
        2 to "2 • Low",
        3 to "3 • Steady",
        4 to "4 • Focused",
        5 to "5 • Peak Flow"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(id = R.string.journal_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (existingJournal != null) "Reflection saved" else "Capture your intention & reflection",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle journal section",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        text = "ENERGY & FOCUS PULSE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        moodLabels.forEach { (score, label) ->
                            FilterChip(
                                selected = moodScore == score,
                                onClick = { moodScore = score },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.testTag("mood_chip_$score")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = headline,
                        onValueChange = { headline = it },
                        label = { Text(stringResource(id = R.string.journal_headline_hint)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("journal_headline_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text(stringResource(id = R.string.journal_content_hint)) },
                        minLines = 3,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("journal_content_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onSaveJournal(headline, content, moodScore) },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("save_journal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(id = R.string.btn_save_journal))
                    }
                }
            }
        }
    }
}
