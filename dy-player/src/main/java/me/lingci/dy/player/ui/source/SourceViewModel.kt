package me.lingci.dy.player.ui.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SourceViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = ""
    }

    val text: LiveData<String> = _text

}