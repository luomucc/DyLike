package me.lingci.dy.player.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import me.lingci.dy.player.R
import me.lingci.dy.player.databinding.DialogVerisonBinding
import me.lingci.dy.player.entity.VersionData

/**
 *   @author : happyc
 *   time    : 2024/09/21
 *   desc    :
 *   version : 1.0
 */
class VersionDialog(
    private val onClick: (() -> Unit)?
) : DialogFragment() {

    private lateinit var binding: DialogVerisonBinding
    private var mVersionData: VersionData? = null

    companion object {
        const val KEY_VERSION = "version"

        fun newInstance(bean: VersionData, onClick: (() -> Unit)?): VersionDialog {
            val f = VersionDialog(onClick)
            val args = Bundle()
            args.putParcelable(KEY_VERSION, bean)
            f.arguments = args
            return f
        }
    }

    constructor() : this(onClick = null)

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mVersionData = it.getParcelable(KEY_VERSION)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogVerisonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        isCancelable = true
        binding.actionCancel.setOnClickListener {
            dismiss()
        }
        binding.actionConfirmed.setOnClickListener {
            dismiss()
        }
        mVersionData?.let { version ->
            if (version.type == 2) {
                isCancelable = false
            }
            binding.textMessage.text = version.info
            if (version.remark.isNullOrBlank()) {
                binding.textRemark.visibility = View.GONE
            } else {
                binding.textRemark.text = version.remark
            }
            if (version.type == 0) {
                binding.textTitle.text = getString(R.string.hint_version_message)
                binding.actionConfirmed.text = getString(me.lingci.lib.base.R.string.action_confirmed)
            } else {
                binding.textTitle.text = getString(R.string.hint_version_update)
                binding.actionConfirmed.text = getString(R.string.action_goto_update)
                binding.actionConfirmed.setOnClickListener {
                    onClick?.let {
                        it()
                    }
                    dismiss()
                }
            }
        }
    }

}
