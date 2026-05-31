package me.lingci.lib.player.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.lingci.lib.player.ui.databinding.LayoutItemMediaTrackBinding
import me.lingci.lib.player.mediainfo.AudioTrackInfo
import me.lingci.lib.player.mediainfo.SubtitleTrackInfo
import me.lingci.lib.player.mediainfo.VideoTrackInfo
import java.util.Locale

sealed class MediaTrackItem {
    data class Video(val info: VideoTrackInfo) : MediaTrackItem()
    data class Audio(val info: AudioTrackInfo) : MediaTrackItem()
    data class Subtitle(val info: SubtitleTrackInfo) : MediaTrackItem()
}

class MediaTrackAdapter(
    private var items: List<MediaTrackItem> = emptyList()
) : RecyclerView.Adapter<MediaTrackAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutItemMediaTrackBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val item = items[position]
        when (item) {
            is MediaTrackItem.Video -> bindVideoTrack(binding, item.info)
            is MediaTrackItem.Audio -> bindAudioTrack(binding, item.info)
            is MediaTrackItem.Subtitle -> bindSubtitleTrack(binding, item.info)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun bindVideoTrack(binding: LayoutItemMediaTrackBinding, info: VideoTrackInfo) {
        binding.tvCodec.text = buildString {
            append(formatCodec(info.codec))
            if (!info.decodeMode.isNullOrBlank()) {
                append(" · ${info.decodeMode}")
            }
            if (!info.hwDecoder.isNullOrBlank() && info.hwDecoder != "no") {
                append(" (${info.hwDecoder})")
            }
        }
        binding.tvDetail.text = buildString {
            if (info.width > 0 && info.height > 0) {
                append("${info.width}x${info.height}")
            }
            if (info.frameRate > 0) {
                append(" @ ${formatFrameRate(info.frameRate)}fps")
            }
            if (info.bitRate > 0) {
                append(" · ${formatBitRate(info.bitRate)}")
            }
            if (!info.pixelFormat.isNullOrBlank()) {
                append(" · ${info.pixelFormat}")
            }
            if (info.bitDepth > 0) {
                append(" · ${info.bitDepth}-bit")
            }
            if (!info.colorSpace.isNullOrBlank()) {
                append(" · ${formatColorSpace(info.colorSpace!!)}")
            }
            if (!info.colorPrimaries.isNullOrBlank()) {
                append(" · 原色: ${formatColorSpace(info.colorPrimaries!!)}")
            }
            if (!info.colorTransfer.isNullOrBlank()) {
                append(" · ${formatColorTransfer(info.colorTransfer!!)}")
            }
            if (!info.chromaLocation.isNullOrBlank()) {
                append(" · 色度: ${info.chromaLocation}")
            }
            if (info.videoRotate != 0) {
                append(" · 旋转${info.videoRotate}°")
            }
            if (info.title != null) {
                append(" · ${info.title}")
            }
        }
        binding.ivSelected.visibility = if (info.isSelected) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun bindAudioTrack(binding: LayoutItemMediaTrackBinding, info: AudioTrackInfo) {
        binding.tvCodec.text = buildString {
            append(formatCodec(info.codec))
            if (info.lang != null) {
                append(" · ${formatLanguage(info.lang!!)}")
            }
            if (info.isExternal) {
                append(" · 外挂")
            }
        }
        binding.tvDetail.text = buildString {
            if (info.sampleRate > 0) {
                append("${info.sampleRate / 1000}kHz")
            }
            if (info.channelCount > 0) {
                append(" ${formatChannelCount(info.channelCount)}")
            }
            if (!info.channelLayout.isNullOrBlank()) {
                append(" (${info.channelLayout})")
            }
            if (info.bitRate > 0) {
                append(" · ${formatBitRate(info.bitRate)}")
            }
            if (!info.outputFormat.isNullOrBlank()) {
                append(" · 输出: ${info.outputFormat}")
            }
            if (!info.outputChannels.isNullOrBlank()) {
                append(" · ${info.outputChannels}")
            }
            if (info.title != null) {
                append(" · ${info.title}")
            }
        }
        binding.ivSelected.visibility = if (info.isSelected) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun bindSubtitleTrack(binding: LayoutItemMediaTrackBinding, info: SubtitleTrackInfo) {
        binding.tvCodec.text = buildString {
            append(formatCodec(info.codec))
            if (info.lang != null) {
                append(" · ${formatLanguage(info.lang!!)}")
            }
            if (info.isExternal) {
                append(" · 外挂")
            }
        }
        binding.tvDetail.text = buildString {
            if (info.title != null) {
                append(info.title)
            }
            if (info.filePath != null) {
                append(" · ${info.filePath}")
            }
        }
        binding.ivSelected.visibility = if (info.isSelected) android.view.View.VISIBLE else android.view.View.GONE
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(items: List<MediaTrackItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    private fun formatCodec(codec: String): String {
        if (codec.isBlank()) return "未知"
        return when (codec.lowercase()) {
            "video/h264", "avc", "h264", "h.264" -> "H.264"
            "video/hevc", "h265", "hevc", "h.265" -> "H.265"
            "video/av1", "av1" -> "AV1"
            "video/vp9", "vp9" -> "VP9"
            "video/vp8", "vp8" -> "VP8"
            "video/mpeg2", "mpeg2video" -> "MPEG-2"
            "video/mpeg4", "mpeg4" -> "MPEG-4"
            "audio/aac", "aac" -> "AAC"
            "audio/mp4a-latm" -> "AAC"
            "audio/opus", "opus" -> "Opus"
            "audio/vorbis", "vorbis" -> "Vorbis"
            "audio/flac", "flac" -> "FLAC"
            "audio/mpeg", "mp3", "audio/mpeg3" -> "MP3"
            "audio/ac-3", "ac3" -> "AC-3"
            "audio/eac-3", "eac3" -> "E-AC-3"
            "audio/truehd" -> "TrueHD"
            "audio/dts" -> "DTS"
            "audio/pcm", "audio/pcm_s16le", "audio/pcm_s24le" -> "PCM"
            "application/x-subrip", "subrip", "srt" -> "SRT"
            "text/vtt", "webvtt" -> "WebVTT"
            "text/x-ssa", "text/x-ass", "ass", "ssa" -> "ASS/SSA"
            "application/ttml+xml", "ttml" -> "TTML"
            else -> codec.substringAfterLast("/").uppercase(Locale.getDefault())
        }
    }

    private fun formatBitRate(bitRate: Long): String {
        return when {
            bitRate >= 1_000_000 -> String.format(Locale.getDefault(), "%.1f Mbps", bitRate / 1_000_000.0)
            bitRate >= 1_000 -> String.format(Locale.getDefault(), "%.0f Kbps", bitRate / 1_000.0)
            else -> "$bitRate bps"
        }
    }

    private fun formatFrameRate(fps: Float): String {
        return if (fps == fps.toInt().toFloat()) {
            fps.toInt().toString()
        } else {
            String.format(Locale.getDefault(), "%.2f", fps)
        }
    }

    private fun formatChannelCount(count: Int): String {
        return when (count) {
            1 -> "单声道"
            2 -> "立体声"
            6 -> "5.1"
            8 -> "7.1"
            else -> "${count}ch"
        }
    }

    private fun formatLanguage(lang: String): String {
        return when (lang.lowercase()) {
            "zh", "chi", "zho", "chinese" -> "中文"
            "en", "eng", "english" -> "英语"
            "ja", "jpn", "japanese" -> "日语"
            "ko", "kor", "korean" -> "韩语"
            "fr", "fra", "french" -> "法语"
            "de", "deu", "german" -> "德语"
            "es", "spa", "spanish" -> "西班牙语"
            "ru", "rus", "russian" -> "俄语"
            "und" -> "未指定"
            else -> lang
        }
    }

    private fun formatColorSpace(cs: String): String {
        return when (cs.lowercase()) {
            "bt.709", "bt709" -> "BT.709"
            "bt.601", "bt601" -> "BT.601"
            "bt.2020", "bt2020", "bt.2020-ncl", "bt.2020-cl" -> "BT.2020"
            "smpte_240m", "smpte240m" -> "SMPTE 240M"
            else -> cs
        }
    }

    private fun formatColorTransfer(transfer: String): String {
        return when (transfer.lowercase()) {
            "bt.1886", "bt1886", "sdr" -> "SDR"
            "pq", "smpte2084", "smpte st 2084" -> "PQ (HDR10)"
            "hlg" -> "HLG"
            "linear" -> "Linear"
            "gamma2.2", "gamma 2.2" -> "Gamma 2.2"
            else -> transfer
        }
    }

    inner class ViewHolder(val binding: LayoutItemMediaTrackBinding) :
        RecyclerView.ViewHolder(binding.root)
}
