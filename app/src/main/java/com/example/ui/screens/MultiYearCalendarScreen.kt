package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DayJournal
import com.example.data.model.DayTask
import com.example.ui.viewmodel.DaytileUiState
import com.example.util.DateUtils

@Composable
fun MultiYearCalendarScreen(
    uiState: DaytileUiState,
    onSetYearMonth: (year: Int, monthZeroIndexed: Int) -> Unit,
    onStepMonth: (Int) -> Unit,
    onSelectDateAndOpenDay: (String) -> Unit,
    onScheduleTaskForDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val twentyYearHorizon = remember { DateUtils.getTwentyYearHorizon() }
    val minYear = twentyYearHorizon.first()
    val maxYear = twentyYearHorizon.last()
    val currentYear = uiState.calendarYear
    val currentMonth = uiState.calendarMonthZeroIndexed

    var showTwelveMonthMatrix by rememberSaveable { mutableStateOf(false) }
    var inspectedDateKey by rememberSaveable { mutableStateOf(uiState.selectedDateKey) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("multi_year_calendar_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Multi-Year Header & 20-Year Selector Ribbon (10 years past to 10 years future)
        item(key = "calendar_header") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.calendar_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Spanning $minYear – $maxYear • Plan ahead or revisit any year",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showTwelveMonthMatrix = !showTwelveMonthMatrix }
                            .testTag("toggle_year_matrix_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (showTwelveMonthMatrix) Icons.Default.CalendarViewMonth else Icons.Default.GridView,
                                contentDescription = "Toggle 12-month overview",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showTwelveMonthMatrix) "Month Detail" else "12-Month Grid",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Decade / Year Jump Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    YearJumpButton(
                        label = "$minYear (-10Y)",
                        onClick = { onSetYearMonth(minYear, currentMonth) },
                        modifier = Modifier.weight(1f)
                    )
                    YearJumpButton(
                        label = "-1 Year",
                        onClick = { onSetYearMonth(currentYear - 1, currentMonth) },
                        modifier = Modifier.weight(1f)
                    )
                    YearJumpButton(
                        label = "This Year",
                        onClick = {
                            val nowCal = java.util.Calendar.getInstance()
                            onSetYearMonth(
                                nowCal.get(java.util.Calendar.YEAR),
                                nowCal.get(java.util.Calendar.MONTH)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    YearJumpButton(
                        label = "+1 Year",
                        onClick = { onSetYearMonth(currentYear + 1, currentMonth) },
                        modifier = Modifier.weight(1f)
                    )
                    YearJumpButton(
                        label = "$maxYear (+10Y)",
                        onClick = { onSetYearMonth(maxYear, currentMonth) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable 21-Year Ribbon (e.g., 2016..2036)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(twentyYearHorizon, key = { it }) { yr ->
                        val isSelectedYear = yr == currentYear
                        val isCurrentActualYear = yr == DateUtils.currentYear()
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isSelectedYear -> MaterialTheme.colorScheme.primary
                                isCurrentActualYear -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            },
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSetYearMonth(yr, currentMonth) }
                                .testTag("year_chip_$yr")
                        ) {
                            Text(
                                text = yr.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelectedYear) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    isSelectedYear -> MaterialTheme.colorScheme.onPrimary
                                    isCurrentActualYear -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showTwelveMonthMatrix) {
            // 12-Month Full Year Overview Matrix (2 columns x 6 rows)
            item(key = "twelve_month_matrix") {
                TwelveMonthYearOverview(
                    year = currentYear,
                    todayDateKey = uiState.todayDateKey,
                    tasksByDate = uiState.tasksByDate,
                    journalsByDate = uiState.journalsByDate,
                    onSelectMonth = { m ->
                        onSetYearMonth(currentYear, m)
                        showTwelveMonthMatrix = false
                    },
                    onSelectDay = { dateKey ->
                        inspectedDateKey = dateKey
                    }
                )
            }
        } else {
            // Month Selector Ribbon + Detailed Interactive Month Grid
            item(key = "month_detail_card") {
                MonthDetailCalendarCard(
                    year = currentYear,
                    monthZeroIndexed = currentMonth,
                    todayDateKey = uiState.todayDateKey,
                    inspectedDateKey = inspectedDateKey,
                    tasksByDate = uiState.tasksByDate,
                    journalsByDate = uiState.journalsByDate,
                    onSelectMonth = { m -> onSetYearMonth(currentYear, m) },
                    onStepMonth = onStepMonth,
                    onTapDate = { dateKey -> inspectedDateKey = dateKey }
                )
            }
        }

        // Inspected Date Preview & Future/Past Task Scheduler Card
        item(key = "inspected_date_preview") {
            InspectedDateActionCard(
                dateKey = inspectedDateKey,
                todayDateKey = uiState.todayDateKey,
                tasks = uiState.tasksByDate[inspectedDateKey] ?: emptyList(),
                journal = uiState.journalsByDate[inspectedDateKey],
                onOpenDayView = { onSelectDateAndOpenDay(inspectedDateKey) },
                onScheduleTask = { onScheduleTaskForDate(inspectedDateKey) }
            )
        }

        item { Spacer(modifier = Modifier.height(76.dp)) }
    }
}

@Composable
private fun YearJumpButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        )
    }
}

@Composable
private fun MonthDetailCalendarCard(
    year: Int,
    monthZeroIndexed: Int,
    todayDateKey: String,
    inspectedDateKey: String,
    tasksByDate: Map<String, List<DayTask>>,
    journalsByDate: Map<String, DayJournal>,
    onSelectMonth: (Int) -> Unit,
    onStepMonth: (Int) -> Unit,
    onTapDate: (String) -> Unit
) {
    val gridCells = remember(year, monthZeroIndexed) {
        DateUtils.getMonthGridDays(year, monthZeroIndexed)
    }
    val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 12 Month quick pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items((0..11).toList(), key = { it }) { m ->
                    val selected = m == monthZeroIndexed
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectMonth(m) }
                            .testTag("month_chip_$m")
                    ) {
                        Text(
                            text = DateUtils.monthNameShort(m),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Month & Year Header with Prev/Next arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onStepMonth(-1) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous month"
                    )
                }

                Text(
                    text = "${DateUtils.monthName(monthZeroIndexed)} $year",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = { onStepMonth(1) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next month"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day of Week Headers
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { d ->
                    Text(
                        text = d,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Calendar Grid Rows
            val rows = gridCells.chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 0 until 7) {
                            val cellDateKey = week.getOrNull(i)
                            if (cellDateKey == null) {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            } else {
                                val dayNum = cellDateKey.takeLast(2).toIntOrNull() ?: 1
                                val isSelected = cellDateKey == inspectedDateKey
                                val isToday = cellDateKey == todayDateKey
                                val dayTasks = tasksByDate[cellDateKey] ?: emptyList()
                                val hasJournal = journalsByDate.containsKey(cellDateKey)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                                dayTasks.isNotEmpty() || hasJournal ->
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                            color = if (isToday && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onTapDate(cellDateKey) }
                                        .testTag("calendar_cell_$cellDateKey"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (dayTasks.isNotEmpty() || hasJournal) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                if (dayTasks.isNotEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                                else MaterialTheme.colorScheme.primary
                                                            )
                                                    )
                                                }
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TwelveMonthYearOverview(
    year: Int,
    todayDateKey: String,
    tasksByDate: Map<String, List<DayTask>>,
    journalsByDate: Map<String, DayJournal>,
    onSelectMonth: (Int) -> Unit,
    onSelectDay: (String) -> Unit
) {
    val monthPairs = (0..11).chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        monthPairs.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { monthIdx ->
                    MiniMonthCard(
                        year = year,
                        monthZeroIndexed = monthIdx,
                        todayDateKey = todayDateKey,
                        tasksByDate = tasksByDate,
                        journalsByDate = journalsByDate,
                        onSelectMonth = { onSelectMonth(monthIdx) },
                        onSelectDay = onSelectDay,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniMonthCard(
    year: Int,
    monthZeroIndexed: Int,
    todayDateKey: String,
    tasksByDate: Map<String, List<DayTask>>,
    journalsByDate: Map<String, DayJournal>,
    onSelectMonth: () -> Unit,
    onSelectDay: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cells = remember(year, monthZeroIndexed) {
        DateUtils.getMonthGridDays(year, monthZeroIndexed)
    }
    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable { onSelectMonth() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "${DateUtils.monthNameShort(monthZeroIndexed)} $year",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            val weeks = cells.chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                weeks.forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (i in 0 until 7) {
                            val dateKey = week.getOrNull(i)
                            if (dateKey == null) {
                                Spacer(modifier = Modifier.weight(1f).height(18.dp))
                            } else {
                                val dayNum = dateKey.takeLast(2).toIntOrNull() ?: 1
                                val hasData = tasksByDate[dateKey]?.isNotEmpty() == true ||
                                    journalsByDate.containsKey(dateKey)
                                val isToday = dateKey == todayDateKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(18.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isToday -> MaterialTheme.colorScheme.primary
                                                hasData -> MaterialTheme.colorScheme.primaryContainer
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onSelectDay(dateKey) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = when {
                                            isToday -> MaterialTheme.colorScheme.onPrimary
                                            hasData -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectedDateActionCard(
    dateKey: String,
    todayDateKey: String,
    tasks: List<DayTask>,
    journal: DayJournal?,
    onOpenDayView: () -> Unit,
    onScheduleTask: () -> Unit
) {
    val fullDate = DateUtils.formatFullDateHeader(dateKey)
    val relLabel = DateUtils.formatRelativeLabel(dateKey)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SELECTED HORIZON DAY • $relLabel",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = fullDate,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = when {
                    tasks.isEmpty() && journal == null ->
                        "No tasks or reflections logged yet for $dateKey. You can schedule future tasks or log archive entries for any date."
                    else ->
                        "${tasks.size} scheduled tile(s) • ${if (journal != null) "Journal saved" else "No journal yet"}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    tasks.take(4).forEach { task ->
                        Text(
                            text = "• ${DateUtils.formatMinutesToTime(task.startMinute.toInt())} — ${task.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onScheduleTask,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calendar_schedule_task_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Plan Task Here")
                }

                OutlinedButton(
                    onClick = onOpenDayView,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calendar_open_day_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Day View")
                }
            }
        }
    }
}
