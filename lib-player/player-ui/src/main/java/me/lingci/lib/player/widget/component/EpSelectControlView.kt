package me.lingci.lib.player.widget.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import me.lingci.lib.base.entity.TitleItem
import me.lingci.lib.player.adapter.EpisodeSelectAdapter
import me.lingci.lib.player.ui.databinding.LayoutEpSelectControlViewBinding
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent

/**
 * @author : happyc
 * time    : 2025/01/20
 * desc    : 选集
 * version : 1.0
 */
class EpSelectControlView : FrameLayout, IControlComponent {

    companion object {
        private const val TAG = "EpSelectControlView"
    }

    private var binding: LayoutEpSelectControlViewBinding = LayoutEpSelectControlViewBinding.inflate(
        LayoutInflater.from(
            context
        ), this, true
    )
    private lateinit var mEpisodeSelectAdapter: EpisodeSelectAdapter
    private lateinit var controlWrapper: ControlWrapper
    private  var onSelectEp: ((item: TitleItem, position: Int) -> Unit)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun setOnEpSelectListener(onSelectEp: (item: TitleItem, position: Int) -> Unit) {
        this.onSelectEp = onSelectEp
    }

    fun setData(data: List<TitleItem>) {
        mEpisodeSelectAdapter.setData(data)
    }

    fun selected(position: Int) {
        var selected = position
        if (binding.actionSort.isSelected) {
            selected = mEpisodeSelectAdapter.itemCount - 1 - position
        }
        mEpisodeSelectAdapter.selected(selected)
    }

    init {
        visibility = GONE
        initView()
        initListener()
        initRecyclerView()
    }

    private fun initListener() {
        setOnClickListener { switchVib() }
        binding.container.setOnClickListener {  }
        binding.actionClose.setOnClickListener { switchVib() }
    }

    private fun initView() {
        binding.actionViewLayout.setOnClickListener {
            val isSelected = binding.actionViewLayout.isSelected
            binding.actionViewLayout.isSelected = !isSelected
            if (isSelected) {
                binding.recyclerView.layoutManager = LinearLayoutManager(context)
            } else {
                binding.recyclerView.layoutManager = GridLayoutManager(context, 2)
            }
        }
        binding.actionSort.setOnClickListener {
            val isSelected = binding.actionSort.isSelected
            binding.actionSort.isSelected = !isSelected
            mEpisodeSelectAdapter.reverse()
        }
    }

    private fun initRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        mEpisodeSelectAdapter = EpisodeSelectAdapter(mutableListOf())
        binding.recyclerView.adapter = mEpisodeSelectAdapter
        mEpisodeSelectAdapter.setOnItemClickListener { episode, position ->
            var selected = position
            if (binding.actionSort.isSelected) {
                selected = mEpisodeSelectAdapter.itemCount - 1 - position
            }
            onSelectEp?.invoke(episode, selected)
            switchVib()
        }
    }

    fun switchVib() {
        if (this.visibility == VISIBLE) {
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