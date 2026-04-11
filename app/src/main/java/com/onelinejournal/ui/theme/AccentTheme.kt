package com.onelinejournal.ui.theme

import androidx.compose.ui.graphics.Color

enum class AccentTheme(
    val label: String,
    val color: Color
) {
    Green("Green", SageGreen),
    Blue("Blue", Color(0xFF2F6FBA)),
    Purple("Purple", Color(0xFF7E57C2)),
    Orange("Orange", Color(0xFFE07A2F)),
    Pink("Pink", Color(0xFFD94F8C)),
    Monochrome("Monochrome", Color(0xFF111111)),
    Teal("Teal", Color(0xFF00897B));

    companion object {
        fun fromName(name: String?): AccentTheme {
            return values().firstOrNull { it.name == name } ?: Green
        }
    }
}
