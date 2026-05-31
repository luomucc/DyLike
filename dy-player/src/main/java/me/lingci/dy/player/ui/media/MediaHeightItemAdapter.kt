package me.lingci.dy.player.ui.media

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import me.lingci.dy.player.databinding.ItemMediaHeightListBinding
import me.lingci.dy.player.entity.MediaData
import me.lingci.dy.player.entity.MediaLibType
import me.lingci.dy.player.util.setCover
import me.lingci.lib.base.ui.BaseAdapter

class MediaHeightItemAdapter(
    private val dataSet: MutableList<MediaData>
) : BaseAdapter<MediaData, ItemMediaHeightListBinding>(dataSet) {

    private var onLongItemClick: ((item: MediaData, position: Int) -> Unit)? = null
    private var batchMode = false

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): ItemMediaHeightListBinding {
        return ItemMediaHeightListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun bindData(binding: ItemMediaHeightListBinding, item: MediaData, position: Int) {
        binding.tvTitle.text = item.title
        binding.ivThumb.setCover(item)
        changeSelect(binding, item, position)
        binding.root.setOnClickListener {
            if (position < dataSet.size) {
                if (batchMode && item.type > MediaLibType.DEFAULT) {
                    item.selected = !item.selected
                    changeSelect(binding, item, position)
                    //notifyItemChanged(position)
                }
                if (batchMode.not()) {
                    onItemClick?.invoke(item, position)
                }
            }
        }
        binding.root.setOnLongClickListener {
            if (position < dataSet.size) {
                if (batchMode.not()) {
                    onLongItemClick?.invoke(item, position)
                    return@setOnLongClickListener true
                }
            }
            false
        }
    }

    private fun changeSelect(binding: ItemMediaHeightListBinding, item: MediaData, position: Int) {
        binding.ivSelect.visibility = if (batchMode && item.type > MediaLibType.DEFAULT && item.selected) View.VISIBLE else View.GONE
        val targetScale = if (batchMode && item.type > MediaLibType.DEFAULT && item.selected) 0.98f else 1f
        if (binding.root.scaleX != targetScale) {
            val scaleAnimation = ScaleAnimation(
                binding.root.scaleX,
                targetScale,
                binding.root.scaleY,
                targetScale,
                binding.root.width / 2f,
                binding.root.height / 2f
            )
            scaleAnimation.duration = 200
            scaleAnimation.fillAfter = true
            scaleAnimation.setAnimationListener(object: Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {

                }

                override fun onAnimationEnd(animation: Animation?) {
                    binding.root.scaleX = targetScale
                    binding.root.scaleY = targetScale
                }

                override fun onAnimationRepeat(animation: Animation?) {

                }
            })
            binding.root.startAnimation(scaleAnimation)
            //binding.root.scaleX = targetScale
            //binding.root.scaleY = targetScale
        }
    }

    fun onLongItemClick(onLongItemClick: (item: MediaData, position: Int) -> Unit) {
        this.onLongItemClick = onLongItemClick
    }

    fun getBatchMode(): Boolean {
        return batchMode
    }

    fun batchMode(position: Int) {
        batchMode = true
        dataSet[position].selected = true
        notifyAllChanged()
    }

    fun exitBatchMode() {
        batchMode = false
        dataSet.forEach {
            it.selected = false
        }
        notifyAllChanged()
    }

    fun selectAll() {
        dataSet.forEach {
            it.selected = it.type > MediaLibType.DEFAULT
        }
        notifyAllChanged()
    }

    fun selectInvert() {
        dataSet.forEach {
            if (it.type > MediaLibType.DEFAULT) {
                it.selected = !it.selected
            } else {
                it.selected = false
            }
        }
        notifyAllChanged()
    }

    fun removeSelect() {
        dataSet.removeIf{item -> item.selected}
        batchMode = false
        notifyAllChanged()
    }

}
