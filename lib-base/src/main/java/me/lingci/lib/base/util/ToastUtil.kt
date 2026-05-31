package me.lingci.lib.base.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * author : happyc
 * e-mail : bafs.jy@live.com
 * time   : 2018/07/24
 * desc   : Toast Util
 * version: 1.0
 */
object ToastUtil {

    private var toast: Toast? = null
    private val mHandler = Handler(Looper.getMainLooper())

    fun showToast(context: Context, info: String) {
        mHandler.post {
            toastShow(
                context,
                info,
                Toast.LENGTH_SHORT
            )
        }
    }

    fun showToast(context: Context, infoId: Int) {
        mHandler.post {
            toastShow(
                context,
                context.resources.getString(infoId),
                Toast.LENGTH_SHORT
            )
        }
    }

    fun longToast(context: Context, info: String) {
        mHandler.post {
            toastShow(
                context,
                info,
                Toast.LENGTH_LONG
            )
        }
    }

    @SuppressLint("ShowToast")
    private fun toastShow(context: Context, info: String, duration: Int) {
        if (toast == null) {
            toast = Toast.makeText(context, info, duration)
        } else {
            toast!!.setText(info)
        }
        toast!!.show()
    }

}