package me.lingci.lib.player.util

object SurfaceRenderTrace {
    const val DEFAULT_LOG_CAPACITY = 50

    @Volatile
    @JvmStatic
    var enabled: Boolean = false

    @Volatile
    @JvmStatic
    var sink: ((tag: String, level: String, message: String) -> Unit)? = null

    @JvmStatic
    fun d(tag: String, message: String) {
        if (enabled) sink?.invoke(tag, "D", message)
    }

    @JvmStatic
    fun e(tag: String, message: String) {
        if (enabled) sink?.invoke(tag, "E", message)
    }
}