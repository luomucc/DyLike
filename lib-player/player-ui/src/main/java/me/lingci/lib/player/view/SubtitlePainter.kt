package me.lingci.lib.player.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import me.lingci.lib.base.util.Log
import me.lingci.lib.player.ui.BuildConfig
import me.lingci.lib.player.subtitle.SubtitleAnchor
import me.lingci.lib.player.subtitle.SubtitleCue
import me.lingci.lib.player.subtitle.SubtitleLineType
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

@SuppressLint("UseKtx", "ResourceType")
internal class SubtitlePainter(context: Context) {
    companion object {
        private const val INNER_PADDING_RATIO = 0.125f
        private const val SUBTITLE_TRACE_LAYOUT_TAG = "SubtitleTraceLayout"
        private const val SUBTITLE_TRACE_RENDER_COLOR_TAG = "SubtitleTraceRenderColor"
    }

    private data class TextLayoutCalculation(
        val textLayout: android.text.StaticLayout,
        val edgeLayout: android.text.StaticLayout,
        val textLeft: Int,
        val textTop: Int,
        val availableWidth: Int,
        val measuredWidth: Int,
        val measuredHeight: Int,
        val finalWidth: Int
    )

    private val shadowRadius: Float
    private val shadowOffset: Float
    private val spacingMult: Float
    private val spacingAdd: Float

    private val textPaint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        isSubpixelText = true
    }
    private val windowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }

    private var lastCue: SubtitleCue? = null
    private var lastStyle: SubtitleStyle? = null
    private var lastDefaultTextSizePx = Float.MIN_VALUE
    private var lastCueTextSizePx = Float.MIN_VALUE
    private var lastBottomPaddingFraction = Float.MIN_VALUE
    private var lastDockMode = Int.MIN_VALUE
    private var lastCueBoxLeft = Int.MIN_VALUE
    private var lastCueBoxTop = Int.MIN_VALUE
    private var lastCueBoxRight = Int.MIN_VALUE
    private var lastCueBoxBottom = Int.MIN_VALUE
    private var resolvedWindowColor = Color.TRANSPARENT

    private var textLayout: android.text.StaticLayout? = null
    private var edgeLayout: android.text.StaticLayout? = null
    private var textLeft = 0
    private var textTop = 0
    private var textPaddingX = 0
    private var bitmapRect: android.graphics.Rect? = null
    private var traceEnabledForLastLayout = false
    private var traceFrameTimeUs = Long.MIN_VALUE

    init {
        val attrs = context.obtainStyledAttributes(
            null,
            intArrayOf(android.R.attr.lineSpacingExtra, android.R.attr.lineSpacingMultiplier),
            0,
            0
        )
        spacingAdd = attrs.getDimensionPixelSize(0, 0).toFloat()
        spacingMult = attrs.getFloat(1, 1f)
        attrs.recycle()

        val twoDp = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            2f,
            context.resources.displayMetrics
        )
        shadowRadius = twoDp
        shadowOffset = twoDp
    }

    fun draw(
        cue: SubtitleCue,
        style: SubtitleStyle,
        defaultTextSizePx: Float,
        cueTextSizePx: Float,
        bottomPaddingFraction: Float,
        dockMode: Int,
        frameTimeUs: Long,
        traceEnabled: Boolean,
        canvas: Canvas,
        cueBoxLeft: Int,
        cueBoxTop: Int,
        cueBoxRight: Int,
        cueBoxBottom: Int
    ) {
        if (cue.bitmap == null && cue.text.isNullOrEmpty()) {
            return
        }
        resolvedWindowColor = if (cue.bitmap == null && cue.windowColor != null && Color.alpha(style.windowColor) == 0) {
            cue.windowColor ?: Color.TRANSPARENT
        } else {
            style.windowColor
        }
        if (isCached(
                cue,
                style,
                defaultTextSizePx,
                cueTextSizePx,
                bottomPaddingFraction,
                dockMode,
                cueBoxLeft,
                cueBoxTop,
                cueBoxRight,
                cueBoxBottom
            )
        ) {
            drawLayout(canvas, cue.bitmap != null, style)
            return
        }

        lastCue = cue
        lastStyle = style
        lastDefaultTextSizePx = defaultTextSizePx
        lastCueTextSizePx = cueTextSizePx
        lastBottomPaddingFraction = bottomPaddingFraction
        lastDockMode = dockMode
        lastCueBoxLeft = cueBoxLeft
        lastCueBoxTop = cueBoxTop
        lastCueBoxRight = cueBoxRight
        lastCueBoxBottom = cueBoxBottom
        traceEnabledForLastLayout = traceEnabled
        traceFrameTimeUs = frameTimeUs
        textPaint.typeface = style.typeface

        if (cue.bitmap == null) {
            setupTextLayout(
                cue,
                style,
                defaultTextSizePx,
                cueTextSizePx,
                bottomPaddingFraction,
                dockMode,
                cueBoxLeft,
                cueBoxTop,
                cueBoxRight,
                cueBoxBottom
            )
        } else {
            setupBitmapLayout(cue, cueBoxLeft, cueBoxTop, cueBoxRight, cueBoxBottom)
        }
        drawLayout(canvas, cue.bitmap != null, style)
    }

    private fun isCached(
        cue: SubtitleCue,
        style: SubtitleStyle,
        defaultTextSizePx: Float,
        cueTextSizePx: Float,
        bottomPaddingFraction: Float,
        dockMode: Int,
        cueBoxLeft: Int,
        cueBoxTop: Int,
        cueBoxRight: Int,
        cueBoxBottom: Int
    ): Boolean {
        return cue == lastCue &&
            style == lastStyle &&
            defaultTextSizePx == lastDefaultTextSizePx &&
            cueTextSizePx == lastCueTextSizePx &&
            bottomPaddingFraction == lastBottomPaddingFraction &&
            dockMode == lastDockMode &&
            cueBoxLeft == lastCueBoxLeft &&
            cueBoxTop == lastCueBoxTop &&
            cueBoxRight == lastCueBoxRight &&
            cueBoxBottom == lastCueBoxBottom
    }

    private fun setupTextLayout(
        cue: SubtitleCue,
        style: SubtitleStyle,
        defaultTextSizePx: Float,
        cueTextSizePx: Float,
        bottomPaddingFraction: Float,
        dockMode: Int,
        cueBoxLeft: Int,
        cueBoxTop: Int,
        cueBoxRight: Int,
        cueBoxBottom: Int
    ) {
        val calculation = calculateTextLayout(
            cue = cue,
            style = style,
            defaultTextSizePx = defaultTextSizePx,
            cueTextSizePx = cueTextSizePx,
            bottomPaddingFraction = bottomPaddingFraction,
            dockMode = dockMode,
            cueBoxLeft = cueBoxLeft,
            cueBoxTop = cueBoxTop,
            cueBoxRight = cueBoxRight,
            cueBoxBottom = cueBoxBottom
        )
        if (calculation == null) {
            textLayout = null
            edgeLayout = null
            return
        }

        textLayout = calculation.textLayout
        edgeLayout = calculation.edgeLayout
        textLeft = calculation.textLeft
        textTop = calculation.textTop
        bitmapRect = null
        traceTextLayout(
            cue,
            style,
            calculation.availableWidth,
            calculation.measuredWidth,
            calculation.measuredHeight,
            calculation.finalWidth,
            calculation.textTop
        )
    }

    private fun buildStaticLayout(
        text: CharSequence,
        width: Int,
        alignment: Layout.Alignment
    ): android.text.StaticLayout {
        return android.text.StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, width)
            .setAlignment(alignment)
            .setLineSpacing(spacingAdd, spacingMult)
            .setIncludePad(true)
            .build()
    }

    private fun setupBitmapLayout(
        cue: SubtitleCue,
        cueBoxLeft: Int,
        cueBoxTop: Int,
        cueBoxRight: Int,
        cueBoxBottom: Int
    ) {
        bitmapRect = calculateBitmapRect(cue, cueBoxLeft, cueBoxTop, cueBoxRight, cueBoxBottom)
        textLayout = null
        edgeLayout = null
    }

    private fun drawLayout(canvas: Canvas, isBitmapCue: Boolean, style: SubtitleStyle) {
        if (isBitmapCue) {
            val bitmap = lastCue?.bitmap ?: return
            val rect = bitmapRect ?: return
            canvas.drawBitmap(bitmap, null, rect, bitmapPaint)
            return
        }

        val textLayout = textLayout ?: return
        val edgeLayout = edgeLayout ?: return
        val saveCount = canvas.save()
        canvas.translate(textLeft.toFloat(), textTop.toFloat())

        if (Color.alpha(resolvedWindowColor) > 0) {
            windowPaint.color = resolvedWindowColor
            canvas.drawRect(
                -textPaddingX.toFloat(),
                0f,
                (textLayout.width + textPaddingX).toFloat(),
                textLayout.height.toFloat(),
                windowPaint
            )
        }

        val effectiveEdgeType = resolveEffectiveEdgeType(
            style = style,
            isBitmapCue = isBitmapCue,
            cueText = lastCue?.text,
            resolvedWindowColor = resolvedWindowColor
        )
        // 这里必须使用“最终会绘制的边框类型”，否则 ASS 内嵌前景色和 edge 图层的处理会不一致。
        traceRenderColors(style, effectiveEdgeType)
        when (effectiveEdgeType) {
            SubtitleStyle.EDGE_TYPE_OUTLINE -> {
                textPaint.strokeJoin = Paint.Join.ROUND
                textPaint.strokeWidth = style.edgeWidth.coerceAtLeast(0f)
                textPaint.color = style.edgeColor
                textPaint.style = Paint.Style.FILL_AND_STROKE
                edgeLayout.draw(canvas)
            }
            SubtitleStyle.EDGE_TYPE_DROP_SHADOW -> {
                textPaint.setShadowLayer(shadowRadius, shadowOffset, shadowOffset, style.edgeColor)
            }
            SubtitleStyle.EDGE_TYPE_RAISED,
            SubtitleStyle.EDGE_TYPE_DEPRESSED -> {
                val raised = style.edgeType == SubtitleStyle.EDGE_TYPE_RAISED
                val colorUp = if (raised) Color.WHITE else style.edgeColor
                val colorDown = if (raised) style.edgeColor else Color.WHITE
                val offset = shadowRadius / 2f
                textPaint.color = style.foregroundColor
                textPaint.style = Paint.Style.FILL
                textPaint.setShadowLayer(shadowRadius, -offset, -offset, colorUp)
                edgeLayout.draw(canvas)
                textPaint.setShadowLayer(shadowRadius, offset, offset, colorDown)
            }
        }

        textPaint.color = style.foregroundColor
        textPaint.style = Paint.Style.FILL
        textLayout.draw(canvas)
        textPaint.setShadowLayer(0f, 0f, 0f, 0)
        canvas.restoreToCount(saveCount)
    }

    private fun resolveEffectiveEdgeType(
        style: SubtitleStyle,
        isBitmapCue: Boolean,
        cueText: CharSequence?,
        resolvedWindowColor: Int
    ): Int {
        if (isBitmapCue || style.edgeType != SubtitleStyle.EDGE_TYPE_NONE) {
            return style.edgeType
        }
        if (Color.alpha(style.backgroundColor) > 0 || Color.alpha(resolvedWindowColor) > 0) {
            return SubtitleStyle.EDGE_TYPE_NONE
        }
        if (cueText.isNullOrEmpty()) {
            return SubtitleStyle.EDGE_TYPE_NONE
        }
        return SubtitleStyle.EDGE_TYPE_OUTLINE
    }

    fun getLastDrawBounds(): Rect? {
        val layout = textLayout
        if (layout != null) {
            return Rect(textLeft, textTop, textLeft + layout.width, textTop + layout.height)
        }
        return bitmapRect?.let { Rect(it) }
    }

    fun measureCueBounds(
        cue: SubtitleCue,
        style: SubtitleStyle,
        defaultTextSizePx: Float,
        cueTextSizePx: Float,
        bottomPaddingFraction: Float,
        dockMode: Int,
        cueBoxLeft: Int,
        cueBoxTop: Int,
        cueBoxRight: Int,
        cueBoxBottom: Int
    ): Rect? {
        if (cue.bitmap == null && cue.text.isNullOrEmpty()) {
            return null
        }
        textPaint.typeface = style.typeface
        return if (cue.bitmap == null) {
            val calculation = calculateTextLayout(
                cue = cue,
                style = style,
                defaultTextSizePx = defaultTextSizePx,
                cueTextSizePx = cueTextSizePx,
                bottomPaddingFraction = bottomPaddingFraction,
                dockMode = dockMode,
                cueBoxLeft = cueBoxLeft,
                cueBoxTop = cueBoxTop,
                cueBoxRight = cueBoxRight,
                cueBoxBottom = cueBoxBottom
            ) ?: return null
            Rect(
                calculation.textLeft,
                calculation.textTop,
                calculation.textLeft + calculation.textLayout.width,
                calculation.textTop + calculation.textLayout.height
            )
        } else {
            calculateBitmapRect(cue, cueBoxLeft, cueBoxTop, cueBoxRight, cueBoxBottom)
        }
    }

    private fun calculateTextLayout(
        cue: SubtitleCue,
        style: SubtitleStyle,
        defaultTextSizePx: Float,
        cueTextSizePx: Float,
        bottomPaddingFraction: Float,
        dockMode: Int,
        cueBoxLeft: Int,
        cueBoxTop: Int,
        cueBoxRight: Int,
        cueBoxBottom: Int
    ): TextLayoutCalculation? {
        val cueText = cue.text ?: return null
        val cueTextBuilder = android.text.SpannableStringBuilder.valueOf(cueText)
        val edgeTextBuilder = android.text.SpannableStringBuilder.valueOf(cueText)
        val parentWidth = cueBoxRight - cueBoxLeft
        val parentHeight = cueBoxBottom - cueBoxTop
        val cueWindowColor = cue.windowColor ?: style.windowColor
        val effectiveEdgeType = resolveEffectiveEdgeType(
            style = style,
            isBitmapCue = false,
            cueText = cueText,
            resolvedWindowColor = cueWindowColor
        )

        textPaint.textSize = defaultTextSizePx
        textPaddingX = (defaultTextSizePx * INNER_PADDING_RATIO + 0.5f).toInt()

        var availableWidth = parentWidth - textPaddingX * 2
        if (cue.size != SubtitleCue.DIMEN_UNSET) {
            availableWidth = (availableWidth * cue.size).toInt()
        }
        if (availableWidth <= 0) {
            return null
        }

        if (cueTextSizePx > 0 && cueTextSizePx.isFinite() && cueTextSizePx < 1000f) {
            cueTextBuilder.setSpan(
                android.text.style.AbsoluteSizeSpan(cueTextSizePx.toInt()),
                0,
                cueTextBuilder.length,
                android.text.Spanned.SPAN_PRIORITY
            )
            edgeTextBuilder.setSpan(
                android.text.style.AbsoluteSizeSpan(cueTextSizePx.toInt()),
                0,
                edgeTextBuilder.length,
                android.text.Spanned.SPAN_PRIORITY
            )
        } else if (cueTextSizePx > 0) {
            Log.d(
                SUBTITLE_TRACE_LAYOUT_TAG,
                "frameUs=$traceFrameTimeUs",
                "text=${previewCueText(cue.text)}",
                "cueTextSizePx=$cueTextSizePx",
                "defaultTextSizePx=$defaultTextSizePx",
                "reason=invalid-cue-text-size-fallback-skipped"
            )
        }

        // ASS 的白字白边问题常出在 edge 图层沿用了嵌入前景色，这里按 effectiveEdgeType 决定是否清理前景 span。
        if (effectiveEdgeType == SubtitleStyle.EDGE_TYPE_OUTLINE) {
            edgeTextBuilder.getSpans(
                0,
                edgeTextBuilder.length,
                android.text.style.ForegroundColorSpan::class.java
            ).forEach(edgeTextBuilder::removeSpan)
        }

        if (Color.alpha(style.backgroundColor) > 0) {
            val backgroundSpan = android.text.style.BackgroundColorSpan(style.backgroundColor)
            // 无描边/阴影时把背景挂到正文图层；有描边时挂到 edge 图层，避免正文和描边背景重复叠加。
            if (effectiveEdgeType == SubtitleStyle.EDGE_TYPE_NONE || effectiveEdgeType == SubtitleStyle.EDGE_TYPE_DROP_SHADOW) {
                cueTextBuilder.setSpan(
                    backgroundSpan,
                    0,
                    cueTextBuilder.length,
                    android.text.Spanned.SPAN_PRIORITY
                )
            } else {
                edgeTextBuilder.setSpan(
                    backgroundSpan,
                    0,
                    edgeTextBuilder.length,
                    android.text.Spanned.SPAN_PRIORITY
                )
            }
        }

        val textAlignment = cue.textAlignment ?: Layout.Alignment.ALIGN_CENTER
        if (BuildConfig.DEBUG && traceEnabledForLastLayout) {
            Log.d(
                SUBTITLE_TRACE_LAYOUT_TAG,
                "frameUs=$traceFrameTimeUs",
                "text=${previewCueText(cue.text)}",
                "availableWidth=$availableWidth",
                "cueTextSizePx=$cueTextSizePx",
                "defaultTextSizePx=$defaultTextSizePx",
                "textPaintSize=${textPaint.textSize}",
                "spacingMult=$spacingMult",
                "spacingAdd=$spacingAdd",
                "textLength=${cueTextBuilder.length}"
            )
        }
        val probeLayout = buildStaticLayout(cueTextBuilder, availableWidth, textAlignment)
        val measuredHeight = probeLayout.height
        if (measuredHeight <= 0 || measuredHeight > parentHeight * 10) {
            Log.d(
                SUBTITLE_TRACE_LAYOUT_TAG,
                "frameUs=$traceFrameTimeUs",
                "text=${previewCueText(cue.text)}",
                "measuredHeight=$measuredHeight",
                "parentHeight=$parentHeight",
                "reason=invalid-layout-height"
            )
            return null
        }
        var measuredWidth = 0
        for (index in 0 until probeLayout.lineCount) {
            measuredWidth = max(measuredWidth, ceil(probeLayout.getLineWidth(index).toDouble()).toInt())
        }
        if (cue.size != SubtitleCue.DIMEN_UNSET && measuredWidth < availableWidth) {
            measuredWidth = availableWidth
        }
        measuredWidth += textPaddingX * 2

        val layoutLeft: Int
        val layoutRight: Int
        if (cue.position != SubtitleCue.DIMEN_UNSET) {
            val anchorPosition = (parentWidth * cue.position).toInt() + cueBoxLeft
            val resolvedLeft = when (cue.positionAnchor) {
                SubtitleAnchor.END -> anchorPosition - measuredWidth
                SubtitleAnchor.MIDDLE -> (anchorPosition * 2 - measuredWidth) / 2
                else -> anchorPosition
            }
            layoutLeft = resolvedLeft.coerceAtLeast(cueBoxLeft)
            layoutRight = (layoutLeft + measuredWidth).coerceAtMost(cueBoxRight)
        } else {
            layoutLeft = (parentWidth - measuredWidth) / 2 + cueBoxLeft
            layoutRight = layoutLeft + measuredWidth
        }

        val finalWidth = layoutRight - layoutLeft
        if (finalWidth <= 0) {
            return null
        }

        val rawLayoutTop = if (cue.line != SubtitleCue.DIMEN_UNSET) {
            if (cue.lineType == SubtitleLineType.FRACTION) {
                val anchorPosition = (parentHeight * cue.line).toInt() + cueBoxTop
                when (cue.lineAnchor) {
                    SubtitleAnchor.END -> anchorPosition - measuredHeight
                    SubtitleAnchor.MIDDLE -> (anchorPosition * 2 - measuredHeight) / 2
                    else -> anchorPosition
                }
            } else {
                val firstLineHeight = probeLayout.getLineBottom(0) - probeLayout.getLineTop(0)
                if (cue.line >= 0f) {
                    (cue.line * firstLineHeight).toInt() + cueBoxTop
                } else {
                    ((cue.line + 1f) * firstLineHeight).toInt() + cueBoxBottom - measuredHeight
                }
            }
        } else if (dockMode == SubtitleView.DOCK_MODE_BOTTOM_BAR) {
            cueBoxTop + ((parentHeight - measuredHeight) * 0.35f).toInt()
        } else {
            cueBoxBottom - measuredHeight - (parentHeight * bottomPaddingFraction).toInt()
        }
        
        val layoutTop = if (rawLayoutTop < cueBoxTop - parentHeight || rawLayoutTop > cueBoxBottom + parentHeight) {
            val fallbackTop = cueBoxBottom - measuredHeight - (parentHeight * bottomPaddingFraction).toInt()
            Log.d(
                SUBTITLE_TRACE_LAYOUT_TAG,
                "frameUs=$traceFrameTimeUs",
                "text=${previewCueText(cue.text)}",
                "rawLayoutTop=$rawLayoutTop",
                "fallbackTop=$fallbackTop",
                "reason=out-of-bounds-layout-top"
            )
            fallbackTop.coerceIn(cueBoxTop, cueBoxBottom - measuredHeight)
        } else {
            rawLayoutTop.coerceIn(cueBoxTop, cueBoxBottom - measuredHeight)
        }

        val textLayout = buildStaticLayout(cueTextBuilder, finalWidth, textAlignment)
        val edgeLayout = buildStaticLayout(edgeTextBuilder, finalWidth, textAlignment)
        return TextLayoutCalculation(
            textLayout = textLayout,
            edgeLayout = edgeLayout,
            textLeft = layoutLeft,
            textTop = layoutTop,
            availableWidth = availableWidth,
            measuredWidth = measuredWidth,
            measuredHeight = measuredHeight,
            finalWidth = finalWidth
        )
    }

    private fun calculateBitmapRect(
        cue: SubtitleCue,
        cueBoxLeft: Int,
        cueBoxTop: Int,
        cueBoxRight: Int,
        cueBoxBottom: Int
    ): Rect? {
        val bitmap = cue.bitmap ?: return null
        val parentWidth = cueBoxRight - cueBoxLeft
        val parentHeight = cueBoxBottom - cueBoxTop
        val cuePosition = if (cue.position != SubtitleCue.DIMEN_UNSET) cue.position else 0.5f
        val cueLine = if (cue.line != SubtitleCue.DIMEN_UNSET) cue.line else 0.9f
        val cueWidth = if (cue.size != SubtitleCue.DIMEN_UNSET) cue.size else 0.8f
        val anchorX = cueBoxLeft + parentWidth * cuePosition
        val anchorY = cueBoxTop + parentHeight * cueLine
        val width = max(1, (parentWidth * cueWidth).toInt())
        val height = if (cue.bitmapHeight != SubtitleCue.DIMEN_UNSET) {
            (parentHeight * cue.bitmapHeight).toInt()
        } else {
            (width * (bitmap.height.toFloat() / bitmap.width.toFloat())).toInt()
        }
        val x = when (cue.positionAnchor) {
            SubtitleAnchor.END -> (anchorX - width).toInt()
            SubtitleAnchor.MIDDLE -> (anchorX - width / 2f).toInt()
            else -> anchorX.toInt()
        }
        val y = when (cue.lineAnchor) {
            SubtitleAnchor.END -> (anchorY - height).toInt()
            SubtitleAnchor.MIDDLE -> (anchorY - height / 2f).toInt()
            else -> anchorY.toInt()
        }
        return Rect(x, y, x + width, y + height)
    }

    private fun traceTextLayout(
        cue: SubtitleCue,
        style: SubtitleStyle,
        availableWidth: Int,
        measuredWidth: Int,
        measuredHeight: Int,
        finalWidth: Int,
        layoutTop: Int
    ) {
        if (!BuildConfig.DEBUG || !traceEnabledForLastLayout) {
            return
        }
        val layout = textLayout ?: return
        val effectiveEdgeType = resolveEffectiveEdgeType(
            style = style,
            isBitmapCue = false,
            cueText = cue.text,
            resolvedWindowColor = cue.windowColor ?: style.windowColor
        )
        Log.d(
            SUBTITLE_TRACE_LAYOUT_TAG,
            "frameUs=$traceFrameTimeUs",
            "text=${previewCueText(cue.text)}",
            "foregroundSpans=${summarizeForegroundColors(cue.text)}",
            "availableWidth=$availableWidth",
            "measuredWidth=$measuredWidth",
            "measuredHeight=$measuredHeight",
            "finalWidth=$finalWidth",
            "layout=[${textLeft},$layoutTop,${textLeft + layout.width},${layoutTop + layout.height}]",
            "lineCount=${layout.lineCount}",
            "effectiveEdgeType=$effectiveEdgeType",
            "foreground=${formatColor(style.foregroundColor)}",
            "edge=${formatColor(style.edgeColor)}"
        )
        if (layout.lineCount > 0) {
            val lineMetrics = (0 until layout.lineCount).joinToString(prefix = "[", postfix = "]") { index ->
                "#$index:${layout.getLineTop(index)}-${layout.getLineBottom(index)}/${ceil(layout.getLineWidth(index).toDouble()).toInt()}"
            }
            Log.d(SUBTITLE_TRACE_LAYOUT_TAG, "frameUs=$traceFrameTimeUs", "lines=$lineMetrics")
        }
    }

    private fun traceRenderColors(style: SubtitleStyle, effectiveEdgeType: Int) {
        if (!BuildConfig.DEBUG || !traceEnabledForLastLayout) {
            return
        }
        val cue = lastCue ?: return
        val textLayout = textLayout ?: return
        val edgeLayout = edgeLayout ?: return
        Log.d(
            SUBTITLE_TRACE_RENDER_COLOR_TAG,
            "frameUs=$traceFrameTimeUs",
            "text=${previewCueText(cue.text)}",
            "effectiveEdgeType=$effectiveEdgeType",
            "styleForeground=${formatColor(style.foregroundColor)}",
            "styleEdge=${formatColor(style.edgeColor)}",
            "window=${formatColor(resolvedWindowColor)}",
            "edgeStrokeWidth=${String.format(Locale.US, "%.2f", style.edgeWidth.coerceAtLeast(0f))}",
            "textLayoutSize=${textLayout.width}x${textLayout.height}",
            "edgeLayoutSize=${edgeLayout.width}x${edgeLayout.height}"
        )
    }

    private fun previewCueText(text: CharSequence?): String {
        val normalized = text?.toString()?.replace("\n", "\\n")?.replace("\r", "") ?: "<null>"
        return if (normalized.length <= 80) normalized else normalized.take(77) + "..."
    }

    private fun formatColor(color: Int): String {
        return String.format(Locale.US, "#%08X", color)
    }

    private fun summarizeForegroundColors(text: CharSequence?): String {
        val spanned = text as? Spanned ?: return "[]"
        val spans = spanned.getSpans(0, spanned.length, ForegroundColorSpan::class.java)
        if (spans.isEmpty()) {
            return "[]"
        }
        return spans.joinToString(prefix = "[", postfix = "]") { formatColor(it.foregroundColor) }
    }

}
