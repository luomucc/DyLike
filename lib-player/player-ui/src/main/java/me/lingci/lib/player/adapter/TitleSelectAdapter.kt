package me.lingci.lib.player.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import me.lingci.lib.base.entity.TitleItem
import me.lingci.lib.base.ui.BaseAdapter
import me.lingci.lib.player.ui.databinding.TitleItemBinding

/**
 *   author : happyc
 *   e-mail : bafs.jy@live.com
 *   time   : 2025/01/20
 *   desc   : 标题
 *   version: 1.0
 */
class TitleSelectAdapter(
    dataSet: MutableList<TitleItem>
) : BaseAdapter<TitleItem, TitleItemBinding>(dataSet) {

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): TitleItemBinding {
        return TitleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun bindData(
        binding: TitleItemBinding,
        item: TitleItem,
        position: Int
    ) {
        binding.title.text = item.title
        binding.title.isSelected = item.selected
        binding.root.setOnClickListener {
            onItemClick?.invoke(item, position)
            selected(position)
        }
    }

    fun selected(position: Int) {
        if (dataList.isEmpty()) {
            return
        }
        if (position < 0 || position >= dataList.size) {
            return
        }
        val tempPosition = dataList.indexOfFirst { it.selected }
        if (tempPosition != -1) {
            dataList[tempPosition].selected = false
            notifyItemChanged(tempPosition)
        }
        dataList[position].selected = true
        notifyItemChanged(position)
    }

    fun cleanSelect() {
        dataList.forEach { item ->
            item.selected = false
        }
        notifyAllChanged()
    }
    
    fun reverse() {
        dataList.reverse()
        notifyAllChanged()
    }
    
}