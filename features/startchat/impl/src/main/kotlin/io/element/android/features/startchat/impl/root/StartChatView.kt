/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.startchat.impl.root

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.startchat.api.ConfirmingStartDmWithMatrixUser
import io.element.android.features.startchat.impl.R
import io.element.android.features.startchat.impl.userlist.UserListEvents
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.components.async.AsyncActionViewDefaults
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.SearchBarResultState
import io.element.android.libraries.designsystem.theme.components.Switch
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.createroom.MomentRoomKind
import io.element.android.libraries.matrix.ui.components.CreateDmConfirmationBottomSheet
import io.element.android.libraries.matrix.ui.components.MatrixUserRow
import io.element.android.libraries.matrix.ui.model.getBestName
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.libraries.usersearch.api.UserSearchResult
import kotlinx.collections.immutable.persistentListOf

@Composable
fun StartChatView(
    state: StartChatState,
    onCloseClick: () -> Unit,
    onOpenDM: (RoomId) -> Unit,
    onInviteFriendsClick: () -> Unit,
    onJoinByAddressClick: () -> Unit,
    onRoomDirectorySearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchPlaceholder = stringResource(R.string.screen_start_chat_moment_direct_placeholder)
    var selectedMode by rememberSaveable { mutableStateOf(MomentStartChatMode.Direct) }
    var roomName by rememberSaveable { mutableStateOf("") }
    var isPublicRoom by rememberSaveable { mutableStateOf(false) }

    fun selectMode(mode: MomentStartChatMode) {
        selectedMode = mode
        when (mode) {
            MomentStartChatMode.Direct -> Unit
            MomentStartChatMode.Group -> isPublicRoom = false
            MomentStartChatMode.Channel -> isPublicRoom = true
        }
    }

    fun submit() {
        when (selectedMode) {
            MomentStartChatMode.Direct -> {
                state.directLookupState().readyResult?.matrixUser?.let {
                    state.eventSink(StartChatEvents.StartDM(it))
                }
            }
            MomentStartChatMode.Group,
            MomentStartChatMode.Channel -> {
                selectedMode.momentRoomKind?.let { momentRoomKind ->
                    state.eventSink(
                        StartChatEvents.CreateMomentRoom(
                            name = roomName,
                            momentRoomKind = momentRoomKind,
                            isPublic = isPublicRoom,
                        )
                    )
                }
            }
        }
    }

    val directLookupState = state.directLookupState()
    val isCreatingMomentRoom = state.createMomentRoomAction is AsyncAction.Loading
    val isStartingDirectChat = state.startDmAction is AsyncAction.Loading
    val canSubmit = when (selectedMode) {
        MomentStartChatMode.Direct -> directLookupState.readyResult != null && !isStartingDirectChat
        MomentStartChatMode.Group,
        MomentStartChatMode.Channel -> roomName.isNotBlank() && !isCreatingMomentRoom
    }
    val showPrimaryProgress = when (selectedMode) {
        MomentStartChatMode.Direct -> isStartingDirectChat
        MomentStartChatMode.Group,
        MomentStartChatMode.Channel -> isCreatingMomentRoom
    }

    Scaffold(
        modifier = modifier.fillMaxWidth(),
        bottomBar = {
            MomentPrimaryAction(
                mode = selectedMode,
                enabled = canSubmit,
                showProgress = showPrimaryProgress,
                onClick = ::submit,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            MomentStartChatHeader(onCloseClick = onCloseClick)
            MomentStartChatTitle(mode = selectedMode)
            MomentModeTabs(
                selectedMode = selectedMode,
                onModeSelected = ::selectMode,
            )
            MomentModeCard(mode = selectedMode)
            when (selectedMode) {
                MomentStartChatMode.Direct -> {
                    MomentDirectInputSection(
                        state = state,
                        searchPlaceholder = searchPlaceholder,
                        lookupState = directLookupState,
                    )
                }
                MomentStartChatMode.Group,
                MomentStartChatMode.Channel -> {
                    MomentRoomNameInput(
                        mode = selectedMode,
                        roomName = roomName,
                        onRoomNameChange = { roomName = it },
                    )
                    MomentVisibilityToggle(
                        mode = selectedMode,
                        isPublicRoom = isPublicRoom,
                        onPublicRoomChange = { isPublicRoom = it },
                    )
                }
            }
            MomentFooterCard(mode = selectedMode)
            if (selectedMode == MomentStartChatMode.Direct) {
                MomentPhonebookContacts(
                    state = state,
                )
                MomentRecentDirectRooms(
                    state = state,
                    onDmClick = onOpenDM,
                )
            }
        }
    }

    AsyncActionView(
        async = state.startDmAction,
        progressDialog = {
            AsyncActionViewDefaults.ProgressDialog(
                progressText = stringResource(CommonStrings.common_starting_chat),
            )
        },
        onSuccess = { onOpenDM(it) },
        errorMessage = { stringResource(R.string.screen_start_chat_error_starting_chat) },
        onRetry = {
            state.userListState.selectedUsers.firstOrNull()
                ?.let { state.eventSink(StartChatEvents.StartDM(it)) }
            // Cancel start DM if there is no more selected user (should not happen)
                ?: state.eventSink(StartChatEvents.CancelStartDM)
        },
        onErrorDismiss = { state.eventSink(StartChatEvents.CancelStartDM) },
        confirmationDialog = { data ->
            if (data is ConfirmingStartDmWithMatrixUser) {
                CreateDmConfirmationBottomSheet(
                    matrixUser = data.matrixUser,
                    isUserIdentityUnknown = data.isUserIdentityUnknown,
                    onSendInvite = {
                        state.eventSink(StartChatEvents.StartDM(data.matrixUser))
                    },
                    onDismiss = {
                        state.eventSink(StartChatEvents.CancelStartDM)
                    },
                )
            }
        },
    )
    AsyncActionView(
        async = state.createMomentRoomAction,
        progressDialog = {
            AsyncActionViewDefaults.ProgressDialog(
                progressText = stringResource(CommonStrings.common_creating_room),
            )
        },
        onSuccess = { onOpenDM(it) },
        errorMessage = { stringResource(R.string.screen_start_chat_error_creating_room) },
        onRetry = ::submit,
        onErrorDismiss = { state.eventSink(StartChatEvents.CancelCreateMomentRoom) },
        confirmationDialog = {},
    )
}

@Composable
private fun MomentStartChatHeader(
    onCloseClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(ElementTheme.colors.bgSubtleSecondary, CircleShape)
                .clickable(onClick = onCloseClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = CompoundIcons.Close(),
                contentDescription = stringResource(CommonStrings.action_close),
                tint = ElementTheme.colors.iconPrimary,
            )
        }

        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.screen_start_chat_moment_title),
            style = ElementTheme.typography.fontHeadingSmMedium.copy(fontWeight = FontWeight.SemiBold),
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.width(36.dp))
    }
}

@Composable
private fun MomentStartChatTitle(mode: MomentStartChatMode) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(mode.titleRes),
            style = ElementTheme.typography.fontHeadingMdBold,
            color = ElementTheme.colors.textPrimary,
        )
        Text(
            text = stringResource(mode.descriptionRes),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun MomentModeTabs(
    selectedMode: MomentStartChatMode,
    onModeSelected: (MomentStartChatMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MomentStartChatMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            val interactionSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .background(
                        color = if (isSelected) Color.Black else ElementTheme.colors.bgSubtleSecondary,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .semantics(mergeDescendants = true) {}
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) { onModeSelected(mode) }
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    imageVector = mode.icon(),
                    contentDescription = null,
                    tint = if (isSelected) Color.White else ElementTheme.colors.iconPrimary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = stringResource(mode.tabTitleRes),
                    style = ElementTheme.typography.fontBodySmMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) Color.White else ElementTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MomentModeCard(mode: MomentStartChatMode) {
    MomentInfoCard(
        modifier = Modifier.padding(horizontal = 20.dp),
        icon = mode.icon(),
        iconTint = mode.accentColor,
        title = stringResource(mode.titleRes),
        body = stringResource(mode.cardSubtitleRes),
    )
}

@Composable
private fun MomentDirectLookupCard(
    state: MomentDirectLookupState,
    modifier: Modifier = Modifier,
) {
    MomentInfoCard(
        modifier = modifier,
        icon = state.icon(),
        iconTint = state.accentColor,
        title = stringResource(state.titleRes),
        body = stringResource(state.bodyRes),
        content = {
            if (state is MomentDirectLookupState.Ready) {
                MomentDirectReadyUser(result = state.result)
            }
            if (state is MomentDirectLookupState.Searching) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(20.dp),
                    color = state.accentColor,
                )
            }
        },
    )
}

@Composable
private fun MomentInfoCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    body: String,
    content: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ElementTheme.colors.bgSubtleSecondary, RoundedCornerShape(24.dp))
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(iconTint.copy(alpha = 0.14f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = ElementTheme.typography.fontBodyLgMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ElementTheme.colors.textPrimary,
            )
            Text(
                text = body,
                style = ElementTheme.typography.fontBodyMdRegular,
                color = ElementTheme.colors.textSecondary,
            )
            content()
        }
    }
}

@Composable
private fun MomentDirectReadyUser(result: UserSearchResult) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = result.matrixUser.getBestName(),
            style = ElementTheme.typography.fontBodyMdMedium.copy(fontWeight = FontWeight.SemiBold),
            color = ElementTheme.colors.textPrimary,
        )
        Text(
            text = result.subtitle?.takeIf { it.isNotBlank() } ?: result.matrixUser.userId.value,
            style = ElementTheme.typography.fontBodySmRegular,
            color = ElementTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun MomentDirectInputSection(
    state: StartChatState,
    searchPlaceholder: String,
    lookupState: MomentDirectLookupState,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(MomentStartChatMode.Direct.fieldLabelRes),
            style = ElementTheme.typography.fontBodyMdMedium.copy(fontWeight = FontWeight.SemiBold),
            color = ElementTheme.colors.textPrimary,
        )
        MomentDirectSearchInput(
            queryState = state.userListState.searchQuery,
            placeholder = searchPlaceholder,
            isSearchActive = state.userListState.isSearchActive,
            onSearchActiveChange = {
                state.userListState.eventSink(UserListEvents.OnSearchActiveChanged(it))
            },
        )
        MomentDirectLookupCard(
            modifier = Modifier.fillMaxWidth(),
            state = lookupState,
        )
    }
}

@Composable
private fun MomentDirectSearchInput(
    queryState: TextFieldState,
    placeholder: String,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
) {
    BasicTextField(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(ElementTheme.colors.bgSubtleSecondary, RoundedCornerShape(18.dp))
            .onFocusChanged { focusState ->
                if (focusState.isFocused && !isSearchActive) {
                    onSearchActiveChange(true)
                }
            }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        state = queryState,
        textStyle = ElementTheme.typography.fontBodyLgMedium.copy(color = ElementTheme.colors.textPrimary),
        cursorBrush = SolidColor(ElementTheme.colors.textPrimary),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
        ),
        decorator = { innerTextField ->
            Box {
                if (queryState.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = ElementTheme.typography.fontBodyLgMedium,
                        color = ElementTheme.colors.textSecondary,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun MomentRoomNameInput(
    mode: MomentStartChatMode,
    roomName: String,
    onRoomNameChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(mode.fieldLabelRes),
            style = ElementTheme.typography.fontBodyMdMedium.copy(fontWeight = FontWeight.SemiBold),
            color = ElementTheme.colors.textPrimary,
        )
        BasicTextField(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(ElementTheme.colors.bgSubtleSecondary, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            value = roomName,
            onValueChange = onRoomNameChange,
            singleLine = true,
            textStyle = ElementTheme.typography.fontBodyLgMedium.copy(color = ElementTheme.colors.textPrimary),
            cursorBrush = SolidColor(ElementTheme.colors.textPrimary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (roomName.isEmpty()) {
                        Text(
                            text = stringResource(mode.placeholderRes),
                            style = ElementTheme.typography.fontBodyLgMedium,
                            color = ElementTheme.colors.textSecondary,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun MomentVisibilityToggle(
    mode: MomentStartChatMode,
    isPublicRoom: Boolean,
    onPublicRoomChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .background(ElementTheme.colors.bgSubtleSecondary, RoundedCornerShape(24.dp))
            .clickable { onPublicRoomChange(!isPublicRoom) }
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(
                    mode.visibilityTitleRes(isPublicRoom)
                ),
                style = ElementTheme.typography.fontBodyLgMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ElementTheme.colors.textPrimary,
            )
            Text(
                text = stringResource(
                    mode.visibilityDescriptionRes(isPublicRoom)
                ),
                style = ElementTheme.typography.fontBodyMdRegular,
                color = ElementTheme.colors.textSecondary,
            )
        }
        Switch(
            checked = isPublicRoom,
            onCheckedChange = onPublicRoomChange,
        )
    }
}

@Composable
private fun MomentFooterCard(mode: MomentStartChatMode) {
    MomentInfoCard(
        modifier = Modifier.padding(horizontal = 20.dp),
        icon = CompoundIcons.FavouriteSolid(),
        iconTint = mode.accentColor,
        title = stringResource(mode.footerTitleRes),
        body = stringResource(mode.footerBodyRes),
    )
}

@Composable
private fun MomentPhonebookContacts(
    state: StartChatState,
) {
    if (state.phonebookContacts.isEmpty()) return

    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = stringResource(id = R.string.screen_start_chat_moment_contacts_section_title),
            style = ElementTheme.typography.fontBodyMdMedium,
            color = ElementTheme.colors.textSecondary,
        )
        state.phonebookContacts.forEach { contact ->
            MatrixUserRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ElementTheme.colors.bgSubtleSecondary, RoundedCornerShape(20.dp))
                    .clickable {
                        state.eventSink(StartChatEvents.StartDM(contact.matrixUser))
                    },
                matrixUser = contact.matrixUser,
                subtext = contact.subtitle,
            )
        }
    }
}

@Composable
private fun MomentRecentDirectRooms(
    state: StartChatState,
    onDmClick: (RoomId) -> Unit,
) {
    val phonebookUserIds = state.phonebookContacts.map { it.matrixUser.userId }.toSet()
    val recentDirectRooms = state.userListState.recentDirectRooms
        .filterNot { recentDirectRoom -> recentDirectRoom.matrixUser.userId in phonebookUserIds }
    if (recentDirectRooms.isEmpty()) return

    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = stringResource(id = CommonStrings.common_suggestions),
            style = ElementTheme.typography.fontBodyMdMedium,
            color = ElementTheme.colors.textSecondary,
        )
        recentDirectRooms.forEach { recentDirectRoom ->
            MatrixUserRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ElementTheme.colors.bgSubtleSecondary, RoundedCornerShape(20.dp))
                    .clickable {
                        onDmClick(recentDirectRoom.roomId)
                    },
                matrixUser = recentDirectRoom.matrixUser,
            )
        }
    }
}

@Composable
private fun MomentPrimaryAction(
    mode: MomentStartChatMode,
    enabled: Boolean,
    showProgress: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ElementTheme.colors.bgCanvasDefault)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(mode.primaryButtonRes),
            enabled = enabled,
            showProgress = showProgress,
            onClick = onClick,
        )
    }
}

private enum class MomentStartChatMode(
    @StringRes val tabTitleRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val cardSubtitleRes: Int,
    @StringRes val fieldLabelRes: Int,
    @StringRes val placeholderRes: Int,
    @StringRes val footerTitleRes: Int,
    @StringRes val footerBodyRes: Int,
    @StringRes val primaryButtonRes: Int,
    val momentRoomKind: MomentRoomKind?,
    val accentColor: Color,
) {
    Direct(
        tabTitleRes = R.string.screen_start_chat_moment_direct_tab_title,
        titleRes = R.string.screen_start_chat_moment_direct_title,
        descriptionRes = R.string.screen_start_chat_moment_direct_description,
        cardSubtitleRes = R.string.screen_start_chat_moment_direct_card_subtitle,
        fieldLabelRes = R.string.screen_start_chat_moment_direct_field_label,
        placeholderRes = R.string.screen_start_chat_moment_direct_placeholder,
        footerTitleRes = R.string.screen_start_chat_moment_direct_footer_title,
        footerBodyRes = R.string.screen_start_chat_moment_direct_footer_body,
        primaryButtonRes = R.string.screen_start_chat_moment_direct_primary_action,
        momentRoomKind = null,
        accentColor = MomentDirectAccent,
    ),
    Group(
        tabTitleRes = R.string.screen_start_chat_moment_group_tab_title,
        titleRes = R.string.screen_start_chat_moment_group_title,
        descriptionRes = R.string.screen_start_chat_moment_group_description,
        cardSubtitleRes = R.string.screen_start_chat_moment_group_card_subtitle,
        fieldLabelRes = R.string.screen_start_chat_moment_group_field_label,
        placeholderRes = R.string.screen_start_chat_moment_group_placeholder,
        footerTitleRes = R.string.screen_start_chat_moment_group_footer_title,
        footerBodyRes = R.string.screen_start_chat_moment_group_footer_body,
        primaryButtonRes = R.string.screen_start_chat_moment_group_primary_action,
        momentRoomKind = MomentRoomKind.Group,
        accentColor = MomentGroupAccent,
    ),
    Channel(
        tabTitleRes = R.string.screen_start_chat_moment_channel_tab_title,
        titleRes = R.string.screen_start_chat_moment_channel_title,
        descriptionRes = R.string.screen_start_chat_moment_channel_description,
        cardSubtitleRes = R.string.screen_start_chat_moment_channel_card_subtitle,
        fieldLabelRes = R.string.screen_start_chat_moment_channel_field_label,
        placeholderRes = R.string.screen_start_chat_moment_channel_placeholder,
        footerTitleRes = R.string.screen_start_chat_moment_channel_footer_title,
        footerBodyRes = R.string.screen_start_chat_moment_channel_footer_body,
        primaryButtonRes = R.string.screen_start_chat_moment_channel_primary_action,
        momentRoomKind = MomentRoomKind.Channel,
        accentColor = MomentChannelAccent,
    ),
}

private sealed interface MomentDirectLookupState {
    val titleRes: Int
    val bodyRes: Int
    val accentColor: Color
    val readyResult: UserSearchResult?
        get() = null

    data object Idle : MomentDirectLookupState {
        override val titleRes = R.string.screen_start_chat_moment_direct_hint_title
        override val bodyRes = R.string.screen_start_chat_moment_direct_hint_body
        override val accentColor = MomentDirectAccent
    }

    data object Searching : MomentDirectLookupState {
        override val titleRes = R.string.screen_start_chat_moment_direct_searching_title
        override val bodyRes = R.string.screen_start_chat_moment_direct_searching_body
        override val accentColor = MomentDirectAccent
    }

    data class Ready(
        val result: UserSearchResult,
    ) : MomentDirectLookupState {
        override val titleRes = R.string.screen_start_chat_moment_direct_ready_title
        override val bodyRes = R.string.screen_start_chat_moment_direct_ready_body
        override val accentColor = MomentReadyAccent
        override val readyResult = result
    }

    data object NotFound : MomentDirectLookupState {
        override val titleRes = R.string.screen_start_chat_moment_direct_not_found_title
        override val bodyRes = R.string.screen_start_chat_moment_direct_not_found_body
        override val accentColor = MomentWarningAccent
    }
}

@Composable
private fun MomentDirectLookupState.icon(): ImageVector {
    return when (this) {
        MomentDirectLookupState.Idle -> CompoundIcons.InfoSolid()
        MomentDirectLookupState.Searching -> CompoundIcons.Time()
        is MomentDirectLookupState.Ready -> CompoundIcons.CheckCircle()
        MomentDirectLookupState.NotFound -> CompoundIcons.Warning()
    }
}

private fun StartChatState.directLookupState(): MomentDirectLookupState {
    val query = userListState.searchQuery.text.toString().trim()
    if (!userListState.isSearchActive || query.isEmpty()) {
        return MomentDirectLookupState.Idle
    }
    if (userListState.showSearchLoader) {
        return MomentDirectLookupState.Searching
    }
    val results = when (val resultState = userListState.searchResults) {
        is SearchBarResultState.Results -> resultState.results
        else -> persistentListOf()
    }
    results.selectBestMomentDirectResult(query)?.let {
        return MomentDirectLookupState.Ready(it)
    }
    if (userListState.searchResults is SearchBarResultState.NoResultsFound) {
        return MomentDirectLookupState.NotFound
    }
    return MomentDirectLookupState.Searching
}

private fun List<UserSearchResult>.selectBestMomentDirectResult(query: String): UserSearchResult? {
    if (isEmpty()) return null
    val trimmedQuery = query.trim()
    val localPartQuery = trimmedQuery.toLocalPartCandidate()
    val queryDigits = trimmedQuery.filter { it.isDigit() }

    return firstOrNull { it.matrixUser.userId.value.equals(trimmedQuery, ignoreCase = true) }
        ?: firstOrNull { it.matrixUser.displayName?.trim()?.equals(trimmedQuery, ignoreCase = true) == true }
        ?: firstOrNull { it.matrixUser.userId.value.toLocalPartCandidate() == localPartQuery }
        ?: firstOrNull {
            queryDigits.length >= MIN_MOMENT_DIRECT_PHONE_MATCH_DIGITS &&
                it.subtitle?.filter { char -> char.isDigit() } == queryDigits
        }
        ?: sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.matrixUser.getBestName() }).first()
}

private fun String.toLocalPartCandidate(): String {
    return trim()
        .removePrefix("@")
        .substringBefore(":")
        .lowercase()
}

@Composable
private fun MomentStartChatMode.icon(): ImageVector {
    return when (this) {
        MomentStartChatMode.Direct -> CompoundIcons.ChatSolid()
        MomentStartChatMode.Group -> CompoundIcons.UserProfileSolid()
        MomentStartChatMode.Channel -> CompoundIcons.Public()
    }
}

@StringRes
private fun MomentStartChatMode.visibilityTitleRes(isPublicRoom: Boolean): Int {
    return when (this) {
        MomentStartChatMode.Direct -> error("Direct chats do not expose room visibility")
        MomentStartChatMode.Group -> if (isPublicRoom) {
            R.string.screen_start_chat_moment_visibility_public_group_title
        } else {
            R.string.screen_start_chat_moment_visibility_private_group_title
        }
        MomentStartChatMode.Channel -> if (isPublicRoom) {
            R.string.screen_start_chat_moment_visibility_public_channel_title
        } else {
            R.string.screen_start_chat_moment_visibility_private_channel_title
        }
    }
}

@StringRes
private fun MomentStartChatMode.visibilityDescriptionRes(isPublicRoom: Boolean): Int {
    return when (this) {
        MomentStartChatMode.Direct -> error("Direct chats do not expose room visibility")
        MomentStartChatMode.Group -> if (isPublicRoom) {
            R.string.screen_start_chat_moment_visibility_public_group_description
        } else {
            R.string.screen_start_chat_moment_visibility_private_group_description
        }
        MomentStartChatMode.Channel -> if (isPublicRoom) {
            R.string.screen_start_chat_moment_visibility_public_channel_description
        } else {
            R.string.screen_start_chat_moment_visibility_private_channel_description
        }
    }
}

private val MomentDirectAccent = Color(0xFF3D78FA)
private val MomentGroupAccent = Color(0xFF21A86A)
private val MomentChannelAccent = Color(0xFFF2911F)
private val MomentReadyAccent = Color(0xFF21A86A)
private val MomentWarningAccent = Color(0xFFD9822B)

private const val MIN_MOMENT_DIRECT_PHONE_MATCH_DIGITS = 10

@PreviewsDayNight
@Composable
internal fun StartChatViewPreview(@PreviewParameter(StartChatStateProvider::class) state: StartChatState) =
    ElementPreview {
        StartChatView(
            state = state,
            onCloseClick = {},
            onOpenDM = {},
            onJoinByAddressClick = {},
            onInviteFriendsClick = {},
            onRoomDirectorySearchClick = {},
        )
    }
