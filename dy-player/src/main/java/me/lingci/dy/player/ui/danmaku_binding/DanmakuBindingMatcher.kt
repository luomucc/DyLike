package me.lingci.dy.player.ui.danmaku_binding

import me.lingci.dy.player.entity.VideoData
import me.lingci.lib.base.storage.entity.FileEntity
import kotlin.math.max

object DanmakuBindingMatcher {

    private val tagTokens = setOf(
        "1080p", "2160p", "720p", "480p", "4k", "bd", "bdrip", "bluray", "web", "webrip",
        "webdl", "web-dl", "x264", "x265", "h264", "h265", "hevc", "avc", "aac", "flac",
        "10bit", "8bit", "gb", "big5", "chs", "cht", "sc", "tc", "中字", "简中", "繁中"
    )

    data class Candidate(
        val file: FileEntity,
        val score: Int,
        val detail: String
    )

    fun match(videoData: VideoData, files: List<FileEntity>): List<Candidate> {
        if (files.isEmpty()) {
            return emptyList()
        }
        val videoName = videoData.name.ifBlank { videoData.videoUrl.substringAfterLast('/') }
        val videoParsed = parse(videoName)
        return files.map { file ->
            val parsed = parse(file.name.ifBlank { file.title })
            val score = score(videoParsed, parsed)
            Candidate(file, score, buildDetail(videoParsed, parsed, score))
        }.filter { it.score > 0 }
            .sortedWith(compareByDescending<Candidate> { it.score }.thenBy { it.file.name })
    }

    fun shouldAutoBind(candidates: List<Candidate>): Boolean {
        val first = candidates.firstOrNull() ?: return false
        val second = candidates.getOrNull(1)
        if (first.score < 70) {
            return false
        }
        return second == null || first.score - second.score >= 12
    }

    private fun buildDetail(videoParsed: ParsedName, fileParsed: ParsedName, score: Int): String {
        val parts = mutableListOf<String>()
        if (videoParsed.episode != null && fileParsed.episode != null) {
            parts += if (videoParsed.episode == fileParsed.episode) "集数一致" else "集数不同"
        }
        parts += "匹配度$score"
        return parts.joinToString(" ")
    }

    private fun score(videoParsed: ParsedName, fileParsed: ParsedName): Int {
        var score = 0
        if (videoParsed.episode != null && fileParsed.episode != null) {
            score += if (videoParsed.episode == fileParsed.episode) 50 else -40
        }
        if (videoParsed.season != null && fileParsed.season != null) {
            score += if (videoParsed.season == fileParsed.season) 10 else -8
        }
        if (videoParsed.normalized.isNotBlank() && fileParsed.normalized.isNotBlank()) {
            if (videoParsed.normalized == fileParsed.normalized) {
                score += 35
            } else {
                if (videoParsed.normalized.contains(fileParsed.normalized) || fileParsed.normalized.contains(videoParsed.normalized)) {
                    score += 20
                }
                score += overlapScore(videoParsed.tokens, fileParsed.tokens)
            }
        }
        return score.coerceAtLeast(0)
    }

    private fun overlapScore(a: Set<String>, b: Set<String>): Int {
        if (a.isEmpty() || b.isEmpty()) {
            return 0
        }
        val common = a.intersect(b)
        if (common.isEmpty()) {
            return 0
        }
        return max(1, (common.size * 20) / max(a.size, b.size))
    }

    private fun parse(name: String): ParsedName {
        val base = name.substringBeforeLast('.')
        val lower = base.lowercase()
        val compact = lower
            .replace(Regex("[【】\\[\\](){}]"), " ")
            .replace(Regex("[_./\\-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        val episode = extractEpisode(compact)
        val season = extractSeason(compact)
        val tokens = compact.split(' ')
            .map { it.trim() }
            .filter { token ->
                token.isNotBlank() && token.length > 1 && token !in tagTokens && token.all { it.isDigit() }.not()
            }
            .toSet()
        return ParsedName(
            normalized = tokens.joinToString(" "),
            episode = episode,
            season = season,
            tokens = tokens
        )
    }

    private fun extractEpisode(text: String): Int? {
        val patterns = listOf(
            Regex("(?:第\\s*)(\\d{1,4})(?:\\s*集)"),
            Regex("(?:ep|e)(\\d{1,4})\\b"),
            Regex("s\\d{1,2}e(\\d{1,4})\\b"),
            Regex("\\b(\\d{1,4})\\b")
        )
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val value = match.groupValues.getOrNull(1)?.toIntOrNull()
            if (value != null) {
                return value
            }
        }
        return null
    }

    private fun extractSeason(text: String): Int? {
        val patterns = listOf(
            Regex("第\\s*(\\d{1,2})\\s*季"),
            Regex("s(\\d{1,2})e\\d{1,4}"),
            Regex("season\\s*(\\d{1,2})")
        )
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val value = match.groupValues.getOrNull(1)?.toIntOrNull()
            if (value != null) {
                return value
            }
        }
        return null
    }

    private data class ParsedName(
        val normalized: String,
        val episode: Int?,
        val season: Int?,
        val tokens: Set<String>
    )
}
