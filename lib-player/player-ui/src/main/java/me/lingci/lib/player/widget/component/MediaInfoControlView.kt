package me.lingci.lib.player.widget.component

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import me.lingci.lib.base.storage.entity.FileEntity
import me.lingci.lib.base.storage.entity.StorageType
import me.lingci.lib.player.adapter.MediaTrackAdapter
import me.lingci.lib.player.adapter.MediaTrackItem
import me.lingci.lib.player.mediainfo.ContainerInfo
import me.lingci.lib.player.mediainfo.MediaInfoData
import me.lingci.lib.player.mediainfo.MediaInfoProvider
import me.lingci.lib.player.ui.databinding.LayoutMediaInfoControlViewBinding
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils
import java.util.Locale

class MediaInfoControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), IControlComponent {

    companion object {
        private const val TAG = "MediaInfoControlView"
    }

    private val binding = LayoutMediaInfoControlViewBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    private lateinit var controlWrapper: ControlWrapper
    private var mediaInfoProvider: MediaInfoProvider? = null
    private var fileEntity: FileEntity? = null
    private var sourceType: StorageType? = null
    private var providerName: String = ""

    private val videoTrackAdapter = MediaTrackAdapter()
    private val audioTrackAdapter = MediaTrackAdapter()
    private val subtitleTrackAdapter = MediaTrackAdapter()

    init {
        visibility = GONE
        initListener()
        initRecyclerView()
    }

    private fun initListener() {
        setOnClickListener { switchVib() }
        binding.container.setOnClickListener { }
        binding.actionClose.setOnClickListener { switchVib() }
    }

    private fun initRecyclerView() {
        binding.rvVideoTracks.layoutManager = LinearLayoutManager(context)
        binding.rvVideoTracks.adapter = videoTrackAdapter
        binding.rvVideoTracks.isNestedScrollingEnabled = false

        binding.rvAudioTracks.layoutManager = LinearLayoutManager(context)
        binding.rvAudioTracks.adapter = audioTrackAdapter
        binding.rvAudioTracks.isNestedScrollingEnabled = false

        binding.rvSubtitleTracks.layoutManager = LinearLayoutManager(context)
        binding.rvSubtitleTracks.adapter = subtitleTrackAdapter
        binding.rvSubtitleTracks.isNestedScrollingEnabled = false
    }

    fun setMediaInfoProvider(provider: MediaInfoProvider?) {
        this.mediaInfoProvider = provider
    }

    fun setProviderName(name: String) {
        this.providerName = name
    }

    fun setFileEntity(fileEntity: FileEntity?, sourceType: StorageType?) {
        this.fileEntity = fileEntity
        this.sourceType = sourceType
    }

    fun refreshMediaInfo() {
        val provider = mediaInfoProvider
        if (provider == null || !provider.isAvailable()) return

        val mediaInfo = provider.getMediaInfo()
        updateFileInfo(mediaInfo)
        updateVideoTracks(mediaInfo)
        updateAudioTracks(mediaInfo)
        updateSubtitleTracks(mediaInfo)
    }

    /**
     * 从 ControlWrapper 同步实时变化的数据（时长、进度、播放状态）
     * 在面板打开时和 setProgress 回调中调用
     */
    private fun syncRealtimeData(duration: Int = 0, position: Int = 0) {
        // 时长和进度：优先使用缓存的 setProgress 值，其次 ControlWrapper
        val dur = if (duration > 0) duration else controlWrapper.duration.toInt()
        val pos = if (position > 0) position else controlWrapper.currentPosition.toInt()
        binding.tvDuration.text = buildString {
            append(formatDuration(dur))
            if (pos > 0 && dur > 0) {
                append(" · 进度: ${formatDuration(pos)}")
            }
        }
        // 播放状态：实时从 ControlWrapper 获取
        binding.tvSourceType.text = buildString {
            append(formatSourceType(sourceType))
            append(" · ${if (controlWrapper.isPlaying) "播放中" else "暂停"}")
        }
    }

    private fun updateFileInfo(mediaInfo: MediaInfoData? = null) {
        val containerInfo = mediaInfo?.containerInfo ?: ContainerInfo()
        val file = fileEntity

        binding.tvFileName.text = containerInfo.fileName.ifBlank { file?.name ?: "" }
        binding.tvFileSize.text = formatFileSize(coalesce(containerInfo.fileSize, file?.size ?: 0L))
        // 时长和进度由 syncRealtimeData() 统一负责，此处先设基础值
        binding.tvDuration.text = buildString {
            append(formatDuration(containerInfo.duration.toInt()))
            val pos = containerInfo.currentPosition.toInt()
            if (pos > 0) {
                append(" · 进度: ${formatDuration(pos)}")
            }
        }
        binding.tvContainer.text = containerInfo.containerFormat.ifBlank {
            file?.mimeType?.substringAfterLast("/") ?: ""
        }
        binding.tvSourceType.text = buildString {
            append(containerInfo.sourceType.ifBlank { formatSourceType(sourceType) })
            if (containerInfo.playState.isNotBlank()) {
                append(" · ${containerInfo.playState}")
            }
        }
        binding.tvDisplayMode.text = buildString {
            when {
                controlWrapper.isFullScreen -> append("全屏")
                controlWrapper.isTinyScreen -> append("小窗")
                else -> append("普通")
            }
            if (containerInfo.displayRefreshRate > 0) {
                append(" · ${String.format(Locale.getDefault(), "%.1f", containerInfo.displayRefreshRate)}Hz")
            }
        }
        binding.tvRuntimeInfo.text = buildString {
            append("Android ${Build.VERSION.RELEASE}")
            append(" · ${Build.MANUFACTURER} ${Build.MODEL}")
            if (providerName.isNotBlank()) {
                append(" · $providerName")
            }
        }
    }

    private fun updateVideoTracks(mediaInfo: MediaInfoData) {
        val tracks = mediaInfo.videoTracks
        binding.sectionVideo.visibility = if (tracks.isEmpty()) GONE else VISIBLE
        binding.rvVideoTracks.visibility = if (tracks.isEmpty()) GONE else VISIBLE
        if (tracks.isNotEmpty()) {
            videoTrackAdapter.setData(tracks.map { MediaTrackItem.Video(it) })
        }
    }

    private fun updateAudioTracks(mediaInfo: MediaInfoData) {
        val tracks = mediaInfo.audioTracks
        binding.sectionAudio.visibility = if (tracks.isEmpty()) GONE else VISIBLE
        binding.rvAudioTracks.visibility = if (tracks.isEmpty()) GONE else VISIBLE
        if (tracks.isNotEmpty()) {
            audioTrackAdapter.setData(tracks.map { MediaTrackItem.Audio(it) })
        }
    }

    private fun updateSubtitleTracks(mediaInfo: MediaInfoData) {
        val tracks = mediaInfo.subtitleTracks
        binding.sectionSubtitle.visibility = if (tracks.isEmpty()) GONE else VISIBLE
        binding.rvSubtitleTracks.visibility = if (tracks.isEmpty()) GONE else VISIBLE
        if (tracks.isNotEmpty()) {
            subtitleTrackAdapter.setData(tracks.map { MediaTrackItem.Subtitle(it) })
        }
    }

    fun switchVib() {
        if (isVisible) {
            visibility = GONE
        } else {
            refreshMediaInfo()
            syncRealtimeData()
            visibility = VISIBLE
        }
    }

    override fun attach(controlWrapper: ControlWrapper) {
        this.controlWrapper = controlWrapper
    }

    override fun getView(): View = this

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation) {}

    override fun onPlayStateChanged(playState: Int) {
        if (playState == VideoView.STATE_PREPARED || playState == VideoView.STATE_PLAYING) {
            if (isVisible) {
                refreshMediaInfo()
                syncRealtimeData()
            }
        }
    }

    override fun onPlayerStateChanged(playerState: Int) {}

    override fun setProgress(duration: Int, position: Int) {
        // 面板可见时实时更新进度
        if (isVisible) {
            syncRealtimeData(duration, position)
        }
    }

    override fun onLockStateChanged(isLocked: Boolean) {}

    private fun coalesce(primary: Long, fallback: Long): Long {
        return if (primary != 0L) primary else fallback
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return ""
        return when {
            size >= 1_073_741_824 -> String.format(Locale.getDefault(), "%.2f GB", size / 1_073_741_824.0)
            size >= 1_048_576 -> String.format(Locale.getDefault(), "%.1f MB", size / 1_048_576.0)
            size >= 1_024 -> String.format(Locale.getDefault(), "%.1f KB", size / 1_024.0)
            else -> "$size B"
        }
    }

    private fun formatDuration(durationMs: Int): String {
        if (durationMs <= 0) return ""
        return PlayerUtils.stringForTime(durationMs)
    }

    private fun formatSourceType(type: StorageType?): String {
        return when (type) {
            StorageType.LOCAL_STORAGE -> "本地"
            StorageType.WEBDAV -> "WebDAV"
            StorageType.SMB -> "SMB"
            StorageType.STREAM_LINK -> "流媒体"
            StorageType.ONLINE_LINK -> "在线"
            StorageType.OPEN_LIST -> "网盘"
            else -> ""
        }
    }

}
