package me.lingci.lib.player.subtitle

import android.graphics.Bitmap
import android.text.Layout

/** Line positioning mode mirrored from common subtitle renderers without depending on Media3. */
enum class SubtitleLineType {
    UNSET,
    FRACTION,
    NUMBER,
}

/** Anchor point used when resolving cue line or horizontal position. */
enum class SubtitleAnchor {
    UNSET,
    START,
    MIDDLE,
    END,
}

/** Text size semantics for subtitle cues. */
enum class SubtitleTextSizeType {
    UNSET,
    FRACTIONAL,
    FRACTIONAL_IGNORE_PADDING,
    ABSOLUTE,
}

/** Vertical writing direction for subtitle cues. */
enum class SubtitleVerticalType {
    UNSET,
    HORIZONTAL,
    VERTICAL_RL,
    VERTICAL_LR,
}

/**
 * Backend-neutral subtitle cue used by player-ui. It intentionally mirrors the subset needed by
 * SubtitleView so Exo can map Media3 Cue objects without player-ui depending on Media3.
 */
data class SubtitleCue(
    val text: CharSequence? = null,
    val bitmap: Bitmap? = null,
    val textAlignment: Layout.Alignment? = null,
    val line: Float = DIMEN_UNSET,
    val lineType: SubtitleLineType = SubtitleLineType.UNSET,
    val lineAnchor: SubtitleAnchor = SubtitleAnchor.UNSET,
    val position: Float = DIMEN_UNSET,
    val positionAnchor: SubtitleAnchor = SubtitleAnchor.UNSET,
    val size: Float = DIMEN_UNSET,
    val bitmapHeight: Float = DIMEN_UNSET,
    val textSize: Float = DIMEN_UNSET,
    val textSizeType: SubtitleTextSizeType = SubtitleTextSizeType.UNSET,
    val verticalType: SubtitleVerticalType = SubtitleVerticalType.UNSET,
    val windowColor: Int? = null,
) {
    companion object {
        const val DIMEN_UNSET = -Float.MAX_VALUE

        val LINE_TYPE_UNSET = SubtitleLineType.UNSET
        val LINE_TYPE_FRACTION = SubtitleLineType.FRACTION
        val LINE_TYPE_NUMBER = SubtitleLineType.NUMBER

        val ANCHOR_TYPE_UNSET = SubtitleAnchor.UNSET
        val ANCHOR_TYPE_START = SubtitleAnchor.START
        val ANCHOR_TYPE_MIDDLE = SubtitleAnchor.MIDDLE
        val ANCHOR_TYPE_END = SubtitleAnchor.END

        val TEXT_SIZE_TYPE_UNSET = SubtitleTextSizeType.UNSET
        val TEXT_SIZE_TYPE_FRACTIONAL = SubtitleTextSizeType.FRACTIONAL
        val TEXT_SIZE_TYPE_FRACTIONAL_IGNORE_PADDING = SubtitleTextSizeType.FRACTIONAL_IGNORE_PADDING
        val TEXT_SIZE_TYPE_ABSOLUTE = SubtitleTextSizeType.ABSOLUTE

        val VERTICAL_TYPE_UNSET = SubtitleVerticalType.UNSET
        val VERTICAL_TYPE_HORIZONTAL = SubtitleVerticalType.HORIZONTAL
        val VERTICAL_TYPE_RL = SubtitleVerticalType.VERTICAL_RL
        val VERTICAL_TYPE_LR = SubtitleVerticalType.VERTICAL_LR
    }
}

/** A complete set of cues for one presentation timestamp. */
data class SubtitleCueGroup(
    val presentationTimeUs: Long,
    val cues: List<SubtitleCue> = emptyList(),
)
