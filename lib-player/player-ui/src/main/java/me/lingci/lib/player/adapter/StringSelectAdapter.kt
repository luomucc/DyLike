package me.lingci.lib.player.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import me.lingci.lib.base.ui.BaseAdapter
import me.lingci.lib.player.ui.databinding.TitleCenterItemBinding

/**
 *   author : happyc
 *   e-mail : bafs.jy@live.com
 *   time   : 2025/03/20
 *   desc   :
 *   version: 1.0
 */
class StringSelectAdapter(
    dataSet: MutableList<String>
) : BaseAdapter<String, TitleCenterItemBinding>(dataSet) {

    private var selectPosition = -1

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): TitleCenterItemBinding {
        return TitleCenterItemBinding.inflate(inflater, parent, false)
    }

    override fun bindData(binding: TitleCenterItemBinding, item: String, position: Int) {
        binding.title.text = item
        binding.title.isSelected = (position == selectPosition)
        binding.root.setOnClickListener {
            val tmp = selectPosition
            selectPosition = position
            if (tmp != -1) {
                notifyItemChanged(tmp)
            }
            notifyItemChanged(position)
            onItemClick?.invoke(item, position)
        }
    }

    fun currentPosition(): Int {
        return selectPosition
    }

    fun setSelect(position: Int) {
        if (position < 0 || position > itemCount - 1) {
            return
        }
        val last = selectPosition
        selectPosition = position
        if (last != -1) {
            notifyItemChanged(last)
        }
        notifyItemChanged(position)
    }

}