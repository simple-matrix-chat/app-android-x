/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalHazeMaterialsApi::class)

package io.element.android.features.home.impl

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.home.impl.components.HomeTopBar
import io.element.android.features.home.impl.components.RoomListContentView
import io.element.android.features.home.impl.components.RoomListMenuAction
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.roomlist.RoomListContextMenu
import io.element.android.features.home.impl.roomlist.RoomListDeclineInviteMenu
import io.element.android.features.home.impl.roomlist.RoomListDirectUserBlockConfirmationDialog
import io.element.android.features.home.impl.roomlist.RoomListEvent
import io.element.android.features.home.impl.roomlist.RoomListState
import io.element.android.features.home.impl.search.RoomListSearchView
import io.element.android.features.home.impl.spacefilters.SpaceFiltersEvent
import io.element.android.features.home.impl.spacefilters.SpaceFiltersState
import io.element.android.features.home.impl.spacefilters.SpaceFiltersView
import io.element.android.features.home.impl.spaces.HomeSpacesView
import io.element.android.libraries.androidutils.throttler.FirstThrottler
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarHost
import io.element.android.libraries.designsystem.utils.snackbar.rememberSnackbarHostState
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.user.MatrixUser
import kotlinx.coroutines.launch

@Composable
fun HomeView(
    homeState: HomeState,
    onRoomClick: (RoomId, EventId?) -> Unit,
    onUserClick: (MatrixUser) -> Unit,
    onSettingsClick: () -> Unit,
    onStartChatClick: () -> Unit,
    onOpenContactsClick: () -> Unit,
    onCreateSpaceClick: () -> Unit,
    onRoomSettingsClick: (roomId: RoomId) -> Unit,
    onMenuActionClick: (RoomListMenuAction) -> Unit,
    onReportRoomClick: (roomId: RoomId) -> Unit,
    onDeclineInviteAndBlockUser: (roomSummary: RoomListRoomSummary) -> Unit,
    acceptDeclineInviteView: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leaveRoomView: @Composable () -> Unit,
) {
    val state: RoomListState = homeState.roomListState
    val coroutineScope = rememberCoroutineScope()
    val firstThrottler = remember { FirstThrottler(300, coroutineScope) }
    Box(modifier) {
        if (state.contextMenu is RoomListState.ContextMenu.Shown) {
            RoomListContextMenu(
                contextMenu = state.contextMenu,
                canReportRoom = state.canReportRoom,
                eventSink = state.eventSink,
                onRoomSettingsClick = onRoomSettingsClick,
                onReportRoomClick = onReportRoomClick,
            )
        }
        if (state.declineInviteMenu is RoomListState.DeclineInviteMenu.Shown) {
            RoomListDeclineInviteMenu(
                menu = state.declineInviteMenu,
                canReportRoom = state.canReportRoom,
                eventSink = state.eventSink,
                onDeclineAndBlockClick = onDeclineInviteAndBlockUser,
            )
        }
        if (state.directUserBlockConfirmation is RoomListState.DirectUserBlockConfirmation.Shown) {
            RoomListDirectUserBlockConfirmationDialog(
                confirmation = state.directUserBlockConfirmation,
                eventSink = state.eventSink,
            )
        }

        leaveRoomView()

        HomeScaffold(
            state = homeState,
            onRoomClick = { if (firstThrottler.canHandle()) onRoomClick(it, null) },
            onOpenSettings = { if (firstThrottler.canHandle()) onSettingsClick() },
            onStartChatClick = { if (firstThrottler.canHandle()) onStartChatClick() },
            onOpenContactsClick = { if (firstThrottler.canHandle()) onOpenContactsClick() },
            onCreateSpaceClick = { if (firstThrottler.canHandle()) onCreateSpaceClick() },
        )
        // This overlaid view will only be visible when state.displaySearchResults is true
        RoomListSearchView(
            state = state.searchState,
            eventSink = state.eventSink,
            hideInvitesAvatars = state.hideInvitesAvatars,
            onRoomClick = { roomId, eventId -> if (firstThrottler.canHandle()) onRoomClick(roomId, eventId) },
            onUserClick = { user -> if (firstThrottler.canHandle()) onUserClick(user) },
            modifier = Modifier
                .fillMaxSize()
        )
        acceptDeclineInviteView()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScaffold(
    state: HomeState,
    onRoomClick: (RoomId) -> Unit,
    onOpenSettings: () -> Unit,
    onStartChatClick: () -> Unit,
    onOpenContactsClick: () -> Unit,
    onCreateSpaceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    fun onRoomSummaryClick(room: RoomListRoomSummary) {
        onRoomClick(room.roomId)
    }

    val appBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(appBarState)
    val snackbarHostState = rememberSnackbarHostState(snackbarMessage = state.snackbarMessage)
    val roomListState: RoomListState = state.roomListState

    BackHandler(enabled = state.isBackHandlerEnabled) {
        if (state.currentHomeNavigationBarItem != HomeNavigationBarItem.Chats) {
            state.eventSink(HomeEvent.SelectHomeNavigationBarItem(HomeNavigationBarItem.Chats))
        } else {
            val spaceFiltersState = state.roomListState.spaceFiltersState
            if (spaceFiltersState is SpaceFiltersState.Selected) {
                spaceFiltersState.eventSink(SpaceFiltersEvent.Selected.ClearSelection)
            }
        }
    }

    val hazeState = rememberHazeState()
    val roomsLazyListState = rememberLazyListState()
    val spacesLazyListState = rememberLazyListState()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            HomeTopBar(
                selectedNavigationItem = state.currentHomeNavigationBarItem,
                currentUserAndNeighbors = state.currentUserAndNeighbors,
                showAvatarIndicator = state.showAvatarIndicator,
                areSearchResultsDisplayed = roomListState.searchState.isSearchActive,
                onToggleSearch = { roomListState.eventSink(RoomListEvent.ToggleSearchResults) },
                onStartChatClick = onStartChatClick,
                onOpenContactsClick = onOpenContactsClick,
                onOpenSettings = onOpenSettings,
                onAccountSwitch = {
                    state.eventSink(HomeEvent.SwitchToAccount(it))
                },
                scrollBehavior = scrollBehavior,
                displayFilters = state.displayRoomListFilters,
                filtersState = roomListState.filtersState,
                spaceFiltersState = roomListState.spaceFiltersState,
                modifier = Modifier.hazeEffect(
                    state = hazeState,
                    style = HazeMaterials.thick(),
                )
            )
        },
        floatingActionButton = {
            val coroutineScope = rememberCoroutineScope()
            HomeBottomBar(
                currentHomeNavigationBarItem = state.currentHomeNavigationBarItem,
                onChatsClick = {
                    // scroll to top if selecting the same item
                    if (state.currentHomeNavigationBarItem == HomeNavigationBarItem.Chats) {
                        coroutineScope.launch {
                            if (roomsLazyListState.firstVisibleItemIndex > 10) {
                                roomsLazyListState.scrollToItem(10)
                            }
                            // Also reset the scrollBehavior height offset as it's not triggered by programmatic scrolls
                            scrollBehavior.state.heightOffset = 0f
                            roomsLazyListState.animateScrollToItem(0)
                        }
                    } else {
                        state.eventSink(HomeEvent.SelectHomeNavigationBarItem(HomeNavigationBarItem.Chats))
                    }
                },
                onProfileClick = onOpenSettings,
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        content = { padding ->
            val contentPadding = PaddingValues(
                bottom = 96.dp,
            )
            when (state.currentHomeNavigationBarItem) {
                HomeNavigationBarItem.Chats -> {
                    RoomListContentView(
                        contentState = roomListState.contentState,
                        filtersState = roomListState.filtersState,
                        spaceFiltersState = roomListState.spaceFiltersState,
                        lazyListState = roomsLazyListState,
                        hideInvitesAvatars = roomListState.hideInvitesAvatars,
                        eventSink = roomListState.eventSink,
                        onRoomClick = ::onRoomSummaryClick,
                        onCreateRoomClick = onStartChatClick,
                        contentPadding = contentPadding,
                        modifier = Modifier
                            .padding(
                                PaddingValues(
                                    start = padding.calculateStartPadding(LocalLayoutDirection.current),
                                    end = padding.calculateEndPadding(LocalLayoutDirection.current),
                                    // Remove these two lines once https://issuetracker.google.com/issues/436432313 has been fixed
                                    bottom = padding.calculateBottomPadding(),
                                    top = padding.calculateTopPadding()
                                )
                            )
                            .consumeWindowInsets(padding)
                            .hazeSource(state = hazeState)
                    )
                    SpaceFiltersView(roomListState.spaceFiltersState)
                }
                HomeNavigationBarItem.Spaces -> {
                    HomeSpacesView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .consumeWindowInsets(padding)
                            .hazeSource(state = hazeState),
                        contentPadding = contentPadding,
                        state = state.homeSpacesState,
                        lazyListState = spacesLazyListState,
                        onSpaceClick = { spaceId ->
                            onRoomClick(spaceId)
                        },
                        onCreateSpaceClick = onCreateSpaceClick,
                        // TODO use actual callbacks for this
                        onExploreClick = {},
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    )
}

@Composable
private fun HomeBottomBar(
    currentHomeNavigationBarItem: HomeNavigationBarItem,
    onChatsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .zIndex(1f)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.12f),
            )
            .clip(CircleShape)
            .background(ElementTheme.colors.bgCanvasDefaultLevel1)
            .border(
                width = 1.dp,
                color = ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.55f),
                shape = CircleShape,
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MomentHomeBottomBarButton(
            item = MomentHomeBottomBarItem.Chats,
            isSelected = currentHomeNavigationBarItem == HomeNavigationBarItem.Chats,
            onClick = onChatsClick,
        )
        MomentHomeBottomBarButton(
            item = MomentHomeBottomBarItem.Profile,
            isSelected = false,
            onClick = onProfileClick,
        )
    }
}

@Composable
private fun MomentHomeBottomBarButton(
    item: MomentHomeBottomBarItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(item.labelRes)
    Box(
        modifier = modifier
            .width(64.dp)
            .height(44.dp)
            .clip(CircleShape)
            .background(if (isSelected) ElementTheme.colors.bgSubtleSecondary else Color.Transparent)
            .clickable(
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                selected = isSelected
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = item.icon(isSelected),
            contentDescription = null,
            tint = if (isSelected) ElementTheme.colors.iconPrimary else ElementTheme.colors.iconSecondary,
        )
    }
}

private enum class MomentHomeBottomBarItem(
    @StringRes val labelRes: Int,
) {
    Chats(R.string.screen_home_tab_chats),
    Profile(R.string.screen_home_tab_profile);

    @Composable
    fun icon(isSelected: Boolean): ImageVector {
        return when (this) {
            Chats -> if (isSelected) CompoundIcons.ChatSolid() else CompoundIcons.Chat()
            Profile -> if (isSelected) CompoundIcons.UserProfileSolid() else CompoundIcons.UserProfile()
        }
    }
}

internal fun RoomListRoomSummary.contentType() = displayType.ordinal

@PreviewsDayNight
@Composable
internal fun HomeViewPreview(@PreviewParameter(HomeStateProvider::class) state: HomeState) = ElementPreview {
    HomeView(
        homeState = state,
        onRoomClick = { _, _ -> },
        onUserClick = {},
        onSettingsClick = {},
        onStartChatClick = {},
        onOpenContactsClick = {},
        onCreateSpaceClick = {},
        onRoomSettingsClick = {},
        onReportRoomClick = {},
        onMenuActionClick = {},
        onDeclineInviteAndBlockUser = {},
        acceptDeclineInviteView = {},
        leaveRoomView = {}
    )
}

@Preview
@Composable
internal fun HomeViewA11yPreview() = ElementPreview {
    HomeView(
        homeState = aHomeState(),
        onRoomClick = { _, _ -> },
        onUserClick = {},
        onSettingsClick = {},
        onStartChatClick = {},
        onOpenContactsClick = {},
        onCreateSpaceClick = {},
        onRoomSettingsClick = {},
        onReportRoomClick = {},
        onMenuActionClick = {},
        onDeclineInviteAndBlockUser = {},
        acceptDeclineInviteView = {},
        leaveRoomView = {}
    )
}
