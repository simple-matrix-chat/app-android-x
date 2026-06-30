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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
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
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarHost
import io.element.android.libraries.designsystem.utils.snackbar.rememberSnackbarHostState
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.user.MatrixUser

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
        content = { padding ->
            val contentPadding = PaddingValues(
                bottom = 96.dp,
            )
            AnimatedContent(
                targetState = state.currentHomeNavigationBarItem,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    slideInHorizontally(
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> fullWidth * direction },
                    ) togetherWith slideOutHorizontally(
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                        targetOffsetX = { fullWidth -> -fullWidth * direction },
                    ) using SizeTransform(clip = true)
                },
                contentAlignment = Alignment.TopStart,
                label = "Home navigation content",
            ) { selectedItem ->
                when (selectedItem) {
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
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    )
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
