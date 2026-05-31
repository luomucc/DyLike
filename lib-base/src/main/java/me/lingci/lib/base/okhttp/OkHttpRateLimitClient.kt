package me.lingci.lib.base.okhttp

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * 创建带有令牌桶限流功能的 OkHttp 客户端
 */
fun rateLimitOkhttpClient(max: Int): OkHttpClient {

    // 添加令牌桶限流拦截器
    return OkHttpClient.Builder()
        .addInterceptor(TokenBucketRateLimitInterceptor(max))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
}

/**
 * OkHttp 令牌桶限流拦截器
 * 限制每秒最多3次请求
 */
class TokenBucketRateLimitInterceptor(max: Int) : Interceptor {
    // 初始化令牌桶（1秒n次请求 = 桶容量n，生成速率n个/秒）
    private val tokenBucket = TokenBucket(max, max.toDouble())

    @Throws(Exception::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        // 获取令牌（若没有则阻塞）
        tokenBucket.acquireToken()
        // 发送请求
        return chain.proceed(chain.request())
    }
}

/**
 * 令牌桶算法实现类
 * @param maxTokens 桶的最大容量（即单位时间内最大请求数）
 * @param tokenRate 令牌生成速率（个/秒）
 */
class TokenBucket(
    private val maxTokens: Int,
    private val tokenRate: Double
) {
    // 上次补充令牌的时间戳（毫秒）
    private val lastRefillTimestamp = AtomicLong(System.currentTimeMillis())
    // 当前可用令牌数
    private val availableTokens = AtomicLong(maxTokens.toLong())

    /**
     * 获取1个令牌，若暂时无法获取则阻塞等待
     */
    @Throws(InterruptedException::class)
    fun acquireToken() {
        while (true) {
            val currentTime = System.currentTimeMillis()
            val lastTime = lastRefillTimestamp.get()
            val timeElapsed = currentTime - lastTime // 距离上次补充令牌的时间（毫秒）

            // 补充这段时间内生成的新令牌
            if (timeElapsed > 1000) {
                // 计算应补充的令牌数 = 时间差（秒） * 令牌速率
                val tokensToAdd = (timeElapsed / 1000.0) * tokenRate
                if (tokensToAdd > 0) {
                    // 原子更新：防止多线程下重复补充令牌
                    if (lastRefillTimestamp.compareAndSet(lastTime, currentTime)) {
                        // 补充后令牌数不超过桶的最大容量
                        val newTokens = (availableTokens.get() + tokensToAdd).coerceAtMost(maxTokens.toDouble()).toLong()
                        availableTokens.set(newTokens)
                    }
                    continue // 补充后重新检查令牌是否可用
                }
            }

            // 尝试获取1个令牌
            if (availableTokens.get() > 0) {
                if (availableTokens.decrementAndGet() >= 0) {
                    return // 获取成功，放行请求
                } else {
                    availableTokens.incrementAndGet() // 若递减后为负，恢复原计数
                }
            }

            // 无令牌可用，短暂睡眠后重试（避免忙等）
            TimeUnit.MILLISECONDS.sleep(10)
        }
    }
}

fun main() {

    val tokenBucketRateLimitClient = rateLimitOkhttpClient(3)

    //val latch = CountDownLatch(11)
    val start = System.currentTimeMillis()
    for (i in 0 .. 100) {
        val newCall = tokenBucketRateLimitClient.newCall(
            Request.Builder().url("http://localhost:81").tag(i).get().build()
        )
        /*newCall.enqueue(object : Callback{
            override fun onFailure(call: Call, e: IOException) {
                println("${call.request().tag()} ${e.message}")
                latch.countDown()
            }

            override fun onResponse(call: Call, response: Response) {
                println("${response.request.tag()} ${response.body!!.string().replace("\n", "")}")
                latch.countDown()
            }

        })*/
        newCall.execute().let {
            println("${it.request.tag()} ${it.code} ${it.message}")
            println("$i ${it.body!!.string().replace("\n", "")}")
        }
    }
    // 等待所有请求处理完成
    //latch.await()
    println("end ${System.currentTimeMillis() - start}")
}