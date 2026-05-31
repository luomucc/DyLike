package me.lingci.lib.player.mpv.track

import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.MPVNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.doikki.videoplayer.util.L
import org.json.JSONObject

/**
 * Parses MPV's track-list property and keeps selected-track state in Kotlin data classes.
 * This is intentionally internal to player-mpv; app-facing code should use MediaTrackProvider.
 */
class MpvTrackManager {

    private val _trackList = MutableStateFlow(TrackList())
    val trackList: StateFlow<TrackList> = _trackList.asStateFlow()

    private var currentVideoTrackId: Int = -1
    private var currentAudioTrackId: Int = -1
    private var currentSubtitleTrackId: Int = -1

    val videoTracks: List<TrackInfo> get() = _trackList.value.videoTracks
    val audioTracks: List<TrackInfo> get() = _trackList.value.audioTracks
    val subtitleTracks: List<TrackInfo> get() = _trackList.value.subtitleTracks

    fun updateFromMPVNode(node: MPVNode) {
        try {
            val trackListJson = node.toJson()
            L.d("MpvTrackManager Parsing track-list: $trackListJson")
            parseTrackListFromJson(trackListJson)
        } catch (e: Exception) {
            L.e("MpvTrackManager Error parsing track-list: ${e.message}")
        }
    }

    private fun parseTrackListFromJson(json: String) {
        try {
            val videoTracks = mutableListOf<TrackInfo>()
            val audioTracks = mutableListOf<TrackInfo>()
            val subtitleTracks = mutableListOf<TrackInfo>()

            val jsonArray = org.json.JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val trackObj = jsonArray.optJSONObject(i) ?: continue
                val track = parseTrackFromJson(trackObj) ?: continue
                
                when (track.type) {
                    TrackType.VIDEO -> videoTracks.add(track)
                    TrackType.AUDIO -> audioTracks.add(track)
                    TrackType.SUBTITLE -> subtitleTracks.add(track)
                }
            }

            _trackList.value = TrackList(
                videoTracks = videoTracks,
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks
            )
            
            L.d("MpvTrackManager Updated tracks: video=${videoTracks.size}, audio=${audioTracks.size}, subtitle=${subtitleTracks.size}")
        } catch (e: Exception) {
            L.e("MpvTrackManager Error parsing track JSON: ${e.message}")
        }
    }

    private fun parseTrackFromJson(obj: JSONObject): TrackInfo? {
        val id = obj.optInt("id", -1)
        if (id < 0) return null
        
        val typeStr = obj.optString("type", "")
        val type = TrackType.fromString(typeStr) ?: return null
        
        val title = obj.optString("title").takeIf { it.isNotBlank() }
        val lang = obj.optString("lang").takeIf { it.isNotBlank() }
        val isDefault = obj.optBoolean("default", false)
        val isSelected = obj.optBoolean("selected", false)
        val isExternal = obj.optBoolean("external", false)
        val codec = obj.optString("codec").takeIf { it.isNotBlank() }
        val filePath = obj.optString("external-filename").takeIf { it.isNotBlank() }
        
        return TrackInfo(
            id = id,
            type = type,
            title = title,
            lang = lang,
            isDefault = isDefault,
            isSelected = isSelected,
            isExternal = isExternal,
            codec = codec,
            filePath = filePath
        )
    }

    fun updateCurrentTracks(mpv: MPV) {
        try {
            // MPV returns numeric ids for selected tracks. Missing/disabled tracks are represented
            // as null/no, which we normalize to -1 for local selection state.
            currentVideoTrackId = mpv.getPropertyLong("vid")?.toInt() ?: -1
            currentAudioTrackId = mpv.getPropertyLong("aid")?.toInt() ?: -1
            currentSubtitleTrackId = mpv.getPropertyLong("sid")?.toInt() ?: -1
            
            updateTrackSelectionStates()
        } catch (e: Exception) {
            L.e("MpvTrackManager Error updating current tracks: ${e.message}")
        }
    }

    private fun updateTrackSelectionStates() {
        val currentList = _trackList.value
        
        val updatedVideoTracks = currentList.videoTracks.map { 
            it.copy(isSelected = it.id == currentVideoTrackId)
        }
        val updatedAudioTracks = currentList.audioTracks.map { 
            it.copy(isSelected = it.id == currentAudioTrackId)
        }
        val updatedSubtitleTracks = currentList.subtitleTracks.map { 
            it.copy(isSelected = it.id == currentSubtitleTrackId)
        }
        
        _trackList.value = TrackList(
            videoTracks = updatedVideoTracks,
            audioTracks = updatedAudioTracks,
            subtitleTracks = updatedSubtitleTracks
        )
    }

    fun selectVideoTrack(mpv: MPV, trackId: Int): Boolean {
        return try {
            mpv.setPropertyLong("vid", trackId.toLong())
            currentVideoTrackId = trackId
            updateTrackSelectionStates()
            L.d("MpvTrackManager Selected video track: $trackId")
            true
        } catch (e: Exception) {
            L.e("MpvTrackManager Error selecting video track: ${e.message}")
            false
        }
    }

    fun selectAudioTrack(mpv: MPV, trackId: Int): Boolean {
        return try {
            mpv.setPropertyLong("aid", trackId.toLong())
            currentAudioTrackId = trackId
            updateTrackSelectionStates()
            L.d("MpvTrackManager Selected audio track: $trackId")
            true
        } catch (e: Exception) {
            L.e("MpvTrackManager Error selecting audio track: ${e.message}")
            false
        }
    }

    fun selectSubtitleTrack(mpv: MPV, trackId: Int): Boolean {
        return try {
            mpv.setPropertyLong("sid", trackId.toLong())
            currentSubtitleTrackId = trackId
            updateTrackSelectionStates()
            L.d("MpvTrackManager Selected subtitle track: $trackId")
            true
        } catch (e: Exception) {
            L.e("MpvTrackManager Error selecting subtitle track: ${e.message}")
            false
        }
    }

    fun disableSubtitle(mpv: MPV): Boolean {
        return try {
            mpv.setPropertyString("sid", "no")
            currentSubtitleTrackId = -1
            updateTrackSelectionStates()
            L.d("MpvTrackManager Subtitle disabled")
            true
        } catch (e: Exception) {
            L.e("MpvTrackManager Error disabling subtitle: ${e.message}")
            false
        }
    }

    fun addExternalSubtitle(mpv: MPV, path: String, flags: String, title: String? = null, lang: String? = null): Boolean {
        return try {
            // sub-add/audio-add make MPV render subtitles internally; no SubtitleCueProvider bridge
            // is required unless a future UI needs custom subtitle styling for MPV.
            val args = mutableListOf("sub-add", path, flags)
            if (title != null || lang != null) {
                args.add(title.orEmpty())
            }
            lang?.let { args.add(it) }
            
            mpv.command(*args.toTypedArray())
            L.d("MpvTrackManager Added external subtitle: $path")
            true
        } catch (e: Exception) {
            L.e("MpvTrackManager Error adding external subtitle: ${e.message}")
            false
        }
    }

    fun addExternalAudio(mpv: MPV, path: String, flags: String, title: String? = null, lang: String? = null): Boolean {
        return try {
            val args = mutableListOf("audio-add", path, flags)
            if (title != null || lang != null) {
                args.add(title.orEmpty())
            }
            lang?.let { args.add(it) }
            
            mpv.command(*args.toTypedArray())
            L.d("MpvTrackManager Added external audio: $path")
            true
        } catch (e: Exception) {
            L.e("MpvTrackManager Error adding external audio: ${e.message}")
            false
        }
    }

    fun removeTrack(mpv: MPV, trackId: Int, type: TrackType): Boolean {
        return try {
            val typeArg = when (type) {
                TrackType.VIDEO -> "video"
                TrackType.AUDIO -> "audio"
                TrackType.SUBTITLE -> "sub"
            }
            mpv.command("track-remove", "$typeArg/$trackId")
            L.d("MpvTrackManager Removed track: $typeArg/$trackId")
            true
        } catch (e: Exception) {
            L.e("MpvTrackManager Error removing track: ${e.message}")
            false
        }
    }

    fun cycleSubtitle(mpv: MPV): Boolean {
        return try {
            mpv.command("cycle", "sub")
            updateCurrentTracks(mpv)
            true
        } catch (e: Exception) {
            L.e("MpvTrackManager Error cycling subtitle: ${e.message}")
            false
        }
    }

    fun cycleAudio(mpv: MPV): Boolean {
        return try {
            mpv.command("cycle", "audio")
            updateCurrentTracks(mpv)
            true
        } catch (e: Exception) {
            L.e("MpvTrackManager Error cycling audio: ${e.message}")
            false
        }
    }

    fun clear() {
        _trackList.value = TrackList()
        currentVideoTrackId = -1
        currentAudioTrackId = -1
        currentSubtitleTrackId = -1
    }

    fun getSelectedSubtitleTrack(): TrackInfo? {
        return subtitleTracks.find { it.isSelected }
    }

    fun getSelectedAudioTrack(): TrackInfo? {
        return audioTracks.find { it.isSelected }
    }

    fun getSelectedVideoTrack(): TrackInfo? {
        return videoTracks.find { it.isSelected }
    }

}
