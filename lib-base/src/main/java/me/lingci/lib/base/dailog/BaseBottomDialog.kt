package me.lingci.lib.base.dailog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.lang.reflect.ParameterizedType

abstract class BaseBottomDialog<VB : ViewBinding>: BottomSheetDialogFragment() {

    // Lazy 初始化 ViewBinding，确保非空且只初始化一次
    private var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = createBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    override fun onStart() {
        super.onStart()
        // 核心逻辑：默认全展开
        expandFully()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 避免内存泄漏
        _binding = null
    }

    protected abstract fun createBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    /**
     * 业务逻辑初始化方法，子类实现
     */
    protected abstract fun init()

    // --- 私有辅助方法 ---

    /**
     * 设置 BottomSheet 为全展开状态
     */
    protected fun expandFully() {
        dialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.maxHeight = resources.displayMetrics.heightPixels
            behavior.skipCollapsed = true
            behavior.isDraggable = false
            bottomSheet.requestLayout()
        }
    }

    /**
     * 反射获取 VB 的 inflate 方法
     */
    @Suppress("UNCHECKED_CAST")
    private fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB {
        try {
            // 获取子类的泛型类型
            val superClass = javaClass.genericSuperclass
            if (superClass is ParameterizedType) {
                val viewModelClass = superClass.actualTypeArguments[0] as Class<VB>
                val method = viewModelClass.getMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java)
                return method.invoke(null, inflater, container, false) as VB
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        throw IllegalArgumentException("Generic VB initialization failed, please check your generic definition.")
    }

}