package me.lingci.lib.dm.view.parser

import master.flame.danmaku.danmaku.loader.IllegalDataException
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory
import master.flame.danmaku.danmaku.model.android.Danmakus
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import me.lingci.lib.base.util.Log
import me.lingci.lib.dm.view.entity.DmLoadOptions
import java.io.InputStream

/**
 * @author : happyc
 * time    : 2022/03/14
 * desc    : [...](https://blog.csdn.net/benhuo931115/article/details/51056646)
 * version : 1.0
 */
object DanmakuParserBuilder {
    /**
     * 创建解析器对象，解析输入流
     * DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_BILI) //xml解析
     * DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_ACFUN) //json文件格式解析
     *
     * @param stream
     * @return
     */
    @JvmStatic
    @JvmOverloads
    fun createParser(
        stream: InputStream?,
        options: DmLoadOptions = DmLoadOptions()
    ): BaseDanmakuParser {
        if (stream == null) {
            return object : BaseDanmakuParser() {
                override fun parse(): Danmakus {
                    return Danmakus()
                }
            }
        }
        Log.d(this, "createParser", Thread.currentThread().name)

        val loader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_BILI)
        try {
            loader.load(stream)
        } catch (e: IllegalDataException) {
            Log.d(this, "createParser", Thread.currentThread().name, e)
        }
        val parser: BaseDanmakuParser = BiliDanmakuParser(options)
        val dataSource = loader.dataSource
        parser.load(dataSource)
        return parser
    }

}