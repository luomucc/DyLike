package me.lingci.lib.base.ui

import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.lingci.lib.base.dailog.LoadingDialog

/**
 *   @author : happyc
 *   time    : 2023/07/05
 *   desc    :
 *   version : 1.0
 */
abstract class BaseFragment : Fragment() {

    companion object {
        const val TAG = "lcDev"
    }

    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadingDialog = LoadingDialog(requireActivity())
    }

    protected fun showLoading() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (!loadingDialog.isShowing) {
                loadingDialog.show()
            }
        }
    }

    protected fun hideLoading() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (loadingDialog.isShowing) {
                loadingDialog.dismiss()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        resetView()
        super.onConfigurationChanged(newConfig)
    }

    abstract fun resetView()

    open fun isLandscape(): Boolean {
        return activity is BaseActivity && !(activity as BaseActivity).isScreenPortrait()
    }

    open fun buildVerticalGrid(span: Int): GridLayoutManager {
        return GridLayoutManager(context, span, RecyclerView.VERTICAL, false)
    }

    open fun buildVerticalLiner() : LinearLayoutManager {
        return LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    }

}