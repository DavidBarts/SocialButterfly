package name.blackcap.socialbutterfly.tools.bluesky

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import name.blackcap.socialbutterfly.jschema.*
import name.blackcap.socialbutterfly.lib.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

lateinit var config: Config
lateinit var state: State

private val BSKY_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'000Z'").apply {
    timeZone = TimeZone.getTimeZone("GMT")
}

fun main(args: Array<String>) {
    setName("bluesky")
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

object Subcommands {
    fun create(args: Array<String>) {
        if (args.isNotEmpty()) {
            printError("unexpected arguments")
            exitProcess(2)
        }
        val exists = config.platforms.values.firstOrNull { it is BlueskyPlatform }
        if (exists != null) {
            printError("bluesky platform already exists")
            exitProcess(1)
        }
        val newPlatform = BlueskyPlatform()
        val id = config.platforms.store(newPlatform)
        println("platform ${id} created")
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
        if (platform !is BlueskyPlatform) {
            printError("${see(platformId)} is not a bluesky platform")
            exitProcess(1)
        }
        val username = readLine("Username: ")
        val password = String(getPassword("Password: "))
        makeHttpClient(json = true).use { httpClient ->
            runBlocking {
                val tokenResponse = httpClient.post("https://${platform.host}/xrpc/com.atproto.server.createSession") {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("identifier", username)
                        put("password", password)
                    })
                }
                verifyResponse(tokenResponse)
                val tokenJson: JsonElement = tokenResponse.body()
                if (tokenJson is JsonObject) {
                    val chanId = config.channels.store(Channel(platform = platformId,
                        credentials = makeCredentials(username, password, tokenJson)))
                    println("channel ${chanId} created")
                } else {
                    printError("response is not JsonObject")
                    exitProcess(1)
                }
            }
        }
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
        val contents = readLine("Enter post: ")
        val platform = config.platforms[channel.platform] as BlueskyPlatform
        val credentials = channel.credentials as BlueskyCredentials
        /* need to add auto-refresh here */
        runBlocking {
            makeHttpClient(json = true, bearerToken = credentials.token).use { httpClient ->
                val response = doAuthRequest(
                    makeRequest = {
                        httpClient.post("https://${platform.host}/xrpc/com.atproto.repo.createRecord") {
                            contentType(ContentType.Application.Json)
                            bearerAuth(credentials.token)
                            setBody(buildJsonObject {
                                put("repo", credentials.did)
                                put("collection", "app.bsky.feed.post")
                                putJsonObject("record") {
                                    put("\$type", "app.bsky.feed.post")
                                    put("text", contents)
                                    put("createdAt", BSKY_DATE_FORMAT.format(Date()))
                                    putJsonArray("langs") { add("en-CA") }
                                }
                            })
                        }
                    },
                    shouldRefreshAfter = ::shouldRetry,
                    refreshAfter = { doRefresh(httpClient, channel) }
                )
                verifyResponse(response)
                val responseJson: JsonElement = response.body()
                if (responseJson is JsonObject) {
                    prettyPrint(responseJson)
                } else {
                    printError("response is not JsonObject")
                    exitProcess(1)
                }
            }
        }
    }

    fun get(args: Array<String>) {
        if (args.size != 2) {
            printError("expecting channel ID and commit.rev")
            exitProcess(2)
        }
        val channelId = args[0]
        val rkey = args[1]
        val channel = config.channels[channelId]
        if (channel == null) {
            printError("${see(channelId)} - unknown channel ID")
            exitProcess(1)
        }
        val platform = config.platforms[channel.platform]!!
        val credentials = channel.credentials as BlueskyCredentials
        makeHttpClient(json = true).use { httpClient ->
            runBlocking {
                val getResponse = httpClient.get {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = platform.host
                        path("/xrpc/com.atproto.repo.getRecord")
                        parameters.run {
                            append("repo", credentials.did)
                            append("collection", "app.bsky.feed.post")
                            append("rkey", rkey)
                        }
                    }
                }
                verifyResponse(getResponse)
                prettyPrint(getResponse.body())
            }
        }
    }
}

private fun prettyPrint(data: JsonElement) {
    val prettyPrinter = Json {
        prettyPrint = true
    }
    prettyPrinter.encodeToStream(data, System.out)
    println()
}

private fun makeCredentials(username: String, password: String, jsonObject: JsonObject): BlueskyCredentials {
    val accessJwt = (jsonObject["accessJwt"] as JsonPrimitive).content
    val refreshJwt = (jsonObject["refreshJwt"] as JsonPrimitive).content
    val did = (jsonObject["did"] as JsonPrimitive).content
    return BlueskyCredentials(username, password, token = accessJwt, refreshToken = refreshJwt, did)
}

private suspend fun verifyResponse(dubious: HttpResponse) {
    if (!dubious.status.isSuccess()) {
        printError("unexpected ${dubious.status.value} response")
        printError("body is: ${dubious.bodyAsText()}")
        exitProcess(1)
    }
}

private suspend fun doRefresh(httpClient: HttpClient, channel: Channel) {
    val host = config.platforms[channel.platform]?.host!!
    val credentials = channel.credentials as BlueskyCredentials
    printError("refreshing via token")
    val refreshResponse = httpClient.post("https://${host}/xrpc/com.atproto.server.refreshSession") {
        bearerAuth(credentials.refreshToken)
    }
    if (refreshResponse.status.isSuccess()) {
        val refreshJson: JsonElement = refreshResponse.body()
        if (refreshJson is JsonObject) {
            val newCredentials = makeCredentials(credentials.username, credentials.password, refreshJson)
            credentials.token = newCredentials.token
            credentials.refreshToken = newCredentials.refreshToken
            return
        }
    }
    printError("refresh token failed, creating new tokens")
    val tokenResponse = httpClient.post("https://${host}/xrpc/com.atproto.server.createSession") {
        contentType(ContentType.Application.Json)
        setBody(buildJsonObject {
            put("identifier", credentials.username)
            put("password", credentials.password)
        })
    }
    if (tokenResponse.status.isSuccess()) {
        val tokenJson: JsonElement = tokenResponse.body()
        if (tokenJson is JsonObject) {
            val newCredentials = makeCredentials(credentials.username, credentials.password, tokenJson)
            credentials.token = newCredentials.token
            credentials.refreshToken = newCredentials.refreshToken
            return
        }
    }
    throw TokenRefreshException("creating new tokens failed")
}

/* stupid bluesky sometimes returns 400 instead of 401 for an expired token, sigh */
private suspend fun shouldRetry(failed: HttpResponse): Boolean {
    if (failed.status.value == 401) {
        return true
    } else if (failed.status.value == 400) {
        val jsonBody: JsonElement = failed.body()
        if (jsonBody is JsonObject) {
            val error = jsonBody["error"]
            if (error is JsonPrimitive) {
                return error.content == "ExpiredToken"
            }
        }
    }
    return false
}
