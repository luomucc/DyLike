package me.lingci.lib.base.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.Gravity
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.graphics.drawable.DrawableCompat
import me.lingci.lib.base.R
import me.lingci.lib.base.util.colorLightness
import me.lingci.lib.base.util.dp

/**
 *   @author : DeepSeek
 *   time    : 2025/03/17
 *   desc    :
 *   version : 1.0
 */
class ColorSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.seekBarStyle
) : AppCompatSeekBar(context, attrs, defStyleAttr) {

    private var mColorSeekBarMode: ColorSeekBarMode = ColorSeekBarMode.SELECT
    private val selectColors = IntArray(361) { i ->
        Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, 1f))
    }

    // 自定义属性
    private var trackHeight = 8f.dp

    // 渐变色参数
    private var gradientColors = selectColors

    init {
        // 初始化属性
        initView()
    }

    private fun initView() {
        max = 360
        setColorMode(ColorSeekBarMode.SELECT)
    }

    @SuppressWarnings
    fun setColorMode(colorSeekBarMode: ColorSeekBarMode) {
        this.mColorSeekBarMode = colorSeekBarMode
        if (colorSeekBarMode == ColorSeekBarMode.SELECT) {
            this.gradientColors = selectColors
        } else {
            this.gradientColors = colorsLightness(Color.RED)
        }
        setColorsDrawable()
    }

    fun changeColor(@ColorInt color: Int) {
        this.mColorSeekBarMode = ColorSeekBarMode.LIGHTNESS
        this.gradientColors = colorsLightness(color)
        setColorsDrawable()
    }

    fun getColorPosition(color: Int): Int {
        return selectColors.indexOfFirst { it == color }
    }

    fun getAmount(): Float {
        return getAmount(progress)
    }

    private fun getAmount(progress: Int): Float {
        return (progress - (max / 2f)) / max
    }

    private fun setColorsDrawable() {
        // 设置进度条样式
        this.progressDrawable = createTrackDrawable(gradientColors)
    }

    private fun colorsLightness(@ColorInt color: Int): IntArray {
        return IntArray(361) { i ->
            color.colorLightness(getAmount(i))
        }
    }

    private fun createTrackDrawable(gradientColors: IntArray): Drawable {
        return LayerDrawable(arrayOf(
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                colors = gradientColors
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
                cornerRadius = trackHeight / 2
            }
        )).apply {
            setLayerHeight(0, trackHeight.toInt())
            setLayerGravity(0, Gravity.CENTER_VERTICAL)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateThumbColor()
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        updateThumbColor()
    }

    fun updateThumbColor() {
        DrawableCompat.setTint(thumb, getCurrentColor())
        invalidate()
    }

    fun getCurrentColor(): Int {
        return gradientColors[progress.coerceIn(0, 360)]
    }

}

enum class ColorSeekBarMode {
    SELECT,
    LIGHTNESS
}
