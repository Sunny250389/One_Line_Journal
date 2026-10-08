package com.onelinejournal.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onelinejournal.ui.theme.AccentTheme

/** One compact row of colour swatches; the selected one is ringed and shows a check. */
@Composable
fun ThemeColorMenu(
    selectedTheme: AccentTheme,
    onThemeSelected: (AccentTheme) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccentTheme.values().forEach { theme ->
            ThemeSwatch(
                theme = theme,
                selected = theme == selectedTheme,
                onClick = { onThemeSelected(theme) }
            )
        }
    }
}

@Composable
private fun ThemeSwatch(
    theme: AccentTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (selected) 1.12f else 1f, label = "swatchScale")
    Box(
        modifier = Modifier
            .scale(scale)
            .size(32.dp)
            .clip(CircleShape)
            .background(theme.color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${theme.label} theme${if (selected) ", selected" else ""}"
            },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** One compact row of font cards, each previewing "Aa" in its own typeface. */
@Composable
fun JournalFontPicker(
    selectedFont: JournalFont,
    onFontSelected: (JournalFont) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        JournalFont.values().forEach { font ->
            FontCard(
                font = font,
                selected = font == selectedFont,
                onClick = { onFontSelected(font) }
            )
        }
    }
}

@Composable
private fun RowScope.FontCard(
    font: JournalFont,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .semantics {
                contentDescription = "${font.label} font${if (selected) ", selected" else ""}"
            },
        shape = RoundedCornerShape(10.dp),
        color = if (selected) colors.primary.copy(alpha = 0.10f) else colors.surfaceContainerLow,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) colors.primary else colors.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Aa",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = font.toFontFamily(),
                    fontSize = 22.sp
                ),
                color = if (selected) colors.primary else colors.onSurface
            )
            Text(
                text = font.label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
