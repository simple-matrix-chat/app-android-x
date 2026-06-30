/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roomdetails.impl

import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object MomentRoomDetailsTypeResolver {
    private const val MOMENT_ROOM_KIND_EVENT_TYPE = "io.moment.room_kind"
    private const val MOMENT_ROOM_KIND_GROUP = "group"
    private const val MOMENT_ROOM_KIND_CHANNEL = "channel"
    private const val FETCH_MOMENT_ROOM_TYPE_ATTEMPTS = 5
    private const val FETCH_MOMENT_ROOM_TYPE_RETRY_DELAY_MS = 1_000L
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(client: MatrixClient, roomId: RoomId): MomentRoomDetailsType {
        repeat(FETCH_MOMENT_ROOM_TYPE_ATTEMPTS) { attempt ->
            client.getRoomStateEventContent(roomId, MOMENT_ROOM_KIND_EVENT_TYPE)
                .onSuccess { content ->
                    return parse(content)
                }
            if (attempt < FETCH_MOMENT_ROOM_TYPE_ATTEMPTS - 1) {
                delay(FETCH_MOMENT_ROOM_TYPE_RETRY_DELAY_MS)
            }
        }
        return MomentRoomDetailsType.Unknown
    }

    private fun parse(content: String): MomentRoomDetailsType {
        val kind = runCatching {
            json.parseToJsonElement(content)
                .jsonObject["kind"]
                ?.jsonPrimitive
                ?.contentOrNull
        }.getOrNull()
        return when (kind) {
            MOMENT_ROOM_KIND_GROUP -> MomentRoomDetailsType.Group
            MOMENT_ROOM_KIND_CHANNEL -> MomentRoomDetailsType.Channel
            else -> MomentRoomDetailsType.Unknown
        }
    }
}
