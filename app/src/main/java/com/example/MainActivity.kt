package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.LocalDaytileDatabase
import com.example.data.model.DayTask
import com.example.data.model.ReminderOption
import com.example.data.model.TaskCategory
import com.example.data.repository.CloudPlannerDataSource
import com.example.data.repository.DaytileRepository
import com.example.data.repository.LocalPlannerDataSource
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.DayTimelineScreen
import com.example.ui.theme.DaytileTheme
import com.example.ui.theme.getTilePalette
import com.example.ui.viewmodel.DaytileViewModel
import com.example.util.DateUtils
import com.example.util.ReminderNotificationManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val PREFS_NAME = "my_daycal_prefs"
private const val KEY_LOCAL_MODE = "use_local_storage_mode"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderNotificationManager.createNotificationChannels(this)
        enableEdgeToEdge()
        setContent {
            DaytileTheme {
                DaytileRootApp()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun DaytileRootApp() {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    var isInitialized by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<FirebaseUser?>(null) }
    var isLocalStorageMode by remember {
        mutableStateOf(prefs.getBoolean(KEY_LOCAL_MODE, false))
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.android.awaitFrame()
        val auth = Firebase.auth
        currentUser = auth.currentUser
        isInitialized = true
        auth.authStateFlow().collect { user ->
            currentUser = user
            if (user != null) {
                isLocalStorageMode = false
                prefs.edit().putBoolean(KEY_LOCAL_MODE, false).apply()
            }
        }
    }

    val user = currentUser
    if (!isInitialized) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {}
    } else if (user != null) {
        DaytilePlannerShell(
            sessionKey = "cloud_${user.uid}",
            isCloudMode = true,
            currentUserId = user.uid,
            userLabel = user.email ?: "Signed in with Google",
            onExitCurrentMode = {}
        )
    } else if (isLocalStorageMode) {
        DaytilePlannerShell(
            sessionKey = "local_device_mode",
            isCloudMode = false,
            currentUserId = "local_user",
            userLabel = "Local Device Storage (Offline)",
            onExitCurrentMode = {
                prefs.edit().putBoolean(KEY_LOCAL_MODE, false).apply()
                isLocalStorageMode = false
            }
        )
    } else {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            },
            onContinueWithLocalStorage = {
                prefs.edit().putBoolean(KEY_LOCAL_MODE, true).apply()
                isLocalStorageMode = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaytilePlannerShell(
    sessionKey: String,
    isCloudMode: Boolean,
    currentUserId: String,
    userLabel: String,
    onExitCurrentMode: () -> Unit
) {
    val viewModel: DaytileViewModel = viewModel(
        key = sessionKey,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val dataSource = if (isCloudMode) {
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    CloudPlannerDataSource(DaytileRepository(db), currentUserId)
                } else {
                    val localDb = LocalDaytileDatabase.getInstance(app)
                    LocalPlannerDataSource(localDb.daytileDao())
                }
                DaytileViewModel(dataSource)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showFull24HourGrid by rememberSaveable { mutableStateOf(false) }
    var showDatePickerModal by rememberSaveable { mutableStateOf(false) }
    var showTaskEditorSheet by rememberSaveable { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<DayTask?>(null) }
    var presetDateForNewTask by rememberSaveable { mutableStateOf(uiState.selectedDateKey) }
    var presetStartMinute by rememberSaveable { mutableIntStateOf(9 * 60) }
    var showStorageInfoDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.statusBanner) {
        val msg = uiState.statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            DayCalTopBar(
                selectedDateKey = uiState.selectedDateKey,
                isCloudMode = isCloudMode,
                showFull24HourGrid = showFull24HourGrid,
                onOpenDatePicker = { showDatePickerModal = true },
                onToggle24hMode = { showFull24HourGrid = !showFull24HourGrid },
                onOpenStorageInfo = { showStorageInfoDialog = true },
                onCloudAction = {
                    if (isCloudMode) {
                        val credentialManager = CredentialManager.create(context)
                        signOutUser(
                            credentialManager = credentialManager,
                            onSignOutComplete = onExitCurrentMode,
                            scope = scope
                        )
                    } else {
                        onExitCurrentMode()
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingTask = null
                    presetDateForNewTask = uiState.selectedDateKey
                    presetStartMinute = 9 * 60
                    showTaskEditorSheet = true
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.btn_add_tile)
                    )
                },
                text = { Text(stringResource(id = R.string.btn_add_tile)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_task")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            DayTimelineScreen(
                uiState = uiState,
                showFull24HourGrid = showFull24HourGrid,
                onSwitchTo24h = { showFull24HourGrid = true },
                onSelectDate = { viewModel.selectDate(it) },
                onOpenCreateTask = { minute ->
                    editingTask = null
                    presetDateForNewTask = uiState.selectedDateKey
                    presetStartMinute = minute
                    showTaskEditorSheet = true
                },
                onEditTask = { task ->
                    editingTask = task
                    presetDateForNewTask = task.dateKey
                    presetStartMinute = task.startMinute.toInt()
                    showTaskEditorSheet = true
                },
                onToggleTask = { viewModel.toggleTaskCompleted(it) },
                onAdjustTaskSchedule = { task, newStart, newDur ->
                    viewModel.adjustTaskSchedule(task, newStart, newDur)
                },
                onDeleteTask = { taskId ->
                    ReminderNotificationManager.cancelTaskReminder(context, taskId)
                    viewModel.deleteTask(taskId)
                },
                onSaveJournal = { headline, content, mood ->
                    viewModel.saveJournalForDate(
                        dateKey = uiState.selectedDateKey,
                        headline = headline,
                        content = content,
                        moodScore = mood
                    )
                },
                onPopulateStarterDay = {
                    viewModel.populateSampleDay(uiState.selectedDateKey)
                }
            )
        }
    }

    if (showDatePickerModal) {
        CalendarDatePickerModal(
            initialDateKey = uiState.selectedDateKey,
            onDismiss = { showDatePickerModal = false },
            onDateSelected = { pickedDateKey ->
                viewModel.selectDate(pickedDateKey)
                showDatePickerModal = false
            }
        )
    }

    if (showTaskEditorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTaskEditorSheet = false }
        ) {
            TaskTileEditorSheetContent(
                initialTask = editingTask,
                defaultDateKey = presetDateForNewTask,
                defaultStartMinute = presetStartMinute,
                onDismiss = { showTaskEditorSheet = false },
                onSave = { title, notes, dateKey, startMin, durationMin, category, colorIdx, reminderMins ->
                    val existing = editingTask
                    if (existing != null) {
                        val updatedTask = existing.copy(
                            title = title,
                            notes = notes,
                            dateKey = dateKey,
                            startMinute = startMin.toLong(),
                            durationMinutes = durationMin.toLong(),
                            category = category,
                            colorIndex = colorIdx.toLong(),
                            reminderMinutes = reminderMins.toLong()
                        )
                        viewModel.updateTaskFull(
                            task = existing,
                            title = title,
                            notes = notes,
                            dateKey = dateKey,
                            startMinute = startMin,
                            durationMinutes = durationMin,
                            category = category,
                            colorIndex = colorIdx,
                            reminderMinutes = reminderMins
                        )
                        ReminderNotificationManager.scheduleTaskReminder(
                            context = context,
                            task = updatedTask
                        )
                    } else {
                        viewModel.addTask(
                            dateKey = dateKey,
                            title = title,
                            notes = notes,
                            startMinute = startMin,
                            durationMinutes = durationMin,
                            category = category,
                            colorIndex = colorIdx,
                            reminderMinutes = reminderMins,
                            onTaskCreated = { createdId ->
                                ReminderNotificationManager.scheduleTaskReminder(
                                    context = context,
                                    task = DayTask(
                                        id = createdId,
                                        userId = currentUserId,
                                        dateKey = dateKey,
                                        title = title,
                                        notes = notes,
                                        startMinute = startMin.toLong(),
                                        durationMinutes = durationMin.toLong(),
                                        category = category,
                                        colorIndex = colorIdx.toLong(),
                                        reminderMinutes = reminderMins.toLong()
                                    )
                                )
                            }
                        )
                    }
                    viewModel.selectDate(dateKey)
                    showTaskEditorSheet = false
                },
                onDelete = editingTask?.let { taskToDelete ->
                    {
                        ReminderNotificationManager.cancelTaskReminder(context, taskToDelete.id)
                        viewModel.deleteTask(taskToDelete.id)
                        showTaskEditorSheet = false
                    }
                }
            )
        }
    }

    if (showStorageInfoDialog) {
        val dbId = context.getString(R.string.firestore_database_id)
        AlertDialog(
            onDismissRequest = { showStorageInfoDialog = false },
            title = {
                Text(if (isCloudMode) "Google Cloud Sync Active" else "Local Device Storage Mode")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isCloudMode) {
                        Text(
                            text = "Your timeline tiles and reflections sync automatically in real time using free Google Firebase services.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Account: $userLabel",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Firestore Paths:\n• /users/$currentUserId/tasks\n• /users/$currentUserId/journals",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Database ID:\n$dbId",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "You are using My DayCal offline without an account. All timeline tiles and daily reflections are stored locally on this device in a Room SQLite database (my_daycal_local.db).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Want to sync across devices? Tap 'Sign in with Google' below at any time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            dismissButton = if (!isCloudMode) {
                {
                    TextButton(
                        onClick = {
                            showStorageInfoDialog = false
                            onExitCurrentMode()
                        }
                    ) {
                        Text("Sign in with Google")
                    }
                }
            } else null,
            confirmButton = {
                TextButton(onClick = { showStorageInfoDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun DayCalTopBar(
    selectedDateKey: String,
    isCloudMode: Boolean,
    showFull24HourGrid: Boolean,
    onOpenDatePicker: () -> Unit,
    onToggle24hMode: () -> Unit,
    onOpenStorageInfo: () -> Unit,
    onCloudAction: () -> Unit
) {
    val shortDateText = remember(selectedDateKey) {
        DateUtils.formatShortMonthDay(selectedDateKey)
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 12.dp)
        ) {
            // Left: App Title + Storage Mode Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCloudMode) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenStorageInfo() }
                        .testTag("storage_mode_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCloudMode) Icons.Default.CloudDone else Icons.Default.PhoneAndroid,
                            contentDescription = "Storage Mode Info",
                            tint = if (isCloudMode) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isCloudMode) "Sync" else "Local",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCloudMode) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Center: MMM XX Date Picker Trigger
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenDatePicker() }
                    .testTag("top_date_picker_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Open calendar date picker",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = shortDateText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Right: 24h / Focus Toggle + Cloud Action Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (showFull24HourGrid) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggle24hMode() }
                        .testTag("toggle_24h_grid_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (showFull24HourGrid) Icons.Default.ViewTimeline else Icons.Default.ViewStream,
                            contentDescription = "Toggle 24h and Focus view",
                            modifier = Modifier.size(15.dp),
                            tint = if (showFull24HourGrid) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = if (showFull24HourGrid) "24h" else "Focus",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (showFull24HourGrid) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                IconButton(
                    onClick = onCloudAction,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag(if (isCloudMode) "sign_out_button" else "switch_to_cloud_button")
                ) {
                    Icon(
                        imageVector = if (isCloudMode) Icons.AutoMirrored.Filled.Logout else Icons.Default.CloudQueue,
                        contentDescription = if (isCloudMode) stringResource(id = R.string.sign_out) else "Switch to Google Cloud Sync"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDatePickerModal(
    initialDateKey: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val initialMillis = remember(initialDateKey) {
        DateUtils.dateKeyToUtcMillis(initialDateKey)
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        yearRange = DateUtils.getSupportedYearRange()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        onDateSelected(DateUtils.todayDateKey())
                    }
                ) {
                    Text("Today")
                }
                Button(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            onDateSelected(DateUtils.utcMillisToDateKey(selectedMillis))
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier.testTag("confirm_date_picker_button")
                ) {
                    Text("Select")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun TaskTileEditorSheetContent(
    initialTask: DayTask?,
    defaultDateKey: String,
    defaultStartMinute: Int,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        notes: String,
        dateKey: String,
        startMinute: Int,
        durationMinutes: Int,
        category: String,
        colorIndex: Int,
        reminderMinutes: Int
    ) -> Unit,
    onDelete: (() -> Unit)?
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf(initialTask?.title ?: "") }
    var notes by rememberSaveable { mutableStateOf(initialTask?.notes ?: "") }
    var dateKey by rememberSaveable { mutableStateOf(initialTask?.dateKey ?: defaultDateKey) }
    var startHour by rememberSaveable {
        mutableIntStateOf(((initialTask?.startMinute?.toInt() ?: defaultStartMinute) / 60).coerceIn(0, 23))
    }
    var startMinutePart by rememberSaveable {
        mutableIntStateOf(((initialTask?.startMinute?.toInt() ?: defaultStartMinute) % 60 / 15) * 15)
    }
    var durationMinutes by rememberSaveable {
        mutableIntStateOf((initialTask?.durationMinutes?.toInt() ?: 60).coerceIn(15, 720))
    }
    var selectedCategory by rememberSaveable {
        mutableStateOf(TaskCategory.fromKey(initialTask?.category ?: TaskCategory.FOCUS.key).key)
    }
    var reminderMinutes by rememberSaveable {
        mutableIntStateOf(initialTask?.reminderMinutes?.toInt() ?: -1)
    }
    var showSheetDatePicker by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val durationPresets = listOf(15, 30, 45, 60, 90, 120, 180, 240)
    val minuteQuarters = listOf(0, 15, 30, 45)
    val quickStartHours = listOf(7, 9, 12, 15, 18, 21)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
            .padding(bottom = 24.dp)
    ) {
        // Header + Compact Horizontal Date Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (initialTask == null) "New Time Tile" else "Edit Time Tile",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showSheetDatePicker = true }
                    .testTag("sheet_date_picker_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Pick task date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = DateUtils.formatShortMonthDay(dateKey),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Date Quick Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val todayKey = DateUtils.todayDateKey()
                FilterChip(
                    selected = dateKey == todayKey,
                    onClick = { dateKey = todayKey },
                    label = { Text("Today") }
                )
            }
            item {
                val tomorrowKey = DateUtils.addDays(DateUtils.todayDateKey(), 1)
                FilterChip(
                    selected = dateKey == tomorrowKey,
                    onClick = { dateKey = tomorrowKey },
                    label = { Text("Tomorrow") }
                )
            }
            item {
                val nextWeekKey = DateUtils.addDays(DateUtils.todayDateKey(), 7)
                FilterChip(
                    selected = dateKey == nextWeekKey,
                    onClick = { dateKey = nextWeekKey },
                    label = { Text("+1 Week") }
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = { showSheetDatePicker = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Calendar…") }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title & Notes Inputs
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task or Activity Title") },
                placeholder = { Text("e.g., Deep Focus Session") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_notes_input")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Horizontal Scroll: Category & Fixed Color Single-Button Row [Focus, Work, Personal, Health, Break]
        Text(
            text = "CATEGORY",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(TaskCategory.entries, key = { it.key }) { cat ->
                val isSelected = selectedCategory == cat.key
                val palette = getTilePalette(cat.fixedColorIndex)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) {
                        palette.accent.copy(alpha = 0.22f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    },
                    modifier = Modifier
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) palette.accent else Color.Transparent,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { selectedCategory = cat.key }
                        .testTag("category_chip_${cat.key}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(palette.accent)
                        )
                        Text(
                            text = cat.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Horizontal Scroll: Start Time
        Text(
            text = "START TIME • ${String.format(java.util.Locale.US, "%02d:%02d", startHour, startMinutePart)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                OutlinedButton(
                    onClick = { startHour = (startHour - 1).mod(24) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("-1h")
                }
            }
            item {
                OutlinedButton(
                    onClick = { startHour = (startHour + 1).mod(24) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+1h")
                }
            }
            items(minuteQuarters, key = { "min_$it" }) { q ->
                FilterChip(
                    selected = startMinutePart == q,
                    onClick = { startMinutePart = q },
                    label = { Text(String.format(java.util.Locale.US, ":%02d", q)) }
                )
            }
            items(quickStartHours, key = { "hr_$it" }) { hr ->
                FilterChip(
                    selected = startHour == hr && startMinutePart == 0,
                    onClick = {
                        startHour = hr
                        startMinutePart = 0
                    },
                    label = { Text(String.format(java.util.Locale.US, "%02d:00", hr)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Horizontal Scroll: Duration
        Text(
            text = "DURATION • ${DateUtils.formatDuration(durationMinutes)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(durationPresets, key = { it }) { dur ->
                FilterChip(
                    selected = durationMinutes == dur,
                    onClick = { durationMinutes = dur },
                    label = { Text(DateUtils.formatDuration(dur)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Horizontal Scroll: Reminder
        val activeReminderLabel = ReminderOption.fromMinutes(reminderMinutes).label
        Text(
            text = "REMINDER • $activeReminderLabel",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ReminderOption.entries, key = { it.minutesBefore }) { option ->
                val isSelected = reminderMinutes == option.minutesBefore
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        reminderMinutes = option.minutesBefore
                        if (option != ReminderOption.NONE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!hasPerm) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (option == ReminderOption.NONE) {
                                Icons.Default.NotificationsNone
                            } else {
                                Icons.Default.NotificationsActive
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(option.label) },
                    modifier = Modifier.testTag("reminder_chip_${option.minutesBefore}")
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (onDelete != null) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            } else {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            }

            Button(
                onClick = {
                    val catObj = TaskCategory.fromKey(selectedCategory)
                    onSave(
                        title.ifBlank { "Focused Time Block" },
                        notes,
                        dateKey,
                        startHour * 60 + startMinutePart,
                        durationMinutes,
                        catObj.key,
                        catObj.fixedColorIndex,
                        reminderMinutes
                    )
                },
                modifier = Modifier
                    .weight(1.4f)
                    .testTag("save_task_sheet_button")
            ) {
                Text(if (initialTask == null) "Create Tile" else "Save Changes")
            }
        }
    }

    if (showSheetDatePicker) {
        CalendarDatePickerModal(
            initialDateKey = dateKey,
            onDismiss = { showSheetDatePicker = false },
            onDateSelected = { picked ->
                dateKey = picked
                showSheetDatePicker = false
            }
        )
    }
}
