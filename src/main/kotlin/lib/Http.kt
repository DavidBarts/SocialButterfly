package name.blackcap.socialbutterfly.lib

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

fun makeHttpClient(json: Boolean = false, bearerToken: String? = null,
                   refreshCallback: ((RefreshTokensParams) -> BearerTokens)? = null) =
    HttpClient(Java) {
        if (json) {
            install(ContentNegotiation) {
                this.json()
            }
        }
        if (bearerToken != null) {
            install(Auth) {
                bearer {
                    loadTokens {
                        BearerTokens(bearerToken,  "")
                    }
                    sendWithoutRequest { true }
                    if (refreshCallback != null) {
                        refreshTokens(refreshCallback)
                    }
                }
            }
        }
        install(DefaultRequest) {
            header(HttpHeaders.UserAgent, "SocialButterfly")
        }
        install(Logging) {
            logger = Logger.EMPTY
            level = LogLevel.NONE
        }
        followRedirects = true
    }
