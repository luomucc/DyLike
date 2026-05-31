package me.lingci.dy.player.ui.media_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import me.lingci.dy.player.databinding.DialogRenameBinding

class RenameDialog : DialogFragment() {

    private lateinit var binding: DialogRenameBinding
    private var onRename: ((newName: String) -> Unit)? = null

    fun onRenameListener(onRename: (newName: String) -> Unit) {
        this.onRename = onRename
    }

    companion object {

        private const val TYPE_FILE_NAME = "file_name"

        fun buildData(fileName: String): Bundle {
            val bundle = Bundle()
            bundle.putString(TYPE_FILE_NAME, fileName)
            return bundle
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogRenameBinding.inflate(inflater, container, false)
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
            val fileName = bundle.getString(TYPE_FILE_NAME, "")
            // 去掉扩展名填入输入框
            val nameWithoutExtension = fileName.substringBeforeLast(".", fileName)
            binding.etFileName.setText(nameWithoutExtension)
            binding.etFileName.setSelection(nameWithoutExtension.length)
        }
    }

    private fun init() {
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
        binding.buttonConfirmed.setOnClickListener {
            val newName = binding.etFileName.text?.toString()?.trim().orEmpty()
            if (newName.isBlank()) {
                binding.tilFileName.error = "文件名不能为空"
                return@setOnClickListener
            }
            binding.tilFileName.error = null
            onRename?.invoke(newName)
            dismiss()
        }
    }

}
