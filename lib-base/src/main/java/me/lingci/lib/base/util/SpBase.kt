@file:Suppress("DEPRECATION")

package me.lingci.lib.base.util

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import androidx.core.content.edit

/**
 *   author : wazing.
 *   e-mail :
 *   time   : 2019/03/16
 *   desc   : https://blog.csdn.net/Jokey_wz/article/details/82350759
 *   version: 1.0
 */
@Suppress("DEPRECATION", "unused")
open class SpBase(context: Context) {

    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var debugMode by SPManager.boolean(false)

    var showDm by SPManager.boolean(true)

    var dmBold by SPManager.boolean(true)

    var dmConf by SPManager.string("{}")

    var videoScaleConf by SPManager.string("{}")

    var seSsData by SPManager.string("")

    var lastFolder by SPManager.string("")

    var lastSubtitleFolder by SPManager.string("")

    var preferredAudioLanguage by SPManager.string("ja")
    var preferredSubtitleLanguage by SPManager.string("zh")
    var subtitleFont by SPManager.string("")
    var subtitleFontSize by SPManager.int(20)

    var archivePasswordMapJson by SPManager.string("{}")

    var useFolders by SPManager.string("[]")

    var passStroke by SPManager.boolean(true)

    var downFolder by SPManager.string("")

    /* 调色 start */
    var customColor by SPManager.string("[]")

    var customGradient by SPManager.string("[]")

    var customColorScheme by SPManager.string("[]")
    /* 调色 end */

    var paletteOptions by SPManager.string("{}")

    var downPalette by SPManager.boolean(false)

    var showDmFps by SPManager.boolean(false)

    var iconDefault by SPManager.boolean(true)

    var longSpeed by SPManager.float(2.0F)

    var showSmooth by SPManager.boolean(true)

    object SPManager {

        fun int(defaultValue: Int = 0) = object : ReadWriteProperty<SpBase, Int> {

            override fun getValue(thisRef: SpBase, property: KProperty<*>): Int {
                return thisRef.preferences.getInt(property.name, defaultValue)
            }

            override fun setValue(thisRef: SpBase, property: KProperty<*>, value: Int) {
                thisRef.preferences.edit { putInt(property.name, value) }
            }
        }

        fun long(defaultValue: Long = 0L) = object : ReadWriteProperty<SpBase, Long> {

            override fun getValue(thisRef: SpBase, property: KProperty<*>): Long {
                return thisRef.preferences.getLong(property.name, defaultValue)
            }

            override fun setValue(thisRef: SpBase, property: KProperty<*>, value: Long) {
                thisRef.preferences.edit() { putLong(property.name, value) }
            }
        }

        fun boolean(defaultValue: Boolean = false) = object : ReadWriteProperty<SpBase, Boolean> {
            override fun getValue(thisRef: SpBase, property: KProperty<*>): Boolean {
                return thisRef.preferences.getBoolean(property.name, defaultValue)
            }

            override fun setValue(thisRef: SpBase, property: KProperty<*>, value: Boolean) {
                thisRef.preferences.edit { putBoolean(property.name, value) }
            }
        }

        fun float(defaultValue: Float = 0.0f) = object : ReadWriteProperty<SpBase, Float> {
            override fun getValue(thisRef: SpBase, property: KProperty<*>): Float {
                return thisRef.preferences.getFloat(property.name, defaultValue)
            }

            override fun setValue(thisRef: SpBase, property: KProperty<*>, value: Float) {
                thisRef.preferences.edit { putFloat(property.name, value) }
            }
        }

        fun string(defaultValue: String? = null) = object : ReadWriteProperty<SpBase, String?> {
            override fun getValue(thisRef: SpBase, property: KProperty<*>): String? {
                return thisRef.preferences.getString(property.name, defaultValue)
            }

            override fun setValue(thisRef: SpBase, property: KProperty<*>, value: String?) {
                thisRef.preferences.edit { putString(property.name, value) }
            }
        }

        fun setString(defaultValue: Set<String>? = null) = object : ReadWriteProperty<SpBase, Set<String>?> {
            override fun getValue(thisRef: SpBase, property: KProperty<*>): Set<String>? {
                return thisRef.preferences.getStringSet(property.name, defaultValue)
            }

            override fun setValue(thisRef: SpBase, property: KProperty<*>, value: Set<String>?) {
                thisRef.preferences.edit { putStringSet(property.name, value) }
            }
        }
    }

}
