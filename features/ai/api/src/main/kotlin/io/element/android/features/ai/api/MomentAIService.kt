/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.ai.api

interface MomentAIService {
    suspend fun transformText(text: String, mode: String): Result<String>
    suspend fun quickRewrite(text: String): Result<String>
    suspend fun factCheck(statement: String, roomId: String?, eventId: String?): Result<MomentAIFactCheckResult>
    suspend fun summarize(text: String): Result<String>
    suspend fun getRoomBriefing(roomId: String): Result<MomentAIRoomBriefing>
    suspend fun getDigestCandidates(fromIso: String, excludedRoomIds: List<String> = emptyList()): Result<MomentAIDigestCandidates>
    suspend fun getDigest(fromIso: String, roomIds: List<String>): Result<MomentAIDailyDigest>
    suspend fun postBriefing(request: MomentAIBriefingPostRequest): Result<MomentAIBriefingPostResponse>
    suspend fun getBriefingBotInfo(): Result<MomentAIBriefingBotInfo>
}

data class MomentAIFactCheckResult(
    val verdict: String,
    val confidence: Double,
    val rationale: String,
    val claims: List<MomentAIFactCheckClaim>,
    val sources: List<MomentAIFactCheckSource>,
    val knowledgeCutoffWarning: Boolean,
    val model: String,
)

data class MomentAIFactCheckClaim(
    val text: String,
    val verdict: String,
    val confidence: Double,
)

data class MomentAIFactCheckSource(
    val type: String,
    val title: String,
    val url: String,
    val snippet: String,
)

data class MomentAIRoomBriefing(
    val summary: String,
    val decisions: List<String>,
    val actionItems: List<String>,
    val alert: String?,
)

data class MomentAIDigestCandidates(
    val primary: List<MomentAIDigestPrimaryCandidate>,
    val secondary: List<MomentAIDigestSecondaryCandidate>,
    val skipped: MomentAIDigestSkipped,
    val totalNewMessages: Int,
    val totalActiveChats: Int,
    val hasHighlights: Boolean,
)

data class MomentAIDigestPrimaryCandidate(
    val roomId: String,
    val title: String,
    val kind: String,
    val unreadCount: Int,
    val youMentioned: Boolean,
)

data class MomentAIDigestSecondaryCandidate(
    val roomId: String,
    val title: String,
    val unreadCount: Int,
)

data class MomentAIDigestSkipped(
    val encrypted: Int,
    val noActivity: Int,
    val filteredOut: Int,
)

data class MomentAIDailyDigest(
    val generatedAt: String,
    val window: MomentAIDigestWindow,
    val metaSummary: String,
    val rooms: List<MomentAIDailyDigestRoom>,
    val skipped: MomentAIDigestSkipped,
    val model: String,
    val partial: Boolean,
)

data class MomentAIDigestWindow(
    val from: String,
    val to: String,
)

data class MomentAIDailyDigestRoom(
    val roomId: String,
    val title: String,
    val kind: String,
    val messageCount: Int,
    val summary: String,
    val highlights: List<String>,
    val youMentioned: Boolean,
    val alert: String?,
)

data class MomentAIBriefingPayload(
    val version: Int,
    val generatedAt: String,
    val window: MomentAIDigestWindow,
    val metaSummary: String,
    val rooms: List<MomentAIDailyDigestRoom>,
    val skipped: MomentAIDigestSkipped,
    val partial: Boolean,
    val model: String,
)

data class MomentAIBriefingPostRequest(
    val roomId: String,
    val body: String,
    val formattedBody: String,
    val payload: MomentAIBriefingPayload,
    val localDate: String,
    val force: Boolean,
)

data class MomentAIBriefingPostResponse(
    val posted: Boolean,
    val eventId: String?,
    val botUserId: String?,
)

data class MomentAIBriefingBotInfo(
    val botUserId: String,
    val botDisplayName: String,
)
