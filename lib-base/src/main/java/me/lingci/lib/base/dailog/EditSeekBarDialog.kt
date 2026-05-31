package me.lingci.lib.base.dailog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import me.lingci.lib.base.R
import me.lingci.lib.base.databinding.DialogEditSeekBarBinding
import me.lingci.lib.base.util.ToastUtil

/**
 * author : happyc
 * e-mail : bafs.jy@live.com
 * time   : 2025/04/02
 * desc   : seekbar Dialog
 * version: 1.0
 */
class EditSeekBarDialog : DialogFragment() {

    private lateinit var binding: DialogEditSeekBarBinding
    private var onValue: ((name: String, quality: Int) -> Unit)? = null

    fun onValueListener(onValue: (name: String, quality: Int) -> Unit) {
        this.onValue = onValue
    }

    companion object {

        private const val TYPE_TITLE = "title"
        private const val TYPE_INPUT = "input"
        private const val TYPE_VALUE = "value"

        fun buildData(title: String, input: String, value: Int): Bundle {
            val bundle = Bundle()
            bundle.putString(TYPE_TITLE, title)
            bundle.putString(TYPE_INPUT, input)
            bundle.putString(TYPE_VALUE, "$value")
            return bundle
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogEditSeekBarBinding.inflate(inflater, container, false)
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
        arguments?.let { bundle ->
            bundle.getString(TYPE_TITLE)?.let {
                binding.toolbar.title = it
            }
            bundle.getString(TYPE_INPUT)?.let {
                binding.inputPdfName.hint = it
            }
            bundle.getString(TYPE_VALUE)?.let {
                var progress = 85
                if (it.isNotBlank()) {
                    progress = it.toInt()
                }
                binding.seekBar.progress = progress
                binding.tvHint.text = resources.getString(R.string.hint_image_quality, progress)
            }
        }
    }

    private fun init() {
        binding.seekBar.progress = 85
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvHint.text = resources.getString(R.string.hint_image_quality, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
        binding.buttonConfirmed.setOnClickListener {
            val input = binding.inputPdfName.editText?.text.toString().trim()
            if (input.isBlank()) {
                ToastUtil.showToast(requireContext(), "请输入内容")
                return@setOnClickListener
            }
            onValue?.invoke(input, binding.seekBar.progress)
            dismiss()
        }
    }

}