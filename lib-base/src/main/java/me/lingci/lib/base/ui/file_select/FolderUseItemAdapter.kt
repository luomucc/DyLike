package me.lingci.lib.base.ui.file_select

import android.view.LayoutInflater
import android.view.ViewGroup
import me.lingci.lib.base.databinding.ItemFolderUseBinding
import me.lingci.lib.base.ui.BaseAdapter
import me.lingci.lib.base.util.subFileName

class FolderUseItemAdapter(
    dataSet: MutableList<String>
) : BaseAdapter<String, ItemFolderUseBinding>(dataSet) {

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): ItemFolderUseBinding {
        return ItemFolderUseBinding.inflate(inflater, parent, false)
    }

    override fun bindData(binding: ItemFolderUseBinding, item: String, position: Int) {
        binding.tvTitle.text = item.subFileName()
        binding.root.setOnClickListener {
            onItemClick?.invoke(item, position)
        }
    }

    fun removeItem(path: String) {
        var position = -1
        dataList.forEachIndexed { index, s ->
            if (s == path) {
                position = index
            }
        }
        if (position != -1) {
            removeItem(position)
        }
    }

}