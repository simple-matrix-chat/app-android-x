/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.search

import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId

data class MatrixMessageSearchPage(
    val results: List<MatrixMessageSearchResult>,
    val nextBatch: String?,
)

data class MatrixMessageSearchResult(
    val roomId: RoomId,
    val eventId: EventId,
    val senderId: UserId?,
    val senderDisplayName: String?,
    val message: String,
    val originServerTimestamp: Long?,
)
