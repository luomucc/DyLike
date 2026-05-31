package me.lingci.lib.base.ui.file_select

import android.view.LayoutInflater
import android.view.ViewGroup
import me.lingci.lib.base.databinding.ItemFolderIndexBinding
import me.lingci.lib.base.storage.entity.FileEntity
import me.lingci.lib.base.ui.BaseAdapter
import me.lingci.lib.base.util.notHome
import java.io.File

class FolderIndexItemAdapter(
    dataSet: MutableList<FileEntity>
) : BaseAdapter<FileEntity, ItemFolderIndexBinding>(dataSet) {

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): ItemFolderIndexBinding {
        return ItemFolderIndexBinding.inflate(inflater, parent, false)
    }

    override fun bindData(binding: ItemFolderIndexBinding, item: FileEntity, position: Int) {
        binding.tvTitle.text = item.name
        binding.tvTitle.isSelected = position == itemCount - 1
        binding.root.setOnClickListener {
            onItemClick?.invoke(item, position)
        }
    }

    fun changeDir(path: String) {
        synchronized(dataList) {
            val first = dataList.first()
            dataList.clear()
            dataList.add(first)
            var file = File(path)
            while (file.notHome()) {
                dataList.add(1, FileEntity(path = file.path, name = file.name))
                file = file.parentFile!!
            }
        }
        notifyAllChanged()
    }

}