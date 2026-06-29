/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.roomlist.aRoomListRoomSummaryList
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

class RoomListSearchStateProvider : PreviewParameterProvider<RoomListSearchState> {
    override val values: Sequence<RoomListSearchState>
        get() = sequenceOf(
            aRoomListSearchState(),
            aRoomListSearchState(
                isSearchActive = true,
                query = "Test",
                results = aRoomListRoomSummaryList(),
                userResults = persistentListOf(aRoomListSearchUserResult()),
                messageResults = persistentListOf(aRoomListSearchMessageResult()),
            ),
        )
}

fun aRoomListSearchState(
    isSearchActive: Boolean = false,
    query: String = "",
    results: ImmutableList<RoomListRoomSummary> = persistentListOf(),
    userResults: ImmutableList<RoomListSearchUserResult> = persistentListOf(),
    messageResults: ImmutableList<RoomListSearchMessageResult> = persistentListOf(),
    isSearchingUsers: Boolean = false,
    isSearchingMessages: Boolean = false,
    hasMoreMessageResults: Boolean = false,
    hasMessageSearchError: Boolean = false,
    recentlyViewedRooms: ImmutableList<RoomListSearchRoomResult> = persistentListOf(),
    recentSearches: ImmutableList<RoomListSearchRoomResult> = persistentListOf(),
    eventSink: (RoomListSearchEvent) -> Unit = { },
) = RoomListSearchState(
    isSearchActive = isSearchActive,
    query = TextFieldState(initialText = query),
    results = results,
    userResults = userResults,
    messageResults = messageResults,
    isSearchingUsers = isSearchingUsers,
    isSearchingMessages = isSearchingMessages,
    hasMoreMessageResults = hasMoreMessageResults,
    hasMessageSearchError = hasMessageSearchError,
    recentlyViewedRooms = recentlyViewedRooms,
    recentSearches = recentSearches,
    eventSink = eventSink,
)

fun aRoomListSearchMessageResult(
    roomId: RoomId = RoomId("!room:server.org"),
    eventId: EventId = EventId("\$event"),
    roomTitle: String = "Room",
    description: String = "Alice: Search result message",
) = RoomListSearchMessageResult(
    roomId = roomId,
    eventId = eventId,
    roomTitle = roomTitle,
    description = description,
    avatarData = AvatarData(roomId.value, roomTitle, size = AvatarSize.RoomListItem),
    heroes = persistentListOf(),
    isTombstoned = false,
)

fun aRoomListSearchUserResult(
    userId: UserId = UserId("@alice:server.org"),
    directRoomId: RoomId? = null,
    title: String = "Alice",
    description: String = "+1 555 0100",
) = MatrixUser(
    userId = userId,
    displayName = title,
).let { user ->
    RoomListSearchUserResult(
        matrixUser = user,
        directRoomId = directRoomId,
        title = title,
        description = description,
        avatarData = AvatarData(userId.value, title, size = AvatarSize.RoomListItem),
    )
}
