package me.lingci.lib.player.widget.component

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import me.lingci.lib.base.util.Log
import me.lingci.lib.base.util.dp
import me.lingci.lib.player.ui.BuildConfig
import me.lingci.lib.player.ui.databinding.LayoutSubtitleControlViewBinding
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import me.lingci.lib.player.subtitle.SubtitleCueGroup
import me.lingci.lib.player.subtitle.SubtitleCueListener
import java.io.File

/**
 * @author : happyc
 * time    : 2025/04/02
 * desc    : 字幕 overlay. Consumes generic subtitle cues instead of Media3 cue objects.
 * version : 1.0
 */
class SubtitleControlView : FrameLayout, IControlComponent, SubtitleCueListener {

    companion object {
        private const val SUBTITLE_LAYOUT_TRACE_TAG = "SubtitleLayoutTrace"
    }

    private var binding: LayoutSubtitleControlViewBinding =
        LayoutSubtitleControlViewBinding.inflate(
            LayoutInflater.from(
                context
            ), this, true
        )
    private lateinit var controlWrapper: ControlWrapper

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        visibility = visibility
        initView()
        initListener()
    }

    private fun initView() {
        binding.tvMaster.setStrokeWidth(3f.dp)
        binding.tvMaster.visibility = View.GONE
        binding.tvSecondary.visibility = View.GONE
        binding.subtitleView.visibility = View.VISIBLE
    }

    private fun initListener() {

    }

    fun clearText() {
        binding.tvMaster.text = ""
        binding.tvSecondary.text = ""
        binding.subtitleView.setCues(emptyList())
    }

    fun setSubtitleFractionalTextSize(fractionOfHeight: Float, ignorePadding: Boolean = false) {
        binding.subtitleView.setFractionalTextSize(fractionOfHeight, ignorePadding)
    }

    fun setSubtitleAbsoluteTextSize(sizeDp: Float) {
        binding.subtitleView.setTextSize(sizeDp.dp)
    }

    fun setSubtitleScale(scale: Float) {
        binding.subtitleView.setSubtitleScale(scale)
    }

    fun getSubtitleScale(): Float {
        return binding.subtitleView.getSubtitleScale()
    }

    fun setSubtitleFont(fontPath: String) {
        if (fontPath.isNotBlank()) {
            val file = File(fontPath)
            if (file.exists()) {
                binding.subtitleView.setTypeface(Typeface.createFromFile(file))
            }
        } else {
            binding.subtitleView.setTypeface(null)
        }
    }

    fun setSubtitleLayoutBounds(left: Int, top: Int, right: Int, bottom: Int) {
        // Callers pass host/render-space bounds. Convert them into SubtitleView-local coordinates
        // before constraining cue layout.
        val subtitleLeft = binding.subtitleView.left
        val subtitleTop = binding.subtitleView.top
        val localLeft = left - subtitleLeft
        val localTop = top - subtitleTop
        val localRight = right - subtitleLeft
        val localBottom = bottom - subtitleTop
        binding.subtitleView.setLayoutBounds(
            localLeft,
            localTop,
            localRight,
            localBottom
        )
        traceLayoutBounds(
            event = "subtitle_layout_bounds",
            mode = binding.subtitleView.getDockMode(),
            hostWidth = width,
            hostHeight = height,
            subtitleWidth = binding.subtitleView.width,
            subtitleHeight = binding.subtitleView.height,
            globalBounds = intArrayOf(left, top, right, bottom),
            localBounds = intArrayOf(localLeft, localTop, localRight, localBottom)
        )
    }

    fun clearSubtitleLayoutBounds() {
        binding.subtitleView.clearLayoutBounds()
        traceLayoutBounds(
            event = "subtitle_layout_bounds_clear",
            mode = binding.subtitleView.getDockMode(),
            hostWidth = width,
            hostHeight = height,
            subtitleWidth = binding.subtitleView.width,
            subtitleHeight = binding.subtitleView.height,
            globalBounds = null,
            localBounds = null
        )
    }

    fun setSubtitleDockMode(mode: Int) {
        binding.subtitleView.setDockMode(mode)
        traceLayoutBounds(
            event = "subtitle_dock_mode",
            mode = mode,
            hostWidth = width,
            hostHeight = height,
            subtitleWidth = binding.subtitleView.width,
            subtitleHeight = binding.subtitleView.height,
            globalBounds = null,
            localBounds = null
        )
    }

    fun setSubtitleWindowColor(color: Int) {
        binding.subtitleView.setWindowColor(color)
    }

    fun getDockMode(): Int {
        return binding.subtitleView.getDockMode()
    }

    override fun onSubtitleCues(cues: SubtitleCueGroup) {
        // SubtitleControlView is the bridge from backend SubtitleCueProvider to player-ui rendering.
        binding.subtitleView.setCues(cues)
    }

    override fun attach(controlWrapper: ControlWrapper) {
        this.controlWrapper = controlWrapper
    }

    override fun getView(): View {
        return this
    }

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation) {
        // Subtitle overlay stays visible even when controller chrome hides or shows.
        if (visibility != View.VISIBLE) {
            visibility = View.VISIBLE
        }
    }
    override fun onPlayStateChanged(playState: Int) {}
    override fun onPlayerStateChanged(playerState: Int) {}
    override fun setProgress(duration: Int, position: Int) {}
    override fun onLockStateChanged(isLocked: Boolean) {}

    private fun traceLayoutBounds(
        event: String,
        mode: Int,
        hostWidth: Int,
        hostHeight: Int,
        subtitleWidth: Int,
        subtitleHeight: Int,
        globalBounds: IntArray?,
        localBounds: IntArray?
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }
        Log.d(
            SUBTITLE_LAYOUT_TRACE_TAG,
            "event=$event",
            "mode=${binding.subtitleView.describeDockMode(mode)}",
            "host=${hostWidth}x${hostHeight}",
            "subtitle=${subtitleWidth}x${subtitleHeight}",
            "global=${formatBounds(globalBounds)}",
            "local=${formatBounds(localBounds)}"
        )
    }

    private fun formatBounds(bounds: IntArray?): String {
        if (bounds == null || bounds.size < 4) {
            return "null"
        }
        return "[${bounds[0]},${bounds[1]},${bounds[2]},${bounds[3]}]"
    }

}
