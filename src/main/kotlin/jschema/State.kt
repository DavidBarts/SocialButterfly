package name.blackcap.socialbutterfly.jschema

import kotlinx.serialization.Serializable

@Serializable
data class State(
    val posts: MutableMap<String, Post>,
    val failures: MutableSet<Failure>
)

@Serializable
data class Post(
    val text: String,
    val media: String? = null,
    var isPosted: Boolean = false
)

@Serializable
data class Failure(
    val post: String,
    val distribution: Distribution
)