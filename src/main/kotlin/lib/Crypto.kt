package name.blackcap.socialbutterfly.lib

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

const val ITERATIONS = 390000
const val IV_LENGTH = 16
const val KEY_LENGTH = 256
const val SALT_LENGTH = 16
const val ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5Padding"
const val KEY_ALGORITHM = "AES"
const val SECRET_KEY_FACTORY = "PBKDF2WithHmacSHA256"

val CHARSET = Charsets.UTF_8

fun writeEncrypted(file: File, key: CharArray, string: String): Unit {
    BufferedOutputStream(FileOutputStream(file)).use { output ->
        val secureRandom = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
        output.write(salt)
        val iv = ByteArray(IV_LENGTH).also { secureRandom.nextBytes(it) }
        output.write(iv)
        val secretKey = getSecretKey(key, salt)
        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        output.write(cipher.doFinal(string.toByteArray(CHARSET)))
    }
}

fun readEncrypted(file: File, key: CharArray): String {
    BufferedInputStream(FileInputStream(file)).use { input ->
        val salt = ByteArray(SALT_LENGTH).also { input.read(it) }
        val iv = ByteArray(IV_LENGTH).also { input.read(it) }
        val secretKey = getSecretKey(key, salt)
        val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        return cipher.doFinal(input.readBytes()).toString(CHARSET)
    }
}

fun getSecretKey(password: CharArray, salt: ByteArray): SecretKey {
    val factory = SecretKeyFactory.getInstance(SECRET_KEY_FACTORY)
    val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
    return SecretKeySpec(factory.generateSecret(spec).encoded, KEY_ALGORITHM)
}
