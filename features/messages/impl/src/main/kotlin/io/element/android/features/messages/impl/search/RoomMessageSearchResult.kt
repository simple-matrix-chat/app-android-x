/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.search

import androidx.compose.runtime.Immutable
import io.element.android.libraries.matrix.api.core.EventId

@Immutable
data class RoomMessageSearchResult(
    val eventId: EventId,
    val title: String,
    val description: String,
)
