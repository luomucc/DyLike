package me.lingci.dy.player.ui.media_scan

data class ScanFolderResult(
    val path: String,
    val name: String,
    val videoCount: Int,
    var existsInLibrary: Boolean = false,
    var selected: Boolean = false
)
