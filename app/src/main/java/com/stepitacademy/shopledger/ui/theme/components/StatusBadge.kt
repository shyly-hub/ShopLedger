package com.stepitacademy.shopledger.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val PendingBackground = Color(0xFFFEE2E2)
private val PendingText = Color(0xFFB91C1C)
private val PaidBackground = Color(0xFFDCFCE7)
private val PaidText = Color(0xFF15803D)

/**
 * Uniform rounded pill for a transaction's status. "Pending" (unpaid) and
 * "Paid" share the same shape/padding so status reads consistently
 * wherever it appears — only color and label text change.
 */
@Composable
fun StatusBadge(isPaid: Boolean, modifier: Modifier = Modifier) {
    val background = if (isPaid) PaidBackground else PendingBackground
    val textColor = if (isPaid) PaidText else PendingText
    val label = if (isPaid) "Paid" else "Pending"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}