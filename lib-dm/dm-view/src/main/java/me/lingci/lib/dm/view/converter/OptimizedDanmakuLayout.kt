package me.lingci.lib.dm.view.converter

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min

object OptimizedDanmakuLayout {

    // 使用二分查找优化布局算法
    suspend fun getPositionYForR2LOptimized(
        fontSize: Int,
        appearTime: Float,
        textLength: Int,
        resolutionX: Int,
        rollTime: Int,
        array: OptimizedDanmakuArray
    ): Int? = withContext(Dispatchers.IO) {
        val velocity = (textLength + resolutionX) / rollTime.toFloat()
        val rows = array.getRows()

        // 并行搜索可用行
        val chunkSize = rows / Runtime.getRuntime().availableProcessors()
        val deferredResults = (0 until rows step chunkSize).map { start ->
            async(Dispatchers.Default) {
                val end = min(start + chunkSize, rows)
                findBestRow(
                    start,
                    end,
                    appearTime,
                    velocity,
                    textLength,
                    rollTime,
                    resolutionX,
                    array
                )
            }
        }

        val results = deferredResults.awaitAll().filterNotNull()
        if (results.isEmpty()) return@withContext null

        val bestRow = results.minByOrNull { it.bias }?.row ?: return@withContext null
        array.setTimeLength(bestRow, appearTime, textLength)
        1 + bestRow * fontSize
    }


    // 顶部/底部固定弹幕算法
    fun getFixedY(
        fontSize: Int,
        appearTime: Float,
        resolutionY: Int,
        fixTime: Int,
        array: OptimizedDanmakuArray,
        fromTop: Boolean = true
    ): Int? {
        var bestRow = 0
        var bestBias = -1f

        val rowRange = if (fromTop) {
            1..array.getRows()
        } else {
            array.getRows() downTo 1
        }

        for (i in rowRange) {
            val rowIndex = i - 1
            val previousAppearTime = array.getTime(rowIndex)
            val deltaTime = appearTime - previousAppearTime

            if (previousAppearTime < 0 || deltaTime > fixTime) {
                array.setTimeLength(rowIndex, appearTime, 0)
                return if (fromTop) {
                    rowIndex * fontSize + 1
                } else {
                    resolutionY - fontSize * (array.getRows() - rowIndex) + 1
                }
            } else if (deltaTime > bestBias) {
                bestBias = deltaTime
                bestRow = rowIndex
            }
        }

        return null
    }

    private fun findBestRow(
        start: Int,
        end: Int,
        appearTime: Float,
        velocity: Float,
        textLength: Int,
        rollTime: Int,
        resolutionX: Int,
        array: OptimizedDanmakuArray
    ): RowResult? {
        var bestRow = -1
        var bestBias = Float.MAX_VALUE

        for (i in start until end) {
            val previousAppearTime = array.getTime(i)
            if (previousAppearTime < 0) {
                return RowResult(i, 0f)
            }

            val previousLength = array.getLength(i)
            val previousVelocity = (previousLength + resolutionX) / rollTime.toFloat()
            val deltaVelocity = velocity - previousVelocity

            val deltaX = (appearTime - previousAppearTime) * previousVelocity -
                    (previousLength + textLength) / 2.0f

            if (deltaX < 0) continue

            if (deltaVelocity <= 0) {
                return RowResult(i, 0f)
            }

            val deltaTime = deltaX / deltaVelocity
            val bias = appearTime - previousAppearTime - deltaTime

            if (bias > 0 && bias < bestBias) {
                bestBias = bias
                bestRow = i
            }
        }

        return if (bestRow >= 0) RowResult(bestRow, bestBias) else null
    }

    private data class RowResult(val row: Int, val bias: Float)

}