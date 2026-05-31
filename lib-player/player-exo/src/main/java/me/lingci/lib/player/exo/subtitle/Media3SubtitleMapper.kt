package me.lingci.lib.player.exo.subtitle

import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import me.lingci.lib.player.subtitle.SubtitleAnchor
import me.lingci.lib.player.subtitle.SubtitleCue
import me.lingci.lib.player.subtitle.SubtitleCueGroup
import me.lingci.lib.player.subtitle.SubtitleLineType
import me.lingci.lib.player.subtitle.SubtitleTextSizeType
import me.lingci.lib.player.subtitle.SubtitleVerticalType

object Media3SubtitleMapper {

    /** Maps Media3 subtitle callbacks to the generic model consumed by player-ui. */
    fun toSubtitleCueGroup(cueGroup: CueGroup): SubtitleCueGroup {
        return SubtitleCueGroup(
            presentationTimeUs = cueGroup.presentationTimeUs,
            cues = cueGroup.cues.map { it.toSubtitleCue() }
        )
    }

    private fun Cue.toSubtitleCue(): SubtitleCue {
        // Only copy fields that SubtitleView understands. Keeping this mapper narrow prevents
        // player-ui from depending on Media3 for rarely used cue attributes.
        return SubtitleCue(
            text = text,
            bitmap = bitmap,
            textAlignment = textAlignment,
            line = line,
            lineType = lineType.toSubtitleLineType(),
            lineAnchor = lineAnchor.toSubtitleAnchor(),
            position = position,
            positionAnchor = positionAnchor.toSubtitleAnchor(),
            size = size,
            bitmapHeight = bitmapHeight,
            textSize = textSize,
            textSizeType = textSizeType.toSubtitleTextSizeType(),
            verticalType = verticalType.toSubtitleVerticalType(),
            windowColor = if (windowColorSet) windowColor else null
        )
    }

    private fun Int.toSubtitleLineType(): SubtitleLineType {
        return when (this) {
            Cue.LINE_TYPE_FRACTION -> SubtitleLineType.FRACTION
            Cue.LINE_TYPE_NUMBER -> SubtitleLineType.NUMBER
            else -> SubtitleLineType.UNSET
        }
    }

    private fun Int.toSubtitleAnchor(): SubtitleAnchor {
        return when (this) {
            Cue.ANCHOR_TYPE_START -> SubtitleAnchor.START
            Cue.ANCHOR_TYPE_MIDDLE -> SubtitleAnchor.MIDDLE
            Cue.ANCHOR_TYPE_END -> SubtitleAnchor.END
            else -> SubtitleAnchor.UNSET
        }
    }

    private fun Int.toSubtitleTextSizeType(): SubtitleTextSizeType {
        return when (this) {
            Cue.TEXT_SIZE_TYPE_FRACTIONAL -> SubtitleTextSizeType.FRACTIONAL
            Cue.TEXT_SIZE_TYPE_FRACTIONAL_IGNORE_PADDING -> SubtitleTextSizeType.FRACTIONAL_IGNORE_PADDING
            Cue.TEXT_SIZE_TYPE_ABSOLUTE -> SubtitleTextSizeType.ABSOLUTE
            else -> SubtitleTextSizeType.UNSET
        }
    }

    private fun Int.toSubtitleVerticalType(): SubtitleVerticalType {
        return when (this) {
            Cue.VERTICAL_TYPE_LR -> SubtitleVerticalType.VERTICAL_LR
            Cue.VERTICAL_TYPE_RL -> SubtitleVerticalType.VERTICAL_RL
            // Media3's unset/horizontal values both use the normal SubtitleView path; only true
            // vertical directions need special fallback handling in player-ui.
            else -> SubtitleVerticalType.UNSET
        }
    }
}
