package me.lingci.lib.base.util

import kotlin.math.max

object DensityCalculator {

    /**
     * 计算密度点
     * @param timestamps 弹幕时间戳列表
     * @param videoDurationMs 视频总时长
     * @param segmentDurationMs 每个统计段的时长（例如 5000ms = 5秒）
     * @return FloatArray 每个段的归一化高度
     */
    fun calculateDensity(
        timestamps: List<Long>,
        videoDurationMs: Long,
        segmentDurationMs: Long = 5000 // 默认5秒一个统计点
    ): FloatArray {
        if (videoDurationMs <= 0 || timestamps.isEmpty() || segmentDurationMs <= 0) {
            return floatArrayOf()
        }

        // 1. 计算总共有多少个“段”
        val totalSegments = ((videoDurationMs + segmentDurationMs - 1) / segmentDurationMs).toInt()

        // 2. 初始化每个段的计数器
        val countPerSegment = IntArray(totalSegments) { 0 }

        // 3. 遍历弹幕，计算每条弹幕属于哪个段
        for (time in timestamps) {
            if (time !in 0..<videoDurationMs) continue
            val segmentIndex = (time / segmentDurationMs).toInt()
            if (segmentIndex < totalSegments) {
                countPerSegment[segmentIndex]++
            }
        }

        // 4. 归一化数据 (0-1)
        val maxCount = countPerSegment.maxOrNull() ?: 1
        return countPerSegment.map {
            it.toFloat() / max(maxCount, 1)
        }.toFloatArray()
    }

}