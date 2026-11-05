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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.CyberTealContainer
import com.example.ui.theme.DaytileTheme
import com.example.ui.theme.FocusBluePillBg
import com.example.ui.theme.FocusBluePillBorder
import com.example.ui.theme.FocusBlueText
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarAmberBorder
import com.example.ui.theme.SolarAmberContainer
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
                currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
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
        containerColor = MaterialTheme.colorScheme.background,
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
            FloatingActionButton(
                onClick = {
                    editingTask = null
                    presetDateForNewTask = uiState.selectedDateKey
                    presetStartMinute = 9 * 60
                    showTaskEditorSheet = true
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = SolarAmber,
                contentColor = Color(0xFF140D00),
                modifier = Modifier
                    .size(58.dp)
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
            onDismissRequest = { showTaskEditorSheet = false },
            containerColor = Color(0xFF0D121C),
            contentColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
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

/**
 * Top Bar matching Screenshots 3 & 4:
 * - Left: My DayCal + [📱 Local] bordered pill
 * - Center: [📅 Nov 05] amber bordered pill
 * - Right: [= Focus] / [⊞ 24h] blue bordered pill + Cloud icon
 */
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: App Title + Storage Mode Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0E1420),
                    border = BorderStroke(1.dp, Color(0xFF1F2A3C)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenStorageInfo() }
                        .testTag("storage_mode_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCloudMode) Icons.Default.CloudDone else Icons.Default.PhoneAndroid,
                            contentDescription = "Storage Mode Info",
                            tint = if (isCloudMode) CyberTeal else FocusBlueText,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isCloudMode) "Sync" else "Local",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Center: MMM XX Date Picker Trigger
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SolarAmberContainer,
                border = BorderStroke(1.dp, SolarAmberBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenDatePicker() }
                    .testTag("top_date_picker_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Open calendar date picker",
                        tint = SolarAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = shortDateText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = SolarAmber,
                        maxLines = 1
                    )
                }
            }

            // Right: Focus / 24h Toggle + Cloud Action Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (showFull24HourGrid) SolarAmber else FocusBluePillBg,
                    border = BorderStroke(
                        1.dp,
                        if (showFull24HourGrid) SolarAmber else FocusBluePillBorder
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggle24hMode() }
                        .testTag("toggle_24h_grid_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (showFull24HourGrid) Icons.Default.GridView else Icons.Default.ViewStream,
                            contentDescription = "Toggle 24h and Focus view",
                            modifier = Modifier.size(14.dp),
                            tint = if (showFull24HourGrid) Color(0xFF140D00) else FocusBlueText
                        )
                        Text(
                            text = if (showFull24HourGrid) "24h" else "Focus",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (showFull24HourGrid) Color(0xFF140D00) else FocusBlueText
                        )
                    }
                }

                IconButton(
                    onClick = onCloudAction,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag(if (isCloudMode) "sign_out_button" else "switch_to_cloud_button")
                ) {
                    Icon(
                        imageVector = if (isCloudMode) Icons.AutoMirrored.Filled.Logout else Icons.Default.CloudQueue,
                        contentDescription = if (isCloudMode) stringResource(id = R.string.sign_out) else "Switch to Google Cloud Sync",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
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

/**
 * New / Edit Time Tile Bottom Sheet matching Screenshot 5 (`image_f1ffec0e.png`)
 */
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
            .padding(top = 4.dp, bottom = 28.dp)
    ) {
        // Header + Amber Date Pill (matching Screenshot 5)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (initialTask == null) "New Time Tile" else "Edit Time Tile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SolarAmberContainer,
                border = BorderStroke(1.dp, SolarAmberBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showSheetDatePicker = true }
                    .testTag("sheet_date_picker_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Pick task date",
                        tint = SolarAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = DateUtils.formatShortMonthDay(dateKey),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = SolarAmber
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Date Quick Chips (Today, Tomorrow, +1 Week, Calendar...)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val todayKey = DateUtils.todayDateKey()
                SheetPillChip(
                    label = "Today",
                    isSelected = dateKey == todayKey,
                    onClick = { dateKey = todayKey }
                )
            }
            item {
                val tomorrowKey = DateUtils.addDays(DateUtils.todayDateKey(), 1)
                SheetPillChip(
                    label = "Tomorrow",
                    isSelected = dateKey == tomorrowKey,
                    onClick = { dateKey = tomorrowKey }
                )
            }
            item {
                val nextWeekKey = DateUtils.addDays(DateUtils.todayDateKey(), 7)
                SheetPillChip(
                    label = "+1 Week",
                    isSelected = dateKey == nextWeekKey,
                    onClick = { dateKey = nextWeekKey }
                )
            }
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF121826),
                    border = BorderStroke(1.dp, Color(0xFF243046)),
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { showSheetDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Calendar...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Title & Notes Inputs with uppercase monospace labels (matching Screenshot 5)
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = {
                    Text(
                        text = "TASK OR ACTIVITY TITLE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                },
                placeholder = { Text("e.g., Midday Walk & Mobility") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0A0E17),
                    unfocusedContainerColor = Color(0xFF0A0E17),
                    focusedBorderColor = CyberTeal,
                    unfocusedBorderColor = Color(0xFF222D42),
                    focusedLabelColor = Color(0xFF94A3B8),
                    unfocusedLabelColor = Color(0xFF94A3B8),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = {
                    Text(
                        text = "NOTES (OPTIONAL)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0A0E17),
                    unfocusedContainerColor = Color(0xFF0A0E17),
                    focusedBorderColor = CyberTeal,
                    unfocusedBorderColor = Color(0xFF222D42),
                    focusedLabelColor = Color(0xFF94A3B8),
                    unfocusedLabelColor = Color(0xFF94A3B8),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_notes_input")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Horizontal Scroll: Category & Fixed Color Single-Button Row [Focus, Work, Personal, Health, Break]
        Text(
            text = "CATEGORY",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B99AF),
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
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
                        palette.containerDark
                    } else {
                        Color(0xFF121826)
                    },
                    border = BorderStroke(
                        width = if (isSelected) 1.4.dp else 1.dp,
                        color = if (isSelected) palette.accent else Color(0xFF243046)
                    ),
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { selectedCategory = cat.key }
                        .testTag("category_chip_${cat.key}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(palette.accent)
                        )
                        Text(
                            text = cat.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) palette.onContainerDark else Color(0xFFE2E8F0),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Horizontal Scroll: Start Time
        Text(
            text = "START TIME • ${String.format(java.util.Locale.US, "%02d:%02d", startHour, startMinutePart)}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B99AF),
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                SheetStepButton(
                    label = "-1h",
                    onClick = { startHour = (startHour - 1).mod(24) }
                )
            }
            item {
                SheetStepButton(
                    label = "+1h",
                    onClick = { startHour = (startHour + 1).mod(24) }
                )
            }
            items(minuteQuarters, key = { "min_$it" }) { q ->
                SheetPillChip(
                    label = String.format(java.util.Locale.US, ":%02d", q),
                    isSelected = startMinutePart == q,
                    onClick = { startMinutePart = q },
                    useMonoFont = true
                )
            }
            items(quickStartHours, key = { "hr_$it" }) { hr ->
                SheetPillChip(
                    label = String.format(java.util.Locale.US, "%02d:00", hr),
                    isSelected = startHour == hr && startMinutePart == 0,
                    onClick = {
                        startHour = hr
                        startMinutePart = 0
                    },
                    useMonoFont = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Horizontal Scroll: Duration
        Text(
            text = "DURATION • ${DateUtils.formatDuration(durationMinutes)}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B99AF),
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(durationPresets, key = { it }) { dur ->
                SheetPillChip(
                    label = DateUtils.formatDuration(dur),
                    isSelected = durationMinutes == dur,
                    onClick = { durationMinutes = dur },
                    useMonoFont = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Horizontal Scroll: Reminder (matching Screenshot 5)
        val activeReminderLabel = ReminderOption.fromMinutes(reminderMinutes).label
        Text(
            text = "REMINDER • $activeReminderLabel",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B99AF),
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ReminderOption.entries, key = { it.minutesBefore }) { option ->
                val isSelected = reminderMinutes == option.minutesBefore
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) CyberTealContainer else Color(0xFF121826),
                    border = BorderStroke(
                        width = if (isSelected) 1.3.dp else 1.dp,
                        color = if (isSelected) CyberTeal else Color(0xFF243046)
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
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
                        }
                        .testTag("reminder_chip_${option.minutesBefore}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = SolarAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) CyberTeal else Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Action Buttons (matching Screenshot 5: dark crimson Delete pill + bright Amber Save Changes pill)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onDelete != null) {
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.dp, Color(0xFF4C1D24)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF1C1016),
                        contentColor = Color(0xFFFDA4AF)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDA4AF)
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.dp, Color(0xFF243046)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF121826),
                        contentColor = Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
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
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SolarAmber,
                    contentColor = Color(0xFF140D00)
                ),
                modifier = Modifier
                    .weight(1.45f)
                    .height(52.dp)
                    .testTag("save_task_sheet_button")
            ) {
                Text(
                    text = if (initialTask == null) "Create Tile" else "Save Changes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
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
private fun SheetPillChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    useMonoFont: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) CyberTealContainer else Color(0xFF121826),
        border = BorderStroke(
            width = if (isSelected) 1.3.dp else 1.dp,
            color = if (isSelected) CyberTeal else Color(0xFF243046)
        ),
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = label,
                style = if (useMonoFont) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) CyberTeal else Color(0xFFCBD5E1)
            )
        }
    }
}

@Composable
private fun SheetStepButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF121826),
        border = BorderStroke(1.dp, Color(0xFF243046)),
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 15.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = SolarAmber
            )
        }
    }
}
