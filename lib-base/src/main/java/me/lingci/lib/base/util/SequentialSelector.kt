package me.lingci.lib.base.util

import kotlin.random.Random

class SequentialSelector<T>(items: List<T>, seed: Long) : ISelector<T> {

    private val random = Random(seed)
    private val itemList = items.toMutableList()
    private var currentIndex = 0

    init {
        shuffle()
    }

    private fun shuffle() {
        itemList.shuffle(random)
        currentIndex = 0
    }

    override fun select(): T {
        if (itemList.isEmpty()) {
            throw IllegalStateException("Cannot select from an empty list")
        }
        val item = itemList[currentIndex]
        currentIndex++
        if (currentIndex >= itemList.size) {
            shuffle()
        }
        return item
    }

}