package me.lingci.lib.player.view

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.accessibility.CaptioningManager
import me.lingci.lib.base.util.Log
import me.lingci.lib.base.util.dp
import me.lingci.lib.player.ui.BuildConfig
import me.lingci.lib.player.ui.R
import me.lingci.lib.player.subtitle.SubtitleAnchor
import me.lingci.lib.player.subtitle.SubtitleCue
import me.lingci.lib.player.subtitle.SubtitleCueGroup
import me.lingci.lib.player.subtitle.SubtitleLineType
import me.lingci.lib.player.subtitle.SubtitleTextSizeType
import me.lingci.lib.player.subtitle.SubtitleVerticalType
import java.util.Locale
import kotlin.math.max

/**
 * Generic cue renderer that keeps subtitle rendering inside player-ui.
 *
 * Backends map their native cue formats to SubtitleCue before reaching this view, so this renderer
 * can evolve independently from Media3/MPV/IJK dependencies.
 */
class SubtitleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_TEXT_SIZE_FRACTION = 0.0533f
        private const val DEFAULT_TEXT_SIZE_DP = 24f
        private const val DEFAULT_BOTTOM_PADDING_FRACTION = 0.08f
        private const val SUBTITLE_TRACE_PREPARE_TAG = "SubtitleTracePrepare"
        private const val SUBTITLE_GEOMETRY_TRACE_TAG = "SubtitleGeometryTrace"
        private const val SUBTITLE_TRACE_VISIBILITY_TAG = "SubtitleVisibilityTrace"
        // 放宽黑/白描边回退区间，让更多浅色直接走黑边、更多深色直接走白边，减少中间色误判成棕边。
        private const val COLOR_LUMINANCE_WHITE_THRESHOLD = 0.72f
        private const val COLOR_LUMINANCE_BLACK_THRESHOLD = 0.28f
        // 对比阈值也同步提高，避免深色文字沿用不够亮的边色时区分度不明显。
        private const val COLOR_CONTRAST_THRESHOLD = 0.45f
        private const val MIN_VISIBLE_TEXT_ALPHA = 32
        private const val MIN_VISIBLE_TEXT_CONTRAST = 0.08f
        // 棕色 #8A6B1F -> #4A341C 默认金色
        private val DEFAULT_MID_TONE_EDGE_COLOR: Int = Color.parseColor("#ffffff")

        const val DOCK_MODE_NORMAL = 0
        const val DOCK_MODE_BOTTOM_BAR = 1
    }

    private data class PreparedCueState(
        val originalCue: SubtitleCue,
        val sanitizedCue: SubtitleCue,
        val drawCue: SubtitleCue,
        val autoStackReason: String
    )

    private data class DrawStyleDecision(
        val drawStyle: SubtitleStyle,
        val resolvedTextColor: Int,
        val edgeFallbackMode: String
    )

    private data class RenderCueState(
        val preparedCue: PreparedCueState,
        val drawStyleDecision: DrawStyleDecision
    )

    private data class DrawnCueState(
        val index: Int,
        val textPreview: String,
        val positionKey: String,
        val bounds: Rect
    )

    private val painters = mutableListOf<SubtitlePainter>()
    private var currentCues: List<SubtitleCue> = emptyList()
    private var style = SubtitleStyle.DEFAULT
    private var defaultTextSizeType = SubtitleTextSizeType.ABSOLUTE
    private var defaultTextSize = 16f.dp
    private var bottomPaddingFraction = DEFAULT_BOTTOM_PADDING_FRACTION
    private var applyEmbeddedStyles = true
    private var applyEmbeddedFontSizes = true
    private var applyEmbeddedWindowColor = true
    private var layoutBounds: Rect? = null
    // Short-video mode can dock subtitles to a bottom bar that is smaller than the whole player.
    private var dockMode = DOCK_MODE_NORMAL
    private var currentCuePresentationTimeUs = Long.MIN_VALUE
    private var adaptiveMidToneEdgeColor = DEFAULT_MID_TONE_EDGE_COLOR
    private var subtitleScale = 1.0f
    private var autoShrinkFactor = 1.0f
    private var autoShrinkDirty = false
    private val shrinkPaint = android.text.TextPaint()

    init {
        initAttrs(attrs)
        updateUserCaptionPreferences()
    }

    private fun initAttrs(attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SubtitleView)
        val textSizePx = ta.getDimension(R.styleable.SubtitleView_subtitleTextSize, 16f.dp)
        val textColor = ta.getColor(R.styleable.SubtitleView_subtitleTextColor, Color.WHITE)
        val backgroundColor = ta.getColor(R.styleable.SubtitleView_subtitleBackgroundColor, Color.TRANSPARENT)
        val edgeType = ta.getInt(R.styleable.SubtitleView_subtitleEdgeType, SubtitleStyle.EDGE_TYPE_NONE)
        val edgeColor = ta.getColor(R.styleable.SubtitleView_subtitleEdgeColor, Color.BLACK)
        val edgeWidth = ta.getDimension(
            R.styleable.SubtitleView_subtitleEdgeWidth,
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                SubtitleStyle.DEFAULT_EDGE_WIDTH_DP,
                resources.displayMetrics
            )
        )
        ta.recycle()

        defaultTextSizeType = SubtitleTextSizeType.ABSOLUTE
        defaultTextSize = textSizePx
        style = style.copy(
            foregroundColor = textColor,
            backgroundColor = backgroundColor,
            edgeType = edgeType,
            edgeColor = edgeColor,
            edgeWidth = edgeWidth
        )
    }

    fun setCues(cueGroup: SubtitleCueGroup) {
        // Keep presentation time for trace logs. Rendering only needs cue geometry/text.
        currentCuePresentationTimeUs = cueGroup.presentationTimeUs
        setCuesInternal(cueGroup.cues)
    }

    fun setCues(cues: List<SubtitleCue>?) {
        currentCuePresentationTimeUs = Long.MIN_VALUE
        setCuesInternal(cues)
    }

    private fun setCuesInternal(cues: List<SubtitleCue>?) {
        currentCues = cues ?: emptyList()
        autoShrinkDirty = true
        ensurePainterCount(currentCues.size)
        invalidate()
    }

    fun setTextSize(size: Float) {
        defaultTextSizeType = SubtitleTextSizeType.ABSOLUTE
        defaultTextSize = size
        invalidate()
    }

    fun setTextColor(color: Int) {
        style = style.copy(foregroundColor = color)
        invalidate()
    }

    fun setTypeface(typeface: android.graphics.Typeface?) {
        style = style.copy(typeface = typeface)
        invalidate()
    }

    fun setEdgeColor(color: Int) {
        style = style.copy(edgeColor = color)
        invalidate()
    }

    fun setEdgeWidth(widthPx: Float) {
        style = style.copy(edgeWidth = widthPx.coerceAtLeast(0f))
        invalidate()
    }

    fun setEdgeWidthDp(widthDp: Float) {
        setEdgeWidth(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                widthDp,
                resources.displayMetrics
            )
        )
    }

    fun setWindowColor(color: Int) {
        style = style.copy(windowColor = color)
        invalidate()
    }

    fun setEdgeType(edgeType: Int) {
        style = style.copy(edgeType = edgeType)
        invalidate()
    }

    fun setAnimeEdgePreset(preset: Int) {
        setEdgeColor(SubtitleStyle.getAnimeEdgeColor(preset))
    }

    fun enableAnimeOutlinePreset(preset: Int, widthDp: Float = SubtitleStyle.DEFAULT_EDGE_WIDTH_DP) {
        style = style.copy(
            edgeType = SubtitleStyle.EDGE_TYPE_OUTLINE,
            edgeColor = SubtitleStyle.getAnimeEdgeColor(preset),
            edgeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                widthDp,
                resources.displayMetrics
            )
        )
        invalidate()
    }

    fun setAdaptiveMidToneEdgeColor(color: Int) {
        adaptiveMidToneEdgeColor = color
        invalidate()
    }

    internal fun setStyle(subtitleStyle: SubtitleStyle) {
        style = subtitleStyle
        invalidate()
    }

    fun setBottomPaddingFraction(fraction: Float) {
        bottomPaddingFraction = fraction.coerceIn(0f, 1f)
        invalidate()
    }

    fun setLayoutBounds(left: Int, top: Int, right: Int, bottom: Int) {
        // Bounds constrain cue placement without resizing the view; short-video uses this to dock
        // subtitles above bottom controls.
        layoutBounds = Rect(left, top, right, bottom)
        invalidate()
    }

    fun clearLayoutBounds() {
        layoutBounds = null
        invalidate()
    }

    fun setDockMode(mode: Int) {
        dockMode = mode
        invalidate()
    }

    fun getDockMode(): Int {
        return dockMode
    }

    fun describeDockMode(mode: Int = dockMode): String {
        return when (mode) {
            DOCK_MODE_BOTTOM_BAR -> "bottom_bar"
            else -> "normal"
        }
    }

    fun setApplyEmbeddedStyles(apply: Boolean) {
        applyEmbeddedStyles = apply
        invalidate()
    }

    fun setApplyEmbeddedFontSizes(apply: Boolean) {
        applyEmbeddedFontSizes = apply
        invalidate()
    }

    fun setApplyEmbeddedWindowColor(apply: Boolean) {
        applyEmbeddedWindowColor = apply
        invalidate()
    }

    fun setSubtitleScale(scale: Float) {
        subtitleScale = scale.coerceAtLeast(0.1f)
        invalidate()
    }

    fun getSubtitleScale(): Float {
        return subtitleScale
    }

    fun setFractionalTextSize(fractionOfHeight: Float, ignorePadding: Boolean = false) {
        defaultTextSizeType = if (ignorePadding) {
            SubtitleTextSizeType.FRACTIONAL_IGNORE_PADDING
        } else {
            SubtitleTextSizeType.FRACTIONAL
        }
        defaultTextSize = fractionOfHeight
        invalidate()
    }

    fun setUserDefaultStyle() {
        style = style.mergeWith(getUserCaptionStyle())
        invalidate()
    }

    fun setUserDefaultTextSize() {
        defaultTextSizeType = SubtitleTextSizeType.ABSOLUTE
        defaultTextSize = DEFAULT_TEXT_SIZE_DP.dp * getUserCaptionFontScale()
        invalidate()
    }

    private fun ensurePainterCount(cueCount: Int) {
        while (painters.size < cueCount) {
            painters += SubtitlePainter(context)
        }
    }

    private fun updateUserCaptionPreferences() {
        setUserDefaultTextSize()
        style = style.mergeWith(getUserCaptionStyle())
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        if (currentCues.isEmpty()) {
            return
        }

        val defaultLeft = paddingLeft
        val defaultTop = paddingTop
        val defaultRight = width - paddingRight
        val defaultBottom = height - paddingBottom
        val bounds = layoutBounds
        val left = bounds?.left?.coerceIn(defaultLeft, defaultRight) ?: defaultLeft
        val top = bounds?.top?.coerceIn(defaultTop, defaultBottom) ?: defaultTop
        val right = bounds?.right?.coerceIn(left, defaultRight) ?: defaultRight
        val bottom = bounds?.bottom?.coerceIn(top, defaultBottom) ?: defaultBottom
        if (right <= left || bottom <= top) {
            return
        }

        val rawHeight = height
        val heightMinusPadding = defaultBottom - defaultTop
        val baseDefaultTextSize = SubtitleViewUtils.resolveTextSize(
            defaultTextSizeType,
            defaultTextSize,
            rawHeight,
            heightMinusPadding
        )
        if (baseDefaultTextSize <= 0f) {
            return
        }

        if (autoShrinkDirty) {
            autoShrinkDirty = false
            autoShrinkFactor = computeAutoShrinkFactor(
                baseDefaultTextSize * subtitleScale,
                left, right
            )
        }

        val effectiveScale = subtitleScale * autoShrinkFactor
        val resolvedDefaultTextSize = baseDefaultTextSize * effectiveScale

        val renderCues = prepareCuesForDraw(currentCues).map { preparedCue ->
            RenderCueState(preparedCue, resolveDrawStyleDecision(preparedCue.drawCue))
        }
        val duplicateGroups = findDuplicatePositionGroups(renderCues)
        val traceThisFrame = shouldTraceFrame(renderCues, duplicateGroups)
        if (traceThisFrame) {
            tracePreparedFrame(renderCues, duplicateGroups, resolvedDefaultTextSize, left, top, right, bottom)
        }

        val drawnCueStates = if (traceThisFrame) mutableListOf<DrawnCueState>() else null
        renderCues.forEachIndexed { index, renderCue ->
            val drawCue = renderCue.preparedCue.drawCue
            val cueTextSize = SubtitleViewUtils.resolveTextSize(
                drawCue.textSizeType,
                drawCue.textSize,
                rawHeight,
                heightMinusPadding
            ) * effectiveScale
            painters[index].draw(
                cue = drawCue,
                style = renderCue.drawStyleDecision.drawStyle,
                defaultTextSizePx = resolvedDefaultTextSize,
                cueTextSizePx = cueTextSize,
                bottomPaddingFraction = bottomPaddingFraction,
                dockMode = dockMode,
                frameTimeUs = currentCuePresentationTimeUs,
                traceEnabled = traceThisFrame,
                canvas = canvas,
                cueBoxLeft = left,
                cueBoxTop = top,
                cueBoxRight = right,
                cueBoxBottom = bottom
            )
            if (traceThisFrame) {
                painters[index].getLastDrawBounds()?.let { bounds ->
                    drawnCueStates?.add(
                        DrawnCueState(
                            index = index,
                            textPreview = previewCueText(drawCue.text),
                            positionKey = buildCuePositionKey(drawCue),
                            bounds = bounds
                        )
                    )
                }
            }
        }
        if (traceThisFrame) {
            traceDrawCollisions(drawnCueStates.orEmpty())
        }
        traceGeometryFrame(left, top, right, bottom, drawnCueStates.orEmpty())
    }

    private fun prepareCuesForDraw(cues: List<SubtitleCue>): List<PreparedCueState> {
        var autoLine = -1f
        val preparedCues = cues.map { originalCue ->
            val sanitizedCue = sanitizeCue(originalCue)
            if (shouldAutoStack(sanitizedCue)) {
                val stackedCue = sanitizedCue.copy(
                    line = autoLine,
                    lineType = SubtitleLineType.NUMBER,
                    lineAnchor = SubtitleAnchor.END
                )
                autoLine -= 1f
                PreparedCueState(
                    originalCue = originalCue,
                    sanitizedCue = sanitizedCue,
                    drawCue = stackedCue,
                    autoStackReason = "applied:no-line"
                )
            } else {
                PreparedCueState(
                    originalCue = originalCue,
                    sanitizedCue = sanitizedCue,
                    drawCue = sanitizedCue,
                    autoStackReason = getAutoStackSkipReason(sanitizedCue)
                )
            }
        }
        // ASS can arrive as several same-position generic cues after backend parsing. First drop
        // equivalent text layers, then stack real bilingual/multi-track lines.
        val deduplicatedCues = deduplicateEquivalentTextCues(preparedCues)
        return resolveConflictingCuePositions(deduplicatedCues)
    }

    private fun shouldAutoStack(cue: SubtitleCue): Boolean {
        return cue.bitmap == null &&
            !cue.text.isNullOrEmpty() &&
            cue.verticalType == SubtitleVerticalType.UNSET &&
            cue.line == SubtitleCue.DIMEN_UNSET
    }

    private fun resolveDrawStyleDecision(cue: SubtitleCue): DrawStyleDecision {
        val textColor = resolveCueTextColor(cue) ?: style.foregroundColor
        // 只基于最终可见的文本色决定兜底描边色，适配内封 ASS 无法读取原始样式的场景。
        val preferredEdgeColor = resolveAdaptiveEdgeColor(textColor, style.edgeColor)
        val visibleTextColor = resolveVisibleTextColor(textColor, preferredEdgeColor)
        val edgeFallbackMode = when (preferredEdgeColor) {
            style.edgeColor -> "keep"
            Color.BLACK -> "adaptive:black"
            Color.WHITE -> "adaptive:white"
            adaptiveMidToneEdgeColor -> "adaptive:mid"
            else -> "adaptive:custom"
        }
        return DrawStyleDecision(
            drawStyle = style.copy(
                foregroundColor = visibleTextColor,
                edgeColor = preferredEdgeColor
            ),
            resolvedTextColor = visibleTextColor,
            edgeFallbackMode = edgeFallbackMode
        )
    }

    private fun resolveCueTextColor(cue: SubtitleCue): Int? {
        if (!applyEmbeddedStyles) {
            return null
        }
        val spanned = cue.text as? Spanned ?: return null
        val spans = spanned.getSpans(0, spanned.length, ForegroundColorSpan::class.java)
        val embeddedColor = spans.lastOrNull()?.foregroundColor ?: return null
        if (Color.alpha(embeddedColor) >= MIN_VISIBLE_TEXT_ALPHA) {
            return embeddedColor
        }
        traceVisibilityDecision(
            "transparent-foreground-fallback",
            "embedded=${formatColor(embeddedColor)}",
            "fallback=${formatColor(style.foregroundColor)}"
        )
        return style.foregroundColor
    }

    private fun resolveVisibleTextColor(textColor: Int, edgeColor: Int): Int {
        if (Color.alpha(textColor) < MIN_VISIBLE_TEXT_ALPHA) {
            return style.foregroundColor
        }
        val contrast = kotlin.math.abs(calculateRelativeLuminance(textColor) - calculateRelativeLuminance(edgeColor))
        if (contrast >= MIN_VISIBLE_TEXT_CONTRAST) {
            return textColor
        }
        val fallbackColor = when {
            calculateRelativeLuminance(edgeColor) > 0.5f -> Color.BLACK
            else -> Color.WHITE
        }
        traceVisibilityDecision(
            "low-contrast-foreground-fallback",
            "text=${formatColor(textColor)}",
            "edge=${formatColor(edgeColor)}",
            "fallback=${formatColor(fallbackColor)}",
            "contrast=${String.format(Locale.US, "%.3f", contrast)}"
        )
        return fallbackColor
    }

    private fun isWhiteRgb(color: Int): Boolean {
        return color and 0x00FFFFFF == 0x00FFFFFF
    }

    private fun resolveAdaptiveEdgeColor(textColor: Int, originalEdgeColor: Int): Int {
        // 先尊重已有边色；只有当前景色和边色对比不足时，才退回到黑/白/棕三档自适应边色。
        if (style.edgeType != SubtitleStyle.EDGE_TYPE_NONE && hasSufficientColorContrast(textColor, originalEdgeColor)) {
            return originalEdgeColor
        }
        if (style.edgeType != SubtitleStyle.EDGE_TYPE_NONE && !hasSufficientColorContrast(textColor, originalEdgeColor)) {
            return selectAdaptiveEdgeColor(textColor)
        }
        if (hasSufficientColorContrast(textColor, originalEdgeColor)) {
            return originalEdgeColor
        }
        return selectAdaptiveEdgeColor(textColor)
    }

    private fun selectAdaptiveEdgeColor(textColor: Int): Int {
        val luminance = calculateRelativeLuminance(textColor)
        return when {
            luminance >= COLOR_LUMINANCE_WHITE_THRESHOLD -> Color.BLACK
            luminance <= COLOR_LUMINANCE_BLACK_THRESHOLD -> Color.WHITE
            // 中间亮度保留暖棕色，避免纯黑/纯白描边在彩色字幕上过于生硬。
            else -> adaptiveMidToneEdgeColor
        }
    }

    private fun hasSufficientColorContrast(firstColor: Int, secondColor: Int): Boolean {
        return kotlin.math.abs(calculateRelativeLuminance(firstColor) - calculateRelativeLuminance(secondColor)) >= COLOR_CONTRAST_THRESHOLD
    }

    private fun calculateRelativeLuminance(color: Int): Float {
        // 这里只需要近似亮度来决定描边回退，不追求严格色彩管理下的 gamma 校正。
        val red = Color.red(color) / 255f
        val green = Color.green(color) / 255f
        val blue = Color.blue(color) / 255f
        return 0.2126f * red + 0.7152f * green + 0.0722f * blue
    }

    private fun getAutoStackSkipReason(cue: SubtitleCue): String {
        return when {
            cue.bitmap != null -> "skip:bitmap"
            cue.text.isNullOrEmpty() -> "skip:empty-text"
            cue.verticalType != SubtitleVerticalType.UNSET -> "skip:vertical-cue"
            cue.line != SubtitleCue.DIMEN_UNSET -> "skip:line-set"
            else -> "skip:unknown"
        }
    }

    private fun deduplicateEquivalentTextCues(preparedCues: List<PreparedCueState>): List<PreparedCueState> {
        if (preparedCues.size < 2) {
            return preparedCues
        }
        val keptStates = LinkedHashMap<String, PreparedCueState>()
        preparedCues.forEach { state ->
            val drawCue = state.drawCue
            val dedupeKey = buildCueDedupeKey(drawCue)
            if (dedupeKey == null) {
                keptStates["unique:${keptStates.size}:${buildCuePositionKey(drawCue)}"] = state
                return@forEach
            }
            val existingState = keptStates[dedupeKey]
            // 同帧、同位、同文的双层 ASS 在当前渲染链里通常只会表现成“重复文本”，保留视觉更稳定的一层即可。
            if (existingState == null || shouldReplaceDeduplicatedCue(existingState, state)) {
                keptStates[dedupeKey] = state
            }
        }
        return keptStates.values.toList()
    }

    private fun buildCueDedupeKey(cue: SubtitleCue): String? {
        if (cue.bitmap != null) {
            return null
        }
        val normalizedText = normalizeCueText(cue.text) ?: return null
        return normalizedText + "||" + buildCuePositionKey(cue)
    }

    private fun normalizeCueText(text: CharSequence?): String? {
        val normalized = text?.toString()?.replace("\r", "")?.trim()
        return normalized?.takeIf { it.isNotEmpty() }
    }

    private fun shouldReplaceDeduplicatedCue(current: PreparedCueState, candidate: PreparedCueState): Boolean {
        val currentScore = computeCueRetentionScore(current.drawCue)
        val candidateScore = computeCueRetentionScore(candidate.drawCue)
        return candidateScore > currentScore
    }

    private fun computeCueRetentionScore(cue: SubtitleCue): Int {
        val textColor = resolveCueTextColor(cue) ?: style.foregroundColor
        val preferredEdgeColor = resolveAdaptiveEdgeColor(textColor, style.edgeColor)
        val contrastScore = (calculateRelativeLuminance(textColor) - calculateRelativeLuminance(preferredEdgeColor))
        val sizeScore = if (cue.textSize != SubtitleCue.DIMEN_UNSET) (cue.textSize * 100f).toInt() else 0
        return (contrastScore * 1000f).toInt() + sizeScore
    }

    private fun resolveConflictingCuePositions(
        preparedCues: List<PreparedCueState>
    ): List<PreparedCueState> {
        if (preparedCues.size < 2 || width <= 0 || height <= 0) {
            return preparedCues
        }

        val defaultLeft = paddingLeft
        val defaultTop = paddingTop
        val defaultRight = width - paddingRight
        val defaultBottom = height - paddingBottom
        val bounds = layoutBounds
        val cueBoxLeft = bounds?.left?.coerceIn(defaultLeft, defaultRight) ?: defaultLeft
        val cueBoxTop = bounds?.top?.coerceIn(defaultTop, defaultBottom) ?: defaultTop
        val cueBoxRight = bounds?.right?.coerceIn(cueBoxLeft, defaultRight) ?: defaultRight
        val cueBoxBottom = bounds?.bottom?.coerceIn(cueBoxTop, defaultBottom) ?: defaultBottom
        if (cueBoxRight <= cueBoxLeft || cueBoxBottom <= cueBoxTop) {
            return preparedCues
        }

        val rawHeight = height
        val heightMinusPadding = defaultBottom - defaultTop
        val effectiveScale = subtitleScale * autoShrinkFactor
        val resolvedDefaultTextSize = SubtitleViewUtils.resolveTextSize(
            defaultTextSizeType,
            defaultTextSize,
            rawHeight,
            heightMinusPadding
        ) * effectiveScale
        if (resolvedDefaultTextSize <= 0f) {
            return preparedCues
        }

        val updatedStates = preparedCues.toMutableList()
        val groupedIndexes = preparedCues.indices.groupBy { index ->
            buildCuePositionKey(preparedCues[index].drawCue)
        }.filterValues { indexes -> indexes.size > 1 }

        groupedIndexes.values.forEach { indexes ->
            val sortedIndexes = sortConflictGroupIndexes(
                indexes = indexes,
                preparedCues = updatedStates,
                rawHeight = rawHeight,
                heightMinusPadding = heightMinusPadding,
                resolvedDefaultTextSize = resolvedDefaultTextSize
            )
            var accumulatedTopOffsetPx = 0
            // 组内从“最靠下的轨道”开始累计高度，前面的轨道依次上移，避免同位 Cue 最终碰撞。
            for (position in sortedIndexes.lastIndex downTo 0) {
                val cueIndex = sortedIndexes[position]
                val state = updatedStates[cueIndex]
                val drawCue = state.drawCue
                val cueTextSize = resolveCueTextSize(drawCue, rawHeight, heightMinusPadding)
                val drawStyle = resolveDrawStyleDecision(drawCue).drawStyle
                val measuredBounds = painters[cueIndex].measureCueBounds(
                    cue = drawCue,
                    style = drawStyle,
                    defaultTextSizePx = resolvedDefaultTextSize,
                    cueTextSizePx = cueTextSize,
                    bottomPaddingFraction = bottomPaddingFraction,
                    dockMode = dockMode,
                    cueBoxLeft = cueBoxLeft,
                    cueBoxTop = cueBoxTop,
                    cueBoxRight = cueBoxRight,
                    cueBoxBottom = cueBoxBottom
                ) ?: continue

                if (accumulatedTopOffsetPx > 0) {
                    val repositionedCue = repositionCueForTopOffset(
                        cue = drawCue,
                        topOffsetPx = accumulatedTopOffsetPx,
                        cueBoxHeight = cueBoxBottom - cueBoxTop
                    )
                    updatedStates[cueIndex] = state.copy(
                        drawCue = repositionedCue,
                        autoStackReason = state.autoStackReason + ", conflict-shift:${accumulatedTopOffsetPx}px"
                    )
                }

                accumulatedTopOffsetPx += measuredBounds.height() + max(1, (resolvedDefaultTextSize * 0.12f).toInt())
            }
        }
        return updatedStates
    }

    private fun sortConflictGroupIndexes(
        indexes: List<Int>,
        preparedCues: List<PreparedCueState>,
        rawHeight: Int,
        heightMinusPadding: Int,
        resolvedDefaultTextSize: Float
    ): List<Int> {
        val cueTextSizes = indexes.associateWith { index ->
            val cueTextSize = resolveCueTextSize(preparedCues[index].drawCue, rawHeight, heightMinusPadding)
            if (cueTextSize > 0f) cueTextSize else resolvedDefaultTextSize
        }
        val maxCueTextSize = cueTextSizes.values.maxOrNull() ?: resolvedDefaultTextSize
        return indexes.sortedWith(
            compareBy<Int> { index ->
                cueVerticalPriority(
                    text = preparedCues[index].drawCue.text,
                    cueTextSizePx = cueTextSizes[index] ?: resolvedDefaultTextSize,
                    maxCueTextSizePx = maxCueTextSize
                )
            }.thenBy { it }
        )
    }

    private fun cueVerticalPriority(
        text: CharSequence?,
        cueTextSizePx: Float,
        maxCueTextSizePx: Float
    ): Int {
        val normalizedText = text?.toString()?.trim().orEmpty()
        if (normalizedText.isEmpty()) {
            return 3
        }
        // 小字号优先当 Ruby/注音，其次把中文主字幕放上面、日文副字幕放下面。
        if (cueTextSizePx < maxCueTextSizePx * 0.82f) {
            return 0
        }
        return when {
            containsJapaneseKana(normalizedText) -> 2
            else -> 1
        }
    }

    private fun resolveCueTextSize(cue: SubtitleCue, rawHeight: Int, heightMinusPadding: Int): Float {
        return SubtitleViewUtils.resolveTextSize(
            cue.textSizeType,
            cue.textSize,
            rawHeight,
            heightMinusPadding
        ) * subtitleScale * autoShrinkFactor
    }

    private fun computeAutoShrinkFactor(scaledTextSizePx: Float, left: Int, right: Int): Float {
        // Safety shrink after user scaling so long single-line cues do not clip. It never enlarges
        // text and intentionally avoids complex span/wrapping analysis.
        if (currentCues.isEmpty() || scaledTextSizePx <= 0f) {
            return 1.0f
        }
        val availableWidth = (right - left).toFloat()
        if (availableWidth <= 0f) {
            return 1.0f
        }
        shrinkPaint.textSize = scaledTextSizePx
        val maxTextWidth = currentCues.maxOfOrNull { cue ->
            val text = cue.text?.toString()?.replace("\r", "")?.trim()
            if (text.isNullOrEmpty()) 0f else shrinkPaint.measureText(text)
        } ?: 0f
        if (maxTextWidth <= availableWidth) {
            return 1.0f
        }
        return (availableWidth / maxTextWidth).coerceIn(0.3f, 1.0f)
    }

    private fun containsJapaneseKana(text: String): Boolean {
        return text.any { char ->
            char in '\u3040'..'\u309F' ||
                char in '\u30A0'..'\u30FF' ||
                char in '\u31F0'..'\u31FF' ||
                char in '\uFF66'..'\uFF9D'
        }
    }

    private fun repositionCueForTopOffset(cue: SubtitleCue, topOffsetPx: Int, cueBoxHeight: Int): SubtitleCue {
        if (topOffsetPx <= 0 || cueBoxHeight <= 0) {
            return cue
        }
        return if (cue.lineType == SubtitleLineType.FRACTION) {
            val offsetFraction = topOffsetPx.toFloat() / cueBoxHeight.toFloat()
            cue.copy(
                line = (cue.line - offsetFraction).coerceAtLeast(0f),
                lineType = SubtitleLineType.FRACTION
            )
        } else {
            val lineStep = max(1f, topOffsetPx.toFloat() / max(1f, defaultTextSize))
            cue.copy(
                line = cue.line - lineStep,
                lineType = SubtitleLineType.NUMBER
            )
        }
    }

    private fun shouldTraceFrame(
        renderCues: List<RenderCueState>,
        duplicateGroups: Map<String, List<RenderCueState>>
    ): Boolean {
        if (!BuildConfig.DEBUG || renderCues.isEmpty()) {
            return false
        }
        return renderCues.size >= 1 ||
            duplicateGroups.isNotEmpty() ||
            renderCues.any { it.drawStyleDecision.edgeFallbackMode != "keep" }
    }

    private fun findDuplicatePositionGroups(
        renderCues: List<RenderCueState>
    ): Map<String, List<RenderCueState>> {
        return renderCues.groupBy { buildCuePositionKey(it.preparedCue.drawCue) }
            .filterValues { it.size > 1 }
    }

    private fun tracePreparedFrame(
        renderCues: List<RenderCueState>,
        duplicateGroups: Map<String, List<RenderCueState>>,
        resolvedDefaultTextSize: Float,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        Log.d(
            SUBTITLE_TRACE_PREPARE_TAG,
            "frameUs=${currentCuePresentationTimeUs}",
            "cueCount=${renderCues.size}",
            "defaultTextSize=${formatCueFloat(resolvedDefaultTextSize)}",
            "bounds=[$left,$top,$right,$bottom]",
            "embeddedStyles=$applyEmbeddedStyles",
            "embeddedFontSizes=$applyEmbeddedFontSizes",
            "edgeType=${style.edgeType}",
            "edgeColor=${formatColor(style.edgeColor)}"
        )
        duplicateGroups.forEach { (positionKey, cuesInGroup) ->
            Log.d(
                SUBTITLE_TRACE_PREPARE_TAG,
                "duplicate-position-group",
                "key=$positionKey",
                "texts=${cuesInGroup.joinToString(prefix = "[", postfix = "]") { previewCueText(it.preparedCue.drawCue.text) }}"
            )
        }
        renderCues.forEachIndexed { index, renderCue ->
            val preparedCue = renderCue.preparedCue
            val decision = renderCue.drawStyleDecision
            Log.d(
                SUBTITLE_TRACE_PREPARE_TAG,
                "cue#$index",
                "text=${previewCueText(preparedCue.drawCue.text)}",
                "original=${summarizeCuePosition(preparedCue.originalCue)}",
                "sanitized=${summarizeCuePosition(preparedCue.sanitizedCue)}",
                "draw=${summarizeCuePosition(preparedCue.drawCue)}",
                "autoStack=${preparedCue.autoStackReason}",
                "styleForeground=${formatColor(style.foregroundColor)}",
                "drawForeground=${formatColor(decision.drawStyle.foregroundColor)}",
                "textColor=${formatColor(decision.resolvedTextColor)}",
                "styleEdge=${formatColor(style.edgeColor)}",
                "edgeColor=${formatColor(decision.drawStyle.edgeColor)}",
                "edgeFallback=${decision.edgeFallbackMode}"
            )
        }
    }

    private fun traceDrawCollisions(drawnCueStates: List<DrawnCueState>) {
        if (drawnCueStates.size < 2) {
            return
        }
        for (firstIndex in 0 until drawnCueStates.lastIndex) {
            val firstCue = drawnCueStates[firstIndex]
            for (secondIndex in firstIndex + 1 until drawnCueStates.size) {
                val secondCue = drawnCueStates[secondIndex]
                if (Rect.intersects(firstCue.bounds, secondCue.bounds)) {
                    Log.d(
                        SUBTITLE_TRACE_PREPARE_TAG,
                        "layout-collision",
                        "frameUs=${currentCuePresentationTimeUs}",
                        "first=#${firstCue.index}:${firstCue.textPreview}",
                        "firstKey=${firstCue.positionKey}",
                        "firstBounds=${formatRect(firstCue.bounds)}",
                        "second=#${secondCue.index}:${secondCue.textPreview}",
                        "secondKey=${secondCue.positionKey}",
                        "secondBounds=${formatRect(secondCue.bounds)}"
                    )
                }
            }
        }
    }

    private fun traceGeometryFrame(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        drawnCueStates: List<DrawnCueState>
    ) {
        if (!BuildConfig.DEBUG || currentCues.isEmpty()) {
            return
        }
        val layoutRect = Rect(left, top, right, bottom)
        if (drawnCueStates.isEmpty()) {
            Log.d(
                SUBTITLE_GEOMETRY_TRACE_TAG,
                "event=subtitle_draw_bounds",
                "frameUs=$currentCuePresentationTimeUs",
                "dockMode=${describeDockMode()}",
                "layout=${formatRect(layoutRect)}",
                "host=${width}x${height}",
                "drawn=[]"
            )
            return
        }
        val drawnMetrics = drawnCueStates.joinToString(prefix = "[", postfix = "]") { state ->
            val distanceToLayoutTop = state.bounds.top - layoutRect.top
            val distanceToScreenBottom = (height - paddingBottom) - state.bounds.bottom
            "{idx=${state.index},bounds=${formatRect(state.bounds)},toLayoutTop=$distanceToLayoutTop,toScreenBottom=$distanceToScreenBottom,text=${state.textPreview}}"
        }
        Log.d(
            SUBTITLE_GEOMETRY_TRACE_TAG,
            "event=subtitle_draw_bounds",
            "frameUs=$currentCuePresentationTimeUs",
            "dockMode=${describeDockMode()}",
            "layout=${formatRect(layoutRect)}",
            "host=${width}x${height}",
            "drawn=$drawnMetrics"
        )
    }

    private fun summarizeCuePosition(cue: SubtitleCue): String {
        return buildString {
            append("line=")
            append(formatCueFloat(cue.line))
            append('/')
            append(cue.lineType)
            append('/')
            append(cue.lineAnchor)
            append(", pos=")
            append(formatCueFloat(cue.position))
            append('/')
            append(cue.positionAnchor)
            append(", size=")
            append(formatCueFloat(cue.size))
            append(", align=")
            append(cue.textAlignment)
            append(", vertical=")
            append(cue.verticalType)
        }
    }

    private fun buildCuePositionKey(cue: SubtitleCue): String {
        return listOf(
            "line=${formatCueFloat(cue.line)}",
            "lineType=${cue.lineType}",
            "lineAnchor=${cue.lineAnchor}",
            "position=${formatCueFloat(cue.position)}",
            "positionAnchor=${cue.positionAnchor}",
            "size=${formatCueFloat(cue.size)}",
            "align=${cue.textAlignment}",
            "vertical=${cue.verticalType}"
        ).joinToString(separator = "|")
    }

    private fun previewCueText(text: CharSequence?): String {
        val normalized = text?.toString()?.replace("\n", "\\n")?.replace("\r", "") ?: "<null>"
        return if (normalized.length <= 80) normalized else normalized.take(77) + "..."
    }

    private fun formatCueFloat(value: Float): String {
        return if (value == SubtitleCue.DIMEN_UNSET) {
            "UNSET"
        } else {
            String.format(Locale.US, "%.3f", value)
        }
    }

    private fun formatColor(color: Int): String {
        return String.format(Locale.US, "#%08X", color)
    }

    private fun formatRect(rect: Rect): String {
        return "[${rect.left},${rect.top},${rect.right},${rect.bottom}]"
    }

    private fun sanitizeCue(cue: SubtitleCue): SubtitleCue {
        var result = cue
        if (!applyEmbeddedStyles) {
            result = SubtitleViewUtils.removeAllEmbeddedStyling(result)
        } else {
            if (!applyEmbeddedFontSizes) {
                result = SubtitleViewUtils.removeEmbeddedFontSizes(result)
            }
            if (!applyEmbeddedWindowColor) {
                result = SubtitleViewUtils.clearWindowColor(result)
            }
        }

        result = sanitizeCueVisibility(result)

        if (result.verticalType != SubtitleVerticalType.UNSET) {
            result = repositionVerticalCue(result)
        }
        return result
    }

    private fun sanitizeCueVisibility(cue: SubtitleCue): SubtitleCue {
        // Malformed or converted ASS cues can contain out-of-range fractional positions. Resetting
        // them lets the auto-stacking path keep subtitles visible.
        val invalidFractionalLine = cue.lineType == SubtitleLineType.FRACTION &&
            cue.line != SubtitleCue.DIMEN_UNSET &&
            (cue.line < -0.2f || cue.line > 1.2f)
        val invalidFractionalPosition = cue.position != SubtitleCue.DIMEN_UNSET &&
            (cue.position < -0.2f || cue.position > 1.2f)
        if (!invalidFractionalLine && !invalidFractionalPosition) {
            return cue
        }
        var result = cue
        if (invalidFractionalLine) {
            result = result.copy(
                line = SubtitleCue.DIMEN_UNSET,
                lineType = SubtitleLineType.UNSET
            )
        }
        if (invalidFractionalPosition) {
            result = result.copy(
                position = SubtitleCue.DIMEN_UNSET,
                positionAnchor = SubtitleAnchor.UNSET
            )
        }
        traceVisibilityDecision(
            "sanitize-position",
            "text=${previewCueText(cue.text)}",
            "line=${formatCueFloat(cue.line)}/${cue.lineType}",
            "position=${formatCueFloat(cue.position)}",
            "lineFallback=$invalidFractionalLine",
            "positionFallback=$invalidFractionalPosition"
        )
        return result
    }

    private fun traceVisibilityDecision(event: String, vararg details: String) {
        if (!BuildConfig.DEBUG) {
            return
        }
        Log.d(SUBTITLE_TRACE_VISIBILITY_TAG, "event=$event", *details)
    }

    private fun repositionVerticalCue(cue: SubtitleCue): SubtitleCue {
        // The generic renderer does not implement true vertical text yet; convert vertical cues into
        // a horizontal fallback that preserves on-screen visibility.
        var result = cue.copy(
            position = SubtitleCue.DIMEN_UNSET,
            positionAnchor = SubtitleAnchor.UNSET,
            textAlignment = null
        )

        result = if (cue.lineType == SubtitleLineType.FRACTION) {
            result.copy(line = 1f - cue.line, lineType = SubtitleLineType.FRACTION)
        } else {
            result.copy(line = -cue.line - 1f, lineType = SubtitleLineType.NUMBER)
        }

        result = when (cue.lineAnchor) {
            SubtitleAnchor.END -> result.copy(lineAnchor = SubtitleAnchor.START)
            SubtitleAnchor.START -> result.copy(lineAnchor = SubtitleAnchor.END)
            else -> result
        }
        return result
    }

    private fun getUserCaptionStyle(): SubtitleStyle {
        val manager = context.getSystemService(Context.CAPTIONING_SERVICE) as? CaptioningManager
        if (manager == null || !manager.isEnabled) {
            return SubtitleStyle.DEFAULT
        }
        val userStyle = manager.userStyle
        return SubtitleStyle(
            foregroundColor = userStyle.foregroundColor,
            backgroundColor = userStyle.backgroundColor,
            windowColor = userStyle.windowColor,
            edgeType = userStyle.edgeType,
            edgeColor = userStyle.edgeColor,
            edgeWidth = style.edgeWidth,
            typeface = userStyle.typeface
        )
    }

    private fun getUserCaptionFontScale(): Float {
        val manager = context.getSystemService(Context.CAPTIONING_SERVICE) as? CaptioningManager
        return if (manager != null && manager.isEnabled) manager.fontScale else 1f
    }

}
