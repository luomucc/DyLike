package me.lingci.lib.player.subtitle

/**
 * Optional provider for self-rendered subtitle overlays. MPV can skip this because it renders
 * subtitles internally; Exo implements it by mapping Media3 cues to SubtitleCueGroup.
 */
interface SubtitleCueProvider {
    fun setSubtitleCueListener(listener: SubtitleCueListener?)
}

/** Consumer for generic subtitle cues, usually SubtitleControlView. */
fun interface SubtitleCueListener {
    fun onSubtitleCues(cues: SubtitleCueGroup)
}
