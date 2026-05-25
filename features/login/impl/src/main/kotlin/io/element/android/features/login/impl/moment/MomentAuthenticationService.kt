/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.moment

import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.element.android.appconfig.ApplicationConfig
import io.element.android.appconfig.AuthenticationConfig
import io.element.android.libraries.androidutils.json.JsonProvider
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.core.uri.ensureProtocol
import io.element.android.libraries.matrix.api.auth.OAuthDetails
import io.element.android.libraries.matrix.api.auth.external.ExternalSession
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.security.SecureRandom

interface MomentAuthenticationService {
    fun isMomentHomeserver(url: String): Boolean
    fun isMomentCallbackUrl(url: String): Boolean
    fun isMomentAuthorizationUrl(url: String): Boolean
    suspend fun getOAuthDetails(): Result<OAuthDetails>
    suspend fun getExternalSession(callbackUrl: String): Result<ExternalSession>
}

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class DefaultMomentAuthenticationService private constructor(
    private val sharedPreferences: SharedPreferences,
    okHttpClient: OkHttpClient,
    private val jsonProvider: JsonProvider,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val runtimeConfig: MomentAuthenticationRuntimeConfig,
) : MomentAuthenticationService {
    private val okHttpClient = okHttpClient.withoutInterceptors()
    @Inject
    constructor(
        sharedPreferences: SharedPreferences,
        okHttpClient: OkHttpClient,
        jsonProvider: JsonProvider,
        coroutineDispatchers: CoroutineDispatchers,
    ) : this(
        sharedPreferences = sharedPreferences,
        okHttpClient = okHttpClient,
        jsonProvider = jsonProvider,
        coroutineDispatchers = coroutineDispatchers,
        runtimeConfig = MomentAuthenticationRuntimeConfig(),
    )

    internal constructor(
        sharedPreferences: SharedPreferences,
        okHttpClient: OkHttpClient,
        jsonProvider: JsonProvider,
        coroutineDispatchers: CoroutineDispatchers,
        oauthBffBaseUrl: String,
        defaultHomeserverUrl: String,
        redirectUri: String,
    ) : this(
        sharedPreferences = sharedPreferences,
        okHttpClient = okHttpClient,
        jsonProvider = jsonProvider,
        coroutineDispatchers = coroutineDispatchers,
        runtimeConfig = MomentAuthenticationRuntimeConfig(
            oauthBffBaseUrl = oauthBffBaseUrl,
            defaultHomeserverUrl = defaultHomeserverUrl,
            redirectUri = redirectUri,
        ),
    )

    private val secureRandom = SecureRandom()

    override fun isMomentHomeserver(url: String): Boolean {
        val candidate = parseUri(url) ?: return false
        val momentHomeserver = parseUri(runtimeConfig.defaultHomeserverUrl) ?: return false
        return sameOrigin(candidate, momentHomeserver)
    }

    override fun isMomentCallbackUrl(url: String): Boolean {
        val candidate = parseUri(url) ?: return false
        val redirectUri = parseUri(runtimeConfig.redirectUri) ?: return false
        return sameOrigin(candidate, redirectUri) && candidate.path == redirectUri.path
    }

    override fun isMomentAuthorizationUrl(url: String): Boolean {
        val candidate = parseUri(url) ?: return false
        return candidate.path == AUTHORIZE_PATH
    }

    override suspend fun getOAuthDetails(): Result<OAuthDetails> = withContext(coroutineDispatchers.io) {
        runCatchingExceptions {
            val state = randomUrlSafeString(byteCount = 24)
            val codeVerifier = randomUrlSafeString(byteCount = 48)
            val codeChallenge = sha256UrlSafe(codeVerifier)
            storeCodeVerifier(state, codeVerifier)

            OAuthDetails(
                url = Uri.parse("${runtimeConfig.oauthBffBaseUrl}$AUTHORIZE_PATH")
                    .buildUpon()
                    .appendQueryParameter("client_id", AuthenticationConfig.WBID_CLIENT_ID)
                    .appendQueryParameter("redirect_uri", runtimeConfig.redirectUri)
                    .appendQueryParameter("scope", AuthenticationConfig.WBID_SCOPE)
                    .appendQueryParameter("audience", AuthenticationConfig.WBID_AUDIENCE)
                    .appendQueryParameter("state", state)
                    .appendQueryParameter("code_challenge_method", "S256")
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("code_challenge", codeChallenge)
                    .appendQueryParameter("prompt", "consent")
                    .build()
                    .toString()
            )
        }
    }

    override suspend fun getExternalSession(callbackUrl: String): Result<ExternalSession> = withContext(coroutineDispatchers.io) {
        runCatchingExceptions {
            val callbackUri = Uri.parse(callbackUrl)
            require(isMomentCallbackUrl(callbackUrl)) { "Unexpected Moment callback URL" }

            val providerError = callbackUri.getQueryParameter("error")?.trim().orEmpty()
            if (providerError.isNotEmpty()) {
                callbackUri.getQueryParameter("state")?.let(::removeCodeVerifier)
                val description = callbackUri.getQueryParameter("error_description")
                    ?: callbackUri.getQueryParameter("errorDescription")
                throw IllegalStateException(description?.trim().orEmpty().ifEmpty { providerError })
            }

            val loginToken = callbackUri.getQueryParameter("loginToken")?.trim().orEmpty()
            if (loginToken.isNotEmpty()) {
                return@runCatchingExceptions loginWithToken(loginToken)
            }

            val code = callbackUri.getQueryParameter("code")?.trim().orEmpty()
            val state = callbackUri.getQueryParameter("state")?.trim().orEmpty()
            if (code.isEmpty() || state.isEmpty()) {
                throw IllegalStateException("WB.ID callback is missing code or state")
            }

            val codeVerifier = readCodeVerifier(state)
                ?: throw IllegalStateException("WB.ID session expired before token exchange")

            try {
                val tokenPayload = postJson<OAuthTokenRequest, OAuthTokenResponse>(
                    url = "${runtimeConfig.oauthBffBaseUrl}/oauth-bff/api/v1/token",
                    body = OAuthTokenRequest(
                        clientId = AuthenticationConfig.WBID_CLIENT_ID,
                        code = code,
                        grantType = "authorization_code",
                        codeVerifier = codeVerifier,
                        redirectUri = runtimeConfig.redirectUri,
                        state = state,
                    ),
                )
                require(tokenPayload.accessToken.isNotBlank()) { "OAuth BFF did not return accessToken" }

                val matrixSessionPayload = postJson<MatrixSessionRequest, MatrixSessionResponse>(
                    url = "${runtimeConfig.oauthBffBaseUrl}/oauth-bff/api/v1/matrix-session",
                    body = MatrixSessionRequest(
                        clientId = AuthenticationConfig.WBID_CLIENT_ID,
                        accessToken = tokenPayload.accessToken,
                        validationKey = tokenPayload.validationKey?.takeIf { it.isNotBlank() },
                    ),
                    headers = buildMap {
                        put("Authorization", "Bearer ${tokenPayload.accessToken}")
                        tokenPayload.validationKey?.takeIf { it.isNotBlank() }?.let {
                            put("X-Validation-Key", it)
                        }
                    },
                )
                matrixSessionPayload.toExternalSession(runtimeConfig.defaultHomeserverUrl)
            } finally {
                removeCodeVerifier(state)
            }
        }
    }

    private fun loginWithToken(loginToken: String): ExternalSession {
        return postJson<MatrixLoginTokenRequest, MatrixLoginResponse>(
            url = "${runtimeConfig.defaultHomeserverUrl}/_matrix/client/v3/login",
            body = MatrixLoginTokenRequest(
                type = "m.login.token",
                token = loginToken,
                initialDeviceDisplayName = "${ApplicationConfig.PRODUCTION_APPLICATION_NAME} Android",
            ),
        ).toExternalSession(runtimeConfig.defaultHomeserverUrl)
    }

    private inline fun <reified RequestBody : Any, reified ResponseBody : Any> postJson(
        url: String,
        body: RequestBody,
        headers: Map<String, String> = emptyMap(),
    ): ResponseBody {
        val request = Request.Builder()
            .url(url)
            .post(jsonProvider().encodeToString(body).toRequestBody(JSON_MEDIA_TYPE))
            .apply {
                headers.forEach { (key, value) -> addHeader(key, value) }
            }
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body.string()
            if (!response.isSuccessful) {
                throw IllegalStateException(responseBody.ifBlank { "Request to $url failed with ${response.code}" })
            }
            require(responseBody.isNotBlank()) { "Request to $url returned an empty response" }
            return jsonProvider().decodeFromString(responseBody)
        }
    }

    private fun storeCodeVerifier(state: String, codeVerifier: String) {
        sharedPreferences.edit()
            .putString(codeVerifierKey(state), codeVerifier)
            .commit()
    }

    private fun readCodeVerifier(state: String): String? {
        return sharedPreferences.getString(codeVerifierKey(state), null)
    }

    private fun removeCodeVerifier(state: String) {
        sharedPreferences.edit()
            .remove(codeVerifierKey(state))
            .commit()
    }

    private fun randomUrlSafeString(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun sha256UrlSafe(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun parseUri(url: String): Uri? {
        val normalized = url.trim()
        if (normalized.isEmpty()) return null
        return runCatching { Uri.parse(normalized.ensureProtocol()) }.getOrNull()
    }

    private fun sameOrigin(left: Uri, right: Uri): Boolean {
        return left.scheme.equals(right.scheme, ignoreCase = true) &&
            left.host.equals(right.host, ignoreCase = true) &&
            normalizedPort(left) == normalizedPort(right)
    }

    private fun normalizedPort(uri: Uri): Int {
        return when {
            uri.port != -1 -> uri.port
            uri.scheme.equals("https", ignoreCase = true) -> 443
            uri.scheme.equals("http", ignoreCase = true) -> 80
            else -> -1
        }
    }

    private fun codeVerifierKey(state: String) = "$CODE_VERIFIER_PREFIX$state"

    private fun OkHttpClient.withoutInterceptors(): OkHttpClient {
        // Moment auth exchanges bearer tokens, so keep it out of the shared HTTP body/header logger.
        return newBuilder()
            .apply {
                interceptors().clear()
                networkInterceptors().clear()
            }
            .build()
    }

    companion object {
        private const val AUTHORIZE_PATH = "/oauth-bff/api/v1/authorize"
        private const val CODE_VERIFIER_PREFIX = "moment_pkce_code_verifier_"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

internal data class MomentAuthenticationRuntimeConfig(
    val oauthBffBaseUrl: String = AuthenticationConfig.OAUTH_BFF_BASE_URL.trimEnd('/'),
    val defaultHomeserverUrl: String = AuthenticationConfig.DEFAULT_HOMESERVER_URL.trimEnd('/'),
    val redirectUri: String = AuthenticationConfig.WBID_REDIRECT_URI,
)

@Serializable
private data class OAuthTokenRequest(
    val clientId: String,
    val code: String,
    val grantType: String,
    val codeVerifier: String,
    val redirectUri: String,
    val state: String,
)

@Serializable
private data class OAuthTokenResponse(
    val accessToken: String,
    val validationKey: String? = null,
)

@Serializable
private data class MatrixSessionRequest(
    val clientId: String,
    val accessToken: String,
    val validationKey: String? = null,
)

@Serializable
private data class MatrixSessionResponse(
    val userId: String,
    val accessToken: String,
    val deviceId: String,
    val refreshToken: String? = null,
    val homeserverUrl: String? = null,
) {
    fun toExternalSession(defaultHomeserverUrl: String): ExternalSession {
        require(userId.isNotBlank() && accessToken.isNotBlank() && deviceId.isNotBlank()) {
            "OAuth BFF did not return a complete Matrix session"
        }
        return ExternalSession(
            userId = userId,
            deviceId = deviceId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            homeserverUrl = homeserverUrl?.takeIf { it.isNotBlank() } ?: defaultHomeserverUrl,
        )
    }
}

@Serializable
private data class MatrixLoginTokenRequest(
    val type: String,
    val token: String,
    @SerialName("initial_device_display_name")
    val initialDeviceDisplayName: String,
)

@Serializable
private data class MatrixLoginResponse(
    @SerialName("user_id")
    val userId: String,
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
) {
    fun toExternalSession(defaultHomeserverUrl: String): ExternalSession {
        require(userId.isNotBlank() && accessToken.isNotBlank() && deviceId.isNotBlank()) {
            "Matrix login token exchange did not return a complete Matrix session"
        }
        return ExternalSession(
            userId = userId,
            deviceId = deviceId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            homeserverUrl = defaultHomeserverUrl,
        )
    }
}
