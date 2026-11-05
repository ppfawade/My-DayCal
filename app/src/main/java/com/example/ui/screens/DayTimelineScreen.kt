package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DayJournal
import com.example.data.model.DayTask
import com.example.data.model.ReminderOption
import com.example.data.model.TaskCategory
import com.example.data.model.endMinute
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.CyberTealContainer
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SolarAmber
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
    val listState = rememberLazyListState()

    LaunchedEffect(showFull24HourGrid, selectedDate) {
        if (showFull24HourGrid) {
            val earliestTaskHour = tasks.minOfOrNull { (it.startMinute.toInt() / 60).coerceIn(0, 23) }
            val targetHour = if (earliestTaskHour != null) minOf(7, earliestTaskHour) else 7
            // Item 0 is day_header, so hour_slot_N is at index 1 + N
            listState.scrollToItem(index = 1 + targetHour)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("day_timeline_list"),
        contentPadding = PaddingValues(bottom = 112.dp)
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
                showProgressSummary = tasks.isNotEmpty(),
                onSelectDate = onSelectDate
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 2. 24-Hour Hourly Timeline Mode OR Focus Mode
        if (showFull24HourGrid) {
            // Consistent 24-Hour Timeline (00:00 .. 23:00) with events inline in their hour slots
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
        } else if (tasks.isEmpty()) {
            item(key = "empty_day_state") {
                EmptyDayTileCard(
                    onOpenCreateTask = { onOpenCreateTask(9 * 60) },
                    onPopulateStarterDay = onPopulateStarterDay,
                    onSwitchTo24h = onSwitchTo24h
                )
            }
        } else {
            items(
                items = tasks,
                key = { task -> task.id.ifBlank { "${task.startMinute}_${task.title}" } }
            ) { task ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
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
            Spacer(modifier = Modifier.height(10.dp))
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
    showProgressSummary: Boolean,
    onSelectDate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Rounded dark container enclosing CW + Mon..Fri
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF0E131E),
            border = BorderStroke(1.dp, Color(0xFF1D2638)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // CW (Calendar Week) emerald-bordered squircle
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CyberTealContainer,
                    border = BorderStroke(1.dp, CyberTeal.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .width(52.dp)
                        .height(64.dp)
                        .testTag("calendar_week_badge")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "CW",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberTeal,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%02d", calendarWeek),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberTeal,
                            fontSize = 18.sp
                        )
                    }
                }

                // Mon to Fri (5 workdays)
                workWeekDays.forEach { dayInfo ->
                    val isSelected = dayInfo.dateKey == selectedDateKey
                    val hasTasks = (tasksByDate[dayInfo.dateKey]?.isNotEmpty() == true)
                    val hasJournal = journalsByDate.containsKey(dayInfo.dateKey)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) SolarAmber else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectDate(dayInfo.dateKey) }
                            .testTag("day_ribbon_${dayInfo.dateKey}")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = dayInfo.dayOfWeekShort.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    Color(0xFF1A1102)
                                } else {
                                    Color(0xFF7E8B9F)
                                }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dayInfo.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = if (isSelected) {
                                    Color(0xFF140D00)
                                } else {
                                    Color.White
                                }
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> Color(0xFF140D00)
                                            hasTasks -> SolarAmber
                                            hasJournal -> CyberTeal
                                            else -> Color.Transparent
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Daily completion summary bar
        if (showProgressSummary) {
            Spacer(modifier = Modifier.height(14.dp))
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$completedCount of $totalCount tiles completed",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF8B99AF)
                    )
                    Text(
                        text = "${DateUtils.formatDuration(totalMinutes)} scheduled",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = SolarAmber
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A2232))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(SolarAmber)
                    )
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .align(Alignment.CenterEnd)
                            .clip(CircleShape)
                            .background(SolarAmber)
                    )
                }
            }
        }
    }
}

/**
 * Focus View Task Tile Card:
 * - Left column: Category & duration pill `[● 1h 30m • Focus]` + reminder bell + edit button on top,
 *   followed by bold Title and Notes below.
 * - Right column: Checkmark circle at top-right, and Start / End time (`08:30 - 10:00`) on the right side
 *   directly below the checkmark.
 */
@Composable
fun DayTaskTileCard(
    task: DayTask,
    onToggleTask: () -> Unit,
    onEditTask: () -> Unit,
    onAdjustTaskSchedule: (newStartMinute: Int, newDurationMinutes: Int) -> Unit,
    onDeleteTask: () -> Unit
) {
    val categoryObj = TaskCategory.fromKey(task.category)
    val palette = getTilePalette(categoryObj.fixedColorIndex)
    var isExpanded by rememberSaveable(task.id) { mutableStateOf(false) }

    val startStr = DateUtils.formatMinutesToTime(task.startMinute.toInt())
    val endStr = DateUtils.formatMinutesToTime(task.endMinute)
    val durationStr = DateUtils.formatDuration(task.durationMinutes.toInt())
    val reminderOpt = ReminderOption.fromMinutes(task.reminderMinutes.toInt())

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = palette.containerDark
        ),
        border = BorderStroke(1.2.dp, palette.borderDark),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { isExpanded = !isExpanded }
            .testTag("task_tile_${task.id}")
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left side: Category Pill + Bell + Edit Icon, Title, and Notes
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.accent.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.38f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(palette.accent)
                                )
                                Text(
                                    text = "$durationStr • ${categoryObj.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.onContainerDark
                                )
                            }
                        }

                        if (reminderOpt != ReminderOption.NONE) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Reminder: ${reminderOpt.label}",
                                tint = palette.accent,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        IconButton(
                            onClick = onEditTask,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("edit_task_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit task tile",
                                tint = palette.onContainerDark.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                    )

                    if (task.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.onContainerDark.copy(alpha = 0.78f),
                            maxLines = if (isExpanded) 6 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right side: Checkmark at top-right, Start/End time directly below Checkmark
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (task.completed) palette.accent
                                else Color.Transparent
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
                                tint = Color(0xFF140D00),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$startStr - $endStr",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (task.completed) {
                                Color(0xFF94A3B8)
                            } else {
                                palette.accent
                            }
                        )
                    }
                }
            }

            // Tactile Daytile Quick-Resize & Time-Shift Bar when expanded
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = palette.accent.copy(alpha = 0.22f))
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

/**
 * 24-Hour Inline Event Card (matching attached `image_b9952440.png` inside the hourly timeline)
 */
@Composable
private fun HourlyInlineTaskCard(
    task: DayTask,
    onToggleTask: () -> Unit,
    onEditTask: () -> Unit,
    onAdjustTaskSchedule: (newStartMinute: Int, newDurationMinutes: Int) -> Unit,
    onDeleteTask: () -> Unit
) {
    val categoryObj = TaskCategory.fromKey(task.category)
    val palette = getTilePalette(categoryObj.fixedColorIndex)
    var isExpanded by rememberSaveable(task.id) { mutableStateOf(false) }

    val startStr = DateUtils.formatMinutesToTime(task.startMinute.toInt())
    val endStr = DateUtils.formatMinutesToTime(task.endMinute)
    val durationStr = DateUtils.formatDuration(task.durationMinutes.toInt())
    val reminderOpt = ReminderOption.fromMinutes(task.reminderMinutes.toInt())

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = palette.containerDark
        ),
        border = BorderStroke(1.2.dp, palette.borderDark),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { isExpanded = !isExpanded }
            .testTag("task_tile_${task.id}")
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            // Top Row: Start - End Time on left, [● duration • Category] pill on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$startStr - $endStr",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (task.completed) Color(0xFF94A3B8) else palette.accent
                    )
                    if (reminderOpt != ReminderOption.NONE) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Reminder: ${reminderOpt.label}",
                            tint = palette.accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.accent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.38f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(palette.accent)
                        )
                        Text(
                            text = "$durationStr • ${categoryObj.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.onContainerDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Second Row: Checkmark circle on left + Title & Notes in middle + Edit icon on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (task.completed) palette.accent
                            else Color.Transparent
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
                            tint = Color(0xFF140D00),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (task.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.onContainerDark.copy(alpha = 0.78f),
                            maxLines = if (isExpanded) 6 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onEditTask,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("edit_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit task tile",
                        tint = palette.onContainerDark.copy(alpha = 0.7f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = palette.accent.copy(alpha = 0.22f))
                    Spacer(modifier = Modifier.height(8.dp))
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
                                .size(32.dp)
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
        color = Color(0xFF0E131E),
        border = BorderStroke(1.dp, Color(0xFF263248)),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFE2E8F0),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

/**
 * 24-Hour Axis Row matching `image_b9952440.png`:
 * - Left column: `07:00`, `08:00`, `10:00` + `NOW`
 * - Right column: `Tap to block HH:00 +` when empty, or `HourlyInlineTaskCard` when tasks exist in that hour.
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .width(52.dp)
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = hourLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isCurrentHour) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrentHour) SolarAmber else Color(0xFF64748B)
            )
            if (isCurrentHour) {
                Text(
                    text = "NOW",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SolarAmber,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (tasksStartingInHour.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCurrentHour) {
                        Color(0xFF1E1910)
                    } else {
                        Color(0xFF0E131E)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isCurrentHour) SolarAmber.copy(alpha = 0.5f) else Color(0xFF1A2232)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTapEmptySlot() }
                        .testTag("empty_hour_slot_$hour")
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add task at $hourLabel",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasksStartingInHour.forEach { task ->
                        HourlyInlineTaskCard(
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
            .padding(top = 14.dp, bottom = 4.dp)
    ) {
        Text(
            text = "QUICK-ADD TIME SLOT",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF7E8B9F),
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickHours, key = { it }) { hour ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0E1420),
                    border = BorderStroke(1.dp, Color(0xFF1F2A3D)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPickHour(hour) }
                        .testTag("quick_slot_$hour")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = SolarAmber
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%02d:00", hour),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFCBD5E1)
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF281B08))
                    .border(1.dp, Color(0xFF6B420B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = SolarAmber,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.empty_day_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No time tiles scheduled yet. Block your hours intentionally or switch to the 24h view.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onOpenCreateTask,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SolarAmber,
                        contentColor = Color(0xFF140D00)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("empty_state_add_tile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add First Tile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                OutlinedButton(
                    onClick = onPopulateStarterDay,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF2A364F)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF121826),
                        contentColor = SolarAmber
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("populate_sample_day_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = SolarAmber,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Starter Template",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SolarAmber,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Or view full 24-hour timeline slots",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFD97706),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSwitchTo24h() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

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

    val row1Moods = listOf(
        1 to "1 • Drained",
        2 to "2 • Low",
        3 to "3 • Steady"
    )
    val row2Moods = listOf(
        4 to "4 • Focused",
        5 to "5 • Peak Flow"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianBorder)
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
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CyberTealContainer,
                        border = BorderStroke(1.dp, CyberTeal.copy(alpha = 0.45f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = CyberTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(id = R.string.journal_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (existingJournal != null) "Reflection saved" else "Capture your intention & reflection",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle journal section",
                    tint = Color(0xFF94A3B8)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "ENERGY & FOCUS PULSE",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF7E8B9F),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 1: 1 • Drained, 2 • Low, 3 • Steady
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row1Moods.forEach { (score, label) ->
                            MoodPulseChip(
                                label = label,
                                isSelected = moodScore == score,
                                onClick = { moodScore = score },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mood_chip_$score")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: 4 • Focused, 5 • Peak Flow
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row2Moods.forEach { (score, label) ->
                            MoodPulseChip(
                                label = label,
                                isSelected = moodScore == score,
                                onClick = { moodScore = score },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mood_chip_$score")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = headline,
                        onValueChange = { headline = it },
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.journal_headline_hint),
                                color = Color(0xFF64748B)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0A0E17),
                            unfocusedContainerColor = Color(0xFF0A0E17),
                            focusedBorderColor = CyberTeal,
                            unfocusedBorderColor = Color(0xFF1F2A3C),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("journal_headline_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.journal_content_hint),
                                color = Color(0xFF64748B)
                            )
                        },
                        minLines = 3,
                        maxLines = 8,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0A0E17),
                            unfocusedContainerColor = Color(0xFF0A0E17),
                            focusedBorderColor = CyberTeal,
                            unfocusedBorderColor = Color(0xFF1F2A3C),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("journal_content_input")
                    )

                    if (headline.isNotBlank() || content.isNotBlank() || existingJournal != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onSaveJournal(headline, content, moodScore) },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SolarAmber,
                                contentColor = Color(0xFF140D00)
                            ),
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
                            Text(
                                text = stringResource(id = R.string.btn_save_journal),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodPulseChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) CyberTealContainer else Color(0xFF0B0F19),
        border = BorderStroke(
            width = if (isSelected) 1.2.dp else 1.dp,
            color = if (isSelected) CyberTeal else Color(0xFF1F2A3C)
        ),
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) CyberTeal else Color(0xFF94A3B8),
                maxLines = 1
            )
        }
    }
}
