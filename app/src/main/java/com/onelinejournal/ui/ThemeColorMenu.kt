package com.onelinejournal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onelinejournal.ui.theme.AccentTheme

@Composable
fun ThemeColorMenu(
    selectedTheme: AccentTheme,
    onThemeSelected: (AccentTheme) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AccentTheme.values().forEach { theme ->
            val selected = theme == selectedTheme
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(theme.color)
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        },
                        shape = CircleShape
                    )
                    .clickable { onThemeSelected(theme) }
                    .semantics {
                        contentDescription = "${theme.label} theme${if (selected) ", selected" else ""}"
                    }
            )
        }
    }
}
