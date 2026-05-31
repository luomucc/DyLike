package me.lingci.lib.player.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.lingci.lib.base.entity.TitleItem
import me.lingci.lib.player.ui.databinding.EpisodeTitleItemBinding

/**
 *   author : happyc
 *   e-mail : bafs.jy@live.com
 *   time   : 2025/01/20
 *   desc   : 剧集
 *   version: 1.0
 */
class EpisodeSelectAdapter(
    private var items: MutableList<TitleItem>
) : RecyclerView.Adapter<EpisodeSelectAdapter.ViewHolder>() {

    private var onItemClick: ((item: TitleItem, position: Int) -> Unit)? = null

    fun setOnItemClickListener(onItemClick: (item: TitleItem, position: Int) -> Unit) {
        this.onItemClick = onItemClick
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(EpisodeTitleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.title.text = items[position].title
        holder.binding.title.isSelected = items[position].selected
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(items[position], position)
            selected(position)
        }
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<TitleItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun addData(item: TitleItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun selected(position: Int) {
        if (items.isEmpty()) {
            return
        }
        if (position < 0 || position >= items.size) {
            return
        }
        val tempPosition = items.indexOfFirst { it.selected }
        if (tempPosition != -1) {
            items[tempPosition].selected = false
            notifyItemChanged(tempPosition)
        }
        items[position].selected = true
        notifyItemChanged(position)
    }

    fun cleanSelect() {
        items.forEach { item ->
            item.selected = false
        }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun reverse() {
        items.reverse()
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        val binding: EpisodeTitleItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

    }
}