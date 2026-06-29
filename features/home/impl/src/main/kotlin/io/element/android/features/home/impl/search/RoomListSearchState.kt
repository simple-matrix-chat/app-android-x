/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.search

import androidx.compose.foundation.text.input.TextFieldState
import io.element.android.features.home.impl.model.RoomListRoomSummary
import kotlinx.collections.immutable.ImmutableList

data class RoomListSearchState(
    val isSearchActive: Boolean,
    val query: TextFieldState,
    val results: ImmutableList<RoomListRoomSummary>,
    val userResults: ImmutableList<RoomListSearchUserResult>,
    val messageResults: ImmutableList<RoomListSearchMessageResult>,
    val isSearchingUsers: Boolean,
    val isSearchingMessages: Boolean,
    val hasMoreMessageResults: Boolean,
    val hasMessageSearchError: Boolean,
    val recentlyViewedRooms: ImmutableList<RoomListSearchRoomResult>,
    val recentSearches: ImmutableList<RoomListSearchRoomResult>,
    val eventSink: (RoomListSearchEvent) -> Unit
) {
    val shouldShowRecents: Boolean
        get() = query.text.isBlank() && (recentlyViewedRooms.isNotEmpty() || recentSearches.isNotEmpty())

    val hasEmptySearchResults: Boolean
        get() = query.text.isNotBlank() &&
            results.isEmpty() &&
            userResults.isEmpty() &&
            messageResults.isEmpty() &&
            !isSearchingUsers &&
            !isSearchingMessages &&
            !hasMessageSearchError
}
