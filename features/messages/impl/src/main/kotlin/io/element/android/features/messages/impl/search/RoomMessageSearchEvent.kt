/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.search

import io.element.android.libraries.matrix.api.core.EventId

sealed interface RoomMessageSearchEvent {
    data object ToggleSearchVisibility : RoomMessageSearchEvent
    data object ClearQuery : RoomMessageSearchEvent
    data class UpdateVisibleRange(val range: IntRange) : RoomMessageSearchEvent
    data class SelectResult(val eventId: EventId) : RoomMessageSearchEvent
}
