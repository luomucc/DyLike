package me.lingci.lib.player.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.lingci.lib.player.ui.databinding.ItemChapterBinding
import me.lingci.lib.player.chapter.ChapterNode

/**
 * 章节列表适配器
 */
class ChapterAdapter(
    chapters: MutableList<ChapterNode> = mutableListOf()
) : me.lingci.lib.base.ui.BaseAdapter<ChapterNode, ItemChapterBinding>(chapters) {

    private var currentChapterIndex: Int = -1

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): ItemChapterBinding {
        return ItemChapterBinding.inflate(inflater, parent, false)
    }

    override fun bindData(
        binding: ItemChapterBinding,
        item: ChapterNode,
        position: Int
    ) {
        binding.chapterIndex.text = "${item.index + 1}"
        binding.chapterTitle.text = item.title

        // 显示时间范围
        val timeText = if (item.endTimeMs != null) {
            "${item.getFormattedStartTime()} - ${item.getFormattedEndTime()}"
        } else {
            item.getFormattedStartTime()
        }
        binding.chapterTime.text = timeText

        // 当前章节高亮
        val isCurrentChapter = position == currentChapterIndex
        binding.chapterPlaying.visibility = if (isCurrentChapter) View.VISIBLE else View.GONE

        // 高亮当前章节背景
        if (isCurrentChapter) {
            binding.root.alpha = 1.0f
        } else {
            binding.root.alpha = 0.7f
        }

        binding.root.setOnClickListener {
            onItemClick?.invoke(item, position)
        }
    }

    /**
     * 更新当前播放的章节
     */
    fun setCurrentChapter(index: Int) {
        if (index == currentChapterIndex) return

        val oldIndex = currentChapterIndex
        currentChapterIndex = index

        if (oldIndex in dataList.indices) {
            notifyItemChanged(oldIndex)
        }
        if (index in dataList.indices) {
            notifyItemChanged(index)
        }
    }

    /**
     * 更新章节数据
     */
    fun updateChapters(chapters: List<ChapterNode>) {
        dataList.clear()
        dataList.addAll(chapters)
        notifyAllChanged()
    }
}
