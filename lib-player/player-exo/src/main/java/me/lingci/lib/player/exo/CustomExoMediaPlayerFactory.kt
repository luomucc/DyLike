package me.lingci.lib.player.exo

import android.content.Context
import xyz.doikki.videoplayer.player.PlayerFactory

/**
 *   @author : happyc
 *   time    : 2025/03/22
 *   desc    : 自定义exo
 *   version : 1.0
 */
class CustomExoMediaPlayerFactory: PlayerFactory<CustomExoMediaPlayer>() {

    companion object {

        fun create(): CustomExoMediaPlayerFactory {
            return CustomExoMediaPlayerFactory()
        }

    }

    override fun createPlayer(context: Context): CustomExoMediaPlayer {
        return CustomExoMediaPlayer(context)
    }

}
