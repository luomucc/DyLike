package me.lingci.dy.player.ui.file_browser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import me.lingci.dy.player.R
import me.lingci.dy.player.databinding.ItemFileBrowserListBinding
import me.lingci.dy.player.util.AppUtil
import me.lingci.lib.base.storage.entity.FileEntity
import me.lingci.lib.base.ui.BaseAdapter
import me.lingci.lib.base.util.isImage
import java.io.File

class FileBrowserItemAdapter(
    dataSet: MutableList<FileEntity>
) : BaseAdapter<FileEntity, ItemFileBrowserListBinding>(dataSet) {

    private var onItemLongClick: ((item: FileEntity, position: Int) -> Unit)? = null
    private var onItemMoreClick: ((view: View, item: FileEntity, position: Int) -> Unit)? = null

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): ItemFileBrowserListBinding {
        return ItemFileBrowserListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun bindData(binding: ItemFileBrowserListBinding, item: FileEntity, position: Int) {
        binding.tvTitle.text = item.name
        if (item.isFile) {
            binding.tvType.text = "视频"
            binding.ivIcon.visibility = View.GONE
            binding.ivImg.visibility = View.VISIBLE
            binding.buttonMore.visibility = View.GONE
            File(binding.root.context.externalCacheDir, ".thumb/${item.thumbName()}.${AppUtil.THUMB_TYPE}").let {
                if (it.isImage()) {
                    Glide.with(binding.ivImg.context).load(it).into(binding.ivImg)
                } else {
                    binding.ivImg.setImageResource(R.drawable.ic_video_default)
                }
            }
        } else {
            binding.tvType.text = "目录"
            binding.ivImg.visibility = View.GONE
            binding.ivIcon.visibility = View.VISIBLE
            binding.ivImg.setImageResource(me.lingci.lib.base.R.drawable.ic_file_folder)
            binding.buttonMore.visibility = View.VISIBLE
        }
        binding.root.setOnClickListener {
            onItemClick?.invoke(item, position)
        }
        binding.root.setOnLongClickListener {
            onItemLongClick?.invoke(item, position)
            true
        }
        binding.buttonMore.setOnClickListener {
            onItemMoreClick?.invoke(it, item, position)
        }
    }

    fun onItemLongClick(onItemLongClick: (item: FileEntity, position: Int) -> Unit) {
        this.onItemLongClick = onItemLongClick
    }

    fun onItemMoreClick(onItemMoreClick: (view: View, item: FileEntity, position: Int) -> Unit) {
        this.onItemMoreClick = onItemMoreClick
    }

}
