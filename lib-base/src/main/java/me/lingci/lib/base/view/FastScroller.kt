package me.lingci.lib.base.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.core.view.isVisible

/**
 * 优化后的快速滚动条
 * 修复了：触控热区小、滑动冲突、内存泄漏、绘制性能及交互反馈
 */
class FastScroller @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 配置参数
    private val handleHeight = dpToPx(32f)
    private val visualWidth = dpToPx(8f)  // 视觉上的宽度
    private val touchTargetWidth = dpToPx(32f) // 实际响应触摸的宽度
    private val hideDelay = 1500L

    private var mRecyclerView: RecyclerView? = null
    private var adapter: RecyclerView.Adapter<*>? = null
    private var handleY = 0f
    private var isDragging = false

    private var scrollNow = false
    private var minVisibleItemCount = 16
    private var onScrollListener: ((Int, Int) -> Unit)? = null

    // 自动隐藏动画
    private var alphaAnimator: ValueAnimator? = null
    private var currentAlpha = 0 // 0-255

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80808080")
    }

    private val hideRunnable = Runnable { fadeOut() }
    private val adapterObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            updateVisibleState()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            updateVisibleState()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            updateVisibleState()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            updateVisibleState()
        }
    }

    init {
        // 初始状态隐藏
        alpha = 0f
    }

    fun setOnScrollListener(listener: (Int, Int) -> Unit) {
        this.onScrollListener = listener
    }

    fun scrollNow(now: Boolean = true) {
        scrollNow = now
    }

    fun minVisibleItemCount(count: Int) {
        minVisibleItemCount = count.coerceAtLeast(0)
        updateVisibleState()
    }

    fun changeColor(color: String) {
        handlePaint.color = Color.parseColor(color)
    }

    fun attachRecyclerView(recyclerView: RecyclerView) {
        adapter?.unregisterAdapterDataObserver(adapterObserver)
        this.mRecyclerView = recyclerView
        adapter = recyclerView.adapter
        adapter?.registerAdapterDataObserver(adapterObserver)
        updateVisibleState()
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!isDragging && isVisible) {
                    showImmediately()
                    updateHandlePosition()
                    postHideDelayed()
                }
            }
        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 赋予较大的测量宽度，确保触控热区足够
        val width = MeasureSpec.makeMeasureSpec(touchTargetWidth.toInt(), MeasureSpec.EXACTLY)
        super.onMeasure(width, heightMeasureSpec)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        if (mRecyclerView == null || alpha == 0f) return

        val range = height - handleHeight
        val safeHandleY = handleY.coerceIn(0f, range.coerceAtLeast(0f))

        // 绘制在 View 的右侧对齐，预留边距
        val rect = RectF(
            width - visualWidth - dpToPx(4f),
            safeHandleY,
            width - dpToPx(4f),
            safeHandleY + handleHeight
        )

        canvas.drawRoundRect(rect, visualWidth / 2, visualWidth / 2, handlePaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val recyclerView = mRecyclerView ?: return false
        val adapter = recyclerView.adapter ?: return false
        if (visibility != View.VISIBLE || adapter.itemCount == 0) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 仅点击在右侧响应区才触发拖拽
                if (event.x < width - touchTargetWidth) return false

                isDragging = true
                parent.requestDisallowInterceptTouchEvent(true) // 拦截父布局事件
                showImmediately()
                handleScrollEvent(event.y, 0)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                handleScrollEvent(event.y, 1)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                handleScrollEvent(event.y, 2)
                postHideDelayed()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleScrollEvent(touchY: Float, type: Int) {
        val recyclerView = mRecyclerView ?: return
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        if (itemCount == 0) return

        // 计算比例
        val availableHeight = height - handleHeight
        val progress = (touchY - handleHeight / 2).coerceIn(0f, availableHeight) / availableHeight

        handleY = progress * availableHeight

        // 映射到具体的 Position
        val targetPos = (progress * (itemCount - 1)).toInt()

        // 使用带有偏移的滚动，确保更精准
        if (scrollNow || type != 1) {
            when (recyclerView.layoutManager) {
                is LinearLayoutManager -> {
                    (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                        targetPos,
                        0
                    )
                }
                is StaggeredGridLayoutManager -> {
                    recyclerView.scrollToPosition(targetPos)
                }
            }
        }
        onScrollListener?.invoke(targetPos, type)
        invalidate()
    }

    private fun updateHandlePosition() {
        val rv = mRecyclerView ?: return
        val offset = rv.computeVerticalScrollOffset()
        val range = rv.computeVerticalScrollRange()
        val extent = rv.computeVerticalScrollExtent()

        val scrollRange = range - extent
        if (scrollRange <= 0) return

        val progress = offset.toFloat() / scrollRange
        handleY = progress * (height - handleHeight)
        invalidate()
    }

    private fun updateVisibleState() {
        val itemCount = mRecyclerView?.adapter?.itemCount ?: 0
        if (itemCount > minVisibleItemCount) {
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
            removeCallbacks(hideRunnable)
            alphaAnimator?.cancel()
            alpha = 0f
        }
    }

    // --- 动画处理 ---

    private fun showImmediately() {
        removeCallbacks(hideRunnable)
        alphaAnimator?.cancel()
        alpha = 1f
    }

    private fun postHideDelayed() {
        removeCallbacks(hideRunnable)
        postDelayed(hideRunnable, hideDelay)
    }

    private fun fadeOut() {
        alphaAnimator?.cancel()
        alphaAnimator = ValueAnimator.ofFloat(alpha, 0f).apply {
            duration = 300
            addUpdateListener { alpha = it.animatedValue as Float }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        adapter?.unregisterAdapterDataObserver(adapterObserver)
        adapter = null
        removeCallbacks(hideRunnable)
        alphaAnimator?.cancel()
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density

}
