package name.blackcap.socialbutterfly.lib

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import name.blackcap.socialbutterfly.jschema.*
import java.util.*

/* for managing configuration (more sensitive, less volatile) and state
   more volatile, less sensitive */

object ConfigState {
    lateinit var config: Config
    lateinit var state: State
    var dirty: Boolean = false

    fun load(key: CharArray): Unit {
        config = if (CONFIG_FILE.exists()) {
            Json.decodeFromString<Config>(readEncrypted(CONFIG_FILE, key))
        } else {
            /* TODO: create a set of standard platforms here? */
            Config(
                channels = HashMap<String, Channel>(),
                distributions = HashMap<String, Distribution>(),
                platforms = HashMap<String, Platform>(),
                installation = UUID.randomUUID().toString()
            )
        }
        state = if (STATE_FILE.exists()) {
            Json.decodeFromString<State>(STATE_FILE.readText(Charsets.UTF_8))
        } else {
            State(
                posts = HashMap<String, Post>(),
                failures = HashSet<Failure>()
            )
        }
    }

    fun save(key: CharArray) {
        writeEncrypted(CONFIG_FILE, key, Json.encodeToString(config))
        STATE_FILE.writeText(Json.encodeToString(state), Charsets.UTF_8)
        dirty = false
    }
}
