package com.rememberflow.app.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rememberflow.app.core.IntervalUnit
import com.rememberflow.app.core.Recurrence
import com.rememberflow.app.core.Reminder
import com.rememberflow.app.ui.Fmt
import com.rememberflow.app.ui.ReminderDraft
import com.rememberflow.app.ui.components.AppIcons
import com.rememberflow.app.ui.components.GlassCard
import com.rememberflow.app.ui.components.GradientButton
import com.rememberflow.app.ui.components.Pill
import com.rememberflow.app.ui.components.SectionTitle
import com.rememberflow.app.ui.theme.Gradients
import com.rememberflow.app.ui.theme.MonoFamily
import com.rememberflow.app.ui.theme.Palette
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun EditorScreen(
    initial: Reminder?,
    now: Instant,
    onClose: () -> Unit,
    onSave: (ReminderDraft) -> Unit
) {
    val state = remember(initial?.id) { EditorState(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    BackHandler(onBack = onClose)

    fun trySave() {
        val problem = state.validate(Instant.now())
        if (problem != null) {
            error = problem
            return
        }
        state.toDraft(initial?.id)?.let(onSave)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Gradients.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top bar.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close editor", tint = Palette.TextHigh)
            }
            Text(
                if (initial == null) "New reminder" else "Edit reminder",
                style = MaterialTheme.typography.titleLarge,
                color = Palette.TextHigh,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { trySave() }) {
                Text("Save", color = Palette.Cyan, style = MaterialTheme.typography.labelLarge)
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // What to remember.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("What should I remember?")
                FieldBox(
                    value = state.title,
                    onChange = { state.title = it; error = null },
                    placeholder = "e.g. Take the medicine",
                    minLines = 1,
                    maxLines = 3
                )
                FieldBox(
                    value = state.details,
                    onChange = { state.details = it },
                    placeholder = "Notes (optional)",
                    minLines = 2,
                    maxLines = 6
                )
            }

            // When.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Starts")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PickerField(
                        modifier = Modifier.weight(1.4f),
                        icon = AppIcons.Event,
                        label = state.date.format(Fmt.shortDate),
                        onClick = { showDate = true }
                    )
                    PickerField(
                        modifier = Modifier.weight(1f),
                        icon = AppIcons.Schedule,
                        label = state.time.format(Fmt.hourMinute),
                        onClick = { showTime = true }
                    )
                }
                QuickStarts(state)
            }

            // Repeat.
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Repeats")
                ModeTabs(state.mode) { state.mode = it; error = null }
                when (state.mode) {
                    RepeatMode.ONCE -> Text(
                        "One notification at the start time, then it's finished.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.TextMid
                    )
                    RepeatMode.PRESET -> PresetPicker(state)
                    RepeatMode.CUSTOM -> CustomSequenceEditor(state)
                }
            }

            // Preview.
            PreviewCard(state, now)

            error?.let {
                Text(it, color = Palette.Rose, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            GradientButton(
                text = if (initial == null) "Save reminder" else "Save changes",
                onClick = { trySave() },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.Check
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDate) {
        DatePickerSheet(
            initial = state.date,
            onDismiss = { showDate = false },
            onPick = {
                state.date = it
                error = null
                showDate = false
            }
        )
    }
    if (showTime) {
        TimePickerSheet(
            hour = state.time.hour,
            minute = state.time.minute,
            onDismiss = { showTime = false },
            onPick = { h, m ->
                state.time = java.time.LocalTime.of(h, m)
                error = null
                showTime = false
            }
        )
    }
}

@Composable
private fun FieldBox(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    minLines: Int,
    maxLines: Int
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Palette.TextLow) },
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(18.dp),
        colors = fieldColors()
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Palette.Violet,
    unfocusedBorderColor = Palette.Outline,
    focusedContainerColor = Palette.NavyRaised,
    unfocusedContainerColor = Palette.NavyRaised,
    cursorColor = Palette.Cyan,
    focusedTextColor = Palette.TextHigh,
    unfocusedTextColor = Palette.TextHigh
)

@Composable
private fun PickerField(modifier: Modifier, icon: ImageVector, label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .clip(shape)
            .background(Palette.NavyRaised)
            .border(1.dp, Palette.Outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Palette.Cyan, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = Palette.TextHigh)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickStarts(state: EditorState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallChip("In 10 min") { state.setStartFromNowPlus(10) }
        SmallChip("In 1 hour") { state.setStartFromNowPlus(60) }
        SmallChip("Tonight 21:00") {
            val tonight = java.time.LocalDateTime.now().withHour(21).withMinute(0).withSecond(0).withNano(0)
            if (tonight.isAfter(java.time.LocalDateTime.now())) {
                state.date = tonight.toLocalDate()
                state.time = tonight.toLocalTime()
            } else {
                state.setStartTomorrowAt(21, 0)
            }
        }
        SmallChip("Tomorrow 09:00") { state.setStartTomorrowAt(9, 0) }
    }
}

@Composable
private fun SmallChip(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .border(1.dp, Palette.Outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = Palette.TextMid)
    }
}

@Composable
private fun ModeTabs(selected: RepeatMode, onSelect: (RepeatMode) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Palette.NavyRaised)
            .border(1.dp, Palette.Outline, shape)
            .padding(4.dp)
    ) {
        listOf(
            RepeatMode.ONCE to "Once",
            RepeatMode.PRESET to "Preset",
            RepeatMode.CUSTOM to "Custom flow"
        ).forEach { (mode, label) ->
            val active = mode == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (active) Gradients.Accent else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                    .clickable { onSelect(mode) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) Color.White else Palette.TextMid
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetPicker(state: EditorState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Recurrence.PRESETS.forEach { preset ->
            val selected = state.presetKey == preset.key
            val shape = RoundedCornerShape(50)
            Box(
                Modifier
                    .clip(shape)
                    .background(if (selected) Palette.Violet.copy(alpha = 0.25f) else Color.Transparent)
                    .border(1.dp, if (selected) Palette.Violet else Palette.Outline, shape)
                    .clickable { state.presetKey = preset.key }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    preset.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Palette.TextHigh else Palette.TextMid
                )
            }
        }
    }
}

@Composable
private fun CustomSequenceEditor(state: EditorState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.animateContentSize()) {
        Text(
            "Build your own sequence. After each notification RememberFlow waits for the next stage's gap, in order.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextMid
        )

        state.stages.forEachIndexed { index, stage ->
            StageCard(
                index = index,
                count = state.stages.size,
                stage = stage,
                onAmount = { state.updateStage(index, amount = it) },
                onUnit = { state.updateStage(index, unit = it) },
                onUp = { state.moveStage(index, -1) },
                onDown = { state.moveStage(index, +1) },
                onDelete = { state.removeStage(index) }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val canAdd = state.stages.size < Recurrence.MAX_STAGES
            Row(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.Cyan.copy(alpha = 0.12f))
                    .border(1.dp, Palette.Cyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .clickable(enabled = canAdd) { state.addStage() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Palette.Cyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (canAdd) "Add stage" else "Max ${Recurrence.MAX_STAGES} stages", style = MaterialTheme.typography.labelLarge, color = Palette.Cyan)
            }
            SmallChip("Use 10m → 1h → 12h") { state.loadTemplate() }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Repeat the whole sequence", style = MaterialTheme.typography.titleSmall, color = Palette.TextHigh)
                    Text(
                        if (state.loop) "After the last stage it starts again from stage 1, forever."
                        else "It runs through once and then stops after the last stage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.TextMid
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = state.loop,
                    onCheckedChange = { state.loop = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Palette.Violet,
                        uncheckedThumbColor = Palette.TextMid,
                        uncheckedTrackColor = Palette.NavyHigh,
                        uncheckedBorderColor = Palette.Outline
                    )
                )
            }
        }
        state.recurrenceError?.let {
            Text(it, color = Palette.Amber, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StageCard(
    index: Int,
    count: Int,
    stage: StageDraft,
    onAmount: (String) -> Unit,
    onUnit: (IntervalUnit) -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Gradients.Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    if (index == 0) "Wait, then notify" else "Then wait",
                    style = MaterialTheme.typography.labelLarge,
                    color = Palette.TextMid,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onUp, enabled = index > 0, modifier = Modifier.size(36.dp)) {
                    Icon(AppIcons.ArrowUp, contentDescription = "Move stage up", tint = if (index > 0) Palette.TextHigh else Palette.TextLow, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDown, enabled = index < count - 1, modifier = Modifier.size(36.dp)) {
                    Icon(AppIcons.ArrowDown, contentDescription = "Move stage down", tint = if (index < count - 1) Palette.TextHigh else Palette.TextLow, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete stage", tint = Palette.Rose, modifier = Modifier.size(20.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = stage.amount,
                    onValueChange = onAmount,
                    modifier = Modifier.width(92.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFamily, textAlign = TextAlign.Center),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UnitChip("min", IntervalUnit.MINUTES, stage.unit, onUnit)
                    UnitChip("hr", IntervalUnit.HOURS, stage.unit, onUnit)
                    UnitChip("day", IntervalUnit.DAYS, stage.unit, onUnit)
                    UnitChip("wk", IntervalUnit.WEEKS, stage.unit, onUnit)
                }
            }
        }
    }
}

@Composable
private fun UnitChip(label: String, unit: IntervalUnit, selected: IntervalUnit, onUnit: (IntervalUnit) -> Unit) {
    val active = unit == selected
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (active) Palette.Cyan.copy(alpha = 0.18f) else Color.Transparent)
            .border(1.dp, if (active) Palette.Cyan else Palette.Outline, shape)
            .clickable { onUnit(unit) }
            .padding(horizontal = 11.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (active) Palette.CyanSoft else Palette.TextMid)
    }
}

@Composable
private fun PreviewCard(state: EditorState, now: Instant) {
    // Recomputed whenever any input changes (state reads inside are tracked by Compose).
    val rows = state.preview(now)
    val recurrence = state.recurrence
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Upcoming preview", Modifier.weight(1f))
                if (recurrence != null) Pill(recurrence.describe(), color = Palette.VioletSoft)
            }
            if (rows.isEmpty()) {
                Text(
                    if (recurrence == null) "Complete the schedule above to see when it will fire."
                    else "Nothing would fire. Pick a start time in the future.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextMid
                )
            } else {
                rows.forEachIndexed { i, row ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "#${i + 1}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFamily),
                            color = Palette.TextLow,
                            modifier = Modifier.width(30.dp)
                        )
                        Text(
                            row.time.atZone(java.time.ZoneId.systemDefault()).format(Fmt.dateTime),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFamily),
                            color = Palette.TextHigh,
                            modifier = Modifier.weight(1f)
                        )
                        row.gap?.let {
                            Text("+" + gapLabel(it), style = MaterialTheme.typography.labelMedium, color = Palette.Cyan)
                        }
                    }
                }
                val total = recurrence?.let { com.rememberflow.app.core.RecurrenceEngine.totalOccurrences(it) }
                if (recurrence != null && !recurrence.isOneTime) {
                    Text(
                        if (total == null) "…and so on, repeating." else "Runs $total times in total, then stops.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.TextLow
                    )
                }
            }
        }
    }
}

/** "10m", "1h", "2h 30m", "3d", "1d 2h". */
private fun gapLabel(d: Duration): String {
    val days = d.toDays()
    val hours = d.toHours() % 24
    val minutes = d.toMinutes() % 60
    return when {
        days > 0 && hours > 0 -> "${days}d ${hours}h"
        days > 0 -> "${days}d"
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "${d.seconds}s"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                } ?: onDismiss()
            }) { Text("OK", color = Palette.Cyan) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.TextMid) } }
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerSheet(hour: Int, minute: Int, onDismiss: () -> Unit, onPick: (Int, Int) -> Unit) {
    val pickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.NavyRaised,
        title = { Text("Select time (24-hour)", color = Palette.TextHigh) },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = pickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(pickerState.hour, pickerState.minute) }) { Text("OK", color = Palette.Cyan) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.TextMid) } }
    )
}

/** Compact formatter reused by tests and previews. */
internal val GapTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
