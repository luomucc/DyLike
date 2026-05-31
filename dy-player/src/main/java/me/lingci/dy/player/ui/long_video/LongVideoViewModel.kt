package me.lingci.dy.player.ui.long_video

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.lingci.dy.player.entity.MediaData
import me.lingci.dy.player.entity.VideoData

/**
 *   @author : happyc
 *   time    : 2024/09/26
 *   desc    :
 *   version : 1.0
 */
class LongVideoViewModel : ViewModel() {

    private val _initData = MutableLiveData<Boolean>()
    val initData: LiveData<Boolean> = _initData
    private var mergeState: Boolean = false
    private val dataList: MutableList<VideoData> = mutableListOf()
    private var mediaData: MediaData? = null

    fun addAll(list: List<VideoData>) {
        dataList.clear()
        _initData.postValue(false)
        dataList.addAll(list)
        _initData.postValue(true)
    }

    fun getItem(position: Int): VideoData {
        return dataList[position]
    }

    fun getData(): MutableList<VideoData> {
        return dataList
    }

    fun getItemSize(): Int {
        return dataList.size
    }

    fun setMediaData(mediaData: MediaData) {
        this.mediaData = mediaData
    }

    fun getMediaData(): MediaData? {
        return mediaData
    }

    fun hasMediaData(): Boolean {
        return mediaData != null
    }

    fun setMergeState(merge: Boolean) {
        this.mergeState = merge
    }

    fun isMerge(): Boolean {
        return mergeState
    }

}