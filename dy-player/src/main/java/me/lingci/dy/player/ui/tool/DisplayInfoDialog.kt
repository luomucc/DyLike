package me.lingci.dy.player.ui.tool

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.Display.HdrCapabilities
import android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION
import android.view.Display.HdrCapabilities.HDR_TYPE_HDR10
import android.view.Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS
import android.view.Display.HdrCapabilities.HDR_TYPE_HLG
import android.view.Display.HdrCapabilities.HDR_TYPE_INVALID
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import me.lingci.dy.player.databinding.DialogDisplayInfoBinding

/**
 * author : happyc
 * e-mail : bafs.jy@live.com
 * time   : 2025/09/15
 * desc   : 显示 Dialog
 * version: 1.0
 */
class DisplayInfoDialog : DialogFragment() {

    private lateinit var binding: DialogDisplayInfoBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogDisplayInfoBinding.inflate(inflater, container, false)
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
    }

    private fun init() {
        binding.toolbar.title = "显示信息支持"
        val string = StringBuilder()
        val capabilities = getSupportedHdrTypes(requireContext())
        val hdrCapabilities = getHdrCapabilities(requireContext())
        val luminanceInfo = buildLuminanceInfo(hdrCapabilities)
        if (capabilities.contains(HDR_TYPE_INVALID)) {
            string.appendLine("Invalid HDR type value.")
        }
        if (capabilities.contains(HDR_TYPE_DOLBY_VISION)) {
            string.appendLine("Dolby Vision high dynamic range (HDR) display.")
        }
        if (capabilities.contains(HDR_TYPE_HDR10)) {
            string.appendLine("HDR10 display.")
        }
        if (capabilities.contains(HDR_TYPE_HLG)) {
            string.appendLine("Hybrid Log-Gamma HDR display.")
        }
        if (capabilities.contains(HDR_TYPE_HDR10_PLUS)) {
            string.appendLine("HDR10+ display.")
        }
        if (string.isEmpty()) {
            string.appendLine("No HDR types reported by this display.")
        }
        string.appendLine()
        string.appendLine("Luminance")
        string.appendLine("max: ${luminanceInfo.max}")
        string.appendLine("max average: ${luminanceInfo.maxAverage}")
        string.appendLine("min: ${luminanceInfo.min}")
        binding.tvHint.text = string.toString()


        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
        binding.buttonConfirmed.setOnClickListener {
            dismiss()
        }
    }

    private fun getSupportedHdrTypes(context: Context): IntArray {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.display.mode.supportedHdrTypes
            // API 34+ 新方法
            // val displayManager = context.getSystemService(DisplayManager::class.java)
            // val defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            // defaultDisplay?.hdrCapabilities?.supportedHdrTypes ?: intArrayOf()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            context.display.hdrCapabilities?.supportedHdrTypes ?: intArrayOf()
        } else {
            // 兼容旧版本
            @Suppress("DEPRECATION")
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            @Suppress("DEPRECATION")
            defaultDisplay?.hdrCapabilities?.supportedHdrTypes ?: intArrayOf()
        }
    }

    private fun getHdrCapabilities(context: Context): HdrCapabilities? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.hdrCapabilities
        } else {
            @Suppress("DEPRECATION")
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            @Suppress("DEPRECATION")
            displayManager.getDisplay(Display.DEFAULT_DISPLAY)?.hdrCapabilities
        }
    }

    private fun buildLuminanceInfo(capabilities: HdrCapabilities?): LuminanceInfo {
        if (capabilities == null) {
            return LuminanceInfo("Unavailable", "Unavailable", "Unavailable")
        }
        return LuminanceInfo(
            max = formatLuminance(capabilities.desiredMaxLuminance),
            maxAverage = formatLuminance(capabilities.desiredMaxAverageLuminance),
            min = formatLuminance(capabilities.desiredMinLuminance)
        )
    }

    private fun formatLuminance(value: Float): String {
        return if (value == HdrCapabilities.INVALID_LUMINANCE) {
            "Unavailable"
        } else {
            String.format("%.2f nits", value)
        }
    }

    private data class LuminanceInfo(
        val max: String,
        val maxAverage: String,
        val min: String,
    )

}
