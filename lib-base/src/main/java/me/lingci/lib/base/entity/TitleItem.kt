package me.lingci.lib.base.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 *   @author : happyc
 *   time    : 2024/09/21
 *   desc    :
 *   version : 1.0
 */
@OptIn(InternalSerializationApi::class)
@Serializable
@Parcelize
data class TitleItem(

    var title: String = "",
    var name: String = "",
    var selected: Boolean = false

) : Parcelable {

}