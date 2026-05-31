package me.lingci.lib.player.subtitle

/** Small MIME helper kept in dkplayer-java so player-ui does not need Media3 MimeTypes. */
object SubtitleMimeTypes {
    const val APPLICATION_SUBRIP = "application/x-subrip"
    const val TEXT_VTT = "text/vtt"
    const val TEXT_SSA = "text/x-ssa"
    const val APPLICATION_TTML = "application/ttml+xml"

    fun fromPath(path: String?): String? {
        val extension = path
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return when (extension) {
            "srt" -> APPLICATION_SUBRIP
            "vtt", "webvtt" -> TEXT_VTT
            "ssa", "ass" -> TEXT_SSA
            "ttml", "xml", "dfxp" -> APPLICATION_TTML
            else -> null
        }
    }
}
