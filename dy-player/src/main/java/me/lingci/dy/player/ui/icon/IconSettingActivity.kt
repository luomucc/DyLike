package me.lingci.dy.player.ui.icon

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.lingci.dy.player.databinding.ActivityIconChangeBinding
import me.lingci.dy.player.ui.main.MainActivity
import me.lingci.dy.player.util.SpUtil
import me.lingci.lib.base.util.ToastUtil

/**
 * 图标设置
 */
class IconSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIconChangeBinding

    // 当前选中的别名（默认初始为默认图标）
    private lateinit var selectedAlias: ComponentName

    private val spUtil: SpUtil by lazy { SpUtil(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIconChangeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // activity-alias 由 namespace 解析，不能使用带 applicationIdSuffix 的 packageName 拼接。
        val aliasDefault = ComponentName(this, "me.lingci.dy.player.DefaultAlias")
        val aliasLine = ComponentName(this, "me.lingci.dy.player.LineAlias")
        selectedAlias = if (spUtil.iconDefault) {
            aliasDefault
        } else {
            aliasLine
        }
        binding.tvSelect.text = if (spUtil.iconDefault) {
            "左边"
        } else {
            "右边"
        }

        binding.btnDefault.setOnClickListener {
            if (selectedAlias == aliasDefault) {
                return@setOnClickListener
            }
            spUtil.iconDefault = true
            selectedAlias = aliasDefault
            switchLauncherIcon(selectedAlias, listOf(aliasDefault, aliasLine))
        }

        binding.btnLine.setOnClickListener {
            if (selectedAlias == aliasLine) {
                return@setOnClickListener
            }
            spUtil.iconDefault = false
            selectedAlias = aliasLine
            switchLauncherIcon(selectedAlias, listOf(aliasDefault, aliasLine))
        }
    }

    /**
     * 核心方法：切换启动图标（禁用所有别名 → 启用选中的别名）
     */
    private fun switchLauncherIcon(targetAlias: ComponentName, list: List<ComponentName>) {
        val packageManager = packageManager

        // 步骤1：禁用所有图标别名（避免多个Launcher入口）
        list.forEach { alias ->
            packageManager.setComponentEnabledSetting(
                alias,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP // 不杀死应用
            )
        }

        // 步骤2：启用选中的别名
        packageManager.setComponentEnabledSetting(
            targetAlias,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // 步骤4：提示用户
        ToastUtil.showToast(this, "图标已修改，若未生效请稍后，或放弃")

        // 可选：返回主页面
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun changeLuncher(name: String) {
        val pm = packageManager
        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
        )
        pm.setComponentEnabledSetting(
            ComponentName(this, name),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )
        //Intent 重启 Launcher 应用
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        val resolves = pm.queryIntentActivities(intent, 0)
        for (res in resolves) {
            if (res.activityInfo != null) {
                val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                am.killBackgroundProcesses(res.activityInfo.packageName)
            }
        }
    }

}
