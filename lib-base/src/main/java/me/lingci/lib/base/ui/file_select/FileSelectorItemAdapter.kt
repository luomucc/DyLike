package me.lingci.lib.base.ui.file_select

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.lingci.lib.base.R
import me.lingci.lib.base.databinding.ItemFileSelectorBinding
import me.lingci.lib.base.storage.entity.FileEntity
import me.lingci.lib.base.ui.BaseAdapter
import me.lingci.lib.base.util.FileOperator
import me.lingci.lib.base.util.childSize
import me.lingci.lib.base.util.isImage
import me.lingci.lib.base.util.isText
import me.lingci.lib.base.util.isVideo
import me.lingci.lib.base.util.sizeFormat
import java.io.File

class FileSelectorItemAdapter(
    dataSet: MutableList<FileEntity>
) : BaseAdapter<FileEntity, ItemFileSelectorBinding>(dataSet) {

    private var onItemSelect: ((item: FileEntity, position: Int) -> Unit)? = null
    private var onFolderMenu: ((v: View, item: FileEntity) -> Unit)? = null
    private var multiSelectMode = true

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): ItemFileSelectorBinding {
        return ItemFileSelectorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    @SuppressLint("SetTextI18n")
    override fun bindData(binding: ItemFileSelectorBinding, item: FileEntity, position: Int) {
        binding.tvTitle.text = item.name
        if (item.isFile) {
            binding.buttonMore.visibility = View.GONE
            binding.tvType.text = File(item.path).sizeFormat()
            binding.ivIcon.setImageResource(
                when {
                    item.name.isVideo() -> R.drawable.ic_file_video_56
                    item.name.isImage() -> R.drawable.ic_file_image_56
                    item.name.isText() -> R.drawable.ic_file_text_56
                    else -> R.drawable.ic_file_none_56
                }
            )
        } else {
            binding.buttonMore.visibility = View.VISIBLE
            if (item.path == FileOperator.downloadFolder.path || item.path == FileOperator.buildDownFile("弹目").path) {
                binding.buttonMore.visibility = View.GONE
            }
            binding.tvType.text = "目录 包含${File(item.path).childSize()}项"
            binding.ivIcon.setImageResource(R.drawable.ic_file_folder_56)
        }
        if (item.isFile && item.selected) {
            binding.ivImg.visibility = View.VISIBLE
        } else {
            binding.ivImg.visibility = View.GONE
        }
        binding.root.setOnClickListener {
            if (item.isFile) {
                if (!multiSelectMode) {
                    onItemSelect?.invoke(item, position)
                    return@setOnClickListener
                }
                item.selected = !item.selected
                updateItem(item, position)
                //notifyItemChanged(position)
            }
            onItemClick?.invoke(item, position)
        }
        binding.buttonMore.setOnClickListener {
            onFolderMenu?.invoke(it, item)
        }
    }

    fun onItemSelect(onItemSelect: (item: FileEntity, position: Int) -> Unit) {
        this.onItemSelect = onItemSelect
    }

    fun onFolderMenu(onFolderMenu: (v: View, item: FileEntity) -> Unit) {
        this.onFolderMenu = onFolderMenu
    }

    fun changeSelectMode(multi: Boolean) {
        this.multiSelectMode = multi
    }

    fun getSelect(): MutableList<String> {
        return dataList.filter {
            it.isFile && it.selected
        }.map {
            it.path
        }.toMutableList()
    }

    fun selectAll() {
        dataList.forEach {
            it.selected = true
        }
        notifyAllChanged()
    }

    fun selectInvert() {
        dataList.forEach {
            it.selected = !it.selected
        }
        notifyAllChanged()
    }

    fun unSelect() {
        val tmpPosition = dataList.indexOfFirst { it.isFile && it.selected }
        if (tmpPosition != -1) {
            dataList[tmpPosition].selected = false
            notifyItemChanged(tmpPosition)
        }
    }

}