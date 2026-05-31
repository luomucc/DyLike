package me.lingci.lib.base.dailog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.lingci.lib.base.R
import me.lingci.lib.base.databinding.ItemChipFilterBinding
import me.lingci.lib.base.databinding.LayoutChipDialogBinding

/**
 *   @author : happyc
 *   time    : 2025/03/20
 *   desc    : 选项
 *   version : 1.0
 */
class ChipSelectDialog(
    private var title: String,
    private var dataSet: MutableList<String>
) : DialogFragment() {

    private var _binding: LayoutChipDialogBinding? = null
    private val binding get() = _binding!!
    private var onSelect: ((list: List<String>) -> Unit)? = null

    fun onSelect(onSelect: (list: List<String>) -> Unit) {
        this.onSelect = onSelect
    }

    @SuppressLint("PrivateResource")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.MaterialAlertDialog_Material3)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutChipDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    fun init() {
        binding.toolbar.title = title
        binding.toolbar.setNavigationOnClickListener { dismiss() }
        binding.toolbar.setOnMenuItemClickListener { menu ->
            when (menu.itemId) {
                R.id.menu_select_all -> {
                    for (id in 0 until dataSet.size) {
                        binding.chipGroup.check(id)
                    }
                }
                R.id.menu_select_invert -> {
                    val ids = binding.chipGroup.checkedChipIds
                    binding.chipGroup.clearCheck()
                    for (id in 0 until dataSet.size) {
                        if (!ids.contains(id)) {
                            binding.chipGroup.check(id)
                        }
                    }
                }
                R.id.menu_positive -> {
                    dataSet.filterIndexed { index, _ ->
                        binding.chipGroup.checkedChipIds.contains(
                            index
                        )
                    }
                        .let {
                            if (it.isNotEmpty()) {
                                onSelect?.invoke(it)
                            }
                        }
                    dismiss()
                }
                android.R.id.home -> {
                    dismiss()
                }
                else -> {}
            }
            true
        }
        dataSet.forEachIndexed { index, value ->
            lifecycleScope.launch(Dispatchers.IO) {
                val chip = ItemChipFilterBinding.inflate(
                    layoutInflater, binding.chipGroup, false
                ).root
                chip.id = index
                chip.text = value
                chip.tag = index
                withContext(Dispatchers.Main) {
                    binding.chipGroup.addView(chip)
                    binding.chipGroup.post {
                    }
                }
            }
        }
    }

    fun show(manager: FragmentManager) {
        show(manager, tag)
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (isAdded && isVisible) {
            return
        }
        super.show(manager, tag)
    }

}
