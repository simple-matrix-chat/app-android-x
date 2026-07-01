/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.ai.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.element.android.features.ai.api.MomentAIBriefingPayload
import io.element.android.features.ai.api.MomentAIBriefingPostRequest
import io.element.android.features.ai.api.MomentAIDailyBriefingManager
import io.element.android.features.ai.api.MomentAIDailyBriefingResult
import io.element.android.features.ai.api.MomentAIDailyDigest
import io.element.android.features.ai.api.MomentAIService
import io.element.android.libraries.androidutils.json.JsonProvider
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.createroom.CreateRoomParameters
import io.element.android.libraries.matrix.api.createroom.RoomPreset
import io.element.android.libraries.matrix.api.room.RoomInfo
import io.element.android.libraries.matrix.api.room.history.RoomHistoryVisibility
import io.element.android.libraries.matrix.api.roomdirectory.RoomVisibility
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@ContributesBinding(SessionScope::class)
class DefaultMomentAIDailyBriefingManager @Inject constructor(
    private val matrixClient: MatrixClient,
    private val momentAIService: MomentAIService,
    private val jsonProvider: JsonProvider,
    private val coroutineDispatchers: CoroutineDispatchers,
) : MomentAIDailyBriefingManager {
    override suspend fun generateAndPost(force: Boolean): Result<MomentAIDailyBriefingResult> = withContext(coroutineDispatchers.io) {
        runCatchingExceptions {
            val accountData = readAccountData()
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val roomId = getOrCreateBriefingRoom(accountData)

            if (!force && accountData?.lastPostedDate == today) {
                throw IllegalStateException("Daily briefing already posted")
            }

            val digest = generateDigest()
            if (digest.rooms.isEmpty()) {
                throw IllegalStateException("Daily briefing is empty")
            }

            val response = momentAIService.postBriefing(
                MomentAIBriefingPostRequest(
                    roomId = roomId.value,
                    body = digest.toPlainText(),
                    formattedBody = digest.toHtml(),
                    payload = digest.toPayload(),
                    localDate = today,
                    force = force,
                )
            ).getOrThrow()

            writeAccountData(
                MomentAIBriefingAccountData(
                    roomId = roomId.value,
                    lastPostedDate = today.takeIf { response.posted } ?: accountData?.lastPostedDate,
                    welcome = accountData?.welcome,
                )
            )

            MomentAIDailyBriefingResult(
                digest = digest,
                roomId = roomId.value,
                eventId = response.eventId,
                posted = response.posted,
            )
        }
    }

    override suspend fun isBriefingRoom(roomId: String): Result<Boolean> = withContext(coroutineDispatchers.io) {
        runCatchingExceptions {
            val accountData = readAccountData()
            if (accountData?.roomId == roomId) {
                return@runCatchingExceptions true
            }

            val room = matrixClient.getJoinedRoom(RoomId(roomId)) ?: return@runCatchingExceptions false
            val info = room.info()
            info.isBriefingRoom()
        }
    }

    private suspend fun generateDigest(): MomentAIDailyDigest {
        val fromIso = dailyWindowStartIso()
        val candidates = momentAIService.getDigestCandidates(fromIso).getOrThrow()
        val roomIds = candidates.primary.map { it.roomId } + candidates.secondary.take(3).map { it.roomId }
        if (roomIds.isEmpty()) {
            throw IllegalStateException("Daily briefing is empty")
        }
        return momentAIService.getDigest(fromIso = fromIso, roomIds = roomIds).getOrThrow()
    }

    private suspend fun getOrCreateBriefingRoom(accountData: MomentAIBriefingAccountData?): RoomId {
        accountData?.roomId?.let { storedRoomId ->
            val roomId = RoomId(storedRoomId)
            val room = matrixClient.getJoinedRoom(roomId)
            if (room != null) {
                return rememberBriefingRoom(room.info().id, accountData)
            }
        }

        findExistingBriefingRoom()?.let { roomId ->
            return rememberBriefingRoom(roomId, accountData)
        }

        val roomId = matrixClient.createRoom(
            CreateRoomParameters(
                name = BRIEFING_ROOM_NAME,
                topic = BRIEFING_ROOM_TOPIC,
                isEncrypted = false,
                isDirect = false,
                visibility = RoomVisibility.Private,
                preset = RoomPreset.PRIVATE_CHAT,
                historyVisibilityOverride = RoomHistoryVisibility.Invited,
            )
        ).getOrThrow()

        return rememberBriefingRoom(roomId, accountData)
    }

    private suspend fun findExistingBriefingRoom(): RoomId? {
        val joinedRoomIds = matrixClient.getJoinedRoomIds().getOrNull().orEmpty()
        val candidates = joinedRoomIds.mapNotNull { roomId ->
            val room = matrixClient.getJoinedRoom(roomId)
            val info = room?.info() ?: return@mapNotNull null
            if (!info.isBriefingRoom()) return@mapNotNull null
            BriefingRoomCandidate(
                roomId = roomId,
                isFavorite = info.isFavorite,
                hasRawName = info.rawName == BRIEFING_ROOM_NAME,
            )
        }
        return candidates
            .sortedWith(
                compareByDescending<BriefingRoomCandidate> { it.isFavorite }
                    .thenByDescending { it.hasRawName }
                    .thenBy { it.roomId.value }
            )
            .firstOrNull()
            ?.roomId
    }

    private suspend fun rememberBriefingRoom(roomId: RoomId, accountData: MomentAIBriefingAccountData?): RoomId {
        matrixClient.getJoinedRoom(roomId)
            ?.takeIf { !it.info().isFavorite }
            ?.setIsFavorite(true)
            ?.getOrNull()
        writeAccountData(
            MomentAIBriefingAccountData(
                roomId = roomId.value,
                lastPostedDate = accountData?.lastPostedDate,
                welcome = accountData?.welcome,
            )
        )
        return roomId
    }

    private suspend fun readAccountData(): MomentAIBriefingAccountData? {
        val content = matrixClient.getAccountData(BRIEFING_ACCOUNT_DATA_TYPE).getOrNull()
            ?: return null
        return runCatchingExceptions {
            jsonProvider().decodeFromString<MomentAIBriefingAccountData>(content)
        }.getOrNull()
    }

    private suspend fun writeAccountData(accountData: MomentAIBriefingAccountData) {
        matrixClient.setAccountData(
            eventType = BRIEFING_ACCOUNT_DATA_TYPE,
            content = jsonProvider().encodeToString(accountData),
        ).getOrThrow()
    }

    private fun dailyWindowStartIso(): String {
        return ZonedDateTime.now()
            .minusDays(1)
            .withHour(22)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}

private const val BRIEFING_ACCOUNT_DATA_TYPE = "org.moment.daily_briefing"
private const val BRIEFING_ROOM_NAME = "Брифинг дня"
private const val BRIEFING_ROOM_TOPIC = "Ежедневная сводка активности в ваших чатах"

@Serializable
private data class MomentAIBriefingAccountData(
    @SerialName("room_id")
    val roomId: String? = null,
    @SerialName("last_posted_date")
    val lastPostedDate: String? = null,
    val welcome: Boolean? = null,
)

private data class BriefingRoomCandidate(
    val roomId: RoomId,
    val isFavorite: Boolean,
    val hasRawName: Boolean,
)

private fun RoomInfo.isBriefingRoom(): Boolean {
    return name == BRIEFING_ROOM_NAME || rawName == BRIEFING_ROOM_NAME
}

private fun MomentAIDailyDigest.toPayload(): MomentAIBriefingPayload {
    return MomentAIBriefingPayload(
        version = 1,
        generatedAt = generatedAt,
        window = window,
        metaSummary = metaSummary,
        rooms = rooms,
        skipped = skipped,
        partial = partial,
        model = model,
    )
}

private fun MomentAIDailyDigest.toPlainText(): String = buildString {
    appendLine("Брифинг дня")
    appendLine()
    if (metaSummary.isNotBlank()) {
        appendLine(metaSummary)
        appendLine()
    }
    rooms.forEachIndexed { index, room ->
        appendLine("${index + 1}. ${room.title}")
        appendLine(room.summary)
        room.highlights.take(3).forEach { highlight ->
            appendLine("- $highlight")
        }
        if (room.alert != null) {
            appendLine("Важно: ${room.alert}")
        }
        appendLine()
    }
    if (skipped.encrypted > 0) {
        appendLine("Зашифрованные чаты не включены: ${skipped.encrypted}")
    }
}.trim()

private fun MomentAIDailyDigest.toHtml(): String = buildString {
    append("<h3>Брифинг дня</h3>")
    if (metaSummary.isNotBlank()) {
        append("<p>${metaSummary.escapeHtml()}</p>")
    }
    rooms.forEach { room ->
        append("<h4>${room.title.escapeHtml()}</h4>")
        append("<p>${room.summary.escapeHtml()}</p>")
        if (room.highlights.isNotEmpty()) {
            append("<ul>")
            room.highlights.take(3).forEach { highlight ->
                append("<li>${highlight.escapeHtml()}</li>")
            }
            append("</ul>")
        }
        room.alert?.let { alert ->
            append("<p><strong>Важно:</strong> ${alert.escapeHtml()}</p>")
        }
    }
    if (skipped.encrypted > 0) {
        append("<p>Зашифрованные чаты не включены: ${skipped.encrypted}</p>")
    }
}

private fun String.escapeHtml(): String {
    return replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
