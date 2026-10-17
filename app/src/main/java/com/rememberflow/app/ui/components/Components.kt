package com.rememberflow.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.rememberflow.app.ui.theme.Gradients
import com.rememberflow.app.ui.theme.Palette
import kotlinx.coroutines.delay
import java.time.Instant

/**
 * A clock state that ticks exactly on each wall-clock second while the screen is visible, and stops
 * ticking (no wakeups) when the app is in the background.
 */
@Composable
fun rememberNow(): State<Instant> {
    val state = remember { mutableStateOf(Instant.now()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val now = Instant.now()
                state.value = now
                delay(1000 - (now.toEpochMilli() % 1000) + 5)
            }
        }
    }
    return state
}

/** Rounded surface with a subtle gradient fill and hairline border. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accent: Brush? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val base = modifier
        .clip(shape)
        .background(Gradients.Card, shape)
        .border(BorderStroke(1.dp, accent ?: Brush.linearGradient(listOf(Palette.Outline, Palette.Outline.copy(alpha = 0.4f)))), shape)
    val clickable = if (onClick != null) {
        base.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else base
    Box(clickable) { content() }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (enabled) Gradients.Accent else Brush.linearGradient(listOf(Palette.NavyHigh, Palette.NavyHigh)), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) Color.White else Palette.TextLow,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Small tinted label such as "Every hour" or "2 missed". */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Palette.VioletSoft,
    leading: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.size(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
    }
}

/** A dot that gently pulses when [pulsing]; the "this reminder is armed" indicator. */
@Composable
fun StatusDot(color: Color, pulsing: Boolean, size: Dp = 10.dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulsing) 1.9f else 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "pulseScale"
    )
    val ringAlpha by transition.animateFloat(
        initialValue = if (pulsing) 0.45f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "pulseAlpha"
    )
    Box(Modifier.size(size * 2), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .scale(scale)
                .alpha(ringAlpha)
                .background(color, CircleShape)
        )
        Box(
            Modifier
                .size(size)
                .background(color, CircleShape)
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Palette.TextLow,
            letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing
        )
        if (trailing != null) trailing()
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Palette.Violet.copy(alpha = 0.35f), Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Gradients.Card)
                    .border(1.dp, Gradients.Accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Palette.CyanSoft, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = Palette.TextHigh, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Palette.TextMid, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}
