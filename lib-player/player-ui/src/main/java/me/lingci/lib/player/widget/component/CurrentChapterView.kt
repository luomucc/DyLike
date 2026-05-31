package me.lingci.lib.player.widget.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import me.lingci.lib.player.ui.databinding.LayoutCurrentChapterViewBinding
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import me.lingci.lib.player.chapter.ChapterNode

/**
 * 当前章节显示组件
 * 显示当前正在播放的章节名称
 */
class CurrentChapterView : FrameLayout, IControlComponent {

    private var binding: LayoutCurrentChapterViewBinding =
        LayoutCurrentChapterViewBinding.inflate(LayoutInflater.from(context), this, true)

    private var currentChapter: ChapterNode? = null
    private var onClick: (() -> Unit)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    init {
        visibility = GONE
        binding.root.setOnClickListener { onClick?.invoke() }
    }

    /**
     * 更新当前章节
     */
    fun updateChapter(chapter: ChapterNode?) {
        currentChapter = chapter
        if (chapter != null) {
            binding.chapterName.text = chapter.title
            visibility = VISIBLE
        } else {
            visibility = GONE
        }
    }

    /**
     * 设置点击回调
     */
    fun setOnClickListener(listener: () -> Unit) {
        this.onClick = listener
    }

    override fun attach(controlWrapper: ControlWrapper) {}
    override fun getView(): View = this
    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation) {}
    override fun onPlayStateChanged(playState: Int) {}
    override fun onPlayerStateChanged(playerState: Int) {}
    override fun setProgress(duration: Int, position: Int) {}
    override fun onLockStateChanged(isLocked: Boolean) {}

}