package me.lingci.dy.player.ui.media

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.lingci.dy.player.entity.MediaData

class MediaViewModel : ViewModel() {

    private val _text = MutableLiveData<String>()
    val text: LiveData<String> = _text

    fun setText(value: String) {
        _text.postValue(value)
    }

    private val mediaList = mutableListOf<MediaData>()

    fun setMediaList(list: List<MediaData>) {
        mediaList.clear()
        mediaList.addAll(list)
    }

    fun listMedia(keyword: String = ""): MutableList<MediaData> {
        return if (keyword.isBlank()) {
            mediaList
        } else {
            mediaList.filter { it.title.contains(keyword) }.toMutableList()
        }
    }

}
