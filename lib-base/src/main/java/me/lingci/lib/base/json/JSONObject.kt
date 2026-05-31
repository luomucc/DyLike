package me.lingci.lib.base.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import me.lingci.lib.base.util.Log

/**
 *   @author : happyc
 *   time    : 2023/07/19
 *   desc    :
 *   version : 1.0
 */
class JSONObject {

    private var jsonObject: JsonObject

    constructor() {
        this.jsonObject = JsonObject(mapOf())
    }

    constructor(json: String) {
        this.jsonObject = try {
            Json.parseToJsonElement(json).jsonObject
        } catch (_: Exception) {
            JsonObject(mapOf())
        }
    }

    constructor(jsonObject: JsonObject) {
        this.jsonObject = jsonObject
    }

    constructor(map: Map<*, *>) {
        this.jsonObject = JsonObject(mapOf())
        map.forEach {
            putValue(it.key.toString(), it.value.toString())
        }
    }

    fun original(): JsonObject {
        return jsonObject
    }

    fun hasKey(key: String): Boolean {
        return jsonObject.containsKey(key)
    }

    fun noHasKey(key: String): Boolean {
        return !hasKey(key)
    }

    fun isObject(key: String): Boolean {
        return try {
            jsonObject[key] is JsonPrimitive
        } catch (e: Exception) {
            false
        }
    }

    fun isJsonObject(key: String): Boolean {
        return try {
            jsonObject[key] is JsonObject
        } catch (e: Exception) {
            false
        }
    }

    fun isJsonArray(key: String): Boolean {
        return try {
            jsonObject[key] is JsonArray
        } catch (e: Exception) {
            false
        }
    }

    fun get(key: String): JsonElement {
        return jsonObject[key]!!
    }

    fun getJSONObject(key: String): JSONObject {
        return try {
            if (jsonObject[key] is JsonObject) {
                return JSONObject(jsonObject[key]!!.jsonObject)
            }
            JSONObject(jsonObject[key]!!.jsonPrimitive.content)
        } catch (e: Exception) {
            JSONObject()
        }
    }

    fun getJSONArray(key: String): JSONArray {
        return try {
            JSONArray(jsonObject[key]!!.jsonArray)
        } catch (e: Exception) {
            JSONArray()
        }
    }

    fun getString(key: String): String {
        return getString(key, "")
    }

    fun getString(key: String, default: String): String {
        return try {
            jsonObject[key]!!.jsonPrimitive.content
        } catch (e: Exception) {
            default
        }
    }

    fun getInt(key: String): Int {
        return getInt(key, -1)
    }

    fun getInt(key: String, default: Int): Int {
        return try {
            jsonObject[key]!!.jsonPrimitive.int
        } catch (e: Exception) {
            default
        }
    }

    fun getLong(key: String): Long {
        return getLong(key, -1L)
    }

    fun getLong(key: String, default: Long): Long {
        return try {
            jsonObject[key]!!.jsonPrimitive.long
        } catch (e: Exception) {
            default
        }
    }

    fun getFloat(key: String): Float {
        return getFloat(key, -1.0f)
    }

    fun getFloat(key: String, default: Float): Float {
        return try {
            jsonObject[key]!!.jsonPrimitive.float
        } catch (e: Exception) {
            default
        }
    }

    fun getDouble(key: String): Double {
        return getDouble(key, -1.0)
    }

    fun getDouble(key: String, default: Double): Double {
        return try {
            jsonObject[key]!!.jsonPrimitive.double
        } catch (e: Exception) {
            default
        }
    }

    fun getBoolean(key: String): Boolean {
        return getBoolean(key, false)
    }

    fun getBoolean(key: String, default: Boolean): Boolean {
        return try {
            jsonObject[key]!!.jsonPrimitive.boolean
        } catch (e: Exception) {
            default
        }
    }

    fun putValue(key: String, value: Any) {
        jsonObject = buildJsonObject {
            jsonObject.forEach {
                put(it.key, it.value)
            }
            when (value) {
                is Number -> put(key, JsonPrimitive(value))
                is Boolean -> put(key, JsonPrimitive(value))
                is String -> put(key, JsonPrimitive(value))
                is JsonElement -> put(key, value)
                is JSONObject -> put(key, value.original())
                is JSONArray -> put(key, value.original())
                else -> Log.d(this, "类型不满足")
            }
        }
    }

    fun keys(): Set<String> {
        return jsonObject.keys
    }

    fun isEmpty(): Boolean {
        return jsonObject.isEmpty()
    }

    fun isNotEmpty(): Boolean {
        return jsonObject.isNotEmpty()
    }

    fun toJsonStr(): String {
        return jsonObject.toString()
        //return "{${jsonObject.entries.joinToString(",") { "\"${it.key}\":${it.value}" }}}"
    }

    override fun toString(): String {
        return toJsonStr()
    }

}