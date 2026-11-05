package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DayJournal
import com.example.data.model.DayTask
import com.example.data.repository.CloudPlannerDataSource
import com.example.data.repository.DaytileRepository
import com.example.data.repository.PlannerDataSource
import com.example.util.DateUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DataSyncState<out T> {
    data object Loading : DataSyncState<Nothing>
    data class Success<T>(val data: T) : DataSyncState<T>
    data class Error(val message: String) : DataSyncState<Nothing>
}

data class ArchiveDaySummary(
    val dateKey: String,
    val tasks: List<DayTask>,
    val journal: DayJournal?,
    val completedCount: Int,
    val totalMinutes: Int,
    val isPast: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean
)

data class DaytileUiState(
    val selectedDateKey: String = DateUtils.todayDateKey(),
    val todayDateKey: String = DateUtils.todayDateKey(),
    val selectedDayTasks: List<DayTask> = emptyList(),
    val selectedDayJournal: DayJournal? = null,
    val tasksByDate: Map<String, List<DayTask>> = emptyMap(),
    val journalsByDate: Map<String, DayJournal> = emptyMap(),
    val archiveDays: List<ArchiveDaySummary> = emptyList(),
    val calendarYear: Int = DateUtils.currentYear(),
    val calendarMonthZeroIndexed: Int = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
    val isCloudBacked: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val statusBanner: String? = null
)

class DaytileViewModel(
    private val dataSource: PlannerDataSource
) : ViewModel() {

    constructor(
        repository: DaytileRepository,
        currentUserId: String
    ) : this(CloudPlannerDataSource(repository, currentUserId))

    private val _selectedDateKey = MutableStateFlow(DateUtils.todayDateKey())
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    private val _calendarYear = MutableStateFlow(DateUtils.currentYear())
    private val _calendarMonth = MutableStateFlow(
        java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
    )
    private val _statusBanner = MutableStateFlow<String?>(null)

    private val tasksFlow: StateFlow<DataSyncState<List<DayTask>>> =
        dataSource.observeTasks()
            .map<List<DayTask>, DataSyncState<List<DayTask>>> { DataSyncState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing tasks", rootError)
                emit(DataSyncState.Error(rootError.message ?: "Failed to load tasks"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DataSyncState.Loading
            )

    private val journalsFlow: StateFlow<DataSyncState<List<DayJournal>>> =
        dataSource.observeJournals()
            .map<List<DayJournal>, DataSyncState<List<DayJournal>>> { DataSyncState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing journals", rootError)
                emit(DataSyncState.Error(rootError.message ?: "Failed to load journals"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DataSyncState.Loading
            )

    val uiState: StateFlow<DaytileUiState> = combine(
        _selectedDateKey,
        tasksFlow,
        journalsFlow,
        _calendarYear,
        combine(_calendarMonth, _statusBanner) { m, b -> m to b }
    ) { selectedDate, tasksState, journalsState, calYear, (calMonth, banner) ->
        val today = DateUtils.todayDateKey()
        val allTasks = (tasksState as? DataSyncState.Success)?.data ?: emptyList()
        val allJournals = (journalsState as? DataSyncState.Success)?.data ?: emptyList()

        val tasksByDate = allTasks
            .groupBy { it.dateKey }
            .mapValues { (_, list) -> list.sortedWith(compareBy<DayTask> { it.startMinute }.thenBy { it.title }) }

        val journalsByDate = allJournals.associateBy { it.dateKey }

        val allLoggedDates = (tasksByDate.keys + journalsByDate.keys)
            .filter { it.isNotBlank() }
            .distinct()
            .sortedDescending()

        val archiveSummaries = allLoggedDates.map { dateKey ->
            val dayTasks = tasksByDate[dateKey] ?: emptyList()
            val dayJournal = journalsByDate[dateKey]
            ArchiveDaySummary(
                dateKey = dateKey,
                tasks = dayTasks,
                journal = dayJournal,
                completedCount = dayTasks.count { it.completed },
                totalMinutes = dayTasks.sumOf { it.durationMinutes.toInt() },
                isPast = dateKey < today,
                isToday = dateKey == today,
                isFuture = dateKey > today
            )
        }

        val errorMsg = (tasksState as? DataSyncState.Error)?.message
            ?: (journalsState as? DataSyncState.Error)?.message

        val loading = tasksState is DataSyncState.Loading || journalsState is DataSyncState.Loading

        DaytileUiState(
            selectedDateKey = selectedDate,
            todayDateKey = today,
            selectedDayTasks = tasksByDate[selectedDate] ?: emptyList(),
            selectedDayJournal = journalsByDate[selectedDate],
            tasksByDate = tasksByDate,
            journalsByDate = journalsByDate,
            archiveDays = archiveSummaries,
            calendarYear = calYear,
            calendarMonthZeroIndexed = calMonth,
            isCloudBacked = dataSource.isCloudBacked,
            isLoading = loading,
            errorMessage = errorMsg,
            statusBanner = banner
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DaytileUiState(isCloudBacked = dataSource.isCloudBacked)
    )

    fun selectDate(dateKey: String) {
        _selectedDateKey.value = dateKey
        val cal = DateUtils.parseDateKey(dateKey)
        _calendarYear.value = cal.get(java.util.Calendar.YEAR)
        _calendarMonth.value = cal.get(java.util.Calendar.MONTH)
    }

    fun stepDay(offsetDays: Int) {
        val nextKey = DateUtils.addDays(_selectedDateKey.value, offsetDays)
        selectDate(nextKey)
    }

    fun jumpToToday() {
        selectDate(DateUtils.todayDateKey())
    }

    fun setCalendarYearMonth(year: Int, monthZeroIndexed: Int) {
        val horizon = DateUtils.getTwentyYearHorizon()
        val clampedYear = year.coerceIn(horizon.first(), horizon.last())
        val clampedMonth = monthZeroIndexed.coerceIn(0, 11)
        _calendarYear.value = clampedYear
        _calendarMonth.value = clampedMonth
    }

    fun stepCalendarMonth(deltaMonths: Int) {
        val cal = java.util.Calendar.getInstance()
        cal.set(_calendarYear.value, _calendarMonth.value, 1)
        cal.add(java.util.Calendar.MONTH, deltaMonths)
        setCalendarYearMonth(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH)
        )
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun addTask(
        dateKey: String = _selectedDateKey.value,
        title: String,
        notes: String,
        startMinute: Int,
        durationMinutes: Int,
        category: String,
        colorIndex: Int
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val result = dataSource.createTask(
                dateKey = dateKey,
                title = title,
                notes = notes,
                startMinute = startMinute,
                durationMinutes = durationMinutes,
                category = category,
                colorIndex = colorIndex,
                completed = false
            )
            result.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Could not create task"
            }
        }
    }

    fun toggleTaskCompleted(task: DayTask) {
        viewModelScope.launch {
            dataSource.updateTask(task.copy(completed = !task.completed))
                .onFailure { e ->
                    _statusBanner.value = e.localizedMessage ?: "Failed to update task"
                }
        }
    }

    fun adjustTaskSchedule(task: DayTask, newStartMinute: Int, newDurationMinutes: Int) {
        val clampedStart = newStartMinute.coerceIn(0, 1425)
        val clampedDuration = newDurationMinutes.coerceIn(15, 720)
        viewModelScope.launch {
            dataSource.updateTask(
                task.copy(
                    startMinute = clampedStart.toLong(),
                    durationMinutes = clampedDuration.toLong()
                )
            ).onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Failed to reschedule tile"
            }
        }
    }

    fun updateTaskFull(
        task: DayTask,
        title: String,
        notes: String,
        dateKey: String,
        startMinute: Int,
        durationMinutes: Int,
        category: String,
        colorIndex: Int
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            dataSource.updateTask(
                task.copy(
                    title = title,
                    notes = notes,
                    dateKey = dateKey,
                    startMinute = startMinute.coerceIn(0, 1439).toLong(),
                    durationMinutes = durationMinutes.coerceIn(15, 720).toLong(),
                    category = category,
                    colorIndex = colorIndex.coerceIn(0, 5).toLong()
                )
            ).onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Failed to update task"
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            dataSource.deleteTask(taskId)
                .onFailure { e ->
                    _statusBanner.value = e.localizedMessage ?: "Failed to delete task"
                }
        }
    }

    fun saveJournalForDate(
        dateKey: String = _selectedDateKey.value,
        headline: String,
        content: String,
        moodScore: Int
    ) {
        val existing = uiState.value.journalsByDate[dateKey]
        viewModelScope.launch {
            val result = dataSource.saveJournal(
                dateKey = dateKey,
                headline = headline,
                content = content,
                moodScore = moodScore,
                existingCreatedAt = existing?.createdAt
            )
            result.onSuccess {
                _statusBanner.value = if (dataSource.isCloudBacked) {
                    "Daily reflection synced to cloud"
                } else {
                    "Daily reflection saved to device"
                }
            }.onFailure { e ->
                _statusBanner.value = e.localizedMessage ?: "Failed to save reflection"
            }
        }
    }

    fun populateSampleDay(dateKey: String = _selectedDateKey.value) {
        viewModelScope.launch {
            dataSource.createTask(
                dateKey = dateKey,
                title = "Morning Deep Work & Planning",
                notes = "Prioritize top 3 outcomes for the day with zero notifications.",
                startMinute = 8 * 60 + 30,
                durationMinutes = 90,
                category = "focus",
                colorIndex = 0
            )
            dataSource.createTask(
                dateKey = dateKey,
                title = "Midday Walk & Mobility",
                notes = "Sunlight exposure and 20-minute brisk walk outside.",
                startMinute = 12 * 60 + 30,
                durationMinutes = 45,
                category = "health",
                colorIndex = 3
            )
            dataSource.createTask(
                dateKey = dateKey,
                title = "Evening Review & Next-Day Prep",
                notes = "Log daily reflection and queue tomorrow's first tile.",
                startMinute = 17 * 60,
                durationMinutes = 60,
                category = "routine",
                colorIndex = 4
            )
            _statusBanner.value = "Added 3 sample timeline tiles for $dateKey"
        }
    }

    private companion object {
        const val TAG = "DaytileViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
