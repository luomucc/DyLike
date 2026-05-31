package me.lingci.dy.player.ui.media_add

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.lingci.dy.player.entity.MediaData
import me.lingci.lib.base.storage.entity.FileEntity

class MediaManagerViewModel : ViewModel() {

    private val _text = MutableLiveData<String>()
    val text: LiveData<String> = _text

    fun setText(value: String) {
        _text.postValue(value)
    }

    private val _mediaData = MutableLiveData<MediaData>()
    val mediaData: LiveData<MediaData> = _mediaData

    fun updateMediaBean(bean: MediaData) {
        _mediaData.value = null  // 先清空，确保下次设置时能触发 observer
        _mediaData.value = bean
    }

    private val _currentFileEntity = MutableLiveData<FileEntity>()

    fun setFileBean(fileEntity: FileEntity): LiveData<FileEntity> {
        _currentFileEntity.postValue(fileEntity)
        return _currentFileEntity
    }

    fun getCurrentFileBean(): FileEntity? {
        return _currentFileEntity.value
    }

    private val _videoList = MutableLiveData<List<MediaData>>()
    val videoList: LiveData<List<MediaData>> get() = _videoList

    fun searchMedia(wd: String): LiveData<List<MediaData>> {
        if (_text.value == wd.trim()) {
            _videoList.postValue(_videoList.value)
            return _videoList
        }
        if (wd.trim().isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {

            }
        }
        return _videoList
    }

}