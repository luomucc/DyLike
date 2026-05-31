// DanmakuArray.kt
package me.lingci.lib.dm.view.converter

/**
 * 弹幕数组管理器
 */
class DanmakuArray(
    private var resolutionX: Int = 1920,
    private var resolutionY: Int = 1080,
    private var fontSize: Int = 38
) {
    private val rows: Int = resolutionY / fontSize
    private val timeLengthArray: Array<Pair<Float, Int>> = Array(rows) { -1f to 0 }
    
    fun setTimeLength(row: Int, time: Float, length: Int) {
        if (row in 0 until rows) {
            timeLengthArray[row] = time to length
        } else {
            throw IndexOutOfBoundsException("Array index out of range")
        }
    }
    
    fun getTime(row: Int): Float {
        if (row in 0 until rows) {
            return timeLengthArray[row].first
        }
        throw IndexOutOfBoundsException("Array index out of range")
    }
    
    fun getLength(row: Int): Int {
        if (row in 0 until rows) {
            return timeLengthArray[row].second
        }
        throw IndexOutOfBoundsException("Array index out of range")
    }
    
    fun getRows(): Int = rows

}