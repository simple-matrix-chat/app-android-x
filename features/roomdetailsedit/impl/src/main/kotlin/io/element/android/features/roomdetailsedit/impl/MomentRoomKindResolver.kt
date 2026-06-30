/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roomdetailsedit.impl

import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.createroom.MomentRoomKind
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object MomentRoomKindResolver {
    private const val MOMENT_ROOM_KIND_EVENT_TYPE = "io.moment.room_kind"
    private const val FETCH_MOMENT_ROOM_KIND_ATTEMPTS = 5
    private const val FETCH_MOMENT_ROOM_KIND_RETRY_DELAY_MS = 1_000L
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(client: MatrixClient, roomId: RoomId): MomentRoomKind? {
        repeat(FETCH_MOMENT_ROOM_KIND_ATTEMPTS) { attempt ->
            client.getRoomStateEventContent(roomId, MOMENT_ROOM_KIND_EVENT_TYPE)
                .onSuccess { content ->
                    return parse(content)
                }
            if (attempt < FETCH_MOMENT_ROOM_KIND_ATTEMPTS - 1) {
                delay(FETCH_MOMENT_ROOM_KIND_RETRY_DELAY_MS)
            }
        }
        return null
    }

    private fun parse(content: String): MomentRoomKind? {
        val kind = runCatching {
            json.parseToJsonElement(content)
                .jsonObject["kind"]
                ?.jsonPrimitive
                ?.contentOrNull
        }.getOrNull()
        return MomentRoomKind.entries.firstOrNull { it.value == kind }
    }
}
