package me.lingci.lib.dm.view.converter

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import me.lingci.lib.base.util.Log
import java.io.File
import java.io.FileWriter

class OptimizedDanmakuConverter(private val config: AssConfig) {

    @Suppress("SpellCheckingInspection")
    suspend fun convertXmlToAss(xmlFile: String, assFile: String) = coroutineScope {
        val start = System.currentTimeMillis()

        val xmlFileObj = File(xmlFile)
        if (!xmlFileObj.exists()) {
            throw RuntimeException("Input file not found: $xmlFile")
        }

        val parser = StreamXmlParser(xmlFileObj)

        // 初始化布局数组
        val rollArray = DanmakuArray(config.resolutionX, config.resolutionY, config.fontSize)
        val topArray = DanmakuArray(config.resolutionX, config.resolutionY, config.fontSize)

        // 创建ASS文件，复用 AssGenerator 实例
        val assGenerator = AssGenerator(config)
        FileWriter(assFile).use { writer ->
            writer.write(assGenerator.generateHeader())

            val count = processDanmakuAsync(parser, writer, rollArray, topArray, assGenerator)

            if (count == 0) {
                throw RuntimeException("danmu null")
            }

            println("Processed $count")
            println("Convert $xmlFile to $assFile successfully. ${System.currentTimeMillis() - start}")
        }
    }

    private suspend fun processDanmakuAsync(
        parser: StreamXmlParser,
        writer: FileWriter,
        rollArray: DanmakuArray,
        topArray: DanmakuArray,
        assGenerator: AssGenerator
    ): Int = coroutineScope {
        val start = System.currentTimeMillis()
        Log.d(this, "start process", start)

        // 全量加载并排序（保留排序逻辑）
        val list = parser.processSortedXml()
        Log.d(this, "end parse", System.currentTimeMillis() - start)

        // 使用 Channel 流式处理，避免 joinToString 创建大字符串
        val channel = Channel<String>(Channel.BUFFERED)
        var count = 0

        // 生产者：处理弹幕并发送到 channel
        val producer = launch {
            for (item in list) {
                val assLine = generateDanmakuLine(item, rollArray, topArray, assGenerator)
                if (assLine != null) {
                    channel.send(assLine)
                    count++
                }
            }
            channel.close()
        }

        // 消费者：从 channel 读取并写入文件
        val consumer = launch {
            val sb = StringBuilder()
            var batchCount = 0

            for (line in channel) {
                sb.append(line).append('\n')
                batchCount++

                // 每 500 行批量写入一次，减少 I/O 次数
                if (batchCount >= 500) {
                    writer.write(sb.toString())
                    sb.clear()
                    batchCount = 0
                }
            }

            // 写入剩余内容
            if (sb.isNotEmpty()) {
                writer.write(sb.toString())
            }
        }

        // 等待生产者和消费者完成
        producer.join()
        consumer.join()

        Log.d(this, "end generate", System.currentTimeMillis() - start)

        count
    }

    private fun generateDanmakuLine(
        item: DanmakuItem,
        rollArray: DanmakuArray,
        topArray: DanmakuArray,
        assGenerator: AssGenerator
    ): String? {
        val colorHex = item.color.toString(16).padStart(6, '0')
            .chunked(2).reversed().joinToString("")
            .uppercase()
        val colorText = "\\c&H$colorHex"

        val startTime = DanmakuUtils.formatTime(item.appearTime)
        val text = DanmakuUtils.removeEmojis(item.text, ".")

        return when (item.type) {
            1 -> { // 滚动弹幕
                val endTime = DanmakuUtils.formatTime(item.appearTime + config.rollTime)
                val textLength = DanmakuUtils.getStringLength(text, config.fontSize)
                val x1 = config.resolutionX + textLength / 2
                val x2 = -textLength / 2

                val y = DanmakuLayout.getPositionYForR2L(
                    config.fontSize, item.appearTime, textLength,
                    config.resolutionX, config.rollTime, rollArray
                )

                if (y != null && y <= config.resolutionY * config.displayArea) {
                    val effect = "\\move($x1,$y,$x2,$y)"
                    assGenerator.generateDanmakuLine(
                        0, startTime, endTime, "R2L", effect, colorText, text
                    )
                } else {
                    null
                }
            }

            5 -> { // 顶部弹幕
                val endTime = DanmakuUtils.formatTime(item.appearTime + config.fixTime)
                val x = config.resolutionX / 2

                val y = DanmakuLayout.getFixedY(
                    config.fontSize, item.appearTime, config.resolutionY,
                    config.fixTime, topArray, true
                )

                if (y != null && y <= config.resolutionY * config.displayArea) {
                    val effect = "\\pos($x,$y)"
                    assGenerator.generateDanmakuLine(
                        1, startTime, endTime, "TOP", effect, colorText, text
                    )
                } else {
                    null
                }
            }

            4 -> { // 底部弹幕
                val endTime = DanmakuUtils.formatTime(item.appearTime + config.fixTime)
                val x = config.resolutionX / 2

                val y = DanmakuLayout.getFixedY(
                    config.fontSize, item.appearTime, config.resolutionY,
                    config.fixTime, topArray, false
                )

                if (y != null && y <= config.resolutionY * config.displayArea) {
                    val effect = "\\pos($x,$y)"
                    assGenerator.generateDanmakuLine(
                        1, startTime, endTime, "BTM", effect, colorText, text
                    )
                } else {
                    null
                }
            }

            else -> null
        }
    }

}