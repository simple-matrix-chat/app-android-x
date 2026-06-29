/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.search

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.element.android.features.home.impl.datasource.RoomListRoomSummaryFactory
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.room.CurrentUserMembership
import io.element.android.libraries.matrix.api.room.RoomInfo
import io.element.android.libraries.matrix.api.roomlist.RoomList
import io.element.android.libraries.matrix.api.roomlist.RoomListFilter
import io.element.android.libraries.matrix.api.roomlist.RoomListService
import io.element.android.libraries.matrix.api.roomlist.updateVisibleRange
import io.element.android.libraries.matrix.api.search.MatrixMessageSearchResult
import io.element.android.libraries.matrix.api.user.MatrixMomentUserSearchMatch
import io.element.android.libraries.matrix.ui.model.getAvatarData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.Locale

private const val PAGE_SIZE = 30
private const val MAX_RECENTLY_VIEWED_ROOMS = 5
private const val MIN_MOMENT_USER_SEARCH_DIGITS = 5
private const val MAX_MOMENT_USER_SEARCH_RESULTS = 10

@AssistedInject
class RoomListSearchDataSource(
    @Assisted coroutineScope: CoroutineScope,
    private val matrixClient: MatrixClient,
    roomListService: RoomListService,
    coroutineDispatchers: CoroutineDispatchers,
    private val roomSummaryFactory: RoomListRoomSummaryFactory,
    private val recentSearchesStore: RoomListRecentSearchesStore,
) {
    @AssistedFactory
    interface Factory {
        fun create(coroutineScope: CoroutineScope): RoomListSearchDataSource
    }

    private val roomList = roomListService.createRoomList(
        pageSize = PAGE_SIZE,
        source = RoomList.Source.All,
        coroutineScope = coroutineScope
    )

    val roomSummaries: Flow<ImmutableList<RoomListRoomSummary>> = roomList.summaries
        .map { roomSummaries ->
            roomSummaries
                .map(roomSummaryFactory::create)
                .toImmutableList()
        }
        .flowOn(coroutineDispatchers.computation)

    val recentSearches: Flow<ImmutableList<RoomListSearchRoomResult>> = recentSearchesStore.roomIds
        .map { roomIds ->
            roomIds.mapNotNull { roomId -> createRoomResult(roomId) }.toImmutableList()
        }
        .flowOn(coroutineDispatchers.io)

    private val _recentlyViewedRooms = MutableStateFlow<ImmutableList<RoomListSearchRoomResult>>(persistentListOf())
    val recentlyViewedRooms: StateFlow<ImmutableList<RoomListSearchRoomResult>> = _recentlyViewedRooms
    private val _userResults = MutableStateFlow<ImmutableList<RoomListSearchUserResult>>(persistentListOf())
    val userResults: StateFlow<ImmutableList<RoomListSearchUserResult>> = _userResults
    private val _isSearchingUsers = MutableStateFlow(false)
    val isSearchingUsers: StateFlow<Boolean> = _isSearchingUsers
    private val _messageResults = MutableStateFlow<ImmutableList<RoomListSearchMessageResult>>(persistentListOf())
    val messageResults: StateFlow<ImmutableList<RoomListSearchMessageResult>> = _messageResults
    private val _isSearchingMessages = MutableStateFlow(false)
    val isSearchingMessages: StateFlow<Boolean> = _isSearchingMessages
    private val _hasMoreMessageResults = MutableStateFlow(false)
    val hasMoreMessageResults: StateFlow<Boolean> = _hasMoreMessageResults
    private val _hasMessageSearchError = MutableStateFlow(false)
    val hasMessageSearchError: StateFlow<Boolean> = _hasMessageSearchError
    private var currentMessageSearchQuery = ""
    private var nextMessageSearchBatch: String? = null
    private var messageSearchGeneration = 0
    private var userSearchGeneration = 0

    suspend fun updateVisibleRange(visibleRange: IntRange) {
        roomList.updateVisibleRange(visibleRange)
    }

    suspend fun loadRecentlyViewedRooms() {
        val roomIds = matrixClient.getRecentlyVisitedRooms().getOrDefault(emptyList())
        _recentlyViewedRooms.value = roomIds
            .mapNotNull { roomId -> createRoomResult(roomId) }
            .take(MAX_RECENTLY_VIEWED_ROOMS)
            .toImmutableList()
    }

    suspend fun trackRecentSearch(roomId: RoomId) {
        recentSearchesStore.track(listOf(roomId))
    }

    suspend fun searchMessages(searchQuery: String) {
        val trimmedSearchQuery = searchQuery.trim()
        val generation = ++messageSearchGeneration
        currentMessageSearchQuery = trimmedSearchQuery
        nextMessageSearchBatch = null

        if (trimmedSearchQuery.isEmpty()) {
            _messageResults.value = persistentListOf()
            _isSearchingMessages.value = false
            _hasMoreMessageResults.value = false
            _hasMessageSearchError.value = false
            return
        }

        _messageResults.value = persistentListOf()
        _isSearchingMessages.value = true
        _hasMoreMessageResults.value = false
        _hasMessageSearchError.value = false

        val result = matrixClient.searchMessages(trimmedSearchQuery)
        if (generation != messageSearchGeneration) return

        _isSearchingMessages.value = false
        result
            .onSuccess { page ->
                nextMessageSearchBatch = page.nextBatch
                _hasMoreMessageResults.value = page.nextBatch != null
                _hasMessageSearchError.value = false
                _messageResults.value = page.results.mapNotNull { result -> result.toSearchMessageResult() }.toImmutableList()
            }
            .onFailure {
                nextMessageSearchBatch = null
                _hasMoreMessageResults.value = false
                _hasMessageSearchError.value = true
            }
    }

    suspend fun searchMomentUsers(searchQuery: String) {
        val trimmedSearchQuery = searchQuery.trim()
        val generation = ++userSearchGeneration

        if (trimmedSearchQuery.isEmpty() || trimmedSearchQuery.count { it.isDigit() } < MIN_MOMENT_USER_SEARCH_DIGITS) {
            _userResults.value = persistentListOf()
            _isSearchingUsers.value = false
            return
        }

        _userResults.value = persistentListOf()
        _isSearchingUsers.value = true

        val result = matrixClient.searchMomentUsers(
            query = trimmedSearchQuery,
            limit = MAX_MOMENT_USER_SEARCH_RESULTS,
            defaultCountry = Locale.getDefault().country,
        )
        if (generation != userSearchGeneration) return

        _isSearchingUsers.value = false
        result.onSuccess { matches ->
            _userResults.value = matches
                .filterNot { match -> matrixClient.isMe(match.userId) }
                .distinctBy { match -> match.userId }
                .mapNotNull { match -> match.toSearchUserResult() }
                .toImmutableList()
        }.onFailure {
            _userResults.value = persistentListOf()
        }
    }

    suspend fun loadMoreMessages() {
        val nextBatch = nextMessageSearchBatch ?: return
        if (_isSearchingMessages.value || currentMessageSearchQuery.isEmpty()) return

        val generation = messageSearchGeneration
        _isSearchingMessages.value = true
        _hasMessageSearchError.value = false

        val result = matrixClient.searchMessages(currentMessageSearchQuery, nextBatch = nextBatch)
        if (generation != messageSearchGeneration) return

        _isSearchingMessages.value = false
        result
            .onSuccess { page ->
                nextMessageSearchBatch = page.nextBatch
                _hasMoreMessageResults.value = page.nextBatch != null
                _hasMessageSearchError.value = false
                val existingResults = _messageResults.value
                val existingEventIds = existingResults.map { it.eventId }.toSet()
                _messageResults.value = (
                    existingResults + page.results
                        .filterNot { result -> existingEventIds.contains(result.eventId) }
                        .mapNotNull { result -> result.toSearchMessageResult() }
                    ).toImmutableList()
            }
            .onFailure {
                nextMessageSearchBatch = null
                _hasMoreMessageResults.value = false
                _hasMessageSearchError.value = true
            }
    }

    suspend fun setSearchQuery(searchQuery: String) = coroutineScope {
        val filter = if (searchQuery.isBlank()) {
            RoomListFilter.None
        } else {
            RoomListFilter.NormalizedMatchRoomName(searchQuery)
        }
        roomList.updateFilter(filter)
    }

    private suspend fun createRoomResult(roomId: RoomId): RoomListSearchRoomResult? {
        return matrixClient.getRoom(roomId)?.use { room ->
            room.info().toSearchResult()
        }
    }

    private fun RoomInfo.toSearchResult(): RoomListSearchRoomResult? {
        if (isSpace || currentUserMembership != CurrentUserMembership.JOINED) return null

        return RoomListSearchRoomResult(
            roomId = id,
            title = name ?: id.value,
            description = topic?.takeIf(String::isNotBlank) ?: canonicalAlias?.value,
            avatarData = getAvatarData(AvatarSize.RoomListItem),
            heroes = heroes.map { user -> user.getAvatarData(AvatarSize.RoomListItem) }.toImmutableList(),
            isTombstoned = successorRoom != null,
        )
    }

    private suspend fun MatrixMessageSearchResult.toSearchMessageResult(): RoomListSearchMessageResult {
        val roomInfo = matrixClient.getRoom(roomId)?.use { room -> room.info() }
        val roomTitle = roomInfo?.name ?: roomId.value
        val sender = senderDisplayName?.takeIf(String::isNotBlank) ?: senderId?.value
        val description = if (sender.isNullOrBlank()) message else "$sender: $message"

        return RoomListSearchMessageResult(
            roomId = roomId,
            eventId = eventId,
            roomTitle = roomTitle,
            description = description,
            avatarData = roomInfo?.getAvatarData(AvatarSize.RoomListItem) ?: AvatarData(roomId.value, roomTitle, size = AvatarSize.RoomListItem),
            heroes = roomInfo?.heroes?.map { user -> user.getAvatarData(AvatarSize.RoomListItem) }?.toImmutableList() ?: persistentListOf(),
            isTombstoned = roomInfo?.successorRoom != null,
        )
    }

    private suspend fun MatrixMomentUserSearchMatch.toSearchUserResult(): RoomListSearchUserResult {
        val matrixUser = matrixUser
        val title = displayName?.takeIf(String::isNotBlank) ?: userId.extractedDisplayName
        val description = phoneNumber?.takeIf(String::isNotBlank) ?: userId.value
        return RoomListSearchUserResult(
            matrixUser = matrixUser,
            directRoomId = matrixClient.findDM(userId).getOrNull(),
            title = title,
            description = description,
            avatarData = matrixUser.getAvatarData(AvatarSize.RoomListItem),
        )
    }
}
