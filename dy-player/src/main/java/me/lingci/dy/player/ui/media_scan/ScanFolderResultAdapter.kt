package me.lingci.dy.player.ui.media_scan

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.lingci.dy.player.databinding.ItemMediaScanFolderBinding
import me.lingci.lib.base.R
import me.lingci.lib.base.ui.BaseAdapter

class ScanFolderResultAdapter(
    dataSet: MutableList<ScanFolderResult>
) : BaseAdapter<ScanFolderResult, ItemMediaScanFolderBinding>(dataSet) {

    private var onSelectionChanged: ((Int) -> Unit)? = null

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): ItemMediaScanFolderBinding {
        return ItemMediaScanFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    @SuppressLint("SetTextI18n")
    override fun bindData(binding: ItemMediaScanFolderBinding, item: ScanFolderResult, position: Int) {
        binding.tvTitle.text = item.name
        binding.tvPath.text = item.path
        binding.tvCount.text = "视频数 ${item.videoCount}"
        binding.tvStatus.text = if (item.existsInLibrary) "已添加" else "新增"
        binding.tvStatus.setTextColor(binding.tvStatus.resources.getColor(if (item.existsInLibrary) me.lingci.lib.base.R.color.red_600 else me.lingci.lib.base.R.color.green_500, null))
        binding.checkboxSelect.visibility = if (item.existsInLibrary) View.GONE else View.VISIBLE
        binding.checkboxSelect.setOnCheckedChangeListener(null)
        binding.checkboxSelect.isChecked = item.selected
        binding.checkboxSelect.setOnCheckedChangeListener { _, checked ->
            if (!item.existsInLibrary && position in 0 until dataList.size) {
                dataList[position].selected = checked
                onSelectionChanged?.invoke(selectedNewFolders().size)
            }
        }
        binding.root.setOnClickListener {
            if (position in 0 until dataList.size && item.existsInLibrary.not()) {
                dataList[position].selected = !dataList[position].selected
                notifyItemChanged(position)
                onSelectionChanged?.invoke(selectedNewFolders().size)
            }
        }
    }

    fun selectAllNew() {
        dataList.forEach {
            if (!it.existsInLibrary) {
                it.selected = true
            }
        }
        notifyAllChanged()
        onSelectionChanged?.invoke(selectedNewFolders().size)
    }

    fun selectInvertNew() {
        dataList.forEach {
            if (!it.existsInLibrary) {
                it.selected = !it.selected
            }
        }
        notifyAllChanged()
        onSelectionChanged?.invoke(selectedNewFolders().size)
    }

    fun selectedNewFolders(): List<ScanFolderResult> {
        return dataList.filter { !it.existsInLibrary && it.selected }
    }

    fun onSelectionChanged(listener: (Int) -> Unit) {
        onSelectionChanged = listener
    }

}
