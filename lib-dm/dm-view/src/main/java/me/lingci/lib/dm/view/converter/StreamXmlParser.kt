package me.lingci.lib.dm.view.converter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import me.lingci.lib.base.util.Log
import me.lingci.lib.base.util.logD
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@Suppress("MemberVisibilityCanBePrivate")
class StreamXmlParser(private val file: File) {

    suspend fun parseDanmaku(): ReceiveChannel<DanmakuItem> = coroutineScope {
        val channel = Channel<DanmakuItem>(Channel.UNLIMITED)

        launch(Dispatchers.IO) {
            try {
                val item = DanmakuItem()
                if (item.text.isNotBlank()) {
                    channel.send(item)
                }
            } finally {
                channel.close()
            }
        }

        channel
    }

    suspend fun parseGifts(): ReceiveChannel<GiftItem> = coroutineScope {
        val channel = Channel<GiftItem>(Channel.UNLIMITED)
        channel.close()
        channel
    }

    suspend fun parseSuperChats(): ReceiveChannel<SuperChatItem> = coroutineScope {
        val channel = Channel<SuperChatItem>(Channel.UNLIMITED)
        channel.close()
        channel
    }


    suspend fun parseXmlStream(): Flow<DanmakuItem> = flow {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser().apply {
            setInput(file.inputStream(), null)
        }

        val total = AtomicInteger()
        try {
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                val type = try {
                    parser.next()
                } catch (e: Exception) {
                    logD("xml read failed", e)
                    // 尝试恢复：跳过错误内容，继续解析下一个标签
                    try {
                        parser.nextTag()
                        parser.eventType
                    } catch (e2: Exception) {
                        logD("xml recovery failed", e2)
                        XmlPullParser.END_DOCUMENT
                    }
                }
                when (type) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "d") {
                            // 使用 nextText 原子读取文本，避免 TEXT 事件分割导致状态不一致
                            parser.getAttributeValue(null, "p")?.let { p ->
                                p.split(",").let { attrs ->
                                    try {
                                        val text = parser.nextText() ?: ""
                                        emit(DanmakuItem(
                                            appearTime = attrs[0].toFloat(),
                                            type = attrs[1].toInt(),
                                            color = attrs[3].toLong().black2White(),
                                            user = if (attrs.size > 6) attrs[6] else null,
                                            text = text
                                        ))
                                        total.getAndIncrement()
                                    } catch (e: Exception) {
                                        logD("parse danmaku text failed", e)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(this, "get xml failed", e)
        }
        Log.d(this, "get xml total", total)
    }

    suspend fun processSortedXml(): List<DanmakuItem> =
        parseXmlStream()
            .buffer() // 缓冲以提高性能
            .toList()
            .sortedBy { it.appearTime }

    fun Long.black2White(): Long {
        if (this == 0L) {
            return 16777215
        }
        return this
    }

}