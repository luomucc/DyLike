// AssGenerator.kt
package me.lingci.lib.dm.view.converter

/**
 * ASS文件生成器
 */
class AssGenerator(private val config: AssConfig) {

    fun generateHeader(): String {
        val alpha = ((1 - config.opacity) * 255).toInt().toString(16).padStart(2, '0')
        val primaryColor = "&H${alpha}FFFFFF"
        val backColor = "&H${alpha}000000"

        return """
[Script Info]
Script Updated By: ZhaoHuaXS (https://github.com/zhaohuaxs)
ScriptType: v4.00+
Collisions: Normal
PlayResX: ${config.resolutionX}
PlayResY: ${config.resolutionY}
Aspect Ratio: ${config.resolutionX}:${config.resolutionY}
; Timer: 100.0000
WrapStyle: 2
ScaledBorderAndShadow: yes
YCbCr Matrix: TV.601

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding

Style: R2L,${config.fontName},${config.fontSize},$primaryColor,&H00FFFFFF,&H00000000,$backColor,${if (config.bold) 1 else 0},0,0,0,100.00,100.00,0.00,0.00,1,${config.outline},${config.shadow},7,0,0,0,1
Style: L2R,${config.fontName},${config.fontSize},$primaryColor,&H00FFFFFF,&H00000000,$backColor,${if (config.bold) 1 else 0},0,0,0,100.00,100.00,0.00,0.00,1,${config.outline},${config.shadow},9,0,0,0,1
Style: TOP,${config.fontName},${config.fontSize},$primaryColor,&H00FFFFFF,&H00000000,$backColor,${if (config.bold) 1 else 0},0,0,0,100.00,100.00,0.00,0.00,1,${config.outline},${config.shadow},8,0,0,0,1
Style: BTM,${config.fontName},${config.fontSize},$primaryColor,&H00FFFFFF,&H00000000,$backColor,${if (config.bold) 1 else 0},0,0,0,100.00,100.00,0.00,0.00,1,${config.outline},${config.shadow},2,0,0,0,1
Style: SP,${config.fontName},${config.fontSize},&H00FFFFFF,&H00FFFFFF,&H00000000,$backColor,${if (config.bold) 1 else 0},0,0,0,100.00,100.00,0.00,0.00,1,${config.outline},${config.shadow},7,0,0,0,1
Style: message_box,${config.fontName},${config.superChatFontSize},&H00FFFFFF,&H00FFFFFF,&H00000000,$backColor,${if (config.bold) 1 else 0},0,0,0,100.00,100.00,0.00,0.00,1,0.0,0.7,7,0,0,0,1
Style: price,${config.fontName},${(config.superChatFontSize * 0.7).toInt()},&H00FFFFFF,&H00FFFFFF,&H00000000,$backColor,${if (config.bold) 1 else 0},0,0,0,100.00,100.00,0.00,0.00,1,0.0,0.7,7,0,0,0,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text

""".trimIndent()
    }

    fun generateDanmakuLine(
        layer: Int,
        startTime: String,
        endTime: String,
        style: String,
        effect: String,
        color: String,
        text: String
    ): String {
        return "Dialogue: $layer,$startTime,$endTime,$style,,0000,0000,0000,,{$effect}{$color}$text"
    }

}