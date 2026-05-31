/*
 * Copyright (C) 2013 Chen Hui <calmer91@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.lingci.lib.dm.view.parser

import android.graphics.Color
import android.text.TextUtils
import master.flame.danmaku.danmaku.model.AlphaValue
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.Duration
import master.flame.danmaku.danmaku.model.IDanmakus
import master.flame.danmaku.danmaku.model.IDisplayer
import master.flame.danmaku.danmaku.model.SpecialDanmaku
import master.flame.danmaku.danmaku.model.android.DanmakuFactory
import master.flame.danmaku.danmaku.model.android.Danmakus
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import master.flame.danmaku.danmaku.parser.android.AndroidFileSource
import master.flame.danmaku.danmaku.util.DanmakuUtils
import me.lingci.lib.base.util.Log
import me.lingci.lib.base.util.WeightedRandomSelector
import me.lingci.lib.dm.view.common.DmInitializer
import me.lingci.lib.dm.view.entity.DmLoadOptions
import me.lingci.lib.dm.view.entity.DmStyleExtend
import org.json.JSONArray
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

open class BiliDanmakuParser : BaseDanmakuParser {

    private val mRandomSelector = WeightedRandomSelector<Boolean>(
        listOf(Pair(true, 4), Pair(false, 6))
    )

    @Suppress("PrivatePropertyName")
    private val TRUE_STRING = "true"

    @JvmField
    protected var mDispScaleX: Float = 0f
    @JvmField
    protected var mDispScaleY: Float = 0f

    private var mLoadOptions: DmLoadOptions = DmLoadOptions()

    constructor()

    constructor(dmGradient: Boolean) {
        val options = DmLoadOptions()
        options.whiteToGradient = dmGradient
        this.mLoadOptions = options
    }

    constructor(options: DmLoadOptions) {
        this.mLoadOptions = options
        mRandomSelector.updateItems(
            listOf(
                Pair(true, options.gradientRatio),
                Pair(false, Integer.max(0, 100 - options.gradientRatio))
            )
        )
    }

    public override fun parse(): Danmakus? {
        Log.d(this, "parse", Thread.currentThread().name)
        if (mDataSource != null) {
            val source = mDataSource as AndroidFileSource
            try {
                source.data()?.use { stream ->
                    val factory = XmlPullParserFactory.newInstance().apply {
                        isNamespaceAware = true
                    }
                    val parser = factory.newPullParser().apply {
                        setInput(stream, "utf-8")
                    }
                    return xmlContentHandle(parser)
                }

            } catch (e: Exception) {
                Log.d(this, "parse", e)
            }
        }
        return null
    }

    private fun xmlContentHandle(parser: XmlPullParser): Danmakus {
        var eventType = parser.eventType
        val result = Danmakus(IDanmakus.ST_BY_TIME, false, mContext.baseComparator)
        val mergeTime = 8000
        val danmakuMap: MutableMap<Pair<String, Long>, Pair<BaseDanmaku, Int>> = mutableMapOf()
        var danmaku: BaseDanmaku? = null
        var textColor: Int
        var index = 0
        val resultSynchronizer = result.obtainSynchronizer()
        try {
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals(DmInitializer.DANMAKU_TAG_D, ignoreCase = true)) {
                            // <d p="23.826000213623,1,25,16777215,1422201084,0,057075e9,757076900">我从未见过如此厚颜无耻之猴</d>
                            // 0:时间(弹幕出现时间)
                            // 1:类型(1从右至左滚动弹幕|6从左至右滚动弹幕|5顶端固定弹幕|4底端固定弹幕|7高级弹幕|8脚本弹幕)
                            // 2:字号
                            // 3:颜色
                            // 4:时间戳 ?
                            // 5:弹幕池id
                            // 6:用户hash
                            // 7:弹幕id
                            val pValue = parser.getAttributeValue(null, DmInitializer.DANMAKU_ATTR_P)
                            val values = pValue?.split(',').orEmpty()
                            if (values.size >= 4) {
                                val time = (parseFloat(values[0]) * 1000).toLong() // 出现时间
                                val type = parseInteger(values[1]) // 弹幕类型
                                val textSize = parseFloat(values[2]) // 字体大小
                                textColor = ((0x00000000ff000000L or parseLong(values[3]))).toInt() // 颜色
                                // int poolType = parseInteger(values[5]); // 弹幕池类型（忽略
                                danmaku = mContext.mDanmakuFactory.createDanmaku(type, mContext)
                                danmaku.time = time
                                danmaku.textSize = textSize * (mDispDensity - 0.6f)
                                danmaku.textColor = textColor
                                danmaku.textShadowColor = if (textColor <= Color.BLACK) Color.WHITE else Color.BLACK
                                if (textColor == Color.WHITE && type != 7 && mLoadOptions.whiteToGradient && mRandomSelector.select()) {
                                    danmaku.tag = DmStyleExtend()
                                }
                                if (type != 7) {
                                    val sValue = parser.getAttributeValue(null, DmInitializer.DANMAKU_ATTR_S)
                                    if (!sValue.isNullOrEmpty()) {
                                        try {
                                            // 渐变
                                            val sValues = sValue.split(',')
                                            if (sValues.isNotEmpty()) {
                                                val colorArray = sValues[0].split(DmInitializer.GRADIENT_SEPARATOR)
                                                val strokeMode = sValues.size == 1 || "1" != sValues[1]
                                                if (colorArray.size > 1 && danmaku != null) {
                                                    val colors = ArrayList<Int>(colorArray.size)
                                                    for (color in colorArray) {
                                                        if (color.isNotEmpty()) {
                                                            colors.add(((0x00000000ff000000L or parseLong(color))).toInt()) // 颜色
                                                        }
                                                    }
                                                    if (colors.size > 1) {
                                                        // 描边渐变，文本置为白色，如果配置保留原色则不变，边缘色随机
                                                        danmaku.textColor =
                                                            if (mLoadOptions.gradientWithTextColor) textColor else Color.WHITE
                                                        danmaku.tag = DmStyleExtend(colors, strokeMode)
                                                        danmaku.textShadowColor = Color.BLACK
                                                        // 如果文本渐变，需要忽略描边
                                                    }
                                                }
                                            }
                                        } catch (_: Exception) {

                                        }
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (danmaku != null) {
                            var text = parser.text.trim()
                            if (text.isNotBlank()) {
                                DanmakuUtils.fillText(danmaku, decodeXmlString(text))
                                danmaku.index = index++
                                text = danmaku.text.toString().trim()
                                if (danmaku.type == BaseDanmaku.TYPE_SPECIAL && text.startsWith("[") && text.endsWith("]")) {
                                    var textArr: Array<String?>? = null
                                    try {
                                        val jsonArray = JSONArray(text)
                                        textArr = arrayOfNulls(jsonArray.length())
                                        for (i in textArr.indices) {
                                            textArr[i] = jsonArray.getString(i)
                                        }
                                    } catch (e: Exception) {
                                        Log.d(this, "danmaku", "7", e.message)
                                    }
                                    if (textArr == null || textArr.size < 5 || TextUtils.isEmpty(textArr[4])) {
                                        danmaku = null
                                    } else {
                                        parseSpecialDanmaku(danmaku, textArr)
                                    }
                                }
                            } else {
                                danmaku = null
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (tagName.equals(DmInitializer.DANMAKU_TAG_D, ignoreCase = true)) {
                            if (danmaku != null && danmaku.text.isNullOrBlank().not()) {
                                if (danmaku.duration != null) {
                                    danmaku.timer = mTimer
                                    danmaku.flags = mContext.mGlobalFlagValues

                                    if (mLoadOptions.mergeContent && (danmaku.type == 1 || danmaku.type == 6)) {
                                        val index: Long = danmaku.time / mergeTime
                                        val temp = danmakuMap[Pair(danmaku.text.toString(), index)]
                                        danmakuMap[Pair(danmaku.text.toString(), index)] = if (temp != null) {
                                            if (temp.second > 8 && mLoadOptions.debug) {
                                                Log.d(this, "merge", index, Pair(danmaku.text.toString(), index), temp.second)
                                            }
                                            Pair(if (danmaku.time < temp.first.time) danmaku else temp.first, temp.second + 1)
                                        } else {
                                            Pair(danmaku, 1)
                                        }
                                    } else {
                                        synchronized(resultSynchronizer) {
                                            result.addItem(danmaku)
                                        }
                                    }
                                }
                                danmaku = null
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.d(this, "load", danmakuMap.size)
            Log.d(this, "xmlContentHandle", e)
        }
        if (mLoadOptions.mergeContent) {
            val iterator = danmakuMap.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val v = entry.value
                danmaku = v.first
                var text = v.first.text
                // 时间轴调试模式
                if (mLoadOptions.timeDebug && v.first.type != BaseDanmaku.TYPE_SPECIAL) {
                    text =
                        ((v.first.time / 60000).toString() + ":" + (v.first.time / 1000 % 60) + " " + text)
                }
                text = if (v.second >= mLoadOptions.mergeShow) "$text x${v.second}" else text
                if (v.second >= mLoadOptions.mergeToTop) {
                    danmaku = mContext.mDanmakuFactory.createDanmaku(5, mContext)
                    danmaku.time = v.first.time
                    danmaku.textSize = v.first.textSize
                    danmaku.textColor = v.first.textColor
                    danmaku.textShadowColor = v.first.textShadowColor
                    danmaku.index = v.first.index
                    danmaku.timer = v.first.timer
                    danmaku.flags = v.first.flags
                    danmaku.tag = v.first.tag
                    danmaku.duration = v.first.duration
                    danmaku.rotationZ = v.first.rotationZ
                    danmaku.rotationY = v.first.rotationY
                }
                DanmakuUtils.fillText(danmaku, text)
                synchronized(resultSynchronizer) {
                    result.addItem(danmaku)
                }
                iterator.remove()
            }
        }
        danmaku = null
        danmakuMap.clear()

        Log.d(this, "xmlContentHandle", result.size())
        return result
    }

    private fun parseSpecialDanmaku(item: BaseDanmaku, textArr: Array<String?>) {
        DanmakuUtils.fillText(item, textArr[4])
        var beginX = parseFloat(textArr[0]!!)
        var beginY = parseFloat(textArr[1]!!)
        var endX = beginX
        var endY = beginY
        val alphaArr = textArr[2]!!.split("-".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val beginAlpha = (AlphaValue.MAX * parseFloat(alphaArr[0])).toInt()
        var endAlpha = beginAlpha
        if (alphaArr.size > 1) {
            endAlpha = (AlphaValue.MAX * parseFloat(alphaArr[1])).toInt()
        }
        val alphaDuraion = (parseFloat(textArr[3]!!) * 1000).toLong()
        var translationDuration = alphaDuraion
        var translationStartDelay: Long = 0
        var rotateY = 0f
        var rotateZ = 0f
        if (textArr.size >= 7) {
            rotateZ = parseFloat(textArr[5]!!)
            rotateY = parseFloat(textArr[6]!!)
        }
        if (textArr.size >= 11) {
            endX = parseFloat(textArr[7]!!)
            endY = parseFloat(textArr[8]!!)
            if ("" != textArr[9]) {
                translationDuration = parseInteger(textArr[9]!!).toLong()
            }
            if ("" != textArr[10]) {
                translationStartDelay = (parseFloat(textArr[10]!!)).toLong()
            }
        }
        if (isPercentageNumber(textArr[0])) {
            beginX *= DanmakuFactory.BILI_PLAYER_WIDTH
        }
        if (isPercentageNumber(textArr[1])) {
            beginY *= DanmakuFactory.BILI_PLAYER_HEIGHT
        }
        if (textArr.size >= 8 && isPercentageNumber(textArr[7])) {
            endX *= DanmakuFactory.BILI_PLAYER_WIDTH
        }
        if (textArr.size >= 9 && isPercentageNumber(textArr[8])) {
            endY *= DanmakuFactory.BILI_PLAYER_HEIGHT
        }
        item.duration = Duration(alphaDuraion)
        item.rotationZ = rotateZ
        item.rotationY = rotateY
        mContext.mDanmakuFactory.fillTranslationData(
            item,
            beginX,
            beginY,
            endX,
            endY,
            translationDuration,
            translationStartDelay,
            mDispScaleX,
            mDispScaleY
        )
        mContext.mDanmakuFactory.fillAlphaData(item, beginAlpha, endAlpha, alphaDuraion)

        if (textArr.size >= 12) {
            // 是否有描边
            if (!TextUtils.isEmpty(textArr[11]) && TRUE_STRING.equals(
                    textArr[11],
                    ignoreCase = true
                )
            ) {
                item.textShadowColor = Color.TRANSPARENT
            }
        }
        if (textArr.size >= 13) {
            //TODO 字体 textArr[12]
        }
        if (textArr.size >= 14) {
            // Linear.easeIn or Quadratic.easeOut
            (item as SpecialDanmaku).isQuadraticEaseOut = ("0" == textArr[13])
        }
        if (textArr.size >= 15) {
            // 路径数据
            if ("" != textArr[14]) {
                val motionPathString = textArr[14]!!.substring(1)
                if (!TextUtils.isEmpty(motionPathString)) {
                    val pointStrArray = motionPathString.split("L".toRegex())
                        .dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (pointStrArray.isNotEmpty()) {
                        val points =
                            Array<FloatArray?>(pointStrArray.size) { FloatArray(2) }
                        for (i in pointStrArray.indices) {
                            val pointArray = pointStrArray[i].split(",".toRegex())
                                .dropLastWhile { it.isEmpty() }.toTypedArray()
                            if (pointArray.size >= 2) {
                                points[i]!![0] = parseFloat(pointArray[0])
                                points[i]!![1] = parseFloat(pointArray[1])
                            }
                        }
                        DanmakuFactory.fillLinePathData(
                            item, points, mDispScaleX,
                            mDispScaleY
                        )
                    }
                }
            }
        }
    }

    private fun decodeXmlString(title: String): String {
        if ('&' !in title) {
            return title
        }
        var title = title
        if (title.contains("&amp;")) {
            title = title.replace("&amp;", "&")
        }
        if (title.contains("&quot;")) {
            title = title.replace("&quot;", "\"")
        }
        if (title.contains("&gt;")) {
            title = title.replace("&gt;", ">")
        }
        if (title.contains("&lt;")) {
            title = title.replace("&lt;", "<")
        }
        if (title.contains("&apos;")) {
            title = title.replace("&apos;", "'")
        }
        return title
    }

    private fun isPercentageNumber(number: String?): Boolean {
        //return number >= 0f && number <= 1f;
        return number != null && number.contains(".")
    }

    private fun parseFloat(floatStr: String): Float {
        return try {
            floatStr.toFloat()
        } catch (e: NumberFormatException) {
            0.0f
        }
    }

    private fun parseInteger(intStr: String): Int {
        return try {
            intStr.toInt()
        } catch (e: NumberFormatException) {
            0
        }
    }

    private fun parseLong(longStr: String): Long {
        return try {
            longStr.toLong()
        } catch (e: NumberFormatException) {
            0
        }
    }

    override fun setDisplayer(disp: IDisplayer?): BaseDanmakuParser {
        super.setDisplayer(disp)
        mDispScaleX = mDispWidth / DanmakuFactory.BILI_PLAYER_WIDTH
        mDispScaleY = mDispHeight / DanmakuFactory.BILI_PLAYER_HEIGHT
        return this
    }

}
