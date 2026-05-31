package me.lingci.lib.base.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import me.lingci.lib.base.R
import me.lingci.lib.base.databinding.ItemDialogActionBinding
import me.lingci.lib.base.ui.BaseAdapter

/**
 *   @author : happyc
 *   time    : 2025/03/12
 *   desc    :
 *   version : 1.0
 */
class ActionItemAdapter(
    dataSet: MutableList<String>
) : BaseAdapter<String, ItemDialogActionBinding>(dataSet) {

    private var selected = -1

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): ItemDialogActionBinding {
        return ItemDialogActionBinding.inflate(inflater, parent, false)
    }

    override fun bindData(binding: ItemDialogActionBinding, item: String, position: Int) {
        binding.root.text = item
        binding.root.setOnClickListener {
            onItemClick?.invoke(item, position)
        }
        binding.root.setTextColor(if (selected == position) binding.root.context.resources.getColor(R.color.purple_300) else binding.root.context.resources.getColor(R.color.text_secondary))
    }

    fun select(position: Int) {
        selected = position
        notifyAllChanged()
    }

}