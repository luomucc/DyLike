package me.lingci.dy.player.ui.tool

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import me.lingci.dy.player.databinding.DialogDmFilterBinding
import me.lingci.dy.player.util.SpUtil
import me.lingci.lib.base.dailog.ValueSetDialog
import me.lingci.lib.dm.view.common.DmInitializer

class DmFilterDialog : DialogFragment() {

    private var _binding: DialogDmFilterBinding? = null
    private val binding get() = _binding!!
    private val spUtil: SpUtil by lazy { SpUtil(requireContext()) }

    private lateinit var chipAdapter: DmFilterChipAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDmFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.MaterialAlertDialog_Material3)
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        binding.toolbar.setNavigationOnClickListener { dismiss() }
        binding.toolbar.title = "屏蔽词管理"

        val flexboxLayoutManager = FlexboxLayoutManager(context)
        flexboxLayoutManager.flexDirection = FlexDirection.ROW
        flexboxLayoutManager.flexWrap = FlexWrap.WRAP
        flexboxLayoutManager.justifyContent = JustifyContent.FLEX_START
        binding.recyclerView.layoutManager = flexboxLayoutManager

        chipAdapter = DmFilterChipAdapter(arrayListOf()) { keyword ->
            chipAdapter.removeItem(chipAdapter.getData().indexOf(keyword))
            spUtil.dmFilter = chipAdapter.getData().joinToString(DmInitializer.FILTER_SEPARATOR)
        }
        binding.recyclerView.adapter = chipAdapter

        spUtil.dmFilter?.split(DmInitializer.FILTER_SEPARATOR)?.let {
            chipAdapter.updateData(it.filter { word -> word.isNotBlank() })
        }

        val valueSetDialog = ValueSetDialog()
        valueSetDialog.onValueListener { _, value ->
            chipAdapter.addItem(value)
            spUtil.dmFilter = chipAdapter.getData().joinToString(DmInitializer.FILTER_SEPARATOR)
        }
        binding.buttonAdd.setOnClickListener {
            valueSetDialog.arguments = ValueSetDialog.buildBundle("添加屏蔽词", "", "输入屏蔽词", 1)
            valueSetDialog.show(childFragmentManager, "dm_filter_add")
        }
    }

    override fun show(manager: androidx.fragment.app.FragmentManager, tag: String?) {
        if (isAdded && isVisible) {
            return
        }
        super.show(manager, tag)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
