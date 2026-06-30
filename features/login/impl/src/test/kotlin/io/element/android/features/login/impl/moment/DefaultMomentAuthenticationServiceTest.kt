/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.login.impl.moment

import android.content.Context
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.androidutils.json.DefaultJsonProvider
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
class DefaultMomentAuthenticationServiceTest {
    @Test
    fun `getOAuthDetails builds BFF authorize URL with PKCE parameters`() = runTest {
        val server = MockWebServer()
        val sut = createService(server)

        val result = sut.getOAuthDetails().getOrThrow()
        val uri = Uri.parse(result.url)

        assertThat(uri.path).isEqualTo("/oauth-bff/api/v1/authorize")
        assertThat(uri.getQueryParameter("client_id")).isEqualTo("wb_m")
        assertThat(uri.getQueryParameter("redirect_uri")).isEqualTo(REDIRECT_URI)
        assertThat(uri.getQueryParameter("scope")).isEqualTo("openid phone read:profile")
        assertThat(uri.getQueryParameter("audience")).isEqualTo("https://unmoment.app")
        assertThat(uri.getQueryParameter("state")).isNotEmpty()
        assertThat(uri.getQueryParameter("code_challenge_method")).isEqualTo("S256")
        assertThat(uri.getQueryParameter("response_type")).isEqualTo("code")
        assertThat(uri.getQueryParameter("code_challenge")).isNotEmpty()
        assertThat(uri.getQueryParameter("prompt")).isEqualTo("consent")
    }

    @Test
    fun `getExternalSession exchanges code callback through BFF`() = runTest {
        val server = MockWebServer()
        val sut = createService(server)
        val homeserverUrl = server.url("/").toString().trimEnd('/')
        val state = Uri.parse(sut.getOAuthDetails().getOrThrow().url).getQueryParameter("state").orEmpty()
        server.enqueue(
            MockResponse().setBody(
                """
                {
                    "accessToken": "oauth-access-token",
                    "validationKey": "validation-key"
                }
                """.trimIndent()
            )
        )
        server.enqueue(
            MockResponse().setBody(
                """
                {
                    "userId": "@alice:unmoment.app",
                    "accessToken": "matrix-access-token",
                    "deviceId": "DEVICE"
                }
                """.trimIndent()
            )
        )

        val result = sut.getExternalSession("$REDIRECT_URI?code=auth-code&state=$state").getOrThrow()

        assertThat(result.userId).isEqualTo("@alice:unmoment.app")
        assertThat(result.accessToken).isEqualTo("matrix-access-token")
        assertThat(result.deviceId).isEqualTo("DEVICE")
        assertThat(result.homeserverUrl).isEqualTo(homeserverUrl)

        val tokenRequest = server.takeRequest()
        assertThat(tokenRequest.path).isEqualTo("/oauth-bff/api/v1/token")
        JSONObject(tokenRequest.body.readUtf8()).let {
            assertThat(it.getString("clientId")).isEqualTo("wb_m")
            assertThat(it.getString("code")).isEqualTo("auth-code")
            assertThat(it.getString("grantType")).isEqualTo("authorization_code")
            assertThat(it.getString("codeVerifier")).isNotEmpty()
            assertThat(it.getString("redirectUri")).isEqualTo(REDIRECT_URI)
            assertThat(it.getString("state")).isEqualTo(state)
        }

        val matrixSessionRequest = server.takeRequest()
        assertThat(matrixSessionRequest.path).isEqualTo("/oauth-bff/api/v1/matrix-session")
        assertThat(matrixSessionRequest.getHeader("Authorization")).isEqualTo("Bearer oauth-access-token")
        assertThat(matrixSessionRequest.getHeader("X-Validation-Key")).isEqualTo("validation-key")
        JSONObject(matrixSessionRequest.body.readUtf8()).let {
            assertThat(it.getString("clientId")).isEqualTo("wb_m")
            assertThat(it.getString("accessToken")).isEqualTo("oauth-access-token")
            assertThat(it.getString("validationKey")).isEqualTo("validation-key")
        }
    }

    @Test
    fun `getExternalSession exchanges login token with Matrix login endpoint`() = runTest {
        val server = MockWebServer()
        val sut = createService(server)
        val homeserverUrl = server.url("/").toString().trimEnd('/')
        server.enqueue(
            MockResponse().setBody(
                """
                {
                    "user_id": "@alice:unmoment.app",
                    "access_token": "matrix-access-token",
                    "device_id": "DEVICE"
                }
                """.trimIndent()
            )
        )

        val result = sut.getExternalSession("$REDIRECT_URI?loginToken=login-token").getOrThrow()

        assertThat(result.userId).isEqualTo("@alice:unmoment.app")
        assertThat(result.accessToken).isEqualTo("matrix-access-token")
        assertThat(result.deviceId).isEqualTo("DEVICE")
        assertThat(result.homeserverUrl).isEqualTo(homeserverUrl)

        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/api/client/v3/login")
        JSONObject(request.body.readUtf8()).let {
            assertThat(it.getString("type")).isEqualTo("m.login.token")
            assertThat(it.getString("token")).isEqualTo("login-token")
            assertThat(it.getString("initial_device_display_name")).isEqualTo("Moment Android")
        }
    }

    @Test
    fun `getExternalSession does not use shared HTTP interceptors for token exchanges`() = runTest {
        val interceptorWasUsed = AtomicBoolean(false)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor {
                interceptorWasUsed.set(true)
                it.proceed(it.request())
            }
            .build()
        val server = MockWebServer()
        val sut = createService(server, okHttpClient)
        val state = Uri.parse(sut.getOAuthDetails().getOrThrow().url).getQueryParameter("state").orEmpty()
        server.enqueue(
            MockResponse().setBody(
                """
                {
                    "accessToken": "oauth-access-token"
                }
                """.trimIndent()
            )
        )
        server.enqueue(
            MockResponse().setBody(
                """
                {
                    "userId": "@alice:unmoment.app",
                    "accessToken": "matrix-access-token",
                    "deviceId": "DEVICE"
                }
                """.trimIndent()
            )
        )

        sut.getExternalSession("$REDIRECT_URI?code=auth-code&state=$state").getOrThrow()

        assertThat(interceptorWasUsed.get()).isFalse()
    }

    @Test
    fun `isMomentCallbackUrl matches redirect origin and path`() {
        val server = MockWebServer()
        val sut = createService(server)

        assertThat(sut.isMomentCallbackUrl("$REDIRECT_URI?code=code")).isTrue()
        assertThat(sut.isMomentCallbackUrl("https://unmoment.app/other?code=code")).isFalse()
        assertThat(sut.isMomentCallbackUrl("https://example.org/auth/callback?code=code")).isFalse()
    }

    private fun createService(
        server: MockWebServer,
        okHttpClient: OkHttpClient = OkHttpClient(),
    ): DefaultMomentAuthenticationService {
        val context = RuntimeEnvironment.getApplication() as Context
        val sharedPreferences = context.getSharedPreferences(
            "DefaultMomentAuthenticationServiceTest-${System.nanoTime()}",
            Context.MODE_PRIVATE,
        )
        val baseUrl = server.url("/").toString().trimEnd('/')
        return DefaultMomentAuthenticationService(
            sharedPreferences = sharedPreferences,
            okHttpClient = okHttpClient,
            jsonProvider = DefaultJsonProvider(),
            coroutineDispatchers = CoroutineDispatchers.Default,
            oauthBffBaseUrl = baseUrl,
            defaultHomeserverUrl = baseUrl,
            redirectUri = REDIRECT_URI,
        )
    }

    companion object {
        private const val REDIRECT_URI = "https://unmoment.app/auth/callback"
    }
}
