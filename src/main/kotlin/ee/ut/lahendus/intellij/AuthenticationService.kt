package ee.ut.lahendus.intellij

import com.google.gson.annotations.SerializedName
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import ee.ut.lahendus.intellij.ui.UiController
import ee.ut.lahendus.intellij.ui.language.LanguageProvider
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.Base64

private val LOG = logger<AuthenticationService>()

@Suppress("ServiceHttpUrlsUsage")
@Service
class AuthenticationService : Disposable {
    private var httpServer: HttpServer? = null
    var port: Int? = null

    private var accessToken: AuthenticationToken? = null
    private var refreshToken: AuthenticationToken? = null

    init {
        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(LahendusApplicationActionNotifier.LAHENDUS_APPLICATION_ACTION_TOPIC,
                object : LahendusApplicationActionNotifier {
                    override fun authenticationSuccessful() {
                        UiController.userAuthenticatedTrue()
                    }
                })
    }

    private fun startServer() {

        if (httpServer != null && port != null) {
            LOG.info("Authentication service already open on port: $port")
            getLoginAddress()?.let { address -> BrowserUtil.open(address) }
            return
        }

        httpServer = HttpServer.create(InetSocketAddress(0), 0)

        httpServer?.createContext("/login", LoginHandler())
        httpServer?.createContext("/keycloak.json", KeyCloakHandler())
        httpServer?.createContext("/deliver-tokens", TokensHandler())

        httpServer?.start()
        port = httpServer?.address?.port

        LOG.info("Authentication server opened on port: $port")

        getLoginAddress()?.let { address -> BrowserUtil.open(address) }
    }

    fun startServerBG() {
        ResourceUtils.invokeOnBackgroundThread { startServer() }
    }

    fun closeServer(delay: Int = 0) {
        httpServer?.stop(delay)
        port = null
        httpServer = null
        LOG.info("Authentication server stopped")
    }

    @Suppress("HttpUrlsUsage")
    private fun getLoginAddress(): String? {
        if (port == null) return null
        return "http://${LOCALHOST}:${port}/login"
    }

    private fun hasActiveAccessToken(): Boolean = accessToken != null && RequestUtils.validToken(accessToken!!)

    fun getValidAccessToken(): AuthenticationToken {
        if (hasActiveAccessToken())
            return accessToken!!

        refreshAccessToken()?.let {
            if (RequestUtils.validToken(it))
                return it
            LOG.error("Access token not valid after refresh")
        }

        throw AuthenticationRequiredException()
    }

    private fun refreshAccessToken(): AuthenticationToken? {
        LOG.info("Refreshing access token")

        refreshToken?.let {
            if (!RequestUtils.validToken(it)) {
                LOG.info("Refresh token expired")
                return null
            }

            val requestBody = mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to it.value,
                "client_id" to IDP_CLIENT_NAME,
            )
            val requestBodyFormEncoded = RequestUtils.toUrlEncodedString(requestBody)

            val response = try {
                RequestUtils.sendPostRequest(
                    "${IDP_URL}${REFRESH_TOKEN_API_PATH}",
                    requestBodyFormEncoded,
                    contentType = "application/x-www-form-urlencoded"
                )
            } catch (e: ConnectException){
                LOG.warn("Failed to fetch refresh accessToken, connection error occurred")
                throw e
            } catch (e: Exception) {
                LOG.warn("Exception occurred when trying to refresh access token", e)
                return null
            }

            if (response.statusCode() == 200) {
                setAuthenticationTokensFromJsonInputStream(response.body())
                LOG.info("Authentication tokens refreshed")
                return accessToken
            } else {
                LOG.warn("Received status code ${response.statusCode()} when trying to refresh access token")
                return null
            }
        }
        LOG.info("No refresh token found")
        return null
    }

    fun setAuthenticationTokensFromJsonInputStream(inputStream: InputStream) {
        val tokensBody = RequestUtils.fromJson<TokensBody>(inputStream)

        val time = RequestUtils.currentTime()
        accessToken =
            AuthenticationToken(tokensBody.accessToken, TokenType.ACCESS, time + tokensBody.accessTokenValidSec)
        refreshToken =
            AuthenticationToken(tokensBody.refreshToken, TokenType.REFRESH, time + tokensBody.refreshTokenValidSec)
    }

    fun checkIn() {
        try {
            val decodedToken = decodeToken(getValidAccessToken().value)
            val requestBody = RequestUtils.asJson(
                mapOf(
                    "first_name" to decodedToken.givenName,
                    "last_name" to decodedToken.familyName
                )
            )

            RequestUtils.sendPostRequest(
                "${LahendusApiService.BASE_URL}${CHECK_IN_API_PATH}",
                requestBody,
                RequestUtils.getAuthHeader()
            )
        } catch (e: Exception) {
            LOG.warn("Failed to checkin", e)
        }
    }

    fun logout() {
        accessToken = null
        refreshToken = null
        BrowserUtil.open("${IDP_URL}${LOGOUT_API_PATH}")
        ApplicationManager.getApplication().messageBus
            .syncPublisher(LahendusApplicationActionNotifier.LAHENDUS_APPLICATION_ACTION_TOPIC)
            .loggedOut()
    }

    data class DecodedToken(
        @SerializedName("given_name") val givenName: String,
        @SerializedName("family_name") val familyName: String,
    )

    private fun decodeToken(token: String): DecodedToken {
        val base64String = token.split(".")[1]
        val decodedString = String(Base64.getDecoder().decode(base64String))
        return RequestUtils.fromJson<DecodedToken>(decodedString.byteInputStream(StandardCharsets.UTF_8))
    }

    override fun dispose() {
        closeServer(0)
    }

    internal class LoginHandler : HttpHandler {

        override fun handle(req: HttpExchange) {
            LOG.info("Received login request")
            if (req.requestMethod.uppercase() != "GET") {
                return
            }
            val loginInjectedProperties = mapOf(
                "idp_url" to AuthenticationProperties.IDP_URL,
                "success_msg" to LanguageProvider.languageModel!!.authService.authSuccessMsg,
                "fail_msg" to LanguageProvider.languageModel!!.authService.authFailMsg
            )
            val injectedProperties = loginInjectedProperties.toMutableMap()
            injectedProperties["port"] = service<AuthenticationService>().port.toString()

            val resp = ResourceUtils.getResourceWithInjection(
                "/auth-templates/login.html",
                injectedProperties
            ) ?: throw IOException("Error encountered fetching the requested file")
            RequestUtils.sendSuccessResponse(resp, req)
        }
    }

    internal class KeyCloakHandler : HttpHandler {
        private val kcInjectedProperties = mapOf(
            "idp_url" to AuthenticationProperties.IDP_URL,
            "client_name" to AuthenticationProperties.IDP_CLIENT_NAME,
        )

        override fun handle(req: HttpExchange) {
            LOG.info("Received keycloak request")
            if (req.requestMethod.uppercase() != "GET") {
                return
            }

            val resp = ResourceUtils.getResourceWithInjection(
                "/auth-templates/keycloak.json",
                kcInjectedProperties
            ) ?: throw IOException("Error encountered fetching the requested file")
            RequestUtils.sendSuccessResponse(resp, req)
        }
    }

    internal class TokensHandler : HttpHandler {

        override fun handle(req: HttpExchange) {
            LOG.info("Received tokens request")
            if (req.requestMethod.uppercase() != "POST") {
                return
            }

            service<AuthenticationService>().setAuthenticationTokensFromJsonInputStream(req.requestBody)
            LOG.info("Saved authentication tokens")
            RequestUtils.sendSuccessResponse(null, req)

            service<AuthenticationService>().closeServer()
            val publisher = ApplicationManager.getApplication().messageBus
                .syncPublisher(LahendusApplicationActionNotifier.LAHENDUS_APPLICATION_ACTION_TOPIC)
            publisher.authenticationSuccessful()
            service<AuthenticationService>().checkIn()
        }
    }


    data class TokensBody(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("refresh_token") val refreshToken: String,
        @SerializedName(value = "access_token_valid_sec", alternate = ["expires_in"])
        val accessTokenValidSec: Int,
        @SerializedName(value = "refresh_token_valid_sec", alternate = ["refresh_expires_in"])
        val refreshTokenValidSec: Int,
    )


    companion object AuthenticationProperties {
        val IDP_URL = RequestUtils.normaliseAddress("idp.lahendus.ut.ee")
        const val IDP_CLIENT_NAME = "lahendus.ut.ee"
        const val LOCALHOST = "127.0.0.1"
        const val AUTH_TOKEN_MIN_VALID_SEC = 20
        const val REFRESH_TOKEN_API_PATH = "/auth/realms/master/protocol/openid-connect/token"
        const val LOGOUT_API_PATH = "/auth/realms/master/protocol/openid-connect/logout" +
                "?redirect_uri=https%3A%2F%2F${IDP_CLIENT_NAME}"
        const val CHECK_IN_API_PATH = "/account/checkin"
    }

    enum class TokenType(val value: Int) {
        ACCESS(0),
        REFRESH(1)
    }

    data class AuthenticationToken(val value: String, val tokenType: TokenType, val expires: Long)

    class AuthenticationRequiredException : RuntimeException("Authentication is required")
}

