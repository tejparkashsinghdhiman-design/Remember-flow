package com.rememberflow.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberflow.app.core.RecurrenceEngine
import com.rememberflow.app.core.Reminder
import com.rememberflow.app.core.ReminderFilter
import com.rememberflow.app.core.ReminderRow
import com.rememberflow.app.core.ReminderStatus
import com.rememberflow.app.ui.Fmt
import com.rememberflow.app.ui.SystemStatus
import com.rememberflow.app.ui.components.AppIcons
import com.rememberflow.app.ui.components.EmptyState
import com.rememberflow.app.ui.components.GlassCard
import com.rememberflow.app.ui.components.GradientButton
import com.rememberflow.app.ui.components.Pill
import com.rememberflow.app.ui.components.SectionTitle
import com.rememberflow.app.ui.components.StatusDot
import com.rememberflow.app.ui.theme.Gradients
import com.rememberflow.app.ui.theme.MonoFamily
import com.rememberflow.app.ui.theme.Palette
import java.time.Instant
import java.time.ZoneId

@Composable
fun HomeScreen(
    allRows: List<ReminderRow>,
    visibleRows: List<ReminderRow>,
    now: Instant,
    query: String,
    filter: ReminderFilter,
    status: SystemStatus,
    highlightId: Long?,
    onQueryChange: (String) -> Unit,
    onFilterChange: (ReminderFilter) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Reminder) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onOpenReliability: () -> Unit,
    contentPadding: PaddingValues
) {
    var pendingDelete by remember { mutableStateOf<Reminder?>(null) }
    var expandedId by remember { mutableLongStateOf(-1L) }
    val listState = rememberLazyListState()
    val zone = remember { ZoneId.systemDefault() }

    // Opening from a notification: expand and scroll to that reminder.
    LaunchedEffect(highlightId, visibleRows.size) {
        if (highlightId != null) {
            val index = visibleRows.indexOfFirst { it.reminder.id == highlightId }
            if (index >= 0) {
                expandedId = highlightId
                listState.animateScrollToItem(index + 3)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 112.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "brand") { BrandHeader() }
        item(key = "clock") {
            ClockHero(
                now = now,
                zone = zone,
                next = allRows.firstOrNull { it.status == ReminderStatus.ACTIVE },
                activeCount = allRows.count { it.status == ReminderStatus.ACTIVE },
                pausedCount = allRows.count { it.status == ReminderStatus.PAUSED }
            )
        }
        if (!status.allGood) {
            item(key = "banner") { AttentionBanner(status, onOpenReliability) }
        }
        if (allRows.isNotEmpty()) {
            item(key = "search") {
                SearchAndFilters(query, filter, onQueryChange, onFilterChange)
            }
        }

        if (allRows.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = AppIcons.Schedule,
                    title = "Nothing to remember yet",
                    message = "Write down anything you don't want to forget, pick when it should nag you, and RememberFlow will keep reminding you, even with the app closed.",
                    action = { GradientButton("Create your first reminder", onAdd, icon = Icons.Filled.Add) }
                )
            }
        } else if (visibleRows.isEmpty()) {
            item(key = "nomatch") {
                EmptyState(
                    icon = Icons.Filled.Search,
                    title = "No matches",
                    message = if (query.isNotBlank()) "Nothing matches \"${query.trim()}\" with this filter." else "No reminders in this view."
                )
            }
        } else {
            items(visibleRows, key = { it.reminder.id }) { row ->
                ReminderCard(
                    row = row,
                    now = now,
                    expanded = expandedId == row.reminder.id,
                    highlighted = highlightId == row.reminder.id,
                    onToggleExpand = { expandedId = if (expandedId == row.reminder.id) -1L else row.reminder.id },
                    onEdit = { onEdit(row.reminder) },
                    onTogglePause = {
                        if (row.reminder.paused) onResume(row.reminder.id) else onPause(row.reminder.id)
                    },
                    onDelete = { pendingDelete = row.reminder }
                )
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Palette.NavyRaised,
            title = { Text("Delete reminder?", color = Palette.TextHigh) },
            text = {
                Text(
                    "\"${target.title.ifBlank { "Untitled" }}\" and its schedule will be removed from this phone. This can't be undone.",
                    color = Palette.TextMid
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    pendingDelete = null
                }) { Text("Delete", color = Palette.Rose) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Keep", color = Palette.TextMid) }
            }
        )
    }
}

@Composable
private fun BrandHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Gradients.Accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(AppIcons.Repeat, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Palette.TextHigh)) { append("Remember") }
                withStyle(SpanStyle(color = Palette.Cyan)) { append("Flow") }
            },
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun ClockHero(
    now: Instant,
    zone: ZoneId,
    next: ReminderRow?,
    activeCount: Int,
    pausedCount: Int
) {
    val local = now.atZone(zone)
    val hm = local.format(Fmt.hourMinute)
    val seconds = local.second.toString().padStart(2, '0')

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Gradients.Hero)
            .border(1.dp, Brush.linearGradient(listOf(Palette.Violet.copy(alpha = 0.7f), Palette.Cyan.copy(alpha = 0.5f))), RoundedCornerShape(32.dp))
    ) {
        // Soft glow in the corner.
        Box(
            Modifier
                .size(220.dp)
                .align(Alignment.TopEnd)
                .background(Brush.radialGradient(listOf(Palette.Cyan.copy(alpha = 0.16f), Color.Transparent)))
        )
        Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(color = Palette.Cyan, pulsing = true, size = 6.dp)
                Text("LIVE", style = MaterialTheme.typography.labelSmall, color = Palette.CyanSoft)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${zone.id}  ·  UTC${local.offset.id.let { if (it == "Z") "+00:00" else it }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.TextMid,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Palette.TextHigh)) { append(hm) }
                    withStyle(SpanStyle(color = Palette.Cyan)) { append(":$seconds") }
                },
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 50.sp,
                    letterSpacing = (-2).sp
                ),
                maxLines = 1
            )
            Text(local.format(Fmt.longDate), style = MaterialTheme.typography.bodyMedium, color = Palette.TextMid)

            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Palette.Outline.copy(alpha = 0.6f))
            )
            Spacer(Modifier.height(14.dp))

            if (next?.next != null) {
                Text("NEXT UP", style = MaterialTheme.typography.labelSmall, color = Palette.TextLow)
                Spacer(Modifier.height(4.dp))
                Text(
                    next.reminder.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    color = Palette.TextHigh,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Fmt.countdown(now, next.next),
                        style = MaterialTheme.typography.titleSmall,
                        color = Palette.Cyan
                    )
                    Text(
                        "  ·  " + next.next.atZone(zone).format(Fmt.dateTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.TextMid
                    )
                }
            } else {
                Text(
                    if (activeCount == 0) "No active reminders" else "Nothing scheduled",
                    style = MaterialTheme.typography.titleSmall,
                    color = Palette.TextMid
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("$activeCount active", color = Palette.Cyan)
                if (pausedCount > 0) Pill("$pausedCount paused", color = Palette.Amber)
            }
        }
    }
}

@Composable
private fun AttentionBanner(status: SystemStatus, onOpen: () -> Unit) {
    val message = when {
        !status.notificationsOk -> "Notifications are blocked, so reminders can't reach you. Tap to fix."
        !status.exactAlarmsAllowed -> "Exact alarms are off: reminders may arrive a few minutes late. Tap to fix."
        else -> ""
    }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
        accent = Brush.linearGradient(listOf(Palette.Amber.copy(alpha = 0.8f), Palette.Rose.copy(alpha = 0.5f)))
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Palette.Amber)
            Spacer(Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = Palette.TextHigh)
        }
    }
}

@Composable
private fun SearchAndFilters(
    query: String,
    filter: ReminderFilter,
    onQueryChange: (String) -> Unit,
    onFilterChange: (ReminderFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search reminders", color = Palette.TextLow) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Palette.TextMid) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = Palette.TextMid)
                    }
                }
            },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Palette.Violet,
                unfocusedBorderColor = Palette.Outline,
                focusedContainerColor = Palette.NavyRaised,
                unfocusedContainerColor = Palette.NavyRaised,
                cursorColor = Palette.Cyan,
                focusedTextColor = Palette.TextHigh,
                unfocusedTextColor = Palette.TextHigh
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterPill("All", filter == ReminderFilter.ALL) { onFilterChange(ReminderFilter.ALL) }
            FilterPill("Active", filter == ReminderFilter.ACTIVE) { onFilterChange(ReminderFilter.ACTIVE) }
            FilterPill("Paused", filter == ReminderFilter.PAUSED) { onFilterChange(ReminderFilter.PAUSED) }
        }
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) Palette.Violet.copy(alpha = 0.25f) else Color.Transparent)
            .border(1.dp, if (selected) Palette.Violet else Palette.Outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Palette.TextHigh else Palette.TextMid
        )
    }
}

@Composable
private fun ReminderCard(
    row: ReminderRow,
    now: Instant,
    expanded: Boolean,
    highlighted: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onTogglePause: () -> Unit,
    onDelete: () -> Unit
) {
    val reminder = row.reminder
    val zone = reminder.zone
    val dotColor = when (row.status) {
        ReminderStatus.ACTIVE -> Palette.Cyan
        ReminderStatus.PAUSED -> Palette.Amber
        ReminderStatus.COMPLETED -> Palette.TextLow
    }
    val dim = row.status != ReminderStatus.ACTIVE

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        onClick = onToggleExpand,
        accent = if (highlighted) Gradients.Accent else null
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 2.dp, end = 6.dp)) {
                    StatusDot(color = dotColor, pulsing = row.status == ReminderStatus.ACTIVE, size = 8.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        reminder.title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (dim) Palette.TextMid else Palette.TextHigh,
                        maxLines = if (expanded) 6 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (reminder.details.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            reminder.details,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Palette.TextMid,
                            maxLines = if (expanded) 12 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(
                    reminder.recurrence.describeShort(),
                    color = Palette.VioletSoft,
                    leading = {
                        Icon(AppIcons.Repeat, contentDescription = null, tint = Palette.VioletSoft, modifier = Modifier.size(12.dp))
                    }
                )
                when (row.status) {
                    ReminderStatus.PAUSED -> Pill("Paused", color = Palette.Amber)
                    ReminderStatus.COMPLETED -> Pill("Finished", color = Palette.TextMid)
                    ReminderStatus.ACTIVE -> Unit
                }
                if (reminder.missedCount > 0) Pill("${reminder.missedCount} missed", color = Palette.Rose)
            }

            Spacer(Modifier.height(12.dp))
            if (row.next != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (row.status == ReminderStatus.PAUSED) "Would fire" else Fmt.countdown(now, row.next),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (dim) Palette.TextMid else Palette.Cyan
                    )
                    Text(
                        "  ·  " + row.next.atZone(zone).format(Fmt.dateTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.TextMid
                    )
                }
            } else {
                Text("No more occurrences", style = MaterialTheme.typography.bodySmall, color = Palette.TextMid)
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    UpcomingList(reminder, now, row.next)
                    if (reminder.lastFiredMillis > 0) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Last notified " + Fmt.ago(now, Instant.ofEpochMilli(reminder.lastFiredMillis)),
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.TextLow
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(Icons.Filled.Edit, "Edit", Palette.VioletSoft, onEdit)
                        if (row.status != ReminderStatus.COMPLETED) {
                            if (reminder.paused) {
                                ActionButton(Icons.Filled.PlayArrow, "Resume", Palette.Mint, onTogglePause)
                            } else {
                                ActionButton(AppIcons.Pause, "Pause", Palette.Amber, onTogglePause)
                            }
                        }
                        ActionButton(Icons.Filled.Delete, "Delete", Palette.Rose, onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingList(reminder: Reminder, now: Instant, next: Instant?) {
    val upcoming = remember(reminder, next) {
        RecurrenceEngine.upcoming(reminder.start, reminder.zone, reminder.recurrence, Instant.now(), 5)
    }
    SectionTitle("Upcoming")
    Spacer(Modifier.height(6.dp))
    if (upcoming.isEmpty()) {
        Text("Nothing further is scheduled.", style = MaterialTheme.typography.bodySmall, color = Palette.TextMid)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            upcoming.forEach { occ ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        occ.time.atZone(reminder.zone).format(Fmt.dateTime),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFamily),
                        color = Palette.TextHigh
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(Fmt.countdown(now, occ.time), style = MaterialTheme.typography.bodySmall, color = Palette.TextLow)
                }
            }
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .clip(shape)
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.3f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}
