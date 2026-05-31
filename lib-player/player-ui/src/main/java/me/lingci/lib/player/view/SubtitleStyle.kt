package me.lingci.lib.player.view

import android.graphics.Color
import android.graphics.Typeface

internal data class SubtitleStyle(
    val foregroundColor: Int,
    val backgroundColor: Int,
    val windowColor: Int,
    val edgeType: Int,
    val edgeColor: Int,
    val edgeWidth: Float,
    val typeface: Typeface?
) {

    companion object {
        const val EDGE_PRESET_CLASSIC = 0
        const val EDGE_PRESET_SOFT = 1
        const val EDGE_PRESET_NAVY = 2
        const val EDGE_PRESET_WARM = 3

        const val EDGE_TYPE_NONE = 0
        const val EDGE_TYPE_OUTLINE = 1
        const val EDGE_TYPE_DROP_SHADOW = 2
        const val EDGE_TYPE_RAISED = 3
        const val EDGE_TYPE_DEPRESSED = 4

        const val DEFAULT_EDGE_WIDTH_DP = 5f

        const val EDGE_COLOR_CLASSIC = Color.BLACK
        val EDGE_COLOR_SOFT: Int = Color.parseColor("#1A1A1A")
        val EDGE_COLOR_NAVY: Int = Color.parseColor("#0B132B")
        val EDGE_COLOR_WARM: Int = Color.parseColor("#2F1B14")

        val DEFAULT = SubtitleStyle(
            foregroundColor = Color.WHITE,
            backgroundColor = Color.TRANSPARENT,
            windowColor = Color.TRANSPARENT,
            edgeType = EDGE_TYPE_NONE,
            edgeColor = Color.BLACK,
            edgeWidth = DEFAULT_EDGE_WIDTH_DP,
            typeface = null
        )

        fun getAnimeEdgeColor(preset: Int): Int {
            return when (preset) {
                EDGE_PRESET_SOFT -> EDGE_COLOR_SOFT
                EDGE_PRESET_NAVY -> EDGE_COLOR_NAVY
                EDGE_PRESET_WARM -> EDGE_COLOR_WARM
                else -> EDGE_COLOR_CLASSIC
            }
        }
    }

    fun mergeWith(overlay: SubtitleStyle): SubtitleStyle {
        return copy(
            foregroundColor = overlay.foregroundColor,
            backgroundColor = overlay.backgroundColor,
            windowColor = if (overlay.windowColor != Color.TRANSPARENT) overlay.windowColor else windowColor,
            edgeType = overlay.edgeType,
            edgeColor = overlay.edgeColor,
            edgeWidth = overlay.edgeWidth,
            typeface = overlay.typeface ?: typeface
        )
    }
}
