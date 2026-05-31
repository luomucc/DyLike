package me.lingci.lib.player.mpv

import android.content.Context
import xyz.doikki.videoplayer.player.PlayerFactory

class MpvMediaPlayerFactory : PlayerFactory<MpvMediaPlayer>() {

    override fun createPlayer(context: Context): MpvMediaPlayer {
        return MpvMediaPlayer(context)
    }

    companion object {

        fun create(): MpvMediaPlayerFactory {
            return MpvMediaPlayerFactory()
        }

    }

}