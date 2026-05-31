// DanmakuData.kt
package me.lingci.lib.dm.view.converter

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 * 数据模型类
 */
@OptIn(InternalSerializationApi::class)
@Parcelize
@Serializable
data class AssConfig(
    var fontName: String = "Microsoft YaHei",
    var fontSize: Int = 40,
    var superChatFontSize: Int = 40,
    var resolutionX: Int = 1920,
    var resolutionY: Int = 1080,
    var displayArea: Float = 1.0f,
    var rollTime: Int = 12,
    var fixTime: Int = 5,
    var opacity: Float = 0.8f,
    var bold: Boolean = false,
    var outline: Float = 1.0f,
    var shadow: Float = 0.0f
): Parcelable

data class DanmakuItem(
    var appearTime: Float,
    var type: Int,
    var color: Long,
    var text: String,
    var user: String? = null
) {
    constructor() :this(appearTime = 0f, type = 0, color = 0, text = "", user = ""){}
}

data class SuperChatItem(
    var appearTime: Float,
    var disappearTime: Float,
    var userName: String,
    var price: Int,
    var message: String
)

data class GiftItem(
    var appearTime: Float,
    var overTime: Float,
    var user: String,
    var name: String,
    var count: Int,
    var price: Int
)