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
data class DmData(

    var chatServer: String = "chat.bilibili.com",
    var mission: String = "0",
    var maxLimit: String = "8000",
    var chatId: String = "",
    var maxCount: Int = 0,
    var source: String = "ZhXs",
    var list: List<DmItem> = emptyList()

): Parcelable  {

    constructor(
        cid: String,
        data: List<DmItem>
    ) : this(
        chatId = cid,
        maxCount = data.size,
        list = data
    )

    constructor(
        cid: String,
        count: Int,
        data: List<DmItem>
    ) : this(
        chatId = cid,
        maxCount = count,
        list = data
    )

    constructor(
        cid: String,
        server: String,
        data: List<DmItem>
    ) : this(
        chatId = cid,
        chatServer = server,
        maxCount = data.size,
        list = data
    )

}