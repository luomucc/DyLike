package me.lingci.dy.player.core

import me.lingci.lib.player.exo.CustomExoMediaPlayerFactory
import me.lingci.lib.player.mpv.MpvMediaPlayerFactory
import me.lingci.lib.player.mpv.render.MpvSurfaceRenderViewFactory
import me.lingci.lib.player.widget.videoview.CustomVideoView

/**
 * dy-player owns core selection now that CustomVideoView no longer knows about Exo/IJK/MPV.
 * Keep all backend factory/render-view wiring here so screens only choose a DyPlayerCore.
 */
object DyPlayerCoreRegistry {

    fun applyCore(videoView: CustomVideoView, core: DyPlayerCore, useMpvSpecialRender: Boolean = true) {
        // Core injection lives in dy-player after player-ui stopped depending on concrete backends.
        // MPV may also replace the render factory, so callers should apply generic render choices
        // before this method and gate short-video overrides with resolveCore().
        when (resolveCore(core)) {
            DyPlayerCore.EXO -> videoView.setPlayerFactory(CustomExoMediaPlayerFactory.create())
            DyPlayerCore.MPV -> {
                videoView.setPlayerFactory(MpvMediaPlayerFactory.create())
                if (useMpvSpecialRender) {
                    videoView.setRenderViewFactory(MpvSurfaceRenderViewFactory.create())
                }
            }
            // Unreachable while resolveCore(AUTO) returns EXO; keep exhaustive for future AUTO policy.
            DyPlayerCore.AUTO -> Unit
        }
    }

    fun resolveCore(core: DyPlayerCore): DyPlayerCore {
        return when (core) {
            // AUTO is kept as a persisted enum value but intentionally falls back to Exo until MPV
            // short-video lifecycle behavior is validated on devices.
            DyPlayerCore.AUTO -> DyPlayerCore.EXO
            else -> core
        }
    }
}
