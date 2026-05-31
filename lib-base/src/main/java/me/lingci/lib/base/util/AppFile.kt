package me.lingci.lib.base.util

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File

/**
 *   @author : happyc
 *   time    : 2025/03/13
 *   desc    :
 *   version : 1.0
 */
class AppFile(private val context: Context) {

    fun buildFiles(name: String): File {
        return File(context.getExternalFilesDir(null), name)
    }

    fun buildCache(name: String): File {
        return File(context.externalCacheDir, name)
    }

    fun buildCustomFolder(dirName: String): File {
        return context.getExternalFilesDir(dirName)?: buildCustom(dirName, "")
    }

    fun buildCustom(dirName: String, name: String): File {
        return File(context.getExternalFilesDir(dirName), name)
    }

    fun copyUsingFileProvider(source: File, dest: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", source)
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

}