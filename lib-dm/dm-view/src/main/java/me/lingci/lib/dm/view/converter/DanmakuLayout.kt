// DanmakuLayout.kt
package me.lingci.lib.dm.view.converter

/**
 * 布局算法
 */
object DanmakuLayout {

    // 从右到左滚动弹幕算法
    fun getPositionYForR2L(
        fontSize: Int,
        appearTime: Float,
        textLength: Int,
        resolutionX: Int,
        rollTime: Int,
        array: DanmakuArray
    ): Int? {
        val velocity = (textLength + resolutionX) / rollTime.toFloat()
        // 使用 -1 表示未找到可用行，避免 bestRow==0 时被误判为未初始化
        var bestRow = -1
        var bestBias = Float.NEGATIVE_INFINITY

        for (i in 0 until array.getRows()) {
            val previousAppearTime = array.getTime(i)
            if (previousAppearTime < 0) {
                array.setTimeLength(i, appearTime, textLength)
                return 1 + i * fontSize
            }

            val previousLength = array.getLength(i)
            val previousVelocity = (previousLength + resolutionX) / rollTime.toFloat()
            val deltaVelocity = velocity - previousVelocity

            val deltaX = (appearTime - previousAppearTime) * previousVelocity -
                    (previousLength + textLength) / 2.0f

            if (deltaX < 0) continue

            if (deltaVelocity <= 0) {
                array.setTimeLength(i, appearTime, textLength)
                return 1 + i * fontSize
            }

            val deltaTime = deltaX / deltaVelocity
            val bias = appearTime - previousAppearTime - deltaTime
            val tCatch = previousAppearTime + deltaTime
            val distancePrev = previousVelocity * (tCatch - previousAppearTime)

            if (distancePrev > resolutionX) {
                array.setTimeLength(i, appearTime, textLength)
                return 1 + i * fontSize
            }

            if (bias > 0) {
                array.setTimeLength(i, appearTime, textLength)
                return 1 + i * fontSize
            } else if (bestRow < 0 || bias > bestBias) {
                bestBias = bias
                bestRow = i
            }
        }

        if (bestRow >= 0) {
            array.setTimeLength(bestRow, appearTime, textLength)
            return 1 + bestRow * fontSize
        }

        return null
    }

    // 顶部/底部固定弹幕算法
    fun getFixedY(
        fontSize: Int,
        appearTime: Float,
        resolutionY: Int,
        fixTime: Int,
        array: DanmakuArray,
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
}