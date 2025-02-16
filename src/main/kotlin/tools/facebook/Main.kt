package name.blackcap.socialbutterfly.tools.facebook

/* Incomplete, because I decided not to support Facebook for now, because
   its API is terminally enshittified. */

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import name.blackcap.socialbutterfly.jschema.Channel
import name.blackcap.socialbutterfly.jschema.FacebookCredentials
import name.blackcap.socialbutterfly.jschema.FacebookPlatform
import name.blackcap.socialbutterfly.lib.*
import name.blackcap.socialbutterfly.tools.mastodon.Subcommands
import name.blackcap.socialbutterfly.tools.mastodon.config
import name.blackcap.socialbutterfly.tools.mastodon.state
import java.awt.Desktop
import java.awt.GraphicsEnvironment
import java.net.URI
import java.util.*
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    setName("facebook")
    if (args.isEmpty()) {
        printError("expecting subcommend")
        exitProcess(2)
    }
    val key = getPassword("Key: ", true)
    loadConfigState(key).run { config = first; state = second }
    val subcommand = args[0]
    val scArgs = args.sliceArray(1 until args.size)
    val method = Subcommands::class.members.firstOrNull { it.name == subcommand }
    if (method == null) {
        printError("${see(subcommand)} - unknown subcommand")
        exitProcess(1)
    } else {
        method.call(Subcommands, scArgs)
    }
    saveConfig(config, key)
    saveState(state)
}

private const val REDIRECT_URI = "https://blackcap.name/cgi-bin/display_state,token.cgi"
private const val SCOPES = "user_posts"

object Subcommands {
    fun create(args: Array<String>) {
        if (args.size != 1) {
            printError("expecting Facebook client ID")
            exitProcess(2)
        }
        val appId = args[0]
        val exists = config.platforms.values.firstOrNull { it is FacebookPlatform }
        if (exists != null) {
            printError("Facebook platform already exists")
            exitProcess(1)
        }
        val newPlatform = FacebookPlatform(clientId = appId)
        val id = config.platforms.store(newPlatform)
        print("platform ${id} created")
    }

    fun token(args: Array<String>) {
        if (args.size != 1) {
            printError("expecting a platform ID (and nothing else)")
            exitProcess(2)
        }
        val platformId = args[0]
        val platform = config.platforms[args[0]]
        if (platform == null) {
            printError("platform ${see(platformId)} not found")
            exitProcess(1)
        }
        if (platform !is FacebookPlatform) {
            printError("${see(platformId)} is not a facebook platform")
            exitProcess(1)
        }

        val state = UUID.randomUUID().toString()
        val url = URLBuilder().run {
            protocol = URLProtocol.HTTPS
            host = "www.facebook.com"
            path("/v22.0/dialog/oauth")
            parameters.run {
                append("response_type", "token")
                append("client_id", platform.clientId)
                append("redirect_uri", REDIRECT_URI)
                append("scope", SCOPES)
                append("state", state)
            }
            buildString()
        }
        if (Desktop.isDesktopSupported() && !GraphicsEnvironment.isHeadless()) {
            println("Use the browser to log in to Facebook and give SocialButterfly")
            println("access to your account.")
            Desktop.getDesktop().browse(URI(url))
        } else {
            println("Use your browser to navigate to the following URL:")
            println(url)
        }

        while (true) {
            val state2 = readLine("Enter state: ")
            if (state == state2) {
                break
            }
            printError("invalid entry, please try again")
        }
        val token = readLine("Enter token: ")
        val (userId, userName) = makeHttpClient().use { httpClient ->
            runBlocking {
                val idResponse = httpClient.get {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = platform.host!!
                        path("/v22.0/me")
                        parameters.run {
                            append("fields", "id,email")
                            append("access_token", token)
                        }
                    }
                }
                verifyResponse(idResponse)
                val idResponseJson = idResponse.body<JsonElement>() as JsonObject
                Pair((idResponseJson["id"] as JsonPrimitive).content,
                    (idResponseJson["email"] as JsonPrimitive).content)
            }
        }
        val channelId = config.channels.store(Channel(platform = platformId,
            credentials = FacebookCredentials(userName, userId, token)))
        println("channel ${channelId} created")
    }

    fun post(args: Array<String>) {
        if (args.size != 1) {
            printError("expecting channel ID (and nothing else)")
            exitProcess(2)
        }
        val channelId = args[0]
        val channel = config.channels[channelId]
        if (channel == null) {
            printError("${see(channelId)} - unknown channel ID")
            exitProcess(1)
        }
        val platform = config.platforms[channel.platform] as FacebookPlatform
        /* NOT FINISHED */
    }

    fun get(args: Array<String>) {

    }
}

private suspend fun verifyResponse(dubious: HttpResponse) {
    if (!dubious.status.isSuccess()) {
        printError("unexpected ${dubious.status.value} response")
        printError("body is: ${dubious.bodyAsText()}")
        exitProcess(1)
    }
}

