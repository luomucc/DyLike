package me.lingci.lib.player.mpv.render

import android.content.Context
import me.lingci.lib.base.util.Log
import xyz.doikki.videoplayer.render.IRenderView
import xyz.doikki.videoplayer.render.RenderViewFactory

class MpvSurfaceRenderViewFactory : RenderViewFactory() {

    override fun createRenderView(context: Context): IRenderView {
        return try {
            // 尝试创建MPV渲染视图实例
            val renderView = MpvSurfaceRenderView(context)
            Log.d("MpvSurfaceRenderViewFactory", "Created new MpvSurfaceRenderView instance")
            renderView
        } catch (e: Exception) {
            // 捕获创建过程中的异常
            Log.d("MpvSurfaceRenderViewFactory", "Failed to create MpvSurfaceRenderView: ${e.message}")
            throw e
        }
    }

    companion object {
        // 单例模式实现
        private var instance: MpvSurfaceRenderViewFactory? = null

        fun create(): MpvSurfaceRenderViewFactory {
            return instance ?: synchronized(this) {
                instance ?: MpvSurfaceRenderViewFactory().also { instance = it }
            }
        }
    }
}