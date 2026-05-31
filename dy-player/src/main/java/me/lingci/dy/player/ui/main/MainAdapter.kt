package me.lingci.dy.player.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import me.lingci.lib.base.ui.BaseAdapter
import me.lingci.dy.player.databinding.ItemFileBrowserListBinding
import me.lingci.lib.base.storage.entity.FileEntity

/**
 * @author : happyc
 * time    : 2025/02/28
 * desc    :
 * version : 1.0
 */
class MainAdapter(
    dataList: ArrayList<FileEntity>
) :
    BaseAdapter<FileEntity, ItemFileBrowserListBinding>(
        dataList
    ) {

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): ItemFileBrowserListBinding {
        return ItemFileBrowserListBinding.inflate(inflater, parent, false)
    }

    override fun bindData(
        binding: ItemFileBrowserListBinding,
        item: FileEntity,
        position: Int
    ) {
        binding.tvTitle.text = item.name
        binding.root.setOnClickListener {
            onItemClick?.invoke(item, position)
        }
    }
}