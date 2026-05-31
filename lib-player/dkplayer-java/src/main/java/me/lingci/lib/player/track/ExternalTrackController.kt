package me.lingci.lib.player.track

import android.net.Uri

/**
 * Request to attach an external stream such as subtitle or audio.
 * The backend is responsible for translating URI/MIME/header details to its native APIs.
 * selectAfterAdd and headers are best-effort because Exo/MPV/IJK support different attachment APIs.
 */
data class ExternalTrackRequest(
    val type: MediaTrackType,
    val uri: Uri,
    val mimeType: String? = null,
    val language: String? = null,
    val title: String? = null,
    val selectAfterAdd: Boolean = true,
    val headers: Map<String, String> = emptyMap(),
)

/** Optional controller for attaching/removing external media streams. */
interface ExternalTrackController {
    fun addExternalTrack(request: ExternalTrackRequest): Boolean

    fun removeExternalTrack(key: MediaTrackKey): Boolean {
        return false
    }
}
