// DanmakuConverter.kt
package me.lingci.lib.dm.view.converter

import me.lingci.lib.base.util.Log
import me.lingci.lib.dm.view.entity.xml.DmItem
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 主转换器
 */
class DanmakuConverter(private val config: AssConfig) {

    fun convertXmlToAss(dmList: List<DmItem>, assFile: String) {
        // 初始化布局数组
        val rollArray = DanmakuArray(config.resolutionX, config.resolutionY, config.fontSize)
        val topArray = DanmakuArray(config.resolutionX, config.resolutionY, config.fontSize)

        // 创建ASS文件
        val assGenerator = AssGenerator(config)
        File(assFile).writeText(assGenerator.generateHeader())

        // 处理普通弹幕
        processNormalDanmaku(dmList, assFile, rollArray, topArray, assGenerator)

        Log.d(this, "Convert to $assFile successfully.")
    }


    private fun processNormalDanmaku(
        dmList: List<DmItem>,
        assFile: String,
        rollArray: DanmakuArray,
        topArray: DanmakuArray,
        assGenerator: AssGenerator
    ) {
        Log.d(this, "The normal danmaku pool is ${dmList.size}.")
        File(assFile).appendText(buildString {
            for (dm in dmList) {
                val pAttrs = dm.style.split(",")
                val appearTime = pAttrs[0].toFloat()
                val danmakuType = pAttrs[1].toInt()

                // 转换颜色
                val color = pAttrs[3].toInt()
                val colorHex = color.toString(16).padStart(6, '0')
                    .chunked(2).reversed().joinToString("")
                    .uppercase()
                val colorText = "\\c&H$colorHex"

                val startTime = DanmakuUtils.formatTime(appearTime)
                val text = DanmakuUtils.removeEmojis(dm.content, ".")

                when (danmakuType) {
                    1 -> { // 滚动弹幕
                        val endTime = DanmakuUtils.formatTime(appearTime + config.rollTime)
                        val textLength = DanmakuUtils.getStringLength(text, config.fontSize)
                        val x1 = config.resolutionX + textLength / 2
                        val x2 = -textLength / 2

                        val y = DanmakuLayout.getPositionYForR2L(
                            config.fontSize, appearTime, textLength,
                            config.resolutionX, config.rollTime, rollArray
                        )

                        if (y != null && y <= config.resolutionY * config.displayArea) {
                            val effect = "\\move($x1,$y,$x2,$y)"
                            appendLine(assGenerator.generateDanmakuLine(
                                0, startTime, endTime, "R2L", effect, colorText, text
                            ))
                        }
                    }

                    5 -> { // 顶部弹幕
                        val endTime = DanmakuUtils.formatTime(appearTime + config.fixTime)
                        val x = config.resolutionX / 2

                        val y = DanmakuLayout.getFixedY(
                            config.fontSize, appearTime, config.resolutionY,
                            config.fixTime, topArray, true
                        )

                        if (y != null && y <= config.resolutionY * config.displayArea) {
                            val effect = "\\pos($x,$y)"
                            appendLine(assGenerator.generateDanmakuLine(
                                1, startTime, endTime, "TOP", effect, colorText, text
                            ))
                        }
                    }

                    4 -> { // 底部弹幕
                        val endTime = DanmakuUtils.formatTime(appearTime + config.fixTime)
                        val x = config.resolutionX / 2

                        val y = DanmakuLayout.getFixedY(
                            config.fontSize, appearTime, config.resolutionY,
                            config.fixTime, topArray, false
                        )

                        if (y != null && y <= config.resolutionY * config.displayArea) {
                            val effect = "\\pos($x,$y)"
                            appendLine(assGenerator.generateDanmakuLine(
                                1, startTime, endTime, "BTM", effect, colorText, text
                            ))
                        }
                    }
                }
            }
        })
    }
    
    fun convertXmlToAss(xmlFile: String, assFile: String) {
        val start = System.currentTimeMillis()
        val xmlDoc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File(xmlFile))
        
        val root = xmlDoc.documentElement
        
        // 初始化布局数组
        val rollArray = DanmakuArray(config.resolutionX, config.resolutionY, config.fontSize)
        val topArray = DanmakuArray(config.resolutionX, config.resolutionY, config.fontSize)
        
        // 创建ASS文件
        val assGenerator = AssGenerator(config)
        File(assFile).writeText(assGenerator.generateHeader())
        
        // 处理普通弹幕
        processNormalDanmaku(root, assFile, rollArray, topArray, assGenerator)
        
        println("Convert $xmlFile to $assFile successfully. ${System.currentTimeMillis() - start}")
    }

    fun convertXmlToAssAll(xmlFile: String, assFile: String) {
        val start = System.currentTimeMillis()
        val xmlDoc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File(xmlFile))

        val root = xmlDoc.documentElement

        // 初始化布局数组
        val rollArray = DanmakuArray(config.resolutionX, config.resolutionY, config.fontSize)
        val topArray = DanmakuArray(config.resolutionX, config.resolutionY, config.fontSize)

        // 创建ASS文件
        val assGenerator = AssGenerator(config)
        File(assFile).writeText(assGenerator.generateHeader())

        // 处理普通弹幕
        processNormalDanmaku(root, assFile, rollArray, topArray, assGenerator)

        // 处理礼物和守护
        processGiftAndGuard(root, assFile)

        // 处理超级聊天
        processSuperChat(root, assFile)

        Log.d(this, "Convert $xmlFile to $assFile successfully. ${System.currentTimeMillis() - start}")
    }
    
    private fun processNormalDanmaku(
        root: Element,
        assFile: String,
        rollArray: DanmakuArray,
        topArray: DanmakuArray,
        assGenerator: AssGenerator
    ) {

        val danmakuList = root.getElementsByTagName("d")
        println("The normal danmaku pool is ${danmakuList.length}.")
        
        File(assFile).appendText(buildString {
            for (i in 0 until danmakuList.length) {
                val d = danmakuList.item(i) as Element
                val pAttrs = d.getAttribute("p").split(",")
                val appearTime = pAttrs[0].toFloat()
                val danmakuType = pAttrs[1].toInt()
                
                // 转换颜色
                val color = pAttrs[3].toInt()
                val colorHex = color.toString(16).padStart(6, '0')
                    .chunked(2).reversed().joinToString("")
                    .uppercase()
                val colorText = "\\c&H$colorHex"
                
                val startTime = DanmakuUtils.formatTime(appearTime)
                val text = DanmakuUtils.removeEmojis(d.textContent ?: "", ".")
                
                when (danmakuType) {
                    1 -> { // 滚动弹幕
                        val endTime = DanmakuUtils.formatTime(appearTime + config.rollTime)
                        val textLength = DanmakuUtils.getStringLength(text, config.fontSize)
                        val x1 = config.resolutionX + textLength / 2
                        val x2 = -textLength / 2
                        
                        val y = DanmakuLayout.getPositionYForR2L(
                            config.fontSize, appearTime, textLength, 
                            config.resolutionX, config.rollTime, rollArray
                        )
                        
                        if (y != null && y <= config.resolutionY * config.displayArea) {
                            val effect = "\\move($x1,$y,$x2,$y)"
                            appendLine(assGenerator.generateDanmakuLine(
                                0, startTime, endTime, "R2L", effect, colorText, text
                            ))
                        }
                    }
                    
                    5 -> { // 顶部弹幕
                        val endTime = DanmakuUtils.formatTime(appearTime + config.fixTime)
                        val x = config.resolutionX / 2
                        
                        val y = DanmakuLayout.getFixedY(
                            config.fontSize, appearTime, config.resolutionY, 
                            config.fixTime, topArray, true
                        )
                        
                        if (y != null && y <= config.resolutionY * config.displayArea) {
                            val effect = "\\pos($x,$y)"
                            appendLine(assGenerator.generateDanmakuLine(
                                1, startTime, endTime, "TOP", effect, colorText, text
                            ))
                        }
                    }
                    
                    4 -> { // 底部弹幕
                        val endTime = DanmakuUtils.formatTime(appearTime + config.fixTime)
                        val x = config.resolutionX / 2
                        
                        val y = DanmakuLayout.getFixedY(
                            config.fontSize, appearTime, config.resolutionY, 
                            config.fixTime, topArray, false
                        )
                        
                        if (y != null && y <= config.resolutionY * config.displayArea) {
                            val effect = "\\pos($x,$y)"
                            appendLine(assGenerator.generateDanmakuLine(
                                1, startTime, endTime, "BTM", effect, colorText, text
                            ))
                        }
                    }
                }
            }
        })
    }
    
    private fun processGiftAndGuard(root: Element, assFile: String) {
        // 礼物和守护处理逻辑
        val gifts = root.getElementsByTagName("gift")
        val guards = root.getElementsByTagName("guard")
        
        val giftDataList = mutableListOf<Map<String, Any>>()
        
        // 处理礼物
        for (i in 0 until gifts.length) {
            val gift = gifts.item(i) as Element
            giftDataList.add(mapOf(
                "appear_time" to gift.getAttribute("ts").toFloat(),
                "over_time" to (gift.getAttribute("ts").toFloat() + 2),
                "user" to gift.getAttribute("user"),
                "name" to gift.getAttribute("giftname"),
                "count" to gift.getAttribute("giftcount").toInt(),
                "price" to gift.getAttribute("price")
            ))
        }
        
        // 处理守护
        for (i in 0 until guards.length) {
            val guard = guards.item(i) as Element
            giftDataList.add(mapOf(
                "appear_time" to guard.getAttribute("ts").toFloat(),
                "over_time" to (guard.getAttribute("ts").toFloat() + 2),
                "user" to guard.getAttribute("user"),
                "name" to guard.getAttribute("giftname"),
                "count" to guard.getAttribute("count").toInt(),
                "price" to guard.getAttribute("price")
            ))
        }
        
        // 这里简化处理，实际应该使用类似Python版本的完整逻辑
        File(assFile).appendText(buildString {
            giftDataList.forEach { gift ->
                val startTime = DanmakuUtils.formatTime(gift["appear_time"] as Float)
                val endTime = DanmakuUtils.formatTime(gift["over_time"] as Float)
                val user = gift["user"] as String
                val name = gift["name"] as String
                val count = gift["count"] as Int
                val price = gift["price"] as String
                
                val (_, _, userColor) = DanmakuUtils.getColorByPrice(price.toInt())
                val text = "{$userColor\\b1}$user:{$userColor\\b0} $name x$count"
                
                appendLine("Dialogue: 0,$startTime,$endTime,message_box,,0000,0000,0000,,{\\pos(0,${config.resolutionY - config.superChatFontSize})}$text")
            }
        })
    }
    
    private fun processSuperChat(root: Element, assFile: String) {
        val superChats = root.getElementsByTagName("sc")
        println("The superchat pool is ${superChats.length}.")
        
        // 简化的超级聊天处理
        File(assFile).appendText(buildString {
            for (i in 0 until superChats.length) {
                val sc = superChats.item(i) as Element
                val appearTime = sc.getAttribute("ts").toFloat()
                val userName = sc.getAttribute("user")
                val price = sc.getAttribute("price")
                val time = sc.getAttribute("time").toFloat()
                val message = sc.textContent ?: ""
                
                val startTime = DanmakuUtils.formatTime(appearTime)
                val endTime = DanmakuUtils.formatTime(appearTime + time)
                
                // 简化的超级聊天显示
                appendLine("Dialogue: 0,$startTime,$endTime,message_box,,0000,0000,0000,,{\\pos(10,${config.resolutionY - 100})}[$userName] $message")
            }
        })
    }
}