# ============================================================
# player-exo Consumer ProGuard Rules for AGP 9 & R8
# ============================================================

# ============================================================
# 1. Media3 ExoPlayer
# ============================================================
-keep class androidx.media3.exoplayer.** { *; }
-keep interface androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.extractor.** { *; }
-keep class * extends androidx.media3.exoplayer.Renderer { *; }
-keep class androidx.media3.exoplayer.audio.** { *; }
-keep class androidx.media3.exoplayer.video.** { *; }
-keep class androidx.media3.exoplayer.text.** { *; }
-keep class androidx.media3.exoplayer.trackselection.** { *; }
-keep class androidx.media3.exoplayer.upstream.** { *; }
-keep class androidx.media3.exoplayer.drm.** { *; }

# ============================================================
# 2. Media3 扩展 (DASH, HLS, RTSP)
# ============================================================
-keep class androidx.media3.exoplayer.dash.** { *; }
-keep class androidx.media3.exoplayer.hls.** { *; }
-keep class androidx.media3.exoplayer.rtsp.** { *; }
-keep class androidx.media3.exoplayer.smoothstreaming.** { *; }

# ============================================================
# 3. Media3 FFmpeg 解码器 (JNI)
# ============================================================
-keep class androidx.media3.decoder.ffmpeg.FfmpegLibrary { *; }
-keep class androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer { *; }
-keep class androidx.media3.decoder.ffmpeg.FfmpegDecoder { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class * extends androidx.media3.decoder.SimpleDecoder { *; }

# ============================================================
# 4. Media3 OkHttp / RTMP DataSource
# ============================================================
-keep class androidx.media3.datasource.okhttp.** { *; }
-keep class androidx.media3.datasource.rtmp.** { *; }

# ============================================================
# 5. 自定义扩展保护
# ============================================================
-keep class me.lingci.lib.player.exo.** extends androidx.media3.exoplayer.Renderer { *; }
-keep class me.lingci.lib.player.exo.** implements androidx.media3.datasource.DataSource { *; }
-keep class me.lingci.lib.player.exo.** implements androidx.media3.extractor.Extractor { *; }

# ============================================================
# 6. 通用规则
# ============================================================
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
