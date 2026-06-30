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
import io.element.android.appconfig.MatrixAppApiAliasesConfig
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.model.RoomSummaryDisplayType
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.sessionstorage.api.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

interface MomentHomeRoomTypeService {
    val roomTypes: StateFlow<Map<RoomId, MomentHomeRoomType>>

    fun resolveRoomTypes(roomSummaries: List<RoomListRoomSummary>)
}

@ContributesBinding(SessionScope::class)
@SingleIn(SessionScope::class)
class DefaultMomentHomeRoomTypeService @Inject constructor(
    private val matrixClient: MatrixClient,
    private val sessionStore: SessionStore,
    private val dispatchers: CoroutineDispatchers,
    @SessionCoroutineScope
    private val sessionCoroutineScope: CoroutineScope,
) : MomentHomeRoomTypeService {
    private companion object {
        const val ROOM_KIND_EVENT_TYPE = "io.moment.room_kind"
        const val CHANNEL_KIND = "channel"
        val JSON = Json { ignoreUnknownKeys = true }
    }

    private val _roomTypes = MutableStateFlow<Map<RoomId, MomentHomeRoomType>>(emptyMap())
    override val roomTypes: StateFlow<Map<RoomId, MomentHomeRoomType>> = _roomTypes.asStateFlow()

    private val loadingRoomIds = mutableSetOf<RoomId>()

    override fun resolveRoomTypes(roomSummaries: List<RoomListRoomSummary>) {
        val roomIds = synchronized(loadingRoomIds) {
            roomSummaries.mapNotNull { summary ->
                val shouldResolve = summary.displayType == RoomSummaryDisplayType.ROOM &&
                    !summary.isDirect &&
                    !summary.isSpace &&
                    _roomTypes.value[summary.roomId] == null &&
                    loadingRoomIds.add(summary.roomId)
                summary.roomId.takeIf { shouldResolve }
            }
        }

        roomIds.forEach { roomId ->
            sessionCoroutineScope.launch(dispatchers.io) {
                val roomType = fetchRoomType(roomId)
                _roomTypes.update { roomTypes ->
                    roomTypes + (roomId to roomType)
                }
                synchronized(loadingRoomIds) {
                    loadingRoomIds.remove(roomId)
                }
            }
        }
    }

    private suspend fun fetchRoomType(roomId: RoomId): MomentHomeRoomType = withContext(dispatchers.io) {
        val homeserverUrl = sessionStore.getSession(matrixClient.sessionId.value)
            ?.homeserverUrl
            ?.trimEnd('/')
            ?: return@withContext MomentHomeRoomType.Unknown
        val encodedRoomId = roomId.value.encodePathComponent()
        val encodedEventType = ROOM_KIND_EVENT_TYPE.encodePathComponent()
        val url =
            "$homeserverUrl${MatrixAppApiAliasesConfig.CLIENT_API_PATH_PREFIX}/v3/rooms/$encodedRoomId/state/$encodedEventType/"
        val response = matrixClient.getUrl(url).getOrElse {
            return@withContext MomentHomeRoomType.Unknown
        }
        val kind = runCatching {
            JSON.parseToJsonElement(response.decodeToString())
                .jsonObject["kind"]
                ?.jsonPrimitive
                ?.contentOrNull
        }.getOrNull()
        if (kind == CHANNEL_KIND) {
            MomentHomeRoomType.Channel
        } else {
            MomentHomeRoomType.Group
        }
    }

    private fun String.encodePathComponent(): String {
        return URLEncoder.encode(this, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }
}
