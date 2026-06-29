/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.joinroom.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.invite.api.InviteData
import io.element.android.libraries.designsystem.atomic.atoms.PlaceholderAtom
import io.element.android.libraries.designsystem.atomic.molecules.MembersCountMolecule
import io.element.android.libraries.designsystem.components.BigIcon
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.button.SuperButton
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.libraries.designsystem.components.dialogs.RetryDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.ButtonSize
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.libraries.designsystem.theme.components.OutlinedButton
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.designsystem.theme.placeholderBackground
import io.element.android.libraries.matrix.api.core.RoomIdOrAlias
import io.element.android.libraries.matrix.api.spaces.SpaceRoomVisibility
import io.element.android.libraries.matrix.ui.components.SpaceInfoRow
import io.element.android.libraries.matrix.ui.components.SpaceMembersView
import io.element.android.libraries.matrix.ui.model.InviteSender
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.persistentListOf

@Composable
fun JoinRoomView(
    state: JoinRoomState,
    onBackClick: () -> Unit,
    onJoinSuccess: () -> Unit,
    onKnockSuccess: () -> Unit,
    onForgetSuccess: () -> Unit,
    onCancelKnockSuccess: () -> Unit,
    onDeclineInviteAndBlockUser: (InviteData) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ElementTheme.colors.bgCanvasDefault,
        topBar = {
            JoinRoomTopBar(
                contentState = state.contentState,
                hideAvatarImage = state.hideAvatarsImages,
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            if (state.joinAuthorisationStatus != JoinAuthorisationStatus.None) {
                Surface(
                    color = ElementTheme.colors.bgCanvasDefault,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    JoinRoomFooter(
                        joinAuthorisationStatus = state.joinAuthorisationStatus,
                        onAcceptInvite = { inviteData ->
                            state.eventSink(JoinRoomEvents.AcceptInvite(inviteData))
                        },
                        onDeclineInvite = { inviteData, blockUser ->
                            if (state.canReportRoom && blockUser) {
                                onDeclineInviteAndBlockUser(inviteData)
                            } else {
                                state.eventSink(JoinRoomEvents.DeclineInvite(inviteData, blockUser = blockUser))
                            }
                        },
                        onJoinRoom = {
                            state.eventSink(JoinRoomEvents.JoinRoom)
                        },
                        onKnockRoom = {
                            state.eventSink(JoinRoomEvents.KnockRoom)
                        },
                        onCancelKnock = {
                            state.eventSink(JoinRoomEvents.CancelKnock(requiresConfirmation = true))
                        },
                        onForgetRoom = {
                            state.eventSink(JoinRoomEvents.ForgetRoom)
                        },
                        onGoBack = onBackClick,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 16.dp),
                    )
                }
            }
        },
    ) { padding ->
        JoinRoomContent(
            roomIdOrAlias = state.roomIdOrAlias,
            contentState = state.contentState,
            knockMessage = state.knockMessage,
            hideAvatarsImages = state.hideAvatarsImages,
            onKnockMessageUpdate = { state.eventSink(JoinRoomEvents.UpdateKnockMessage(it)) },
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        )
    }
    if (state.contentState is ContentState.Failure) {
        RetryDialog(
            title = stringResource(R.string.screen_join_room_loading_alert_title),
            content = stringResource(CommonStrings.error_network_or_server_issue),
            onRetry = { state.eventSink(JoinRoomEvents.RetryFetchingContent) },
            onDismiss = {
                state.eventSink(JoinRoomEvents.DismissErrorAndHideContent)
                onBackClick()
            }
        )
    }
    // This particular error is shown directly in the footer
    if (!state.isJoinActionUnauthorized) {
        AsyncActionView(
            async = state.joinAction,
            errorTitle = { stringResource(CommonStrings.common_something_went_wrong) },
            errorMessage = { stringResource(CommonStrings.error_network_or_server_issue) },
            onSuccess = { onJoinSuccess() },
            onErrorDismiss = { state.eventSink(JoinRoomEvents.ClearActionStates) },
        )
    }
    AsyncActionView(
        async = state.knockAction,
        errorTitle = { stringResource(CommonStrings.common_something_went_wrong) },
        errorMessage = { stringResource(CommonStrings.error_network_or_server_issue) },
        onSuccess = { onKnockSuccess() },
        onErrorDismiss = { state.eventSink(JoinRoomEvents.ClearActionStates) },
    )
    AsyncActionView(
        async = state.forgetAction,
        errorTitle = { stringResource(CommonStrings.common_something_went_wrong) },
        errorMessage = { stringResource(CommonStrings.error_network_or_server_issue) },
        onSuccess = { onForgetSuccess() },
        onErrorDismiss = { state.eventSink(JoinRoomEvents.ClearActionStates) },
    )
    AsyncActionView(
        async = state.cancelKnockAction,
        onSuccess = { onCancelKnockSuccess() },
        onErrorDismiss = { state.eventSink(JoinRoomEvents.ClearActionStates) },
        errorTitle = { stringResource(CommonStrings.common_something_went_wrong) },
        errorMessage = { stringResource(CommonStrings.error_network_or_server_issue) },
        confirmationDialog = {
            ConfirmationDialog(
                content = stringResource(R.string.screen_join_room_cancel_knock_alert_description),
                title = stringResource(R.string.screen_join_room_cancel_knock_alert_title),
                submitText = stringResource(R.string.screen_join_room_cancel_knock_alert_confirmation),
                cancelText = stringResource(CommonStrings.action_no),
                onSubmitClick = { state.eventSink(JoinRoomEvents.CancelKnock(requiresConfirmation = false)) },
                onDismiss = { state.eventSink(JoinRoomEvents.ClearActionStates) },
            )
        },
    )
}

@Composable
private fun JoinRoomFooter(
    joinAuthorisationStatus: JoinAuthorisationStatus,
    onAcceptInvite: (InviteData) -> Unit,
    onDeclineInvite: (InviteData, Boolean) -> Unit,
    onJoinRoom: () -> Unit,
    onKnockRoom: () -> Unit,
    onCancelKnock: () -> Unit,
    onForgetRoom: () -> Unit,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (joinAuthorisationStatus) {
            is JoinAuthorisationStatus.IsInvited -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        text = stringResource(CommonStrings.action_decline),
                        onClick = { onDeclineInvite(joinAuthorisationStatus.inviteData, false) },
                        modifier = Modifier.weight(1f),
                        size = ButtonSize.LargeLowPadding,
                        leadingIcon = IconSource.Vector(CompoundIcons.Close())
                    )
                    Button(
                        text = stringResource(CommonStrings.action_accept),
                        onClick = { onAcceptInvite(joinAuthorisationStatus.inviteData) },
                        modifier = Modifier.weight(1f),
                        size = ButtonSize.LargeLowPadding,
                        leadingIcon = IconSource.Vector(CompoundIcons.Check())
                    )
                }
                TextButton(
                    text = stringResource(R.string.screen_join_room_decline_and_block_button_title),
                    onClick = { onDeclineInvite(joinAuthorisationStatus.inviteData, true) },
                    modifier = Modifier.fillMaxWidth(),
                    destructive = true
                )
            }
            JoinAuthorisationStatus.CanJoin -> {
                SuperButton(
                    onClick = onJoinRoom,
                    modifier = Modifier.fillMaxWidth(),
                    buttonSize = ButtonSize.Large,
                ) {
                    Text(
                        text = stringResource(R.string.screen_join_room_join_action),
                    )
                }
            }
            JoinAuthorisationStatus.CanKnock -> {
                SuperButton(
                    onClick = onKnockRoom,
                    modifier = Modifier.fillMaxWidth(),
                    buttonSize = ButtonSize.Large,
                ) {
                    Text(
                        text = stringResource(R.string.screen_join_room_knock_action),
                    )
                }
            }
            JoinAuthorisationStatus.IsKnocked -> {
                OutlinedButton(
                    text = stringResource(R.string.screen_join_room_cancel_knock_action),
                    onClick = onCancelKnock,
                    modifier = Modifier.fillMaxWidth(),
                    size = ButtonSize.Large,
                )
            }
            JoinAuthorisationStatus.NeedInvite -> {
                JoinRoomNoticeCard(
                    title = stringResource(R.string.screen_join_room_invite_required_message),
                )
            }
            is JoinAuthorisationStatus.IsBanned -> JoinBannedFooter(joinAuthorisationStatus, onForgetRoom)
            JoinAuthorisationStatus.Unknown -> JoinRestrictedFooter(onJoinRoom)
            JoinAuthorisationStatus.Restricted -> JoinRestrictedFooter(onJoinRoom)
            JoinAuthorisationStatus.Unauthorized -> JoinUnauthorizedFooter(onGoBack)
            JoinAuthorisationStatus.None -> Unit
        }
    }
}

@Composable
private fun JoinUnauthorizedFooter(
    onOkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        JoinRoomNoticeCard(
            title = stringResource(R.string.screen_join_room_fail_message),
            description = stringResource(R.string.screen_join_room_fail_reason),
            isCritical = true,
        )
        Button(
            text = stringResource(CommonStrings.action_ok),
            onClick = onOkClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun JoinBannedFooter(
    status: JoinAuthorisationStatus.IsBanned,
    onForgetRoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val banReason = status.reason?.let {
            stringResource(R.string.screen_join_room_ban_reason, it.removeSuffix("."))
        }
        val title = if (status.banSender != null) {
            stringResource(R.string.screen_join_room_ban_by_message, status.banSender.displayName)
        } else {
            stringResource(R.string.screen_join_room_ban_message)
        }
        JoinRoomNoticeCard(
            title = title,
            description = banReason,
            isCritical = true,
        )
        Button(
            text = stringResource(R.string.screen_join_room_forget_action),
            onClick = onForgetRoom,
            modifier = Modifier.fillMaxWidth(),
            size = ButtonSize.Large,
        )
    }
}

@Composable
private fun JoinRestrictedFooter(
    onJoinRoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        JoinRoomNoticeCard(
            title = stringResource(R.string.screen_join_room_join_restricted_message),
        )
        SuperButton(
            onClick = onJoinRoom,
            modifier = Modifier.fillMaxWidth(),
            buttonSize = ButtonSize.Large,
        ) {
            Text(
                text = stringResource(R.string.screen_join_room_join_action),
            )
        }
    }
}

@Composable
private fun JoinRoomNoticeCard(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    isCritical: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isCritical) ElementTheme.colors.bgCriticalSubtle else ElementTheme.colors.bgSubtleSecondary,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = if (isCritical) CompoundIcons.ErrorSolid() else CompoundIcons.Info(),
                contentDescription = null,
                tint = if (isCritical) ElementTheme.colors.iconCriticalPrimary else ElementTheme.colors.iconSecondary,
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = ElementTheme.typography.fontBodyMdMedium,
                    color = if (isCritical) ElementTheme.colors.textCriticalPrimary else ElementTheme.colors.textPrimary,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = ElementTheme.typography.fontBodyMdRegular,
                        color = if (isCritical) ElementTheme.colors.textCriticalPrimary else ElementTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun JoinRoomContent(
    roomIdOrAlias: RoomIdOrAlias,
    contentState: ContentState,
    knockMessage: String,
    hideAvatarsImages: Boolean,
    onKnockMessageUpdate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        when (contentState) {
            is ContentState.Loaded -> {
                when (contentState.joinAuthorisationStatus) {
                    is JoinAuthorisationStatus.IsKnocked -> {
                        IsKnockedLoadedContent(modifier = Modifier.fillMaxWidth())
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            DefaultLoadedContent(
                                contentState = contentState,
                                hideAvatarImage = hideAvatarsImages,
                            )
                            when (contentState.joinAuthorisationStatus) {
                                is JoinAuthorisationStatus.IsInvited -> {
                                    val inviteSender = contentState.joinAuthorisationStatus.inviteSender
                                    if (inviteSender != null) {
                                        InvitedByView(inviteSender, hideAvatarsImages)
                                    }
                                }
                                is JoinAuthorisationStatus.CanKnock -> {
                                    TextField(
                                        value = knockMessage,
                                        onValueChange = { onKnockMessageUpdate(it.take(MAX_KNOCK_MESSAGE_LENGTH)) },
                                        placeholder = stringResource(R.string.screen_join_room_knock_message_description),
                                        maxLines = 4,
                                        minLines = 4,
                                        modifier = Modifier.fillMaxWidth(),
                                        supportingText = "${knockMessage.length}/$MAX_KNOCK_MESSAGE_LENGTH",
                                    )
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }
            is ContentState.UnknownRoom -> UnknownRoomContent(modifier = Modifier.fillMaxWidth())
            is ContentState.Loading -> IncompleteContent(roomIdOrAlias, isLoading = true, modifier = Modifier.fillMaxWidth())
            is ContentState.Dismissing -> IncompleteContent(roomIdOrAlias, isLoading = false, modifier = Modifier.fillMaxWidth())
            is ContentState.Failure -> IncompleteContent(roomIdOrAlias, isLoading = false, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun InvitedByView(
    sender: InviteSender,
    hideAvatarImage: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.screen_join_room_invited_by),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary
        )
        Spacer(Modifier.height(8.dp))
        Avatar(
            avatarData = sender.avatarData,
            avatarType = AvatarType.User,
            hideImage = hideAvatarImage,
            forcedAvatarSize = AvatarSize.RoomPreviewInviter.dp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = sender.displayName,
            style = ElementTheme.typography.fontBodyLgRegular,
            color = ElementTheme.colors.textPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = sender.userId.value,
            style = ElementTheme.typography.fontBodySmRegular,
            color = ElementTheme.colors.textSecondary
        )
    }
}

@Composable
private fun UnknownRoomContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(AvatarSize.RoomPreviewHeader.dp)
                .background(
                    color = ElementTheme.colors.placeholderBackground,
                    shape = CircleShape
                )
        ) {
            Icon(
                modifier = Modifier.align(Alignment.Center),
                tint = ElementTheme.colors.iconPrimary,
                imageVector = CompoundIcons.VisibilityOff(),
                contentDescription = null,
            )
        }
        Text(
            text = stringResource(R.string.screen_join_room_title_no_preview),
            style = ElementTheme.typography.fontHeadingLgBold,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.screen_join_room_subtitle_no_preview),
            style = ElementTheme.typography.fontBodyLgRegular,
            color = ElementTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun IncompleteContent(
    roomIdOrAlias: RoomIdOrAlias,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PlaceholderAtom(width = AvatarSize.RoomPreviewHeader.dp, height = AvatarSize.RoomPreviewHeader.dp)
        when (roomIdOrAlias) {
            is RoomIdOrAlias.Alias -> {
                Text(
                    text = roomIdOrAlias.identifier,
                    style = ElementTheme.typography.fontBodyLgRegular,
                    color = ElementTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            is RoomIdOrAlias.Id -> {
                PlaceholderAtom(width = 200.dp, height = 22.dp)
            }
        }
        if (isLoading) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun IsKnockedLoadedContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BigIcon(style = BigIcon.Style.SuccessSolid)
        Text(
            text = stringResource(R.string.screen_join_room_knock_sent_title),
            style = ElementTheme.typography.fontHeadingMdBold,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.screen_join_room_knock_sent_description),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DefaultLoadedContent(
    contentState: ContentState.Loaded,
    hideAvatarImage: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(
            contentState.avatarData(AvatarSize.RoomPreviewHeader),
            hideImage = hideAvatarImage,
            avatarType = if (contentState.isSpace) AvatarType.Space() else AvatarType.Room(),
        )
        Text(
            text = contentState.name ?: stringResource(id = CommonStrings.common_no_room_name),
            style = ElementTheme.typography.fontHeadingLgBold.copy(
                fontStyle = if (contentState.name == null) FontStyle.Italic else FontStyle.Normal
            ),
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )
        if (contentState.alias != null) {
            Text(
                text = contentState.alias.value,
                style = ElementTheme.typography.fontBodyLgRegular,
                color = ElementTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (contentState.details is LoadedDetails.Space) {
            SpaceInfoRow(visibility = SpaceRoomVisibility.fromJoinRule(contentState.joinRule))
        }
        if (contentState.showMemberCount) {
            val membersCount = contentState.numberOfMembers?.toInt() ?: 0
            if (contentState.isSpace) {
                SpaceMembersView(persistentListOf(), membersCount)
            } else {
                MembersCountMolecule(memberCount = membersCount)
            }
        }
        val topic = contentState.topic
        if (!topic.isNullOrBlank()) {
            Text(
                text = topic,
                style = ElementTheme.typography.fontBodyMdRegular,
                color = ElementTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun JoinRoomTopBar(
    contentState: ContentState,
    hideAvatarImage: Boolean,
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                imageVector = CompoundIcons.ChevronLeft(),
                contentDescription = stringResource(CommonStrings.action_back),
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        if (contentState is ContentState.Loaded && contentState.joinAuthorisationStatus is JoinAuthorisationStatus.IsKnocked) {
            Row(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .padding(horizontal = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (contentState.name != null) {
                    Avatar(
                        avatarData = contentState.avatarData(AvatarSize.TimelineRoom),
                        hideImage = hideAvatarImage,
                        avatarType = if (contentState.isSpace) AvatarType.Space() else AvatarType.Room(),
                    )
                    Text(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .semantics { heading() },
                        text = contentState.name,
                        style = ElementTheme.typography.fontBodyLgMedium,
                        color = ElementTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    PlaceholderAtom(width = AvatarSize.TimelineRoom.dp, height = AvatarSize.TimelineRoom.dp)
                    PlaceholderAtom(
                        width = 120.dp,
                        height = 18.dp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@PreviewsDayNight
@Composable
internal fun JoinRoomViewPreview(@PreviewParameter(JoinRoomStateProvider::class) state: JoinRoomState) = ElementPreview {
    JoinRoomView(
        state = state,
        onBackClick = { },
        onJoinSuccess = { },
        onKnockSuccess = { },
        onForgetSuccess = { },
        onCancelKnockSuccess = { },
        onDeclineInviteAndBlockUser = { },
    )
}
