package me.lingci.lib.player.widget.component

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import me.lingci.lib.dm.view.entity.xml.DmItem
import me.lingci.lib.player.adapter.DmListAdapter
import me.lingci.lib.player.ui.databinding.LayoutDmListControlViewBinding
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import xyz.doikki.videoplayer.util.PlayerUtils

/**
 *   @author : lingci
 *   time    : 2026/01/23
 *   desc    : 弹幕列表
 *   version : 1.0
 */
class DmListControlView : FrameLayout, IControlComponent {

    companion object {
        private const val TAG = "DmListControlView"
    }

    private var binding: LayoutDmListControlViewBinding = LayoutDmListControlViewBinding.inflate(
        LayoutInflater.from(
            context
        ), this, true
    )
    private var followUs = false
    private var tempUs = false
    private var fastScroll = false
    private lateinit var dmListAdapter: DmListAdapter
    private lateinit var controlWrapper: ControlWrapper

    private var onChangeTime: ((time: Long, position: Int) -> Unit)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun setOnChangeTimeListener(onChangeTime: ((time: Long, position: Int) -> Unit)) {
        this.onChangeTime = onChangeTime
    }

    @SuppressLint("SetTextI18n")
    fun setDmList(items: List<DmItem>) {
        dmListAdapter.updateData(items)
        if (items.isEmpty()) {
            return
        }
        val min = PlayerUtils.stringForTime((items.minBy { it.time }.time * 1000).toInt())
        val max = PlayerUtils.stringForTime((items.maxBy { it.time }.time * 1000).toInt())
        binding.tvInfo.text = "${min}-${max}  共 ${items.size} 条弹幕"
    }

    fun cleanDmList() {
        dmListAdapter.updateData(emptyList())
        binding.tvInfo.text = "暂未装载弹幕"
    }

    fun scrollToPosition(currentTime: Long, smoothScroll: Boolean = false) {
        if (dmListAdapter.itemCount <= 0) {
            return
        }
        val index = dmListAdapter.getData()
            .indexOfFirst { item -> (item.time * 1000).toLong() >= currentTime }
        if (index > -1) {
            if (smoothScroll) {
                binding.recyclerView.smoothScrollToPosition(index)
            } else {
                fastScroll = false
                binding.recyclerView.scrollToPosition(index)
            }
        }
    }

    init {
        visibility = GONE
        initListener()
        initRecyclerView()
    }

    private fun initListener() {
        setOnClickListener { switchVib() }
        binding.container.setOnClickListener { }
        binding.actionClose.setOnClickListener { switchVib() }
        binding.swFollow.setOnClickListener {
            fastScroll = true
            followUs = binding.swFollow.isChecked
        }
    }

    private fun initRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        dmListAdapter = DmListAdapter(arrayListOf())
        binding.recyclerView.adapter = dmListAdapter
        dmListAdapter.onItemClick { item, position ->

            //switchVib()
        }
        binding.fastScroller.scrollNow()
        binding.fastScroller.attachRecyclerView(binding.recyclerView)
        binding.fastScroller.setOnScrollListener { _, type ->
            when (type) {
                0 -> {
                    tempUs = followUs
                    followUs = false
                }

                2 -> {
                    fastScroll = true
                    followUs = tempUs
                }
            }
        }
    }

    fun switchVib() {
        visibility = if (isVisible) GONE else VISIBLE
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
    override fun setProgress(duration: Int, position: Int) {
        if (isVisible) {
            binding.time.text = PlayerUtils.stringForTime(position)
        }
        if (isVisible && followUs) {
            scrollToPosition(position.toLong(), fastScroll.not())
        }
    }

    override fun onLockStateChanged(isLocked: Boolean) {}

}