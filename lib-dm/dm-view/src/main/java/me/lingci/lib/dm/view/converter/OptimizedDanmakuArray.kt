package me.lingci.lib.dm.view.converter


import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicIntegerArray

class OptimizedDanmakuArray(
    private var resolutionX: Int = 1920,
    private var resolutionY: Int = 1080,
    private var fontSize: Int = 38
) {
    private val rows: Int = resolutionY / fontSize
    private val timeArray = AtomicIntegerArray(rows) // 使用原子数组提高并发性能
    private val lengthArray = AtomicIntegerArray(rows)

    // 使用分段锁提高并发性
    private val segments = 16
    private val locks = Array(segments) { Any() }

    fun setTimeLength(row: Int, time: Float, length: Int) {
        if (row in 0 until rows) {
            val segment = row % segments
            synchronized(locks[segment]) {
                timeArray.set(row, (time * 1000).toInt()) // 使用整数存储提高性能
                lengthArray.set(row, length)
            }
        }
    }

    fun getTime(row: Int): Float {
        return if (row in 0 until rows) {
            timeArray.get(row) / 1000f
        } else {
            -1f
        }
    }

    fun getLength(row: Int): Int {
        return if (row in 0 until rows) {
            lengthArray.get(row)
        } else {
            0
        }
    }

    fun getRows(): Int = rows

}