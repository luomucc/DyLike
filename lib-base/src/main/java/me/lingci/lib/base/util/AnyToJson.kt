package me.lingci.lib.base.util

import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Array as ReflectArray
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.Date
import java.util.IdentityHashMap

object AnyToJson {

    private const val DEFAULT_MAX_DEPTH = 6

    private val fieldCache = ConcurrentHashMap<Class<*>, Array<Field>>()

    fun toString(obj: Any?): String {
        return when (obj) {
            null -> ""
            is Number -> "$obj"
            is String -> obj
            is Boolean -> "$obj"
            is me.lingci.lib.base.json.JSONObject -> obj.toJsonStr()
            is me.lingci.lib.base.json.JSONArray -> obj.toString()
            else -> try {
                val ctx = ConvertContext(0, Collections.newSetFromMap(IdentityHashMap()), DEFAULT_MAX_DEPTH)
                convertValue(obj, ctx).toString()
            } catch (e: Exception) {
                Log.d(this, "toString failed", e)
                obj.toString()
            }
        }
    }

    fun convert(obj: Any?): JSONObject {
        val ctx = ConvertContext(0, Collections.newSetFromMap(IdentityHashMap()), DEFAULT_MAX_DEPTH)
        return try {
            val result = convertValue(obj, ctx)
            result as? JSONObject ?: JSONObject().apply {
                put("value", result)
            }
        } catch (e: Exception) {
            Log.d(this, "convert failed", e)
            JSONObject().put("_error", e.message.orEmpty())
        }
    }

    fun convertToJsonString(obj: Any?, indent: Int = 0): String {
        val json = convert(obj)
        return if (indent > 0) json.toString(indent) else json.toString()
    }

    private data class ConvertContext(
        val depth: Int,
        val processedObjects: MutableSet<Any>,
        val maxDepth: Int
    )

    private fun ConvertContext.incDepth() = copy(depth = depth + 1)

    private fun convertValue(value: Any?, ctx: ConvertContext): Any? {
        if (ctx.depth > ctx.maxDepth) {
            return JSONObject().put("_depth_limit_reached", true)
        }

        return when {
            value == null -> JSONObject.NULL
            value is Number || value is Boolean || value is String -> value
            value is Enum<*> -> value.name
            value is Date -> value.time
            value is CharSequence -> value.toString()
            value is Class<*> -> value.name
            value is JSONObject -> value
            value is JSONArray -> value
            value is me.lingci.lib.base.json.JSONObject -> JSONObject(value.toJsonStr())
            value is Collection<*> -> convertCollection(value, ctx.incDepth())
            value is Map<*, *> -> convertMap(value, ctx.incDepth())
            value.javaClass.isArray -> convertArray(value, ctx.incDepth())
            else -> convertObject(value, ctx)
        }
    }

    private fun convertObject(obj: Any, ctx: ConvertContext): JSONObject {
        if (!ctx.processedObjects.add(obj)) {
            return JSONObject().put("_circular_reference", "true")
        }

        return try {
            JSONObject().apply {
                val fields = getFields(obj.javaClass)
                for (field in fields) {
                    try {
                        val fieldValue = field.get(obj)
                        put(field.name, convertValue(fieldValue, ctx.incDepth()))
                    } catch (e: Exception) {
                        // 忽略转换失败的字段
                    }
                }
            }
        } finally {
            ctx.processedObjects.remove(obj)
        }
    }

    private fun getFields(clazz: Class<*>): Array<Field> {
        return fieldCache.getOrPut(clazz) {
            val result = mutableListOf<Field>()
            var current: Class<*>? = clazz
            while (current != null && current != Any::class.java) {
                for (field in current.declaredFields) {
                    if (!Modifier.isStatic(field.modifiers) &&
                        !Modifier.isTransient(field.modifiers) &&
                        !field.isSynthetic
                    ) {
                        field.isAccessible = true
                        result.add(field)
                    }
                }
                current = current.superclass
            }
            result.toTypedArray()
        }
    }

    private fun convertCollection(collection: Collection<*>, ctx: ConvertContext): JSONArray {
        return JSONArray().apply {
            for (item in collection) {
                put(convertValue(item, ctx))
            }
        }
    }

    private fun convertMap(map: Map<*, *>, ctx: ConvertContext): JSONObject {
        return JSONObject().apply {
            for ((key, value) in map) {
                put(key?.toString() ?: "null", convertValue(value, ctx))
            }
        }
    }

    private fun convertArray(array: Any, ctx: ConvertContext): JSONArray {
        return JSONArray().apply {
            val length = ReflectArray.getLength(array)
            for (i in 0 until length) {
                put(convertValue(ReflectArray.get(array, i), ctx))
            }
        }
    }

}
