package me.lingci.lib.player.track

/**
 * Optional read side for player track state. Implementing this does not imply the backend can
 * select or disable tracks; use MediaTrackController for mutations.
 */
interface MediaTrackProvider {
    fun getMediaTracks(): MediaTrackSnapshot

    fun setOnMediaTracksChangedListener(listener: OnMediaTracksChangedListener?)
}

/** Listener receives complete snapshots so UI can rebuild panels without retaining backend objects. */
fun interface OnMediaTracksChangedListener {
    fun onMediaTracksChanged(snapshot: MediaTrackSnapshot)
}
