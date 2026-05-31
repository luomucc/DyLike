package me.lingci.lib.dm.view.util

import me.lingci.lib.archive.ArchivePasswordVerifyResult
import me.lingci.lib.archive.ArchiveEntryOpenResult
import me.lingci.lib.archive.ZArchiveFile
import me.lingci.lib.base.storage.entity.FileEntity
import me.lingci.lib.base.util.Comparators
import me.lingci.lib.base.util.Log
import me.lingci.lib.base.util.md5
import java.io.File
import java.io.InputStream

object ZipXmlLoader {

    const val ZIP = "zip"
    const val ZIP_EXTENSION: String = ".zip"
    const val SEVEN_Z_EXTENSION: String = ".7z"
    const val RAR_EXTENSION: String = ".rar"

    enum class PasswordVerifyResult {
        VALID,
        WRONG_PASSWORD,
        UNSUPPORTED_METHOD,
        ENTRY_NOT_FOUND,
        ERROR
    }

    enum class OpenResultState {
        SUCCESS,
        WRONG_PASSWORD,
        UNSUPPORTED_METHOD,
        ENTRY_NOT_FOUND,
        ERROR
    }

    data class OpenResult(
        val state: OpenResultState,
        val stream: InputStream? = null
    )

    fun listXmlEntries(file: File, password: String = ""): List<FileEntity> {
        Log.d(this, file.name)
        return try {
            ZArchiveFile.listZEntry(file.path, password)
                .filter { !it.isDirectory }
                .filter { isXml(it.name) }
                .map {
                    Log.d(this, it.name)
                    FileEntity(
                        mimeType = ZIP,
                        isFile = true,
                        title = it.name,
                        name = it.name,
                        size = it.size,
                        path = file.path,
                        id = "${file.name}/${it.name}".md5()
                    )
                }
                .sortedWith(Comparators.fileEntityNameComparator())
                .toList()
        } catch (e: Exception) {
            Log.d("list zip failed", e)
            emptyList()
        }
    }

    private fun isXml(name: String): Boolean {
        val ext = name.substringAfterLast(".", "").lowercase()
        return ext in setOf("xml")
    }

    fun loadXmlFromZip(fileEntity: FileEntity, password: String = ""): InputStream? {
        try {
            return ZArchiveFile.openZEntry(fileEntity.path, password, fileEntity.name)
        } catch (e: Exception) {
            Log.d("load zip xml failed", e)
            return null
        }
    }

    fun loadXmlFromZipResult(fileEntity: FileEntity, password: String = ""): OpenResult {
        return try {
            when (val result = ZArchiveFile.openZEntryResult(fileEntity.path, password, fileEntity.name)) {
                is ArchiveEntryOpenResult.Success -> OpenResult(OpenResultState.SUCCESS, result.stream)
                ArchiveEntryOpenResult.WrongPassword -> OpenResult(OpenResultState.WRONG_PASSWORD)
                ArchiveEntryOpenResult.UnsupportedMethod -> OpenResult(OpenResultState.UNSUPPORTED_METHOD)
                ArchiveEntryOpenResult.EntryNotFound -> OpenResult(OpenResultState.ENTRY_NOT_FOUND)
                ArchiveEntryOpenResult.Error -> OpenResult(OpenResultState.ERROR)
            }
        } catch (e: Exception) {
            Log.d("load zip xml result failed", e)
            OpenResult(OpenResultState.ERROR)
        }
    }

    fun checkPassword(file: File): Boolean {
        return ZArchiveFile.checkPassword(file.path)
    }

    fun verifyPassword(file: File, password: String): PasswordVerifyResult {
        return when (ZArchiveFile.verifyPassword(file.path, password)) {
            ArchivePasswordVerifyResult.VALID -> PasswordVerifyResult.VALID
            ArchivePasswordVerifyResult.WRONG_PASSWORD -> PasswordVerifyResult.WRONG_PASSWORD
            ArchivePasswordVerifyResult.UNSUPPORTED_METHOD -> PasswordVerifyResult.UNSUPPORTED_METHOD
            ArchivePasswordVerifyResult.ENTRY_NOT_FOUND -> PasswordVerifyResult.ENTRY_NOT_FOUND
            ArchivePasswordVerifyResult.ERROR -> PasswordVerifyResult.ERROR
        }
    }

}
