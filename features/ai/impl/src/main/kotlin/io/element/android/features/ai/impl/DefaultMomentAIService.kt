/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.ai.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.element.android.appconfig.AuthenticationConfig
import io.element.android.features.ai.api.MomentAIBriefingBotInfo
import io.element.android.features.ai.api.MomentAIBriefingPayload
import io.element.android.features.ai.api.MomentAIBriefingPostRequest
import io.element.android.features.ai.api.MomentAIBriefingPostResponse
import io.element.android.features.ai.api.MomentAIDailyDigest
import io.element.android.features.ai.api.MomentAIDailyDigestRoom
import io.element.android.features.ai.api.MomentAIDigestCandidates
import io.element.android.features.ai.api.MomentAIDigestPrimaryCandidate
import io.element.android.features.ai.api.MomentAIDigestSecondaryCandidate
import io.element.android.features.ai.api.MomentAIDigestSkipped
import io.element.android.features.ai.api.MomentAIDigestWindow
import io.element.android.features.ai.api.MomentAIFactCheckClaim
import io.element.android.features.ai.api.MomentAIFactCheckResult
import io.element.android.features.ai.api.MomentAIFactCheckSource
import io.element.android.features.ai.api.MomentAIRoomBriefing
import io.element.android.features.ai.api.MomentAIService
import io.element.android.libraries.androidutils.json.JsonProvider
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.di.SessionScope
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

@ContributesBinding(SessionScope::class)
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

    override suspend fun factCheck(
        statement: String,
        roomId: String?,
        eventId: String?,
    ): Result<MomentAIFactCheckResult> = withContext(coroutineDispatchers.io) {
        runCatchingExceptions {
            postJson<MomentAIFactCheckRequest, MomentAIFactCheckResponse>(
                path = "/api/ai/text/factcheck",
                body = MomentAIFactCheckRequest(
                    statement = statement,
                    sourceRoomId = roomId,
                    eventId = eventId,
                ),
            ).toResult()
        }
    }

    override suspend fun summarize(text: String): Result<String> = withContext(coroutineDispatchers.io) {
        runCatchingExceptions {
            val response = postJson<MomentAISummarizeRequest, MomentAISummarizeResponse>(
                path = "/api/ai/text/summarize",
                body = MomentAISummarizeRequest(text = text),
            )
            response.result.trim().ifEmpty { throw IllegalStateException("Empty AI response") }
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

    override suspend fun getDigestCandidates(
        fromIso: String,
        excludedRoomIds: List<String>,
    ): Result<MomentAIDigestCandidates> = withContext(coroutineDispatchers.io) {
        runCatchingExceptions {
            postJson<MomentAIDigestCandidatesRequest, MomentAIDigestCandidatesResponse>(
                path = "/api/ai/assistant/digest/candidates",
                body = MomentAIDigestCandidatesRequest(
                    fromIso = fromIso,
                    excludedRoomIds = excludedRoomIds.ifEmpty { null },
                ),
            ).toResult()
        }
    }

    override suspend fun getDigest(fromIso: String, roomIds: List<String>): Result<MomentAIDailyDigest> = withContext(coroutineDispatchers.io) {
        runCatchingExceptions {
            postJson<MomentAIDigestRequest, MomentAIDailyDigestResponse>(
                path = "/api/ai/assistant/digest",
                body = MomentAIDigestRequest(
                    window = MomentAIDigestWindowRequest(fromIso = fromIso),
                    roomIds = roomIds,
                ),
            ).toResult()
        }
    }

    override suspend fun postBriefing(request: MomentAIBriefingPostRequest): Result<MomentAIBriefingPostResponse> = withContext(coroutineDispatchers.io) {
        runCatchingExceptions {
            postJson<MomentAIBriefingPostRequestBody, MomentAIBriefingPostResponseBody>(
                path = "/api/ai/assistant/briefing/post",
                body = request.toRequestBody(),
            ).toResult()
        }
    }

    override suspend fun getBriefingBotInfo(): Result<MomentAIBriefingBotInfo> = withContext(coroutineDispatchers.io) {
        runCatchingExceptions {
            getJson<MomentAIBriefingBotInfoResponse>(
                path = "/api/ai/assistant/briefing/bot-info",
            ).toResult()
        }
    }

    private suspend inline fun <reified ResponseBody : Any> getJson(
        path: String,
    ): ResponseBody {
        val request = requestBuilder(path)
            .get()
            .build()
        return executeJson(request)
    }

    private suspend inline fun <reified RequestBody : Any, reified ResponseBody : Any> postJson(
        path: String,
        body: RequestBody,
    ): ResponseBody {
        val request = requestBuilder(path)
            .post(jsonProvider().encodeToString(body).toRequestBody(JSON_MEDIA_TYPE))
            .addHeader("Content-Type", "application/json")
            .build()
        return executeJson(request)
    }

    private suspend fun requestBuilder(path: String): Request.Builder {
        val session = sessionStore.getSession(matrixClient.sessionId.value)
            ?: throw IllegalStateException("Missing Matrix session")
        val url = "${AuthenticationConfig.OAUTH_BFF_BASE_URL.trimEnd('/')}$path"
        val userAgent = userAgentProvider.provide()
        return Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer ${session.accessToken}")
            .addHeader("User-Agent", userAgent)
            .addHeader("X-Element-User-Agent", userAgent)
    }

    private inline fun <reified ResponseBody : Any> executeJson(request: Request): ResponseBody {
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

    private fun MomentAIFactCheckResponse.toResult(): MomentAIFactCheckResult {
        return MomentAIFactCheckResult(
            verdict = verdict.trim().ifEmpty { "unverifiable" },
            confidence = confidence,
            rationale = rationale.trim().ifEmpty { throw IllegalStateException("Empty AI response") },
            claims = claims.map { claim ->
                MomentAIFactCheckClaim(
                    text = claim.text.trim(),
                    verdict = claim.verdict.trim().ifEmpty { "unverifiable" },
                    confidence = claim.confidence,
                )
            }.filter { it.text.isNotEmpty() },
            sources = sources.map { source ->
                MomentAIFactCheckSource(
                    type = source.type.trim(),
                    title = source.title.trim(),
                    url = source.url.trim(),
                    snippet = source.snippet.trim(),
                )
            }.filter { it.title.isNotEmpty() || it.snippet.isNotEmpty() },
            knowledgeCutoffWarning = knowledgeCutoffWarning,
            model = model.trim(),
        )
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
private data class MomentAIFactCheckRequest(
    val statement: String,
    val context: String? = null,
    @SerialName("source_room_id")
    val sourceRoomId: String? = null,
    @SerialName("event_id")
    val eventId: String? = null,
)

@Serializable
private data class MomentAIFactCheckResponse(
    val verdict: String = "unverifiable",
    val confidence: Double = 0.0,
    val rationale: String = "",
    val claims: List<MomentAIFactCheckClaimResponse> = emptyList(),
    val sources: List<MomentAIFactCheckSourceResponse> = emptyList(),
    @SerialName("knowledge_cutoff_warning")
    val knowledgeCutoffWarning: Boolean = false,
    val model: String = "",
)

@Serializable
private data class MomentAIFactCheckClaimResponse(
    val text: String = "",
    val verdict: String = "unverifiable",
    val confidence: Double = 0.0,
)

@Serializable
private data class MomentAIFactCheckSourceResponse(
    val type: String = "",
    val title: String = "",
    val url: String = "",
    val snippet: String = "",
)

@Serializable
private data class MomentAISummarizeRequest(
    val text: String,
)

@Serializable
private data class MomentAISummarizeResponse(
    val result: String,
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

@Serializable
private data class MomentAIDigestCandidatesRequest(
    @SerialName("from_iso")
    val fromIso: String,
    @SerialName("excluded_room_ids")
    val excludedRoomIds: List<String>? = null,
    @SerialName("top_n")
    val topN: Int = 5,
    @SerialName("top_n_secondary")
    val topNSecondary: Int = 3,
)

@Serializable
private data class MomentAIDigestCandidatesResponse(
    val primary: List<MomentAIDigestPrimaryCandidateResponse> = emptyList(),
    val secondary: List<MomentAIDigestSecondaryCandidateResponse> = emptyList(),
    val skipped: MomentAIDigestSkippedResponse = MomentAIDigestSkippedResponse(),
    @SerialName("total_new_messages")
    val totalNewMessages: Int = 0,
    @SerialName("total_active_chats")
    val totalActiveChats: Int = 0,
    @SerialName("has_highlights")
    val hasHighlights: Boolean = false,
) {
    fun toResult(): MomentAIDigestCandidates {
        return MomentAIDigestCandidates(
            primary = primary.map { it.toResult() },
            secondary = secondary.map { it.toResult() },
            skipped = skipped.toResult(),
            totalNewMessages = totalNewMessages,
            totalActiveChats = totalActiveChats,
            hasHighlights = hasHighlights,
        )
    }
}

@Serializable
private data class MomentAIDigestPrimaryCandidateResponse(
    @SerialName("room_id")
    val roomId: String,
    val title: String = "",
    val kind: String = "",
    @SerialName("unread_count")
    val unreadCount: Int = 0,
    @SerialName("you_mentioned")
    val youMentioned: Boolean = false,
) {
    fun toResult(): MomentAIDigestPrimaryCandidate {
        return MomentAIDigestPrimaryCandidate(
            roomId = roomId,
            title = title,
            kind = kind,
            unreadCount = unreadCount,
            youMentioned = youMentioned,
        )
    }
}

@Serializable
private data class MomentAIDigestSecondaryCandidateResponse(
    @SerialName("room_id")
    val roomId: String,
    val title: String = "",
    @SerialName("unread_count")
    val unreadCount: Int = 0,
) {
    fun toResult(): MomentAIDigestSecondaryCandidate {
        return MomentAIDigestSecondaryCandidate(
            roomId = roomId,
            title = title,
            unreadCount = unreadCount,
        )
    }
}

@Serializable
private data class MomentAIDigestSkippedResponse(
    val encrypted: Int = 0,
    @SerialName("no_activity")
    val noActivity: Int = 0,
    @SerialName("filtered_out")
    val filteredOut: Int = 0,
) {
    fun toResult(): MomentAIDigestSkipped {
        return MomentAIDigestSkipped(
            encrypted = encrypted,
            noActivity = noActivity,
            filteredOut = filteredOut,
        )
    }
}

@Serializable
private data class MomentAIDigestRequest(
    val window: MomentAIDigestWindowRequest,
    @SerialName("room_ids")
    val roomIds: List<String>,
    val model: String? = null,
)

@Serializable
private data class MomentAIDigestWindowRequest(
    @SerialName("from_iso")
    val fromIso: String,
)

@Serializable
private data class MomentAIDailyDigestResponse(
    @SerialName("generated_at")
    val generatedAt: String = "",
    val window: MomentAIDigestWindowResponse = MomentAIDigestWindowResponse(),
    @SerialName("meta_summary")
    val metaSummary: String = "",
    val rooms: List<MomentAIDailyDigestRoomResponse> = emptyList(),
    val skipped: MomentAIDigestSkippedResponse = MomentAIDigestSkippedResponse(),
    val model: String = "",
    val partial: Boolean = false,
) {
    fun toResult(): MomentAIDailyDigest {
        return MomentAIDailyDigest(
            generatedAt = generatedAt,
            window = window.toResult(),
            metaSummary = metaSummary,
            rooms = rooms.map { it.toResult() },
            skipped = skipped.toResult(),
            model = model,
            partial = partial,
        )
    }
}

@Serializable
private data class MomentAIDigestWindowResponse(
    val from: String = "",
    val to: String = "",
) {
    fun toResult(): MomentAIDigestWindow {
        return MomentAIDigestWindow(
            from = from,
            to = to,
        )
    }
}

@Serializable
private data class MomentAIDailyDigestRoomResponse(
    @SerialName("room_id")
    val roomId: String,
    val title: String = "",
    val kind: String = "",
    @SerialName("message_count")
    val messageCount: Int = 0,
    val summary: String = "",
    val highlights: List<String> = emptyList(),
    @SerialName("you_mentioned")
    val youMentioned: Boolean = false,
    val alert: String? = null,
) {
    fun toResult(): MomentAIDailyDigestRoom {
        return MomentAIDailyDigestRoom(
            roomId = roomId,
            title = title,
            kind = kind,
            messageCount = messageCount,
            summary = summary,
            highlights = highlights,
            youMentioned = youMentioned,
            alert = alert,
        )
    }
}

@Serializable
private data class MomentAIBriefingPostRequestBody(
    @SerialName("room_id")
    val roomId: String,
    val body: String,
    @SerialName("formatted_body")
    val formattedBody: String,
    val payload: MomentAIBriefingPayloadBody,
    @SerialName("local_date")
    val localDate: String,
    val force: Boolean,
)

@Serializable
private data class MomentAIBriefingPayloadBody(
    val version: Int,
    @SerialName("generated_at")
    val generatedAt: String,
    val window: MomentAIDigestWindowResponse,
    @SerialName("meta_summary")
    val metaSummary: String,
    val rooms: List<MomentAIDailyDigestRoomBody>,
    val skipped: MomentAIDigestSkippedResponse,
    val partial: Boolean,
    val model: String,
)

@Serializable
private data class MomentAIDailyDigestRoomBody(
    @SerialName("room_id")
    val roomId: String,
    val title: String,
    val kind: String,
    @SerialName("message_count")
    val messageCount: Int,
    val summary: String,
    val highlights: List<String>,
    @SerialName("you_mentioned")
    val youMentioned: Boolean,
    val alert: String?,
)

@Serializable
private data class MomentAIBriefingPostResponseBody(
    val posted: Boolean = false,
    @SerialName("event_id")
    val eventId: String? = null,
    @SerialName("bot_user_id")
    val botUserId: String? = null,
) {
    fun toResult(): MomentAIBriefingPostResponse {
        return MomentAIBriefingPostResponse(
            posted = posted,
            eventId = eventId,
            botUserId = botUserId,
        )
    }
}

@Serializable
private data class MomentAIBriefingBotInfoResponse(
    @SerialName("bot_user_id")
    val botUserId: String = "",
    @SerialName("bot_display_name")
    val botDisplayName: String = "",
) {
    fun toResult(): MomentAIBriefingBotInfo {
        return MomentAIBriefingBotInfo(
            botUserId = botUserId,
            botDisplayName = botDisplayName,
        )
    }
}

private fun MomentAIBriefingPostRequest.toRequestBody(): MomentAIBriefingPostRequestBody {
    return MomentAIBriefingPostRequestBody(
        roomId = roomId,
        body = body,
        formattedBody = formattedBody,
        payload = payload.toRequestBody(),
        localDate = localDate,
        force = force,
    )
}

private fun MomentAIBriefingPayload.toRequestBody(): MomentAIBriefingPayloadBody {
    return MomentAIBriefingPayloadBody(
        version = version,
        generatedAt = generatedAt,
        window = MomentAIDigestWindowResponse(
            from = window.from,
            to = window.to,
        ),
        metaSummary = metaSummary,
        rooms = rooms.map { it.toRequestBody() },
        skipped = MomentAIDigestSkippedResponse(
            encrypted = skipped.encrypted,
            noActivity = skipped.noActivity,
            filteredOut = skipped.filteredOut,
        ),
        partial = partial,
        model = model,
    )
}

private fun MomentAIDailyDigestRoom.toRequestBody(): MomentAIDailyDigestRoomBody {
    return MomentAIDailyDigestRoomBody(
        roomId = roomId,
        title = title,
        kind = kind,
        messageCount = messageCount,
        summary = summary,
        highlights = highlights,
        youMentioned = youMentioned,
        alert = alert,
    )
}
