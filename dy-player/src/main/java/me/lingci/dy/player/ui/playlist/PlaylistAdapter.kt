package me.lingci.dy.player.ui.playlist

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import me.lingci.dy.player.databinding.ItemPlaylistBinding
import me.lingci.dy.player.entity.MediaData
import me.lingci.lib.base.ui.BaseAdapter

class PlaylistAdapter(
    dataSet: MutableList<MediaData>
) : BaseAdapter<MediaData, ItemPlaylistBinding>(dataSet) {

    private var onItemLongClick: ((item: MediaData, position: Int) -> Unit)? = null

    fun onItemLongClick(listener: (item: MediaData, position: Int) -> Unit) {
        this.onItemLongClick = listener
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): ItemPlaylistBinding {
        return ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    @SuppressLint("SetTextI18n")
    override fun bindData(binding: ItemPlaylistBinding, item: MediaData, position: Int) {
        binding.tvTitle.text = item.title
        binding.tvCount.text = "包含 ${item.items.size} 条媒体"
        binding.root.setOnClickListener {
            if (position < dataList.size) {
                onItemClick?.invoke(item, position)
            }
        }
        binding.root.setOnLongClickListener {
            if (position < dataList.size) {
                onItemLongClick?.invoke(item, position)
            }
            true
        }
    }

}
