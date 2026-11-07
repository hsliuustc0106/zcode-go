package com.zcodego.app.ui.theme

import androidx.compose.ui.graphics.Color

internal val LightExtra = ZCodeExtraColors(
    backdropTop = Color(0xFFE4ECF8),
    backdropBottom = Color(0xFFF6F3FA),
    selectedCard = Color(0xFFF3F7FF),
)

internal val DarkExtra = ZCodeExtraColors(
    backdropTop = Color(0xFF0E1422),
    backdropBottom = Color(0xFF14141C),
    selectedCard = Color(0xFF18243C),
)

internal data class ZCodeExtraColors(
    val backdropTop: Color,
    val backdropBottom: Color,
    val selectedCard: Color,
)
