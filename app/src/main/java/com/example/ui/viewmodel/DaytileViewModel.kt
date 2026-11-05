package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DayJournal
import com.example.data.model.DayTask
import com.example.data.model.TaskCategory
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

data class DaytileUiState(
    val selectedDateKey: String = DateUtils.todayDateKey(),
    val todayDateKey: String = DateUtils.todayDateKey(),
    val selectedDayTasks: List<DayTask> = emptyList(),
    val selectedDayJournal: DayJournal? = null,
    val tasksByDate: Map<String, List<DayTask>> = emptyMap(),
    val journalsByDate: Map<String, DayJournal> = emptyMap(),
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
        _statusBanner
    ) { selectedDate, tasksState, journalsState, banner ->
        val today = DateUtils.todayDateKey()
        val allTasks = (tasksState as? DataSyncState.Success)?.data ?: emptyList()
        val allJournals = (journalsState as? DataSyncState.Success)?.data ?: emptyList()

        val tasksByDate = allTasks
            .groupBy { it.dateKey }
            .mapValues { (_, list) ->
                list.sortedWith(compareBy<DayTask> { it.startMinute }.thenBy { it.title })
            }

        val journalsByDate = allJournals.associateBy { it.dateKey }

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
    }

    fun stepDay(offsetDays: Int) {
        val nextKey = DateUtils.addDays(_selectedDateKey.value, offsetDays)
        selectDate(nextKey)
    }

    fun jumpToToday() {
        selectDate(DateUtils.todayDateKey())
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
        colorIndex: Int,
        reminderMinutes: Int = -1,
        onTaskCreated: ((String) -> Unit)? = null
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
                completed = false,
                reminderMinutes = reminderMinutes
            )
            result.onSuccess { taskId ->
                onTaskCreated?.invoke(taskId)
            }.onFailure { e ->
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
        colorIndex: Int,
        reminderMinutes: Int = -1
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
                    colorIndex = colorIndex.coerceIn(0, 5).toLong(),
                    reminderMinutes = reminderMinutes.coerceIn(-1, 1440).toLong()
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
                    "Daily reflection saved"
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
                category = TaskCategory.FOCUS.key,
                colorIndex = TaskCategory.FOCUS.fixedColorIndex,
                reminderMinutes = 10
            )
            dataSource.createTask(
                dateKey = dateKey,
                title = "Midday Walk & Mobility",
                notes = "Sunlight exposure and 20-minute brisk walk outside.",
                startMinute = 12 * 60 + 30,
                durationMinutes = 45,
                category = TaskCategory.HEALTH.key,
                colorIndex = TaskCategory.HEALTH.fixedColorIndex,
                reminderMinutes = 5
            )
            dataSource.createTask(
                dateKey = dateKey,
                title = "Afternoon Recharge & Tea",
                notes = "Step away from screens and reset attention.",
                startMinute = 16 * 60,
                durationMinutes = 30,
                category = TaskCategory.BREAK.key,
                colorIndex = TaskCategory.BREAK.fixedColorIndex,
                reminderMinutes = 0
            )
            _statusBanner.value = "Added 3 sample timeline tiles"
        }
    }

    private companion object {
        const val TAG = "DaytileViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
