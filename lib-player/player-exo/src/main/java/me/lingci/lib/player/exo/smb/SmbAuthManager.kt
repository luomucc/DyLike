package me.lingci.lib.player.exo.smb

import jcifs.CIFSContext
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import me.lingci.lib.base.util.Log

object SmbAuthManager {

    // 获取基础上下文
    private val baseContext = SingletonContext.getInstance()

    /**
     * 生成认证上下文
     * @param username 用户名，匿名登录传 null
     * @param password 密码，匿名登录传 null
     * @param domain 域名/工作组，通常传 null 或 "WORKGROUP"
     */
    fun getContext(username: String?, password: String?, domain: String? = null): CIFSContext {
        Log.d(this, "smb", username, password)
        return if (username == null) {
            // 匿名登录
            baseContext.withGuestCrendentials()
        } else {
            // 账号密码登录
            val auth = NtlmPasswordAuthenticator(domain, username, password)
            baseContext.withCredentials(auth)
        }
    }

}