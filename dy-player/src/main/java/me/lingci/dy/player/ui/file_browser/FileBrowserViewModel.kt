package me.lingci.dy.player.ui.file_browser

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.lingci.lib.base.storage.entity.FileEntity
import me.lingci.lib.base.util.Comparators

class FileBrowserViewModel : ViewModel() {

    private val _text = MutableLiveData<String>()
    val text: LiveData<String> = _text

    private val fileList = mutableListOf<FileEntity>()

    fun setFileList(list: List<FileEntity>) {
        fileList.clear()
        fileList.addAll(list)
    }

    fun listFile(search: String = ""): MutableList<FileEntity> {
        return if (search.isBlank()) {
            fileList
        } else {
            fileList.filter {
                item -> item.name.contains(search, ignoreCase = true)
            }.toMutableList()
        }
    }

    fun sorted() {
        fileList.sortWith(Comparators.fileEntityFolderComparator())
    }

}