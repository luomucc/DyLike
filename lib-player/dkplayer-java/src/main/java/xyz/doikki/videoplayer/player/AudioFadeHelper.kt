package xyz.doikki.videoplayer.player

import android.animation.ValueAnimator

/**
 * 音频渐入渐出辅助类
 * 在视频开播时渐入音量，在视频接近结束时渐出音量
 */
class AudioFadeHelper(private val videoView: BaseVideoView<*>) {

    companion object {
        private const val TAG = "AudioFadeHelper"
    }

    // 是否启用音频渐变
    var isEnabled: Boolean = false

    // 渐入时长(ms)
    var fadeInDuration: Int = 1000

    // 渐出时长(ms)
    var fadeOutDuration: Int = 1000

    // 渐变检查间隔(ms)
    private val fadeCheckInterval = 200L

    private var fadeInAnimator: ValueAnimator? = null
    private var fadeOutAnimator: ValueAnimator? = null

    // 是否正在渐出中
    private var isFadingOut: Boolean = false

    // 渐出是否已完成（音量已降到0）
    private var fadeOutCompleted: Boolean = false

    // 进度检查Runnable
    private val fadeCheckRunnable = Runnable { checkFadeOutByProgress() }

    /**
     * 开始渐入：音量从0渐变到1
     */
    fun startFadeIn() {
        if (!isEnabled || videoView.isMute) return

        cancelFadeOut()
        isFadingOut = false
        fadeOutCompleted = false

        fadeInAnimator?.cancel()
        fadeInAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = fadeInDuration.toLong()
            addUpdateListener { animator ->
                val volume = animator.animatedValue as Float
                // 渐入期间如果被静音，立即取消
                if (videoView.isMute) {
                    cancel()
                    return@addUpdateListener
                }
                // 渐入期间如果已经开始渐出，取消渐入
                if (isFadingOut) {
                    cancel()
                    return@addUpdateListener
                }
                videoView.setVolume(volume, volume)
            }
            start()
        }

        startFadeCheck()
    }

    /**
     * 开始渐出：音量从当前值渐变到0
     */
    fun startFadeOut() {
        if (!isEnabled || videoView.isMute || isFadingOut) return

        isFadingOut = true
        cancelFadeIn()

        // 获取当前音量作为渐出起始值
        val startVolume = getCurrentFadeVolume()

        fadeOutAnimator?.cancel()
        fadeOutAnimator = ValueAnimator.ofFloat(startVolume, 0f).apply {
            duration = fadeOutDuration.toLong()
            addUpdateListener { animator ->
                val volume = animator.animatedValue as Float
                if (videoView.isMute) {
                    cancel()
                    return@addUpdateListener
                }
                videoView.setVolume(volume, volume)
            }
            start()
        }

        stopFadeCheck()
    }

    /**
     * 通过播放进度检查是否需要开始渐出
     */
    private fun checkFadeOutByProgress() {
        if (!isEnabled || isFadingOut || fadeOutCompleted) return

        val duration = videoView.duration
        val position = videoView.currentPosition

        // 循环播放不执行渐出
        if (videoView.isLooping) {
            postFadeCheck()
            return
        }

        // 视频时长无效或太短，不执行渐出
        if (duration <= 0 || duration < fadeOutDuration * 2) {
            postFadeCheck()
            return
        }

        val remaining = duration - position
        if (remaining <= fadeOutDuration && remaining > 0) {
            startFadeOut()
        } else {
            postFadeCheck()
        }
    }

    /**
     * 获取当前渐变中的音量值
     */
    private fun getCurrentFadeVolume(): Float {
        fadeOutAnimator?.let {
            if (it.isRunning) return it.animatedValue as Float
        }
        fadeInAnimator?.let {
            if (it.isRunning) return it.animatedValue as Float
        }
        return 1f
    }

    /**
     * 开始定期检查渐出时机
     */
    private fun startFadeCheck() {
        stopFadeCheck()
        videoView.postDelayed(fadeCheckRunnable, fadeCheckInterval)
    }

    /**
     * 停止定期检查
     */
    private fun stopFadeCheck() {
        videoView.removeCallbacks(fadeCheckRunnable)
    }

    /**
     * 继续定期检查
     */
    private fun postFadeCheck() {
        videoView.removeCallbacks(fadeCheckRunnable)
        videoView.postDelayed(fadeCheckRunnable, fadeCheckInterval)
    }

    /**
     * 取消渐入
     */
    private fun cancelFadeIn() {
        fadeInAnimator?.cancel()
        fadeInAnimator = null
    }

    /**
     * 取消渐出
     */
    private fun cancelFadeOut() {
        fadeOutAnimator?.cancel()
        fadeOutAnimator = null
        isFadingOut = false
    }

    /**
     * 取消所有渐变动画和检查
     */
    fun cancel() {
        cancelFadeIn()
        cancelFadeOut()
        fadeOutCompleted = false
        stopFadeCheck()
    }

    /**
     * 暂停渐变（播放暂停时调用）
     */
    fun pause() {
        fadeInAnimator?.pause()
        fadeOutAnimator?.pause()
        stopFadeCheck()
    }

    /**
     * 恢复渐变（播放恢复时调用）
     */
    fun resume() {
        fadeInAnimator?.resume()
        fadeOutAnimator?.resume()
        if (isEnabled && !isFadingOut && !fadeOutCompleted) {
            startFadeCheck()
        }
    }

    /**
     * Seek时重置渐出状态，重新开始检查
     */
    fun onSeekTo() {
        if (!isEnabled) return
        cancelFadeOut()
        fadeOutCompleted = false
        // 如果音量被渐出降低了，渐入恢复
        if (!videoView.isMute) {
            val currentVolume = getCurrentFadeVolume()
            if (currentVolume < 1f) {
                fadeInAnimator?.cancel()
                fadeInAnimator = ValueAnimator.ofFloat(currentVolume, 1f).apply {
                    duration = (fadeInDuration.toLong() / 2).coerceAtMost(500)
                    addUpdateListener { animator ->
                        val volume = animator.animatedValue as Float
                        if (videoView.isMute) {
                            cancel()
                            return@addUpdateListener
                        }
                        videoView.setVolume(volume, volume)
                    }
                    start()
                }
            }
        }
        startFadeCheck()
    }

    /**
     * 播放完成时确保渐出完成
     */
    fun onCompletion() {
        fadeOutCompleted = true
        cancelFadeIn()
        // 如果渐出动画还在进行，让它自然完成
        // 如果没有渐出（比如视频很短），直接静音
        if (fadeOutAnimator?.isRunning != true && isEnabled && !videoView.isMute) {
            videoView.setVolume(0f, 0f)
        }
        stopFadeCheck()
    }

    /**
     * 缓冲开始时暂停渐变
     */
    fun onBufferingStart() {
        pause()
    }

    /**
     * 缓冲结束时恢复渐变
     */
    fun onBufferingEnd() {
        resume()
    }
}
