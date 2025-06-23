package name.blackcap.socialbutterfly.jschema

import kotlinx.serialization.Serializable
import name.blackcap.socialbutterfly.lib.InstantSerializer
import java.time.Instant

@Serializable
data class State(
    val posts: MutableMap<String, Post>,
    val failures: MutableSet<Failure>
)

@Serializable
data class Post(
    val text: String,
    val media: String? = null,
    var isPosted: Boolean = false,
    @Serializable(with = InstantSerializer::class) val created: Instant = Instant.now(),
    var isEdited: Boolean = false
)

@Serializable
data class Failure(
    val post: String,
    val distribution: Distribution,
    @Serializable(with = InstantSerializer::class) val created: Instant = Instant.now(),
    var isRetried: Boolean = false
)
