/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.ai

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.element.android.appconfig.AuthenticationConfig
import io.element.android.libraries.androidutils.json.JsonProvider
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.di.RoomScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.network.useragent.UserAgentProvider
import io.element.android.libraries.sessionstorage.api.SessionStore
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

interface MomentAIService {
    suspend fun transformText(text: String, mode: String): Result<String>
    suspend fun quickRewrite(text: String): Result<String>
    suspend fun getRoomBriefing(roomId: String): Result<MomentAIRoomBriefing>
}

data class MomentAIRoomBriefing(
    val summary: String,
    val decisions: List<String>,
    val actionItems: List<String>,
    val alert: String?,
)

@ContributesBinding(RoomScope::class)
class DefaultMomentAIService @Inject constructor(
    okHttpClient: OkHttpClient,
    private val jsonProvider: JsonProvider,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val sessionStore: SessionStore,
    private val matrixClient: MatrixClient,
    private val userAgentProvider: UserAgentProvider,
) : MomentAIService {
    private val okHttpClient = okHttpClient.withoutInterceptors()

    override suspend fun transformText(text: String, mode: String): Result<String> = withContext(coroutineDispatchers.io) {
        runCatchingExceptions {
            val response = postJson<MomentAITransformRequest, MomentAITransformResponse>(
                path = "/api/ai/text/transform",
                body = MomentAITransformRequest(text = text, mode = mode),
            )
            response.result.trim().ifEmpty { throw IllegalStateException("Empty AI response") }
        }
    }

    override suspend fun quickRewrite(text: String): Result<String> = withContext(coroutineDispatchers.io) {
        runCatchingExceptions {
            val response = postJson<MomentAIChatRunRequest, MomentAIChatRunResponse>(
                path = "/api/ai/chat/run",
                body = MomentAIChatRunRequest(
                    messages = listOf(
                        MomentAIChatMessage(
                            role = "system",
                            content = QUICK_REWRITE_SYSTEM_PROMPT,
                        ),
                        MomentAIChatMessage(
                            role = "user",
                            content = text,
                        ),
                    ),
                ),
            )
            response.reply.trim().ifEmpty { throw IllegalStateException("Empty AI response") }
        }
    }

    override suspend fun getRoomBriefing(roomId: String): Result<MomentAIRoomBriefing> = withContext(coroutineDispatchers.io) {
        runCatchingExceptions {
            val response = postJson<MomentAIBriefingRequest, MomentAIBriefingResponse>(
                path = "/api/ai/assistant/briefing",
                body = MomentAIBriefingRequest(
                    conversation = MomentAIBriefingConversation(
                        kind = "room",
                        sourceRoomId = roomId,
                    ),
                ),
            )
            response.toRoomBriefing()
        }
    }

    private suspend inline fun <reified RequestBody : Any, reified ResponseBody : Any> postJson(
        path: String,
        body: RequestBody,
    ): ResponseBody {
        val session = sessionStore.getSession(matrixClient.sessionId.value)
            ?: throw IllegalStateException("Missing Matrix session")
        val url = "${AuthenticationConfig.OAUTH_BFF_BASE_URL.trimEnd('/')}$path"
        val userAgent = userAgentProvider.provide()
        val request = Request.Builder()
            .url(url)
            .post(jsonProvider().encodeToString(body).toRequestBody(JSON_MEDIA_TYPE))
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer ${session.accessToken}")
            .addHeader("User-Agent", userAgent)
            .addHeader("X-Element-User-Agent", userAgent)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body.string()
            if (!response.isSuccessful) {
                throw IllegalStateException("AI request failed with HTTP ${response.code}")
            }
            require(responseBody.isNotBlank()) { "AI request returned an empty response" }
            return jsonProvider().decodeFromString(responseBody)
        }
    }

    private fun OkHttpClient.withoutInterceptors(): OkHttpClient {
        return newBuilder()
            .apply {
                interceptors().clear()
                networkInterceptors().clear()
            }
            .build()
    }

    private fun MomentAIBriefingResponse.toRoomBriefing(): MomentAIRoomBriefing {
        structured?.let { structured ->
            return MomentAIRoomBriefing(
                summary = structured.summary.trim().ifEmpty { throw IllegalStateException("Empty AI response") },
                decisions = structured.decisions.orEmpty().mapNotNull { it.trimmedOrNull() },
                actionItems = structured.actionItems.orEmpty().mapNotNull { it.trimmedOrNull() },
                alert = structured.alert.trimmedOrNull(),
            )
        }

        return MomentAIRoomBriefing(
            summary = briefing.orEmpty().trim().ifEmpty { throw IllegalStateException("Empty AI response") },
            decisions = emptyList(),
            actionItems = emptyList(),
            alert = null,
        )
    }

    private fun String?.trimmedOrNull(): String? {
        return this?.trim()?.takeIf { it.isNotEmpty() }
    }

    private companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val QUICK_REWRITE_SYSTEM_PROMPT = """
            You are an AI assistant inside a messenger. The user writes a draft or an instruction.

            If the input is an instruction, produce the final message text. Respect length and style modifiers.
            If the input is already a draft, rewrite it to be clearer, natural, and appropriate.

            Rules:
            - Plain text only.
            - Return only the resulting message, without quotes, explanations, or markdown.
            - Write like a human in a messenger.
        """
    }
}

@Serializable
private data class MomentAITransformRequest(
    val text: String,
    val mode: String,
)

@Serializable
private data class MomentAITransformResponse(
    val result: String,
)

@Serializable
private data class MomentAIChatRunRequest(
    val messages: List<MomentAIChatMessage>,
)

@Serializable
private data class MomentAIChatMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class MomentAIChatRunResponse(
    val reply: String,
)

@Serializable
private data class MomentAIBriefingRequest(
    val conversation: MomentAIBriefingConversation,
)

@Serializable
private data class MomentAIBriefingConversation(
    val kind: String,
    @SerialName("source_room_id")
    val sourceRoomId: String,
)

@Serializable
private data class MomentAIBriefingResponse(
    val briefing: String? = null,
    val structured: MomentAIBriefingStructured? = null,
)

@Serializable
private data class MomentAIBriefingStructured(
    val summary: String,
    val decisions: List<String>? = null,
    @SerialName("action_items")
    val actionItems: List<String>? = null,
    val alert: String? = null,
)
