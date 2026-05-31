package me.lingci.lib.base.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import me.lingci.lib.base.R
import me.lingci.lib.base.databinding.ItemFileOptionsBinding
import me.lingci.lib.base.storage.entity.FileEntity
import me.lingci.lib.base.ui.BaseAdapter
import me.lingci.lib.base.util.isImage
import me.lingci.lib.base.util.isText

class FileOptionsItemAdapter(
    dataSet: MutableList<FileEntity>
) : BaseAdapter<FileEntity, ItemFileOptionsBinding>(dataSet) {

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): ItemFileOptionsBinding {
        return ItemFileOptionsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun bindData(binding: ItemFileOptionsBinding, item: FileEntity, position: Int) {
        binding.itemTitle.text = item.title
        binding.itemLogo.setImageResource(when{
            item.path.isBlank() -> R.drawable.ic_file_none_56
            item.path.isImage() -> R.drawable.ic_file_image_56
            item.path.isText() -> R.drawable.ic_file_text_56
            else -> R.drawable.ic_file_none_56
        })
        binding.itemRemove.setOnClickListener {
            removeItem(position)
        }

    }

}