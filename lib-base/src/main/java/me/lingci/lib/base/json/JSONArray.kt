package me.lingci.lib.base.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.lingci.lib.base.util.Log


/**
 *   @author : happyc
 *   time    : 2023/07/19
 *   desc    :
 *   version : 1.0
 */
class JSONArray {

    private var jsonArray: JsonArray

    constructor() {
        this.jsonArray = JsonArray(listOf())
    }

    constructor(json: String) {
        this.jsonArray = try {
            Json.parseToJsonElement(json).jsonArray
        } catch (e: Exception) {
            Log.d(this, "init failed", e.message)
            JsonArray(listOf())
        }
    }

    constructor(jsonArray: JsonArray) {
        this.jsonArray = jsonArray
    }

    constructor(list: List<JsonElement>) {
        this.jsonArray = JsonArray(list)
    }

    fun original(): JsonArray {
        return jsonArray
    }

    fun itemIsArray(index: Int): Boolean {
        if (index > jsonArray.size) return false
        return jsonArray[index] is JsonArray
    }

    fun itemIsObj(index: Int): Boolean {
        if (index > jsonArray.size) return false
        return jsonArray[index] is JsonObject
    }

    fun firstJSONObject(): JSONObject {
        return try {
            JSONObject(jsonArray.first().jsonObject)
        } catch (e : Exception) {
            Log.d(this, "first object failed", e.message)
            JSONObject()
        }
    }

    fun getJSONObject(index: Int): JSONObject {
        return try {
            JSONObject(jsonArray[index].jsonObject)
        } catch (e: Exception) {
            Log.d(this, "get object failed", e.message)
            JSONObject()
        }
    }

    fun getJSONArray(index: Int): JSONArray {
        return try {
            JSONArray(jsonArray[index].jsonArray)
        } catch (e: Exception) {
            Log.d(this, "first array failed", e.message)
            JSONArray()
        }
    }

    fun getStringArray(index: Int): List<String> {
        return try {
            jsonArray[index].jsonArray.map { it.jsonPrimitive.content }
        } catch (e: Exception) {
            Log.d(this, "get string array failed", e.message)
            emptyList()
        }
    }

    fun getString(index: Int) : String {
        return try {
            jsonArray[index].jsonPrimitive.content
        } catch (e: Exception) {
            ""
        }
    }

    fun size(): Int {
        return jsonArray.size
    }

    fun length(): Int {
        return size()
    }

    override fun toString(): String {
        return jsonArray.toString()
    }

    fun isEmpty(): Boolean {
        return jsonArray.isEmpty()
    }

    fun isNotEmpty(): Boolean {
        return jsonArray.isNotEmpty()
    }

}

fun JSONArray.forEach(action: (JSONObject) -> Unit) {
    original().forEach { action.invoke(JSONObject(it.jsonObject)) }
}

fun JSONArray.forEach2(item: (jsonObject: JSONObject) -> Unit) {
    for (i in 0 until this.size()) {
        if(this.itemIsObj(i)) {
            item(this.getJSONObject(i))
        }
    }
}