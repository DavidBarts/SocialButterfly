package name.blackcap.socialbutterfly.lib

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

fun pkcePair(): Pair<String, String> {
    val encoder = Base64.getUrlEncoder().withoutPadding()
    val rawVerifier = ByteArray(64)
    SecureRandom().nextBytes(rawVerifier)
    val verifier = encoder.encodeToString(rawVerifier)
    val verifierBytes = verifier.toByteArray(Charsets.US_ASCII)
    val sha256 = MessageDigest.getInstance("SHA-256")
    sha256.update(verifierBytes)
    val challenge = encoder.encodeToString(sha256.digest())
    return Pair(challenge, verifier)
}
