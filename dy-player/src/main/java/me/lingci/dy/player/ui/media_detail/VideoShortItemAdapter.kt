package me.lingci.dy.player.ui.media_detail

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import android.view.View
import me.lingci.dy.player.R
import me.lingci.dy.player.databinding.ItemVideoShortListBinding
import me.lingci.dy.player.entity.VideoData
import me.lingci.dy.player.util.AppUtil
import me.lingci.lib.base.storage.entity.StorageType
import me.lingci.lib.base.ui.BaseAdapter
import me.lingci.lib.base.util.CodeUtil
import java.io.File

class VideoShortItemAdapter(
    private val dataSet: MutableList<VideoData>
) : BaseAdapter<VideoData, ItemVideoShortListBinding>(dataSet) {

    private var onLongItemClick: ((v: View, item: VideoData, position: Int) -> Unit)? = null
    private var batchMode = false

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): ItemVideoShortListBinding {
        return ItemVideoShortListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun bindData(
        binding: ItemVideoShortListBinding,
        item: VideoData,
        position: Int
    ) {
        File(
            binding.ivThumb.context.externalCacheDir,
            ".thumb/${CodeUtil.md5(item.videoUrl)}.${AppUtil.THUMB_TYPE}"
        ).let {
            if (it.exists()) {
                val params: ConstraintLayout.LayoutParams = binding.ivThumb.layoutParams as ConstraintLayout.LayoutParams
                try {
                    val wh = getWh(it.path)
                    val width = wh.first
                    val height = wh.second
                    params.dimensionRatio = if (width > 0 && height > 0 && height > width) "${width}:${height}" else "1:1"
                } catch (e: Exception) {
                    params.dimensionRatio = "3:4"
                }
                binding.ivThumb.layoutParams = params
                Glide.with(binding.ivThumb.context).load(it).into(binding.ivThumb)
            } else {
                loadDefault(binding)
            }
        }
        binding.tvTitle.text = item.name
        binding.viewBorder.visibility = if (item.lastPlay && !batchMode) View.VISIBLE else View.GONE
        changeSelect(binding, item)
        binding.root.setOnClickListener {
            if (position < dataSet.size) {
                if (batchMode) {
                    item.selected = !item.selected
                    changeSelect(binding, item)
                } else {
                    onItemClick?.invoke(item, position)
                }
            }
        }
        binding.root.setOnLongClickListener {
            if (position < dataSet.size && !batchMode) {
                onLongItemClick?.invoke(it, item, position)
                return@setOnLongClickListener true
            }
            false
        }
    }

    private fun changeSelect(binding: ItemVideoShortListBinding, item: VideoData) {
        binding.ivSelect.visibility = if (batchMode && item.selected) View.VISIBLE else View.GONE
        val targetAlpha = if (batchMode && item.selected) 0.6f else 1f
        binding.ivThumb.alpha = targetAlpha
    }

    fun loadDefault(binding: ItemVideoShortListBinding) {
        binding.ivThumb.setImageResource(R.drawable.ic_video_default)
        val params: ConstraintLayout.LayoutParams = binding.ivThumb.layoutParams as ConstraintLayout.LayoutParams
        params.dimensionRatio = "3:4"
        binding.ivThumb.layoutParams = params
    }

    fun getWh(path: String): Pair<Int, Int> {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        val width = options.outWidth
        val height = options.outHeight
        return Pair(width, height)
    }

    fun onLongItemClick(onLongItemClick: (v: View, item: VideoData, position: Int) -> Unit) {
        this.onLongItemClick = onLongItemClick
    }

    fun getBatchMode(): Boolean = batchMode

    fun batchMode(position: Int) {
        batchMode = true
        if (position in dataSet.indices) {
            dataSet[position].selected = true
        }
        notifyAllChanged()
    }

    fun exitBatchMode() {
        batchMode = false
        dataSet.forEach { it.selected = false }
        notifyAllChanged()
    }

    fun selectAll() {
        dataSet.forEach { it.selected = true }
        notifyAllChanged()
    }

    fun selectInvert() {
        dataSet.forEach { it.selected = !it.selected }
        notifyAllChanged()
    }

    fun listSelect(): List<VideoData> {
        return dataList.filter { it.selected }
    }

    fun removeSelect() {
        dataSet.removeIf { it.selected }
        batchMode = false
        notifyAllChanged()
    }

    fun sorted() {
        dataSet.reverse()
        notifyAllChanged()
    }

    fun sortByName(ascending: Boolean) {
        dataSet.sortBy { it.name.lowercase() }
        if (!ascending) dataSet.reverse()
        notifyAllChanged()
    }

    fun sortByModifiedTime(ascending: Boolean) {
        dataSet.sortBy { videoData ->
            if (videoData.type == StorageType.LOCAL_STORAGE) {
                File(videoData.videoUrl).lastModified()
            } else {
                0L
            }
        }
        if (!ascending) dataSet.reverse()
        notifyAllChanged()
    }

    fun getLastPlayPosition(): Int {
        return dataSet.indexOfFirst { it.lastPlay }
    }

    fun updateLastPlay(playLast: String) {
        dataSet.forEach { it.lastPlay = it.videoUrl == playLast }
        notifyAllChanged()
    }

}
