package name.blackcap.socialbutterfly.jschema

import kotlinx.serialization.Serializable

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
    var tokenExpires: Long
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
    abstract val isPermanent: Boolean
}

@Serializable
data class MastodonPlatform(
    override val host: String,
    val clientId: String,
    val clientSecret: String
): Platform() {
    override val isPermanent = false
}

@Serializable
data class FacebookPlatform(
    override val host: String = "graph.facebook.com",
    val clientId: String,
): Platform() {
    override val isPermanent = true
}

@Serializable
data class TwitterPlatform(
    override val host: String = "api.x.com",
    val clientId: String
): Platform() {
    override val isPermanent: Boolean = true
}

@Serializable
data class BlueskyPlatform(
    override val host: String = "bsky.social",
): Platform() {
    override val isPermanent: Boolean = true
}

/* other platforms will go here */

/* a distribution is a set of channel ID's that index into the channels map */
@Serializable
abstract class Distribution(): MutableSet<String>