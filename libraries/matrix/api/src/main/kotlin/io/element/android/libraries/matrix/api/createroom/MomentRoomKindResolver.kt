/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.createroom

import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object MomentRoomKindResolver {
    const val MOMENT_ROOM_KIND_EVENT_TYPE = "io.moment.room_kind"

    private const val FETCH_MOMENT_ROOM_KIND_ATTEMPTS = 5
    private const val FETCH_MOMENT_ROOM_KIND_RETRY_DELAY_MS = 1_000L
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(
        client: MatrixClient,
        roomId: RoomId,
        attempts: Int = FETCH_MOMENT_ROOM_KIND_ATTEMPTS,
    ): MomentRoomKind? {
        repeat(attempts.coerceAtLeast(1)) { attempt ->
            client.getRoomStateEventContent(roomId, MOMENT_ROOM_KIND_EVENT_TYPE)
                .onSuccess { content ->
                    return parse(content)
                }
            if (attempt < attempts - 1) {
                delay(FETCH_MOMENT_ROOM_KIND_RETRY_DELAY_MS)
            }
        }
        return null
    }

    fun parse(content: String): MomentRoomKind? {
        val kind = runCatching {
            json.parseToJsonElement(content)
                .jsonObject["kind"]
                ?.jsonPrimitive
                ?.contentOrNull
        }.getOrNull()
        return MomentRoomKind.entries.firstOrNull { it.value == kind }
    }
}
