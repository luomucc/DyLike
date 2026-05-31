package me.lingci.lib.base.util

import kotlin.random.Random

/**
 *   @author : DouBao
 *   time    : 2025/03/19
 *   desc    : 权重随机选择器
 *   version : 1.0
 */
// 定义接口
interface ISelector<T> {
    fun select(): T
}

// 定义一个类来存储元素、权重和累积权重
data class WeightedItem<T>(val item: T, val weight: Int, var cumulativeWeight: Int = 0)

// 定义一个类来管理带权重的随机选择
class WeightedRandomSelector<T>(items: List<Pair<T, Int>>) : ISelector<T> {

    private lateinit var weightedItems: List<WeightedItem<T>>
    private var totalWeight: Int = 0

    init {
        updateItems(items)
    }

    fun updateItems(items: List<Pair<T, Int>>) {
        var currentTotal = 0
        weightedItems = items.map { (item, weight) ->
            currentTotal += weight
            WeightedItem(item, weight, currentTotal)
        }
        totalWeight = currentTotal
    }

    // 从带权重的元素集合中随机选择一个元素
    override fun select(): T {
        val randomValue = Random.nextInt(totalWeight)
        // 使用二分查找来找到合适的元素
        var left = 0
        var right = weightedItems.size - 1
        while (left < right) {
            val mid = left + (right - left) / 2
            if (randomValue >= weightedItems[mid].cumulativeWeight) {
                left = mid + 1
            } else {
                right = mid
            }
        }
        return weightedItems[left].item
    }

}