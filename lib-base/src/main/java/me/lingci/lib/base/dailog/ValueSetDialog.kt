package me.lingci.lib.base.dailog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import me.lingci.lib.base.R
import me.lingci.lib.base.databinding.ValueSetDialogBinding
import me.lingci.lib.base.util.Log
import me.lingci.lib.base.util.ToastUtil

/**
 * author : happyc
 * e-mail : bafs.jy@live.com
 * time   : 2023/07/05
 * desc   : 文本设置添加Dialog
 * version: 1.0
 */
class ValueSetDialog : DialogFragment() {

    companion object {

        private const val KEY_TITLE = "title"
        private const val KEY_VALUE = "value"
        private const val KEY_HINT = "hint"
        private const val KEY_CODE = "code"

        fun buildBundle(
            context: Context,
            @StringRes title: Int,
            @StringRes value: Int,
            @StringRes hint: Int,
            code: Int
        ): Bundle {
            return buildBundle(
                context.getString(title), context.getString(value), context.getString(hint), code
            )
        }

        fun buildBundle(title: String, value: String, hint: String, code: Int): Bundle {
            return Bundle().apply {
                putString(KEY_TITLE, title)
                putString(KEY_VALUE, value)
                putString(KEY_HINT, hint)
                putInt(KEY_CODE, code)
            }
        }
    }

    private lateinit var binding: ValueSetDialogBinding
    private var hintTitle: String? = null
    private var inputValue: String? = null
    private var inputHint: String? = null
    private var actionCode: Int? = null
    private var onValue: ((code: Int, value: String) -> Unit)? = null

    fun onValueListener(onValue: (code: Int, value: String) -> Unit) {
        this.onValue = onValue
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = ValueSetDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        init()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (isAdded && !isHidden) {
            return
        }
        Log.d(this@ValueSetDialog,"show")
        super.show(manager, tag)
    }

    override fun show(transaction: FragmentTransaction, tag: String?): Int {
        if (isAdded && !isHidden) {
            return -1
        }
        return super.show(transaction, tag)
    }

    override fun onStart() {
        super.onStart()
        Log.d(this@ValueSetDialog, "onStart")
        getData()
        hintTitle?.let {
            binding.toolbar.title = it
        }
        inputValue?.let {
            binding.inputValue.editText?.setText(it)
            binding.inputValue.editText?.setSelection(it.length)
        }
        inputHint?.let {
            binding.inputValue.helperText = it
        }
    }

    private fun getData() {
        arguments?.let {
            if (::binding.isInitialized) {
                hintTitle = it.getString(KEY_TITLE)
                inputValue = it.getString(KEY_VALUE)
                inputHint = it.getString(KEY_HINT)
                actionCode = it.getInt(KEY_CODE)
            }
        }
    }

    private fun init() {
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
        binding.buttonConfirmed.setOnClickListener {
            val value = binding.inputValue.editText?.text.toString().trim()
            if (value.isBlank()) {
                ToastUtil.showToast(requireContext(), R.string.hint_empty_text)
                return@setOnClickListener
            }
            onValue?.invoke(actionCode!!, value)
            dismiss()
        }
    }

}