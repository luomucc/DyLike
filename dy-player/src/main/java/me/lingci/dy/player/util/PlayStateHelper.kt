package me.lingci.dy.player.util

import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.player.VideoView

/**
 *   @author : happyc
 *   time    : 2024/09/26
 *   desc    :
 *   version : 1.0
 */
object PlayStateHelper {

    @JvmStatic
    fun isInPlaybackState(controlWrapper: ControlWrapper?, playState: Int): Boolean {
        return controlWrapper != null
                && playState != VideoView.STATE_ERROR
                && playState != VideoView.STATE_IDLE
                && playState != VideoView.STATE_PREPARING
                && playState != VideoView.STATE_PREPARED
                && playState != VideoView.STATE_START_ABORT
                && playState != VideoView.STATE_PLAYBACK_COMPLETED
    }

}