package me.lingci.dy.player.ui.source

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import me.lingci.dy.player.R
import me.lingci.dy.player.databinding.DialogStreamLinkBinding
import me.lingci.lib.base.util.Log
import me.lingci.lib.base.util.ToastUtil

/**
 * author : happyc
 * e-mail : bafs.jy@live.com
 * time   : 2025/04/02
 * desc   : 串流Dialog
 * version: 1.0
 */
class StreamLinkDialog : DialogFragment() {

    private lateinit var binding: DialogStreamLinkBinding
    private var onValue: ((value: String, header: String) -> Unit)? = null

    fun onValueListener(onValue: (value: String, header: String) -> Unit) {
        this.onValue = onValue
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogStreamLinkBinding.inflate(inflater, container, false)
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
        Log.d(this@StreamLinkDialog, "show")
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
        Log.d(this@StreamLinkDialog, "onStart")
    }

    private fun init() {
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
        binding.buttonConfirmed.setOnClickListener {
            val value = binding.inputValue.editText?.text.toString().trim()
            if (value.isBlank()) {
                ToastUtil.showToast(requireContext(), me.lingci.lib.base.R.string.hint_empty_text)
                return@setOnClickListener
            }
            val header = binding.inputHeader.editText?.text.toString().trim()
            onValue?.invoke(value, header)
            dismiss()
        }
    }

}
