package com.rememberflow.app.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rememberflow.app.core.ReminderRows
import com.rememberflow.app.core.Reminder
import com.rememberflow.app.core.TimelineEntry
import com.rememberflow.app.ui.Fmt
import com.rememberflow.app.ui.components.AppIcons
import com.rememberflow.app.ui.components.EmptyState
import com.rememberflow.app.ui.components.GlassCard
import com.rememberflow.app.ui.theme.Gradients
import com.rememberflow.app.ui.theme.MonoFamily
import com.rememberflow.app.ui.theme.Palette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private sealed interface TimelineItem {
    val key: String

    data class DayHeader(val label: String, val date: LocalDate, val count: Int) : TimelineItem {
        override val key: String get() = "day-$date"
    }

    data class Entry(val entry: TimelineEntry, val first: Boolean, val last: Boolean) : TimelineItem {
        override val key: String get() = "e-${entry.reminder.id}-${entry.occurrence.index}"
    }
}

@Composable
fun TimelineScreen(
    reminders: List<Reminder>,
    now: Instant,
    onOpen: (Reminder) -> Unit,
    contentPadding: PaddingValues
) {
    val zone = ZoneId.systemDefault()
    // Rebuilt when the reminders change or the minute changes, not every second.
    val minuteBucket = now.epochSecond / 60
    val entries = remember(reminders, minuteBucket) { ReminderRows.timeline(reminders, now) }
    val today = now.atZone(zone).toLocalDate()

    val items = remember(entries, today) {
        val out = ArrayList<TimelineItem>()
        val byDay = entries.groupBy { it.occurrence.time.atZone(zone).toLocalDate() }
        for ((date, list) in byDay) {
            out.add(TimelineItem.DayHeader(Fmt.dayLabel(list.first().occurrence.time, zone, today), date, list.size))
            list.forEachIndexed { i, e -> out.add(TimelineItem.Entry(e, first = i == 0, last = i == list.lastIndex)) }
        }
        out
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 112.dp
        )
    ) {
        item(key = "title") {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                Text("Timeline", style = MaterialTheme.typography.headlineMedium, color = Palette.TextHigh)
                Text(
                    "Everything coming up, in order.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextMid
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        if (items.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = AppIcons.Timeline,
                    title = "Your timeline is empty",
                    message = "Active reminders appear here in the order they will fire. Paused or finished ones are left out."
                )
            }
        } else {
            items(items, key = { it.key }) { item ->
                when (item) {
                    is TimelineItem.DayHeader -> DayHeader(item)
                    is TimelineItem.Entry -> TimelineRow(item, now, zone, onOpen)
                }
            }
        }
    }
}

@Composable
private fun DayHeader(item: TimelineItem.DayHeader) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            item.label,
            style = MaterialTheme.typography.titleMedium,
            color = Palette.TextHigh
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (item.count == 1) "1 reminder" else "${item.count} reminders",
            style = MaterialTheme.typography.labelMedium,
            color = Palette.TextLow
        )
    }
}

@Composable
private fun TimelineRow(
    item: TimelineItem.Entry,
    now: Instant,
    zone: ZoneId,
    onOpen: (Reminder) -> Unit
) {
    val entry = item.entry
    val time = entry.occurrence.time
    val soon = java.time.Duration.between(now, time).toMinutes() < 60

    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Time column.
        Column(
            Modifier
                .width(64.dp)
                .padding(top = 14.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                time.atZone(zone).format(Fmt.hourMinute),
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFamily),
                color = if (soon) Palette.Cyan else Palette.TextHigh
            )
            Text(
                time.atZone(zone).format(java.time.format.DateTimeFormatter.ofPattern(":ss")),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFamily),
                color = Palette.TextLow
            )
        }

        // Rail with a dot.
        Box(
            Modifier
                .width(28.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .padding(top = if (item.first) 20.dp else 0.dp, bottom = if (item.last) 0.dp else 0.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Palette.Violet.copy(alpha = 0.55f), Palette.Cyan.copy(alpha = 0.25f))
                        )
                    )
            )
            Box(
                Modifier
                    .padding(top = 17.dp)
                    .size(10.dp)
                    .background(if (soon) Gradients.Accent else Brush.linearGradient(listOf(Palette.Violet, Palette.Violet)), CircleShape)
            )
        }

        // Card.
        GlassCard(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 10.dp),
            onClick = { onOpen(entry.reminder) }
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    entry.reminder.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleSmall,
                    color = Palette.TextHigh,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Repeat, contentDescription = null, tint = Palette.VioletSoft, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        entry.reminder.recurrence.describeShort(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Palette.VioletSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        Fmt.countdown(now, time),
                        style = MaterialTheme.typography.labelMedium,
                        color = Palette.TextLow
                    )
                }
            }
        }
    }
}
