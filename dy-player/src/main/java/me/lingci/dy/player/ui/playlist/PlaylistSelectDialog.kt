package me.lingci.dy.player.ui.playlist

import android.content.Context
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import me.lingci.dy.player.R
import me.lingci.dy.player.entity.VideoData
import me.lingci.dy.player.util.LibraryCompat
import me.lingci.dy.player.util.SpUtil
import me.lingci.lib.base.util.ToastUtil

object PlaylistSelectDialog {

    fun show(context: Context, videos: List<VideoData>) {
        val spUtil = SpUtil(context)
        val playlists = LibraryCompat.loadPlaylist(spUtil)
        val playlistNames = playlists.map { "${it.title} (${it.items.size})" }
            .plus(context.getString(R.string.hint_playlist_create_new))
            .toTypedArray()

        AlertDialog.Builder(context)
            .setTitle(R.string.hint_playlist_select)
            .setItems(playlistNames) { _, which ->
                if (which == playlists.size) {
                    showCreateAndAdd(context, videos)
                } else {
                    val playlist = playlists[which]
                    LibraryCompat.addToPlaylist(spUtil, playlist.id, videos)
                    ToastUtil.showToast(context, context.getString(R.string.hint_playlist_video_added))
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showCreateAndAdd(context: Context, videos: List<VideoData>) {
        val spUtil = SpUtil(context)
        val editText = EditText(context).apply {
            hint = context.getString(R.string.hint_playlist_name)
            isSingleLine = true
        }
        AlertDialog.Builder(context)
            .setTitle(R.string.hint_playlist_add)
            .setView(editText)
            .setPositiveButton(R.string.action_confirmed) { dialog, _ ->
                val name = editText.text?.toString()?.trim().orEmpty()
                if (name.isNotBlank()) {
                    val playlist = LibraryCompat.createPlaylist(spUtil, name)
                    LibraryCompat.addToPlaylist(spUtil, playlist.id, videos)
                    ToastUtil.showToast(context, context.getString(R.string.hint_playlist_created))
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

}
