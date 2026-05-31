package me.lingci.dy.player.ui.tool

import android.view.LayoutInflater
import android.view.ViewGroup
import me.lingci.dy.player.databinding.ItemDmFilterChipBinding
import me.lingci.lib.base.ui.BaseAdapter

class DmFilterChipAdapter(
    dataSet: ArrayList<String>,
    private val onRemove: (String) -> Unit
) : BaseAdapter<String, ItemDmFilterChipBinding>(dataSet) {

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): ItemDmFilterChipBinding {
        return ItemDmFilterChipBinding.inflate(inflater, parent, false)
    }

    override fun bindData(binding: ItemDmFilterChipBinding, item: String, position: Int) {
        binding.root.text = item
        binding.root.isCloseIconVisible = true
        binding.root.setOnCloseIconClickListener {
            onRemove.invoke(item)
        }
    }

}
