/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roomdetailsedit.impl

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.components.async.AsyncActionViewDefaults
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.dialogs.SaveChangesDialog
import io.element.android.libraries.designsystem.modifiers.clearFocusOnTap
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.matrix.api.createroom.MomentRoomKind
import io.element.android.libraries.matrix.api.room.history.RoomHistoryVisibility
import io.element.android.libraries.matrix.api.room.join.JoinRule
import io.element.android.libraries.matrix.ui.components.AvatarActionBottomSheet
import io.element.android.libraries.matrix.ui.components.AvatarPickerState
import io.element.android.libraries.matrix.ui.components.AvatarPickerView
import io.element.android.libraries.permissions.api.PermissionsView
import io.element.android.libraries.ui.strings.CommonStrings
import androidx.compose.ui.semantics.onClick as semanticsOnClick

internal const val MOMENT_ROOM_DETAILS_EDIT_ROOM_ACCESS_TAG = "moment_room_details_edit-room_access"
internal const val MOMENT_ROOM_DETAILS_EDIT_ROOM_PERMISSIONS_TAG = "moment_room_details_edit-room_permissions"

/**
 * For space:
 * https://www.figma.com/design/pDlJZGBsri47FNTXMnEdXB/Compound-Android-Templates?node-id=2216-110711
 * For room:
 * https://www.figma.com/design/pDlJZGBsri47FNTXMnEdXB/Compound-Android-Templates?node-id=3187-47342
 */
@Composable
fun RoomDetailsEditView(
    state: RoomDetailsEditState,
    onDone: () -> Unit,
    onOpenSecurityAndPrivacy: () -> Unit,
    onOpenRolesAndPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val isAvatarActionsSheetVisible = remember { mutableStateOf(false) }

    fun onAvatarClick() {
        focusManager.clearFocus()
        isAvatarActionsSheetVisible.value = true
    }

    BackHandler {
        state.eventSink(RoomDetailsEditEvent.OnBackPress)
    }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .clearFocusOnTap(focusManager),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 32.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            MomentRoomDetailsEditTopBar(
                title = roomDetailsEditTitle(state),
                saveEnabled = state.saveButtonEnabled,
                onCancelClick = {
                    focusManager.clearFocus()
                    state.eventSink(RoomDetailsEditEvent.OnBackPress)
                },
                onSaveClick = {
                    focusManager.clearFocus()
                    state.eventSink(RoomDetailsEditEvent.Save)
                },
            )
            val avatarPickerState = remember(state.roomAvatarUrl, state.roomRawName) {
                val size = if (state.isSpace) AvatarSize.EditSpaceDetails else AvatarSize.EditRoomDetails
                val type = if (state.isSpace) AvatarType.Space() else AvatarType.Room()
                AvatarPickerState.Selected(
                    avatarData = AvatarData(id = state.roomId.value, name = state.roomRawName, size = size, url = state.roomAvatarUrl),
                    type = type,
                )
            }
            AvatarPickerView(
                state = avatarPickerState,
                onClick = ::onAvatarClick,
                enabled = state.canChangeAvatar,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            MomentRoomDetailsEditSection(
                title = stringResource(id = CommonStrings.common_name),
            ) {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    value = state.roomRawName,
                    placeholder = stringResource(CommonStrings.common_room_name_placeholder),
                    singleLine = true,
                    readOnly = !state.canChangeName,
                    onValueChange = { state.eventSink(RoomDetailsEditEvent.UpdateRoomName(it)) },
                )
            }

            MomentRoomDetailsEditSection(
                title = if (state.usesMomentRoomSettings) {
                    stringResource(R.string.screen_moment_room_settings_description_title_android)
                } else {
                    stringResource(id = CommonStrings.common_topic)
                },
            ) {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    value = state.roomTopic,
                    placeholder = if (state.isSpace) {
                        stringResource(CommonStrings.common_space_topic_placeholder)
                    } else if (state.usesMomentRoomSettings) {
                        stringResource(R.string.screen_moment_room_settings_description_placeholder_android)
                    } else {
                        stringResource(CommonStrings.common_topic_placeholder)
                    },
                    maxLines = 10,
                    readOnly = !state.canChangeTopic,
                    onValueChange = { state.eventSink(RoomDetailsEditEvent.UpdateRoomTopic(it)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                )
            }

            if (state.usesMomentRoomSettings) {
                MomentRoomDetailsEditSection(
                    title = stringResource(R.string.screen_moment_room_settings_access_section_title_android),
                ) {
                    MomentRoomDetailsEditRow(
                        title = stringResource(R.string.screen_moment_room_settings_history_title_android),
                        subtitle = historyVisibilitySummary(state.roomHistoryVisibility),
                        imageVector = CompoundIcons.History(),
                        onClick = onOpenSecurityAndPrivacy.takeIf { state.canEditSecurityAndPrivacy },
                    )
                    MomentRoomDetailsEditRow(
                        title = stringResource(R.string.screen_moment_room_settings_access_title_android),
                        subtitle = roomAccessSummary(state.roomJoinRule),
                        imageVector = CompoundIcons.LockSolid(),
                        onClick = onOpenSecurityAndPrivacy.takeIf { state.canEditSecurityAndPrivacy },
                        rowTestTag = MOMENT_ROOM_DETAILS_EDIT_ROOM_ACCESS_TAG,
                    )
                    if (state.canEditRolesAndPermissions) {
                        MomentRoomDetailsEditRow(
                            title = stringResource(R.string.screen_moment_room_settings_permissions_title_android),
                            subtitle = stringResource(R.string.screen_moment_room_settings_permissions_subtitle_android),
                            imageVector = CompoundIcons.Admin(),
                            onClick = onOpenRolesAndPermissions,
                            rowTestTag = MOMENT_ROOM_DETAILS_EDIT_ROOM_PERMISSIONS_TAG,
                        )
                    }
                }
            }
        }
    }
    AvatarActionBottomSheet(
        actions = state.avatarActions,
        isVisible = isAvatarActionsSheetVisible.value,
        onDismiss = { isAvatarActionsSheetVisible.value = false },
        onSelectAction = { state.eventSink(RoomDetailsEditEvent.HandleAvatarAction(it)) }
    )
    AsyncActionView(
        async = state.saveAction,
        progressDialog = {
            AsyncActionViewDefaults.ProgressDialog(
                progressText = stringResource(R.string.screen_room_details_updating_room),
            )
        },
        confirmationDialog = {
            if (state.saveAction == AsyncAction.ConfirmingCancellation) {
                SaveChangesDialog(
                    onSaveClick = { state.eventSink(RoomDetailsEditEvent.Save) },
                    onDiscardClick = { state.eventSink(RoomDetailsEditEvent.OnBackPress) },
                    onDismiss = { state.eventSink(RoomDetailsEditEvent.CloseDialog) }
                )
            }
        },
        onSuccess = { onDone() },
        errorMessage = { stringResource(R.string.screen_room_details_edition_error) },
        onErrorDismiss = { state.eventSink(RoomDetailsEditEvent.CloseDialog) }
    )

    PermissionsView(
        state = state.cameraPermissionState,
    )
}

@Composable
private fun roomDetailsEditTitle(state: RoomDetailsEditState): String {
    return when {
        state.isResolvingMomentRoomKind -> stringResource(CommonStrings.common_settings)
        state.momentRoomKind == MomentRoomKind.Channel -> stringResource(R.string.screen_moment_room_profile_settings_title_channel_android)
        state.momentRoomKind == MomentRoomKind.Group -> stringResource(R.string.screen_moment_room_profile_settings_title_group_android)
        else -> stringResource(R.string.screen_room_details_edit_room_title)
    }
}

@Composable
private fun MomentRoomDetailsEditTopBar(
    title: String,
    saveEnabled: Boolean,
    onCancelClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        val backContentDescription = stringResource(CommonStrings.action_back)
        TextButton(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .semantics {
                    contentDescription = backContentDescription
                },
            text = stringResource(CommonStrings.action_cancel),
            onClick = onCancelClick,
        )
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 112.dp),
            text = title,
            style = ElementTheme.typography.fontHeadingMdBold,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            text = stringResource(CommonStrings.action_save),
            enabled = saveEnabled,
            onClick = onSaveClick,
        )
    }
}

@Composable
private fun MomentRoomDetailsEditRow(
    title: String,
    imageVector: ImageVector,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    rowTestTag: String? = null,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                if (rowTestTag != null) {
                    testTag = rowTestTag
                    testTagsAsResourceId = true
                }
                if (onClick != null) {
                    semanticsOnClick(action = {
                        onClick()
                        true
                    })
                }
            }
            .then(clickableModifier)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = ElementTheme.colors.iconSecondary,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = ElementTheme.colors.textPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            }
        }
        if (onClick != null) {
            Icon(
                imageVector = CompoundIcons.ChevronRight(),
                contentDescription = null,
                tint = ElementTheme.colors.iconSecondary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun historyVisibilitySummary(roomHistoryVisibility: RoomHistoryVisibility): String {
    return when (roomHistoryVisibility) {
        RoomHistoryVisibility.Invited -> stringResource(R.string.screen_moment_room_settings_history_invited_android)
        RoomHistoryVisibility.Joined -> stringResource(R.string.screen_moment_room_settings_history_joined_android)
        RoomHistoryVisibility.Shared -> stringResource(R.string.screen_moment_room_settings_history_shared_android)
        RoomHistoryVisibility.WorldReadable -> stringResource(R.string.screen_moment_room_settings_history_world_readable_android)
        is RoomHistoryVisibility.Custom -> roomHistoryVisibility.value
    }
}

@Composable
private fun roomAccessSummary(joinRule: JoinRule?): String {
    return when (joinRule) {
        JoinRule.Public -> stringResource(R.string.screen_moment_room_settings_access_anyone_android)
        JoinRule.Knock -> stringResource(R.string.screen_moment_room_settings_access_ask_to_join_android)
        JoinRule.Invite -> stringResource(R.string.screen_moment_room_settings_access_invite_only_android)
        is JoinRule.Restricted -> stringResource(R.string.screen_moment_room_settings_access_space_members_android)
        is JoinRule.KnockRestricted -> stringResource(R.string.screen_moment_room_settings_access_ask_to_join_android)
        is JoinRule.Custom -> joinRule.value
        null -> stringResource(R.string.screen_moment_room_settings_access_invite_only_android)
    }
}

@Composable
private fun MomentRoomDetailsEditSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = title,
            style = ElementTheme.typography.fontBodyMdMedium,
            color = ElementTheme.colors.textSecondary,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = ElementTheme.colors.bgCanvasDefault,
            border = BorderStroke(1.dp, ElementTheme.colors.borderDisabled),
            shadowElevation = 3.dp,
        ) {
            Column(content = content)
        }
    }
}

@PreviewsDayNight
@Composable
internal fun RoomDetailsEditViewPreview(@PreviewParameter(RoomDetailsEditStateProvider::class) state: RoomDetailsEditState) = ElementPreview {
    RoomDetailsEditView(
        state = state,
        onDone = {},
        onOpenSecurityAndPrivacy = {},
        onOpenRolesAndPermissions = {},
    )
}
