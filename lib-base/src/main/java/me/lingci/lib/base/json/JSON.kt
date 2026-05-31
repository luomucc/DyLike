package me.lingci.lib.base.json

/**
 *   @author : happyc
 *   time    : 2023/07/19
 *   desc    :
 *   version : 1.0
 */
object JSON {

    @JvmStatic
    fun parseObject(json: String): JSONObject {
        return JSONObject(json)
    }

    @JvmStatic
    fun parseArray(json: String): JSONArray {
        return JSONArray(json)
    }

}

fun String.toJSONObject(): JSONObject {
    return JSONObject(this)
}

fun String.toJSONArray(): JSONArray {
    return JSONArray(this)
}