package me.lingci.lib.base.dailog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import me.lingci.lib.base.R
import me.lingci.lib.base.adapter.ActionItemAdapter
import me.lingci.lib.base.databinding.ActionBottomDialogBinding

/**
 * @author : happyc
 * time    : 2022/03/19
 * desc    : 功能项通用弹窗
 * version : 1.0
 */
open class ActionBottomDialog : BottomSheetDialogFragment() {

    companion object {
        const val TITLE_KEY = "title"
        const val DATA_KEY = "dataSet"
        const val KEY_SELECTED = "selected"

        fun buildBundle(title:String, dataSet: List<String>, selected: String = ""): Bundle {
            return Bundle().apply {
                putString(TITLE_KEY, title)
                putStringArrayList(DATA_KEY, ArrayList(dataSet))
                putString(KEY_SELECTED, selected)
            }
        }

    }

    private lateinit var binding: ActionBottomDialogBinding
    private lateinit var adapter: ActionItemAdapter
    private var onSelect: ((index: Int) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_Material3_DayNight_BottomSheetDialog)
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ActionBottomDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    override fun onStart() {
        super.onStart()
        getData()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (isAdded && !isHidden) {
            return
        }
        super.show(manager, tag)
    }

    private fun getData() {
        arguments?.let { bundle ->
            bundle.getString(TITLE_KEY)?.let { title ->
                if (::binding.isInitialized) {
                    binding.title.text = title
                }
            }
            bundle.getStringArrayList(DATA_KEY)?.let { list ->
                if (::binding.isInitialized) {
                    binding.recyclerView.post {
                        adapter.updateData(list)
                    }
                    bundle.getString(KEY_SELECTED)?.let { selected ->
                        if (selected.isNotEmpty()) {
                            list.indexOfFirst { it == selected }.let { index ->
                                adapter.select(index)
                            }
                        }
                    }
                }
            }
        }
    }

    fun setOnSelectListener(onSelect: (index: Int) -> Unit) {
        this.onSelect = onSelect
    }

    private fun init() {
        binding.actionClose.setOnClickListener { dismiss() }
        binding.actionNegative.setOnClickListener { dismiss() }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.setHasFixedSize(true)

        adapter = ActionItemAdapter(mutableListOf())
        binding.recyclerView.adapter = adapter
        adapter.onItemClick { _, position ->
            this.onSelect?.invoke(position)
            dismiss()
        }
    }

}
