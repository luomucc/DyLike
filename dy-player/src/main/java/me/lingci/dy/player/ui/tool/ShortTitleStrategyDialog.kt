package me.lingci.dy.player.ui.tool

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import me.lingci.dy.player.core.ShortTitleStrategy
import me.lingci.dy.player.databinding.DialogShortTitleStrategyBinding
import me.lingci.dy.player.ui.tool.ShortTitleStrategyDialog.Companion.buildArgs
import me.lingci.dy.player.util.ShortTitleFormatter

/**
 * 短视频标题策略设置弹窗。
 *
 * 通过 [buildArgs] 传入当前策略参数，确认后通过 [onValueListener] 回调新参数。
 * 内含实时预览，帮助用户直观查看格式化效果。
 */
class ShortTitleStrategyDialog : DialogFragment() {

    private lateinit var binding: DialogShortTitleStrategyBinding
    private var onValue: ((strategy: Int, delimiter: String, regex: String, maxLines: Int) -> Unit)? = null

    private val maxLinesOptions = listOf(0, 2, 3, 4, 5)
    private val maxLinesLabels = listOf("不限", "2", "3", "4", "5")

    private val previewExamples = listOf(
        "UP-VLOG-这是一段文字.mp4",
        "UP_VLOG_这也是一段文字.mp4"
    )

    fun onValueListener(onValue: (strategy: Int, delimiter: String, regex: String, maxLines: Int) -> Unit) {
        this.onValue = onValue
    }

    companion object {
        private const val KEY_STRATEGY = "strategy"
        private const val KEY_DELIMITER = "delimiter"
        private const val KEY_REGEX = "regex"
        private const val KEY_MAX_LINES = "max_lines"

        fun buildArgs(
            strategy: Int,
            delimiter: String,
            regex: String,
            maxLines: Int
        ): Bundle {
            return Bundle().apply {
                putInt(KEY_STRATEGY, strategy)
                putString(KEY_DELIMITER, delimiter)
                putString(KEY_REGEX, regex)
                putInt(KEY_MAX_LINES, maxLines)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogShortTitleStrategyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    override fun onStart() {
        super.onStart()
        arguments?.let { bundle ->
            val strategy = ShortTitleStrategy.fromValue(bundle.getInt(KEY_STRATEGY, 0))
            val delimiter = bundle.getString(KEY_DELIMITER, "-")
            val regex = bundle.getString(KEY_REGEX, "")
            val maxLines = bundle.getInt(KEY_MAX_LINES, 0)

            binding.spinnerStrategyMode.setSelection(strategy.ordinal)
            binding.etDelimiter.setText(delimiter)
            binding.etRegex.setText(regex)
            binding.spinnerMaxLines.setSelection(
                maxLinesOptions.indexOf(maxLines).coerceAtLeast(0)
            )

            updateDependentViews(strategy)
            updatePreview()
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (isAdded && !isHidden) return
        super.show(manager, tag)
    }

    override fun show(transaction: FragmentTransaction, tag: String?): Int {
        if (isAdded && !isHidden) return -1
        return super.show(transaction, tag)
    }

    private fun init() {
        // 策略 Spinner
        val strategyAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            ShortTitleStrategy.entries.map { it.displayName }
        )
        strategyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStrategyMode.adapter = strategyAdapter
        binding.spinnerStrategyMode.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateDependentViews(ShortTitleStrategy.entries[position])
                updatePreview()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // 最大行数 Spinner
        val maxLinesAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            maxLinesLabels
        )
        maxLinesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMaxLines.adapter = maxLinesAdapter
        binding.spinnerMaxLines.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                updatePreview()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // 分隔符输入
        binding.etDelimiter.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreview()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // 正则输入
        binding.etRegex.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreview()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // 取消 / 确认
        binding.buttonCancel.setOnClickListener { dismiss() }
        binding.buttonConfirmed.setOnClickListener {
            val strategy = ShortTitleStrategy.entries[binding.spinnerStrategyMode.selectedItemPosition]
            val delimiter = binding.etDelimiter.text.toString()
            val regex = binding.etRegex.text.toString()
            val maxLines = maxLinesOptions[binding.spinnerMaxLines.selectedItemPosition]
            onValue?.invoke(strategy.value, delimiter, regex, maxLines)
            dismiss()
        }
    }

    /** 根据策略启用/置灰正则输入框。 */
    private fun updateDependentViews(strategy: ShortTitleStrategy) {
        val regexEnabled = strategy == ShortTitleStrategy.REGEX_FIRST
        binding.etRegex.isEnabled = regexEnabled
        binding.etRegex.alpha = if (regexEnabled) 1f else 0.4f
        binding.tvLabelRegex.alpha = if (regexEnabled) 1f else 0.4f
        binding.tvLabelRegexDesc.alpha = if (regexEnabled) 1f else 0.4f
    }

    /** 用预设示例名实时预览格式化效果。 */
    private fun updatePreview() {
        val strategy = ShortTitleStrategy.entries[binding.spinnerStrategyMode.selectedItemPosition]
        val delimiter = binding.etDelimiter.text.toString()
        val regex = binding.etRegex.text.toString()
        val maxLines = maxLinesOptions[binding.spinnerMaxLines.selectedItemPosition]

        val lines = previewExamples.map { example ->
            val result = ShortTitleFormatter.format(example, strategy, delimiter, regex, maxLines)
            // 显示源文件名和结果
            val src = example.substringBeforeLast(".")
            if (result == src) {
                "• $src"
            } else {
                "• $result"
            }
        }
        binding.tvPreview.text = lines.joinToString("\n")
    }
}
