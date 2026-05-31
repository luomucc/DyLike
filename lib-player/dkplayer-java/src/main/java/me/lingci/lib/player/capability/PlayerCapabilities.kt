package me.lingci.lib.player.capability

/**
 * Describes optional backend features without requiring every player implementation to expose
 * backend-specific classes. UI code should branch on these flags instead of checking Exo/IJK/MPV.
 */
data class PlayerCapabilities(
    val canListTracks: Boolean = false,
    val canSelectVideoTrack: Boolean = false,
    val canSelectAudioTrack: Boolean = false,
    val canSelectSubtitleTrack: Boolean = false,
    val canDisableAudio: Boolean = false,
    val canDisableSubtitle: Boolean = false,
    val canAddExternalAudio: Boolean = false,
    val canAddExternalSubtitle: Boolean = false,
    val canProvideSubtitleCues: Boolean = false,
    val canRenderSubtitleInternally: Boolean = false,
    val canProvideChapters: Boolean = false,
    val canProvideMediaInfo: Boolean = false,
    val maxPlaybackSpeed: Float = 4f,
    val maxLongPressSpeed: Float = 4f,
    val requiresSurfaceRenderView: Boolean = false,
    val supportsScreenshot: Boolean = true,
)

/**
 * Implemented by player backends that can report their optional feature set at runtime.
 * Kept separate from AbstractPlayer to avoid breaking binary compatibility with dkplayer backends.
 */
interface PlayerCapabilityProvider {
    fun getPlayerCapabilities(): PlayerCapabilities
}
