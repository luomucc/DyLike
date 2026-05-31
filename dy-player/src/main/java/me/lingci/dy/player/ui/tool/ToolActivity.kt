package me.lingci.dy.player.ui.tool

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import me.lingci.dy.player.databinding.ActivityToolBinding


/**
 *   @author : happyc
 *   time    : 2025/10/21
 *   desc    :
 *   version : 1.0
 */
@Suppress("unused", "SpellCheckingInspection")
class ToolActivity : AppCompatActivity() {

    companion object {

        const val KEY_PAGE = "page"
        const val PAGE_DANMAKU = "danmaku"
        const val PAGE_PLAYER = "player"
        const val PAGE_BACKUP = "backup"
        const val PAGE_LAB = "lab"

        fun start(context: Context, clazz: String) {
            val i = Intent(context, ToolActivity::class.java)
            i.putExtra(KEY_PAGE, clazz)
            context.startActivity(i)
        }

    }

    private lateinit var binding: ActivityToolBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityToolBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    fun initView() {
        val page = intent?.getStringExtra(KEY_PAGE)
        if (page.isNullOrBlank()) {
            finish()
            return
        }

        val fragment: Fragment? = when (page) {
            PAGE_DANMAKU -> DanmakuSettingsFragment()
            PAGE_PLAYER -> PlayerSettingsFragment()
            PAGE_BACKUP -> BackupSettingsFragment()
            PAGE_LAB -> LabSettingsFragment()
            else -> null
        }

        if (fragment == null) {
            finish()
            return
        }

        supportFragmentManager
            .beginTransaction()
            .replace(binding.root.id, fragment)
            .commit()
    }

}