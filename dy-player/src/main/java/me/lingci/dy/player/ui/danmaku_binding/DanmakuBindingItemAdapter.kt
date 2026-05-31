package me.lingci.dy.player.ui.danmaku_binding

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.graphics.Typeface
import me.lingci.dy.player.databinding.ItemDanmakuBindingBinding
import me.lingci.lib.base.R as BaseR
import me.lingci.lib.base.ui.BaseAdapter

class DanmakuBindingItemAdapter(
    dataSet: MutableList<DanmakuBindingRowItem>
) : BaseAdapter<DanmakuBindingRowItem, ItemDanmakuBindingBinding>(dataSet) {

    private var onActionClick: ((item: DanmakuBindingRowItem, position: Int) -> Unit)? = null
    private var onItemLongClick: ((item: DanmakuBindingRowItem, position: Int) -> Unit)? = null

    fun onActionClick(listener: (item: DanmakuBindingRowItem, position: Int) -> Unit) {
        onActionClick = listener
    }

    fun onItemLongClick(listener: (item: DanmakuBindingRowItem, position: Int) -> Unit) {
        onItemLongClick = listener
    }

    fun selectedPosition(): Int {
        return dataList.indexOfFirst { it.selected }
    }

    fun select(position: Int) {
        dataList.forEachIndexed { index, item ->
            item.selected = index == position
        }
        notifyAllChanged()
    }

    fun clearSelection() {
        dataList.forEach { it.selected = false }
        notifyAllChanged()
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): ItemDanmakuBindingBinding {
        return ItemDanmakuBindingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun bindData(binding: ItemDanmakuBindingBinding, item: DanmakuBindingRowItem, position: Int) {
        val context = binding.root.context
        val backgroundColor = ContextCompat.getColor(
            context,
            if (item.selected) BaseR.color.background_selected else android.R.color.transparent
        )
        val titleColor = ContextCompat.getColor(
            context,
            if (item.selected) BaseR.color.purple_300 else BaseR.color.text_primary
        )
        val summaryColor = ContextCompat.getColor(
            context,
            if (item.selected) BaseR.color.text_secondary else BaseR.color.text_third
        )
        val iconColor = ContextCompat.getColor(
            context,
            if (item.selected) BaseR.color.purple_300 else BaseR.color.text_third
        )

        binding.root.setBackgroundColor(backgroundColor)
        binding.itemTitle.text = item.title
        binding.itemTitle.setTextColor(titleColor)
        binding.itemTitle.setTypeface(null, if (item.selected) Typeface.BOLD else Typeface.NORMAL)
        binding.itemSummary.text = item.summary
        binding.itemSummary.setTextColor(summaryColor)
        binding.itemSummary.visibility = if (item.summary.isBlank()) View.GONE else View.VISIBLE
        binding.itemMove.setImageResource(item.actionIconRes)
        binding.itemMove.imageTintList = ColorStateList.valueOf(iconColor)
        binding.itemMove.alpha = if (item.actionEnabled) 1f else 0.35f
        binding.root.alpha = if (item.dimmed) 0.55f else 1f
        binding.root.setOnClickListener {
            onItemClick?.invoke(item, position)
        }
        binding.root.setOnLongClickListener {
            onItemLongClick?.invoke(item, position)
            true
        }
        binding.itemMove.setOnClickListener {
            if (item.actionEnabled) {
                onActionClick?.invoke(item, position)
            }
        }
    }

}

data class DanmakuBindingRowItem(
    val sourceIndex: Int,
    val title: String,
    val summary: String = "",
    val actionIconRes: Int,
    val actionEnabled: Boolean = true,
    val dimmed: Boolean = false,
    var selected: Boolean = false
)
