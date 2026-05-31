package me.lingci.lib.base.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 *   @author : happyc
 *   time    : 2025/02/22
 *   desc    :
 *   version : 1.0
 */
object PermissionUtil {

    private const val PERMISSION_REQUEST_CODE = 101

    val storagePermissions = arrayOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    fun requestStoragePermission(activity: Activity, callback: (Boolean) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        } else {
            callback(true)
        }
    }

    fun handlePermissionResult(
        requestCode: Int,
        grantResults: IntArray,
        callback: (Boolean) -> Unit
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            callback(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        }
    }

    /**
     * 检查所有权限是否已被授予
     */
    fun checkPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 申请权限
     */
    fun requestPermissions(activity: android.app.Activity, permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    /**
     * 检查并申请权限
     * @param activity Activity实例
     * @param permissions 要申请的权限数组
     * @param requestCode 请求码
     * @param onGranted 权限全部被授予时的回调
     * @param onDenied 有权限被拒绝时的回调
     */
    fun checkAndRequestPermissions(
        activity: Activity,
        permissions: Array<String>,
        requestCode: Int,
        onGranted: () -> Unit,
        onDenied: () -> Unit = {}
    ) {
        if (checkPermissions(activity, permissions)) {
            onGranted()
        } else {
            requestPermissions(activity, permissions, requestCode)
            onDenied()
        }
    }

}