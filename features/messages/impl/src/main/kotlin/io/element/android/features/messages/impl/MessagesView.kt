/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.location.api.LiveLocationSharingBanner
import io.element.android.features.messages.api.timeline.voicemessages.composer.VoiceMessageComposerEvent
import io.element.android.features.messages.impl.actionlist.ActionListEvent
import io.element.android.features.messages.impl.actionlist.ActionListView
import io.element.android.features.messages.impl.actionlist.model.TimelineItemAction
import io.element.android.features.messages.impl.ai.MomentAIBriefingBottomSheet
import io.element.android.features.messages.impl.link.LinkEvent
import io.element.android.features.messages.impl.link.LinkView
import io.element.android.features.messages.impl.messagecomposer.AttachmentsBottomSheet
import io.element.android.features.messages.impl.messagecomposer.ComposerEmojiPickerBottomSheet
import io.element.android.features.messages.impl.messagecomposer.ContactAttachmentPickerBottomSheet
import io.element.android.features.messages.impl.messagecomposer.MessageComposerEvent
import io.element.android.features.messages.impl.messagecomposer.MessageComposerView
import io.element.android.features.messages.impl.messagecomposer.suggestions.SuggestionsPickerView
import io.element.android.features.messages.impl.pinned.banner.PinnedMessagesBannerState
import io.element.android.features.messages.impl.pinned.banner.PinnedMessagesBannerView
import io.element.android.features.messages.impl.pinned.banner.PinnedMessagesBannerViewDefaults
import io.element.android.features.messages.impl.search.RoomMessageSearchEvent
import io.element.android.features.messages.impl.search.RoomMessageSearchResult
import io.element.android.features.messages.impl.search.RoomMessageSearchState
import io.element.android.features.messages.impl.timeline.FOCUS_ON_PINNED_EVENT_DEBOUNCE_DURATION_IN_MILLIS
import io.element.android.features.messages.impl.timeline.TimelineEvent
import io.element.android.features.messages.impl.timeline.TimelineView
import io.element.android.features.messages.impl.timeline.aGroupedEvents
import io.element.android.features.messages.impl.timeline.aTimelineItemDaySeparator
import io.element.android.features.messages.impl.timeline.aTimelineItemEvent
import io.element.android.features.messages.impl.timeline.aTimelineState
import io.element.android.features.messages.impl.timeline.components.CallMenuItem
import io.element.android.features.messages.impl.timeline.components.customreaction.CustomReactionBottomSheet
import io.element.android.features.messages.impl.timeline.components.customreaction.CustomReactionEvent
import io.element.android.features.messages.impl.timeline.components.reactionsummary.ReactionSummaryEvent
import io.element.android.features.messages.impl.timeline.components.reactionsummary.ReactionSummaryView
import io.element.android.features.messages.impl.timeline.components.receipt.bottomsheet.ReadReceiptBottomSheet
import io.element.android.features.messages.impl.timeline.components.receipt.bottomsheet.ReadReceiptBottomSheetEvent
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.features.messages.impl.timeline.model.TimelineItemGroupPosition
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemStateEventContent
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemTextContent
import io.element.android.features.messages.impl.topbars.MessagesViewTopBar
import io.element.android.features.messages.impl.topbars.MomentRoomHeaderActionSize
import io.element.android.features.messages.impl.topbars.ThreadTopBar
import io.element.android.features.messages.impl.voicemessages.composer.VoiceMessagePermissionRationaleDialog
import io.element.android.features.messages.impl.voicemessages.composer.VoiceMessageSendingFailedDialog
import io.element.android.features.roomcall.api.RoomCallState
import io.element.android.libraries.androidutils.ui.hideKeyboard
import io.element.android.libraries.designsystem.atomic.molecules.ComposerAlertMolecule
import io.element.android.libraries.designsystem.components.ExpandableBottomSheetLayout
import io.element.android.libraries.designsystem.components.ExpandableBottomSheetLayoutState
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.libraries.designsystem.components.rememberExpandableBottomSheetLayoutState
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.text.toAnnotatedString
import io.element.android.libraries.designsystem.text.toDp
import io.element.android.libraries.designsystem.theme.components.BottomSheetDragHandle
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.SearchField
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.HideKeyboardWhenDisposed
import io.element.android.libraries.designsystem.utils.KeepScreenOn
import io.element.android.libraries.designsystem.utils.OnLifecycleEvent
import io.element.android.libraries.designsystem.utils.OnVisibleRangeChangeEffect
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarHost
import io.element.android.libraries.designsystem.utils.snackbar.rememberSnackbarHostState
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.tombstone.SuccessorRoom
import io.element.android.libraries.matrix.api.timeline.Timeline
import io.element.android.libraries.matrix.api.timeline.item.event.LocalEventSendState
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.textcomposer.model.VoiceMessageRecorderEvent
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.wysiwyg.link.Link
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MessagesView(
    state: MessagesState,
    onBackClick: () -> Unit,
    onRoomDetailsClick: () -> Unit,
    onEventContentClick: (isLive: Boolean, event: TimelineItem.Event) -> Boolean,
    onUserDataClick: (UserId) -> Unit,
    onLinkClick: (String, Boolean) -> Unit,
    onSendLocationClick: () -> Unit,
    onCreatePollClick: () -> Unit,
    onJoinCallClick: (isAudioCall: Boolean) -> Unit,
    onViewAllPinnedMessagesClick: () -> Unit,
    onThreadsListClick: () -> Unit,
    modifier: Modifier = Modifier,
    forceJumpToBottomVisibility: Boolean = false,
    knockRequestsBannerView: @Composable () -> Unit,
) {
    OnLifecycleEvent { _, event ->
        state.voiceMessageComposerState.eventSink(VoiceMessageComposerEvent.LifecycleEvent(event))
    }

    KeepScreenOn(state.voiceMessageComposerState.keepScreenOn)

    HideKeyboardWhenDisposed()

    val snackbarHostState = rememberSnackbarHostState(snackbarMessage = state.snackbarMessage)

    var maxComposerHeightPx by remember { mutableIntStateOf(120) }

    // This is needed because the composer is inside an AndroidView that can't be affected by the FocusManager in Compose
    val localView = LocalView.current

    fun hidingKeyboard(block: () -> Unit) {
        localView.hideKeyboard()
        block()
    }

    fun onContentClick(event: TimelineItem.Event) {
        Timber.v("onMessageClick= ${event.id}")
        val hideKeyboard = onEventContentClick(state.timelineState.isLive, event)
        if (hideKeyboard) {
            localView.hideKeyboard()
        }
    }

    fun onMessageLongClick(event: TimelineItem.Event) {
        Timber.v("OnMessageLongClicked= ${event.id}")
        hidingKeyboard {
            state.actionListState.eventSink(
                ActionListEvent.ComputeForMessage(
                    event = event,
                    userEventPermissions = state.userEventPermissions,
                )
            )
        }
    }

    fun onActionSelected(action: TimelineItemAction, event: TimelineItem.Event) {
        state.eventSink(MessagesEvent.HandleAction(action, event))
    }

    fun onEmojiReactionClick(emoji: String, event: TimelineItem.Event) {
        state.eventSink(MessagesEvent.ToggleReaction(emoji, event.eventOrTransactionId))
    }

    fun onEmojiReactionLongClick(emoji: String, event: TimelineItem.Event) {
        if (event.eventId == null) return
        state.reactionSummaryState.eventSink(ReactionSummaryEvent.ShowReactionSummary(event.eventId, event.reactionsState.reactions, emoji))
    }

    fun onMoreReactionsClick(event: TimelineItem.Event) {
        state.customReactionState.eventSink(CustomReactionEvent.ShowCustomReactionSheet(event))
    }

    val expandableState = rememberExpandableBottomSheetLayoutState()
    ExpandableBottomSheetLayout(
        modifier = modifier
                .fillMaxSize()
                .imePadding()
                .systemBarsPadding()
                .onSizeChanged { size ->
                    // Let the composer takes at max half of the available height.
                    // The value will be different if the soft keyboard is displayed
                    // or not.
                    maxComposerHeightPx = (size.height * 0.5f).toInt()
                },
        content = {
            Scaffold(
                contentWindowInsets = WindowInsets.statusBars,
                topBar = {
                    if (state.timelineState.timelineMode is Timeline.Mode.Thread) {
                        ThreadTopBar(
                            roomName = state.roomName,
                            roomAvatarData = state.roomAvatar,
                            heroes = state.heroes,
                            isTombstoned = state.isTombstoned,
                            onBackClick = onBackClick,
                        )
                    } else {
                        MessagesViewTopBar(
                            roomName = state.roomName,
                            onBackClick = { hidingKeyboard { onBackClick() } },
                            onRoomDetailsClick = { hidingKeyboard { onRoomDetailsClick() } },
                            onAIBriefingClick = {
                                hidingKeyboard {
                                    state.eventSink(MessagesEvent.OpenAIBriefing)
                                }
                            },
                            menuActions = {
                                MessagesMenuActions(
                                    displayThreads = state.timelineState.timelineMode !is Timeline.Mode.Thread && state.threads.hasThreads,
                                    displaySearch = state.roomMessageSearchState.isSearchEnabled,
                                    roomCallState = state.roomCallState,
                                    onSearchClick = {
                                        hidingKeyboard {
                                            state.roomMessageSearchState.eventSink(RoomMessageSearchEvent.ToggleSearchVisibility)
                                        }
                                    },
                                    onJoinCallClick = onJoinCallClick,
                                    onThreadsListClick = onThreadsListClick
                                )
                            }
                        )
                    }
                },
                content = { padding ->
                    Box(
                        modifier = Modifier
                                .padding(padding)
                                .consumeWindowInsets(padding)
                    ) {
                        MessagesViewContent(
                            state = state,
                            onContentClick = ::onContentClick,
                            onMessageLongClick = ::onMessageLongClick,
                            onUserDataClick = {
                                hidingKeyboard {
                                    state.eventSink(MessagesEvent.OnUserClicked(it))
                                }
                            },
                            onLinkClick = { link, customTab ->
                                if (customTab) {
                                    onLinkClick(link.url, true)
                                    // Do not check those links, they are internal link only
                                } else {
                                    state.linkState.eventSink(LinkEvent.OnLinkClick(link))
                                }
                            },
                            onReactionClick = ::onEmojiReactionClick,
                            onReactionLongClick = ::onEmojiReactionLongClick,
                            onMoreReactionsClick = ::onMoreReactionsClick,
                            onReadReceiptClick = { event ->
                                state.readReceiptBottomSheetState.eventSink(ReadReceiptBottomSheetEvent.EventSelected(event))
                            },
                            onSendLocationClick = onSendLocationClick,
                            onCreatePollClick = onCreatePollClick,
                            onSwipeToReply = { targetEvent ->
                                state.eventSink(MessagesEvent.HandleAction(TimelineItemAction.Reply, targetEvent))
                            },
                            forceJumpToBottomVisibility = forceJumpToBottomVisibility,
                            onViewAllPinnedMessagesClick = onViewAllPinnedMessagesClick,
                            knockRequestsBannerView = knockRequestsBannerView,
                        )

                        SuggestionsPickerView(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .heightIn(max = 230.dp),
                            roomId = state.roomId,
                            roomName = state.roomName,
                            roomAvatarData = state.roomAvatar,
                            suggestions = state.composerState.suggestions,
                            onSelectSuggestion = {
                                state.composerState.eventSink(MessageComposerEvent.InsertSuggestion(it))
                            }
                        )
                    }
                },
                snackbarHost = {
                    SnackbarHost(
                        snackbarHostState,
                        modifier = Modifier.navigationBarsPadding()
                    )
                },
            )
        },
        bottomSheetContent = {
            MessagesViewComposerBottomSheetContents(
                state = state,
                onLinkClick = { url, customTab -> onLinkClick(url, customTab) },
                onRoomSuccessorClick = { roomId ->
                    state.timelineState.eventSink(TimelineEvent.NavigateToPredecessorOrSuccessorRoom(roomId = roomId))
                },
            )
        },
        sheetDragHandle = @Composable { toggleAction ->
            if (state.composerState.showTextFormatting) {
                val expandA11yLabel = stringResource(CommonStrings.a11y_expand_message_text_field)
                val collapseA11yLabel = stringResource(CommonStrings.a11y_collapse_message_text_field)
                BottomSheetDragHandle(
                    modifier = Modifier.semantics {
                        role = Role.Button
                        // Accessibility action to toggle the bottom sheet state
                        val label = when (expandableState.position) {
                            ExpandableBottomSheetLayoutState.Position.COLLAPSED, ExpandableBottomSheetLayoutState.Position.DRAGGING -> expandA11yLabel
                            ExpandableBottomSheetLayoutState.Position.EXPANDED -> collapseA11yLabel
                        }
                        onClick(label) {
                            toggleAction()
                            true
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    // Ensure that the bottom sheet is collapsed
                    if (expandableState.position == ExpandableBottomSheetLayoutState.Position.EXPANDED) {
                        toggleAction()
                    }
                }
            }
        },
        isSwipeGestureEnabled = state.composerState.showTextFormatting,
        state = expandableState,
        sheetShape = if (state.composerState.showTextFormatting || state.composerState.suggestions.isNotEmpty()) {
            MaterialTheme.shapes.large
        } else {
            RectangleShape
        },
        maxBottomSheetContentHeight = maxComposerHeightPx.toDp(),
    )

    RoomMessageSearchView(
        state = state.roomMessageSearchState,
        roomAvatar = state.roomAvatar,
        heroes = state.heroes,
        modifier = Modifier.fillMaxSize(),
    )

    var endPollConfirmingEvent: TimelineItem.Event? by remember { mutableStateOf(null) }

    if (endPollConfirmingEvent != null) {
        ConfirmationDialog(
            content = stringResource(id = CommonStrings.common_poll_end_confirmation),
            onSubmitClick = {
                endPollConfirmingEvent?.let { event ->
                    onActionSelected(TimelineItemAction.EndPoll, event)
                }
                endPollConfirmingEvent = null
            },
            onDismiss = { endPollConfirmingEvent = null },
        )
    }

    ActionListView(
        state = state.actionListState,
        onSelectAction = { action: TimelineItemAction, event: TimelineItem.Event ->
            if (action == TimelineItemAction.EndPoll) {
                endPollConfirmingEvent = event
            } else {
                onActionSelected(action, event)
            }
        },
        onCustomReactionClick = { event ->
            state.customReactionState.eventSink(CustomReactionEvent.ShowCustomReactionSheet(event))
        },
        onEmojiReactionClick = ::onEmojiReactionClick,
        onVerifiedUserSendFailureClick = { event ->
            state.timelineState.eventSink(TimelineEvent.ComputeVerifiedUserSendFailure(event))
        },
    )

    CustomReactionBottomSheet(
        state = state.customReactionState,
        onSelectEmoji = { uniqueId, emoji ->
            state.eventSink(MessagesEvent.ToggleReaction(emoji.unicode, uniqueId))
        }
    )

    ReactionSummaryView(state = state.reactionSummaryState)
    ReadReceiptBottomSheet(
        state = state.readReceiptBottomSheetState,
        onUserDataClick = onUserDataClick,
    )
    MomentAIBriefingBottomSheet(
        state = state.aiBriefingState,
        onDismiss = { state.eventSink(MessagesEvent.DismissAIBriefing) },
        onRetry = { state.eventSink(MessagesEvent.RetryAIBriefing) },
    )
    ReinviteDialog(state = state)
    LinkView(
        onLinkValid = { link ->
            onLinkClick(link.url, false)
        },
        state = state.linkState,
    )
}

@Composable
internal fun MessagesMenuActions(
    displayThreads: Boolean,
    displaySearch: Boolean,
    roomCallState: RoomCallState,
    onSearchClick: () -> Unit,
    onJoinCallClick: (isAudioCall: Boolean) -> Unit,
    onThreadsListClick: () -> Unit,
) {
    if (displayThreads) {
        Icon(
            modifier = Modifier.clickable(enabled = true, onClick = onThreadsListClick),
            imageVector = CompoundIcons.ThreadsSolid(),
            contentDescription = stringResource(CommonStrings.common_threads),
        )
        Spacer(Modifier.width(8.dp))
    }
    if (displaySearch) {
        IconButton(
            modifier = Modifier.size(MomentRoomHeaderActionSize),
            onClick = onSearchClick,
        ) {
            Icon(
                imageVector = CompoundIcons.Search(),
                contentDescription = stringResource(CommonStrings.action_search),
                tint = ElementTheme.colors.iconPrimary,
            )
        }
    }
    CallMenuItem(
        roomCallState = roomCallState,
        onJoinCallClick = onJoinCallClick,
        modifier = Modifier.size(MomentRoomHeaderActionSize),
    )
}

@Composable
private fun RoomMessageSearchView(
    state: RoomMessageSearchState,
    roomAvatar: AvatarData,
    heroes: ImmutableList<AvatarData>,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.isSearchActive) {
        state.eventSink(RoomMessageSearchEvent.ToggleSearchVisibility)
    }

    AnimatedVisibility(
        visible = state.isSearchActive,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        RoomMessageSearchContent(
            state = state,
            roomAvatar = roomAvatar,
            heroes = heroes,
            modifier = modifier,
        )
    }
}

@Composable
private fun RoomMessageSearchContent(
    state: RoomMessageSearchState,
    roomAvatar: AvatarData,
    heroes: ImmutableList<AvatarData>,
    modifier: Modifier = Modifier,
) {
    fun onDismiss() {
        state.eventSink(RoomMessageSearchEvent.ToggleSearchVisibility)
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
                .clip(RoomMessageSearchCardShape)
                .background(ElementTheme.colors.bgCanvasDefault)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
            )
        ) {
            RoomMessageSearchHeader(
                state = state,
            )

            val lazyListState = rememberLazyListState()
            OnVisibleRangeChangeEffect(lazyListState) { visibleRange ->
                state.eventSink(RoomMessageSearchEvent.UpdateVisibleRange(visibleRange))
            }
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                val highlightedEventId = state.results.firstOrNull()?.eventId
                if (state.hasEmptySearchResults) {
                    item(contentType = "empty") {
                        RoomMessageSearchEmptyRow()
                    }
                }
                items(
                    items = state.results,
                    key = { result -> result.eventId.value },
                    contentType = { "message-search-result" },
                ) { result ->
                    RoomMessageSearchResultRow(
                        result = result,
                        roomAvatar = roomAvatar,
                        heroes = heroes,
                        isHighlighted = result.eventId == highlightedEventId,
                        onClick = { state.eventSink(RoomMessageSearchEvent.SelectResult(result.eventId)) },
                    )
                }
                if (state.isSearching) {
                    item(contentType = "loading") {
                        RoomMessageSearchLoadingRow()
                    }
                }
                if (state.hasSearchError) {
                    item(contentType = "error") {
                        RoomMessageSearchErrorRow()
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomMessageSearchHeader(
    state: RoomMessageSearchState,
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

@Composable
private fun RoomMessageSearchResultRow(
    result: RoomMessageSearchResult,
    roomAvatar: AvatarData,
    heroes: ImmutableList<AvatarData>,
    isHighlighted: Boolean,
    onClick: () -> Unit,
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
                .clickable { onClick() }
                .heightIn(min = 64.dp)
                .padding(start = 20.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                avatarData = roomAvatar,
                avatarType = AvatarType.Room(
                    heroes = heroes,
                    isTombstoned = false,
                ),
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 20.dp))
    }
}

@Composable
private fun RoomMessageSearchEmptyRow(modifier: Modifier = Modifier) {
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
private fun RoomMessageSearchLoadingRow(modifier: Modifier = Modifier) {
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
private fun RoomMessageSearchErrorRow(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        text = stringResource(CommonStrings.error_unknown),
        style = ElementTheme.typography.fontBodyMdRegular,
        color = ElementTheme.colors.textCriticalPrimary,
    )
}

private val RoomMessageSearchCardShape = RoundedCornerShape(10.dp)

@Composable
private fun ReinviteDialog(state: MessagesState) {
    if (state.showReinvitePrompt) {
        ConfirmationDialog(
            title = stringResource(id = R.string.screen_room_invite_again_alert_title),
            content = stringResource(id = R.string.screen_room_invite_again_alert_message),
            cancelText = stringResource(id = CommonStrings.action_cancel),
            submitText = stringResource(id = CommonStrings.action_invite),
            onSubmitClick = { state.eventSink(MessagesEvent.InviteDialogDismissed(InviteDialogAction.Invite)) },
            onDismiss = { state.eventSink(MessagesEvent.InviteDialogDismissed(InviteDialogAction.Cancel)) }
        )
    }
}

@Composable
private fun MessagesViewContent(
    state: MessagesState,
    onContentClick: (TimelineItem.Event) -> Unit,
    onUserDataClick: (MatrixUser) -> Unit,
    onLinkClick: (Link, Boolean) -> Unit,
    onReactionClick: (key: String, TimelineItem.Event) -> Unit,
    onReactionLongClick: (key: String, TimelineItem.Event) -> Unit,
    onMoreReactionsClick: (TimelineItem.Event) -> Unit,
    onReadReceiptClick: (TimelineItem.Event) -> Unit,
    onMessageLongClick: (TimelineItem.Event) -> Unit,
    onSendLocationClick: () -> Unit,
    onCreatePollClick: () -> Unit,
    onViewAllPinnedMessagesClick: () -> Unit,
    forceJumpToBottomVisibility: Boolean,
    onSwipeToReply: (TimelineItem.Event) -> Unit,
    modifier: Modifier = Modifier,
    knockRequestsBannerView: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
    ) {
        AttachmentsBottomSheet(
            state = state.composerState,
            onSendLocationClick = onSendLocationClick,
            onCreatePollClick = onCreatePollClick,
            onStartVoiceMessageRecordingClick = {
                state.voiceMessageComposerState.eventSink(
                    VoiceMessageComposerEvent.RecorderEvent(VoiceMessageRecorderEvent.Start)
                )
            },
            enableTextFormatting = state.enableTextFormatting,
        )
        ComposerEmojiPickerBottomSheet(state = state.composerState)
        ContactAttachmentPickerBottomSheet(state = state.composerState)

        if (state.voiceMessageComposerState.showPermissionRationaleDialog) {
            VoiceMessagePermissionRationaleDialog(
                onContinue = {
                    state.voiceMessageComposerState.eventSink(VoiceMessageComposerEvent.AcceptPermissionRationale)
                },
                onDismiss = {
                    state.voiceMessageComposerState.eventSink(VoiceMessageComposerEvent.DismissPermissionsRationale)
                },
                appName = state.appName
            )
        }
        if (state.voiceMessageComposerState.showSendFailureDialog) {
            VoiceMessageSendingFailedDialog(
                onDismiss = { state.voiceMessageComposerState.eventSink(VoiceMessageComposerEvent.DismissSendFailureDialog) },
            )
        }

        Box {
            val scrollBehavior = PinnedMessagesBannerViewDefaults.rememberScrollBehavior(
                pinnedMessagesCount = (state.pinnedMessagesBannerState as? PinnedMessagesBannerState.Visible)?.pinnedMessagesCount() ?: 0,
            )
            val density = LocalDensity.current
            var pinnedBannerHeightDp by remember { mutableStateOf(0.dp) }

            TimelineView(
                state = state.timelineState,
                timelineProtectionState = state.timelineProtectionState,
                onUserDataClick = onUserDataClick,
                onLinkClick = { link -> onLinkClick(link, false) },
                onContentClick = onContentClick,
                onMessageLongClick = onMessageLongClick,
                onSwipeToReply = onSwipeToReply,
                onReactionClick = onReactionClick,
                onReactionLongClick = onReactionLongClick,
                onMoreReactionsClick = onMoreReactionsClick,
                onReadReceiptClick = onReadReceiptClick,
                forceJumpToBottomVisibility = forceJumpToBottomVisibility,
                nestedScrollConnection = scrollBehavior.nestedScrollConnection,
                floatingDateTopOffset = pinnedBannerHeightDp,
            )

            if (state.timelineState.timelineMode !is Timeline.Mode.Thread) {
                Column {
                    AnimatedVisibility(
                        visible = state.pinnedMessagesBannerState is PinnedMessagesBannerState.Visible && scrollBehavior.isVisible,
                        modifier = Modifier.onSizeChanged { pinnedBannerHeightDp = with(density) { it.height.toDp() } },
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        fun focusOnPinnedEvent(eventId: EventId) {
                            state.timelineState.eventSink(
                                TimelineEvent.FocusOnEvent(eventId = eventId, debounce = FOCUS_ON_PINNED_EVENT_DEBOUNCE_DURATION_IN_MILLIS.milliseconds)
                            )
                        }
                        PinnedMessagesBannerView(
                            state = state.pinnedMessagesBannerState,
                            onClick = ::focusOnPinnedEvent,
                            onViewAllClick = onViewAllPinnedMessagesClick,
                        )
                    }
                    if (state.showLiveLocationShareBanner) {
                        LiveLocationSharingBanner(
                            onClick = { state.eventSink(MessagesEvent.ShowLiveLocationShare) },
                            onStopClick = { state.eventSink(MessagesEvent.StopLiveLocationShare) }
                        )
                    }
                }
            }

            knockRequestsBannerView()
        }
    }
}

@Composable
private fun MessagesViewComposerBottomSheetContents(
    state: MessagesState,
    onRoomSuccessorClick: (RoomId) -> Unit,
    onLinkClick: (String, Boolean) -> Unit,
) {
    when {
        state.successorRoom != null -> {
            SuccessorRoomBanner(roomSuccessor = state.successorRoom, onRoomSuccessorClick = onRoomSuccessorClick)
        }
        state.userEventPermissions.canSendMessage -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                MessageComposerView(
                    state = state.composerState,
                    voiceMessageState = state.voiceMessageComposerState,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        else -> {
            CantSendMessageBanner()
        }
    }
}

@Composable
private fun CantSendMessageBanner() {
    Row(
        modifier = Modifier
                .fillMaxWidth()
                .background(ElementTheme.colors.bgSubtleSecondary)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.screen_room_timeline_no_permission_to_post),
            color = ElementTheme.colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic,
        )
    }
}

@Composable
private fun SuccessorRoomBanner(
    roomSuccessor: SuccessorRoom,
    onRoomSuccessorClick: (RoomId) -> Unit,
    modifier: Modifier = Modifier,
) {
    ComposerAlertMolecule(
        avatar = null,
        content = stringResource(R.string.screen_room_timeline_tombstoned_room_message).toAnnotatedString(),
        onSubmitClick = { onRoomSuccessorClick(roomSuccessor.roomId) },
        modifier = modifier,
        submitText = stringResource(R.string.screen_room_timeline_tombstoned_room_action)
    )
}

@PreviewsDayNight
@Composable
internal fun MessagesViewPreview(@PreviewParameter(MessagesStateProvider::class) state: MessagesState) = ElementPreview {
    MessagesView(
        state = state,
        onBackClick = {},
        onRoomDetailsClick = {},
        onEventContentClick = { _, _ -> false },
        onUserDataClick = {},
        onLinkClick = { _, _ -> },
        onSendLocationClick = {},
        onCreatePollClick = {},
        onJoinCallClick = {},
        onViewAllPinnedMessagesClick = { },
        forceJumpToBottomVisibility = true,
        knockRequestsBannerView = {},
        onThreadsListClick = {},
    )
}

@Preview
@Composable
internal fun MessagesViewA11yPreview() = ElementPreview {
    val content = aTimelineItemTextContent(
        body = "A message content"
    )
    MessagesView(
        state = aMessagesState(
            roomName = "A DM with a very looong name",
            timelineState = aTimelineState(
                timelineItems = persistentListOf(
                    // 1 items with isMine = false
                    aTimelineItemEvent(
                        isMine = false,
                        content = content,
                        groupPosition = TimelineItemGroupPosition.None,
                        sendState = LocalEventSendState.Failed.Unknown("Message failed to send"),
                    ),
                    // A state event on top of it
                    aTimelineItemEvent(
                        isMine = false,
                        content = aTimelineItemStateEventContent(),
                        groupPosition = TimelineItemGroupPosition.None
                    ),
                    // 1 item with isMine = true
                    aTimelineItemEvent(
                        isMine = true,
                        content = content,
                        groupPosition = TimelineItemGroupPosition.None
                    ),
                    // A grouped event on top of it
                    aGroupedEvents(),
                    // A day separator
                    aTimelineItemDaySeparator(),
                ),
                // Render a focused event for an event with sender information displayed
                focusedEventIndex = 2,
            )
        ),
        onBackClick = {},
        onRoomDetailsClick = {},
        onEventContentClick = { _, _ -> false },
        onUserDataClick = {},
        onLinkClick = { _, _ -> },
        onSendLocationClick = {},
        onCreatePollClick = {},
        onJoinCallClick = {},
        onViewAllPinnedMessagesClick = {},
        onThreadsListClick = {},
        forceJumpToBottomVisibility = true,
        knockRequestsBannerView = {},
    )
}
