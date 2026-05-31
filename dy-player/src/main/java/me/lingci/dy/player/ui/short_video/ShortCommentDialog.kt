package me.lingci.dy.player.ui.short_video

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import me.lingci.dy.player.R
import me.lingci.dy.player.databinding.DialogShortCommentBinding
import me.lingci.dy.player.ui.long_video.PlayInfo
import me.lingci.dy.player.util.AppUtil
import me.lingci.lib.base.util.AppFile
import me.lingci.lib.base.util.JsonUtil
import me.lingci.lib.base.util.Log
import me.lingci.lib.dm.view.entity.xml.DmItem

/**
 * @author : happyc
 * time    : 2026/01/31
 * desc    : 短视频评论/笔记
 * version : 1.0
 */
open class ShortCommentDialog() : BottomSheetDialogFragment() {

    companion object{

        const val KEY_VIDEO_ID = "video_id"

    }

    private lateinit var binding: DialogShortCommentBinding
    private lateinit var commentItemAdapter: ShortCommentItemAdapter
    private var onComment: ((videoId: String, item: DmItem, playInfo: PlayInfo?) -> PlayInfo?)? = null
    private var onDeleteComment: ((videoId: String, position: Int, playInfo: PlayInfo?) -> PlayInfo?)? = null

    private var videoId = ""
    private var playInfo: PlayInfo? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_Material3_DayNight_BottomSheetDialog)
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        arguments?.getString(KEY_VIDEO_ID)?.let { newVideoId ->
            if (newVideoId.isNotEmpty()) {
                if (newVideoId != videoId) {
                    videoId = newVideoId
                }
                loadPlayInfo()
            }
        }
    }

    private fun loadPlayInfo() {
        val file = AppFile(requireContext()).buildCustom("info", "${videoId}.json")
        if (file.exists() && file.canRead()) {
            playInfo = JsonUtil.toEntity<PlayInfo>(file.readText())
            commentItemAdapter.updateData(playInfo!!.comments)
        } else {
            playInfo = null
            commentItemAdapter.updateData(mutableListOf())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogShortCommentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        Log.d(this@ShortCommentDialog, "onViewStateRestored")
        super.onViewStateRestored(savedInstanceState)
    }

    override fun onDestroyView() {
        Log.d(this@ShortCommentDialog, "onDestroyView")
        super.onDestroyView()
    }

    fun onComment(onComment: ((videoId: String, item: DmItem, playInfo: PlayInfo?) -> PlayInfo?)) {
        this.onComment = onComment
    }

    fun onDeleteComment(onDeleteComment: ((videoId: String, position: Int, playInfo: PlayInfo?) -> PlayInfo?)) {
        this.onDeleteComment = onDeleteComment
    }

    private fun init() {
        commentItemAdapter = ShortCommentItemAdapter(mutableListOf())
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = commentItemAdapter

        commentItemAdapter.onItemMoreClick { view, item, position ->
            val popupMenu = PopupMenu(requireContext(), view)
            popupMenu.menu.apply {
                add(1, 1, 1, "删除当前笔记")
            }
            popupMenu.setOnMenuItemClickListener {
                if (it.itemId == 1) {
                    commentItemAdapter.removeItem(position)
                    playInfo = onDeleteComment?.invoke(videoId, position, playInfo)
                }
                return@setOnMenuItemClickListener true
            }
            popupMenu.show()
        }

        binding.btnSend.setOnClickListener {
            val comment = binding.inputValue.editText?.text.toString()
            if (comment.isNotBlank()) {
                onComment?.let { callback ->
                    val item = DmItem(content = comment, extend = AppUtil.formatNow("yyyy-MM-dd HH:mm:ss"))
                    commentItemAdapter.addItem(item)
                    playInfo = callback.invoke(
                        videoId,
                        item,
                        playInfo
                    )
                    binding.inputValue.editText?.setText("")
                }

            }
        }
    }

}
