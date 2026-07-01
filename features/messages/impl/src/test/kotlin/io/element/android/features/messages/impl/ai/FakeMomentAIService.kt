/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.ai

import io.element.android.features.ai.api.MomentAIBriefingBotInfo
import io.element.android.features.ai.api.MomentAIBriefingPostRequest
import io.element.android.features.ai.api.MomentAIBriefingPostResponse
import io.element.android.features.ai.api.MomentAIDailyDigest
import io.element.android.features.ai.api.MomentAIDailyDigestRoom
import io.element.android.features.ai.api.MomentAIDigestCandidates
import io.element.android.features.ai.api.MomentAIDigestPrimaryCandidate
import io.element.android.features.ai.api.MomentAIDigestSecondaryCandidate
import io.element.android.features.ai.api.MomentAIDigestSkipped
import io.element.android.features.ai.api.MomentAIDigestWindow

class FakeMomentAIService(
    private val transformTextResult: (text: String, mode: String) -> Result<String> = { text, _ -> Result.success(text) },
    private val quickRewriteResult: (text: String) -> Result<String> = { text -> Result.success(text) },
    private val factCheckResult: (statement: String, roomId: String?, eventId: String?) -> Result<MomentAIFactCheckResult> = { statement, _, _ ->
        Result.success(
            MomentAIFactCheckResult(
                verdict = "unverifiable",
                confidence = 0.0,
                rationale = statement,
                claims = emptyList(),
                sources = emptyList(),
                knowledgeCutoffWarning = false,
                model = "fake",
            )
        )
    },
    private val summarizeResult: (text: String) -> Result<String> = { text -> Result.success(text) },
    private val roomBriefingResult: (roomId: String) -> Result<MomentAIRoomBriefing> = {
        Result.success(
            MomentAIRoomBriefing(
                summary = "Room summary",
                decisions = emptyList(),
                actionItems = emptyList(),
                alert = null,
            )
        )
    },
    private val digestCandidatesResult: (fromIso: String, excludedRoomIds: List<String>) -> Result<MomentAIDigestCandidates> = { _, _ ->
        Result.success(
            MomentAIDigestCandidates(
                primary = listOf(
                    MomentAIDigestPrimaryCandidate(
                        roomId = "!room:server",
                        title = "Room",
                        kind = "group",
                        unreadCount = 1,
                        youMentioned = false,
                    )
                ),
                secondary = listOf(
                    MomentAIDigestSecondaryCandidate(
                        roomId = "!secondary:server",
                        title = "Secondary",
                        unreadCount = 1,
                    )
                ),
                skipped = MomentAIDigestSkipped(
                    encrypted = 0,
                    noActivity = 0,
                    filteredOut = 0,
                ),
                totalNewMessages = 2,
                totalActiveChats = 2,
                hasHighlights = true,
            )
        )
    },
    private val digestResult: (fromIso: String, roomIds: List<String>) -> Result<MomentAIDailyDigest> = { fromIso, roomIds ->
        Result.success(
            MomentAIDailyDigest(
                generatedAt = "2026-07-01T10:00:00Z",
                window = MomentAIDigestWindow(
                    from = fromIso,
                    to = "2026-07-01T10:00:00Z",
                ),
                metaSummary = "Daily summary",
                rooms = roomIds.map { roomId ->
                    MomentAIDailyDigestRoom(
                        roomId = roomId,
                        title = "Room",
                        kind = "group",
                        messageCount = 1,
                        summary = "Summary",
                        highlights = emptyList(),
                        youMentioned = false,
                        alert = null,
                    )
                },
                skipped = MomentAIDigestSkipped(
                    encrypted = 0,
                    noActivity = 0,
                    filteredOut = 0,
                ),
                model = "fake",
                partial = false,
            )
        )
    },
    private val postBriefingResult: (MomentAIBriefingPostRequest) -> Result<MomentAIBriefingPostResponse> = {
        Result.success(
            MomentAIBriefingPostResponse(
                posted = true,
                eventId = "\$event",
                botUserId = "@bot:server",
            )
        )
    },
    private val briefingBotInfoResult: () -> Result<MomentAIBriefingBotInfo> = {
        Result.success(
            MomentAIBriefingBotInfo(
                botUserId = "@bot:server",
                botDisplayName = "Moment AI",
            )
        )
    },
) : MomentAIService {
    override suspend fun transformText(text: String, mode: String): Result<String> {
        return transformTextResult(text, mode)
    }

    override suspend fun quickRewrite(text: String): Result<String> {
        return quickRewriteResult(text)
    }

    override suspend fun factCheck(statement: String, roomId: String?, eventId: String?): Result<MomentAIFactCheckResult> {
        return factCheckResult(statement, roomId, eventId)
    }

    override suspend fun summarize(text: String): Result<String> {
        return summarizeResult(text)
    }

    override suspend fun getRoomBriefing(roomId: String): Result<MomentAIRoomBriefing> {
        return roomBriefingResult(roomId)
    }

    override suspend fun getDigestCandidates(fromIso: String, excludedRoomIds: List<String>): Result<MomentAIDigestCandidates> {
        return digestCandidatesResult(fromIso, excludedRoomIds)
    }

    override suspend fun getDigest(fromIso: String, roomIds: List<String>): Result<MomentAIDailyDigest> {
        return digestResult(fromIso, roomIds)
    }

    override suspend fun postBriefing(request: MomentAIBriefingPostRequest): Result<MomentAIBriefingPostResponse> {
        return postBriefingResult(request)
    }

    override suspend fun getBriefingBotInfo(): Result<MomentAIBriefingBotInfo> {
        return briefingBotInfoResult()
    }
}
