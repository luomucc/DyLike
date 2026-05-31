package me.lingci.lib.base.entity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.io.IOException

sealed class CallData<out T> {
    data class Loading(val boolean: Boolean): CallData<Nothing>()
    data class Success<out T>(val data: T) : CallData<T>()
    data class Error(val message: String) : CallData<Nothing>()
}

suspend fun fetchTitle(): CallData<TitleItem> {
    return try {
        CallData.Success(TitleItem())
    } catch (e: Exception) {
        CallData.Error("请求失败: ${e.message}")
    }
}

fun fetchTitles(): Flow<CallData<List<TitleItem>>> = flow {
    emit(CallData.Loading(true))
    try {
        emit(CallData.Success(mutableListOf()))
    } catch (e: IOException) {
        emit(CallData.Error("网络错误，请稍后重试。"))
    }
}

@JvmInline
value class DataId(val id: String)