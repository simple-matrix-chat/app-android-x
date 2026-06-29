/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.home.impl.R
import io.element.android.features.home.impl.components.RoomSummaryRow
import io.element.android.features.home.impl.contentType
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.roomlist.RoomListEvent
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.SearchField
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.OnVisibleRangeChangeEffect
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
internal fun RoomListSearchView(
    state: RoomListSearchState,
    hideInvitesAvatars: Boolean,
    eventSink: (RoomListEvent) -> Unit,
    onRoomClick: (RoomId, EventId?) -> Unit,
    onUserClick: (MatrixUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.isSearchActive) {
        state.eventSink(RoomListSearchEvent.ToggleSearchVisibility)
    }

    AnimatedVisibility(
        visible = state.isSearchActive,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        RoomListSearchContent(
            state = state,
            hideInvitesAvatars = hideInvitesAvatars,
            onRoomClick = onRoomClick,
            onUserClick = onUserClick,
            eventSink = eventSink,
            modifier = modifier,
        )
    }
}

@Composable
private fun RoomListSearchContent(
    state: RoomListSearchState,
    hideInvitesAvatars: Boolean,
    eventSink: (RoomListEvent) -> Unit,
    onRoomClick: (RoomId, EventId?) -> Unit,
    onUserClick: (MatrixUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun onDismiss() {
        state.eventSink(RoomListSearchEvent.ToggleSearchVisibility)
    }

    fun onRoomClick(room: RoomListRoomSummary) {
        state.eventSink(RoomListSearchEvent.TrackRecentSearch(room.roomId))
        onRoomClick(room.roomId, null)
    }

    fun onRecentSearchClick(room: RoomListSearchRoomResult) {
        state.eventSink(RoomListSearchEvent.TrackRecentSearch(room.roomId))
        onRoomClick(room.roomId, null)
    }

    fun onRecentlyViewedClick(room: RoomListSearchRoomResult) {
        onRoomClick(room.roomId, null)
    }

    fun onMessageClick(result: RoomListSearchMessageResult) {
        state.eventSink(RoomListSearchEvent.TrackRecentSearch(result.roomId))
        onRoomClick(result.roomId, result.eventId)
    }

    fun onUserResultClick(result: RoomListSearchUserResult) {
        val directRoomId = result.directRoomId
        if (directRoomId != null) {
            state.eventSink(RoomListSearchEvent.TrackRecentSearch(directRoomId))
            onRoomClick(directRoomId, null)
        } else {
            onUserClick(result.matrixUser)
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = ::onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxWidth()
                .heightIn(max = 800.dp)
                .clip(SearchCardShape)
                .background(ElementTheme.colors.bgCanvasDefault)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
        ) {
            SearchHeader(
                state = state,
            )

            val lazyListState = rememberLazyListState()
            OnVisibleRangeChangeEffect(lazyListState) { visibleRange ->
                state.eventSink(RoomListSearchEvent.UpdateVisibleRange(visibleRange))
            }
            val recentlyViewedTitle = stringResource(R.string.screen_home_search_recently_viewed)
            val recentSearchesTitle = stringResource(R.string.screen_home_search_recent_searches)
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                if (state.shouldShowRecents) {
                    val highlightedRoomId = state.recentlyViewedRooms.firstOrNull()?.roomId ?: state.recentSearches.firstOrNull()?.roomId
                    searchResultSection(
                        title = recentlyViewedTitle,
                        rooms = state.recentlyViewedRooms,
                        keyPrefix = "recently-viewed",
                        highlightedRoomId = highlightedRoomId,
                        onRoomClick = ::onRecentlyViewedClick,
                    )
                    searchResultSection(
                        title = recentSearchesTitle,
                        rooms = state.recentSearches,
                        keyPrefix = "recent-search",
                        highlightedRoomId = highlightedRoomId,
                        onRoomClick = ::onRecentSearchClick,
                    )
                } else {
                    if (state.hasEmptySearchResults) {
                        item {
                            EmptySearchRow()
                        }
                    }
                    val highlightedRoomId = state.results.firstOrNull()?.roomId
                    val highlightedUserId = if (highlightedRoomId == null) {
                        state.userResults.firstOrNull()?.matrixUser?.userId
                    } else {
                        null
                    }
                    val highlightedMessageEventId = if (highlightedRoomId == null && highlightedUserId == null) {
                        state.messageResults.firstOrNull()?.eventId
                    } else {
                        null
                    }
                    items(
                        items = state.results,
                        contentType = { room -> room.contentType() },
                    ) { room ->
                        RoomSummaryRow(
                            room = room,
                            hideInviteAvatars = hideInvitesAvatars,
                            // TODO
                            isInviteSeen = false,
                            onClick = ::onRoomClick,
                            eventSink = eventSink,
                            modifier = Modifier.background(
                                if (room.roomId == highlightedRoomId) {
                                    ElementTheme.colors.bgSubtlePrimary
                                } else {
                                    Color.Transparent
                                }
                            ),
                        )
                    }
                    items(
                        items = state.userResults,
                        key = { result -> "user-${result.matrixUser.userId.value}" },
                        contentType = { "user-search-result" },
                    ) { result ->
                        RoomListSearchUserResultRow(
                            result = result,
                            isHighlighted = result.matrixUser.userId == highlightedUserId,
                            onClick = ::onUserResultClick,
                        )
                    }
                    items(
                        items = state.messageResults,
                        key = { result -> result.eventId.value },
                        contentType = { "message-search-result" },
                    ) { result ->
                        RoomListSearchMessageResultRow(
                            result = result,
                            isHighlighted = result.eventId == highlightedMessageEventId,
                            onClick = ::onMessageClick,
                        )
                    }
                    if (state.isSearchingMessages || state.isSearchingUsers) {
                        item(contentType = "loading") {
                            LoadingSearchRow()
                        }
                    }
                    if (state.hasMessageSearchError) {
                        item(contentType = "error") {
                            ErrorSearchRow()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHeader(
    state: RoomListSearchState,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    SearchField(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 8.dp)
            .focusRequester(focusRequester),
        state = state.query,
        placeholder = stringResource(CommonStrings.action_search),
    )

    LaunchedEffect(Unit) {
        if (!focusRequester.restoreFocusedChild()) {
            focusRequester.requestFocus()
        }
        focusRequester.saveFocusedChild()
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.searchResultSection(
    title: String,
    rooms: List<RoomListSearchRoomResult>,
    keyPrefix: String,
    highlightedRoomId: RoomId?,
    onRoomClick: (RoomListSearchRoomResult) -> Unit,
) {
    if (rooms.isEmpty()) return

    item(contentType = "header") {
        SearchSectionHeader(title = title)
    }
    items(
        items = rooms,
        key = { room -> "$keyPrefix-${room.roomId.value}" },
        contentType = { "recent-room" },
    ) { room ->
        RoomListSearchResultRow(
            room = room,
            isHighlighted = room.roomId == highlightedRoomId,
            onClick = onRoomClick,
        )
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 14.dp, end = 24.dp, bottom = 6.dp),
        text = title,
        style = ElementTheme.typography.fontBodySmMedium,
        color = ElementTheme.colors.textSecondary,
    )
}

@Composable
private fun RoomListSearchResultRow(
    room: RoomListSearchRoomResult,
    isHighlighted: Boolean,
    onClick: (RoomListSearchRoomResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isHighlighted) ElementTheme.colors.bgSubtlePrimary else Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(room) }
                .heightIn(min = 64.dp)
                .padding(start = 16.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                avatarData = room.avatarData,
                avatarType = AvatarType.Room(
                    heroes = room.heroes,
                    isTombstoned = room.isTombstoned,
                ),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = room.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ElementTheme.typography.fontBodyLgRegular,
                    color = ElementTheme.colors.textPrimary,
                )
                Text(
                    text = room.description ?: stringResource(CommonStrings.common_room),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
    }
}

@Composable
private fun RoomListSearchUserResultRow(
    result: RoomListSearchUserResult,
    isHighlighted: Boolean,
    onClick: (RoomListSearchUserResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isHighlighted) ElementTheme.colors.bgSubtlePrimary else Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(result) }
                .heightIn(min = 64.dp)
                .padding(start = 16.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                avatarData = result.avatarData,
                avatarType = AvatarType.User,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = result.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ElementTheme.typography.fontBodyLgRegular,
                    color = ElementTheme.colors.textPrimary,
                )
                Text(
                    text = result.description,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
    }
}

@Composable
private fun RoomListSearchMessageResultRow(
    result: RoomListSearchMessageResult,
    isHighlighted: Boolean,
    onClick: (RoomListSearchMessageResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isHighlighted) ElementTheme.colors.bgSubtlePrimary else Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(result) }
                .heightIn(min = 64.dp)
                .padding(start = 16.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                avatarData = result.avatarData,
                avatarType = AvatarType.Room(
                    heroes = result.heroes,
                    isTombstoned = result.isTombstoned,
                ),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = result.roomTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ElementTheme.typography.fontBodyLgRegular,
                    color = ElementTheme.colors.textPrimary,
                )
                Text(
                    text = result.description,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
    }
}

@Composable
private fun EmptySearchRow(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        text = stringResource(CommonStrings.common_no_results),
        style = ElementTheme.typography.fontBodyMdRegular,
        color = ElementTheme.colors.textSecondary,
    )
}

@Composable
private fun LoadingSearchRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = ElementTheme.colors.iconSecondary,
            strokeWidth = 2.dp,
        )
        Text(
            text = stringResource(CommonStrings.common_loading),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun ErrorSearchRow(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        text = stringResource(CommonStrings.error_unknown),
        style = ElementTheme.typography.fontBodyMdRegular,
        color = ElementTheme.colors.textCriticalPrimary,
    )
}

@PreviewsDayNight
@Composable
internal fun RoomListSearchContentPreview(@PreviewParameter(RoomListSearchStateProvider::class) state: RoomListSearchState) = ElementPreview {
    RoomListSearchContent(
        state = state,
        hideInvitesAvatars = false,
        onRoomClick = { _, _ -> },
        onUserClick = {},
        eventSink = {},
    )
}

private val SearchCardShape = RoundedCornerShape(10.dp)
