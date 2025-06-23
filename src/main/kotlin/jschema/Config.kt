package name.blackcap.socialbutterfly.jschema

import kotlinx.serialization.Serializable
import name.blackcap.socialbutterfly.lib.InstantSerializer
import java.time.Instant

@Serializable
data class Config(
    val channels: MutableMap<String, Channel>,
    val distributions: MutableMap<String, Distribution>,
    val platforms: MutableMap<String, Platform>,
    val installation: String
)

@Serializable data class Channel(
    var platform: String,
    val credentials: Credentials,
)

@Serializable
sealed class Credentials {
    abstract var username: String
}

@Serializable
data class MastodonCredentials(
    override var username: String,
    var token: String
): Credentials()

@Serializable
data class FacebookCredentials(
    override var username: String,
    var userId: String,
    var token: String,
): Credentials()

@Serializable
data class TwitterCredentials(
    override var username: String,
    var token: String,
    var refreshToken: String,
    @Serializable(with = InstantSerializer::class) var tokenExpires: Instant
): Credentials()

@Serializable
data class BlueskyCredentials(
    override var username: String,
    var password: String,
    var token: String,
    var refreshToken: String,
    var did: String
): Credentials()

/* other platform credentials will go here */

@Serializable
sealed class Platform {
    abstract val host: String
    abstract val isCentralized: Boolean
    abstract val maxChars: Int
}

@Serializable
data class MastodonPlatform(
    override val host: String,
    val clientId: String,
    val clientSecret: String
): Platform() {
    override val isCentralized = false
    override val maxChars = 500
}

@Serializable
data class TwitterPlatform(
    override val host: String = "api.x.com",
    val clientId: String
): Platform() {
    override val isCentralized: Boolean = true
    override val maxChars = 280
}

@Serializable
data class BlueskyPlatform(
    override val host: String = "bsky.social",
): Platform() {
    override val isCentralized: Boolean = true
    override val maxChars = 300
}

/* other platforms will go here */

@Serializable
data class Distribution(
    var name: String,
    val channels: MutableSet<String>
) {
    companion object {
        fun createUntitled(distributions: Map<String, Distribution>) : Distribution {
            val BASE = "Untitled"
            fun available(name: String) =
                distributions.values.firstOrNull { it.name == name } == null

            if (available(BASE)) {
                return Distribution(BASE, mutableSetOf())
            }
            var suffix: Int = 2
            while (true) {
                val name = "$BASE $suffix"
                if (available(name)) {
                    return Distribution(name, mutableSetOf())
                }
                suffix++
            }
        }
    }
}
