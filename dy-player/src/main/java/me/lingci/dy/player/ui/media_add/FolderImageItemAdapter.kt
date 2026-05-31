package me.lingci.dy.player.ui.media_add

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import me.lingci.lib.base.storage.entity.FileEntity
import me.lingci.lib.base.util.FileOperator
import me.lingci.dy.player.R
import me.lingci.dy.player.databinding.ItemFolderSelectListBinding
import me.lingci.dy.player.databinding.ItemMediaSearchListBinding
import java.io.File

class FolderImageItemAdapter(
    private val mList: MutableList<FileEntity>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onItemClick: ((item: FileEntity, position: Int) -> Unit)? = null
    private var onItemLongClick: ((item: FileEntity, position: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 1) {
            return ImageViewHolder(
                ItemMediaSearchListBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        } else {
            return ViewHolder(
                ItemFolderSelectListBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        when (holder) {
            is ViewHolder -> {
                holder.bindItem(mList[position], position)
                holder.itemView.setOnClickListener {
                    onItemClick?.invoke(mList[position], position)
                }
                holder.itemView.setOnLongClickListener {
                    onItemLongClick?.invoke(mList[position], position)
                    true
                }
            }

            is ImageViewHolder -> {
                holder.bindItem(mList[position], position)
                holder.itemView.setOnClickListener {
                    onItemClick?.invoke(mList[position], position)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun getItemViewType(position: Int) =
        if (mList[position].isFile) 1 else 2

    fun onItemClick(onItemClick: (item: FileEntity, position: Int) -> Unit) {
        this.onItemClick = onItemClick
    }

    fun onItemLongClick(onItemLongClick: (item: FileEntity, position: Int) -> Unit) {
        this.onItemLongClick = onItemLongClick
    }

    fun getData(): MutableList<FileEntity> {
        return mList;
    }

    fun addItem(item: FileEntity) {
        mList.add(item)
        notifyItemChanged(itemCount - 1)
    }

    fun setData(it: List<FileEntity>) {
        mList.clear()
        mList.addAll(it)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<FileEntity>, parent: File?) {
        mList.clear()
        mList.addAll(list)
        if (parent != null && parent.path != FileOperator.rootFolder.path) {
            mList.add(
                0, FileEntity(
                    title = "返回上级",
                    name = parent.name,
                    path = parent.path,
                    isFile = false,
                    returnParent = true
                )
            )
        }
        notifyDataSetChanged()
    }

    fun getItem(position: Int): FileEntity {
        return mList[position]
    }

    fun findOne(): FileEntity? {
        return mList.firstOrNull()
    }

    class ViewHolder(
        private val mBinding: ItemFolderSelectListBinding
    ) : RecyclerView.ViewHolder(mBinding.root) {

        private var mPosition = 0

        init {
            itemView.tag = this
        }

        fun bindItem(item: FileEntity, position: Int) {
            mPosition = position
            mBinding.tvTitle.text = item.title
            if (item.returnParent) {
                mBinding.ivIcon.setImageResource(me.lingci.lib.base.R.drawable.ic_file_back)
            } else {
                mBinding.ivIcon.setImageResource(me.lingci.lib.base.R.drawable.ic_file_folder)
            }
        }
    }

    class ImageViewHolder(
        private val mBinding: ItemMediaSearchListBinding
    ) : RecyclerView.ViewHolder(mBinding.root) {

        private var mPosition = 0

        init {
            itemView.tag = this
        }

        fun bindItem(item: FileEntity, position: Int) {
            mPosition = position
            mBinding.tvTitle.text = item.title
            Glide.with(mBinding.root.context).load(item.path).into(mBinding.ivThumb)
        }
    }

}
