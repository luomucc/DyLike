package me.lingci.lib.player.exo

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener

@UnstableApi
open class FfmpegRenderersFactory(context: Context) : DefaultRenderersFactory(context) {

    companion object {
        const val TAG = "FfmpegRenderersFactory"
    }

    private var ffmpegVideoEnabled = false

    fun videoEnabled(enabled: Boolean) {
        ffmpegVideoEnabled = true
    }

    fun onExtensionRendererMode() {
        setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)
    }

    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>
    ) {
        if (extensionRendererMode != EXTENSION_RENDERER_MODE_OFF) {
            try {
                val renderer = FfmpegAudioRenderer(eventHandler, eventListener, audioSink)
                out.add(renderer)
                Log.i(TAG, "Loaded FfmpegAudioRenderer.")
            } catch (e: Exception) {
                // The extension is present, but instantiation failed.
                throw RuntimeException("Error instantiating Ffmpeg extension", e)
            }
        }
        super.buildAudioRenderers(
            context,
            extensionRendererMode,
            mediaCodecSelector,
            enableDecoderFallback,
            audioSink,
            eventHandler,
            eventListener,
            out
        )
    }

    override fun buildVideoRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        eventHandler: Handler,
        eventListener: VideoRendererEventListener,
        allowedVideoJoiningTimeMs: Long,
        out: ArrayList<Renderer>
    ) {
        if (extensionRendererMode != EXTENSION_RENDERER_MODE_OFF && ffmpegVideoEnabled) {
            /*try {
                val renderer = FfmpegVideoRenderer(
                    allowedVideoJoiningTimeMs,
                    eventHandler,
                    eventListener,
                    MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY
                )
                out.add(renderer)
                Log.i(TAG, "Loaded FfmpegVideoRenderer.")
            } catch (e: java.lang.Exception) {
                // The extension is present, but instantiation failed.
                throw java.lang.RuntimeException("Error instantiating Ffmpeg extension", e)
            }*/
        }
        super.buildVideoRenderers(
            context,
            extensionRendererMode,
            mediaCodecSelector,
            enableDecoderFallback,
            eventHandler,
            eventListener,
            allowedVideoJoiningTimeMs,
            out
        )
    }

    override fun buildTextRenderers(
        context: Context,
        output: TextOutput,
        outputLooper: Looper,
        extensionRendererMode: Int,
        out: ArrayList<Renderer>
    ) {
        val textRenderer = TextRenderer(output, outputLooper)
        textRenderer.experimentalSetLegacyDecodingEnabled(true)
        out.add(textRenderer)
    }

}
