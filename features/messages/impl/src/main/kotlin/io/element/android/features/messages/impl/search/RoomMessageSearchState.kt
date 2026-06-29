/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.search

import androidx.compose.foundation.text.input.TextFieldState
import kotlinx.collections.immutable.ImmutableList

data class RoomMessageSearchState(
    val isSearchEnabled: Boolean,
    val isSearchActive: Boolean,
    val query: TextFieldState,
    val results: ImmutableList<RoomMessageSearchResult>,
    val isSearching: Boolean,
    val hasMoreResults: Boolean,
    val hasSearchError: Boolean,
    val eventSink: (RoomMessageSearchEvent) -> Unit,
) {
    val hasEmptySearchResults: Boolean
        get() = query.text.isNotBlank() &&
            results.isEmpty() &&
            !isSearching &&
            !hasSearchError
}
