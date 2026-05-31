package me.lingci.dy.player.ui.playlist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.LinearLayoutManager
import me.lingci.dy.player.R
import me.lingci.dy.player.databinding.ActivityPlaylistBinding
import me.lingci.dy.player.entity.MediaData
import me.lingci.dy.player.ui.media_detail.MediaDetailActivity
import me.lingci.dy.player.util.LibraryCompat
import me.lingci.dy.player.util.SpUtil
import me.lingci.lib.base.ui.BaseActivity
import me.lingci.lib.base.ui.setEmptyView
import me.lingci.lib.base.util.ToastUtil

/**
 * 播放列表
 */
class PlaylistActivity : BaseActivity(), MenuProvider {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, PlaylistActivity::class.java))
        }
    }

    private var _binding: ActivityPlaylistBinding? = null
    private val binding get() = _binding!!
    private val spUtil by lazy { SpUtil(this) }
    private lateinit var playlistAdapter: PlaylistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityPlaylistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        init()
    }

    private fun init() {
        playlistAdapter = PlaylistAdapter(mutableListOf())
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = playlistAdapter
        binding.recyclerView.setEmptyView(emptyText = getString(R.string.hint_playlist_empty))

        playlistAdapter.onItemClick { item, _ ->
            val synced = LibraryCompat.syncPlaylistVideos(spUtil, item.id) ?: item
            MediaDetailActivity.start(this@PlaylistActivity, synced)
        }
        playlistAdapter.onItemLongClick { item, position ->
            showItemMenu(item, position)
        }

        loadPlaylists()
    }

    override fun onStart() {
        super.onStart()
        addMenuProvider(this, this)
    }

    override fun onStop() {
        removeMenuProvider(this)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        loadPlaylists()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_playlist, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.menu_playlist_add) {
            showCreateDialog()
            return true
        }
        return false
    }

    private fun loadPlaylists() {
        val playlists = LibraryCompat.loadPlaylist(spUtil)
        playlistAdapter.updateData(playlists)
    }

    private fun showCreateDialog() {
        val editText = android.widget.EditText(this).apply {
            hint = getString(R.string.hint_playlist_name)
            isSingleLine = true
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.hint_playlist_add)
            .setView(editText)
            .setPositiveButton(R.string.action_confirmed) { dialog, _ ->
                val name = editText.text?.toString()?.trim().orEmpty()
                if (name.isNotBlank()) {
                    LibraryCompat.createPlaylist(spUtil, name)
                    ToastUtil.showToast(this, getString(R.string.hint_playlist_created))
                    loadPlaylists()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showItemMenu(item: MediaData, position: Int) {
        val menuItems = arrayOf(
            getString(R.string.action_rename),
            getString(R.string.action_delete)
        )
        AlertDialog.Builder(this)
            .setTitle(item.title)
            .setItems(menuItems) { _, which ->
                when (which) {
                    0 -> renamePlaylist(item)
                    1 -> deletePlaylist(item, position)
                }
            }
            .show()
    }

    private fun renamePlaylist(playlist: MediaData) {
        val editText = android.widget.EditText(this).apply {
            setText(playlist.title)
            setSelection(text.length)
            isSingleLine = true
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.action_rename)
            .setView(editText)
            .setPositiveButton(R.string.action_confirmed) { dialog, _ ->
                val name = (editText.text?.toString()?.trim().orEmpty())
                if (name.isNotBlank()) {
                    LibraryCompat.renamePlaylist(spUtil, playlist.id, name)
                    ToastUtil.showToast(this, getString(R.string.hint_playlist_renamed))
                    loadPlaylists()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun deletePlaylist(playlist: MediaData, position: Int) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.action_delete))
            .setMessage("确定删除播放列表「${playlist.title}」？")
            .setPositiveButton(R.string.action_confirmed) { _, _ ->
                LibraryCompat.deletePlaylist(spUtil, playlist.id)
                ToastUtil.showToast(this, getString(R.string.hint_playlist_deleted))
                playlistAdapter.removeItem(position)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

}
