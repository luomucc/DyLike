package me.lingci.lib.base.storage

import kotlinx.coroutines.flow.Flow
import me.lingci.lib.base.storage.entity.FileEntity

/**
 * 文件系统接口，定义了所有文件操作的标准方法
 */
interface IStorage {

    /**
     * 根目录
     */
    fun rootFile(): FileEntity

    /**
     * 完整目录
     */
    fun fullPath(path: String): String

    /**
     * 获取认证
     */
    fun getToken(): String

    /**
     * 测试连接
     */
    suspend fun testConnect(): Boolean

    /**
     * 获取指定路径下的文件列表
     * @param path 路径
     * @param refresh 是否强制刷新（不使用缓存）
     * @return 文件列表的Flow
     */
    suspend fun listFiles(path: String, refresh: Boolean = false): Flow<List<FileEntity>>

    /**
     * 获取指定路径下的文件列表
     * @param path 路径
     * @param refresh 是否强制刷新（不使用缓存）
     * @return 文件的Flow
     */
    suspend fun listFile(path: String, refresh: Boolean): Flow<FileEntity>

    /**
     * 获取单个文件的详细信息
     * @param path 文件路径
     * @return 文件信息
     */
    suspend fun getFileInfo(path: String): FileEntity?

    /**
     * 创建目录
     * @param path 目录路径
     * @return 是否创建成功
     */
    suspend fun createDirectory(path: String): Boolean

    /**
     * 重命名文件或目录
     * @param oldPath 旧路径
     * @param newName 新名称
     * @return 是否重命名成功
     */
    suspend fun rename(oldPath: String, newName: String): Boolean

    /**
     * 删除文件或目录
     * @param path 文件或目录路径
     * @return 是否删除成功
     */
    suspend fun delete(path: String): Boolean

    /**
     * 复制文件
     * @param sourcePath 源文件路径
     * @param targetPath 目标路径
     * @param progressCallback 进度回调
     * @return 是否复制成功
     */
    suspend fun copy(
        sourcePath: String,
        targetPath: String,
        progressCallback: (Float) -> Unit = {}
    ): Boolean

    /**
     * 移动文件
     * @param sourcePath 源文件路径
     * @param targetPath 目标路径
     * @param progressCallback 进度回调
     * @return 是否移动成功
     */
    suspend fun move(
        sourcePath: String,
        targetPath: String,
        progressCallback: (Float) -> Unit = {}
    ): Boolean

    /**
     * 下载文件（主要用于网络存储）
     * @param remotePath 远程文件路径
     * @param localPath 本地保存路径
     * @param progressCallback 进度回调
     * @return 是否下载成功
     */
    suspend fun download(
        remotePath: String,
        localPath: String,
        progressCallback: (Float) -> Unit = {}
    ): Boolean

    /**
     * 上传文件（主要用于网络存储）
     * @param localPath 本地文件路径
     * @param remotePath 远程保存路径
     * @param progressCallback 进度回调
     * @return 是否上传成功
     */
    suspend fun upload(
        localPath: String,
        remotePath: String,
        progressCallback: (Float) -> Unit = {}
    ): Boolean

    /**
     * 搜索文件
     * @param query 搜索关键词
     * @param rootPath 搜索根目录
     * @return 搜索结果的Flow
     */
    fun searchFiles(query: String, rootPath: String = "/"): Flow<List<FileEntity>>

    /**
     * 释放资源
     */
    fun release()

}
