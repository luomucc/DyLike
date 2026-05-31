package me.lingci.dy.player.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import me.lingci.dy.player.R
import me.lingci.dy.player.databinding.ItemShortVideoListBinding
import me.lingci.dy.player.entity.VideoData
import me.lingci.dy.player.util.AppUtil
import me.lingci.lib.base.ui.BaseAdapter
import me.lingci.lib.base.util.CodeUtil
import java.io.File

class HistoryItemAdapter(
    private val dataSet: MutableList<VideoData>
) : BaseAdapter<VideoData, ItemShortVideoListBinding>(dataSet) {

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): ItemShortVideoListBinding {
        return ItemShortVideoListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun bindData(
        binding: ItemShortVideoListBinding,
        item: VideoData,
        position: Int
    ) {
        File(
            binding.ivThumb.context.externalCacheDir,
            ".thumb/${CodeUtil.md5(item.videoUrl)}.${AppUtil.THUMB_TYPE}"
        ).let {
            if (it.exists()) {
                Glide.with(binding.ivThumb.context).load(it).into(binding.ivThumb)
            } else {
                binding.ivThumb.setImageResource(R.drawable.ic_video_default)
            }
        }
        binding.tvTitle.text = item.name
        binding.root.setOnClickListener {
            onItemClick?.invoke(dataSet[position], position)
        }
    }

}