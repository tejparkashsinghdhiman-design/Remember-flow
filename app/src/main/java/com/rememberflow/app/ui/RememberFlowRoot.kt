package com.rememberflow.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.rememberflow.app.core.ReminderRows
import com.rememberflow.app.ui.components.AppIcons
import com.rememberflow.app.ui.components.rememberNow
import com.rememberflow.app.ui.editor.EditorScreen
import com.rememberflow.app.ui.home.HomeScreen
import com.rememberflow.app.ui.reliability.ReliabilityScreen
import com.rememberflow.app.ui.theme.Gradients
import com.rememberflow.app.ui.theme.Palette
import com.rememberflow.app.ui.timeline.TimelineScreen

private const val TAB_HOME = 0
private const val TAB_TIMELINE = 1
private const val TAB_RELIABILITY = 2

@Composable
fun RememberFlowRoot(viewModel: MainViewModel, openReminderId: Long?) {
    val reminders by viewModel.reminders.collectAsState()
    val query by viewModel.query.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val status by viewModel.status.collectAsState()
    val message by viewModel.message.collectAsState()
    val now by rememberNow()

    var tab by rememberSaveable { mutableIntStateOf(TAB_HOME) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var highlightId by remember { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshStatus()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onForeground() }

    LaunchedEffect(openReminderId) {
        if (openReminderId != null) {
            tab = TAB_HOME
            highlightId = openReminderId
        }
    }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val allRows = remember(reminders, now.epochSecond) { ReminderRows.build(reminders, now) }
    val visibleRows = remember(allRows, query, filter) { ReminderRows.filter(allRows, query, filter) }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !status.notificationPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Gradients.Background)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(containerColor = Palette.Navy.copy(alpha = 0.96f)) {
                    val colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Palette.CyanSoft,
                        selectedTextColor = Palette.CyanSoft,
                        indicatorColor = Palette.Violet.copy(alpha = 0.28f),
                        unselectedIconColor = Palette.TextLow,
                        unselectedTextColor = Palette.TextLow
                    )
                    NavigationBarItem(
                        selected = tab == TAB_HOME,
                        onClick = { tab = TAB_HOME },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("Today") },
                        colors = colors
                    )
                    NavigationBarItem(
                        selected = tab == TAB_TIMELINE,
                        onClick = { tab = TAB_TIMELINE },
                        icon = { Icon(AppIcons.Timeline, contentDescription = null) },
                        label = { Text("Timeline") },
                        colors = colors
                    )
                    NavigationBarItem(
                        selected = tab == TAB_RELIABILITY,
                        onClick = {
                            tab = TAB_RELIABILITY
                            viewModel.refreshStatus()
                        },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("Reliability") },
                        colors = colors
                    )
                }
            },
            floatingActionButton = {
                if (tab != TAB_RELIABILITY) {
                    Box(
                        Modifier
                            .size(62.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Gradients.Accent)
                            .clickable {
                                editingId = null
                                editorOpen = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "New reminder", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
            }
        ) { padding ->
            when (tab) {
                TAB_HOME -> HomeScreen(
                    allRows = allRows,
                    visibleRows = visibleRows,
                    now = now,
                    query = query,
                    filter = filter,
                    status = status,
                    highlightId = highlightId,
                    onQueryChange = viewModel::setQuery,
                    onFilterChange = viewModel::setFilter,
                    onAdd = {
                        editingId = null
                        editorOpen = true
                    },
                    onEdit = {
                        editingId = it.id
                        editorOpen = true
                    },
                    onPause = viewModel::pause,
                    onResume = viewModel::resume,
                    onDelete = viewModel::delete,
                    onOpenReliability = { tab = TAB_RELIABILITY },
                    contentPadding = padding
                )
                TAB_TIMELINE -> TimelineScreen(
                    reminders = reminders,
                    now = now,
                    onOpen = {
                        highlightId = it.id
                        tab = TAB_HOME
                    },
                    contentPadding = padding
                )
                else -> ReliabilityScreen(
                    status = status,
                    onRequestNotificationPermission = { requestNotificationPermissionIfNeeded() },
                    onSendTest = viewModel::sendTestNotification,
                    contentPadding = padding
                )
            }
        }

        AnimatedVisibility(
            visible = editorOpen,
            enter = slideInVertically(tween(280)) { it / 3 } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(220)) { it / 3 } + fadeOut(tween(180))
        ) {
            val editing = editingId?.let { id -> reminders.firstOrNull { it.id == id } }
            EditorScreen(
                initial = editing,
                now = now,
                onClose = { editorOpen = false },
                onSave = { draft ->
                    viewModel.save(draft)
                    editorOpen = false
                    requestNotificationPermissionIfNeeded()
                }
            )
        }
    }
}
