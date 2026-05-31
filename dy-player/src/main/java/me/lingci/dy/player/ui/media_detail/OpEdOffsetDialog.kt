package me.lingci.dy.player.ui.media_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import me.lingci.dy.player.R
import me.lingci.dy.player.databinding.DialogOpEdOffsetBinding
import me.lingci.dy.player.entity.formatTime

/**
 * author : happyc
 * e-mail : bafs.jy@live.com
 * time   : 2025/04/02
 * desc   : seekbar Dialog
 * version: 1.0
 */
class OpEdOffsetDialog : DialogFragment() {

    private lateinit var binding: DialogOpEdOffsetBinding
    private var onValue: ((op: Int, ed: Int) -> Unit)? = null

    fun onValueListener(onValue: (op: Int, ed: Int) -> Unit) {
        this.onValue = onValue
    }

    companion object {

        private const val TYPE_TITLE = "title"
        private const val TYPE_OP_OFFSET = "op_offset"
        private const val TYPE_ED_OFFSET = "ed_offset"

        fun buildData(title: String, op: Int, ed: Int): Bundle {
            val bundle = Bundle()
            bundle.putString(TYPE_TITLE, title)
            bundle.putInt(TYPE_OP_OFFSET, op)
            bundle.putInt(TYPE_ED_OFFSET, ed)
            return bundle
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogOpEdOffsetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                if (it.isNotBlank()) {
                    binding.toolbar.title = it
                }
            }
            val op = bundle.getInt(TYPE_OP_OFFSET, 0)
            val ed = bundle.getInt(TYPE_ED_OFFSET, 0)

            binding.tvOpHint.text = resources.getString(R.string.hint_media_op_offset, op.formatTime())
            binding.tvEdHint.text = resources.getString(R.string.hint_media_ed_offset, ed.formatTime())
            binding.opSeekBar.progress = op
            binding.edSeekBar.progress = ed
        }
    }

    private fun init() {
        binding.opSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvOpHint.text = resources.getString(R.string.hint_media_op_offset, progress.formatTime())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        binding.edSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvEdHint.text = resources.getString(R.string.hint_media_ed_offset, progress.formatTime())

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
            onValue?.invoke(binding.opSeekBar.progress, binding.edSeekBar.progress)
            dismiss()
        }
    }

}