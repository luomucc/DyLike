package me.lingci.dy.player.ui.source

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.lingci.dy.player.R
import me.lingci.dy.player.databinding.DialogSourceDavBinding
import me.lingci.dy.player.entity.SourceData
import me.lingci.dy.player.util.AppUtil
import me.lingci.dy.player.util.LibraryCompat
import me.lingci.lib.base.storage.IStorage
import me.lingci.lib.base.storage.entity.StorageConfig
import me.lingci.lib.base.storage.entity.StorageType
import me.lingci.lib.base.storage.impl.WebDavStorage
import me.lingci.lib.base.util.ToastUtil

/**
 * @author : happyc
 * time    : 2024/09/21
 * desc    :
 * version : 1.0
 */
open class WebdavManagerDialog(
    private val onSave: (source: SourceData) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: DialogSourceDavBinding
    private var mSourceData: SourceData? = null
    private var storage: IStorage? = null
    private var testJob: Job? = null

    companion object {
        const val KEY_SOURCE = "version"

        fun newInstance(
            bean: SourceData?,
            onClick: (source: SourceData) -> Unit
        ): WebdavManagerDialog {
            val f = WebdavManagerDialog(onClick)
            val args = Bundle()
            args.putParcelable(KEY_SOURCE, bean)
            f.arguments = args
            return f
        }
    }

    constructor() : this(onSave = {})

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_Material3_DayNight_BottomSheetDialog)
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mSourceData = it.getParcelable(KEY_SOURCE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogSourceDavBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        mSourceData?.let { source ->
            binding.toolbar.title = getString(R.string.hint_source_dav_edit)
            binding.inputName.editText?.setText(source.title)
            binding.inputSite.editText?.setText(source.siteUrl)
            binding.inputUsername.editText?.setText(source.username)
            binding.inputPassword.editText?.setText(source.password)
        }

        binding.actionTest.setOnClickListener {
            binding.textStatus.text = ""
            if (binding.inputSite.editText!!.text.isNullOrBlank()) {
                ToastUtil.showToast(requireContext(), "请填写服务器地址")
                return@setOnClickListener
            }
            var siteUrl = binding.inputSite.editText!!.text.toString().trim()
            if (!AppUtil.isUrl(siteUrl)) {
                ToastUtil.showToast(requireContext(), "请填写正确的服务器地址")
                return@setOnClickListener
            }
            var username = "guest"
            var password = "guest"
            if (!binding.inputUsername.editText!!.text.isNullOrBlank() && !binding.inputPassword.editText!!.text.isNullOrBlank()) {
                username = binding.inputUsername.editText!!.text.toString().trim()
                password = binding.inputPassword.editText!!.text.toString().trim()
            }
            storage = WebDavStorage(
                StorageConfig.WebDavStorageConfig(
                    id = "",
                    name = "",
                    url = siteUrl,
                    username = username,
                    password = password
                ), ""
            )
            if (testJob != null && testJob!!.isCompleted.not()) {
                return@setOnClickListener
            }
            testJob = lifecycleScope.launch(Dispatchers.IO) {
                storage?.testConnect().let { flag ->
                    withContext(Dispatchers.Main) {
                        if (flag == true) {
                            binding.textStatus.text = "连接成功"
                            binding.textStatus.setTextColor(
                                resources.getColor(
                                    me.lingci.lib.base.R.color.green_500,
                                    requireContext().theme
                                )
                            )
                        } else {
                            binding.textStatus.text = "连接失败"
                            binding.textStatus.setTextColor(
                                resources.getColor(
                                    me.lingci.lib.base.R.color.red_600,
                                    requireContext().theme
                                )
                            )
                        }
                    }
                }
            }
        }
        binding.actionCancel.setOnClickListener { dismiss() }
        binding.actionConfirmed.setOnClickListener {
            if (binding.inputSite.editText!!.text.isNullOrBlank()) {
                ToastUtil.showToast(requireContext(), "请填写服务器地址")
                return@setOnClickListener
            }
            var siteUrl = binding.inputSite.editText!!.text.toString().trim()
            if (!AppUtil.isUrl(siteUrl)) {
                ToastUtil.showToast(requireContext(), "请填写正确的服务器地址")
                return@setOnClickListener
            }
            val source = SourceData()
            source.type = StorageType.WEBDAV
            mSourceData?.let {
                source.id = it.id
            }
            source.siteUrl = siteUrl
            if (binding.inputName.editText!!.text.isNullOrBlank()) {
                source.title = "WebDav资源库"
            } else {
                source.title = binding.inputName.editText!!.text.toString().trim()
            }
            if (!binding.inputUsername.editText!!.text.isNullOrBlank() && !binding.inputPassword.editText!!.text.isNullOrBlank()) {
                source.username = binding.inputUsername.editText!!.text.toString().trim()
                source.password = binding.inputPassword.editText!!.text.toString().trim()
            } else {
                source.username = "guest"
                source.password = "guest"
            }
            if (source.id.isBlank()) {
                source.id = LibraryCompat.sourceId(source)
            }
            onSave(source)
            dismiss()
        }
    }

    override fun dismiss() {
        storage?.release()
        testJob?.cancel()
        super.dismiss()
    }

}
