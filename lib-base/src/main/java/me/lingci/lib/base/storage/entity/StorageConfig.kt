package me.lingci.lib.base.storage.entity

/**
 * 存储配置信息类
 */
sealed class StorageConfig(
    open val id: String, // 存储唯一标识
    open val name: String, // 存储名称
    open val type: StorageType // 存储类型
) {

    // 本地存储配置
    data class LocalStorageConfig(
        override val id: String,
        override val name: String,
        val rootPath: String // 根目录路径
    ) : StorageConfig(id, name, StorageType.LOCAL_STORAGE)

    // SMB存储配置
    data class SmbStorageConfig(
        override val id: String,
        override val name: String,
        val server: String, // 服务器地址
        val port: Int = 445, // 端口，默认445
        val share: String, // 共享名称
        val username: String?, // 用户名，可为空
        val password: String?, // 密码，可为空
        val domain: String? = null // 域，可为空
    ) : StorageConfig(id, name, StorageType.SMB)

    // WebDAV存储配置
    data class WebDavStorageConfig(
        override val id: String,
        override val name: String,
        val url: String, // WebDAV服务器URL
        val username: String?, // 用户名，可为空
        val password: String?, // 密码，可为空
        val isHttps: Boolean = true // 是否使用HTTPS
    ) : StorageConfig(id, name, StorageType.WEBDAV)

}