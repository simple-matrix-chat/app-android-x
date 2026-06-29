/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.rolesandpermissions.impl.roles

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import io.element.android.features.rolesandpermissions.impl.R
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.components.async.AsyncIndicatorHost
import io.element.android.libraries.designsystem.components.async.rememberAsyncIndicatorState
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.libraries.designsystem.components.dialogs.SaveChangesDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Checkbox
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.SearchBar
import io.element.android.libraries.designsystem.theme.components.SearchBarResultState
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.utils.CommonDrawables
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.room.RoomMembershipState
import io.element.android.libraries.matrix.api.room.getBestName
import io.element.android.libraries.matrix.api.room.toMatrixUser
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.components.SelectedUsersRowList
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeRolesView(
    state: ChangeRolesState,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.isSearchActive) {
        state.eventSink(ChangeRolesEvent.Exit)
    }
    Box(modifier = modifier) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            containerColor = ElementTheme.colors.bgSubtleSecondary,
            topBar = {
                AnimatedVisibility(visible = !state.isSearchActive) {
                    ChangeRolesTopBar(
                        title = changeRolesTitle(state.role),
                        onBackClick = { state.eventSink(ChangeRolesEvent.Exit) },
                        onSaveClick = { state.eventSink(ChangeRolesEvent.Save) },
                        saveEnabled = state.hasPendingChanges,
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .fillMaxSize(),
            ) {
                val lazyListState = rememberLazyListState()
                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 12.dp),
                    placeHolderTitle = stringResource(CommonStrings.common_search_for_someone),
                    queryState = state.searchQuery,
                    active = state.isSearchActive,
                    onActiveChange = { state.eventSink(ChangeRolesEvent.ToggleSearchActive) },
                    resultState = state.searchResults,
                ) { members ->
                    SearchResultsList(
                        modifier = Modifier.fillMaxSize(),
                        currentRole = state.role,
                        lazyListState = lazyListState,
                        searchResults = members,
                        selectedUsers = state.selectedUsers,
                        canRemoveMember = state.canChangeMemberRole,
                        onToggleSelection = { state.eventSink(ChangeRolesEvent.UserSelectionToggled(it.toMatrixUser())) },
                        selectedUsersList = {},
                        contentPadding = PaddingValues(bottom = 24.dp),
                    )
                }
                AnimatedVisibility(
                    visible = !state.isSearchActive,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        SearchResultsList(
                            modifier = Modifier.fillMaxSize(),
                            currentRole = state.role,
                            lazyListState = lazyListState,
                            searchResults = (state.searchResults as? SearchBarResultState.Results)?.results ?: MembersByRole(),
                            selectedUsers = state.selectedUsers,
                            canRemoveMember = state.canChangeMemberRole,
                            onToggleSelection = { state.eventSink(ChangeRolesEvent.UserSelectionToggled(it.toMatrixUser())) },
                            selectedUsersList = { users ->
                                SelectedUsersSection(
                                    selectedUsers = users,
                                    onUserRemove = {
                                        state.eventSink(ChangeRolesEvent.UserSelectionToggled(it))
                                    },
                                    canDeselect = { state.canChangeMemberRole(it.userId) },
                                )
                            },
                            contentPadding = PaddingValues(bottom = 24.dp),
                        )
                    }
                }
            }
        }

        val asyncIndicatorState = rememberAsyncIndicatorState()
        AsyncIndicatorHost(modifier = Modifier.statusBarsPadding(), asyncIndicatorState)
        AsyncActionView(
            async = state.savingState,
            onSuccess = {},
            confirmationDialog = { confirming ->
                when (confirming) {
                    is AsyncAction.ConfirmingCancellation -> {
                        SaveChangesDialog(
                            onSaveClick = { state.eventSink(ChangeRolesEvent.Save) },
                            onDiscardClick = { state.eventSink(ChangeRolesEvent.Exit) },
                            onDismiss = { state.eventSink(ChangeRolesEvent.CloseDialog) },
                        )
                    }
                    is ConfirmingModifyingOwners -> {
                        ConfirmationDialog(
                            title = stringResource(R.string.screen_room_change_role_confirm_change_owners_title),
                            content = stringResource(R.string.screen_room_change_role_confirm_change_owners_description),
                            submitText = stringResource(CommonStrings.action_continue),
                            onSubmitClick = { state.eventSink(ChangeRolesEvent.Save) },
                            onDismiss = { state.eventSink(ChangeRolesEvent.CloseDialog) },
                            destructiveSubmit = true,
                        )
                    }
                    is ConfirmingModifyingAdmins -> {
                        ConfirmationDialog(
                            title = stringResource(R.string.screen_room_change_role_confirm_add_admin_title),
                            content = stringResource(R.string.screen_room_change_role_confirm_add_admin_description),
                            onSubmitClick = { state.eventSink(ChangeRolesEvent.Save) },
                            onDismiss = { state.eventSink(ChangeRolesEvent.CloseDialog) }
                        )
                    }
                }
            },
            errorMessage = {
                stringResource(CommonStrings.error_unknown)
            },
            onErrorDismiss = {
                state.eventSink(ChangeRolesEvent.CloseDialog)
            },
        )
    }
}

@Composable
private fun changeRolesTitle(role: RoomMember.Role): String {
    return when (role) {
        is RoomMember.Role.Owner -> stringResource(R.string.screen_room_change_role_owners_title)
        RoomMember.Role.Admin -> stringResource(R.string.screen_room_change_role_administrators_title)
        RoomMember.Role.Moderator -> stringResource(R.string.screen_room_change_role_moderators_title)
        RoomMember.Role.User -> error("This should never be reached")
    }
}

@Composable
private fun ChangeRolesTopBar(
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
            enabled = saveEnabled,
            onClick = onSaveClick,
        )
    }
}

@Composable
private fun SearchResultsList(
    modifier: Modifier = Modifier,
    currentRole: RoomMember.Role,
    searchResults: MembersByRole,
    selectedUsers: ImmutableList<MatrixUser>,
    canRemoveMember: (UserId) -> Boolean,
    onToggleSelection: (RoomMember) -> Unit,
    lazyListState: LazyListState,
    selectedUsersList: @Composable (ImmutableList<MatrixUser>) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = contentPadding,
    ) {
        item {
            selectedUsersList(selectedUsers)
        }
        if (searchResults.owners.isNotEmpty()) {
            stickyHeader { ListSectionHeader(text = stringResource(R.string.screen_room_roles_and_permissions_owners)) }
            items(searchResults.owners, key = { it.userId }) { roomMember ->
                ListMemberItem(
                    roomMember = roomMember,
                    canRemoveMember = canRemoveMember,
                    onToggleSelection = onToggleSelection,
                    selectedUsers = selectedUsers
                )
            }
            if (currentRole !is RoomMember.Role.Owner) {
                item {
                    ListSectionFooter(text = stringResource(R.string.screen_room_change_role_moderators_owner_section_footer))
                }
            }
        }
        if (searchResults.admins.isNotEmpty()) {
            stickyHeader { ListSectionHeader(text = stringResource(R.string.screen_room_change_role_section_administrators)) }
            // Add a footer for the admin section in change role to moderator screen
            items(searchResults.admins, key = { it.userId }) { roomMember ->
                ListMemberItem(
                    roomMember = roomMember,
                    canRemoveMember = canRemoveMember,
                    onToggleSelection = onToggleSelection,
                    selectedUsers = selectedUsers
                )
            }
            if (currentRole == RoomMember.Role.Moderator) {
                item {
                    ListSectionFooter(text = stringResource(R.string.screen_room_change_role_moderators_admin_section_footer))
                }
            }
        }
        if (searchResults.moderators.isNotEmpty()) {
            stickyHeader { ListSectionHeader(text = stringResource(R.string.screen_room_change_role_section_moderators)) }
            items(searchResults.moderators, key = { it.userId }) { roomMember ->
                ListMemberItem(
                    roomMember = roomMember,
                    canRemoveMember = canRemoveMember,
                    onToggleSelection = onToggleSelection,
                    selectedUsers = selectedUsers
                )
            }
        }
        if (searchResults.members.isNotEmpty()) {
            stickyHeader { ListSectionHeader(text = stringResource(R.string.screen_room_change_role_section_users)) }
            items(searchResults.members, key = { it.userId }) { roomMember ->
                ListMemberItem(
                    roomMember = roomMember,
                    canRemoveMember = canRemoveMember,
                    onToggleSelection = onToggleSelection,
                    selectedUsers = selectedUsers
                )
            }
        }
    }
}

@Composable
private fun ListSectionHeader(text: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .background(ElementTheme.colors.bgSubtleSecondary)
            .padding(start = 24.dp, top = 14.dp, end = 24.dp, bottom = 8.dp),
        text = text,
        color = ElementTheme.colors.textSecondary,
        style = ElementTheme.typography.fontBodySmMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ListSectionFooter(text: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .background(ElementTheme.colors.bgSubtleSecondary)
            .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 12.dp),
        text = text,
        color = ElementTheme.colors.textSecondary,
        style = ElementTheme.typography.fontBodySmRegular,
    )
}

@Composable
private fun ListMemberItem(
    roomMember: RoomMember,
    canRemoveMember: (UserId) -> Boolean,
    onToggleSelection: (RoomMember) -> Unit,
    selectedUsers: ImmutableList<MatrixUser>,
) {
    val canToggle = canRemoveMember(roomMember.userId)
    val trailingContent: @Composable (() -> Unit) = {
        if (canToggle) {
            Checkbox(
                checked = selectedUsers.any { it.userId == roomMember.userId },
                onCheckedChange = { onToggleSelection(roomMember) },
            )
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ElementTheme.colors.bgCanvasDefault),
    ) {
        MemberRow(
            modifier = Modifier.clickable(enabled = canToggle, onClick = { onToggleSelection(roomMember) }),
            avatarData = roomMember.getAvatarData(size = AvatarSize.UserListItem),
            name = roomMember.getBestName(),
            userId = roomMember.userId.value.takeIf { roomMember.displayName?.isNotBlank() == true },
            isPending = roomMember.membership == RoomMembershipState.INVITE,
            enabled = canToggle,
            trailingContent = trailingContent,
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 76.dp),
            color = ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.22f),
        )
    }
}

@Composable
private fun MemberRow(
    avatarData: AvatarData,
    name: String,
    userId: String?,
    isPending: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            avatarData = avatarData,
            avatarType = AvatarType.User,
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Name
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (enabled) ElementTheme.colors.textPrimary else ElementTheme.colors.textDisabled,
                    style = ElementTheme.typography.fontBodyLgRegular,
                )
                // Invitation pending marker
                if (isPending) {
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = stringResource(id = R.string.screen_room_member_list_pending_status),
                        style = ElementTheme.typography.fontBodySmRegular.copy(fontStyle = FontStyle.Italic),
                        color = if (enabled) ElementTheme.colors.textSecondary else ElementTheme.colors.textDisabled
                    )
                }
            }
            // Id
            userId?.let {
                Text(
                    text = userId,
                    color = if (enabled) ElementTheme.colors.textSecondary else ElementTheme.colors.textDisabled,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ElementTheme.typography.fontBodySmRegular,
                )
            }
        }
        trailingContent?.invoke()
    }
}

@Composable
private fun SelectedUsersSection(
    selectedUsers: ImmutableList<MatrixUser>,
    onUserRemove: (MatrixUser) -> Unit,
    canDeselect: (MatrixUser) -> Boolean,
) {
    if (selectedUsers.isEmpty()) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        border = BorderStroke(1.dp, ElementTheme.colors.borderDisabled),
        shadowElevation = 2.dp,
    ) {
        SelectedUsersRowList(
            modifier = Modifier.padding(top = 12.dp, bottom = 10.dp),
            selectedUsers = selectedUsers,
            onUserRemove = onUserRemove,
            canDeselect = canDeselect,
            autoScroll = true,
            contentPadding = PaddingValues(horizontal = 16.dp),
        )
    }
}

@PreviewsDayNight
@Composable
internal fun ChangeRolesViewPreview(@PreviewParameter(ChangeRolesStateProvider::class) state: ChangeRolesState) {
    ElementPreview {
        ChangeRolesView(
            state = state
        )
    }
}

@PreviewsDayNight
@Composable
internal fun PendingMemberRowWithLongNamePreview() {
    ElementPreview(
        drawableFallbackForImages = CommonDrawables.sample_avatar,
    ) {
        MemberRow(
            avatarData = AvatarData("userId", "A very long name that should be truncated", "https://example.com/avatar.png", AvatarSize.UserListItem),
            name = "A very long name that should be truncated",
            userId = "@alice:matrix.org",
            isPending = true,
            trailingContent = {
                Checkbox(
                    checked = true,
                    onCheckedChange = {},
                    enabled = true,
                )
            }
        )
    }
}
