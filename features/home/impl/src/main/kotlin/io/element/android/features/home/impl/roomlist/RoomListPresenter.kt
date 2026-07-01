/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.roomlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import dev.zacsweers.metro.Inject
import im.vector.app.features.analytics.plan.Interaction
import io.element.android.features.announcement.api.Announcement
import io.element.android.features.announcement.api.AnnouncementService
import io.element.android.features.home.impl.datasource.RoomListDataSource
import io.element.android.features.home.impl.filters.MomentHomeMuteDuration
import io.element.android.features.home.impl.filters.MomentHomePreferencesStore
import io.element.android.features.home.impl.filters.MomentHomeRoomType
import io.element.android.features.home.impl.filters.MomentHomeRoomTypeService
import io.element.android.features.home.impl.filters.MomentMutedChatsStore
import io.element.android.features.home.impl.filters.RoomListFiltersEvent
import io.element.android.features.home.impl.filters.RoomListFiltersState
import io.element.android.features.home.impl.filters.matches
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.model.RoomSummaryDisplayType
import io.element.android.features.home.impl.search.RoomListSearchEvent
import io.element.android.features.home.impl.search.RoomListSearchState
import io.element.android.features.home.impl.spacefilters.SpaceFiltersState
import io.element.android.features.home.impl.spacefilters.into
import io.element.android.features.home.impl.spacefilters.selectedFilter
import io.element.android.features.invite.api.SeenInvitesStore
import io.element.android.features.invite.api.acceptdecline.AcceptDeclineInviteEvents.AcceptInvite
import io.element.android.features.invite.api.acceptdecline.AcceptDeclineInviteEvents.DeclineInvite
import io.element.android.features.invite.api.acceptdecline.AcceptDeclineInviteState
import io.element.android.features.leaveroom.api.LeaveRoomEvent
import io.element.android.features.leaveroom.api.LeaveRoomState
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.fullscreenintent.api.FullScreenIntentPermissionsState
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.matrix.api.room.getBestName
import io.element.android.libraries.matrix.api.roomlist.RoomList
import io.element.android.libraries.matrix.api.roomlist.RoomListFilter
import io.element.android.libraries.matrix.api.timeline.ReceiptType
import io.element.android.libraries.matrix.ui.safety.rememberHideInvitesAvatar
import io.element.android.libraries.preferences.api.store.AppPreferencesStore
import io.element.android.libraries.preferences.api.store.SessionPreferencesStore
import io.element.android.libraries.push.api.battery.BatteryOptimizationState
import io.element.android.libraries.push.api.notifications.NotificationCleaner
import io.element.android.services.analytics.api.AnalyticsService
import io.element.android.services.analytics.api.watchers.AnalyticsColdStartWatcher
import io.element.android.services.analyticsproviders.api.trackers.captureInteraction
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import io.element.android.features.home.impl.filters.RoomListFilter as HomeRoomListFilter

@Inject
class RoomListPresenter(
    private val client: MatrixClient,
    private val leaveRoomPresenter: Presenter<LeaveRoomState>,
    private val roomListDataSource: RoomListDataSource,
    private val filtersPresenter: Presenter<RoomListFiltersState>,
    private val searchPresenter: Presenter<RoomListSearchState>,
    private val sessionPreferencesStore: SessionPreferencesStore,
    private val analyticsService: AnalyticsService,
    private val acceptDeclineInvitePresenter: Presenter<AcceptDeclineInviteState>,
    private val fullScreenIntentPermissionsPresenter: Presenter<FullScreenIntentPermissionsState>,
    private val batteryOptimizationPresenter: Presenter<BatteryOptimizationState>,
    private val notificationCleaner: NotificationCleaner,
    private val appPreferencesStore: AppPreferencesStore,
    private val seenInvitesStore: SeenInvitesStore,
    private val announcementService: AnnouncementService,
    private val coldStartWatcher: AnalyticsColdStartWatcher,
    private val spaceFiltersPresenter: Presenter<SpaceFiltersState>,
    private val momentHomeRoomTypeService: MomentHomeRoomTypeService,
    private val momentHomePreferencesStore: MomentHomePreferencesStore,
    private val momentMutedChatsStore: MomentMutedChatsStore,
) : Presenter<RoomListState> {
    @Composable
    override fun present(): RoomListState {
        val coroutineScope = rememberCoroutineScope()
        val leaveRoomState = leaveRoomPresenter.present()
        val filtersState = filtersPresenter.present()
        val searchState = searchPresenter.present()
        val spaceFiltersState = spaceFiltersPresenter.present()
        val acceptDeclineInviteState = acceptDeclineInvitePresenter.present()

        LaunchedEffect(Unit) {
            roomListDataSource.launchIn(this)
        }

        val showNewNotificationSoundBanner by remember {
            announcementService.announcementsToShowFlow().map { announcements ->
                announcements.contains(Announcement.NewNotificationSound)
            }
        }.collectAsState(false)

        // Avatar indicator
        val hideInvitesAvatar by client.rememberHideInvitesAvatar()

        val contextMenu = remember { mutableStateOf<RoomListState.ContextMenu>(RoomListState.ContextMenu.Hidden) }
        val declineInviteMenu = remember { mutableStateOf<RoomListState.DeclineInviteMenu>(RoomListState.DeclineInviteMenu.Hidden) }
        val directUserBlockConfirmation = remember {
            mutableStateOf<RoomListState.DirectUserBlockConfirmation>(RoomListState.DirectUserBlockConfirmation.Hidden)
        }

        fun handleEvent(event: RoomListEvent) {
            when (event) {
                is RoomListEvent.UpdateVisibleRange -> coroutineScope.launch {
                    roomListDataSource.updateVisibleRange(event.range)
                }
                RoomListEvent.DismissNewNotificationSoundBanner -> coroutineScope.launch {
                    announcementService.onAnnouncementDismissed(Announcement.NewNotificationSound)
                }
                RoomListEvent.ToggleSearchResults -> searchState.eventSink(RoomListSearchEvent.ToggleSearchVisibility)
                is RoomListEvent.ShowContextMenu -> {
                    coroutineScope.showContextMenu(event, contextMenu)
                }
                is RoomListEvent.HideContextMenu -> {
                    contextMenu.value = RoomListState.ContextMenu.Hidden
                }
                is RoomListEvent.LeaveRoom -> {
                    coroutineScope.leaveRoom(event, leaveRoomState)
                }
                is RoomListEvent.SetRoomIsFavorite -> coroutineScope.setRoomIsFavorite(event.roomId, event.isFavorite)
                is RoomListEvent.SetRoomIsArchived -> coroutineScope.launch {
                    momentHomePreferencesStore.setRoomArchived(event.roomId, event.isArchived)
                }
                is RoomListEvent.SetRoomMuteDuration -> coroutineScope.muteRoom(event.roomId, event.duration)
                is RoomListEvent.UnmuteRoom -> coroutineScope.unmuteRoom(event.roomId)
                is RoomListEvent.MarkAsRead -> coroutineScope.markAsRead(event.roomId)
                is RoomListEvent.MarkAsUnread -> coroutineScope.markAsUnread(event.roomId)
                is RoomListEvent.AcceptInvite -> {
                    acceptDeclineInviteState.eventSink(
                        AcceptInvite(event.roomSummary.toInviteData())
                    )
                }
                is RoomListEvent.DeclineInvite -> {
                    acceptDeclineInviteState.eventSink(
                        DeclineInvite(event.roomSummary.toInviteData(), blockUser = event.blockUser, shouldConfirm = false)
                    )
                }
                is RoomListEvent.ShowDeclineInviteMenu -> declineInviteMenu.value = RoomListState.DeclineInviteMenu.Shown(event.roomSummary)
                RoomListEvent.HideDeclineInviteMenu -> declineInviteMenu.value = RoomListState.DeclineInviteMenu.Hidden
                is RoomListEvent.ShowDirectUserBlockConfirmation -> {
                    directUserBlockConfirmation.value = RoomListState.DirectUserBlockConfirmation.Shown(
                        userId = event.userId,
                        displayName = event.displayName,
                        blocked = event.blocked,
                    )
                }
                RoomListEvent.HideDirectUserBlockConfirmation -> {
                    directUserBlockConfirmation.value = RoomListState.DirectUserBlockConfirmation.Hidden
                }
                is RoomListEvent.SetDirectUserBlocked -> {
                    directUserBlockConfirmation.value = RoomListState.DirectUserBlockConfirmation.Hidden
                    coroutineScope.setDirectUserBlocked(event.userId, event.blocked)
                }
                is RoomListEvent.ClearCacheOfRoom -> coroutineScope.clearCacheOfRoom(event.roomId)
            }
        }

        LaunchedEffect(spaceFiltersState.selectedFilter()) {
            val selectedSpaceFilter = spaceFiltersState.selectedFilter().into()
            val allFilters = RoomListFilter.All(listOfNotNull(selectedSpaceFilter))
            roomListDataSource.updateFilter(allFilters)
        }

        val roomListPresentationContent = roomListContentState(
            showNewNotificationSoundBanner,
            filtersState,
        )

        val canReportRoom by produceState(false) { value = client.canReportRoom() }

        return RoomListState(
            contextMenu = contextMenu.value,
            declineInviteMenu = declineInviteMenu.value,
            directUserBlockConfirmation = directUserBlockConfirmation.value,
            leaveRoomState = leaveRoomState,
            filtersState = roomListPresentationContent.filtersState,
            searchState = searchState,
            spaceFiltersState = spaceFiltersState,
            contentState = roomListPresentationContent.contentState,
            acceptDeclineInviteState = acceptDeclineInviteState,
            hideInvitesAvatars = hideInvitesAvatar,
            canReportRoom = canReportRoom,
            eventSink = ::handleEvent,
        )
    }

    @Composable
    private fun roomListContentState(
        showNewNotificationSoundBanner: Boolean,
        filtersState: RoomListFiltersState,
    ): RoomListPresentationContent {
        val roomSummaries by produceState(initialValue = AsyncData.Loading()) {
            roomListDataSource.roomSummariesFlow.collect { value = AsyncData.Success(it) }
        }
        val roomTypes by momentHomeRoomTypeService.roomTypes.collectAsState()
        val archivedRoomIds by momentHomePreferencesStore.archivedRoomIds.collectAsState()
        val finiteMutedRoomIds by momentMutedChatsStore.finiteMutedRoomIds.collectAsState()
        val ignoredUserIds by client.ignoredUsersFlow.collectAsState()
        val loadingState by roomListDataSource.loadingState.collectAsState()
        val showEmpty by remember {
            derivedStateOf {
                (loadingState as? RoomList.LoadingState.Loaded)?.numberOfRooms == 0
            }
        }
        val showSkeleton by remember {
            derivedStateOf {
                loadingState == RoomList.LoadingState.NotLoaded || roomSummaries is AsyncData.Loading
            }
        }
        val seenRoomInvites by remember { seenInvitesStore.seenRoomIds() }.collectAsState(emptySet())
        val securityBannerState = SecurityBannerState.None
        return when {
            showEmpty -> RoomListPresentationContent(
                contentState = RoomListContentState.Empty(
                    securityBannerState = securityBannerState,
                ),
                filtersState = filtersState.withArchivedFilterVisibility(visible = false),
            )
            showSkeleton -> RoomListPresentationContent(
                contentState = RoomListContentState.Skeleton(count = 16),
                filtersState = filtersState.withArchivedFilterVisibility(visible = false),
            )
            else -> {
                coldStartWatcher.onRoomListVisible()
                val summaries = roomSummaries.dataOrNull().orEmpty().map { summary ->
                    val roomType = if (summary.isDirect) {
                        MomentHomeRoomType.Direct
                    } else {
                        roomTypes[summary.roomId] ?: summary.momentHomeRoomType
                    }
                    summary.copy(
                        momentHomeRoomType = roomType,
                        isArchived = archivedRoomIds.contains(summary.roomId),
                        isMuted = summary.userDefinedNotificationMode == RoomNotificationMode.MUTE || finiteMutedRoomIds.contains(summary.roomId),
                        isDirectUserBlocked = summary.directUserId?.let { ignoredUserIds.contains(it) } == true,
                    )
                }
                val hasArchivedRooms = summaries.any { summary ->
                    summary.displayType == RoomSummaryDisplayType.ROOM && summary.isArchived
                }
                LaunchedEffect(summaries) {
                    momentHomeRoomTypeService.resolveRoomTypes(summaries)
                }
                val selectedFilter = filtersState.selectedFilter().takeUnless {
                    it == HomeRoomListFilter.Archived && !hasArchivedRooms
                }
                LaunchedEffect(hasArchivedRooms, filtersState.selectedFilter()) {
                    if (!hasArchivedRooms && filtersState.selectedFilter() == HomeRoomListFilter.Archived) {
                        filtersState.eventSink(RoomListFiltersEvent.ClearSelectedFilters)
                    }
                }

                RoomListPresentationContent(
                    contentState = RoomListContentState.Rooms(
                        securityBannerState = securityBannerState,
                        showNewNotificationSoundBanner = showNewNotificationSoundBanner,
                        fullScreenIntentPermissionsState = fullScreenIntentPermissionsPresenter.present(),
                        batteryOptimizationState = batteryOptimizationPresenter.present(),
                        summaries = summaries
                            .filter { it.matches(selectedFilter) }
                            .favoriteRoomsFirst()
                            .toImmutableList(),
                        seenRoomInvites = seenRoomInvites.toImmutableSet(),
                    ),
                    filtersState = filtersState.withArchivedFilterVisibility(visible = hasArchivedRooms),
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun CoroutineScope.showContextMenu(event: RoomListEvent.ShowContextMenu, contextMenuState: MutableState<RoomListState.ContextMenu>) = launch {
        val initialState = RoomListState.ContextMenu.Shown(
            roomId = event.roomSummary.roomId,
            roomName = event.roomSummary.name,
            isDm = event.roomSummary.isDm,
            isFavorite = event.roomSummary.isFavorite,
            isArchived = event.roomSummary.isArchived,
            isMuted = event.roomSummary.isMuted,
            isEncrypted = event.roomSummary.isEncrypted,
            isOneToOne = event.roomSummary.isOneToOne,
            directUserId = event.roomSummary.directUserId,
            directUserDisplayName = event.roomSummary.directUserDisplayName,
            isDirectUserBlocked = event.roomSummary.isDirectUserBlocked,
            hasNewContent = event.roomSummary.hasNewContent,
            canLeaveRoom = event.roomSummary.canLeaveRoom(client.sessionId),
            displayClearRoomCacheAction = appPreferencesStore.isDeveloperModeEnabledFlow().first(),
        )
        contextMenuState.value = initialState

        client.getRoom(event.roomSummary.roomId)?.use { room ->
            val roomInfo = room.roomInfoFlow.value
            val directRoomMember = if (event.roomSummary.isDirect || room.isDm()) {
                room.getDirectRoomMember()
            } else {
                null
            }
            val directUserId = event.roomSummary.directUserId ?: directRoomMember?.userId
            val directUserDisplayName = event.roomSummary.directUserDisplayName ?: directRoomMember?.getBestName()
            val baseState = initialState.copy(
                directUserId = directUserId,
                directUserDisplayName = directUserDisplayName,
                isDirectUserBlocked = directUserId?.let { client.ignoredUsersFlow.value.contains(it) } == true,
                canLeaveRoom = !roomInfo.isSelfDirectRoom(),
            )
            contextMenuState.value = baseState

            val isShowingContextMenuFlow = snapshotFlow { contextMenuState.value is RoomListState.ContextMenu.Shown }
                .distinctUntilChanged()

            val isFavoriteFlow = room.roomInfoFlow
                .map { it.isFavorite }
                .distinctUntilChanged()
            val isDirectUserBlockedFlow = directUserId?.let { userId ->
                client.ignoredUsersFlow
                    .map { ignoredUserIds -> ignoredUserIds.contains(userId) }
                    .distinctUntilChanged()
            } ?: flowOf(false)

            combine(isFavoriteFlow, isDirectUserBlockedFlow, isShowingContextMenuFlow) { isFavorite, isDirectUserBlocked, isShowingContextMenu ->
                Triple(isFavorite, isDirectUserBlocked, isShowingContextMenu)
            }
                .takeWhile { (_, _, isShowingContextMenu) -> isShowingContextMenu }
                .onEach { (isFavorite, isDirectUserBlocked, _) ->
                    contextMenuState.value = baseState.copy(
                        isFavorite = isFavorite,
                        isDirectUserBlocked = isDirectUserBlocked,
                    )
                }
                .collect()
        }
    }

    private fun CoroutineScope.setRoomIsFavorite(roomId: RoomId, isFavorite: Boolean) = launch {
        client.getRoom(roomId)?.use { room ->
            room.setIsFavorite(isFavorite)
                .onSuccess {
                    analyticsService.captureInteraction(name = Interaction.Name.MobileRoomListRoomContextMenuFavouriteToggle)
                }
        }
    }

    private fun CoroutineScope.leaveRoom(event: RoomListEvent.LeaveRoom, leaveRoomState: LeaveRoomState) = launch {
        client.getRoom(event.roomId)?.use { room ->
            if (room.roomInfoFlow.value.isSelfDirectRoom()) {
                return@launch
            }
        }
        leaveRoomState.eventSink(LeaveRoomEvent.LeaveRoom(event.roomId, needsConfirmation = event.needsConfirmation))
    }

    private fun CoroutineScope.muteRoom(roomId: RoomId, duration: MomentHomeMuteDuration) = launch {
        client.getRoom(roomId)?.use { room ->
            val roomInfo = room.roomInfoFlow.value
            momentMutedChatsStore.muteRoom(
                roomId = roomId,
                duration = duration,
                isEncrypted = roomInfo.isEncrypted == true,
                isOneToOne = roomInfo.isDm,
            )
        }
    }

    private fun CoroutineScope.unmuteRoom(roomId: RoomId) = launch {
        client.getRoom(roomId)?.use { room ->
            val roomInfo = room.roomInfoFlow.value
            momentMutedChatsStore.unmuteRoom(
                roomId = roomId,
                isEncrypted = roomInfo.isEncrypted == true,
                isOneToOne = roomInfo.isDm,
            )
        }
    }

    private fun CoroutineScope.setDirectUserBlocked(userId: UserId, blocked: Boolean) = launch {
        if (blocked) {
            client.ignoreUser(userId)
        } else {
            client.unignoreUser(userId)
        }
    }

    private fun CoroutineScope.markAsRead(roomId: RoomId) = launch {
        notificationCleaner.clearMessagesForRoom(client.sessionId, roomId)
        client.getRoom(roomId)?.use { room ->
            room.setUnreadFlag(isUnread = false)
            val receiptType = if (sessionPreferencesStore.isSendPublicReadReceiptsEnabled().first()) {
                ReceiptType.READ
            } else {
                ReceiptType.READ_PRIVATE
            }
            room.markAsRead(receiptType)
                .onSuccess {
                    analyticsService.captureInteraction(name = Interaction.Name.MobileRoomListRoomContextMenuUnreadToggle)
                }
        }
    }

    private fun CoroutineScope.markAsUnread(roomId: RoomId) = launch {
        client.getRoom(roomId)?.use { room ->
            room.setUnreadFlag(isUnread = true)
                .onSuccess {
                    analyticsService.captureInteraction(name = Interaction.Name.MobileRoomListRoomContextMenuUnreadToggle)
                }
        }
    }

    private fun CoroutineScope.clearCacheOfRoom(roomId: RoomId) = launch {
        client.getRoom(roomId)?.use { room ->
            room.clearEventCacheStorage()
        }
    }
}

private data class RoomListPresentationContent(
    val contentState: RoomListContentState,
    val filtersState: RoomListFiltersState,
)

private fun List<RoomListRoomSummary>.favoriteRoomsFirst(): List<RoomListRoomSummary> {
    return withIndex()
        .sortedWith(
            compareBy<IndexedValue<RoomListRoomSummary>> { if (it.value.isFavorite) 0 else 1 }
                .thenBy { it.index }
        )
        .map { it.value }
}

private fun RoomListRoomSummary.canLeaveRoom(sessionId: UserId): Boolean {
    return !isDirect || directUserId?.let { it != sessionId } == true
}

private fun io.element.android.libraries.matrix.api.room.RoomInfo.isSelfDirectRoom(): Boolean {
    return isDirect && activeMembersCount <= 1
}
