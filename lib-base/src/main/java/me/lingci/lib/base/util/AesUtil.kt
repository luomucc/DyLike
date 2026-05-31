package me.lingci.lib.base.util

import android.util.Base64
import me.lingci.lib.base.BuildConfig
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * @author : happyc
 * time    : 2022/05/09
 * desc    : AES的加密和解密
 * version : 1.0
 */
@Suppress("SpellCheckingInspection")
object AesUtil {

    /**
     * 密钥 (需要前端和后端保持一致)
     * 从 BuildConfig 读取，配置在 local.properties 中
     */
    private val KEY: String get() = BuildConfig.AES_KEY
    private val IV: String get() = BuildConfig.AES_IV

    /**
     * AES加密为base 64 code
     *
     * @param content 待加密的内容
     * @return 加密后的base 64 code
     */
    @JvmStatic
    fun aesEncrypt(content: String): String {
        return try {
            val bytes = aesCbcEncrypt(
                content.toByteArray(StandardCharsets.UTF_8),
                KEY.toByteArray(StandardCharsets.UTF_8),
                IV.toByteArray(StandardCharsets.UTF_8)
            )
            base64Encode(bytes)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 将base 64 code AES解密
     *
     * @param encryptStr 待解密的base 64 code
     * @return 解密后的string
     */
    @JvmStatic
    fun aesDecrypt(encryptStr: String): String {
        return try {
            String(aesDecryptByte(encryptStr, IV))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 将base 64 code AES解密
     *
     * @param encryptStr 待解密的base 64 code
     * @return 解密后的string
     */
    @JvmStatic
    fun aesDecrypt(encryptStr: String, iv: String): String {
        return try {
            String(aesDecryptByte(encryptStr, iv))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 将base 64 code AES解密
     *
     * @param encryptStr 待解密的base 64 code
     * @param iv         解密密钥
     * @return 解密后的string
     */
    @JvmStatic
    fun aesDecryptByte(encryptStr: String, iv: String): ByteArray {
        return try {
            aesCbcDecrypt(
                base64Decode(encryptStr),
                KEY.toByteArray(StandardCharsets.UTF_8),
                iv.toByteArray(StandardCharsets.UTF_8)
            )
        } catch (e: Exception) {
            "".toByteArray()
        }
    }

    @JvmStatic
    fun aesCbcEncrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = getCipher(Cipher.ENCRYPT_MODE, key, iv)
        return cipher.doFinal(data)
    }

    @JvmStatic
    fun aesCbcDecrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = getCipher(Cipher.DECRYPT_MODE, key, iv)
        return cipher.doFinal(data)
    }

    @JvmStatic
    private fun getCipher(mode: Int, key: ByteArray, iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        // 因为AES的加密块大小是128bit(16byte), 所以key是128、192、256bit无关
        val secretKeySpec = SecretKeySpec(key, "AES")
        cipher.init(mode, secretKeySpec, IvParameterSpec(iv))
        return cipher
    }

    /**
     * base 64 encode
     *
     * @param bytes 待编码的byte[]
     * @return 编码后的base 64 code
     */
    @JvmStatic
    fun base64Encode(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * base 64 decode
     *
     * @param base64Code 待解码的base 64 code
     * @return 解码后的byte[]
     */
    @JvmStatic
    fun base64Decode(base64Code: String): ByteArray {
        return if (base64Code.isBlank()) {
            "".toByteArray()
        } else {
            Base64.decode(base64Code, Base64.NO_WRAP)
        }
    }

}