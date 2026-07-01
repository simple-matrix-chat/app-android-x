/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl

import android.os.Build
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import im.vector.app.features.analytics.plan.PinUnpinAction
import io.element.android.appconfig.MessageComposerConfig
import io.element.android.features.location.api.live.ActiveLiveLocationShareManager
import io.element.android.features.location.api.live.isCurrentlySharing
import io.element.android.features.messages.api.timeline.HtmlConverterProvider
import io.element.android.features.messages.impl.MessagesState.Threads
import io.element.android.features.messages.impl.actionlist.ActionListState
import io.element.android.features.messages.impl.actionlist.model.TimelineItemAction
import io.element.android.features.messages.impl.ai.MomentAIService
import io.element.android.features.messages.impl.link.LinkState
import io.element.android.features.messages.impl.messagecomposer.MessageComposerEvent
import io.element.android.features.messages.impl.messagecomposer.MessageComposerState
import io.element.android.features.messages.impl.pinned.banner.PinnedMessagesBannerState
import io.element.android.features.messages.impl.search.RoomMessageSearchEvent
import io.element.android.features.messages.impl.search.RoomMessageSearchResult
import io.element.android.features.messages.impl.search.RoomMessageSearchState
import io.element.android.features.messages.impl.timeline.MarkAsFullyRead
import io.element.android.features.messages.impl.timeline.TimelineController
import io.element.android.features.messages.impl.timeline.TimelineEvent
import io.element.android.features.messages.impl.timeline.TimelineState
import io.element.android.features.messages.impl.timeline.components.customreaction.CustomReactionState
import io.element.android.features.messages.impl.timeline.components.reactionsummary.ReactionSummaryState
import io.element.android.features.messages.impl.timeline.components.receipt.bottomsheet.ReadReceiptBottomSheetState
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.features.messages.impl.timeline.model.TimelineItemThreadInfo
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemEventContentWithAttachment
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemPollContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemStateContent
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemTextBasedContent
import io.element.android.features.messages.impl.timeline.protection.TimelineProtectionState
import io.element.android.features.messages.impl.voicemessages.composer.DefaultVoiceMessageComposerPresenter
import io.element.android.features.roomcall.api.RoomCallState
import io.element.android.features.roommembermoderation.api.RoomMemberModerationEvents
import io.element.android.features.roommembermoderation.api.RoomMemberModerationState
import io.element.android.libraries.androidutils.clipboard.ClipboardHelper
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.extensions.flatMap
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.core.meta.BuildMeta
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarMessage
import io.element.android.libraries.designsystem.utils.snackbar.collectSnackbarMessageAsState
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.featureflag.api.FeatureFlagService
import io.element.android.libraries.featureflag.api.FeatureFlags
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.toThreadId
import io.element.android.libraries.matrix.api.permalink.PermalinkParser
import io.element.android.libraries.matrix.api.room.JoinedRoom
import io.element.android.libraries.matrix.api.room.RoomInfo
import io.element.android.libraries.matrix.api.room.RoomMembersState
import io.element.android.libraries.matrix.api.room.powerlevels.permissionsAsState
import io.element.android.libraries.matrix.api.search.MatrixMessageSearchResult
import io.element.android.libraries.matrix.api.timeline.Timeline
import io.element.android.libraries.matrix.api.timeline.item.event.EventOrTransactionId
import io.element.android.libraries.matrix.ui.messages.reply.map
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.recentemojis.api.AddRecentEmoji
import io.element.android.libraries.textcomposer.model.MessageComposerMode
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.services.analytics.api.AnalyticsService
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

@AssistedInject
class MessagesPresenter(
    @Assisted private val navigator: MessagesNavigator,
    private val room: JoinedRoom,
    private val matrixClient: MatrixClient,
    @Assisted private val composerPresenter: Presenter<MessageComposerState>,
    voiceMessageComposerPresenterFactory: DefaultVoiceMessageComposerPresenter.Factory,
    @Assisted private val timelinePresenter: Presenter<TimelineState>,
    private val timelineProtectionPresenter: Presenter<TimelineProtectionState>,
    private val linkPresenter: Presenter<LinkState>,
    @Assisted private val actionListPresenter: Presenter<ActionListState>,
    private val customReactionPresenter: Presenter<CustomReactionState>,
    private val reactionSummaryPresenter: Presenter<ReactionSummaryState>,
    private val readReceiptBottomSheetPresenter: Presenter<ReadReceiptBottomSheetState>,
    private val pinnedMessagesBannerPresenter: Presenter<PinnedMessagesBannerState>,
    private val roomCallStatePresenter: Presenter<RoomCallState>,
    private val roomMemberModerationPresenter: Presenter<RoomMemberModerationState>,
    private val snackbarDispatcher: SnackbarDispatcher,
    private val dispatchers: CoroutineDispatchers,
    private val clipboardHelper: ClipboardHelper,
    private val htmlConverterProvider: HtmlConverterProvider,
    private val buildMeta: BuildMeta,
    @Assisted private val timelineController: TimelineController,
    private val permalinkParser: PermalinkParser,
    private val analyticsService: AnalyticsService,
    private val featureFlagService: FeatureFlagService,
    private val addRecentEmoji: AddRecentEmoji,
    private val markAsFullyRead: MarkAsFullyRead,
    private val liveLocationShareManager: ActiveLiveLocationShareManager,
    private val momentAIService: MomentAIService,
    @SessionCoroutineScope private val sessionCoroutineScope: CoroutineScope,
) : Presenter<MessagesState> {
    @AssistedFactory
    interface Factory {
        fun create(
            navigator: MessagesNavigator,
            composerPresenter: Presenter<MessageComposerState>,
            timelinePresenter: Presenter<TimelineState>,
            actionListPresenter: Presenter<ActionListState>,
            timelineController: TimelineController,
        ): MessagesPresenter
    }

    private val voiceMessageComposerPresenter = voiceMessageComposerPresenterFactory.create(
        timelineMode = timelineController.mainTimelineMode()
    )

    private val markingAsReadAndExiting = AtomicBoolean(false)

    @Composable
    override fun present(): MessagesState {
        htmlConverterProvider.Update()

        val coroutineScope = rememberCoroutineScope()
        val roomInfo by room.roomInfoFlow.collectAsState()
        val localCoroutineScope = rememberCoroutineScope()
        val composerState = composerPresenter.present()
        val voiceMessageComposerState = voiceMessageComposerPresenter.present()
        val timelineState = timelinePresenter.present()
        val timelineProtectionState = timelineProtectionPresenter.present()
        val actionListState = actionListPresenter.present()
        val linkState = linkPresenter.present()
        val customReactionState = customReactionPresenter.present()
        val reactionSummaryState = reactionSummaryPresenter.present()
        val readReceiptBottomSheetState = readReceiptBottomSheetPresenter.present()
        val pinnedMessagesBannerState = pinnedMessagesBannerPresenter.present()
        val roomCallState = roomCallStatePresenter.present()
        val roomMemberModerationState = roomMemberModerationPresenter.present()
        val threadsList by produceState(persistentListOf()) {
            room.threadsListService.subscribeToItemUpdates()
                .onStart { room.threadsListService.paginate() }
                .collectLatest { value = it.toImmutableList() }
        }

        val canOpenThreadList by featureFlagService.isFeatureEnabledFlow(FeatureFlags.RoomThreadList).collectAsState(initial = false)
        val isCurrentlySharingLiveLocationInRoom by remember { liveLocationShareManager.isCurrentlySharing(room.roomId) }.collectAsState()
        val roomMessageSearchQuery = rememberTextFieldState()
        var isRoomMessageSearchActive by remember { mutableStateOf(false) }
        var roomMessageSearchResults by remember { mutableStateOf<ImmutableList<RoomMessageSearchResult>>(persistentListOf()) }
        var isSearchingRoomMessages by remember { mutableStateOf(false) }
        var hasRoomMessageSearchError by remember { mutableStateOf(false) }
        var hasMoreRoomMessageSearchResults by remember { mutableStateOf(false) }
        var currentRoomMessageSearchQuery by remember { mutableStateOf("") }
        var nextRoomMessageSearchBatch by remember { mutableStateOf<String?>(null) }
        var roomMessageSearchGeneration by remember { mutableIntStateOf(0) }
        var aiBriefingState by remember { mutableStateOf(MomentAIBriefingState.Default) }

        val userEventPermissions by room.permissionsAsState(UserEventPermissions.DEFAULT) { perms ->
            perms.userEventPermissions()
        }

        val roomAvatar by remember {
            derivedStateOf { roomInfo.avatarData() }
        }
        val heroes by remember {
            derivedStateOf { roomInfo.heroes().toImmutableList() }
        }

        var hasDismissedInviteDialog by rememberSaveable {
            mutableStateOf(false)
        }
        LaunchedEffect(Unit) {
            // Remove the unread flag on entering but don't send read receipts
            // as those will be handled by the timeline.
            withContext(dispatchers.io) {
                room.setUnreadFlag(isUnread = false)
            }
        }

        val inviteProgress = remember { mutableStateOf<AsyncData<Unit>>(AsyncData.Uninitialized) }
        var showReinvitePrompt by remember { mutableStateOf(false) }
        val composerHasFocus by remember { derivedStateOf { composerState.textEditorState.hasFocus() } }
        LaunchedEffect(hasDismissedInviteDialog, composerHasFocus, roomInfo) {
            withContext(dispatchers.io) {
                val shouldOfferReinvite = !hasDismissedInviteDialog &&
                    composerHasFocus &&
                    roomInfo.isDm &&
                    roomInfo.activeMembersCount == 1L
                showReinvitePrompt = shouldOfferReinvite && room.getDirectRoomMember() != null
            }
        }

        val snackbarMessage by snackbarDispatcher.collectSnackbarMessageAsState()

        fun resetRoomMessageSearch(clearQuery: Boolean) {
            if (clearQuery) {
                roomMessageSearchQuery.clearText()
            }
            roomMessageSearchResults = persistentListOf()
            isSearchingRoomMessages = false
            hasRoomMessageSearchError = false
            hasMoreRoomMessageSearchResults = false
            currentRoomMessageSearchQuery = ""
            nextRoomMessageSearchBatch = null
            roomMessageSearchGeneration++
        }

        fun MatrixMessageSearchResult.toRoomMessageSearchResult(): RoomMessageSearchResult {
            val sender = senderDisplayName?.takeIf(String::isNotBlank) ?: senderId?.value
            val roomTitle = roomInfo.name ?: room.roomId.value
            return RoomMessageSearchResult(
                eventId = eventId,
                title = roomTitle,
                description = sender
                    ?.takeIf(String::isNotBlank)
                    ?.let { "$it: $message" }
                    ?: message,
            )
        }

        suspend fun searchRoomMessages(searchQuery: String) {
            val trimmedSearchQuery = searchQuery.trim()
            val generation = ++roomMessageSearchGeneration
            currentRoomMessageSearchQuery = trimmedSearchQuery
            nextRoomMessageSearchBatch = null

            if (trimmedSearchQuery.isEmpty()) {
                resetRoomMessageSearch(clearQuery = false)
                return
            }

            roomMessageSearchResults = persistentListOf()
            isSearchingRoomMessages = true
            hasMoreRoomMessageSearchResults = false
            hasRoomMessageSearchError = false

            val result = matrixClient.searchMessages(trimmedSearchQuery, roomId = room.roomId)
            if (generation != roomMessageSearchGeneration) return

            isSearchingRoomMessages = false
            result
                .onSuccess { page ->
                    nextRoomMessageSearchBatch = page.nextBatch
                    hasMoreRoomMessageSearchResults = page.nextBatch != null
                    hasRoomMessageSearchError = false
                    roomMessageSearchResults = page.results
                        .map { result -> result.toRoomMessageSearchResult() }
                        .toImmutableList()
                }
                .onFailure {
                    nextRoomMessageSearchBatch = null
                    hasMoreRoomMessageSearchResults = false
                    hasRoomMessageSearchError = true
                }
        }

        suspend fun loadMoreRoomMessages() {
            val nextBatch = nextRoomMessageSearchBatch ?: return
            if (isSearchingRoomMessages || currentRoomMessageSearchQuery.isEmpty()) return

            val generation = roomMessageSearchGeneration
            isSearchingRoomMessages = true
            hasRoomMessageSearchError = false

            val result = matrixClient.searchMessages(currentRoomMessageSearchQuery, nextBatch = nextBatch, roomId = room.roomId)
            if (generation != roomMessageSearchGeneration) return

            isSearchingRoomMessages = false
            result
                .onSuccess { page ->
                    nextRoomMessageSearchBatch = page.nextBatch
                    hasMoreRoomMessageSearchResults = page.nextBatch != null
                    hasRoomMessageSearchError = false
                    val existingResults = roomMessageSearchResults
                    val existingEventIds = existingResults.map { it.eventId }.toSet()
                    roomMessageSearchResults = (
                        existingResults + page.results
                            .filterNot { result -> existingEventIds.contains(result.eventId) }
                            .map { result -> result.toRoomMessageSearchResult() }
                        ).toImmutableList()
                }
                .onFailure {
                    nextRoomMessageSearchBatch = null
                    hasMoreRoomMessageSearchResults = false
                    hasRoomMessageSearchError = true
                }
        }

        LaunchedEffect(isRoomMessageSearchActive, roomMessageSearchQuery.text) {
            if (isRoomMessageSearchActive) {
                searchRoomMessages(roomMessageSearchQuery.text.toString())
            }
        }

        fun requestAIBriefing() {
            aiBriefingState = MomentAIBriefingState.Default.copy(
                isVisible = true,
                isLoading = true,
            )
            localCoroutineScope.launch {
                momentAIService.getRoomBriefing(room.roomId.value)
                    .onSuccess { briefing ->
                        aiBriefingState = aiBriefingState.copy(
                            isLoading = false,
                            briefing = briefing,
                            errorMessageResId = null,
                        )
                    }
                    .onFailure {
                        aiBriefingState = aiBriefingState.copy(
                            isLoading = false,
                            briefing = null,
                            errorMessageResId = R.string.screen_room_ai_briefing_error,
                        )
                    }
            }
        }

        fun handleRoomMessageSearchEvent(event: RoomMessageSearchEvent) {
            when (event) {
                RoomMessageSearchEvent.ClearQuery -> roomMessageSearchQuery.clearText()
                RoomMessageSearchEvent.ToggleSearchVisibility -> {
                    isRoomMessageSearchActive = !isRoomMessageSearchActive
                    resetRoomMessageSearch(clearQuery = true)
                }
                is RoomMessageSearchEvent.UpdateVisibleRange -> localCoroutineScope.launch {
                    if (event.range.last >= roomMessageSearchResults.size - 3) {
                        loadMoreRoomMessages()
                    }
                }
                is RoomMessageSearchEvent.SelectResult -> {
                    timelineState.eventSink(TimelineEvent.FocusOnEvent(event.eventId))
                    isRoomMessageSearchActive = false
                    resetRoomMessageSearch(clearQuery = true)
                }
            }
        }

        fun handleEvent(event: MessagesEvent) {
            when (event) {
                is MessagesEvent.HandleAction -> {
                    localCoroutineScope.handleTimelineAction(
                        action = event.action,
                        targetEvent = event.event,
                        composerState = composerState,
                        enableTextFormatting = composerState.showTextFormatting,
                        timelineState = timelineState,
                        timelineProtectionState = timelineProtectionState,
                    )
                }
                is MessagesEvent.ToggleReaction -> {
                    localCoroutineScope.toggleReaction(event.emoji, event.eventOrTransactionId)
                }
                is MessagesEvent.InviteDialogDismissed -> {
                    hasDismissedInviteDialog = true

                    if (event.action == InviteDialogAction.Invite) {
                        localCoroutineScope.reinviteOtherUser(inviteProgress)
                    }
                }
                is MessagesEvent.OnUserClicked -> {
                    roomMemberModerationState.eventSink(RoomMemberModerationEvents.ShowActionsForUser(event.user))
                }
                MessagesEvent.StopLiveLocationShare -> {
                    localCoroutineScope.launch {
                        liveLocationShareManager.stopShare(room.roomId)
                            .onFailure {
                                Timber.e(it, "Failed to stop live location share for roomId=${room.roomId}")
                                snackbarDispatcher.post(SnackbarMessage(CommonStrings.common_error))
                            }
                    }
                }
                MessagesEvent.ShowLiveLocationShare -> {
                    navigator.navigateToCurrentLiveLocation()
                }
                is MessagesEvent.MarkAsFullyReadAndExit -> if (!markingAsReadAndExiting.getAndSet(true)) {
                    coroutineScope.launch {
                        val latestEventId = room.liveTimeline.getLatestEventId().getOrElse {
                            Timber.w(it, "Failed to get latest event id to mark as fully read")
                            null
                        }
                        latestEventId?.let { eventId ->
                            sessionCoroutineScope.launch {
                                markAsFullyRead(room.roomId, eventId)
                            }
                        }
                        navigator.close()
                    }.invokeOnCompletion {
                        markingAsReadAndExiting.set(false)
                    }
                }
                MessagesEvent.OpenAIBriefing -> requestAIBriefing()
                MessagesEvent.DismissAIBriefing -> {
                    aiBriefingState = MomentAIBriefingState.Default
                }
                MessagesEvent.RetryAIBriefing -> requestAIBriefing()
            }
        }

        return MessagesState(
            roomId = room.roomId,
            roomName = roomInfo.name,
            roomAvatar = roomAvatar,
            heroes = heroes,
            userEventPermissions = userEventPermissions,
            composerState = composerState,
            voiceMessageComposerState = voiceMessageComposerState,
            timelineState = timelineState,
            timelineProtectionState = timelineProtectionState,
            linkState = linkState,
            actionListState = actionListState,
            customReactionState = customReactionState,
            reactionSummaryState = reactionSummaryState,
            readReceiptBottomSheetState = readReceiptBottomSheetState,
            snackbarMessage = snackbarMessage,
            inviteProgress = inviteProgress.value,
            showReinvitePrompt = showReinvitePrompt,
            enableTextFormatting = MessageComposerConfig.ENABLE_RICH_TEXT_EDITING,
            roomCallState = roomCallState,
            appName = buildMeta.applicationName,
            pinnedMessagesBannerState = pinnedMessagesBannerState,
            roomMessageSearchState = RoomMessageSearchState(
                isSearchEnabled = true,
                isSearchActive = isRoomMessageSearchActive,
                query = roomMessageSearchQuery,
                results = roomMessageSearchResults,
                isSearching = isSearchingRoomMessages,
                hasMoreResults = hasMoreRoomMessageSearchResults,
                hasSearchError = hasRoomMessageSearchError,
                eventSink = ::handleRoomMessageSearchEvent,
            ),
            aiBriefingState = aiBriefingState,
            roomMemberModerationState = roomMemberModerationState,
            successorRoom = roomInfo.successorRoom,
            threads = Threads(
                hasThreads = canOpenThreadList && threadsList.isNotEmpty(),
                // TODO calculate this properly based on the thread list and the read state of each thread
                hasUnreadThreads = false,
            ),
            showLiveLocationShareBanner = isCurrentlySharingLiveLocationInRoom && timelineState.timelineMode !is Timeline.Mode.Thread,
            eventSink = ::handleEvent,
        )
    }

    private fun RoomInfo.avatarData(): AvatarData {
        return AvatarData(
            id = id.value,
            name = name,
            url = avatarUrl,
            size = AvatarSize.TimelineRoom
        )
    }

    private fun RoomInfo.heroes(): List<AvatarData> {
        return heroes.map { user ->
            user.getAvatarData(size = AvatarSize.TimelineRoom)
        }
    }

    private fun CoroutineScope.handleTimelineAction(
        action: TimelineItemAction,
        targetEvent: TimelineItem.Event,
        composerState: MessageComposerState,
        timelineProtectionState: TimelineProtectionState,
        enableTextFormatting: Boolean,
        timelineState: TimelineState,
    ) = launch {
        when (action) {
            TimelineItemAction.CopyText -> handleCopyContents(targetEvent)
            TimelineItemAction.CopyCaption -> handleCopyCaption(targetEvent)
            TimelineItemAction.CopyLink -> handleCopyLink(targetEvent)
            TimelineItemAction.Redact -> handleActionRedact(targetEvent)
            TimelineItemAction.Edit,
            TimelineItemAction.EditPoll -> handleActionEdit(targetEvent, composerState, enableTextFormatting)
            TimelineItemAction.AddCaption -> handleActionAddCaption(targetEvent, composerState)
            TimelineItemAction.EditCaption -> handleActionEditCaption(targetEvent, composerState)
            TimelineItemAction.RemoveCaption -> handleRemoveCaption(targetEvent)
            TimelineItemAction.Reply -> handleActionReply(targetEvent, composerState, timelineProtectionState)
            TimelineItemAction.ReplyInThread -> {
                val displayThreads = featureFlagService.isFeatureEnabled(FeatureFlags.Threads)
                if (displayThreads) {
                    // Get either the thread id this event is in, or the event id if it's not in a thread so we can start one
                    val threadId = when (targetEvent.threadInfo) {
                        is TimelineItemThreadInfo.ThreadResponse -> targetEvent.threadInfo.threadRootId
                        is TimelineItemThreadInfo.ThreadRoot, null -> targetEvent.eventId?.toThreadId()
                    } ?: return@launch
                    navigator.navigateToThread(threadId, null)
                } else {
                    handleActionReply(targetEvent, composerState, timelineProtectionState)
                }
            }
            TimelineItemAction.ViewSource -> handleShowDebugInfoAction(targetEvent)
            TimelineItemAction.Forward -> handleForwardAction(targetEvent)
            TimelineItemAction.ReportContent -> handleReportAction(targetEvent)
            TimelineItemAction.EndPoll -> handleEndPollAction(targetEvent, timelineState)
            TimelineItemAction.Pin -> handlePinAction(targetEvent)
            TimelineItemAction.Unpin -> handleUnpinAction(targetEvent)
            TimelineItemAction.ViewInTimeline -> Unit
        }
    }

    private suspend fun handleRemoveCaption(targetEvent: TimelineItem.Event) {
        timelineController.invokeOnCurrentTimeline {
            editCaption(
                eventOrTransactionId = targetEvent.eventOrTransactionId,
                caption = null,
                formattedCaption = null,
            )
        }
    }

    private suspend fun handlePinAction(targetEvent: TimelineItem.Event) {
        if (targetEvent.eventId == null) return
        analyticsService.capture(
            PinUnpinAction(
                from = PinUnpinAction.From.Timeline,
                kind = PinUnpinAction.Kind.Pin,
            )
        )
        timelineController.invokeOnCurrentTimeline {
            pinEvent(targetEvent.eventId)
                .onFailure {
                    Timber.e(it, "Failed to pin event ${targetEvent.eventId}")
                    snackbarDispatcher.post(SnackbarMessage(CommonStrings.common_error))
                }
        }
    }

    private suspend fun handleUnpinAction(targetEvent: TimelineItem.Event) {
        if (targetEvent.eventId == null) return
        analyticsService.capture(
            PinUnpinAction(
                from = PinUnpinAction.From.Timeline,
                kind = PinUnpinAction.Kind.Unpin,
            )
        )
        timelineController.invokeOnCurrentTimeline {
            unpinEvent(targetEvent.eventId)
                .onFailure {
                    Timber.e(it, "Failed to unpin event ${targetEvent.eventId}")
                    snackbarDispatcher.post(SnackbarMessage(CommonStrings.common_error))
                }
        }
    }

    private fun CoroutineScope.toggleReaction(
        emoji: String,
        eventOrTransactionId: EventOrTransactionId,
    ) = launch(dispatchers.io) {
        timelineController.invokeOnCurrentTimeline {
            toggleReaction(emoji, eventOrTransactionId)
                .flatMap { added -> if (added) addRecentEmoji(emoji) else Result.success(Unit) }
                .onFailure { Timber.e(it) }
        }
    }

    private fun CoroutineScope.reinviteOtherUser(inviteProgress: MutableState<AsyncData<Unit>>) = launch(dispatchers.io) {
        inviteProgress.value = AsyncData.Loading()
        runCatchingExceptions {
            val memberList = when (val memberState = room.membersStateFlow.value) {
                is RoomMembersState.Ready -> memberState.roomMembers
                is RoomMembersState.Error -> memberState.prevRoomMembers.orEmpty()
                else -> emptyList()
            }

            val member = memberList.first { it.userId != room.sessionId }
            room.inviteUserById(member.userId).onFailure { t ->
                Timber.e(t, "Failed to reinvite DM partner")
            }.getOrThrow()
        }.fold(
            onSuccess = {
                inviteProgress.value = AsyncData.Success(Unit)
            },
            onFailure = {
                inviteProgress.value = AsyncData.Failure(it)
            }
        )
    }

    private suspend fun handleActionRedact(event: TimelineItem.Event) {
        timelineController.invokeOnCurrentTimeline {
            redactEvent(eventOrTransactionId = event.eventOrTransactionId, reason = null)
                .onFailure { Timber.e(it) }
        }
    }

    private fun handleActionEdit(
        targetEvent: TimelineItem.Event,
        composerState: MessageComposerState,
        enableTextFormatting: Boolean,
    ) {
        when (targetEvent.content) {
            is TimelineItemPollContent -> {
                if (targetEvent.eventId == null) return
                navigator.navigateToEditPoll(targetEvent.eventId)
            }
            else -> {
                val composerMode = MessageComposerMode.Edit(
                    targetEvent.eventOrTransactionId,
                    (targetEvent.content as? TimelineItemTextBasedContent)?.let {
                        if (enableTextFormatting) {
                            it.htmlBody ?: it.body
                        } else {
                            it.body
                        }
                    }.orEmpty(),
                )
                composerState.eventSink(
                    MessageComposerEvent.SetMode(composerMode)
                )
            }
        }
    }

    private suspend fun handleActionAddCaption(
        targetEvent: TimelineItem.Event,
        composerState: MessageComposerState,
    ) {
        val composerMode = MessageComposerMode.EditCaption(
            eventOrTransactionId = targetEvent.eventOrTransactionId,
            content = "",
        )
        composerState.eventSink(
            MessageComposerEvent.SetMode(composerMode)
        )
    }

    private suspend fun handleActionEditCaption(
        targetEvent: TimelineItem.Event,
        composerState: MessageComposerState,
    ) {
        val composerMode = MessageComposerMode.EditCaption(
            eventOrTransactionId = targetEvent.eventOrTransactionId,
            content = (targetEvent.content as? TimelineItemEventContentWithAttachment)?.caption.orEmpty(),
        )
        composerState.eventSink(
            MessageComposerEvent.SetMode(composerMode)
        )
    }

    private suspend fun handleActionReply(
        targetEvent: TimelineItem.Event,
        composerState: MessageComposerState,
        timelineProtectionState: TimelineProtectionState,
    ) {
        if (targetEvent.eventId == null) return
        timelineController.invokeOnCurrentTimeline {
            val replyToDetails = loadReplyDetails(targetEvent.eventId).map(permalinkParser)
            val composerMode = MessageComposerMode.Reply(
                replyToDetails = replyToDetails,
                hideImage = timelineProtectionState.hideMediaContent(targetEvent.eventId),
            )
            composerState.eventSink(
                MessageComposerEvent.SetMode(composerMode)
            )
        }
    }

    private fun handleShowDebugInfoAction(event: TimelineItem.Event) {
        navigator.navigateToEventDebugInfo(event.eventId, event.debugInfo)
    }

    private fun handleForwardAction(event: TimelineItem.Event) {
        if (event.eventId == null) return
        navigator.forwardEvent(event.eventId)
    }

    private fun handleReportAction(event: TimelineItem.Event) {
        if (event.eventId == null) return
        navigator.navigateToReportMessage(event.eventId, event.senderId)
    }

    private fun handleEndPollAction(
        event: TimelineItem.Event,
        timelineState: TimelineState,
    ) {
        event.eventId?.let { timelineState.eventSink(TimelineEvent.EndPoll(it)) }
    }

    private suspend fun handleCopyLink(event: TimelineItem.Event) {
        event.eventId ?: return
        room.getPermalinkFor(event.eventId).fold(
            onSuccess = { permalink ->
                clipboardHelper.copyPlainText(permalink)
                snackbarDispatcher.post(SnackbarMessage(CommonStrings.common_link_copied_to_clipboard))
            },
            onFailure = {
                Timber.e(it, "Failed to get permalink for event ${event.eventId}")
                snackbarDispatcher.post(SnackbarMessage(CommonStrings.common_error))
            }
        )
    }

    private fun handleCopyContents(event: TimelineItem.Event) {
        val content = when (event.content) {
            is TimelineItemTextBasedContent -> event.content.body
            is TimelineItemStateContent -> event.content.body
            else -> return
        }
        clipboardHelper.copyPlainText(content)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            snackbarDispatcher.post(SnackbarMessage(R.string.screen_room_timeline_message_copied))
        }
    }

    private fun handleCopyCaption(event: TimelineItem.Event) {
        val content = (event.content as? TimelineItemEventContentWithAttachment)?.caption ?: return
        clipboardHelper.copyPlainText(content)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            snackbarDispatcher.post(SnackbarMessage(CommonStrings.common_copied_to_clipboard))
        }
    }
}
