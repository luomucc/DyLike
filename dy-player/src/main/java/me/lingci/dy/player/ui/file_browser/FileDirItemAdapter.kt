package me.lingci.dy.player.ui.file_browser

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.lingci.dy.player.databinding.ItemFileDirListBinding
import me.lingci.lib.base.storage.entity.FileEntity

class FileDirItemAdapter(
    private val mList: MutableList<FileEntity>
) : RecyclerView.Adapter<FileDirItemAdapter.ViewHolder?>() {

    private lateinit var binding: ItemFileDirListBinding
    private var onItemClick: ((item: FileEntity, position: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = ItemFileDirListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        holder.bindItem(mList[position], position, mList.size)
        holder.itemView.setOnClickListener {
            onItemClick?.let { it1 -> it1(mList[position], position) }
        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    fun onItemClick(onItemClick: (item: FileEntity, position: Int) -> Unit) {
        this.onItemClick = onItemClick
    }

    fun getData(): MutableList<FileEntity> {
        return mList;
    }

    fun getItem(position: Int): FileEntity {
        if (position < 0) {
            return mList.first()
        }
        if (position > itemCount - 1) {
            return mList.last()
        }
        return mList[position]
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addItem(item: FileEntity) {
        mList.add(item)
        notifyDataSetChanged()
        //notifyItemChanged(itemCount - 1)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(it: List<FileEntity>) {
        mList.clear()
        mList.addAll(it)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeEnd(position: Int) {
        mList.removeAll(mList.subList(position + 1, mList.size))
        notifyDataSetChanged()
    }

    class ViewHolder(
        private val mBinding: ItemFileDirListBinding
    ) : RecyclerView.ViewHolder(mBinding.root) {

        private var mPosition = 0

        init {
            itemView.tag = this
        }

        fun bindItem(item: FileEntity, position: Int, itemCount: Int) {
            mPosition = position
            mBinding.tvTitle.text = item.name
            mBinding.tvTitle.isSelected = position == itemCount - 1
        }
    }
}