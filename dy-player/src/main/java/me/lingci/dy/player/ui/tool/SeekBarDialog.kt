package me.lingci.dy.player.ui.tool

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.DialogFragment
import me.lingci.dy.player.R
import me.lingci.dy.player.databinding.DialogSeekBarBinding
import me.lingci.lib.base.util.Log
import kotlin.math.max

/**
 * author : happyc
 * e-mail : bafs.jy@live.com
 * time   : 2025/04/02
 * desc   : seekbar Dialog
 * version: 1.0
 */
class SeekBarDialog : DialogFragment() {

    private lateinit var binding: DialogSeekBarBinding
    private var onValue: ((value: Number) -> Unit)? = null
    private var type = 1
    private var factor = 1

    fun onValueListener(onValue: (value: Number) -> Unit) {
        this.onValue = onValue
    }

    companion object {

        private const val TYPE_TITLE = "title"
        private const val TYPE_VALUE = "value"
        private const val TYPE_FACTOR = "factor"

        fun buildData(title: String, value: Int): Bundle {
            val bundle = Bundle()
            bundle.putString(TYPE_TITLE, title)
            bundle.putInt(TYPE_VALUE, value)
            bundle.putInt(TYPE_FACTOR, 1)
            return bundle
        }

        fun buildData(title: String, value: Int, factor: Int): Bundle {
            val bundle = Bundle()
            bundle.putString(TYPE_TITLE, title)
            bundle.putInt(TYPE_VALUE, value)
            bundle.putInt(TYPE_FACTOR, factor)
            return bundle
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogSeekBarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    fun isShowing(): Boolean {
        return isAdded && !isHidden
    }

    @SuppressLint("DefaultLocale")
    override fun onStart() {
        super.onStart()
        Log.d(this@SeekBarDialog, "onStart")
        arguments?.let { bundle ->
            bundle.getString(TYPE_TITLE)?.let {
                binding.toolbar.title = it
            }
            val value = bundle.getInt(TYPE_VALUE, 50)
            val factor = bundle.getInt(TYPE_FACTOR, 1)
            this.factor = factor
            type = if (factor == 1) {
                binding.tvHint.text = resources.getString(R.string.hint_ratio_value, "$value")
                1
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    binding.seekBar.min = this.factor
                }
                binding.tvHint.text = resources.getString(R.string.hint_ratio_value, String.format("%.2f", (value.toFloat() / this.factor)))
                2
            }
            binding.seekBar.progress = value
        }
    }

    private fun init() {
        binding.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            @SuppressLint("DefaultLocale")
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (type == 1) {
                    binding.tvHint.text = resources.getString(R.string.hint_ratio_value, "$progress")
                } else {
                    binding.tvHint.text = resources.getString(R.string.hint_ratio_value, String.format("%.2f", (progress.toFloat() / factor)))
                }
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
            if (type == 1) {
                onValue?.invoke(binding.seekBar.progress)
            } else {
                onValue?.invoke(max(binding.seekBar.progress, 1).toFloat() / factor)
            }
            dismiss()
        }
    }

}