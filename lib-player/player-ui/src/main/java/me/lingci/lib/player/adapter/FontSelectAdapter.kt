package me.lingci.lib.player.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import me.lingci.lib.base.entity.TitleItem
import me.lingci.lib.base.ui.BaseAdapter
import me.lingci.lib.player.ui.databinding.FontTitleItemBinding
import java.io.File

/**
 *   author : happyc
 *   e-mail : bafs.jy@live.com
 *   time   : 2025/04/09
 *   desc   : 字体选择
 *   version: 1.0
 */
class FontSelectAdapter(
    dataSet: ArrayList<TitleItem>
) : BaseAdapter<TitleItem, FontTitleItemBinding>(dataSet) {

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): FontTitleItemBinding {
        return FontTitleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun bindData(binding: FontTitleItemBinding, item: TitleItem, position: Int) {
        binding.title.text = item.title
        binding.title.isSelected = item.selected
        item.name.let { path ->
            File(path).let {file ->
                if (file.exists()) {
                    binding.title.typeface = Typeface.createFromFile(file)
                }
            }
        }
        binding.root.setOnClickListener {
            onItemClick?.invoke(item, position)
            selected(position)
        }
    }

    fun selected(position: Int) {
        if (dataEmpty()) {
            return
        }
        if (position < 0 || position >= itemCount) {
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

    fun selected(name: String) {
        if (dataEmpty()) {
            return
        }
        if (name.isBlank()) {
            return
        }
        dataList.forEachIndexed { index, item ->
            if (item.selected) {
                item.selected = false
                notifyItemChanged(index)
            }
            if (item.name == name) {
                item.selected = true
                notifyItemChanged(index)
            }
        }
    }

}