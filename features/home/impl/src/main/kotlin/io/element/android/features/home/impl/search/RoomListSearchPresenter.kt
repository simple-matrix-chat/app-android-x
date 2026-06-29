/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.search

import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import io.element.android.libraries.architecture.Presenter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Inject
class RoomListSearchPresenter(
    private val dataSourceFactory: RoomListSearchDataSource.Factory,
) : Presenter<RoomListSearchState> {
    @Composable
    override fun present(): RoomListSearchState {
        // Do not use rememberSaveable so that search is not active when the user navigates back to the screen
        var isSearchActive by remember {
            mutableStateOf(false)
        }
        val searchQuery = rememberTextFieldState()

        val coroutineScope = rememberCoroutineScope()
        val dataSource = remember { dataSourceFactory.create(coroutineScope) }

        LaunchedEffect(searchQuery.text) {
            val query = searchQuery.text.toString()
            dataSource.setSearchQuery(query)
            dataSource.searchMessages(query)
            dataSource.searchMomentUsers(query)
        }

        val searchResults by dataSource.roomSummaries.collectAsState(initial = persistentListOf())
        val userResults by dataSource.userResults.collectAsState()
        val messageResults by dataSource.messageResults.collectAsState()
        val isSearchingUsers by dataSource.isSearchingUsers.collectAsState()
        val isSearchingMessages by dataSource.isSearchingMessages.collectAsState()
        val hasMoreMessageResults by dataSource.hasMoreMessageResults.collectAsState()
        val hasMessageSearchError by dataSource.hasMessageSearchError.collectAsState()
        val recentlyViewedRooms by dataSource.recentlyViewedRooms.collectAsState()
        val recentSearches by dataSource.recentSearches.collectAsState(initial = persistentListOf())

        LaunchedEffect(isSearchActive) {
            if (isSearchActive) {
                dataSource.loadRecentlyViewedRooms()
            }
        }

        fun handleEvent(event: RoomListSearchEvent) {
            when (event) {
                RoomListSearchEvent.ClearQuery -> {
                    searchQuery.clearText()
                }
                RoomListSearchEvent.ToggleSearchVisibility -> {
                    isSearchActive = !isSearchActive
                    searchQuery.clearText()
                }
                is RoomListSearchEvent.UpdateVisibleRange -> coroutineScope.launch {
                    dataSource.updateVisibleRange(visibleRange = event.range)
                    val resultCount = searchResults.size + userResults.size + messageResults.size
                    if (event.range.last >= resultCount - 3) {
                        dataSource.loadMoreMessages()
                    }
                }
                is RoomListSearchEvent.TrackRecentSearch -> coroutineScope.launch {
                    dataSource.trackRecentSearch(event.roomId)
                }
            }
        }

        return RoomListSearchState(
            isSearchActive = isSearchActive,
            query = searchQuery,
            results = if (searchQuery.text.isBlank()) persistentListOf() else searchResults,
            userResults = if (searchQuery.text.isBlank()) persistentListOf() else userResults,
            messageResults = if (searchQuery.text.isBlank()) persistentListOf() else messageResults,
            isSearchingUsers = isSearchingUsers,
            isSearchingMessages = isSearchingMessages,
            hasMoreMessageResults = hasMoreMessageResults,
            hasMessageSearchError = hasMessageSearchError,
            recentlyViewedRooms = recentlyViewedRooms,
            recentSearches = recentSearches,
            eventSink = ::handleEvent,
        )
    }
}
