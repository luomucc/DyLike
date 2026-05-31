package me.lingci.lib.player.widget.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import me.lingci.lib.base.entity.TitleItem
import me.lingci.lib.base.util.SpBase
import me.lingci.lib.player.adapter.TitleSelectAdapter
import me.lingci.lib.player.danmaku.PlayerInitializer
import me.lingci.lib.player.ui.databinding.LayoutSpeedControlViewBinding
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent

/**
 *   @author : lingci
 *   time    : 2026/01/18
 *   desc    : 倍数
 *   version : 1.0
 */
class SpeedControlView : FrameLayout, IControlComponent {

    companion object {
        private const val TAG = "SpeedControlView"
    }

    private var binding: LayoutSpeedControlViewBinding = LayoutSpeedControlViewBinding.inflate(
        LayoutInflater.from(
            context
        ), this, true
    )
    private lateinit var videoSpeedAdapter: TitleSelectAdapter
    private lateinit var longSpeedAdapter: TitleSelectAdapter
    private lateinit var controlWrapper: ControlWrapper

    private var onChangeSpeed: ((name: String, position: Int) -> Unit)? = null

    private val spBase by lazy { SpBase(context) }

    private val videoSpeedList = mutableListOf(
        TitleItem(title = "0.5X", name = "0.5"),
        TitleItem(title = "0.75X", name = "0.75"),
        TitleItem(title = "1X", name = "1", selected = true),
        TitleItem(title = "1.25X", name = "1.25"),
        TitleItem(title = "1.5X", name = "1.5"),
        TitleItem(title = "2X", name = "2"),
        TitleItem(title = "3X", name = "3"),
        TitleItem(title = "4X", name = "4"),
    )

    private val longSpeedList = mutableListOf(
        TitleItem(title = "1.5X", name = "1.5"),
        TitleItem(title = "2X", name = "2", selected = true),
        TitleItem(title = "2.5X", name = "2.5"),
        TitleItem(title = "3X", name = "3"),
        TitleItem(title = "3.5X", name = "3.5"),
        TitleItem(title = "4X", name = "4"),
    )

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun setOnChangeSpeedListener(onChangeSpeed: ((name: String, position: Int) -> Unit)) {
        this.onChangeSpeed = onChangeSpeed
    }

    init {
        visibility = GONE
        binding.title.text = "倍数设置"
        PlayerInitializer.changeLongSpeed(spBase.longSpeed)
        initListener()
        initRecyclerView()
    }

    private fun initListener() {
        setOnClickListener { switchVib() }
        binding.container.setOnClickListener { }
        binding.actionClose.setOnClickListener { switchVib() }
    }

    private fun initRecyclerView() {
        val videoLayoutManager = FlexboxLayoutManager(context)
        videoLayoutManager.flexDirection = FlexDirection.ROW
        videoLayoutManager.flexWrap = FlexWrap.WRAP
        videoLayoutManager.justifyContent = JustifyContent.FLEX_START
        binding.videoRecyclerView.layoutManager = videoLayoutManager

        videoSpeedAdapter = TitleSelectAdapter(videoSpeedList)
        binding.videoRecyclerView.adapter = videoSpeedAdapter
        videoSpeedAdapter.onItemClick { item, position ->
            PlayerInitializer.changeSpeed(item.name.toFloat())
            onChangeSpeed?.invoke(item.name, position)
            switchVib()
        }

        val longLayoutManager = FlexboxLayoutManager(context)
        longLayoutManager.flexDirection = FlexDirection.ROW
        longLayoutManager.flexWrap = FlexWrap.WRAP
        longLayoutManager.justifyContent = JustifyContent.FLEX_START
        binding.longRecyclerView.layoutManager = longLayoutManager
        longSpeedAdapter = TitleSelectAdapter(longSpeedList)
        binding.longRecyclerView.adapter = longSpeedAdapter
        longSpeedAdapter.onItemClick { item, position ->
            PlayerInitializer.changeLongSpeed(item.name.toFloat())
            spBase.longSpeed = item.name.toFloat()
        }
        longSpeedList.indexOfFirst { item -> item.name.toFloat() == PlayerInitializer.Player.pressVideoSpeed }.let {
            if (it > -1) {
                longSpeedAdapter.selected(it)
            }
        }
    }

    fun switchVib() {
        visibility = if (visibility == VISIBLE) GONE else VISIBLE
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