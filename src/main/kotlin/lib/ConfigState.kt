package name.blackcap.socialbutterfly.lib

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import name.blackcap.socialbutterfly.jschema.*
import java.util.*

/* for managing configuration (more sensitive, less volatile) and state
   more volatile, less sensitive */

fun loadConfigState(key: CharArray): Pair<Config, State> {
    val config = if (CONFIG_FILE.exists()) {
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
    val state = if (STATE_FILE.exists()) {
        Json.decodeFromString<State>(STATE_FILE.readText(Charsets.UTF_8))
    } else {
        State(
            posts = HashMap<String, Post>(),
            failures = HashSet<Failure>()
        )
    }
    return Pair(config, state)
}

fun saveConfig(config: Config, key: CharArray) {
    writeEncrypted(CONFIG_FILE, key, Json.encodeToString(config))
}

fun saveState(state: State) {
    STATE_FILE.writeText(Json.encodeToString(state), Charsets.UTF_8)
}
