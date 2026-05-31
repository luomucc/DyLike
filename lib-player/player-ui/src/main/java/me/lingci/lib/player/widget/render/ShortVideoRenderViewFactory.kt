package me.lingci.lib.player.widget.render

import android.content.Context
import xyz.doikki.videoplayer.render.IRenderView
import xyz.doikki.videoplayer.render.RenderViewFactory
import xyz.doikki.videoplayer.render.TextureRenderView

class ShortVideoRenderViewFactory : RenderViewFactory() {

    companion object {
        fun create(): ShortVideoRenderViewFactory {
            return ShortVideoRenderViewFactory()
        }
    }

    override fun createRenderView(context: Context): IRenderView {
        return ShortVideoRenderView(TextureRenderView(context))
    }
}