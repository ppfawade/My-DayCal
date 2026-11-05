package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.DayTask
import com.example.data.model.TaskCategory
import com.example.data.repository.DaytileRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.ArchiveScreen
import com.example.ui.screens.DayTimelineScreen
import com.example.ui.screens.MultiYearCalendarScreen
import com.example.ui.theme.DaytilePalettes
import com.example.ui.theme.DaytileTheme
import com.example.ui.viewmodel.DaytileViewModel
import com.example.util.DateUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

enum class DaytileDestination(val titleRes: Int) {
    DAY_VIEW(R.string.tab_today),
    ARCHIVE(R.string.tab_archive),
    MULTI_YEAR_CALENDAR(R.string.tab_calendar)
}

@Composable
fun DaytileRootApp(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        DaytileAuthenticatedShell(
            currentUserId = user.uid,
            userEmail = user.email ?: "Signed in with Google"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaytileAuthenticatedShell(
    currentUserId: String,
    userEmail: String
) {
    val viewModel: DaytileViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                DaytileViewModel(DaytileRepository(db), currentUserId)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var currentDestination by rememberSaveable { mutableStateOf(DaytileDestination.DAY_VIEW) }
    var showTaskEditorSheet by rememberSaveable { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<DayTask?>(null) }
    var presetDateForNewTask by rememberSaveable { mutableStateOf(uiState.selectedDateKey) }
    var presetStartMinute by rememberSaveable { mutableIntStateOf(9 * 60) }
    var showCloudSyncDialog by rememberSaveable { mutableStateOf(false) }

    // Secondary screen BackHandler per Android guidelines
    BackHandler(enabled = currentDestination != DaytileDestination.DAY_VIEW) {
        currentDestination = DaytileDestination.DAY_VIEW
    }

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
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showCloudSyncDialog = true }
                                .testTag("cloud_sync_status_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Cloud Sync Info",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Synced",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            signOutUser(
                                credentialManager = credentialManager,
                                onSignOutComplete = {},
                                scope = scope
                            )
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(id = R.string.sign_out)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
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
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentDestination == DaytileDestination.DAY_VIEW,
                    onClick = { currentDestination = DaytileDestination.DAY_VIEW },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.tab_today)) },
                    modifier = Modifier.testTag("nav_day_view")
                )
                NavigationBarItem(
                    selected = currentDestination == DaytileDestination.ARCHIVE,
                    onClick = { currentDestination = DaytileDestination.ARCHIVE },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.tab_archive)) },
                    modifier = Modifier.testTag("nav_archive")
                )
                NavigationBarItem(
                    selected = currentDestination == DaytileDestination.MULTI_YEAR_CALENDAR,
                    onClick = { currentDestination = DaytileDestination.MULTI_YEAR_CALENDAR },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.tab_calendar)) },
                    modifier = Modifier.testTag("nav_calendar")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                DaytileDestination.DAY_VIEW -> {
                    DayTimelineScreen(
                        uiState = uiState,
                        onSelectDate = { viewModel.selectDate(it) },
                        onStepDay = { viewModel.stepDay(it) },
                        onJumpToToday = { viewModel.jumpToToday() },
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
                        onDeleteTask = { viewModel.deleteTask(it) },
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

                DaytileDestination.ARCHIVE -> {
                    ArchiveScreen(
                        uiState = uiState,
                        onOpenDayInTimeline = { dateKey ->
                            viewModel.selectDate(dateKey)
                            currentDestination = DaytileDestination.DAY_VIEW
                        }
                    )
                }

                DaytileDestination.MULTI_YEAR_CALENDAR -> {
                    MultiYearCalendarScreen(
                        uiState = uiState,
                        onSetYearMonth = { y, m -> viewModel.setCalendarYearMonth(y, m) },
                        onStepMonth = { delta -> viewModel.stepCalendarMonth(delta) },
                        onSelectDateAndOpenDay = { dateKey ->
                            viewModel.selectDate(dateKey)
                            currentDestination = DaytileDestination.DAY_VIEW
                        },
                        onScheduleTaskForDate = { dateKey ->
                            viewModel.selectDate(dateKey)
                            editingTask = null
                            presetDateForNewTask = dateKey
                            presetStartMinute = 9 * 60
                            showTaskEditorSheet = true
                        }
                    )
                }
            }
        }
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
                onSave = { title, notes, dateKey, startMin, durationMin, category, colorIdx ->
                    val existing = editingTask
                    if (existing != null) {
                        viewModel.updateTaskFull(
                            task = existing,
                            title = title,
                            notes = notes,
                            dateKey = dateKey,
                            startMinute = startMin,
                            durationMinutes = durationMin,
                            category = category,
                            colorIndex = colorIdx
                        )
                    } else {
                        viewModel.addTask(
                            dateKey = dateKey,
                            title = title,
                            notes = notes,
                            startMinute = startMin,
                            durationMinutes = durationMin,
                            category = category,
                            colorIndex = colorIdx
                        )
                    }
                    viewModel.selectDate(dateKey)
                    showTaskEditorSheet = false
                },
                onDelete = editingTask?.let { taskToDelete ->
                    {
                        viewModel.deleteTask(taskToDelete.id)
                        showTaskEditorSheet = false
                    }
                }
            )
        }
    }

    if (showCloudSyncDialog) {
        val dbId = context.getString(R.string.firestore_database_id)
        AlertDialog(
            onDismissRequest = { showCloudSyncDialog = false },
            title = { Text("Google Cloud Sync Active") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your timeline tiles, future plans, and past journal archives sync automatically in real time using free Google Firebase services.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Account: $userEmail",
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
                }
            },
            confirmButton = {
                TextButton(onClick = { showCloudSyncDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
        colorIndex: Int
    ) -> Unit,
    onDelete: (() -> Unit)?
) {
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
    var category by rememberSaveable {
        mutableStateOf(initialTask?.category ?: TaskCategory.FOCUS.key)
    }
    var colorIndex by rememberSaveable {
        mutableIntStateOf((initialTask?.colorIndex?.toInt() ?: 0).coerceIn(0, 5))
    }

    val durationPresets = listOf(15, 30, 45, 60, 90, 120, 180, 240)
    val minuteQuarters = listOf(0, 15, 30, 45)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .padding(bottom = 28.dp)
    ) {
        Text(
            text = if (initialTask == null) "New Time Tile" else "Edit Time Tile",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Scheduled for ${DateUtils.formatFullDateHeader(dateKey)} ($dateKey)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Date Offset Selector (Today, Tomorrow, +7d, or custom YYYY-MM-DD)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = dateKey == DateUtils.todayDateKey(),
                onClick = { dateKey = DateUtils.todayDateKey() },
                label = { Text("Today") }
            )
            FilterChip(
                selected = dateKey == DateUtils.addDays(DateUtils.todayDateKey(), 1),
                onClick = { dateKey = DateUtils.addDays(DateUtils.todayDateKey(), 1) },
                label = { Text("Tomorrow") }
            )
            FilterChip(
                selected = dateKey == DateUtils.addDays(dateKey, 7),
                onClick = { dateKey = DateUtils.addDays(dateKey, 7) },
                label = { Text("+1 Week") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = dateKey,
            onValueChange = { input ->
                if (input.length <= 10) dateKey = input
            },
            label = { Text("Date (YYYY-MM-DD • ±10 Years Supported)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("task_date_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Task or Activity Title") },
            placeholder = { Text("e.g., Deep Focus Architecture") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("task_title_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes or Sub-steps (optional)") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("task_notes_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Start Time Selector
        Text(
            text = "START TIME: ${String.format(java.util.Locale.US, "%02d:%02d", startHour, startMinutePart)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { startHour = (startHour - 1).mod(24) }) {
                Text("-1h")
            }
            Text(
                text = String.format(java.util.Locale.US, "%02d:00", startHour),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = { startHour = (startHour + 1).mod(24) }) {
                Text("+1h")
            }
            Spacer(modifier = Modifier.width(6.dp))
            minuteQuarters.forEach { q ->
                FilterChip(
                    selected = startMinutePart == q,
                    onClick = { startMinutePart = q },
                    label = { Text(String.format(java.util.Locale.US, ":%02d", q)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Duration Selector
        Text(
            text = "DURATION: ${DateUtils.formatDuration(durationMinutes)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            durationPresets.forEach { dur ->
                FilterChip(
                    selected = durationMinutes == dur,
                    onClick = { durationMinutes = dur },
                    label = { Text(DateUtils.formatDuration(dur)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category Selector
        Text(
            text = "CATEGORY & TILE COLOR",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TaskCategory.entries.forEach { cat ->
                FilterChip(
                    selected = category == cat.key,
                    onClick = {
                        category = cat.key
                        colorIndex = cat.defaultColorIndex
                    },
                    label = { Text(cat.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DaytilePalettes.forEachIndexed { idx, palette ->
                val isSelectedColor = colorIndex == idx
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(palette.accent)
                        .border(
                            width = if (isSelectedColor) 3.dp else 0.dp,
                            color = if (isSelectedColor) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { colorIndex = idx },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelectedColor) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected color",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    val validDate = if (dateKey.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) {
                        dateKey
                    } else {
                        defaultDateKey
                    }
                    onSave(
                        title.ifBlank { "Focused Time Block" },
                        notes,
                        validDate,
                        startHour * 60 + startMinutePart,
                        durationMinutes,
                        category,
                        colorIndex
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
}
