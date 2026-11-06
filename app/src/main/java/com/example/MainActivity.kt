package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.launch

private const val PREFS_NAME = "my_daycal_prefs"
private const val KEY_LOCAL_MODE = "use_local_storage_mode"
private const val KEY_DARK_THEME = "use_dark_theme_mode"

class MainActivity : ComponentActivity() {
    private val widgetDateKey = mutableStateOf<String?>(null)
    private val widgetOpenAdd = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderNotificationManager.createNotificationChannels(this)
        handleWidgetIntent(intent)
        enableEdgeToEdge()
        setContent {
            DaytileRootApp(
                initialDateKey = widgetDateKey.value,
                initialOpenAdd = widgetOpenAdd.value,
                onConsumedWidgetIntent = {
                    widgetDateKey.value = null
                    widgetOpenAdd.value = false
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        intent?.getStringExtra(EXTRA_DATE_KEY)?.let {
            widgetDateKey.value = it
        }
        if (intent?.getBooleanExtra(EXTRA_OPEN_ADD, false) == true) {
            widgetOpenAdd.value = true
        }
    }

    companion object {
        const val EXTRA_DATE_KEY = "com.example.extra.DATE_KEY"
        const val EXTRA_OPEN_ADD = "com.example.extra.OPEN_ADD"
        const val EXTRA_TASK_ID = "com.example.extra.TASK_ID"
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
fun DaytileRootApp(
    initialDateKey: String? = null,
    initialOpenAdd: Boolean = false,
    onConsumedWidgetIntent: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    var isInitialized by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<FirebaseUser?>(null) }
    var isLocalStorageMode by remember {
        mutableStateOf(prefs.getBoolean(KEY_LOCAL_MODE, false))
    }
    var isDarkTheme by rememberSaveable {
        mutableStateOf(prefs.getBoolean(KEY_DARK_THEME, true))
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.android.awaitFrame()
        val auth = runCatching { Firebase.auth }.getOrNull()
        if (auth != null) {
            currentUser = auth.currentUser
            isInitialized = true
            auth.authStateFlow().collect { user ->
                currentUser = user
                if (user != null) {
                    isLocalStorageMode = false
                    prefs.edit().putBoolean(KEY_LOCAL_MODE, false).apply()
                }
            }
        } else {
            isInitialized = true
        }
    }

    val onToggleTheme = {
        val nextDark = !isDarkTheme
        isDarkTheme = nextDark
        prefs.edit().putBoolean(KEY_DARK_THEME, nextDark).apply()
    }

    DaytileTheme(darkTheme = isDarkTheme) {
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
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                currentUserId = user.uid,
                userLabel = user.email ?: "Signed in with Google",
                initialDateKey = initialDateKey,
                initialOpenAdd = initialOpenAdd,
                onConsumedWidgetIntent = onConsumedWidgetIntent,
                onExitCurrentMode = {}
            )
        } else if (isLocalStorageMode) {
            DaytilePlannerShell(
                sessionKey = "local_device_mode",
                isCloudMode = false,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                currentUserId = "local_user",
                userLabel = "Local Device Storage (Offline)",
                initialDateKey = initialDateKey,
                initialOpenAdd = initialOpenAdd,
                onConsumedWidgetIntent = onConsumedWidgetIntent,
                onExitCurrentMode = {
                    prefs.edit().putBoolean(KEY_LOCAL_MODE, false).apply()
                    isLocalStorageMode = false
                }
            )
        } else {
            AuthScreen(
                onAuthSuccess = {
                    currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
                },
                onContinueWithLocalStorage = {
                    prefs.edit().putBoolean(KEY_LOCAL_MODE, true).apply()
                    isLocalStorageMode = true
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaytilePlannerShell(
    sessionKey: String,
    isCloudMode: Boolean,
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    currentUserId: String,
    userLabel: String,
    initialDateKey: String? = null,
    initialOpenAdd: Boolean = false,
    onConsumedWidgetIntent: () -> Unit = {},
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
    val colors = MaterialTheme.colorScheme

    var showFull24HourGrid by rememberSaveable { mutableStateOf(false) }
    var showDatePickerModal by rememberSaveable { mutableStateOf(false) }
    var showTaskEditorSheet by rememberSaveable { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<DayTask?>(null) }
    var presetDateForNewTask by rememberSaveable { mutableStateOf(uiState.selectedDateKey) }
    var presetStartMinute by rememberSaveable { mutableIntStateOf(10 * 60) }
    var showStorageInfoDialog by rememberSaveable { mutableStateOf(false) }

    val handleDeleteWithUndo: (String) -> Unit = { taskId ->
        val taskTitle = uiState.selectedDayTasks.firstOrNull { it.id == taskId }?.title ?: "Time block"
        ReminderNotificationManager.cancelTaskReminder(context, taskId)
        viewModel.deleteTask(taskId)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Deleted \"$taskTitle\"",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastDelete { restoredTask ->
                    ReminderNotificationManager.scheduleTaskReminder(context, restoredTask)
                }
            }
        }
    }

    LaunchedEffect(uiState.statusBanner) {
        val msg = uiState.statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    LaunchedEffect(initialDateKey) {
        if (initialDateKey != null) {
            viewModel.selectDate(initialDateKey)
            if (!initialOpenAdd) {
                onConsumedWidgetIntent()
            }
        }
    }

    LaunchedEffect(initialOpenAdd) {
        if (initialOpenAdd) {
            editingTask = null
            presetDateForNewTask = initialDateKey ?: uiState.selectedDateKey
            presetStartMinute = DateUtils.currentMinuteOfDay()
            showTaskEditorSheet = true
            onConsumedWidgetIntent()
        }
    }

    LaunchedEffect(uiState.tasksByDate) {
        val allTasks = uiState.tasksByDate.values.flatten()
        com.example.widget.DayCalWidgetDataManager.saveTasksCache(context, allTasks)
        com.example.widget.DayCalWidgetProvider.notifyWidgetDataChanged(context)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            DayCalTopBar(
                isCloudMode = isCloudMode,
                isDarkTheme = isDarkTheme,
                showFull24HourGrid = showFull24HourGrid,
                onToggle24hMode = { showFull24HourGrid = !showFull24HourGrid },
                onToggleTheme = onToggleTheme,
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
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTask = null
                    presetDateForNewTask = uiState.selectedDateKey
                    presetStartMinute = 10 * 60
                    showTaskEditorSheet = true
                },
                shape = CircleShape,
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("fab_add_task")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.btn_add_tile),
                    modifier = Modifier.size(28.dp)
                )
            }
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
                onStepWeek = { offsetDays -> viewModel.stepDay(offsetDays) },
                onOpenDatePicker = { showDatePickerModal = true },
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
                onToggleTask = { task ->
                    val toggled = task.copy(completed = !task.completed)
                    viewModel.toggleTaskCompleted(task)
                    ReminderNotificationManager.scheduleTaskReminder(context, toggled)
                },
                onAdjustTaskSchedule = { task, newStart, newDur ->
                    val clampedStart = newStart.coerceIn(0, 1425)
                    val clampedDuration = newDur.coerceIn(15, 720)
                    viewModel.adjustTaskSchedule(task, clampedStart, clampedDuration)
                    ReminderNotificationManager.scheduleTaskReminder(
                        context = context,
                        task = task.copy(
                            startMinute = clampedStart.toLong(),
                            durationMinutes = clampedDuration.toLong()
                        )
                    )
                },
                onDeleteTask = handleDeleteWithUndo,
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
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showTaskEditorSheet = false },
            sheetState = sheetState,
            containerColor = colors.surfaceContainerLow,
            contentColor = colors.onSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(colors.outlineVariant.copy(alpha = 0.6f))
                    )
                }
            }
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
                        showTaskEditorSheet = false
                        handleDeleteWithUndo(taskToDelete.id)
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
                            text = "Your timeline tiles sync automatically in real time using free Google Firebase services.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Account: $userLabel",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Firestore Path:\n• /users/$currentUserId/tasks",
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
                            text = "You are using My DayCal offline without an account. All timeline tiles are stored locally on this device in a Room SQLite database (my_daycal_local.db).",
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
    isCloudMode: Boolean,
    isDarkTheme: Boolean,
    showFull24HourGrid: Boolean,
    onToggle24hMode: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenStorageInfo: () -> Unit,
    onCloudAction: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.background,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Schedule Emblem + Title + Storage Mode Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.surfaceContainerHigh)
                        .border(
                            width = 1.dp,
                            color = colors.primaryContainer.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(9.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_launcher_daytile_1793874081765),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                    )
                }

                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.surfaceContainerLow,
                    border = BorderStroke(1.dp, colors.surfaceContainerHighest),
                    modifier = Modifier
                        .height(24.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { onOpenStorageInfo() }
                        .testTag("storage_mode_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isCloudMode) Icons.Default.CloudDone else Icons.Default.PhoneAndroid,
                            contentDescription = "Storage Mode Info",
                            tint = if (isCloudMode) colors.tertiary else colors.secondary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = if (isCloudMode) "Sync" else "Local",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            // Right: Segmented [Timeline | 24h] Pill + Theme Toggle (☀/☾) + Account Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.surfaceContainerLow,
                    border = BorderStroke(1.dp, colors.surfaceContainerHighest),
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { onToggle24hMode() }
                        .testTag("toggle_24h_grid_button")
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (!showFull24HourGrid) colors.primaryContainer else Color.Transparent
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Timeline",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (!showFull24HourGrid) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (!showFull24HourGrid) colors.onPrimaryContainer else colors.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (showFull24HourGrid) colors.primaryContainer else Color.Transparent
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "24h",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (showFull24HourGrid) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (showFull24HourGrid) colors.onPrimaryContainer else colors.onSurfaceVariant
                            )
                        }
                    }
                }

                // Warm Ivory Light / Obsidian Dark Theme Toggle
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainer)
                        .border(1.dp, colors.surfaceContainerHighest, CircleShape)
                        .clickable { onToggleTheme() }
                        .testTag("theme_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkTheme) "Switch to Warm Ivory Light Theme" else "Switch to Obsidian Dark Theme",
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainer)
                        .border(1.dp, colors.surfaceContainerHighest, CircleShape)
                        .clickable { onCloudAction() }
                        .testTag(if (isCloudMode) "sign_out_button" else "switch_to_cloud_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCloudMode) Icons.AutoMirrored.Filled.Logout else Icons.Default.Person,
                        contentDescription = if (isCloudMode) stringResource(id = R.string.sign_out) else "Switch to Google Cloud Sync",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
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

private enum class ActiveTimeDial {
    NONE,
    HOUR,
    MINUTE
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
    val colors = MaterialTheme.colorScheme
    var title by rememberSaveable { mutableStateOf(initialTask?.title ?: "") }
    var notes by rememberSaveable { mutableStateOf(initialTask?.notes ?: "") }
    var dateKey by rememberSaveable { mutableStateOf(initialTask?.dateKey ?: defaultDateKey) }
    var startHour by rememberSaveable {
        mutableIntStateOf(((initialTask?.startMinute?.toInt() ?: defaultStartMinute) / 60).coerceIn(0, 23))
    }
    var startMinutePart by rememberSaveable {
        mutableIntStateOf(((initialTask?.startMinute?.toInt() ?: defaultStartMinute) % 60 / 5) * 5)
    }
    var durationMinutes by rememberSaveable {
        mutableIntStateOf((initialTask?.durationMinutes?.toInt() ?: 45).coerceIn(15, 720))
    }
    var selectedCategory by rememberSaveable {
        mutableStateOf(TaskCategory.fromKey(initialTask?.category ?: TaskCategory.FOCUS.key).key)
    }
    var reminderMinutes by rememberSaveable {
        mutableIntStateOf(initialTask?.reminderMinutes?.toInt() ?: -1)
    }
    var showSheetDatePicker by rememberSaveable { mutableStateOf(false) }
    var activeDial by rememberSaveable { mutableStateOf(ActiveTimeDial.NONE) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val isPm = startHour >= 12
    val displayHour12 = when {
        startHour == 0 -> 12
        startHour > 12 -> startHour - 12
        else -> startHour
    }

    val hourOptions = remember { (1..12).toList() }
    val minuteOptions = remember { (0..60 step 5).toList() }
    val durationPresets = remember { listOf(15, 30, 45, 60, 90, 120, 180, 240) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 28.dp)
    ) {
        // Header Row: Title on left, Date Picker in the right corner center + crisp non-clipping 34dp X button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (initialTask == null) "New Entry" else "Edit Entry",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colors.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.surfaceContainerHigh,
                    border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.40f)),
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { showSheetDatePicker = true }
                        .testTag("sheet_date_picker_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick task date",
                            tint = colors.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = DateUtils.formatShortMonthDay(dateKey),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = colors.primary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHigh)
                        .border(1.dp, colors.surfaceContainerHighest, CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        // Step 1: TIME & DURATION Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceContainer,
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.30f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "TIME",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (activeDial == ActiveTimeDial.NONE) {
                            "Tap hour or minute to spin dial"
                        } else {
                            "Scroll dial or tap to lock"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = colors.outline
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TimeDialBox(
                        values = hourOptions,
                        selectedValue = displayHour12,
                        label = "HOUR",
                        isDialOpen = activeDial == ActiveTimeDial.HOUR,
                        onOpenDial = {
                            activeDial = if (activeDial == ActiveTimeDial.HOUR) {
                                ActiveTimeDial.NONE
                            } else {
                                ActiveTimeDial.HOUR
                            }
                        },
                        onValueSelected = { newHour12 ->
                            val normalized12 = newHour12 % 12
                            startHour = if (isPm) normalized12 + 12 else normalized12
                        },
                        onCloseDial = { activeDial = ActiveTimeDial.NONE },
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = colors.outline
                    )

                    TimeDialBox(
                        values = minuteOptions,
                        selectedValue = startMinutePart,
                        label = "MINUTE",
                        isDialOpen = activeDial == ActiveTimeDial.MINUTE,
                        onOpenDial = {
                            activeDial = if (activeDial == ActiveTimeDial.MINUTE) {
                                ActiveTimeDial.NONE
                            } else {
                                ActiveTimeDial.MINUTE
                            }
                        },
                        onValueSelected = { newMinute ->
                            startMinutePart = newMinute
                        },
                        onCloseDial = { activeDial = ActiveTimeDial.NONE },
                        modifier = Modifier.weight(1f)
                    )

                    // Stacked AM / PM Toggle
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceContainerHigh,
                        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .width(54.dp)
                            .height(if (activeDial != ActiveTimeDial.NONE) 132.dp else 92.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(if (!isPm) colors.primaryContainer else Color.Transparent)
                                    .clickable {
                                        if (isPm) startHour = (startHour - 12).coerceAtLeast(0)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "AM",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (!isPm) colors.onPrimaryContainer else colors.outline
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(if (isPm) colors.primaryContainer else Color.Transparent)
                                    .clickable {
                                        if (!isPm) startHour = (startHour + 12).coerceAtMost(23)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "PM",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isPm) colors.onPrimaryContainer else colors.outline
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timelapse,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "DURATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(durationPresets, key = { it }) { preset ->
                        SheetPillChip(
                            label = DateUtils.formatDuration(preset),
                            isSelected = durationMinutes == preset,
                            onClick = { durationMinutes = preset },
                            testTag = "duration_chip_$preset"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2: Enlarged & Prominent TASK & DETAILS Card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Notes,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "TASK & DETAILS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = colors.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceContainer,
            border = BorderStroke(1.dp, colors.primaryContainer.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = colors.onSurface
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                        .testTag("task_title_input"),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (title.isEmpty()) {
                                Text(
                                    text = "What are you working on?",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp,
                                    color = colors.outline.copy(alpha = 0.70f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.45f))

                BasicTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    minLines = 3,
                    maxLines = 5,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = colors.onSurfaceVariant
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 92.dp)
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                        .testTag("task_notes_input"),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.TopStart) {
                            if (notes.isEmpty()) {
                                Text(
                                    text = "Add key outcomes, context, or quick notes (optional)...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontSize = 15.sp,
                                    color = colors.outline.copy(alpha = 0.60f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 3: CATEGORY Pills (`[Focus, Work, Personal, Health, Break]`)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Label,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = colors.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(TaskCategory.entries, key = { it.key }) { cat ->
                val isSelected = selectedCategory == cat.key
                val palette = getTilePalette(cat.fixedColorIndex)
                val catIcon: ImageVector = when (cat) {
                    TaskCategory.FOCUS -> Icons.Default.Bolt
                    TaskCategory.WORK -> Icons.Default.Work
                    TaskCategory.PERSONAL -> Icons.Default.Spa
                    TaskCategory.HEALTH -> Icons.Default.Favorite
                    TaskCategory.BREAK -> Icons.Default.LocalCafe
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) {
                        palette.accent.copy(alpha = 0.18f)
                    } else {
                        colors.surfaceContainerHigh
                    },
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) palette.accent else colors.outlineVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { selectedCategory = cat.key }
                        .testTag("category_chip_${cat.key}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = catIcon,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = cat.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (isSelected) palette.accent else colors.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 4: REMINDER Strip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "REMINDER",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = colors.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ReminderOption.entries, key = { it.minutesBefore }) { option ->
                SheetPillChip(
                    label = option.label,
                    isSelected = reminderMinutes == option.minutesBefore,
                    onClick = {
                        reminderMinutes = option.minutesBefore
                        if (option != ReminderOption.NONE &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ) {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!hasPerm) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    },
                    testTag = "reminder_chip_${option.minutesBefore}"
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Step 5: Prominent Amber Action Button (`+ Add` or `Save Changes` + `Delete`)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (onDelete != null) {
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, colors.error.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.error
                    ),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("delete_task_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }

            Button(
                onClick = {
                    val finalTitle = title.trim().ifBlank { "Scheduled Focus Block" }
                    val fixedColorIndex = TaskCategory.fromKey(selectedCategory).fixedColorIndex
                    val normalizedStartMinutes = if (startMinutePart == 60) {
                        ((startHour + 1) % 24) * 60
                    } else {
                        startHour * 60 + startMinutePart
                    }
                    onSave(
                        finalTitle,
                        notes.trim(),
                        dateKey,
                        normalizedStartMinutes,
                        durationMinutes,
                        selectedCategory,
                        fixedColorIndex,
                        reminderMinutes
                    )
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("save_task_sheet_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (initialTask == null) "Add" else "Save Changes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
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

@Composable
private fun TimeDialBox(
    values: List<Int>,
    selectedValue: Int,
    label: String,
    isDialOpen: Boolean,
    onOpenDial: () -> Unit,
    onValueSelected: (Int) -> Unit,
    onCloseDial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerHigh,
        border = BorderStroke(
            width = if (isDialOpen) 2.dp else 1.dp,
            color = if (isDialOpen) colors.primaryContainer else colors.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .height(if (isDialOpen) 132.dp else 92.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isDialOpen) { onOpenDial() }
    ) {
        if (!isDialOpen) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format(java.util.Locale.US, "%02d", selectedValue),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    color = colors.primary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = colors.outline
                )
            }
        } else {
            VerticalLoopDial(
                values = values,
                selectedValue = selectedValue,
                label = label,
                onValueSelected = onValueSelected,
                onCloseDial = onCloseDial
            )
        }
    }
}

@Composable
private fun VerticalLoopDial(
    values: List<Int>,
    selectedValue: Int,
    label: String,
    onValueSelected: (Int) -> Unit,
    onCloseDial: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val count = values.size
    val totalLoops = 200
    val totalItems = count * totalLoops
    val selectedIdxInValues = values.indexOf(selectedValue).coerceAtLeast(0)
    val initialCenterIndex = (totalLoops / 2) * count + selectedIdxInValues
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (initialCenterIndex - 1).coerceAtLeast(0)
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()

    val centerItemIndex by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex + 1
        }
    }

    LaunchedEffect(centerItemIndex, listState.isScrollInProgress) {
        val valueAtCenter = values[centerItemIndex.mod(count)]
        if (valueAtCenter != selectedValue) {
            onValueSelected(valueAtCenter)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.primaryContainer.copy(alpha = 0.16f))
                .border(
                    width = 1.dp,
                    color = colors.primaryContainer.copy(alpha = 0.50f),
                    shape = RoundedCornerShape(10.dp)
                )
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(
                count = totalItems,
                key = { index -> index }
            ) { index ->
                val itemValue = values[index.mod(count)]
                val isCenter = index == centerItemIndex

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clickable {
                            if (isCenter) {
                                onCloseDial()
                            } else {
                                onValueSelected(itemValue)
                                scope.launch {
                                    listState.animateScrollToItem((index - 1).coerceAtLeast(0))
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "%02d", itemValue),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (isCenter) 24.sp else 15.sp,
                        color = if (isCenter) colors.primary else colors.outline.copy(alpha = 0.65f)
                    )
                }
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            color = colors.primary.copy(alpha = 0.75f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 4.dp)
        )
    }
}

@Composable
private fun SheetPillChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String? = null
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isSelected) colors.primaryContainer else colors.surfaceContainerHigh,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) colors.primaryContainer else colors.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 12.sp,
                color = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
