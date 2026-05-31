package me.lingci.lib.dm.view.entity.xml

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 *   @author : happyc
 *   time    : 2023/07/17
 *   desc    :
 *   version : 1.0
 */
@OptIn(InternalSerializationApi::class)
@Serializable
@Parcelize
data class DmItem(

    var content: String = "",
    var style: String = "",
    var extend: String? = ""

): Parcelable  {

    constructor(style: String, content: String) : this(
        content = content,
        style = style,
        extend = ""
    )

    val extendStyle: String
        get() = if (extend == null) "" else extend!!

    val time: Double
        get() = style.substringBefore(",").toDouble()

    val rid: String
        get() = try {
            style.split(",")[7]
        } catch (_: Exception) {
            "0"
        }

}