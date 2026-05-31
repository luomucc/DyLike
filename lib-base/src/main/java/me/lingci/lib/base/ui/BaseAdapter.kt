package me.lingci.lib.base.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * 定义抽象适配器类，使用泛型 T 表示数据类型，VB 表示 ViewBinding 类型
 */
abstract class BaseAdapter<T, VB : ViewBinding>(
    protected var dataList: MutableList<T> = mutableListOf()
) : RecyclerView.Adapter<BaseAdapter.BaseViewHolder<VB>>() {

    protected var onItemClick: ((item: T, position: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VB> {
        return BaseViewHolder(createBinding(LayoutInflater.from(parent.context), parent))
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VB>, position: Int) {
        // 获取最新的适配器位置，避免position过期
        val currentPos = holder.bindingAdapterPosition
        // 边界检查，防止异步数据更新导致崩溃
        if (currentPos < 0 || currentPos >= dataList.size) return
        bindData(holder.binding, dataList[currentPos], currentPos)
    }

    override fun getItemCount(): Int = dataList.size

    fun onItemClick(listener: (item: T, position: Int) -> Unit) {
        this.onItemClick = listener
    }

    protected abstract fun createBinding(inflater: LayoutInflater, parent: ViewGroup): VB
    protected abstract fun bindData(binding: VB, item: T, position: Int)

    // --- 数据操作方法优化 ---
    fun dataEmpty(): Boolean = dataList.isEmpty()

    fun getItem(position: Int): T? {
        // 修复：越界时返回 null 或处理，避免直接抛出异常
        return if (position in 0 until dataList.size) dataList[position] else null
    }

    fun addItem(item: T) {
        val position = dataList.size
        dataList.add(item)
        notifyItemInserted(position)
    }

    fun addItem(item: T, position: Int) {
        // 允许添加到末尾
        if (position in 0..dataList.size) {
            dataList.add(position, item)
            notifyItemInserted(position)
        } else {
            addItem(item)
        }
    }

    fun addItems(items: List<T>) {
        if (items.isEmpty()) return
        val startPosition = dataList.size
        dataList.addAll(items)
        notifyItemRangeInserted(startPosition, items.size)
    }

    fun updateItem(item: T, position: Int) {
        if (position in 0 until dataList.size) {
            dataList[position] = item
            notifyItemChanged(position)
        }
    }

    fun updateData(newData: List<T>) {
        dataList.clear()
        dataList.addAll(newData)
        notifyAllChanged()
    }

    fun removeItem(position: Int) {
        if (position in 0 until dataList.size) {
            dataList.removeAt(position)
            notifyItemRemoved(position)
            // 优化：移除后刷新后续位置，避免索引错乱
            notifyItemRangeChanged(position, dataList.size - position)
        }
    }

    fun removeEnd(position: Int) {
        if (position >= 0 && position < dataList.size - 1) {
            val size = dataList.size
            // subList 操作是原列表的视图，clear 会影响原列表
            dataList.subList(position + 1, size).clear()
            notifyItemRangeRemoved(position + 1, size - (position + 1))
        }
    }

    fun clearData(isRange: Boolean = false) {
        if (isRange) {
            val size = dataList.size
            dataList.clear()
            notifyItemRangeRemoved(0, size)
        } else {
            dataList.clear()
            notifyAllChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun notifyAllChanged() {
        notifyDataSetChanged()
    }

    fun getData(): List<T> = dataList.toList()

    class BaseViewHolder<VB : ViewBinding>(
        val binding: VB
    ) : RecyclerView.ViewHolder(binding.root)

}