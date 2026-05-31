package me.lingci.lib.player.widget.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.SeekBar
import me.lingci.lib.player.chapter.ChapterNode

/**
 * 带章节标记的进度条
 * 继承 SeekBar，在进度条上绘制章节标记
 */
class ChapterSeekBar : SeekBar {

    private val chapterMarkers = mutableListOf<ChapterMarker>()
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x80FFFFFF.toInt() // 半透明白色
        style = Paint.Style.FILL
    }
    private val activeMarkerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt() // 白色
        style = Paint.Style.FILL
    }

    private var totalDurationMs: Long = 0L
    private var currentChapterIndex: Int = -1

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    init {
        // 禁用硬件加速以支持自定义绘制
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /**
     * 设置章节列表
     */
    fun setChapters(chapters: List<ChapterNode>, durationMs: Long) {
        totalDurationMs = durationMs
        chapterMarkers.clear()

        if (durationMs <= 0 || chapters.isEmpty()) {
            invalidate()
            return
        }

        for (chapter in chapters) {
            val progress = (chapter.startTimeMs.toFloat() / durationMs.toFloat())
                .coerceIn(0f, 1f)
            chapterMarkers.add(ChapterMarker(progress, chapter.index))
        }
        invalidate()
    }

    /**
     * 更新当前章节
     */
    fun setCurrentChapter(index: Int) {
        if (currentChapterIndex != index) {
            currentChapterIndex = index
            invalidate()
        }
    }

    /**
     * 清除章节标记
     */
    fun clearChapters() {
        chapterMarkers.clear()
        currentChapterIndex = -1
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (chapterMarkers.isEmpty()) return

        val height = height.toFloat()
        val width = width.toFloat()
        val paddingLeft = paddingLeft.toFloat()
        val paddingRight = paddingRight.toFloat()
        val paddingTop = paddingTop.toFloat()
        val paddingBottom = paddingBottom.toFloat()

        val trackHeight = 4f // 标记高度
        val trackTop = (height - trackHeight) / 2
        val trackLeft = paddingLeft
        val trackRight = width - paddingRight
        val trackWidth = trackRight - trackLeft

        for (marker in chapterMarkers) {
            val x = trackLeft + (trackWidth * marker.progress)

            // 跳过开始和结束位置的标记
            if (marker.progress <= 0.01f || marker.progress >= 0.99f) continue

            val markerWidth = 2f
            val markerHeight = height - paddingTop - paddingBottom
            val rect = RectF(
                x - markerWidth / 2,
                paddingTop,
                x + markerWidth / 2,
                height - paddingBottom
            )

            val paint = if (marker.chapterIndex == currentChapterIndex) {
                activeMarkerPaint
            } else {
                markerPaint
            }

            canvas.drawRoundRect(rect, 1f, 1f, paint)
        }
    }

    /**
     * 章节标记数据
     */
    private data class ChapterMarker(
        val progress: Float, // 0.0 - 1.0
        val chapterIndex: Int
    )
}
