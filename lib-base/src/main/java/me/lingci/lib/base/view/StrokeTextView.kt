package me.lingci.lib.base.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import me.lingci.lib.base.util.dp

class StrokeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var _strokeWidth = 2f.dp
    private var strokeColor: Int = Color.BLACK
    private val strokePaint = TextPaint()

    @SuppressLint("RtlHardcoded")
    override fun onDraw(canvas: Canvas) {
        val layout = layout ?: return
        val text = text.toString()

        if (_strokeWidth > 0f) {
            // 设置描边画笔，与文字画笔完全同步
            // 重要：复制所有与字符间距和字体渲染相关的属性
            strokePaint.apply {
                isAntiAlias = paint.isAntiAlias
                isSubpixelText = paint.isSubpixelText // 与文字渲染精度有关
                // isLinearText = paint.isLinearText // 通常不需要
                isDither = paint.isDither
                hinting = paint.hinting // 字体提示信息
                // Typeface
                textSize = paint.textSize
                typeface = paint.typeface
                isFakeBoldText = paint.isFakeBoldText
                // Text Alignment
                textAlign = paint.textAlign
                // Text Scaling and Skewing
                textScaleX = paint.textScaleX
                textSkewX = paint.textSkewX
                letterSpacing = paint.letterSpacing // <<< 关键：复制字间距
                // Text Path Effect
                pathEffect = paint.pathEffect // 通常描边不需要 path effect
                // Shader, Color Filter
                shader = paint.shader // 通常描边不需要 shader
                colorFilter = paint.colorFilter // 通常描边不需要 color filter
                // Xfermode
                xfermode = paint.xfermode // 通常描边不需要 xfermode
                // Text Metrics
                // fontFeatureSettings = paint.fontFeatureSettings // API 21+
                // breakStrategy = paint.breakStrategy // API 23+
                // hyphenationFrequency = paint.hyphenationFrequency // API 23+

                color = strokeColor
                style = Paint.Style.STROKE
                strokeWidth = _strokeWidth
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }

            // 直接复用TextView的绘制逻辑，确保位置完全一致
            val saveCount = canvas.save()

            // 调整画布偏移，匹配TextView的滚动和padding
            canvas.translate(totalPaddingLeft.toFloat(), totalPaddingTop.toFloat())
            if (scrollX != 0) {
                canvas.translate(-scrollX.toFloat(), 0f)
            }

            // 逐行绘制描边，完全匹配文字位置
            for (i in 0 until layout.lineCount) {
                val lineStart = layout.getLineStart(i)
                val lineEnd = layout.getLineEnd(i)
                val lineText = text.substring(lineStart, lineEnd)
                // 关键：使用 Layout 提供的精确起始 X 坐标
                // getLineLeft 返回的是该行相对于 Layout 左边界的 X 坐标
                // getLineBaseline 返回的是该行基线的 Y 坐标
                val x = layout.getLineLeft(i).toFloat()
                val baseline = layout.getLineBaseline(i).toFloat()
                canvas.drawText(lineText, x, baseline, strokePaint)
            }

            canvas.restoreToCount(saveCount)
        }

        // 最后绘制文字本体，使其镂空
        val originalColor = paint.color
        paint.color = Color.TRANSPARENT
        super.onDraw(canvas)
        paint.color = originalColor
    }

    fun setStrokeWidth(width: Float) {
        _strokeWidth = width
        invalidate()
    }

    fun setStrokeColor(color: Int) {
        strokeColor = color
        invalidate()
    }
}