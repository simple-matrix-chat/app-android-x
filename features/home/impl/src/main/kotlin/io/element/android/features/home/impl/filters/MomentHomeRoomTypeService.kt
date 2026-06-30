/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.filters

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.model.RoomSummaryDisplayType
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface MomentHomeRoomTypeService {
    val roomTypes: StateFlow<Map<RoomId, MomentHomeRoomType>>

    fun resolveRoomTypes(roomSummaries: List<RoomListRoomSummary>)
}

@ContributesBinding(SessionScope::class)
@SingleIn(SessionScope::class)
class DefaultMomentHomeRoomTypeService @Inject constructor(
    private val matrixClient: MatrixClient,
    private val dispatchers: CoroutineDispatchers,
    @SessionCoroutineScope
    private val sessionCoroutineScope: CoroutineScope,
) : MomentHomeRoomTypeService {
    private companion object {
        const val ROOM_KIND_EVENT_TYPE = "io.moment.room_kind"
        const val CHANNEL_KIND = "channel"
        const val FETCH_ROOM_TYPE_ATTEMPTS = 5
        const val FETCH_ROOM_TYPE_RETRY_DELAY_MS = 1_000L
        val JSON = Json { ignoreUnknownKeys = true }
    }

    private val _roomTypes = MutableStateFlow<Map<RoomId, MomentHomeRoomType>>(emptyMap())
    override val roomTypes: StateFlow<Map<RoomId, MomentHomeRoomType>> = _roomTypes.asStateFlow()

    private val loadingRoomIds = mutableSetOf<RoomId>()

    override fun resolveRoomTypes(roomSummaries: List<RoomListRoomSummary>) {
        val roomIds = synchronized(loadingRoomIds) {
            roomSummaries.mapNotNull { summary ->
                val currentRoomType = _roomTypes.value[summary.roomId]
                val shouldResolve = summary.displayType == RoomSummaryDisplayType.ROOM &&
                    !summary.isDirect &&
                    !summary.isSpace &&
                    currentRoomType != MomentHomeRoomType.Group &&
                    currentRoomType != MomentHomeRoomType.Channel &&
                    loadingRoomIds.add(summary.roomId)
                summary.roomId.takeIf { shouldResolve }
            }
        }

        roomIds.forEach { roomId ->
            sessionCoroutineScope.launch(dispatchers.io) {
                try {
                    val roomType = fetchRoomTypeWithRetry(roomId)
                    if (roomType != null) {
                        _roomTypes.update { roomTypes ->
                            roomTypes + (roomId to roomType)
                        }
                    }
                } finally {
                    synchronized(loadingRoomIds) {
                        loadingRoomIds.remove(roomId)
                    }
                }
            }
        }
    }

    private suspend fun fetchRoomTypeWithRetry(roomId: RoomId): MomentHomeRoomType? {
        repeat(FETCH_ROOM_TYPE_ATTEMPTS) { attempt ->
            val roomType = fetchRoomType(roomId)
            if (roomType != null) {
                return roomType
            }
            if (attempt < FETCH_ROOM_TYPE_ATTEMPTS - 1) {
                delay(FETCH_ROOM_TYPE_RETRY_DELAY_MS)
            }
        }
        return null
    }

    private suspend fun fetchRoomType(roomId: RoomId): MomentHomeRoomType? {
        val response = matrixClient.getRoomStateEventContent(roomId, ROOM_KIND_EVENT_TYPE).getOrElse {
            return null
        }
        val kind = runCatching {
            JSON.parseToJsonElement(response)
                .jsonObject["kind"]
                ?.jsonPrimitive
                ?.contentOrNull
        }.getOrNull()
        return if (kind == CHANNEL_KIND) {
            MomentHomeRoomType.Channel
        } else {
            MomentHomeRoomType.Group
        }
    }
}
