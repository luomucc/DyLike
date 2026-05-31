package me.lingci.dy.player.ui.media_full

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.lingci.dy.player.entity.MediaData

class MediaFullViewModel : ViewModel() {

    private val _text = MutableLiveData<String>()
    val text: LiveData<String> = _text

    private val mediaList = mutableListOf<MediaData>()

    fun setMediaList(list: List<MediaData>) {
        mediaList.clear()
        mediaList.addAll(list)
    }

    fun listMedia(search: String = ""): MutableList<MediaData> {
        return if (search.isBlank()) {
            mediaList
        } else {
            mediaList.filter {
                item -> item.title.contains(search, ignoreCase = true)
            }.toMutableList()
        }
    }

    fun removeItem(position: Int) {
        try {
            mediaList.removeAt(position)
        } catch (_: Exception) {}
    }

    fun addItem(media: MediaData) {
        mediaList.add(media)
    }

    fun removeList(listSelect: List<MediaData>) {
        mediaList.removeAll(listSelect)
    }

}