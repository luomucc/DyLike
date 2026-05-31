package me.lingci.lib.player.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import me.lingci.lib.base.ui.BaseAdapter
import me.lingci.lib.base.util.formatTime
import me.lingci.lib.base.util.toColor
import me.lingci.lib.dm.view.entity.xml.DmItem
import me.lingci.lib.player.ui.R
import me.lingci.lib.player.ui.databinding.ItemDmListBinding

/**
 *   @author : happyc
 *   time    : 2026/01/23
 *   desc    :
 *   version : 1.0
 */
class DmListAdapter(
    dataSet: MutableList<DmItem>
) : BaseAdapter<DmItem, ItemDmListBinding>(dataSet) {

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): ItemDmListBinding {
        return ItemDmListBinding.inflate(inflater, parent, false)
    }

    override fun bindData(binding: ItemDmListBinding, item: DmItem, position: Int) {
        val ps = item.style.split(",")
        val color = ps[3].toColor()
        if (color == Color.WHITE) {
            binding.itemTitle.setTextColor(binding.root.context.getColor(me.lingci.lib.base.R.color.white))
        } else {
            binding.itemTitle.setTextColor(color)
        }
        binding.itemTime.text = (ps[0].toDouble() * 1000).toInt().formatTime()
        binding.itemTitle.text = item.content
        binding.root.setOnClickListener {
            onItemClick?.invoke(item, position)
        }
    }

}
