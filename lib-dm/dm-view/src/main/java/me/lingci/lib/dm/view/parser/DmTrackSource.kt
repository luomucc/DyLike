package me.lingci.lib.dm.view.parser

import master.flame.danmaku.danmaku.parser.IDataSource
import me.lingci.lib.dm.view.entity.DmTrack

/**
 *   @author : happyc
 *   time    : 2026/01/24
 *   desc    :
 *   version : 1.0
 */
class DmTrackSource(

): IDataSource<List<DmTrack>> {

    override fun data(): List<DmTrack> {


        return emptyList()
    }

    override fun release() {

    }
}