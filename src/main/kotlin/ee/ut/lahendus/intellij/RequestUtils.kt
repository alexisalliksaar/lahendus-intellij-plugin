package ee.ut.lahendus.intellij

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.sun.net.httpserver.HttpExchange
import java.io.InputStream
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

object RequestUtils {
    val LOG = logger<RequestUtils>()

    private val httpClient: HttpClient = HttpClient.newBuilder().build()
    private val objectMapper: ObjectMapper = ObjectMapper()
    val gson = Gson()

    fun sendSuccessResponse(response: String?, req: HttpExchange) {
        if (response == null) {
            req.sendResponseHeaders(204, -1)
        } else {
            val bytes = response.toByteArray()
            req.sendResponseHeaders(200, bytes.size.toLong())
            req.responseBody.use { out -> out.write(bytes) }
        }
        req.close()
    }

    fun sendPostRequest(
        addr: String,
        body: String,
        headers: Map<String, String>? = null,
        contentType: String = "application/json"
    ): HttpResponse<InputStream> {
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(addr))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type", contentType)

        headers?.let {
            headers.forEach { (key, value) -> requestBuilder.header(key, value) }
        }
        val request = requestBuilder.build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

        if (response.statusCode() == 200) {
            LOG.info("Successful post request against $addr")
        } else {
            LOG.warn("Post request against $addr failed with status code: ${response.statusCode()}")
        }
        return response
    }

    fun sendGetRequest(addr: String, headers: Map<String, String>? = null): HttpResponse<InputStream> {
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(addr))
        headers?.let {
            it.forEach { (key, value) -> requestBuilder.header(key, value) }
        }
        val request = requestBuilder.build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

        if (response.statusCode() == 200) {
            LOG.info("Successful get request against $addr")
        } else {
            LOG.warn("Get request against $addr failed with status code: ${response.statusCode()}")
        }
        return response
    }

    fun asJson(map: Map<String, Any>): String {
        return objectMapper.writeValueAsString(map)
    }

    @Suppress("RemoveRedundantQualifierName")
    inline fun <reified T : Any> fromJson(json: InputStream, failSilently: Boolean = false): T {
        val jsonString = json.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return RequestUtils.fromJson(jsonString, failSilently)
    }
    inline fun <reified T : Any> fromJson(json: String, failSilently: Boolean = false): T {
        try {
            return gson.fromJson(json, T::class.java)
        } catch (e: JsonSyntaxException) {
            if (! failSilently){
                LOG.warn("Couldn't parse string to ${T::class.simpleName}. Json:\n$json")
            }
            throw e
        }
    }

    fun toUrlEncodedString(map: Map<String, String>): String = map.entries
        .joinToString("&") { (key, value) ->
            "${urlEncode(key)}=${urlEncode(value)}"
        }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8)

    fun getAuthHeader(): Map<String, String> {
        val accessToken = service<AuthenticationService>().getValidAccessToken()
        return mapOf("Authorization" to "Bearer ${accessToken.value}")
    }

    fun normaliseAddress(addr: String): String =
        if (!addr.startsWith("http")) "https://" + addr.trimEnd('/')
        else addr.trimEnd('/')

    fun currentTime(): Long {
        return Instant.now().epochSecond
    }

    fun validToken(token: AuthenticationService.AuthenticationToken): Boolean {
        return currentTime() <= token.expires - AuthenticationService.AUTH_TOKEN_MIN_VALID_SEC
    }

    fun publishNetworkErrorMessage(project: Project? = null) {
        if (project == null) {
            ApplicationManager.getApplication().messageBus
                .syncPublisher(LahendusApplicationActionNotifier.LAHENDUS_APPLICATION_ACTION_TOPIC)
                .networkErrorMessage()
        } else {
            project.messageBus.syncPublisher(LahendusProjectActionNotifier.LAHENDUS_PROJECT_ACTION_TOPIC)
                .networkErrorMessage()
        }
    }

    fun publishRequestFailedMessage(message: String, project: Project? = null) {
        if (project == null) {
            ApplicationManager.getApplication().messageBus
                .syncPublisher(LahendusApplicationActionNotifier.LAHENDUS_APPLICATION_ACTION_TOPIC)
                .requestFailed(message)
        } else {
            project.messageBus.syncPublisher(LahendusProjectActionNotifier.LAHENDUS_PROJECT_ACTION_TOPIC)
                .requestFailed(message)
        }
    }

    fun publishAuthenticationRequired() {
        ApplicationManager.getApplication().messageBus.syncPublisher(
            LahendusApplicationActionNotifier.LAHENDUS_APPLICATION_ACTION_TOPIC
        ).reAuthenticationRequired()
    }
}