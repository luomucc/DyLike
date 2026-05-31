package me.lingci.lib.player.view

import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.RelativeSizeSpan
import me.lingci.lib.player.subtitle.SubtitleCue
import me.lingci.lib.player.subtitle.SubtitleTextSizeType

internal object SubtitleViewUtils {
    fun resolveTextSize(
        textSizeType: SubtitleTextSizeType,
        textSize: Float,
        rawViewHeight: Int,
        viewHeightMinusPadding: Int
    ): Float {
        if (textSize == SubtitleCue.DIMEN_UNSET) {
            return SubtitleCue.DIMEN_UNSET
        }
        return when (textSizeType) {
            SubtitleTextSizeType.ABSOLUTE -> textSize
            SubtitleTextSizeType.FRACTIONAL -> textSize * viewHeightMinusPadding
            SubtitleTextSizeType.FRACTIONAL_IGNORE_PADDING -> textSize * rawViewHeight
            else -> SubtitleCue.DIMEN_UNSET
        }
    }

    fun removeAllEmbeddedStyling(cue: SubtitleCue): SubtitleCue {
        var result = cue.copy(
            windowColor = null,
            textSize = SubtitleCue.DIMEN_UNSET,
            textSizeType = SubtitleTextSizeType.UNSET
        )
        val text = cue.text
        if (text is Spanned) {
            val spannable = SpannableString.valueOf(text)
            removeSpansIf(spannable) { true }
            result = result.copy(text = spannable)
        }
        return result
    }

    fun removeEmbeddedFontSizes(cue: SubtitleCue): SubtitleCue {
        var result = cue.copy(
            textSize = SubtitleCue.DIMEN_UNSET,
            textSizeType = SubtitleTextSizeType.UNSET
        )
        val text = cue.text
        if (text is Spanned) {
            val spannable = SpannableString.valueOf(text)
            removeSpansIf(spannable) { span ->
                span is AbsoluteSizeSpan || span is RelativeSizeSpan
            }
            result = result.copy(text = spannable)
        }
        return result
    }

    fun clearWindowColor(cue: SubtitleCue): SubtitleCue {
        return cue.copy(windowColor = null)
    }

    private inline fun removeSpansIf(spannable: Spannable, removeFilter: (Any) -> Boolean) {
        val spans = spannable.getSpans(0, spannable.length, Any::class.java)
        spans.forEach { span ->
            if (removeFilter(span)) {
                spannable.removeSpan(span)
            }
        }
    }

}
