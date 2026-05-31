package me.lingci.lib.player.mediainfo

/**
 * Optional factory exposed by backends that can build a media-info adapter for the current player.
 * This keeps Activity code from constructing Exo/MPV provider classes directly.
 */
interface MediaInfoProviderOwner {
    fun createMediaInfoProvider(): MediaInfoProvider?

    fun getMediaInfoProviderName(): String
}
