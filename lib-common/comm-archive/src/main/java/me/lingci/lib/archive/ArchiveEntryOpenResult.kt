package me.lingci.lib.archive

import java.io.InputStream

sealed class ArchiveEntryOpenResult {
    data class Success(val stream: InputStream) : ArchiveEntryOpenResult()
    data object WrongPassword : ArchiveEntryOpenResult()
    data object UnsupportedMethod : ArchiveEntryOpenResult()
    data object EntryNotFound : ArchiveEntryOpenResult()
    data object Error : ArchiveEntryOpenResult()
}
