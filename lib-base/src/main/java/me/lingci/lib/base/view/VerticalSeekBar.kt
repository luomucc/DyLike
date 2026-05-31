package me.lingci.lib.base.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar


class VerticalSeekBar(context: Context, attrs: AttributeSet) :
    AppCompatSeekBar(context, attrs) {


    private var onSeekBarChangeListener: OnSeekBarChangeListener? = null

    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener) {
        onSeekBarChangeListener = l
    }

    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        // 添加以下代码强制更新thumb位置
        onSizeChanged(width, height, 0, 0)
        invalidate()
    }

    override fun setProgress(progress: Int, animate: Boolean) {
        super.setProgress(progress, animate)
        // 添加以下代码强制更新thumb位置
        onSizeChanged(width, height, 0, 0)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(h, w, oldh, oldw)
    }

    @Synchronized
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec)
        setMeasuredDimension(measuredHeight, measuredWidth)
    }

    override fun onDraw(c: Canvas) {
        c.rotate(-90f)
        c.translate(-height.toFloat(), 0f)
        super.onDraw(c)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                onStartTrackingTouch()
                updateProgress(event)
            }
            MotionEvent.ACTION_MOVE -> {
                updateProgress(event)
                onSizeChanged(width, height, 0, 0)
            }
            MotionEvent.ACTION_UP -> {
                updateProgress(event)
                onStopTrackingTouch()
                parent.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_CANCEL -> {
                onStopTrackingTouch()
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    private fun updateProgress(event: MotionEvent) {
        progress = max - (max * event.y / height).toInt().coerceIn(0, max)
        onProgressChanged(this, progress, true)
    }

    private fun onStartTrackingTouch() {
        onSeekBarChangeListener?.onStartTrackingTouch(this)
    }

    private fun onStopTrackingTouch() {
        onSeekBarChangeListener?.onStopTrackingTouch(this)
    }

    private fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        onSeekBarChangeListener?.onProgressChanged(seekBar, progress, fromUser)
    }

}