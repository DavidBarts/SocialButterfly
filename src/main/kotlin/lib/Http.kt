package name.blackcap.socialbutterfly.lib

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

fun makeHttpClient(json: Boolean = false, bearerToken: String? = null,
                   refreshCallback: (RefreshTokensParams.() -> BearerTokens)? = null) =
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

/* ktor purportedly has a way to refresh tokens, but it is crippled and not
   up to the job, so we use our own */
suspend fun doAuthRequest(shouldRefreshBefore: suspend () -> Boolean = { false },
                  refreshBefore: suspend () -> Unit = { throw NotImplementedError("refreshBefore not implemented") },
                  makeRequest: suspend () -> HttpResponse,
                  shouldRefreshAfter: suspend (HttpResponse) -> Boolean = { false },
                  refreshAfter: suspend () -> Unit = { throw NotImplementedError("refreshAfter not implemented") }): HttpResponse {
    if (shouldRefreshBefore()) {
        refreshBefore()
    }
    val try1 = makeRequest()
    if (try1.status.isSuccess()) {
        return try1
    }
    if (shouldRefreshAfter(try1)) {
        refreshAfter()
        return makeRequest()
    } else {
        return try1
    }
}

class TokenRefreshException(message: String): Exception(message)
