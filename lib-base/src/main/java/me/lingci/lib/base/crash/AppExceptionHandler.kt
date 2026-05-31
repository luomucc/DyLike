package me.lingci.lib.base.crash

import android.content.Context
import me.lingci.lib.base.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * app异常处理
 */
class AppExceptionHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            // 保存异常信息到本地文件
            CrashToFile.saveExceptionToFile(context, e)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        // 调用默认的异常处理程序
        defaultHandler?.uncaughtException(t, e)
    }

}

object CrashToFile {

    fun saveExceptionToFile(context: Context, throwable: Throwable) {
        saveExceptionToFile(context, throwable, "global")
    }

    fun saveExceptionToFile(context: Context, throwable: Throwable, type: String) {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val fileName = "${type}_crash_$timeStamp.log"
        val logFile = File(context.getExternalFilesDir("logs"), fileName)

        try {
            FileWriter(logFile).use { writer ->
                throwable.printStackTrace(object : java.io.PrintWriter(writer) {})
            }
        } catch (e: IOException) {
            Log.d(this, e.message)
        }
    }

}