package me.lingci.lib.base.dailog

import android.os.Bundle
import android.text.SpannableString
import android.text.style.LeadingMarginSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import me.lingci.lib.base.databinding.HelpInfoDialogBinding
import me.lingci.lib.base.util.CodeUtil

/**
 * author : happyc
 * e-mail : bafs.jy@live.com
 * time   : 2025/03/14
 * desc   : 提示信息Dialog
 * version: 1.0
 */
class HelpInfoDialog : DialogFragment() {

    companion object {

        private const val TAG = "HelpInfoDialog"
        private const val KEY_TITLE = "title"
        private const val KEY_MSG = "msg"
        private const val KEY_ACTION = "action"

        fun buildBundle(msg: String): Bundle {
            return Bundle().apply {
                putString(KEY_MSG, msg)
            }
        }

        fun buildBundle(title: String, msg: String, action: String): Bundle {
            return Bundle().apply {
                putString(KEY_TITLE, title)
                putString(KEY_MSG, msg)
                putString(KEY_ACTION, action)
            }
        }
    }

    private lateinit var binding: HelpInfoDialogBinding
    private var onRoger: (() -> Unit)? = null

    fun setOnRogerListener(onRoger: () -> Unit) {
        this.onRoger = onRoger
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HelpInfoDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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
        arguments?.let {
            if (::binding.isInitialized) {
                it.getString(KEY_TITLE)?.let { title ->
                    binding.toolbar.title = title
                }
                it.getString(KEY_MSG)?.let { msg ->
                    setHelpInfo(msg)
                }
                it.getString(KEY_ACTION)?.let { action ->
                    binding.actionRoger.text = action
                }
            }
        }
    }

    private fun init() {
        binding.actionRoger.setOnClickListener {
            onRoger?.invoke()
            dismiss()
        }
    }

    private fun setHelpInfo(text: String) {
        val spannableString = SpannableString(text)
        // 第一个参数为首行缩进像素值，第二个参数为后续行缩进像素值
        val marginSpan = LeadingMarginSpan.Standard(CodeUtil.dp2px(24f), 0)
        spannableString.setSpan(marginSpan, 0, text.length, 0)
        binding.tvHelpInfo.text = spannableString
    }

}
