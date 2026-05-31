package me.lingci.lib.base.dailog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.lingci.lib.base.R

/**
 *   @author : happyc
 *   time    : 2025/03/18
 *   desc    : dialog辅助类
 *   version : 1.0
 */
object DialogHelper {

    fun createMsg(context: Context, title: String, msg: String, action: String): AlertDialog {
        return MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton(action) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    fun createAction(
        context: Context,
        title: String,
        msg: String,
        onAction: () -> Unit
    ): AlertDialog {
        return MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton(context.getString(R.string.action_positive)) { dialog, _ ->
                dialog.dismiss()
                onAction.invoke()
            }
            .setNegativeButton(context.getString(R.string.action_negative)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    fun createAction2(
        context: Context,
        title: String,
        msg: String,
        onAction: (onConfirmed: Boolean) -> Unit
    ): AlertDialog {
        return MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton(context.getString(R.string.action_positive)) { dialog, _ ->
                dialog.dismiss()
                onAction.invoke(true)
            }
            .setNegativeButton(context.getString(R.string.action_negative)) { dialog, _ ->
                dialog.dismiss()
                onAction.invoke(false)
            }
            .create()
    }

}