/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.rolesandpermissions.impl.permissions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.rolesandpermissions.impl.R
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.components.dialogs.SaveChangesDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.DropdownMenu
import io.element.android.libraries.designsystem.theme.components.DropdownMenuItem
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.ui.strings.CommonStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeRoomPermissionsView(
    state: ChangeRoomPermissionsState,
    onComplete: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        state.eventSink(ChangeRoomPermissionsEvent.Exit)
    }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
        topBar = {
            ChangeRoomPermissionsTopBar(
                title = stringResource(R.string.screen_room_change_permissions_title),
                onBackClick = { state.eventSink(ChangeRoomPermissionsEvent.Exit) },
                onSaveClick = { state.eventSink(ChangeRoomPermissionsEvent.Save) },
                saveEnabled = state.hasChanges,
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(state.itemsBySection.entries.toList()) { (section, items) ->
                PermissionSection(
                    title = titleForSection(section),
                    items = items,
                    state = state,
                )
            }
        }
    }

    AsyncActionView(
        async = state.saveAction,
        onSuccess = { onComplete(it) },
        confirmationDialog = { confirming ->
            when (confirming) {
                is AsyncAction.ConfirmingCancellation -> {
                    SaveChangesDialog(
                        onSaveClick = { state.eventSink(ChangeRoomPermissionsEvent.Save) },
                        onDiscardClick = { state.eventSink(ChangeRoomPermissionsEvent.Exit) },
                        onDismiss = { state.eventSink(ChangeRoomPermissionsEvent.ResetPendingActions) },
                    )
                }
            }
        },
        onErrorDismiss = { state.eventSink(ChangeRoomPermissionsEvent.ResetPendingActions) }
    )
}

@Composable
private fun ChangeRoomPermissionsTopBar(
    title: String,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    saveEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp),
            onClick = onBackClick,
        ) {
            Icon(
                imageVector = CompoundIcons.ChevronLeft(),
                contentDescription = stringResource(CommonStrings.action_back),
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 72.dp)
                .semantics { heading() },
            text = title,
            style = ElementTheme.typography.fontHeadingSmMedium,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            text = stringResource(CommonStrings.action_save),
            onClick = onSaveClick,
            enabled = saveEnabled,
        )
    }
}

@Composable
private fun PermissionSection(
    title: String,
    items: List<RoomPermissionType>,
    state: ChangeRoomPermissionsState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = title,
            color = ElementTheme.colors.textSecondary,
            style = ElementTheme.typography.fontBodySmMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = ElementTheme.colors.bgCanvasDefault,
            border = BorderStroke(1.dp, ElementTheme.colors.borderDisabled),
            shadowElevation = 0.dp,
        ) {
            Column {
                items.forEachIndexed { index, permissionType ->
                    PermissionPickerRow(
                        title = titleForType(permissionType),
                        selectedRole = state.selectedRoleForType(permissionType),
                        selectableRoles = state.selectableRoles,
                        enabled = state.canChangePermission(permissionType),
                        onSelectRole = { role ->
                            state.eventSink(
                                ChangeRoomPermissionsEvent.ChangeMinimumRoleForAction(
                                    action = permissionType,
                                    role = role,
                                )
                            )
                        },
                    )
                    if (index < items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = ElementTheme.colors.borderDisabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionPickerRow(
    title: String,
    selectedRole: SelectableRole?,
    selectableRoles: List<SelectableRole>,
    enabled: Boolean,
    onSelectRole: (SelectableRole) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { isDropdownExpanded = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = ElementTheme.typography.fontBodyLgRegular,
            color = if (enabled) ElementTheme.colors.textPrimary else ElementTheme.colors.textDisabled,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Box {
            Row(
                modifier = Modifier
                    .clickable(enabled = enabled) { isDropdownExpanded = true }
                    .padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedRole?.getText().orEmpty(),
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = if (enabled) ElementTheme.colors.textSecondary else ElementTheme.colors.textDisabled,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = CompoundIcons.ChevronDown(),
                    contentDescription = null,
                    tint = if (enabled) ElementTheme.colors.iconSecondary else ElementTheme.colors.iconDisabled,
                )
            }
            DropdownMenu(
                expanded = isDropdownExpanded,
                minWidth = 180.dp,
                onDismissRequest = { isDropdownExpanded = false },
            ) {
                selectableRoles.forEach { option ->
                    DropdownMenuItem(
                        enabled = enabled,
                        text = {
                            Text(
                                text = option.getText(),
                                style = ElementTheme.typography.fontBodyMdRegular,
                            )
                        },
                        trailingIcon = {
                            if (option == selectedRole) {
                                Icon(
                                    imageVector = CompoundIcons.Check(),
                                    contentDescription = null,
                                    tint = ElementTheme.colors.iconAccentPrimary,
                                )
                            }
                        },
                        onClick = {
                            onSelectRole(option)
                            isDropdownExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun titleForSection(section: RoomPermissionsSection): String = when (section) {
    RoomPermissionsSection.EditDetails -> stringResource(R.string.screen_room_change_permissions_room_details)
    RoomPermissionsSection.MessagesAndContent -> stringResource(R.string.screen_room_change_permissions_messages_and_content)
    RoomPermissionsSection.ManageMembers -> stringResource(R.string.screen_room_change_permissions_member_moderation)
    RoomPermissionsSection.ManageSpace -> stringResource(R.string.screen_room_change_permissions_manage_space)
}

@Composable
private fun titleForType(type: RoomPermissionType): String = when (type) {
    RoomPermissionType.INVITE -> stringResource(R.string.screen_room_change_permissions_invite_people)
    RoomPermissionType.KICK -> stringResource(R.string.screen_room_change_permissions_remove_people)
    RoomPermissionType.BAN -> stringResource(R.string.screen_room_change_permissions_ban_people)
    RoomPermissionType.SEND_EVENTS -> stringResource(R.string.screen_room_change_permissions_send_messages)
    RoomPermissionType.REDACT_EVENTS -> stringResource(R.string.screen_room_change_permissions_delete_messages)
    RoomPermissionType.ROOM_NAME -> stringResource(R.string.screen_room_change_permissions_room_name)
    RoomPermissionType.ROOM_AVATAR -> stringResource(R.string.screen_room_change_permissions_room_avatar)
    RoomPermissionType.ROOM_TOPIC -> stringResource(R.string.screen_room_change_permissions_room_topic)
    RoomPermissionType.SPACE_MANAGE_ROOMS -> stringResource(R.string.screen_room_change_permissions_manage_space_rooms)
}

@PreviewsDayNight
@Composable
internal fun ChangeRoomPermissionsViewPreview(@PreviewParameter(ChangeRoomPermissionsStateProvider::class) state: ChangeRoomPermissionsState) {
    ElementPreview {
        ChangeRoomPermissionsView(
            state = state,
            onComplete = {},
        )
    }
}
