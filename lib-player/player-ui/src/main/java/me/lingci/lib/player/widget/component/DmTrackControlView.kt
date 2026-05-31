package me.lingci.lib.player.widget.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import me.lingci.lib.base.util.ToastUtil
import me.lingci.lib.dm.view.entity.DmTrack
import me.lingci.lib.dm.view.entity.DmTrackConf
import me.lingci.lib.dm.view.entity.DmTrackMode
import me.lingci.lib.player.ui.R
import me.lingci.lib.player.adapter.DmTrackAdapter
import me.lingci.lib.player.ui.databinding.LayoutDmTrackControlViewBinding
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import androidx.core.view.isVisible

/**
 *   @author : happyc
 *   time    : 2025/02/25
 *   desc    : 弹幕轨道
 *   version : 1.0
 */
class DmTrackControlView : FrameLayout, IControlComponent {

    companion object {
        private const val TAG = "DmTrackControlView"
    }

    private var binding: LayoutDmTrackControlViewBinding = LayoutDmTrackControlViewBinding.inflate(
        LayoutInflater.from(
            context
        ), this, true
    )
    private lateinit var dmTrackAdapter: DmTrackAdapter
    private lateinit var controlWrapper: ControlWrapper
    private var dmTrackConf: DmTrackConf = DmTrackConf()

    private var onChangeTrack: ((name: DmTrack, position: Int) -> Unit)? = null
    private var onMergeTrack: ((onSave: Boolean) -> Unit)? = null
    private var onDmOffset: ((offset: Long) -> Unit)? = null
    private var onRemoveTrack: (() -> Unit)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun setOnChangeTrackListener(onChangeTrack: ((track: DmTrack, position: Int) -> Unit)) {
        this.onChangeTrack = onChangeTrack
    }

    fun setOnMergeTrackListener(onMergeTrack: ((onSave: Boolean) -> Unit)) {
        this.onMergeTrack = onMergeTrack
    }

    fun setOnDmOffsetListener(onDmOffset: ((offset: Long) -> Unit)) {
        this.onDmOffset = onDmOffset
    }

    fun setOnRemoveTrackListener(onRemoveTrack: (() -> Unit)) {
        this.onRemoveTrack = onRemoveTrack
    }

    fun setConf(conf: DmTrackConf) {
        dmTrackConf = conf
        binding.rgDmTrackMode.check(if (conf.trackMode == DmTrackMode.SINGLE_SWITCH) R.id.rb_track_single else R.id.rb_track_multi)
    }

    fun setDmTrack(data: ArrayList<DmTrack>) {
        dmTrackAdapter.setData(data)
    }

    fun findDmTrack(dmTrack: DmTrack): Boolean {
        return dmTrackAdapter.findData(dmTrack)
    }

    fun addDmTrack(dmTrack: DmTrack) {
        dmTrackAdapter.addData(dmTrack)
    }

    fun findTrack(dmTrack: DmTrack): DmTrack? {
        return dmTrackAdapter.findTrack(dmTrack)
    }

    fun selectTrack(dmTrack: DmTrack): DmTrack? {
        val track = dmTrackAdapter.findTrack(dmTrack) ?: return null
        val position = dmTrackAdapter.getData().indexOf(track)
        if (position > -1) {
            dmTrackAdapter.changeSelect(position)
        }
        return track
    }

    fun allTrack(): ArrayList<DmTrack> {
        return dmTrackAdapter.getData()
    }

    fun listSelectedTrack(): MutableList<DmTrack> {
        return dmTrackAdapter.getData().filter { it.selected }.toMutableList()
    }

    fun dataSize(): Int {
        return dmTrackAdapter.itemCount
    }

    fun cleanData() {
        dmTrackAdapter.clear()
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
        binding.rgDmTrackMode.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_track_single) {
                dmTrackConf.trackMode = DmTrackMode.SINGLE_SWITCH
                binding.clAction.visibility = GONE
            } else {
                dmTrackConf.trackMode = DmTrackMode.MULTI_MERGE
                binding.clAction.visibility = VISIBLE
            }
            dmTrackAdapter.setTrackMode(dmTrackConf.trackMode)
        }
        binding.actionSelectAll.setOnClickListener {
            dmTrackAdapter.selectAll()
        }
        binding.actionLoad.setOnClickListener {
            if (dmTrackAdapter.checked()) {
                dmTrackAdapter.checkSelect()
                onMergeTrack?.invoke(false)
            } else {
                ToastUtil.showToast(context, "请先勾选弹幕")
            }
        }
        binding.actionOutput.setOnClickListener {
            if (dmTrackAdapter.checked()) {
                onMergeTrack?.invoke(true)
            } else{
                ToastUtil.showToast(context, "请先勾选弹幕")
            }
        }
        binding.actionRemove.setOnClickListener {
            if(dmTrackAdapter.checked()) {
                dmTrackAdapter.removedChecked()
                onRemoveTrack?.invoke()
            } else {
                ToastUtil.showToast(context, "请先勾选弹幕")
            }
        }
    }


    private fun initRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        dmTrackAdapter = DmTrackAdapter(ArrayList())
        binding.recyclerView.adapter = dmTrackAdapter
        dmTrackAdapter.setOnChangeTrackListener { track, hide, position ->
            onChangeTrack?.invoke(track, position)
            if (hide) {
                switchVib()
            }
        }
        dmTrackAdapter.setOnDmOffsetListener {offset ->
            onDmOffset?.invoke(offset)
        }
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
