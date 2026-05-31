package me.lingci.lib.dm.view.widget

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.text.TextPaint
import android.util.Log
import android.view.LayoutInflater
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.android.AndroidDisplayer.DisplayerConfig
import master.flame.danmaku.danmaku.model.android.ViewCacheStuffer
import me.lingci.lib.dm.view.widget.TextViewCacheStuffer.GradientViewHolder
import me.lingci.lib.dm.view.databinding.TextViewCacheBinding

/**
 * @author : happyc
 * time    : 2025/03/12
 * desc    : [](https://github.com/bilibili/DanmakuFlameMaster/blob/master/Sample/src/main/java/com/sample/UglyViewCacheStufferSampleActivity.java)
 * version : 1.0
 */
internal class TextViewCacheStuffer(
    private val context: Context
) : ViewCacheStuffer<GradientViewHolder>() {

    override fun onCreateViewHolder(viewType: Int): GradientViewHolder {
        Log.e("DFM", "onCreateViewHolder:$viewType")
        return GradientViewHolder(
            TextViewCacheBinding.inflate(
                LayoutInflater.from(
                    context
                )
            )
        )
    }

    override fun onBindViewHolder(
        viewType: Int,
        viewHolder: GradientViewHolder,
        danmaku: BaseDanmaku,
        displayerConfig: DisplayerConfig,
        paint: TextPaint
    ) {
        val colors = intArrayOf(-0x10000, -0xff0100, -0xffff01)
        val positions = floatArrayOf(0f, 0.5f, 1f)
        val shader = LinearGradient(
            0f,
            0f,
            viewHolder.binding.tvTitle.width + 0f,
            0f,
            colors,
            positions,
            Shader.TileMode.CLAMP
        )
        viewHolder.binding.tvTitle.paint.shader = shader
        viewHolder.binding.tvTitle.text = danmaku.text
    }

    class GradientViewHolder(
        val binding: TextViewCacheBinding
    ) : ViewHolder(binding.root)

}