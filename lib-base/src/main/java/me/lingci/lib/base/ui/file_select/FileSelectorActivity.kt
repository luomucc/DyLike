package me.lingci.lib.base.ui.file_select

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import me.lingci.lib.base.R
import me.lingci.lib.base.databinding.ActivityFileSelectorBinding
import me.lingci.lib.base.storage.entity.FileEntity
import me.lingci.lib.base.ui.BaseDisplayActivity
import me.lingci.lib.base.util.FileOperator
import me.lingci.lib.base.util.JsonUtil
import me.lingci.lib.base.util.SpBase
import me.lingci.lib.base.util.subFileName
import java.io.File

/**
 *   @author : happyc
 *   time    : 2024/09/21
 *   desc    :
 *   version : 1.0
 */
class FileSelectorActivity : BaseDisplayActivity() {

    companion object {

        const val KEY_PATH = "path"
        const val KEY_FOLDER = "folder"
        private const val KEY_MULTI_MODE = "multiMode"
        private const val KEY_FILE_TYPE = "fileType"
        private const val KEY_FOLDER_MODE = "folderMode"

        fun Intent.selectorFile(): List<String> {
            return getStringArrayListExtra(KEY_PATH) ?: emptyList()
        }

        fun Intent.selectorFile(onSelect:(paths: List<String>, folderMode: Boolean) -> Unit) {
            onSelect.invoke(
                getStringArrayListExtra(KEY_PATH)?: emptyList(),
                getBooleanExtra(KEY_FOLDER, false)
            )
        }

        fun start(context: Activity, launcher: ActivityResultLauncher<Intent>) {
            val intent = Intent(context, FileSelectorActivity::class.java)
            intent.putExtra(KEY_MULTI_MODE, true)
            intent.putExtra(KEY_FOLDER_MODE, false)
            launcher.launch(intent)
        }

        fun start(
            context: Activity,
            fileType: ArrayList<String>,
            launcher: ActivityResultLauncher<Intent>
        ) {
            val intent = Intent(context, FileSelectorActivity::class.java)
            intent.putExtra(KEY_MULTI_MODE, true)
            intent.putExtra(KEY_FOLDER_MODE, false)
            intent.putStringArrayListExtra(KEY_FILE_TYPE, fileType)
            launcher.launch(intent)
        }

        fun startSingle(context: Activity, launcher: ActivityResultLauncher<Intent>) {
            val intent = Intent(context, FileSelectorActivity::class.java)
            intent.putExtra(KEY_MULTI_MODE, false)
            intent.putExtra(KEY_FOLDER_MODE, false)
            launcher.launch(intent)
        }

        fun startSingle(
            context: Activity,
            fileType: ArrayList<String>,
            launcher: ActivityResultLauncher<Intent>
        ) {
            val intent = Intent(context, FileSelectorActivity::class.java)
            intent.putExtra(KEY_MULTI_MODE, false)
            intent.putExtra(KEY_FOLDER_MODE, false)
            intent.putStringArrayListExtra(KEY_FILE_TYPE, fileType)
            launcher.launch(intent)
        }

        fun startFolder(context: Activity, launcher: ActivityResultLauncher<Intent>) {
            val intent = Intent(context, FileSelectorActivity::class.java)
            intent.putExtra(KEY_MULTI_MODE, false)
            intent.putExtra(KEY_FOLDER_MODE, true)
            launcher.launch(intent)
        }

    }

    private val spBase: SpBase by lazy { SpBase(this) }
    private lateinit var binding: ActivityFileSelectorBinding
    private lateinit var folderUseItemAdapter: FolderUseItemAdapter
    private lateinit var folderIndexItemAdapter: FolderIndexItemAdapter
    private lateinit var fileSelectorItemAdapter: FileSelectorItemAdapter
    private var extensions = FileOperator.XML_EXTENSIONS
    private val folderList = mutableListOf<String>(
        FileOperator.downloadFolder.path,
        FileOperator.buildDownFile("弹目").path,
    )
    private var isFolder = false
    private var currentFolder = FileOperator.rootFolder.path

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFileSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        init()
    }

    fun init() {
        intent.getStringArrayListExtra(KEY_FILE_TYPE)?.let {
            extensions = it
        }
        isFolder = intent.getBooleanExtra(KEY_FOLDER_MODE, false)
        initRecycle()
        initBackPressed()
        initStorage()
    }

    private fun initStorage() {
        val root = FileEntity()
        root.name = "root"
        root.path = FileOperator.rootFolder.path
        folderIndexItemAdapter.addItem(root)
        if (isFolder) {
            FileOperator.getSortedFolders(FileOperator.rootFolder)
        } else {
            FileOperator.getSortedFiles(FileOperator.rootFolder, true, extensions)
        }.map { FileEntity(it) }.let { files ->
            binding.recyclerView.post {
                fileSelectorItemAdapter.updateData(files)
            }
        }
    }

    private fun initRecycle() {
        // 常用目录
        binding.folderRecyclerView.visibility = View.VISIBLE
        val flexboxLayoutManager = FlexboxLayoutManager(this)
        flexboxLayoutManager.flexDirection = FlexDirection.ROW
        flexboxLayoutManager.flexWrap = FlexWrap.WRAP
        flexboxLayoutManager.justifyContent = JustifyContent.FLEX_START
        binding.folderRecyclerView.layoutManager = flexboxLayoutManager
        val customUse = mutableListOf<String>()
        spBase.useFolders?.let {
            customUse.addAll(JsonUtil.toList<String>(it))
        }
        folderUseItemAdapter = FolderUseItemAdapter((folderList + customUse).toMutableList())
        binding.folderRecyclerView.adapter = folderUseItemAdapter
        folderUseItemAdapter.onItemClick { item, _ ->
            folderIndexItemAdapter.changeDir(item)
            nextDir(FileEntity(path = item, name = item.subFileName()))
        }

        // 目录导航
        binding.titleRecyclerView.layoutManager =
            LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        folderIndexItemAdapter = FolderIndexItemAdapter(arrayListOf())
        binding.titleRecyclerView.adapter = folderIndexItemAdapter
        folderIndexItemAdapter.onItemClick { item, position ->
            folderIndexItemAdapter.removeEnd(position)
            nextDir(item)
        }
        // 文件列表
        binding.recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        fileSelectorItemAdapter = FileSelectorItemAdapter(arrayListOf())
        binding.recyclerView.adapter = fileSelectorItemAdapter
        fileSelectorItemAdapter.onItemClick { item, _ ->
            if (!item.isFile) {
                folderIndexItemAdapter.addItem(item)
                nextDir(item)
            }
        }
        fileSelectorItemAdapter.onItemSelect { item, _ ->
            onResult(mutableListOf(item.path))
        }

        fileSelectorItemAdapter.onFolderMenu { v, item ->
            val use = folderUseItemAdapter.getData().contains(item.path)
            val popupMenu = PopupMenu(this@FileSelectorActivity, v)
            popupMenu.menu.apply {
                if (use) {
                    add(1, 2, 2, "取消常用")
                } else {
                    add(1, 1, 1, "设为常用")
                }
            }
            popupMenu.setOnMenuItemClickListener {
                if (it.itemId == 1) {
                    folderUseItemAdapter.addItem(item.path)
                } else {
                    folderUseItemAdapter.removeItem(item.path)
                }
                saveFolderUse()
                return@setOnMenuItemClickListener true
            }
            popupMenu.show()
        }

        intent.getBooleanExtra(KEY_MULTI_MODE, true).let { multi ->
            fileSelectorItemAdapter.changeSelectMode(multi)
        }
    }

    private fun saveFolderUse() {
        val list = mutableListOf<String>()
        list.addAll(folderUseItemAdapter.getData())
        list.removeAll(folderList)
        spBase.useFolders = JsonUtil.toJsonString(list)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        intent.getBooleanExtra(KEY_MULTI_MODE, true).let { multi ->
            if (multi) {
                menuInflater.inflate(R.menu.menu_select_action, menu)
            }
        }
        intent.getBooleanExtra(KEY_FOLDER_MODE, false).let { folder ->
            if (folder) {
                menuInflater.inflate(R.menu.menu_select_action, menu)
                menu.children.forEach {
                    if (it.itemId != R.id.menu_positive) {
                        it.isVisible = false
                    }
                }
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_positive -> {
                if (isFolder) {
                    onResult(arrayListOf(currentFolder))
                } else {
                    fileSelectorItemAdapter.getSelect().let {
                        if (it.isNotEmpty()) {
                            onResult(it)
                        } else {
                            finish()
                        }
                    }
                }
                true
            }

            R.id.menu_select_all -> {
                fileSelectorItemAdapter.selectAll()
                true
            }

            R.id.menu_select_invert -> {
                fileSelectorItemAdapter.selectInvert()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onResult(list: MutableList<String>) {
        val intent = Intent()
        intent.putStringArrayListExtra(KEY_PATH, ArrayList(list))
        intent.putExtra(KEY_FOLDER, isFolder)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun initBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (folderIndexItemAdapter.itemCount > 1) {
                    folderIndexItemAdapter.removeEnd(folderIndexItemAdapter.itemCount - 2)
                    nextDir(folderIndexItemAdapter.getData().last())
                } else {
                    finish()
                }
            }
        })
    }

    private fun nextDir(item: FileEntity) {
        if (isFolder) {
            FileOperator.getSortedFolders(File(item.path))
        } else {
            FileOperator.getSortedFiles(File(item.path), true, extensions)
        }.map { FileEntity(it) }.let { files ->
            currentFolder = item.path
            binding.recyclerView.post {
                fileSelectorItemAdapter.updateData(files)
            }
        }
    }

}