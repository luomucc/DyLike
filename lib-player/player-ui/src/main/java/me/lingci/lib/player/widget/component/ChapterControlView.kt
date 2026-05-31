package me.lingci.lib.player.widget.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import me.lingci.lib.player.adapter.ChapterAdapter
import me.lingci.lib.player.ui.databinding.LayoutChapterControlViewBinding
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import me.lingci.lib.player.chapter.ChapterNode

/**
 * 章节列表控制视图
 * 显示视频章节列表，支持点击跳转
 */
class ChapterControlView : FrameLayout, IControlComponent {

    companion object {
        private const val TAG = "ChapterControlView"
    }

    private var binding: LayoutChapterControlViewBinding =
        LayoutChapterControlViewBinding.inflate(LayoutInflater.from(context), this, true)

    private lateinit var chapterAdapter: ChapterAdapter
    private lateinit var controlWrapper: ControlWrapper

    private var onChapterClick: ((chapter: ChapterNode) -> Unit)? = null
    private var chapters: List<ChapterNode> = emptyList()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    init {
        visibility = GONE
        initListener()
        initRecyclerView()
    }

    private fun initListener() {
        setOnClickListener { toggleVisibility() }
        binding.container.setOnClickListener { }
        binding.actionClose.setOnClickListener { toggleVisibility() }
    }

    private fun initRecyclerView() {
        chapterAdapter = ChapterAdapter()
        binding.chapterRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.chapterRecyclerView.adapter = chapterAdapter

        chapterAdapter.onItemClick { item, _ ->
            onChapterClick?.invoke(item)
            toggleVisibility()
        }
    }

    /**
     * 设置章节列表
     */
    fun setChapters(chapters: List<ChapterNode>) {
        this.chapters = chapters
        chapterAdapter.updateChapters(chapters)
        binding.chapterCount.text = "${chapters.size} 章节"
    }

    /**
     * 更新当前播放的章节
     */
    fun setCurrentChapter(chapter: ChapterNode?) {
        chapter?.let {
            chapterAdapter.setCurrentChapter(it.index)
        }
    }

    /**
     * 设置章节点击回调
     */
    fun setOnChapterClickListener(listener: (ChapterNode) -> Unit) {
        this.onChapterClick = listener
    }

    fun toggleVisibility() {
        visibility = if (visibility == VISIBLE) GONE else VISIBLE
    }

    override fun attach(controlWrapper: ControlWrapper) {
        this.controlWrapper = controlWrapper
    }

    override fun getView(): View = this

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation) {}
    override fun onPlayStateChanged(playState: Int) {}
    override fun onPlayerStateChanged(playerState: Int) {}
    override fun setProgress(duration: Int, position: Int) {}
    override fun onLockStateChanged(isLocked: Boolean) {}
}
