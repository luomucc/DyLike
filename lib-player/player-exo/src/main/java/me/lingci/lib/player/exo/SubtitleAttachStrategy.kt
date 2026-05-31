package me.lingci.lib.player.exo

/**
 *   @author : happyc
 *   time    : 2026/04/08
 *   desc    : Exo-only strategy for attaching external subtitles.
 *   version : 1.0
 */
enum class SubtitleAttachStrategy {

    /** Rebuild the MediaItem with SubtitleConfiguration; broadly compatible but heavier. */
    REBUILD_MEDIA_ITEM,
    /** Merge a SingleSampleMediaSource into the current source; preserves the base item better. */
    MERGE_SUBTITLE_SOURCE

}
