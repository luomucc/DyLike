package me.lingci.lib.dm.view.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import me.lingci.lib.base.json.JSON
import me.lingci.lib.base.json.JSONObject
import me.lingci.lib.base.util.logD
import me.lingci.lib.base.util.toRgbDecimals
import me.lingci.lib.dm.view.entity.xml.DmData
import me.lingci.lib.dm.view.entity.xml.DmItem

/**
 *   @author : happyc
 *   time    : 2025/02/09
 *   desc    :
 *   version : 1.0
 */
object JsonConverter {

    private val apiJsonFilter = listOf("条弹幕", "换源", "弹幕礼仪", "列队来袭", "欢迎使用", "官方弹幕库")

    fun apiJsonToXml(json: String): Pair<String, List<DmItem>> {
        val jsonObject = JSON.parseObject(json)
        val name = jsonObject.getString("name")
        jsonObject.getJSONArray("danmuku").let {jsonArray ->
            val  dmList = arrayListOf<DmItem>()
            for (i in 0 until  jsonArray.size()) {
                val dms = jsonArray.getStringArray(i)
                if (dms.isNotEmpty() && !(dms[0] == "0" && dms[4].contains(""))) {
                    if (dms[0].toDouble() < 10 && apiJsonFilter.any { dms[4].contains(it) }) {
                        continue
                    }
                    var color = 16777215
                    try {
                        color = dms[2].toRgbDecimals()
                    } catch (_: Exception) { }
                    var type = 1
                    try {
                        type = when(dms[1]) {
                            "right" -> 1
                            "top" -> 5
                            else -> 1
                        }
                    }catch (_: Exception) { }
                    dmList.add(DmItem(style = "${dms[0]},$type,25,$color,0,0,0,0", content = dms[4]))
                }
            }
            return Pair(name, dmList)
        }
    }

    fun xmlToNipaJson(dmData: DmData) : String {
        val dmJson = JSONObject()
        dmJson.putValue("timestamp", System.currentTimeMillis())
        dmJson.putValue("animeId", -1)
        dmData.list.map { item ->
            val split = item.style.split(",")
            try {
                JSONObject().apply {
                    putValue("time", split[0].toDouble())
                    putValue("content", item.content)
                    putValue("type", when(split[2]) {
                        "4" -> "bottom"
                        "5" -> "top"
                        else -> "scroll"
                    })
                    putValue("color", toRgb(split[3]))
                    putValue("isMe", false)
                }.original()
            } catch (e: Exception) {
                JsonObject(mapOf())
            }
        }.filter { it.isNotEmpty() }.let {
            logD("comments ${it.size}")
            dmJson.putValue("comments", JsonArray(it))
        }
        return dmJson.toJsonStr()
    }

    private fun toRgb(color: String): String {
        return try {
            val decimal = color.toInt()
            // 检查值是否在有效范围（0 到 0xFFFFFF，即 16777215）
            if (decimal < 0 || decimal > 0xFFFFFF) {
                return "rgb(255,255,255)"
            }
            // 解析 R、G、B 分量（通过位运算提取）
            val red = (decimal shr 16) and 0xFF   // 右移16位，取高8位（R）
            val green = (decimal shr 8) and 0xFF  // 右移8位，取中8位（G）
            val blue = decimal and 0xFF           // 取低8位（B）
            "rgb(${red},${green},${blue})"
        } catch (e: Exception) {
            "rgb(255,255,255)"
        }
    }

}