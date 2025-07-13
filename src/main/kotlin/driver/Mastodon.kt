package name.blackcap.socialbutterfly.driver

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.serialization.json.*
import name.blackcap.socialbutterfly.Application
import name.blackcap.socialbutterfly.gui.HttpErrorDialog
import name.blackcap.socialbutterfly.gui.useWaitCursor
import name.blackcap.socialbutterfly.gui.verifyResponse
import name.blackcap.socialbutterfly.jschema.*
import name.blackcap.socialbutterfly.lib.ConfigState
import name.blackcap.socialbutterfly.lib.bodyAsJson
import name.blackcap.socialbutterfly.lib.makeHttpClient
import java.util.*
import javax.swing.JOptionPane

object Mastodon : Driver {
    private const val REDIRECT_URI = "https://blackcap.name/cgi-bin/display_code,state.cgi"
    private const val SCOPES = "read:statuses write:statuses write:media"

    override val NAME = "Mastodon"

    override fun createPlatform(): Unit {
        val host = JOptionPane.showInputDialog(
            Application.frame,
            "Enter the Mastodon host name below:",
            "Enter Host Name",
            JOptionPane.PLAIN_MESSAGE
        )
        val exists =
            ConfigState.config.platforms.values.firstOrNull { it is MastodonPlatform && it.host == host } != null
        if (exists) {
            JOptionPane.showMessageDialog(
                Application.frame,
                "Mastodon platform object already exists for\n$host.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
        Application.frame.useWaitCursor()

        /* I *think* this is sufficient, because I believe Ktor uses
           Dispatchers.IO under the hood. But the Ktor documentation is
           clear as mud on this, so it *may* be necessary to push I/O
           into a different dispatcher (e.g. Dispatchers.IO) manually,
           to keep it off the Swing EDT. */
        GlobalScope.launch(Dispatchers.Swing) {
            val response = makeHttpClient(json = true).use { httpClient ->
                val clientName = Application.CLIENTNAME + " " + ConfigState.config.installation
                httpClient.post("https://${host}//api/v1/apps") {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("client_name", clientName)
                        putJsonArray("redirect_uris") {
                            add(REDIRECT_URI)
                        }
                        put("scopes", SCOPES)
                    })
                }
            }
            if (verifyResponse(response)) {
                val jsonBody = response.bodyAsJson()
                if (jsonBody is JsonObject) {
                    Application.platformsConnector.add(
                        UUID.randomUUID().toString(),
                        MastodonPlatform(
                            host,
                            (jsonBody["client_id"] as JsonPrimitive).content,
                            (jsonBody["client_secret"] as JsonPrimitive).content
                        )
                    )
                    Application.platformsPanel.run {
                        revalidate()
                        repaint()
                    }
                } else {
                    HttpErrorDialog(
                        Application.frame,
                        "Unexpected Mastodon API call return value.",
                        response.bodyAsText()
                    )
                }
            }
        }
    }

    override fun editPlatform(platform: Platform): Unit {
        val mastodonPlatform = platform as MastodonPlatform
        TODO("Not yet implemented")
    }

    override fun createCredentials(platform: Platform): Unit {
        val mastodonPlatform = platform as MastodonPlatform
        TODO("Not yet implemented")
    }

    override fun editCredentials(platform: Platform, credentials: Credentials): Unit {
        val mastodonPlatform = platform as MastodonPlatform
        val mastodonCredentials = credentials as MastodonCredentials
        TODO("Not yet implemented")
    }

    override fun createPost(platform: Platform, credentials: Credentials, post: Post): Unit {
        val mastodonPlatform = platform as MastodonPlatform
        val mastodonCredentials = credentials as MastodonCredentials
        TODO("Not yet implemented")
    }
}