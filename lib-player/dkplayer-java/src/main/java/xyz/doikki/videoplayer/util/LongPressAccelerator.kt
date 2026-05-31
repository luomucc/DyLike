package xyz.doikki.videoplayer.util

import xyz.doikki.videoplayer.controller.ControlWrapper

/**
 * Created by tiiime on 2022/7/27.
 * 长按倍速播放器
 */

class LongPressAccelerator(
    private val controlWrapper: ControlWrapper,
    private val onStart: () -> Unit,
    private val onStop: () -> Unit
) {
    private var originSpeed: Float = 1F
    private var isEnable = false

    fun enable() {
        // 已暂停
        if (controlWrapper.isPlaying.not()) {
            return
        }
        // 已锁屏
        if (controlWrapper.isLocked) {
            return
        }
        if (isEnable) {
            return
        }
        isEnable = true
        L.d("LongPressAccelerator enable")

        originSpeed = controlWrapper.speed
        onStart.invoke()
    }

    fun disable() {
        if (!isEnable) {
            return
        }
        isEnable = false
        L.d("LongPressAccelerator disable")

        controlWrapper.speed = originSpeed
        onStop.invoke()
    }

}