package me.lingci.lib.base.dailog

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.view.LayoutInflater
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import me.lingci.lib.base.R
import me.lingci.lib.base.databinding.DialogLoadingBinding

/**
 * author : happyc
 * e-mail : bafs.jy@live.com
 * time   : 2018/02/24
 * desc   : 加载动画
 * version: 1.0
 */
class LoadingDialog @JvmOverloads constructor(
    context: Context,
    @StyleRes themeResId: Int = R.style.AppTheme_Dialog
) : AlertDialog(context, themeResId) {

    private var binding: DialogLoadingBinding =
        DialogLoadingBinding.inflate(LayoutInflater.from(context))
    private lateinit var animation: AnimationDrawable

    init {
        setView(binding.root)
        init()
    }

    private fun init() {
        setCanceledOnTouchOutside(true)
        animation = binding.loadingImg.background as AnimationDrawable
    }

    override fun onStart() {
        super.onStart()
        binding.loadingImg.post { animation.start() }
    }

    override fun onStop() {
        binding.loadingImg.post { if (animation.isRunning) animation.stop() }
        super.onStop()
    }

    override fun show() {
        if (isShowing) {
            return
        }
        super.show()
    }

}