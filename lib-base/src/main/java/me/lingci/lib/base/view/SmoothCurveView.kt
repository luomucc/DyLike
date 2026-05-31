package me.lingci.lib.base.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt

/**
 * 密度曲线
 */
class SmoothCurveView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val pink = "#FF4081".toColorInt()
    private val purple = "#8B5CF6".toColorInt()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = purple
    }

    private val shaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val assistantPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.LTGRAY
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f) // 默认虚线
    }

    private val path = Path()
    private val shaderPath = Path()

    // 接收外部计算好的数据点
    var dataPoints: FloatArray = floatArrayOf()
        set(value) {
            field = value
            invalidate()
        }

    var progress: Float = 0f // 0.0 ~ 1.0
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var curveColor: Int = purple
        set(value) {
            field = value
            paint.color = value
            updateShader()
            invalidate()
        }

    // 控制开关
    var showVerticalLine: Boolean = true
    var showHorizontalLine: Boolean = true
    var enableTouch: Boolean = false // 默认不开启触摸

    private fun updateShader() {
        // 着色器会在 onDraw 中动态设置
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!enableTouch) return super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                progress = event.x / width.toFloat()
                parent.requestDisallowInterceptTouchEvent(true) // 防止与 ScrollView 冲突
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    @SuppressLint("DrawAllocation")
    @Suppress("UnnecessaryVariable")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 12f // 内边距，防止顶天立地
        val chartHeight = height - padding * 2

        // 设置着色器
        val shader = LinearGradient(
            0f, 0f, 0f, height,
            curveColor,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        shaderPaint.shader = shader

        path.reset()
        shaderPath.reset()

        val pointCount = dataPoints.size
        // 如果只有一个点，直接画一条竖线
        if (pointCount == 1) {
            val x = width / 2
            val y = height - padding - (dataPoints[0] * (height - padding * 2))
            path.moveTo(x, height - padding)
            path.lineTo(x, y)

            shaderPath.moveTo(x, height - padding)
            shaderPath.lineTo(x, y)
            shaderPath.lineTo(x + 10, height - padding)
            shaderPath.close()
        } else {
            // 多个点，使用贝塞尔曲线连接
            for (i in dataPoints.indices) {
                // 均匀分布 X 轴坐标
                val x = i * (width / (pointCount - 1).toFloat())
                // Y 轴：底部是0，顶部是100%。减去 padding 留出边距
                val y = height - padding - (dataPoints[i] * (height - padding * 2))

                if (i == 0) {
                    path.moveTo(x, y)
                    shaderPath.moveTo(0f, height - padding)
                    shaderPath.lineTo(x, y)
                } else {
                    // 计算贝塞尔控制点
                    val prevX = (i - 1) * (width / (pointCount - 1).toFloat())
                    val prevY = height - padding - (dataPoints[i - 1] * (height - padding * 2))

                    val ctrlX1 = prevX + (x - prevX) / 3
                    val ctrlY1 = prevY
                    val ctrlX2 = x - (x - prevX) / 3
                    val ctrlY2 = y

                    path.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, x, y)
                    // 阴影路径直接连线（不平滑阴影底部，更清晰）
                    shaderPath.lineTo(x, y)
                }
            }
            // 闭合阴影路径到底部
            shaderPath.lineTo(width, height - padding)
            shaderPath.lineTo(0f, height - padding)
            shaderPath.close()
        }

        // 绘制
        canvas.drawPath(shaderPath, shaderPaint)
        canvas.drawPath(path, paint)

        // 2. 绘制辅助线
        drawAssistantLines(canvas, width, height, padding, chartHeight)
    }

    private fun drawMainCurve(canvas: Canvas, w: Float, h: Float, padding: Float, chartHeight: Float) {
        val shader = LinearGradient(0f, 0f, 0f, h, curveColor, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        shaderPaint.shader = shader
        path.reset()
        shaderPath.reset()

        val count = dataPoints.size
        if (count < 2) return

        for (i in dataPoints.indices) {
            val x = i * (w / (count - 1))
            val y = h - padding - (dataPoints[i] * chartHeight)

            if (i == 0) {
                path.moveTo(x, y)
                shaderPath.moveTo(0f, h - padding)
                shaderPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * (w / (count - 1))
                val prevY = h - padding - (dataPoints[i - 1] * chartHeight)
                path.cubicTo(prevX + (x - prevX) / 3, prevY, x - (x - prevX) / 3, y, x, y)
                shaderPath.lineTo(x, y)
            }
        }
        shaderPath.lineTo(w, h - padding)
        shaderPath.lineTo(0f, h - padding)
        shaderPath.close()

        canvas.drawPath(shaderPath, shaderPaint)
        canvas.drawPath(path, paint)
    }

    @Suppress("SameParameterValue")
    private fun drawAssistantLines(canvas: Canvas, w: Float, h: Float, padding: Float, chartHeight: Float) {
        if (!showVerticalLine && !showHorizontalLine) return

        val currentX = w * progress

        // 计算当前进度对应的 Y 轴数值（线性插值模拟曲线高度）
        val count = dataPoints.size
        val floatIndex = progress * (count - 1)
        val index = floatIndex.toInt().coerceIn(0, count - 2)
        val ratio = floatIndex - index
        val currentDataValue = dataPoints[index] + (dataPoints[index + 1] - dataPoints[index]) * ratio
        val currentY = h - padding - (currentDataValue * chartHeight)

        // 绘制竖线
        if (showVerticalLine) {
            canvas.drawLine(currentX, currentY, currentX, h, assistantPaint)
        }

        // 绘制横线
        if (showHorizontalLine) {
            canvas.drawLine(0f, h - paint.strokeWidth, currentX, h, paint)
        }
    }

    fun usePurple() {
        curveColor = purple
    }

    fun usePink() {
        curveColor = pink
    }

    fun unLine() {
        showVerticalLine = false
        showHorizontalLine = false
    }

    fun changeProgress(current: Long) {
        if (dataPoints.isEmpty()) {
            return
        }
        progress = current / dataPoints.size.toFloat()
    }

}