package me.lingci.lib.dm.view.util

import me.lingci.lib.base.util.Log
import me.lingci.lib.base.util.createNew
import me.lingci.lib.base.util.decodeDm
import me.lingci.lib.base.util.encodeDm
import me.lingci.lib.base.util.notExists
import me.lingci.lib.base.util.replaceDm
import me.lingci.lib.dm.view.common.DmInitializer
import me.lingci.lib.dm.view.entity.xml.DmData
import me.lingci.lib.dm.view.entity.xml.DmItem
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.StringReader
import java.nio.charset.StandardCharsets
import kotlin.math.max
import kotlin.math.min
import kotlin.streams.asSequence

object XmlConverter {

    private const val XML_HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"

    fun fromXmlStr(xmlStr: String): DmData {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser().apply {
            setInput(StringReader(xmlStr))
        }
        return processFromXml(parser)
    }

    fun fromXmlFile(path: String): DmData {
        return fromXmlFile(File(path))
    }

    fun fromXmlFile(file: File): DmData {
        if (file.notExists() || file.canRead().not()) {
            return DmData()
        }
        return fromXmlInput(file.inputStream())
    }

    fun fromXmlInput(inputStream: InputStream): DmData {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser().apply {
            setInput(inputStream, null)
        }
        return processFromXml(parser)
    }

    private fun processFromXml(parser: XmlPullParser): DmData {
        val data = DmData()
        val list = mutableListOf<DmItem>()
        try {
            var currentTag = ""
            var p = ""
            var s = ""
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                when (parser.next()) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        when (currentTag) {
                            DmInitializer.DANMAKU_TAG_D -> {
                                for (i in 0 until parser.attributeCount) {
                                    when(parser.getAttributeName(i)) {
                                        DmInitializer.DANMAKU_ATTR_P -> p = parser.getAttributeValue(i)
                                        DmInitializer.DANMAKU_ATTR_S -> s = parser.getAttributeValue(i)
                                        else -> {}
                                    }
                                }
                                //p = parser.getAttributeValue(null, "p")
                                //s = parser.getAttributeValue(null, "s")
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        if (parser.text.isNullOrBlank()) {
                            continue
                        }
                        when (currentTag) {
                            DmInitializer.DANMAKU_TAG_CHAT_SERVER -> data.chatServer = parser.text
                            DmInitializer.DANMAKU_TAG_CHATID -> data.chatId = parser.text
                            DmInitializer.DANMAKU_TAG_MISSION -> data.mission = parser.text
                            DmInitializer.DANMAKU_TAG_MAXLIMIT -> data.maxLimit = parser.text
                            DmInitializer.DANMAKU_TAG_MAXCOUNT -> data.maxCount =
                                if (parser.text.isNullOrBlank()) 0 else parser.text.toInt()
                            DmInitializer.DANMAKU_TAG_SOURCE -> data.source = parser.text
                            DmInitializer.DANMAKU_TAG_D -> {
                                if (p.isNotBlank()) {
                                    list.add(
                                        DmItem(
                                            content = parser.text.decodeDm(), style = p, extend = s
                                        )
                                    )
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        currentTag = ""
                        p = ""
                        s = ""
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(this, "from xml failed", e)
        }
        data.list = list
        return data
    }

    fun getMinMaxSendTime(path: String): Triple<Long, Long, Int> {
        var minSend: Long = 0
        var maxSend: Long = 0
        var total = 0
        try {
            var currentTag = ""
            File(path).inputStream().use { stream ->
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser().apply {
                    setInput(stream, null)
                }
                while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                    when (parser.next()) {
                        XmlPullParser.START_TAG -> {
                            currentTag = parser.name
                            when (currentTag) {
                                DmInitializer.DANMAKU_TAG_D -> {
                                    total++
                                    parser.getAttributeValue(null, DmInitializer.DANMAKU_ATTR_P).let { style ->
                                        val time = style.split(",")[4]
                                        var sendTime = 0L
                                        try {
                                            sendTime = time.toLong()
                                        } catch (e: Exception) {
                                            if (total < 2) {
                                                Log.d(this, "get min max failed", e.message)
                                            }
                                        }
                                        if (minSend == 0L) {
                                            minSend = sendTime
                                        }
                                        minSend = min(minSend, sendTime)
                                        maxSend = max(maxSend, sendTime)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(this, "get min max failed", e)
        }
        // 时间戳兼容
        if (minSend > Int.MAX_VALUE) minSend = minSend / 1000
        if (maxSend > Int.MAX_VALUE) maxSend = maxSend / 1000
        return Triple(minSend, maxSend, total)
    }

    fun writeXml(data: DmData, file: File): Boolean {
        try {
            // 创建解析器工厂并配置
            val factory = XmlPullParserFactory.newInstance().apply {
                isNamespaceAware = true
            }
            file.createNew()
            file.outputStream().use { stream ->
                // 配置XML序列化器
                val serializer = factory.newSerializer().apply {
                    setOutput(OutputStreamWriter(stream, StandardCharsets.UTF_8))
                    startDocument(StandardCharsets.UTF_8.name().lowercase(), null)
                }
                // 根节点
                serializer.text("\n")
                serializer.startTag(null, DmInitializer.DANMAKU_TAG_I)
                serializer.text("\n\t")

                serializer.startTag(null, DmInitializer.DANMAKU_TAG_CHAT_SERVER)
                serializer.text(data.chatServer)
                serializer.endTag(null, DmInitializer.DANMAKU_TAG_CHAT_SERVER)
                serializer.text("\n\t")

                serializer.startTag(null, DmInitializer.DANMAKU_TAG_CHATID)
                serializer.text(data.chatId)
                serializer.endTag(null, DmInitializer.DANMAKU_TAG_CHATID)
                serializer.text("\n\t")

                serializer.startTag(null, DmInitializer.DANMAKU_TAG_MISSION)
                serializer.text(data.mission)
                serializer.endTag(null, DmInitializer.DANMAKU_TAG_MISSION)
                serializer.text("\n\t")

                serializer.startTag(null, DmInitializer.DANMAKU_TAG_MAXLIMIT)
                serializer.text(data.maxLimit)
                serializer.endTag(null, DmInitializer.DANMAKU_TAG_MAXLIMIT)
                serializer.text("\n\t")

                serializer.startTag(null, DmInitializer.DANMAKU_TAG_MAXCOUNT)
                serializer.text("${data.maxCount}")
                serializer.endTag(null, DmInitializer.DANMAKU_TAG_MAXCOUNT)
                serializer.text("\n\t")

                serializer.startTag(null, DmInitializer.DANMAKU_TAG_SOURCE)
                serializer.text(data.source)
                serializer.endTag(null, DmInitializer.DANMAKU_TAG_SOURCE)
                serializer.text("\n")

                data.list.forEach { item ->
                    // 开始当前节点
                    serializer.text("\t")
                    serializer.startTag(null, DmInitializer.DANMAKU_TAG_D)
                    // p
                    serializer.attribute(null, DmInitializer.DANMAKU_ATTR_P, item.style)
                    // s
                    if (item.extendStyle.isNotBlank()) {
                        serializer.attribute(
                            null, DmInitializer.DANMAKU_ATTR_S, item.extendStyle
                        )
                    }
                    // 弹幕内容
                    try {
                        serializer.text(item.content.replaceDm().encodeDm())
                    } catch (e: Exception) {
                        Log.d(this, "InvalidChars", item.content, e.message)
                    }
                    // 结束当前节点
                    serializer.endTag(null, DmInitializer.DANMAKU_TAG_D)
                    serializer.text("\n")
                }

                // 完成文档
                serializer.endTag(null, DmInitializer.DANMAKU_TAG_I)
                serializer.endDocument()
            }
            return true
        } catch (e: Exception) {
            Log.d(this, "写入弹幕失败", file.name, e)
            file.delete()
            return false
        }
    }

    fun toXml(data: DmData): String {
        return """
$XML_HEADER
<i>
<chatserver>${data.chatServer}</chatserver>
<chatid>${data.chatId}</chatid>
<mission>${data.mission}</mission>
<maxlimit>${data.maxLimit}</maxlimit>
<maxcount>${data.maxCount}</maxcount>
<source>${data.source}</source>
${
            data.list.stream()
                .map { item ->
                    if (item.extendStyle.isNotBlank()) {
                        "<d p=\"${item.style}\" s=\"${item.extendStyle}\">${item.content.encodeDm()}</d>"
                    } else {
                        "<d p=\"${item.style}\">${item.content.encodeDm()}</d>"
                    }
                }
                .asSequence().joinToString("\n")
        }
</i>
        """.trimIndent()
    }

}