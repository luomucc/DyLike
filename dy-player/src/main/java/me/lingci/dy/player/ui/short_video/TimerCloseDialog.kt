package me.lingci.dy.player.ui.short_video

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import me.lingci.dy.player.R
import me.lingci.dy.player.databinding.DialogTimerCloseBinding

class TimerCloseDialog : BottomSheetDialogFragment(), View.OnClickListener {

    companion object {
        private const val KEY_CURRENT_MINUTES = "current_minutes"

        fun newInstance(currentMinutes: Int): TimerCloseDialog {
            return TimerCloseDialog().apply {
                arguments = Bundle().apply {
                    putInt(KEY_CURRENT_MINUTES, currentMinutes)
                }
            }
        }
    }

    private lateinit var binding: DialogTimerCloseBinding
    private var onTimerSelected: ((minutes: Int) -> Unit)? = null

    fun onTimerSelected(onTimerSelected: (minutes: Int) -> Unit) {
        this.onTimerSelected = onTimerSelected
    }

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
    ): View {
        binding = DialogTimerCloseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        val currentMinutes = arguments?.getInt(KEY_CURRENT_MINUTES, 0) ?: 0
        updateSelectedState(currentMinutes)

        binding.tvTimerOff.setOnClickListener(this)
        binding.tvTimer10.setOnClickListener(this)
        binding.tvTimer20.setOnClickListener(this)
        binding.tvTimer30.setOnClickListener(this)
        binding.tvTimer45.setOnClickListener(this)
        binding.tvTimer60.setOnClickListener(this)
        binding.tvTimerCustom.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        v?.let {
            val minutes = getTimerMinutes(v)
            if (minutes == -2) {
                showCustomTimerDialog()
            } else if (minutes >= 0) {
                onTimerSelected?.invoke(minutes)
                dismiss()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateSelectedState(currentMinutes: Int) {
        binding.tvTimerOff.isSelected = currentMinutes == 0
        binding.tvTimer10.isSelected = currentMinutes == 10
        binding.tvTimer20.isSelected = currentMinutes == 20
        binding.tvTimer30.isSelected = currentMinutes == 30
        binding.tvTimer45.isSelected = currentMinutes == 45
        binding.tvTimer60.isSelected = currentMinutes == 60
        binding.tvTimerCustom.isSelected = currentMinutes > 0 && currentMinutes !in listOf(10, 20, 30, 45, 60)
    }

    private fun getTimerMinutes(v: View): Int {
        return when (v.id) {
            binding.tvTimerOff.id -> 0
            binding.tvTimer10.id -> 10
            binding.tvTimer20.id -> 20
            binding.tvTimer30.id -> 30
            binding.tvTimer45.id -> 45
            binding.tvTimer60.id -> 60
            binding.tvTimerCustom.id -> -2
            else -> -1
        }
    }

    private fun showCustomTimerDialog() {
        val editText = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.hint_timer_custom_input)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.hint_timer_close)
            .setView(editText)
            .setPositiveButton(R.string.action_confirmed) { _, _ ->
                val minutes = editText.text.toString().toIntOrNull()
                if (minutes != null && minutes in 1..999) {
                    onTimerSelected?.invoke(minutes)
                    dismiss()
                } else {
                    Toast.makeText(
                        requireContext(),
                        R.string.hint_timer_custom_invalid,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

}
