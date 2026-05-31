package me.lingci.lib.base.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

class FlowManager {
    // 保存当前正在执行的Flow任务
    private var currentJob: Job? = null

    // 用于管理协程的作用域
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * 执行Flow，如果已有任务则先取消
     */
    fun executeFlow(flow: Flow<*>) {
        // 取消当前可能存在的任务
        currentJob?.cancel()

        // 启动新任务并保存Job引用
        currentJob = scope.launch {
            try {
                flow.collect {
                    // 处理Flow发射的数据
                    handleFlowData(it)
                }
            } catch (e: Exception) {
                // 处理取消或其他异常
                e.message?.contains("Job was cancelled")?.let {
                    if (!it) {
                        // 非取消异常的处理
                    }
                }
            } finally {
                // 任务结束后清除引用
                currentJob = null
            }
        }
    }

    /**
     * 处理Flow发射的数据
     */
    private fun handleFlowData(data: Any?) {
        // 具体的数据处理逻辑
    }

    /**
     * 手动取消当前任务
     */
    fun cancelCurrentTask() {
        currentJob?.cancel()
        currentJob = null
    }
}



fun main() = runBlocking {
    // 创建Flow管理器
    val flowManager = FlowManager()

    // 模拟多次触发Flow执行
    repeat(3) { index ->
        println("第${index + 1}次触发Flow执行")

        // 创建一个新的Flow，每500ms发射一个随机数
        val newFlow = createSampleFlow(index + 1)

        // 执行Flow，管理器会自动取消之前的任务
        flowManager.executeFlow(newFlow)

        // 等待1200ms，让Flow有时间发射一些数据
        delay(1200)
    }

    // 等待最后一个任务完成
    delay(2000)
}

/**
 * 创建一个示例Flow，用于演示
 */
fun createSampleFlow(flowId: Int): Flow<Int> {
    return flow {
        repeat(5) { count ->
            // 模拟耗时操作
            delay(500)

            // 发射数据
            val value = Random.nextInt(100)
            emit(value)

            println("Flow $flowId 发射数据: $value (第${count + 1}次)")
        }
        println("Flow $flowId 完成")
    }
}
