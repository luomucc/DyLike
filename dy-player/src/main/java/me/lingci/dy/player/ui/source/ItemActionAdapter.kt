package me.lingci.dy.player.ui.source

import android.view.LayoutInflater
import android.view.ViewGroup
import me.lingci.dy.player.databinding.ItemActionListBinding
import me.lingci.lib.base.ui.BaseAdapter

/**
 *   @author : happyc
 *   time    : 2025/07/16
 *   desc    :
 *   version : 1.0
 */
open class ItemActionAdapter(
    dataSet: MutableList<ItemAction>
) : BaseAdapter<ItemAction, ItemActionListBinding>(dataSet) {

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): ItemActionListBinding {
        return ItemActionListBinding.inflate(inflater, parent, false)
    }

    override fun bindData(binding: ItemActionListBinding, item: ItemAction, position: Int) {
        binding.actionItem.text = item.title
        item.color?.let {
            binding.actionItem.setTextColor(binding.root.resources.getColor(it, null))
        }

        binding.root.setOnClickListener {
            onItemClick?.invoke(item, position)
        }
    }

}