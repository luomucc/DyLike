package me.lingci.lib.base.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.text.TextPaint
import android.util.AttributeSet
import android.view.Gravity
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import me.lingci.lib.base.util.dp

/**
 *   @author : SeepSeek
 *   time    : 2025/03/17
 *   desc    :
 *   version : 1.0
 */

class GradientStrokeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val strokePaint = TextPaint()
    private var _strokeWidth = 2f.dp
    private var strokeColor = Color.BLACK
    private var strokeGradient = true
    private var gradientColors = intArrayOf(Color.RED, Color.MAGENTA, Color.BLUE)
    private var gradientPositions = floatArrayOf(0.2f, 0.5f, 0.8f)
    private var gradientType = GradientType.START_END
    private var textGradient = false

    enum class GradientType {
        LEFT_RIGHT, TOP_BOTTOM,
        START_END, CENTER_RADIUS
    }

    override fun onDraw(canvas: Canvas) {
        // 设置描边画笔
        strokePaint.apply {
            textSize = paint.textSize
            typeface = paint.typeface
            isFakeBoldText = paint.isFakeBoldText
            color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = _strokeWidth
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            shader = if (strokeGradient) {
                createGradientShader()
            } else {
                null
            }
        }
        // 获取文字位置和尺寸
        val text = text.toString()
        val textWidth = paint.measureText(text)
        val textHeight = paint.descent() - paint.ascent()

        // 计算居中位置（考虑gravity属性）
        val x = when ((gravity and Gravity.HORIZONTAL_GRAVITY_MASK)) {
            Gravity.CENTER_HORIZONTAL -> (width - textWidth) / 2
            Gravity.RIGHT -> width - textWidth - paddingRight
            Gravity.END -> width - textWidth - paddingEnd
            else -> paddingLeft.toFloat()
        }

        val y = when ((gravity and Gravity.VERTICAL_GRAVITY_MASK)) {
            Gravity.CENTER_VERTICAL -> (height + textHeight) / 2 - paint.descent()
            Gravity.BOTTOM -> height - paddingBottom - paint.descent()
            else -> paddingTop - paint.ascent()
        }

        // 更新描边画笔位置
        if (strokeGradient) {
            strokePaint.shader = createGradientShader(x, y, x + textWidth, y + textHeight)
        }

        // 先绘制描边
        canvas.drawText(text, x, y , strokePaint)
        // 再绘制原始文字
        if (textGradient) {
            paint.shader = createGradientShader(0f, 0f, textWidth, 0f)
        } else {
            paint.shader = null
        }
        super.onDraw(canvas)
    }

    private fun createGradientShader(startX: Float, startY: Float, endX: Float, endY: Float): Shader {
        return when (gradientType) {
            GradientType.LEFT_RIGHT -> LinearGradient(
                startX, startY, endX, startY,
                gradientColors, gradientPositions, Shader.TileMode.CLAMP
            )
            GradientType.TOP_BOTTOM -> LinearGradient(
                startX, startY, startX, endY,
                gradientColors, gradientPositions, Shader.TileMode.CLAMP
            )
            GradientType.START_END -> LinearGradient(
                startX, startY, width.toFloat(), height.toFloat(),
                gradientColors, gradientPositions, Shader.TileMode.CLAMP
            )
            GradientType.CENTER_RADIUS -> RadialGradient(
                width / 2f, height / 2f,
                maxOf(width, height) / 2f,
                gradientColors, gradientPositions, Shader.TileMode.CLAMP
            )
        }
    }

    private fun createGradientShader(): Shader {
        return when (gradientType) {
            GradientType.LEFT_RIGHT -> LinearGradient(
                0f, 0f, width.toFloat(), 0f,
                gradientColors, gradientPositions, Shader.TileMode.CLAMP
            )
            GradientType.TOP_BOTTOM -> LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                gradientColors, gradientPositions, Shader.TileMode.CLAMP
            )
            GradientType.START_END -> LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                gradientColors, gradientPositions, Shader.TileMode.CLAMP
            )
            GradientType.CENTER_RADIUS -> RadialGradient(
                width / 2f, height / 2f,
                maxOf(width, height) / 2f,
                gradientColors, gradientPositions, Shader.TileMode.CLAMP
            )
        }
    }

    // 设置方法
    fun setStrokeWidth(width: Float) {
        _strokeWidth = width
        invalidate()
    }

    fun setGradientColors(colors: IntArray) {
        gradientColors = colors
        gradientPositions = if (colors.size > 2) {
            floatArrayOf(0.2f, 0.5f, 0.8f)
        } else {
            floatArrayOf(0.3f, 0.7f)
        }
        invalidate()
    }

    fun setGradientType(type: GradientType) {
        gradientType = type
        invalidate()
    }

    fun setStrokeColor(@ColorInt color: Int) {
        strokeColor = color
        invalidate()
    }

    fun isStrokeGradient(boolean: Boolean) {
        strokeGradient = boolean
        if (boolean) {
            textGradient = false
        }
        invalidate()
    }

    fun isTextGradient(boolean: Boolean) {
        textGradient = boolean
        if (boolean) {
            strokeGradient = false
        }
        invalidate()
    }

}