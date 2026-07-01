/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.ai.api

interface MomentAIDailyBriefingManager {
    suspend fun generateAndPost(force: Boolean): Result<MomentAIDailyBriefingResult>
    suspend fun isBriefingRoom(roomId: String): Result<Boolean>
}

data class MomentAIDailyBriefingResult(
    val digest: MomentAIDailyDigest,
    val roomId: String,
    val eventId: String?,
    val posted: Boolean,
)
