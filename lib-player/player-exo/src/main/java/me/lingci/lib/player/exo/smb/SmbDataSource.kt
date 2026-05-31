package me.lingci.lib.player.exo.smb

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import jcifs.CIFSContext
import jcifs.context.SingletonContext
import jcifs.smb.SmbFile
import java.io.InputStream

@OptIn(UnstableApi::class)
class SmbDataSource(
    val cifsContext: CIFSContext = SingletonContext.getInstance()
) : BaseDataSource(true) {

    @UnstableApi
    open class Factory(
        val headers: Map<String, String>? = null
    ) : DataSource.Factory {

        override fun createDataSource(): DataSource {
            return if (headers == null) {
                SmbDataSource()
            } else {
                SmbDataSource(
                    SmbAuthManager.getContext(
                        headers["username"]?.trim(),
                        headers["password"]?.trim()
                    )
                )
            }
        }

    }

    private var smbInputStream: InputStream? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = 0
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)

        // 解析 SMB 地址，例如 smb://user:password@ip/share/file.mp4
        val smbFile = SmbFile(uri.toString(), cifsContext)
        // 链接验证权限
        smbFile.connect()

        // 处理 Seek 逻辑（ExoPlayer 会请求特定位置的数据）
        val inputStream = smbFile.inputStream
        if (dataSpec.position > 0) {
            inputStream.skip(dataSpec.position)
        }

        smbInputStream = inputStream

        // 确定读取长度
        val fileLength = smbFile.length()
        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            fileLength - dataSpec.position
        }

        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (readLength == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val bytesToRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            readLength
        } else {
            bytesRemaining.coerceAtMost(readLength.toLong()).toInt()
        }

        val bytesRead = smbInputStream?.read(buffer, offset, bytesToRead) ?: -1
        if (bytesRead == -1) return C.RESULT_END_OF_INPUT

        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }

        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        try {
            smbInputStream?.close()
        } finally {
            smbInputStream = null
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }

}