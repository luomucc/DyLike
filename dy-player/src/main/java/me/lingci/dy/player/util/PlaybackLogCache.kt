package me.lingci.dy.player.util

import me.lingci.lib.player.util.SurfaceRenderTrace

class PlaybackLogCache(private val capacity: Int = DEFAULT_CAPACITY) {

    companion object {
        const val DEFAULT_CAPACITY = SurfaceRenderTrace.DEFAULT_LOG_CAPACITY
    }

    data class LogEntry(
        val timestamp: Long,
        val tag: String,
        val level: String,
        val message: String
    )

    private val buffer = ArrayDeque<LogEntry>(capacity + 1)

    @Synchronized
    fun add(tag: String, level: String, message: String) {
        if (buffer.size >= capacity) {
            buffer.removeFirst()
        }
        buffer.addLast(LogEntry(System.currentTimeMillis(), tag, level, message))
    }

    @Synchronized
    fun getEntries(): List<LogEntry> = buffer.toList()

    @Synchronized
    fun clear() {
        buffer.clear()
    }
}
