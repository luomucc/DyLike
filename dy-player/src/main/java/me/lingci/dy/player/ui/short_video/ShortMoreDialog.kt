package me.lingci.dy.player.ui.short_video

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import me.lingci.dy.player.databinding.DialogShortMoreBinding

class ShortMoreDialog : BottomSheetDialogFragment() {

    private lateinit var binding: DialogShortMoreBinding
    private var moreActionListener: OnMoreActionListener? = null

    interface OnMoreActionListener {
        fun onRename()
        fun onDelete()
        fun onShare()
    }

    fun setMoreActionListener(listener: OnMoreActionListener) {
        moreActionListener = listener
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
        binding = DialogShortMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.layoutRename.setOnClickListener {
            moreActionListener?.onRename()
            dismiss()
        }
        binding.layoutDelete.setOnClickListener {
            moreActionListener?.onDelete()
            dismiss()
        }
        binding.layoutShare.setOnClickListener {
            moreActionListener?.onShare()
            dismiss()
        }
    }

}
