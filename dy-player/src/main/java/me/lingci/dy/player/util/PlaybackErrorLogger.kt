package me.lingci.dy.player.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.lingci.lib.base.util.AppFile
import me.lingci.lib.base.util.FileOperator
import java.text.SimpleDateFormat
import java.util.Locale

object PlaybackErrorLogger {

    fun saveErrorLog(
        scope: CoroutineScope,
        context: Context,
        logCache: PlaybackLogCache,
        errorInfo: ErrorInfo
    ) {
        scope.launch(Dispatchers.IO) {
            val content = buildErrorReport(logCache.getEntries(), errorInfo)
            val file = AppFile(context).buildCustom(
                "logs",
                "short_play_error_${System.currentTimeMillis()}.log"
            )
            FileOperator.writeText(file, content)
        }
    }

    data class ErrorInfo(
        val videoPosition: Int,
        val videoName: String,
        val videoUrl: String,
        val playPosition: Long,
        val playState: Int,
        val playerState: Int,
        val hasMediaPlayer: Boolean,
        val hasRenderView: Boolean,
        val storageType: String,
        val extraInfo: Map<String, Any?> = emptyMap()
    )

    private fun buildErrorReport(
        recentLogs: List<PlaybackLogCache.LogEntry>,
        errorInfo: ErrorInfo
    ): String = buildString {
        appendLine("=== 短视频播放错误日志 ===")
        appendLine("时间: ${System.currentTimeMillis()}")
        appendLine("视频位置: ${errorInfo.videoPosition}")
        appendLine("视频名称: ${errorInfo.videoName}")
        appendLine("存储类型: ${errorInfo.storageType}")
        appendLine("播放进度: ${errorInfo.playPosition}ms")
        appendLine("播放状态: ${errorInfo.playState}")
        appendLine("播放器状态: ${errorInfo.playerState}")
        appendLine("是否有MediaPlayer: ${errorInfo.hasMediaPlayer}")
        appendLine("是否有RenderView: ${errorInfo.hasRenderView}")
        appendLine("URL: ${errorInfo.videoUrl}")
        errorInfo.extraInfo.forEach { (k, v) ->
            appendLine("$k: $v")
        }
        appendLine()
        appendLine("--- 报错前最近 ${recentLogs.size} 条日志 ---")
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        recentLogs.forEach { entry ->
            appendLine("[${sdf.format(entry.timestamp)}] [${entry.level}] [${entry.tag}] ${entry.message}")
        }
        appendLine("======================")
    }

}
