/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.ai

import io.element.android.features.ai.api.MomentAIBriefingPayload
import io.element.android.features.ai.api.MomentAIDailyDigestRoom
import io.element.android.features.ai.api.MomentAIDigestSkipped
import io.element.android.features.ai.api.MomentAIDigestWindow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val BRIEFING_CONTENT_KEY = "org.moment.briefing"

internal object MomentAIBriefingPayloadParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(originalJson: String?): MomentAIBriefingPayload? {
        if (originalJson.isNullOrBlank()) return null
        return runCatching { parsePayload(originalJson) }.getOrNull()
    }

    private fun parsePayload(originalJson: String): MomentAIBriefingPayload? {
        val root = json.parseToJsonElement(originalJson).objectOrNull() ?: return null
        val content = root["content"].objectOrNull() ?: return null
        val payload = content[BRIEFING_CONTENT_KEY].objectOrNull() ?: return null
        val rooms = payload.array("rooms")
            ?.mapNotNull { room -> room.objectOrNull()?.toDigestRoom() }
            .orEmpty()
        if (rooms.isEmpty()) return null
        return MomentAIBriefingPayload(
            version = payload.int("version") ?: 1,
            generatedAt = payload.string("generated_at"),
            window = payload.obj("window")?.toWindow() ?: MomentAIDigestWindow(from = "", to = ""),
            metaSummary = payload.string("meta_summary"),
            rooms = rooms,
            skipped = payload.obj("skipped")?.toSkipped() ?: MomentAIDigestSkipped(encrypted = 0, noActivity = 0, filteredOut = 0),
            partial = payload.boolean("partial") ?: false,
            model = payload.string("model"),
        )
    }
}

private fun JsonObject.toDigestRoom(): MomentAIDailyDigestRoom {
    return MomentAIDailyDigestRoom(
        roomId = string("room_id"),
        title = string("title"),
        kind = string("kind"),
        messageCount = int("message_count") ?: 0,
        summary = string("summary"),
        highlights = array("highlights")
            ?.mapNotNull { item -> item.stringOrNull() }
            .orEmpty(),
        youMentioned = boolean("you_mentioned") ?: false,
        alert = string("alert").ifBlank { null },
    )
}

private fun JsonObject.toWindow(): MomentAIDigestWindow {
    return MomentAIDigestWindow(
        from = string("from"),
        to = string("to"),
    )
}

private fun JsonObject.toSkipped(): MomentAIDigestSkipped {
    return MomentAIDigestSkipped(
        encrypted = int("encrypted") ?: 0,
        noActivity = int("no_activity") ?: 0,
        filteredOut = int("filtered_out") ?: 0,
    )
}

private fun JsonElement?.objectOrNull(): JsonObject? = this as? JsonObject

private fun JsonElement?.stringOrNull(): String? = this?.jsonPrimitive?.contentOrNull

private fun JsonObject.obj(key: String): JsonObject? = get(key).objectOrNull()

private fun JsonObject.array(key: String): JsonArray? = get(key) as? JsonArray

private fun JsonObject.string(key: String): String = get(key).stringOrNull().orEmpty()

private fun JsonObject.int(key: String): Int? = get(key)?.jsonPrimitive?.intOrNull

private fun JsonObject.boolean(key: String): Boolean? = get(key)?.jsonPrimitive?.booleanOrNull
