package name.blackcap.socialbutterfly.lib

import java.util.UUID

fun <T> MutableMap<String, T>.store(value: T): String {
    val id = UUID.randomUUID().toString()
    this[id] = value
    return id
}
