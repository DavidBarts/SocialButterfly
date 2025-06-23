package name.blackcap.socialbutterfly.tools.mastodon
// for testing various parts of the Mastodon interface. insert call
// to the appropriate worker function to test

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import name.blackcap.socialbutterfly.Application
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
    setName("mastodon")
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
private const val SCOPES = "read:statuses write:statuses write:media"

object Subcommands {
    fun create(args: Array<String>) {
        if (args.size != 1) {
            printError("expecting a host name (and nothing else)")
            exitProcess(2)
        }
        val host = args[0]
        makeHttpClient(json = true).use { httpClient ->
            val clientName = Application.MYNAME.replace(" ", "") + " " + config.installation
            runBlocking {
                val response = httpClient.post("https://${host}//api/v1/apps") {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("client_name", clientName)
                        putJsonArray("redirect_uris") {
                            add(REDIRECT_URI)
                        }
                        put("scopes", SCOPES)
                    })
                }
                verifyResponse(response)
                val jsonBody: JsonElement = response.body()
                if (jsonBody is JsonObject) {
                    val id = config.platforms.store(
                        MastodonPlatform(
                            host,
                            (jsonBody["client_id"] as JsonPrimitive).content,
                            (jsonBody["client_secret"] as JsonPrimitive).content
                        )
                    )
                    println("platform $id created")
                } else {
                    printError("response is not JsonObject")
                    exitProcess(1)
                }
            }
        }
    }

    fun verify(args: Array<String>) {
        printError("command not supported")
        exitProcess(1)
    }

    fun token(args: Array<String>) {
        if (args.size != 1) {
            printError("expecting a platform ID (and nothing else)")
            exitProcess(2)
        }
        val id = args[0]
        val platform = config.platforms[args[0]]
        if (platform == null) {
            printError("platform ${see(id)} not found")
            exitProcess(1)
        }
        if (platform !is MastodonPlatform) {
            printError("${see(id)} is not a mastodon platfgrm")
            exitProcess(1)
        }
        val (challenge, verifier) = pkcePair()
        val state = UUID.randomUUID().toString()
        val url = URLBuilder().run {
            protocol = URLProtocol.HTTPS
            host = platform.host
            pathSegments = listOf("oauth", "authorize")
            parameters.run  {
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
            println("Use the browser to log in to Mastodon and give SocialButterfly")
            println("access to your account.")
            Desktop.getDesktop().browse(URI(url))
        } else {
            println("Use your browser to navigate to the following URL:")
            println(url)
        }
        val code = readLine("Enter code: ")
        makeHttpClient(json = true).use { httpClient ->
            runBlocking {
                val response = httpClient.post("https://${platform.host}/oauth/token") {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("grant_type", "authorization_code")
                        put("code", code)
                        put("client_id", platform.clientId)
                        put("client_secret", platform.clientSecret)
                        put("redirect_uri", REDIRECT_URI)
                        put("code_verifier", verifier)
                        put("scope", SCOPES)
                    })
                }
                verifyResponse(response)
                val jsonBody: JsonElement = response.body()
                if (jsonBody is JsonObject) {
                    val token = (jsonBody["access_token"] as JsonPrimitive).content
                    val chanId = config.channels.store(Channel(platform = id, credentials = MastodonCredentials(
                        username = "(unknown)", token = token)))
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
        val host = config.platforms[channel.platform]?.host
        val credentials = channel.credentials as MastodonCredentials
        makeHttpClient(json = true, bearerToken = credentials.token).use { httpClient ->
            runBlocking {
                val response = httpClient.post("https://${host}/api/v1/statuses") {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("status", contents)
                        put("visibility", "public")
                        put("language", "en")
                    })
                }
                verifyResponse(response)
                val postJson: JsonElement = response.body()
                val postId = ((postJson as JsonObject)["id"] as JsonPrimitive).content
                println("post ${postId} created")
            }
        }
    }

    fun image(args: Array<String>) {
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
        val platform = config.platforms[channel.platform] as MastodonPlatform
        val credentials = channel.credentials as MastodonCredentials
        val text = readLine("Post text: ")
        val imageName = readLine("Post image: ")
        val imageFile = File(imageName)
        val imageProblem = verifyImage(imageFile)
        if (imageProblem != null) {
            printError("invalid image: ${imageProblem}")
            exitProcess(1)
        }
        val altText = readLine("Alt text: ")
        val imageType = getMimeType(imageFile)
        makeHttpClient(json = true, bearerToken = credentials.token).use { httpClient ->
            runBlocking {
                println("uploading...")
                val imageBytes = withContext(Dispatchers.IO) { imageFile.readBytes() }
                val imageResponse = httpClient.submitFormWithBinaryData(
                    url = "https://${platform.host}/api/v2/media",
                    formData = formData {
                        append("description", altText)
                        append("file", imageBytes, Headers.build {
                            append(HttpHeaders.ContentType, imageType)
                            append(HttpHeaders.ContentDisposition, "filename=\"${sanitizedName(imageFile)}\"")
                        })
                    }
                )
                verifyResponse(imageResponse)
                val imageId = ((imageResponse.body() as JsonObject)["id"] as JsonPrimitive).content
                println("posting...")
                val postResponse = httpClient.post("https://${platform.host}/api/v1/statuses") {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("status", text)
                        putJsonArray("media_ids") {
                            add(imageId)
                        }
                        put("visibility", "public")
                        put("language", "en")
                    })
                }
                verifyResponse(postResponse)
                val postJson: JsonElement = postResponse.body()
                val postId = ((postJson as JsonObject)["id"] as JsonPrimitive).content
                println("post ${postId} created with image ${imageId}")
            }
        }
    }

    fun list(args: Array<String>) {
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
        val host = config.platforms[channel.platform]?.host!!
        val credentials = channel.credentials as MastodonCredentials
        makeHttpClient(json = true, bearerToken = credentials.token).use { httpClient ->
            runBlocking {
                val lookupResponse = httpClient.get {
                    url {
                        protocol = URLProtocol.HTTPS
                        this.host = host
                        pathSegments = listOf("/api/v1/accounts/lookup")
                        parameters.append("acct", credentials.username)
                        }
                    }
                verifyResponse(lookupResponse)
                val lookupJson: JsonElement = lookupResponse.body()
                val userId = ((lookupJson as JsonObject)["id"] as JsonPrimitive).content
                val listResponse = httpClient.get("https://${host}/api/v1/accounts/${userId}/statuses")
                verifyResponse(listResponse)
                val listJson: JsonElement = listResponse.body()
                (listJson as JsonArray).forEach {
                    val asObject = it as JsonObject
                    val postId = (asObject["id"] as JsonPrimitive).content
                    val postTime = (asObject["created_at"] as JsonPrimitive).content
                    System.out.format("%20s %s%n", postId, postTime)
                }
            }
        }
    }

    fun get(args: Array<String>) {
        if (args.size != 2) {
            printError("expecting channel ID and post (status) ID")
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
        val credentials = channel.credentials as MastodonCredentials
        makeHttpClient(json = true, bearerToken = credentials.token).use { httpClient ->
            runBlocking {
                val getResponse = httpClient.get("https://${host}/api/v1/statuses/${postId}")
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

private suspend fun verifyResponse(dubious: HttpResponse) {
    if (!dubious.status.isSuccess()) {
        printError("unexpected ${dubious.status.value} response")
        printError("body is: ${dubious.bodyAsText()}")
        exitProcess(1)
    }
}
