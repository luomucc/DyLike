package me.lingci.lib.player.widget.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.view.isVisible
import me.lingci.lib.base.json.JSON
import me.lingci.lib.base.util.SpBase
import me.lingci.lib.base.view.EffectType
import me.lingci.lib.player.ui.databinding.LayoutVideoScaleControlViewBinding
import me.lingci.lib.player.widget.linstener.VideoScaleListener
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent

/**
 * @author : happyc
 * time    : 2025/03/29
 * desc    : 视频缩放位移
 * version : 1.0
 */
class VideoScaleControlView : FrameLayout, IControlComponent {

    companion object {
        private const val KEY_ZOOM = "zoom"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
    }

    private var binding: LayoutVideoScaleControlViewBinding =
        LayoutVideoScaleControlViewBinding.inflate(
            LayoutInflater.from(
                context
            ), this, true
        )
    private lateinit var controlWrapper: ControlWrapper
    private var scaleListener: VideoScaleListener? = null
    private var currentZoom = 50
    private var currentX = 125
    private var currentY = 125
    private val spBase by lazy { SpBase(context) }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun setOnVideoScaleListener(listener: VideoScaleListener) {
        this.scaleListener = listener
    }

    init {
        visibility = GONE
        initView()
        loadCache()
        initListener()
    }

    private fun initView() {
        binding.ivZoom.isSelected = true
        binding.seekbarWidth.progress = 125
        binding.seekbarHeight.progress = 50
    }

    private fun loadCache() {
        spBase.videoScaleConf?.let { json ->
            if (json == "{}") return@let
            val conf = JSON.parseObject(json)
            if (conf.keys().isEmpty()) return@let

            currentZoom = conf.getInt(KEY_ZOOM)
            currentX = conf.getInt(KEY_X)
            currentY = conf.getInt(KEY_Y)

            binding.seekbarWidth.progress = currentX
            binding.seekbarHeight.progress = if (getType() == 1) currentZoom else currentY
        }
    }

    private fun saveCache() {
        val conf = JSON.parseObject(spBase.videoScaleConf ?: "{}")
        conf.putValue(KEY_ZOOM, currentZoom)
        conf.putValue(KEY_X, currentX)
        conf.putValue(KEY_Y, currentY)
        spBase.videoScaleConf = conf.toJsonStr()
    }

    fun applyCache() {
        scaleListener?.let { listener ->
            listener.onScale((currentZoom + 50) / 100f)
            listener.onScroll((currentX - 125f) * -20f, 650f, 0)
            listener.onScroll((currentY - 125f) * 20f, 650f)
        }
    }

    private fun initListener() {
        setOnClickListener { switchVib() }
        binding.container.setOnClickListener { }
        binding.ivZoom.setOnClickListener {
            binding.ivZoom.isSelected = true
            binding.ivScroll.isSelected = false
            binding.seekbarHeight.progress = currentZoom
            saveCache()
        }
        binding.ivScroll.setOnClickListener {
            binding.ivScroll.isSelected = true
            binding.ivZoom.isSelected = false
            binding.seekbarHeight.progress = currentY
            saveCache()
        }
        binding.ivReset.setOnClickListener {
            resetValue()
            scaleListener?.onScale(1f)
            scaleListener?.onScroll(0f, 0f)
            scaleListener?.onScroll(0f, 0f, 0)
        }
        binding.seekbarWidth.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentX = progress
                    scaleListener?.onScroll((progress - 125f) * -20f, 650f, 0)
                    saveCache()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        binding.seekbarHeight.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    if (getType() == 1) {
                        currentZoom = progress
                        scaleListener?.onScale((progress + 50) / 100f)
                    } else {
                        currentY = progress
                        scaleListener?.onScroll((progress - 125f) * 20f, 650f)
                    }
                    saveCache()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        binding.effectGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.effectNone.id -> scaleListener?.onEffectChange(null)
                binding.effectStar.id -> scaleListener?.onEffectChange(EffectType.STAR)
                binding.effectSnow.id -> scaleListener?.onEffectChange(EffectType.SNOW)
                binding.effectMeteor.id -> scaleListener?.onEffectChange(EffectType.METEOR)
            }
        }
    }

    private fun getType(): Int {
        return when {
            binding.ivZoom.isSelected -> 1
            else -> 2
        }
    }

    fun resetValue() {
        currentZoom = 50
        currentX = 125
        currentY = 125
        binding.seekbarWidth.progress = 125
        binding.seekbarHeight.progress = 50
        binding.ivZoom.isSelected = true
        binding.ivScroll.isSelected = false
        saveCache()
    }

    fun switchVib() {
        if (this.isVisible) {
            this.visibility = GONE
        } else {
            this.visibility = VISIBLE
        }
    }

    override fun attach(controlWrapper: ControlWrapper) {
        this.controlWrapper = controlWrapper
    }

    override fun getView(): View {
        return this
    }

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation) {}
    override fun onPlayStateChanged(playState: Int) {}
    override fun onPlayerStateChanged(playerState: Int) {}
    override fun setProgress(duration: Int, position: Int) {}
    override fun onLockStateChanged(isLocked: Boolean) {}

}
