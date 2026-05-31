package me.lingci.lib.base.recycle

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 *   @author : happyc
 *   time    : 2025/03/03
 *   desc    :
 *   version : 1.0
 */
@Serializable
abstract class BaseModel(
    var id: String = UUID.randomUUID().toString()
) {
}