/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roommembermoderation.impl

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.roommembermoderation.api.ModerationAction
import io.element.android.features.roommembermoderation.api.ModerationActionState
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.components.async.AsyncIndicator
import io.element.android.libraries.designsystem.components.async.AsyncIndicatorHost
import io.element.android.libraries.designsystem.components.async.rememberAsyncIndicatorState
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.dialogs.TextFieldDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.matrix.ui.model.getBestName
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun RoomMemberModerationView(
    state: InternalRoomMemberModerationState,
    onSelectAction: (ModerationAction, MatrixUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        val selectedUser = state.selectedUser
        if (selectedUser != null && state.canDisplayActions) {
            RoomMemberActionsBottomSheet(
                user = selectedUser,
                actions = state.actions,
                onSelectAction = onSelectAction,
                onDismiss = { state.eventSink(InternalRoomMemberModerationEvents.Reset) },
            )
        }
        RoomMemberAsyncActions(state = state)
    }
}

@Composable
private fun RoomMemberAsyncActions(
    state: InternalRoomMemberModerationState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        val selectedUser = state.selectedUser
        val asyncIndicatorState = rememberAsyncIndicatorState()
        AsyncIndicatorHost(modifier = Modifier.statusBarsPadding(), state = asyncIndicatorState)

        when (val action = state.kickUserAsyncAction) {
            is AsyncAction.Confirming -> {
                TextFieldDialog(
                    title = stringResource(R.string.screen_bottom_sheet_manage_room_member_kick_member_confirmation_title),
                    submitText = stringResource(R.string.screen_bottom_sheet_manage_room_member_kick_member_confirmation_action),
                    destructiveSubmit = true,
                    minLines = 2,
                    onSubmit = { reason ->
                        state.eventSink(InternalRoomMemberModerationEvents.DoKickUser(reason = reason))
                    },
                    onDismissRequest = { state.eventSink(InternalRoomMemberModerationEvents.Reset) },
                    placeholder = stringResource(id = CommonStrings.common_reason),
                    content = stringResource(R.string.screen_bottom_sheet_manage_room_member_kick_member_confirmation_description),
                    value = "",
                )
            }
            is AsyncAction.Loading -> {
                LaunchedEffect(action) {
                    val userDisplayName = selectedUser?.getBestName().orEmpty()
                    asyncIndicatorState.enqueue {
                        AsyncIndicator.Loading(text = stringResource(R.string.screen_bottom_sheet_manage_room_member_removing_user, userDisplayName))
                    }
                }
            }
            is AsyncAction.Failure -> {
                Timber.e(action.error, "Failed to kick user.")
                LaunchedEffect(action) {
                    asyncIndicatorState.enqueue(AsyncIndicator.DURATION_SHORT) {
                        AsyncIndicator.Failure(
                            text = stringResource(CommonStrings.common_failed),
                        )
                    }
                }
            }
            is AsyncAction.Success -> {
                LaunchedEffect(action) { asyncIndicatorState.clear() }
            }
            else -> Unit
        }

        when (val action = state.banUserAsyncAction) {
            is AsyncAction.Confirming -> {
                TextFieldDialog(
                    title = stringResource(R.string.screen_bottom_sheet_manage_room_member_ban_member_confirmation_title),
                    submitText = stringResource(R.string.screen_bottom_sheet_manage_room_member_ban_member_confirmation_action),
                    destructiveSubmit = true,
                    minLines = 2,
                    onSubmit = { reason ->
                        state.eventSink(InternalRoomMemberModerationEvents.DoBanUser(reason = reason))
                    },
                    onDismissRequest = { state.eventSink(InternalRoomMemberModerationEvents.Reset) },
                    placeholder = stringResource(id = CommonStrings.common_reason),
                    content = stringResource(R.string.screen_bottom_sheet_manage_room_member_ban_member_confirmation_description),
                    value = "",
                )
            }
            is AsyncAction.Loading -> {
                LaunchedEffect(action) {
                    val userDisplayName = selectedUser?.getBestName().orEmpty()
                    asyncIndicatorState.enqueue {
                        AsyncIndicator.Loading(text = stringResource(R.string.screen_bottom_sheet_manage_room_member_banning_user, userDisplayName))
                    }
                }
            }
            is AsyncAction.Failure -> {
                Timber.e(action.error, "Failed to ban user.")
                LaunchedEffect(action) {
                    asyncIndicatorState.enqueue(AsyncIndicator.DURATION_SHORT) {
                        AsyncIndicator.Failure(
                            text = stringResource(CommonStrings.common_failed),
                        )
                    }
                }
            }
            is AsyncAction.Success -> {
                LaunchedEffect(action) { asyncIndicatorState.clear() }
            }
            else -> Unit
        }
        when (val action = state.unbanUserAsyncAction) {
            is AsyncAction.Confirming -> {
                TextFieldDialog(
                    title = stringResource(R.string.screen_bottom_sheet_manage_room_member_unban_member_confirmation_title),
                    submitText = stringResource(R.string.screen_bottom_sheet_manage_room_member_unban_member_confirmation_action),
                    destructiveSubmit = true,
                    minLines = 2,
                    onSubmit = { reason ->
                        val userDisplayName = selectedUser?.getBestName().orEmpty()
                        asyncIndicatorState.enqueue {
                            AsyncIndicator.Loading(text = stringResource(R.string.screen_bottom_sheet_manage_room_member_unbanning_user, userDisplayName))
                        }
                        state.eventSink(InternalRoomMemberModerationEvents.DoUnbanUser(reason = reason))
                    },
                    onDismissRequest = { state.eventSink(InternalRoomMemberModerationEvents.Reset) },
                    placeholder = stringResource(id = CommonStrings.common_reason),
                    content = stringResource(R.string.screen_bottom_sheet_manage_room_member_unban_member_confirmation_description),
                    value = "",
                )
            }
            is AsyncAction.Failure -> {
                Timber.e(action.error, "Failed to unban user.")
                LaunchedEffect(action) {
                    asyncIndicatorState.enqueue(AsyncIndicator.DURATION_SHORT) {
                        AsyncIndicator.Failure(
                            text = stringResource(CommonStrings.common_failed),
                        )
                    }
                }
            }
            is AsyncAction.Success -> {
                LaunchedEffect(action) { asyncIndicatorState.clear() }
            }
            is AsyncAction.Loading,
            AsyncAction.Uninitialized -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomMemberActionsBottomSheet(
    user: MatrixUser,
    actions: ImmutableList<ModerationActionState>,
    onSelectAction: (ModerationAction, MatrixUser) -> Unit,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        modifier = Modifier.systemBarsPadding(),
        sheetState = bottomSheetState,
        onDismissRequest = {
            coroutineScope.launch {
                bottomSheetState.hide()
                onDismiss()
            }
        },
        scrollable = false,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Avatar(
                avatarData = user.getAvatarData(size = AvatarSize.RoomListManageUser),
                avatarType = AvatarType.User,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )
            val bestName = user.getBestName()
            Text(
                text = bestName,
                style = ElementTheme.typography.fontHeadingLgBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    .fillMaxWidth()
            )
            // Show user ID only if it's different from the display name
            if (bestName != user.userId.value) {
                Text(
                    text = user.userId.value,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            actions.firstOrNull { it.action is ModerationAction.DisplayProfile }?.let { actionState ->
                MomentMemberActionCard {
                    val action = actionState.action
                    MomentMemberActionRow(
                        title = stringResource(R.string.screen_bottom_sheet_manage_room_member_member_user_info),
                        imageVector = CompoundIcons.UserProfileSolid(),
                        enabled = actionState.isEnabled,
                        showDivider = false,
                        onClick = {
                            coroutineScope.launch {
                                onSelectAction(action, user)
                                bottomSheetState.hide()
                            }
                        },
                    )
                }
            }

            val moderationActions = actions.filterNot { it.action is ModerationAction.DisplayProfile }
            if (moderationActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                MomentMemberActionCard {
                    moderationActions.forEachIndexed { index, actionState ->
                        val showDivider = index != moderationActions.lastIndex
                        when (val action = actionState.action) {
                            is ModerationAction.DisplayProfile -> Unit
                            is ModerationAction.KickUser -> {
                                MomentMemberActionRow(
                                    title = stringResource(R.string.screen_bottom_sheet_manage_room_member_remove),
                                    imageVector = CompoundIcons.Close(),
                                    isDestructive = true,
                                    enabled = actionState.isEnabled,
                                    showDivider = showDivider,
                                    onClick = {
                                        coroutineScope.launch {
                                            bottomSheetState.hide()
                                            onSelectAction(action, user)
                                        }
                                    },
                                )
                            }
                            is ModerationAction.BanUser -> {
                                MomentMemberActionRow(
                                    title = stringResource(R.string.screen_bottom_sheet_manage_room_member_ban),
                                    imageVector = CompoundIcons.Block(),
                                    isDestructive = true,
                                    enabled = actionState.isEnabled,
                                    showDivider = showDivider,
                                    onClick = {
                                        coroutineScope.launch {
                                            bottomSheetState.hide()
                                            onSelectAction(action, user)
                                        }
                                    },
                                )
                            }
                            is ModerationAction.UnbanUser -> {
                                MomentMemberActionRow(
                                    title = stringResource(R.string.screen_bottom_sheet_manage_room_member_unban),
                                    imageVector = CompoundIcons.Restart(),
                                    isDestructive = true,
                                    enabled = actionState.isEnabled,
                                    showDivider = showDivider,
                                    onClick = {
                                        coroutineScope.launch {
                                            bottomSheetState.hide()
                                            onSelectAction(action, user)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MomentMemberActionCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.55f)),
    ) {
        Column(content = { content() })
    }
}

@Composable
private fun MomentMemberActionRow(
    title: String,
    imageVector: ImageVector,
    enabled: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
) {
    val textColor = when {
        !enabled -> ElementTheme.colors.textDisabled
        isDestructive -> ElementTheme.colors.textCriticalPrimary
        else -> ElementTheme.colors.textPrimary
    }
    val iconTint = when {
        !enabled -> ElementTheme.colors.iconDisabled
        isDestructive -> ElementTheme.colors.iconCriticalPrimary
        else -> ElementTheme.colors.iconPrimary
    }
    val iconBackground = when {
        isDestructive && enabled -> ElementTheme.colors.bgCriticalSubtle
        else -> ElementTheme.colors.bgSubtleSecondary
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBackground, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(22.dp),
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = iconTint,
                )
            }
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = ElementTheme.typography.fontBodyLgRegular,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 70.dp),
                color = ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.45f),
            )
        }
    }
}

@PreviewsDayNight
@Composable
internal fun RoomMemberModerationViewPreview(@PreviewParameter(InternalRoomMemberModerationStateProvider::class) state: InternalRoomMemberModerationState) {
    ElementPreview {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
        ) {
            RoomMemberModerationView(
                state = state,
                onSelectAction = { _, _ ->
                },
            )
        }
    }
}
