package me.lingci.dy.player.ui.source

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import me.lingci.dy.player.R
import me.lingci.dy.player.databinding.DialogItemActionBinding

/**
 * @author : happyc
 * time    : 2024/09/23
 * desc    : 功能选项
 * version : 1.0
 */
open class ItemActionDialog(
    private val dataSet: MutableList<ItemAction>,
    private val onSelect: (type: Int) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: DialogItemActionBinding
    private lateinit var actionAdapter: ItemActionAdapter

    constructor() : this(dataSet = mutableListOf<ItemAction>(), onSelect = {})

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
        binding = DialogItemActionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        actionAdapter = ItemActionAdapter(dataSet)
        binding.recyclerView.adapter = actionAdapter
        actionAdapter.onItemClick { item, _ ->
            onSelect.invoke(item.id)
            dismiss()
        }
    }

}
