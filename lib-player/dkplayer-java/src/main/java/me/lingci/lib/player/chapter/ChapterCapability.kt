package me.lingci.lib.player.chapter

/** Optional read side for chapter metadata. */
interface ChapterProvider {
    fun getChapters(): List<ChapterNode>

    fun getCurrentChapter(): ChapterNode?

    fun setOnChapterChangeListener(listener: OnChapterChangeListener?)
}

/** Optional write side for chapter navigation. */
interface ChapterController {
    fun seekToChapter(index: Int): Boolean

    fun seekToNextChapter(): Boolean {
        return false
    }

    fun seekToPreviousChapter(): Boolean {
        return false
    }
}

/** Listener for chapter list and current chapter changes. */
interface OnChapterChangeListener {
    fun onChaptersLoaded(chapters: List<ChapterNode>)

    fun onCurrentChapterChanged(chapter: ChapterNode?)
}
