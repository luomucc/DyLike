package me.lingci.lib.player.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import me.lingci.lib.base.storage.entity.FileEntity
import me.lingci.lib.base.ui.BaseAdapter
import me.lingci.lib.base.util.FileOperator
import me.lingci.lib.player.ui.R
import me.lingci.lib.player.ui.databinding.DmSelectItemBinding
import java.io.File

/**
 * @author : happyc
 * time    : 2023/03/24
 * desc    :
 * version : 1.0
 */
open class FileSelectAdapter(
    private var dataSet: ArrayList<FileEntity>
) : BaseAdapter<FileEntity, DmSelectItemBinding>(dataSet) {

    private var customColorRes: Int? = null

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): DmSelectItemBinding {
        return DmSelectItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun bindData(
        binding: DmSelectItemBinding,
        item: FileEntity,
        position: Int
    ) {
        customColorRes?.let { colorRes ->
            binding.fileTitle.setTextColor(ContextCompat.getColorStateList(binding.root.context, colorRes))
        }
        binding.fileTitle.text = item.title
        binding.fileTitle.isSelected = item.selected
        binding.fileIcon.setImageResource(getImageResource(item))
        binding.arrowIcon.visibility = if (item.isFile) View.GONE else View.VISIBLE
        binding.root.setOnClickListener {
            changeSelect(position)
            onItemClick?.invoke(item, position)
        }
    }

    private fun changeSelect(position: Int) {
        dataSet.forEachIndexed { index, item ->
            if (item.selected && index != position) {
                item.selected = false
                notifyItemChanged(index)
            }
            if (index == position && !item.selected) {
                item.selected = true
                notifyItemChanged(index)
            }
        }
    }

    private fun getImageResource(file: FileEntity): Int {
        return when {
            !file.isFile -> me.lingci.lib.base.R.drawable.ic_file_folder
            file.returnParent -> me.lingci.lib.base.R.drawable.ic_file_back
            else -> me.lingci.lib.base.R.drawable.ic_file_text
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<FileEntity>) {
        dataSet.clear()
        dataSet.addAll(data)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(file: File, data: List<FileEntity>) {
        dataSet.clear()
        dataSet.addAll(data)
        if (file.path != FileOperator.rootFolder.path) {
            dataSet.add(
                0,
                FileEntity(
                    title = "返回上级",
                    name = file.name,
                    path = file.parent!!,
                    isFile = true,
                    returnParent = true
                )
            )
        }
        notifyDataSetChanged()
    }

    public fun setCustomColor(@ColorRes color: Int) {
        this.customColorRes = color
    }

}
