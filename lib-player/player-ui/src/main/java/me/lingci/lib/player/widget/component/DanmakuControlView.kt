package me.lingci.lib.player.widget.component

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import androidx.core.view.isGone
import master.flame.danmaku.controller.DrawHandler
import master.flame.danmaku.controller.IDanmakuView
import master.flame.danmaku.danmaku.loader.android.BiliDanmakuLoader
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.DanmakuTimer
import master.flame.danmaku.danmaku.model.IDanmakus
import master.flame.danmaku.danmaku.model.IDisplayer.DANMAKU_STYLE_STROKEN
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import me.lingci.lib.base.util.Log
import me.lingci.lib.base.util.ToastUtil
import me.lingci.lib.base.util.logD
import me.lingci.lib.dm.view.controller.KeywordFilter
import me.lingci.lib.dm.view.controller.RegexFilter
import me.lingci.lib.dm.view.entity.DmLoadOptions
import me.lingci.lib.dm.view.parser.BiliDanmakuParser
import me.lingci.lib.dm.view.parser.DanmakuParserBuilder
import me.lingci.lib.dm.view.widget.GradientViewCacheStuffer
import me.lingci.lib.player.danmaku.PlayerInitializer
import me.lingci.lib.player.ui.databinding.LayoutDmMuControlViewBinding
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IGestureComponent
import xyz.doikki.videoplayer.player.VideoView
import java.io.File
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.max

/**
 * Created by xyoye on 2020/11/17.
 * bullet screen
 */
@Suppress("MemberVisibilityCanBePrivate")
class DanmakuControlView : FrameLayout, IGestureComponent {

    companion object {
        private const val TAG = "DanmakuControlView"

        private const val DAN_MU_MAX_TEXT_SIZE = 2.4f
        private const val DAN_MU_MAX_TEXT_ALPHA = 1f
        private const val DAN_MU_MAX_TEXT_SPEED = 2.5f
        private const val DAN_MU_MAX_TEXT_STOKE = 20f

        private const val SYNC_THRESHOLD_MS = 500L
        private const val SYNC_DEBOUNCE_MS = 500L

        private const val INVALID_VALUE = -1L
    }

    private var binding: LayoutDmMuControlViewBinding = LayoutDmMuControlViewBinding.inflate(
        LayoutInflater.from(
            context
        ), this, true
    )
    private lateinit var mControlWrapper: ControlWrapper

    private val danmakuContext = DanmakuContext.create()
    private val biliDanmakuLoader = BiliDanmakuLoader.instance()
    private val mKeywordFilter = KeywordFilter()
    private val mRegexFilter = RegexFilter()

    private var mUrl: String? = null
    private var isLoaded = false
    private var strokeMultiple: Float = 1f
    private var dmLoadOptions = DmLoadOptions()
    private var lastSyncTargetTime = INVALID_VALUE
    private var lastSyncRequestTime = 0L

    init {
        binding.danmakuView.showFPS(false)

        initdanmakuView()
        initDanmakuContext()

        binding.danmakuView.setCallback(object : DrawHandler.Callback {
            override fun drawingFinished() {

            }

            override fun danmakuShown(danmaku: BaseDanmaku?) {

            }

            override fun prepared() {
                post {
                    isLoaded = true
                    if (mControlWrapper.isPlaying) {
                        seekTo(mControlWrapper.currentPosition, mControlWrapper.isPlaying)
                    }
                }
            }

            override fun updateTimer(timer: DanmakuTimer?) {

            }
        })
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun attach(controlWrapper: ControlWrapper) {
        mControlWrapper = controlWrapper
    }

    override fun getView(): View {
        return this
    }

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {

    }

    override fun onPlayStateChanged(playState: Int) {
        Log.d(TAG, "onPlayStateChanged:", playState, "playing", mControlWrapper.isPlaying,
            "video", mControlWrapper.currentPosition, "danMu", binding.danmakuView.currentTime,
            "state", binding.danmakuView.isPaused, binding.danmakuView.isPrepared, binding.danmakuView.isViewReady)
        when (playState) {
            VideoView.STATE_IDLE -> {
                release()
            }
            // 准备
            VideoView.STATE_PREPARING,
            VideoView.STATE_PREPARED -> {

            }
            // 播放
            VideoView.STATE_PLAYING -> {
                if (binding.danmakuView.isPrepared) {
                    if (binding.danmakuView.isPaused) {
                        resume()
                    } else {
                        updateOffsetTime()
                    }
                }
            }
            // 暂停加载失败等
            VideoView.STATE_PLAYBACK_COMPLETED,
            VideoView.STATE_ERROR,
            VideoView.STATE_PAUSED -> {
                if (binding.danmakuView.isPrepared) {
                    pause()
                }
            }
            // 预加载
            VideoView.STATE_BUFFERING -> {
                if (binding.danmakuView.isPrepared) {
                    pause()
                }
            }
            // 预加载完成
            VideoView.STATE_BUFFERED -> {
                if (binding.danmakuView.isPrepared && mControlWrapper.isPlaying) {
                    resume()
                }
            }
        }
    }

    override fun onPlayerStateChanged(playerState: Int) {
        Log.d(TAG, "onPlayerStateChanged: $playerState")
        when (playerState) {
            VideoView.PLAYER_NORMAL -> {

            }

            VideoView.PLAYER_FULL_SCREEN -> {

            }

            VideoView.PLAYER_TINY_SCREEN -> {

            }
        }

    }

    override fun setProgress(duration: Int, position: Int) {

    }

    override fun onLockStateChanged(isLocked: Boolean) {

    }

    override fun onStartSlide() {

    }

    override fun onStopSlide() {

    }

    override fun onPositionChange(slidePosition: Int, currentPosition: Int, duration: Int) {

    }

    override fun onBrightnessChange(percent: Int) {

    }

    override fun onVolumeChange(percent: Int) {

    }

    override fun onStartAccelerate() {
        if (PlayerInitializer.Player.videoSpeed == PlayerInitializer.Player.pressVideoSpeed) {
            return
        }
        danmakuContext.setSpeed(PlayerInitializer.Player.pressVideoSpeed)
    }

    override fun onStopAccelerate() {
        danmakuContext.setSpeed(mControlWrapper.speed)
    }

    fun getPosition(): Long {
        return binding.danmakuView.currentTime
    }

    fun resume() {
        binding.danmakuView.resume()
        updateOffsetTime()
    }

    fun pause() {
        binding.danmakuView.pause()
        logD("pause")
    }

    fun release() {
        binding.danmakuView.release()
        mUrl = null
        lastSyncTargetTime = INVALID_VALUE
        lastSyncRequestTime = 0L
        binding.danmakuView.clear()
        binding.danmakuView.clearDanmakusOnScreen()
    }

    fun setStrokeMultiple(multiple: Float) {
        this.strokeMultiple = multiple
        danmakuContext.setCacheStuffer(GradientViewCacheStuffer(strokeMultiple), null)
    }

    fun seekTo(timeMs: Long, isPlaying: Boolean) {
        if (isPlaying.and(binding.danmakuView.config != null).and(binding.danmakuView.isPrepared)) {
            val targetTime = timeMs + PlayerInitializer.Danmu.offsetPosition
            markSyncTarget(targetTime)
            logD("seek to $targetTime")
            binding.danmakuView.seekTo(targetTime)
            //binding.danmakuView.start(timeMs + PlayerInitializer.Danmu.offsetPosition)
        }
    }

    fun loadDanMu(url: String) {
        loadDanMu(url, dmLoadOptions)
    }

    fun loadDanMu(url: String, options: DmLoadOptions) {
        PlayerInitializer.Danmu.offsetPosition = 0L
        if (url.isBlank())
            return
        val danMuFile = File(url)
        if (!danMuFile.exists())
            return
        mUrl = url
        biliDanmakuLoader.load(url)
        if (biliDanmakuLoader.dataSource != null) {
            val danMaKuParser = BiliDanmakuParser(options)
            danMaKuParser.load(biliDanmakuLoader.dataSource)
            binding.danmakuView.prepare(danMaKuParser, danmakuContext)
            isLoaded = false
        } else {
            (context as? Activity)?.runOnUiThread {
                ToastUtil.showToast(context, "弹幕加载失败")
            }
        }
    }

    fun loadDanMu(stream: InputStream?) {
        loadDanMu(stream, dmLoadOptions)
    }

    fun loadDanMu(stream: InputStream?, options: DmLoadOptions) {
        PlayerInitializer.Danmu.offsetPosition = 0L
        if (null == stream) {
            return
        }
        try {
            val danMaKuParser = DanmakuParserBuilder.createParser(stream, options)
            binding.danmakuView.prepare(danMaKuParser, danmakuContext)
            isLoaded = false
        } catch (e: Exception) {
            (context as? Activity)?.runOnUiThread {
                ToastUtil.showToast(context, "弹幕加载失败")
            }
        }
    }

    fun toggleVis() {
        if (binding.danmakuView.isShown) {
            binding.danmakuView.hide()
        } else {
            binding.danmakuView.show()
        }
    }

    fun toggleVis(show: Boolean) {
        if (show) {
            binding.danmakuView.show()
        } else {
            binding.danmakuView.hide()
        }
    }

    private fun initdanmakuView() {
        // 硬解加速
        binding.danmakuView.setLayerType(LAYER_TYPE_HARDWARE, null)
        //binding.danmakuView.setLayerType(LAYER_TYPE_SOFTWARE, null)
        // 设置高优先级绘制线程
        binding.danmakuView.setDrawingThreadType(IDanmakuView.THREAD_TYPE_HIGH_PRIORITY)

    }

    private fun setDanmakuClickListener() {
        binding.danmakuView.onDanmakuClickListener = object: IDanmakuView.OnDanmakuClickListener {
            override fun onDanmakuClick(danmakus: IDanmakus?): Boolean {
                Log.d("DFM", "onDanmakuClick: danmakus size:" + danmakus!!.size())
                val latest = danmakus.last()
                if (null != latest) {
                    Log.d(
                        "DFM",
                        "onDanmakuClick: text of latest danmaku:" + latest.text
                    )
                    return false
                }
                return false
            }

            override fun onDanmakuLongClick(danmakus: IDanmakus?): Boolean {
                return false
            }

            override fun onViewClick(view: IDanmakuView?): Boolean {
                return false
            }
        }
    }

    private fun initDanmakuContext() {
        // 设置禁止重叠
        val overlappingPair: MutableMap<Int, Boolean> = HashMap()
        overlappingPair[BaseDanmaku.TYPE_SCROLL_LR] = true
        overlappingPair[BaseDanmaku.TYPE_SCROLL_RL] = true
        overlappingPair[BaseDanmaku.TYPE_FIX_TOP] = true
        overlappingPair[BaseDanmaku.TYPE_FIX_BOTTOM] = true

        // 弹幕更新方式, 0:Choreographer, 1:new Thread, 2:DrawHandler
        val danMuUpdateMethod: Byte =
            if (PlayerInitializer.Danmu.updateInChoreographer) 0 else 2

        danmakuContext.apply {
            // 合并重复弹幕
            isDuplicateMergingEnabled = true
            // 设置禁止重叠
            danmakuContext.preventOverlapping(overlappingPair)
            // 使用DrawHandler驱动刷新，避免在高刷新率时时间轴错位
            updateMethod = danMuUpdateMethod
            // 添加关键字过滤器
            registerFilter(mKeywordFilter)
            // 添加正则过滤器
            registerFilter(mRegexFilter)
            // 自定义绘制
            setCacheStuffer(GradientViewCacheStuffer(strokeMultiple), null)
            setDanmakuMargin(PlayerInitializer.Danmu.margin)
        }
        // 弹幕view开启绘制缓存
        binding.danmakuView.enableDanmakuDrawingCache(true)
        binding.danmakuView.prepare(BiliDanmakuParser(), danmakuContext)

        updateDanmuSize()
        updateDanmuSpeed()
        updateDanmuAlpha()
        updateDanmuStroke()
        updateMobileDanmuState()
        updateTopDanmuState()
        updateBottomDanmuState()
        updateMaxLine()
        updateMaxScreenNum()
    }

    fun showFps(show: Boolean) {
        binding.danmakuView.showFPS(show)
    }

    fun updateTextBold(bold: Boolean) {
        Log.d(this@DanmakuControlView, "textBold", bold)
        danmakuContext.setDanmakuBold(bold)
    }

    fun updateDanmuSize() {
        val progress = PlayerInitializer.Danmu.size / 100f
        val size = progress * DAN_MU_MAX_TEXT_SIZE
        danmakuContext.setScaleTextSize(size)
    }

    fun updateDanmuSpeed() {
        danmakuContext.setSpeed(PlayerInitializer.Player.videoSpeed)
    }

    fun updateScrollSpeed() {
        val progress = PlayerInitializer.Danmu.speed / 100f
        var speed = DAN_MU_MAX_TEXT_SPEED * (1 - progress)
        speed = max(0.1f, speed)
        Log.d(this@DanmakuControlView, "speed", speed, "progress", progress)
        danmakuContext.setScrollSpeedFactor(speed)
    }

    fun updateDanmuAlpha() {
        val progress = PlayerInitializer.Danmu.alpha / 100f
        val alpha = progress * DAN_MU_MAX_TEXT_ALPHA
        danmakuContext.setDanmakuTransparency(alpha)
    }

    fun updateDanmuStroke() {
        val progress = PlayerInitializer.Danmu.stoke / 100f
        val stoke = progress * DAN_MU_MAX_TEXT_STOKE
        Log.d(this@DanmakuControlView, "stroke", stoke)
        danmakuContext.setDanmakuStyle(DANMAKU_STYLE_STROKEN, stoke)
    }

    fun updateDanmuMargin() {
        danmakuContext.setDanmakuMargin(PlayerInitializer.Danmu.margin)
    }

    fun updateMobileDanmuState() {
        danmakuContext.r2LDanmakuVisibility = PlayerInitializer.Danmu.mobileDanmu
    }

    fun updateTopDanmuState() {
        danmakuContext.ftDanmakuVisibility = PlayerInitializer.Danmu.topDanmu
    }

    fun updateBottomDanmuState() {
        danmakuContext.fbDanmakuVisibility = PlayerInitializer.Danmu.bottomDanmu
    }

    fun updateOffsetTime() {
        syncTimeIfNeeded(mControlWrapper.currentPosition)
    }

    fun syncTime() {
        seekTo(mControlWrapper.currentPosition, mControlWrapper.isPlaying)
    }

    private fun syncTimeIfNeeded(positionMs: Long) {
        // 进度回调是 MPV seek 后最稳定的同步入口，正常播放时只在明显偏移时校正。
        val targetTime = positionMs + PlayerInitializer.Danmu.offsetPosition
        val difference = abs(targetTime - binding.danmakuView.currentTime)
        Log.d(TAG, "syncTimeIfNeeded: $positionMs ${binding.danmakuView.currentTime} $difference ${binding.danmakuView.isShown} ${binding.danmakuView.isGone}")
        if (difference > SYNC_THRESHOLD_MS) {
            if (isDuplicateSyncTarget(targetTime)) {
                Log.d(TAG, "skip duplicate sync: $targetTime")
                return
            }
            seekTo(positionMs, mControlWrapper.isPlaying)
        }
    }

    private fun isDuplicateSyncTarget(targetTime: Long): Boolean {
        val now = SystemClock.uptimeMillis()
        return lastSyncTargetTime != INVALID_VALUE &&
            abs(targetTime - lastSyncTargetTime) <= SYNC_THRESHOLD_MS &&
            now - lastSyncRequestTime < SYNC_DEBOUNCE_MS
    }

    private fun markSyncTarget(targetTime: Long) {
        lastSyncTargetTime = targetTime
        lastSyncRequestTime = SystemClock.uptimeMillis()
    }

    fun updateMaxLine() {
        val scroll: Int? = if (PlayerInitializer.Danmu.maxLine == -1) null else PlayerInitializer.Danmu.maxLine
        val top: Int? = if (PlayerInitializer.Danmu.maxTopLine == -1) null else PlayerInitializer.Danmu.maxTopLine
        val bottom: Int? = if (PlayerInitializer.Danmu.maxBottomLine == -1) null else PlayerInitializer.Danmu.maxBottomLine
        danmakuContext.setMaximumLines(
            mutableMapOf(
                BaseDanmaku.TYPE_SCROLL_LR to scroll,
                BaseDanmaku.TYPE_SCROLL_RL to scroll,
                BaseDanmaku.TYPE_FIX_TOP to top,
                BaseDanmaku.TYPE_FIX_BOTTOM to bottom,
            )
        )
    }

    fun updateMaxScreenNum() {
        danmakuContext.setMaximumVisibleSizeInScreen(PlayerInitializer.Danmu.maxNum)
    }

    fun addBlackList(isRegex: Boolean, vararg keyword: String) {
        keyword.forEach {
            if (isRegex) {
                mRegexFilter.addRegex(it)
            } else {
                mKeywordFilter.addKeyword(it)
            }
        }
        notifyFilterChanged()
    }

    fun removeBlackList(isRegex: Boolean, keyword: String) {
        if (isRegex) {
            mRegexFilter.removeRegex(keyword)
        } else {
            mKeywordFilter.removeKeyword(keyword)
        }
        notifyFilterChanged()
    }

    fun setCloudBlockData(cloudBlockData: MutableList<String>) {
        if (PlayerInitializer.Danmu.cloudBlock && cloudBlockData.isNotEmpty()) {
            cloudBlockData.forEach { entity ->
                if (entity.isNotBlank()) {
                    mRegexFilter.addRegex(entity)
                } else {
                    mKeywordFilter.addKeyword(entity)
                }
            }
            notifyFilterChanged()
        }
    }

    fun allowSendDanmu(): Boolean {
        return isLoaded
    }

    fun addDanmuToView(isScroll: Boolean, isTop: Boolean, textStr: String, color: Int) {
        val type = when {
            isScroll -> BaseDanmaku.TYPE_SCROLL_RL
            isTop -> BaseDanmaku.TYPE_FIX_TOP
            else -> BaseDanmaku.TYPE_FIX_BOTTOM
        }

        val danMaKu = danmakuContext.mDanmakuFactory.createDanmaku(type).apply {
            text = textStr
            padding = 5
            isLive = false
            priority = 0
            textColor = color
            underlineColor = Color.GREEN
            time = binding.danmakuView.currentTime + 500
        }
        binding.danmakuView.addDanmaku(danMaKu)
    }

    fun setSpeed(speed: Float) {
        danmakuContext.setSpeed(speed)
    }

    fun setDmGradient(dmGradient: Boolean, gradientRatio: Int) {
        this.dmLoadOptions.whiteToGradient = dmGradient
        this.dmLoadOptions.gradientRatio = gradientRatio
    }

    fun setGradientWithTextColor(withTextColor: Boolean) {
        this.dmLoadOptions.gradientWithTextColor = withTextColor
    }

    fun setMergeOption(show: Boolean, merge: Boolean, showNum: Int, topNum: Int, debug: Boolean) {
        this.dmLoadOptions.timeDebug = show
        this.dmLoadOptions.mergeContent = merge
        this.dmLoadOptions.mergeShow = showNum
        this.dmLoadOptions.mergeToTop = topNum
        this.dmLoadOptions.debug = debug
    }

    fun setTypeface(typeface: Typeface) {
        danmakuContext.setTypeface(typeface)
        repairStyle()
    }

    fun clearTypeface() {
        danmakuContext.setTypeface(null)
        repairStyle()
    }

    private fun repairStyle() {
        val size = PlayerInitializer.Danmu.size
        PlayerInitializer.Danmu.size = size + 1
        updateDanmuSize()
        PlayerInitializer.Danmu.size = size
        updateDanmuSize()
    }

    fun updateDanMuViewMargin() {
        var tmpLayoutParams = binding.danmakuView.layoutParams
        Log.d(this@DanmakuControlView, tmpLayoutParams, binding.danmakuView.width, binding.danmakuView.height, width, height)
        if (tmpLayoutParams == null) {
            tmpLayoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        if (tmpLayoutParams is MarginLayoutParams) {
            tmpLayoutParams.topMargin = PlayerInitializer.Danmu.viewTopMargin * 10
            tmpLayoutParams.bottomMargin = PlayerInitializer.Danmu.viewBottomMargin * 10
            binding.danmakuView.layoutParams = tmpLayoutParams
        }
    }

    private fun notifyFilterChanged() {
        // 该方法内部会调用弹幕刷新，能达到相应效果
        danmakuContext.addUserHashBlackList()
    }

}
