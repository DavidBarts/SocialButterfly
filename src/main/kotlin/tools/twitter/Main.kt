package name.blackcap.socialbutterfly.tools.twitter

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import name.blackcap.socialbutterfly.jschema.*
import name.blackcap.socialbutterfly.lib.*
import java.awt.Desktop
import java.awt.GraphicsEnvironment
import java.io.File
import java.net.URI
import java.util.*
import kotlin.system.exitProcess

lateinit var config: Config
lateinit var state: State

fun main(args: Array<String>) {
    setName("twitter")
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

private const val REDIRECT_URI = "https://blackcap.name/cgi-bin/display_code,state.cgi"
private const val SCOPES = "offline.access tweet.read tweet.write users.read media.write"

object Subcommands {
    fun create(args: Array<String>) {
        if (args.size != 1) {
            printError("expecting Twitter client ID")
            exitProcess(2)
        }
        val appId = args[0]
        val exists = config.platforms.values.firstOrNull { it is TwitterPlatform }
        if (exists != null) {
            printError("twitter platform already exists")
            exitProcess(1)
        }
        val newPlatform = TwitterPlatform(clientId = appId)
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
        if (platform !is TwitterPlatform) {
            printError("${see(platformId)} is not a twitter platform")
            exitProcess(1)
        }
        val (challenge, verifier) = pkcePair()
        val state = UUID.randomUUID().toString()
        val url = URLBuilder().run {
            protocol = URLProtocol.HTTPS
            host = "x.com"
            path("/i/oauth2/authorize")
            parameters.run {
                append("response_type", "code")
                append("client_id", platform.clientId)
                append("redirect_uri", REDIRECT_URI)
                append("scope", SCOPES)
                append("state", state)
                append("code_challenge", challenge)
                append("code_challenge_method", "S256")
            }
            buildString()
        }
        if (Desktop.isDesktopSupported() && !GraphicsEnvironment.isHeadless()) {
            println("Use the browser to log in to Twitter and give SocialButterfly")
            println("access to your account.")
            Desktop.getDesktop().browse(URI(url))
        } else {
            println("Use your browser to navigate to the following URL:")
            println(url)
        }

        val code = readLine("Enter code: ")
        makeHttpClient(json = true).use { httpClient ->
            runBlocking {
                val tokenResponse = httpClient.submitForm(
                    url = "https://${platform.host}/2/oauth2/token",
                    formParameters = parameters {
                        append("code", code)
                        append("grant_type", "authorization_code")
                        append("client_id", platform.clientId)
                        append("redirect_uri", REDIRECT_URI)
                        append("code_verifier", verifier)
                    }
                )
                verifyResponse(tokenResponse)
                val tokenJson: JsonElement = tokenResponse.body()
                /* debug */
                val prettyPrinter = Json {
                    prettyPrint = true
                }
                prettyPrinter.encodeToStream(tokenJson, System.out)
                println()
                /* gubed */
                if (tokenJson is JsonObject) {
                    val chanId = config.channels.store(
                        Channel(platform = platformId, credentials = makeCredentials(tokenJson)))
                    println("channel ${see(chanId)} created")
                } else {
                    printError("response is not JsonObject")
                    exitProcess(1)
                }
            }
        }
        while (true) {
            val state2 = readLine("Enter state: ")
            if (state == state2) {
                break
            }
            printError("invalid entry, please try again")
        }
    }

    fun revoke(args: Array<String>) {
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
        val credentials = channel.credentials as? TwitterCredentials
        if (credentials == null) {
            printError("${see(channelId)} - is not a twitter channel")
            exitProcess(1)
        }
        val platform = config.platforms[channel.platform] as TwitterPlatform
        makeHttpClient(json = true).use { httpClient ->
            runBlocking {
                val tokenResponse = httpClient.submitForm(
                    url = "https://${platform.host}/2/oauth2/revoke",
                    formParameters = parameters {
                        append("token", credentials.token)
                        append("client_id", platform.clientId)
                    }
                )
                verifyResponse(tokenResponse)
            }
        }
        config.distributions.values.forEach {
            it.remove(channelId)
        }
        config.channels.remove(channelId)
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
        val platform = config.platforms[channel.platform] as TwitterPlatform
        val credentials = channel.credentials as TwitterCredentials
        makeHttpClient(json = true).use { httpClient ->
            runBlocking {
                val response = doAuthRequest(
                    shouldRefreshBefore = { shouldRefresh(credentials) },
                    refreshBefore = { doRefresh(httpClient, channel) },
                    makeRequest = {
                        httpClient.post("https://${platform.host}/2/tweets") {
                            bearerAuth(credentials.token)
                            contentType(ContentType.Application.Json)
                            setBody(buildJsonObject {
                                put("text", contents)
                            })
                        }
                    }
                )
                verifyResponse(response)
                val responseJson: JsonElement = response.body()
                val postId = ((responseJson as JsonObject)["data"] as JsonObject)["id"]
                println("post ${postId} created")
            }
        }
    }

    fun image(args: Array<String>) {
        /* post an image (together with text) */
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
        val text = readLine("Post text: ")
        val imageName = readLine("Post image: ")
        val imageFile = File(imageName)
        val imageProblem = verifyImage(imageFile)
        if (imageProblem != null) {
            printError("invalid image: ${imageProblem}")
            exitProcess(1)
        }
        val imageType = getMimeType(imageFile)
        val platform = config.platforms[channel.platform] as TwitterPlatform
        val credentials = channel.credentials as TwitterCredentials
        makeHttpClient(json = true).use { httpClient ->
            runBlocking {
                println("initializing...")
                val initResponse = doAuthRequest(
                    shouldRefreshBefore = { shouldRefresh(credentials) },
                    refreshBefore = { doRefresh(httpClient, channel) },
                    makeRequest = {
                        httpClient.submitForm(
                            url = "https://${platform.host}/2/media/upload",
                            formParameters = parameters {
                                append("command", "INIT")
                                append("media_type", imageType)
                                append("total_bytes", imageFile.length().toString())
                                append("media_category", "tweet_image")
                            }
                        ) { bearerAuth(credentials.token) }
                    }
                )
                verifyResponse(initResponse)
                val mediaId = (((initResponse.body() as JsonObject)["data"] as JsonObject)["id"] as JsonPrimitive).content
                println("uploading...")
                val imageBytes = withContext(Dispatchers.IO) { imageFile.readBytes() }
                val uploadResponse = doAuthRequest(
                    shouldRefreshBefore = { shouldRefresh(credentials) },
                    refreshBefore = { doRefresh(httpClient, channel) },
                    makeRequest = {
                        httpClient.submitFormWithBinaryData(
                            url = "https://${platform.host}/2/media/upload",
                            formData = formData {
                                append("command", "APPEND")
                                append("media_id", mediaId)
                                append("segment_index", "0")
                                append("media", imageBytes, Headers.build {
                                    append(HttpHeaders.ContentType, imageType)
                                    append(HttpHeaders.ContentDisposition, "filename=\"${sanitizedName(imageFile)}\"")
                                })
                            }
                        ) { bearerAuth(credentials.token) }
                    }
                )
                verifyResponse(uploadResponse)
                println("finalizing...")
                val finalizeResponse = doAuthRequest(
                    shouldRefreshBefore = { shouldRefresh(credentials) },
                    refreshBefore = { doRefresh(httpClient, channel) },
                    makeRequest = {
                        httpClient.submitForm(
                            url = "https://${platform.host}/2/media/upload",
                            formParameters = parameters {
                                append("command", "FINALIZE")
                                append("media_id", mediaId)
                            }
                        ) { bearerAuth(credentials.token) }
                    }
                )
                verifyResponse(finalizeResponse)
                println("posting...")
                val postResponse = doAuthRequest(
                    shouldRefreshBefore = { shouldRefresh(credentials) },
                    refreshBefore = { doRefresh(httpClient, channel) },
                    makeRequest = {
                        httpClient.post("https://${platform.host}/2/tweets") {
                            bearerAuth(credentials.token)
                            contentType(ContentType.Application.Json)
                            setBody(buildJsonObject {
                                put("text", text)
                                putJsonObject("media") {
                                    putJsonArray("media_ids") {
                                        add(mediaId)
                                    }
                                }
                            })
                        }
                    }
                )
                verifyResponse(postResponse)
                val postId = (((postResponse.body() as JsonObject)["data"] as JsonObject)["id"] as JsonPrimitive).content
                println("post ${postId} created with image ${mediaId}")
            }
        }
    }

    fun get(args: Array<String>) {
        if (args.size != 2) {
            printError("expecting channel ID and post (tweet) ID")
            exitProcess(2)
        }
        val channelId = args[0]
        val postId = args[1]
        val channel = config.channels[channelId]
        if (channel == null) {
            printError("${see(channelId)} - unknown channel ID")
            exitProcess(1)
        }
        val host = config.platforms[channel.platform]?.host
        val credentials = channel.credentials as TwitterCredentials
        makeHttpClient(json = true).use { httpClient ->
            runBlocking {
                val getResponse = doAuthRequest(
                    shouldRefreshBefore = { shouldRefresh(credentials) },
                    refreshBefore = { doRefresh(httpClient, channel) },
                    makeRequest = {
                        httpClient.get("https://${host}/2/tweets/${postId}") {
                            bearerAuth(credentials.token)
                        }
                    }
                )
                verifyResponse(getResponse)
                val getJson: JsonElement = getResponse.body()
                val prettyPrinter = Json {
                    prettyPrint = true
                }
                prettyPrinter.encodeToStream(getJson, System.out)
                println()
            }
        }
    }
}

private fun makeCredentials(jsonElement: JsonElement): TwitterCredentials {
    val jsonObject = jsonElement as JsonObject
    val token = (jsonObject["access_token"] as JsonPrimitive).content
    val refreshToken = (jsonObject["refresh_token"] as JsonPrimitive).content
    val expiresIn = (jsonObject["expires_in"] as JsonPrimitive).long
    return TwitterCredentials(
        username = "(unknown)", token = token, refreshToken = refreshToken,
        tokenExpires = System.currentTimeMillis() + expiresIn * 1000L)
}

private suspend fun verifyResponse(dubious: HttpResponse) {
    if (!dubious.status.isSuccess()) {
        printError("unexpected ${dubious.status.value} response")
        printError("body is: ${dubious.bodyAsText()}")
        exitProcess(1)
    }
}

private suspend fun shouldRefresh(credentials: TwitterCredentials): Boolean {
    val SLOP: Long = 5L * 60L * 1000L  /* 5 minutes */
    return System.currentTimeMillis() - SLOP > credentials.tokenExpires
}

private suspend fun doRefresh(httpClient: HttpClient, channel: Channel) {
    val credentials = channel.credentials as? TwitterCredentials
        ?: throw IllegalArgumentException("not a twitter channel")
    val platform = config.platforms[channel.platform] as TwitterPlatform
    println("refreshing old token")
    runBlocking {
        val refreshResponse = makeHttpClient(json = true).use {
            it.submitForm(
                url = "https://${platform.host}/2/oauth2/token",
                formParameters = parameters {
                    append("refresh_token", credentials.refreshToken)
                    append("grant_type", "refresh_token")
                    append("client_id", platform.clientId)
                }
            )
        }
        if(!refreshResponse.status.isSuccess()) {
            throw TokenRefreshException("refresh failed with status ${refreshResponse.status.value}")
        }
        channel.credentials.let {
            makeCredentials(refreshResponse.body()).run {
                it.token = token
                it.tokenExpires = tokenExpires
                it.refreshToken = refreshToken
            }
        }
    }
}
