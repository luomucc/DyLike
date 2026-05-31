package me.lingci.lib.player.track

/**
 * Optional write side for track selection. Unsupported operations return false instead of throwing,
 * allowing callers to offer one UI across Exo/MPV/IJK with capability checks.
 */
interface MediaTrackController {
    fun selectTrack(key: MediaTrackKey): Boolean

    fun disableTrack(type: MediaTrackType): Boolean

    fun clearTrackOverride(type: MediaTrackType): Boolean {
        return false
    }

    fun setPreferredLanguages(
        audioLanguages: List<String>,
        subtitleLanguages: List<String>,
    ): Boolean {
        return false
    }
}
