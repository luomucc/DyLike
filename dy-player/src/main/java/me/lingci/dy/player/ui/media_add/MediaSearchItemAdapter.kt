package me.lingci.dy.player.ui.media_add

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.lingci.lib.base.util.CodeUtil
import me.lingci.dy.player.databinding.ItemMediaSearchListBinding
import me.lingci.dy.player.entity.MediaData
import me.lingci.dy.player.util.AppUtil
import me.lingci.dy.player.util.loadImage
import java.io.File

class MediaSearchItemAdapter(
    private val mVideoBeans: MutableList<MediaData>
) : RecyclerView.Adapter<MediaSearchItemAdapter.ViewHolder>() {

    private lateinit var binding: ItemMediaSearchListBinding
    private var changePosition: Pair<Int, Int> = Pair(-1, -1)
    private var onItemClick: ((item: MediaData, position: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = ItemMediaSearchListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        holder.bindItem(mVideoBeans[position], position)
        holder.itemView.setOnClickListener {
            changePosition = Pair(position, changePosition.first)
            onItemClick?.let { it1 -> it1(mVideoBeans[position], position) }
        }
    }

    override fun getItemCount(): Int {
        return mVideoBeans.size
    }

    fun onItemClick(onItemClick: (item: MediaData, position: Int) -> Unit) {
        this.onItemClick = onItemClick
    }

    fun getData(): MutableList<MediaData> {
        return mVideoBeans
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: ArrayList<MediaData>) {
        mVideoBeans.clear()
        mVideoBeans.addAll(list)
        notifyDataSetChanged()
    }

    class ViewHolder(
        private val mBinding: ItemMediaSearchListBinding
    ) : RecyclerView.ViewHolder(mBinding.root) {

        private var mPosition = 0

        init {
            itemView.tag = this
        }

        fun bindItem(item: MediaData, position: Int) {
            mPosition = position
            File(
                itemView.context.externalCacheDir,
                ".thumb/${CodeUtil.md5(item.showFile)}.${AppUtil.THUMB_TYPE}"
            ).let {
                if (it.exists()) {
                    mBinding.ivThumb.loadImage(it.path)
                } else {
                    mBinding.ivThumb.loadImage(item.showFile)
                }
            }
            mBinding.tvTitle.text = item.title
        }
    }

}
