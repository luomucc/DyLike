package me.lingci.lib.player.mediainfo

/** Provides backend-neutral media information without exposing Exo/IJK/MPV native models. */
interface MediaInfoProvider {
    fun getMediaInfo(): MediaInfoData

    /** True when enough metadata is available to populate the panel, not necessarily while playing. */
    fun isAvailable(): Boolean
}
