package me.lingci.lib.base.okhttp

abstract class BaseRequest<T : BaseRequest<T>>(protected var url: String) {

    protected val headers = mutableMapOf<String, String>().apply { putAll(OkUtil.commonHeaders) }
    protected val params = mutableMapOf<String, String>()
    protected var useUnsafe = false

    @Suppress("UNCHECKED_CAST")
    fun webUa() = apply { headers["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.0.0" } as T

    @Suppress("UNCHECKED_CAST")
    fun cookie(key: String, value: String) = apply { headers["Cookie"] = "$key=$value" } as T

    @Suppress("UNCHECKED_CAST")
    fun header(key: String, value: String?) = apply { if (value.isNullOrBlank().not()) {headers[key] = value} } as T

    @Suppress("UNCHECKED_CAST")
    fun headers(map: MutableMap<String, String>) = apply { headers.putAll(map) } as T

    @Suppress("UNCHECKED_CAST")
    fun param(key: String, value: Any?) = apply { value?.let { params[key] = it.toString() } } as T

    @Suppress("UNCHECKED_CAST")
    fun params(map: MutableMap<String, String>) = apply { params.putAll(map) } as T

    @Suppress("UNCHECKED_CAST")
    fun unsafe() = apply { useUnsafe = true } as T

    protected fun getClient() = if (useUnsafe) OkUtil.unsafeClient else OkUtil.client

    abstract fun get() : String

    abstract suspend fun execute(): Result<String>

}