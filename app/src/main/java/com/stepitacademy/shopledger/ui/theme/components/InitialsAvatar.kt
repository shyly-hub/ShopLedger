package com.stepitacademy.shopledger.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

// A small fixed palette so avatar colors stay legible against white text
// and don't clash with the app's primary/error colors used elsewhere.
private val avatarPalette = listOf(
    Color(0xFF6366F1), // indigo
    Color(0xFFEC4899), // pink
    Color(0xFF10B981), // emerald
    Color(0xFFF59E0B), // amber
    Color(0xFF3B82F6), // blue
    Color(0xFFEF4444), // red
    Color(0xFF8B5CF6), // violet
    Color(0xFF14B8A6)  // teal
)

/** Deterministic color per name, so the same customer always gets the same avatar color. */
fun avatarColorForName(name: String): Color {
    if (name.isBlank()) return avatarPalette.first()
    return avatarPalette[abs(name.hashCode()) % avatarPalette.size]
}

/**
 * A colored circle showing a customer's first initial, replacing the
 * generic grey person icon used previously.
 */
@Composable
fun InitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val color = avatarColorForName(name)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}