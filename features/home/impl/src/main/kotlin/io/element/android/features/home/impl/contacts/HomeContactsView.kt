/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.home.impl.HomeState
import io.element.android.features.home.impl.HomeStateProvider
import io.element.android.features.home.impl.R
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.model.RoomSummaryDisplayType
import io.element.android.features.home.impl.roomlist.RoomListContentState
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.dialogs.AlertDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.SearchField
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.permissions.api.PermissionsView
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
internal fun HomeContactsView(
    state: HomeState,
    contactsState: HomeContactsState,
    onBackClick: () -> Unit,
    onStartChatClick: () -> Unit,
    onRoomClick: (RoomListRoomSummary) -> Unit,
    onUserClick: (MatrixUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentUserId = state.currentUserAndNeighbors.firstOrNull()?.userId
    val contactsData = contactsState.contacts.dataOrNull()
    val allMomentContacts = contactsData?.momentContacts.orEmpty()
    val allUnavailableContacts = contactsData?.unavailableContacts.orEmpty()
    val momentContactUserIds = allMomentContacts.map { it.matrixUser.userId }.toSet()
    val allContacts = state.roomListState.contentState.directContacts(currentUserId)
        .filterNot { it.directUserId in momentContactUserIds }
    val queryState = rememberTextFieldState()
    val query = queryState.text.toString().trim()
    val filteredContacts = allContacts.filter { contact ->
        query.isEmpty() ||
            contact.name.orEmpty().contains(query, ignoreCase = true) ||
            contact.directUserDisplayName.orEmpty().contains(query, ignoreCase = true) ||
            contact.directUserId?.value.orEmpty().contains(query, ignoreCase = true)
    }
    val filteredMomentContacts = allMomentContacts.filter { contact ->
        query.isEmpty() ||
            contact.matrixUser.displayName.orEmpty().contains(query, ignoreCase = true) ||
            contact.matrixUser.userId.value.contains(query, ignoreCase = true) ||
            contact.subtitle.orEmpty().contains(query, ignoreCase = true)
    }
    val filteredUnavailableContacts = allUnavailableContacts.filter { contact ->
        query.isEmpty() ||
            contact.displayName.contains(query, ignoreCase = true) ||
            contact.phoneNumbers.any { phoneNumber -> phoneNumber.contains(query, ignoreCase = true) }
    }
    val showContactsPermissionCard = contactsState.contactsPermissionState.permissionGranted.not()
    val hasContacts = allContacts.isNotEmpty() || allMomentContacts.isNotEmpty() || allUnavailableContacts.isNotEmpty()
    val hasFilteredContacts = filteredContacts.isNotEmpty() || filteredMomentContacts.isNotEmpty() || filteredUnavailableContacts.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ElementTheme.colors.bgCanvasDefault)
            .statusBarsPadding(),
    ) {
        PermissionsView(
            state = contactsState.contactsPermissionState,
            title = stringResource(R.string.screen_home_contacts_permission_dialog_title),
            content = stringResource(R.string.screen_home_contacts_permission_dialog_message),
        )
        contactsState.unavailableContactDialog?.let { contact ->
            AlertDialog(
                title = stringResource(R.string.screen_home_contacts_unavailable_dialog_title),
                content = stringResource(R.string.screen_home_contacts_unavailable_dialog_message, contact.displayName),
                onDismiss = { contactsState.eventSink(HomeContactsEvent.DismissUnavailableContactDialog) },
            )
        }
        HomeContactsTopBar(onBackClick = onBackClick)
        SearchField(
            state = queryState,
            placeholder = stringResource(R.string.screen_home_contacts_search_placeholder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 8.dp),
        )

        when {
            hasContacts.not() &&
                showContactsPermissionCard.not() &&
                contactsState.contacts.isLoading().not() &&
                contactsState.contacts.isFailure().not() -> {
                HomeContactsEmptyState(
                    title = stringResource(R.string.screen_home_contacts_no_contacts_title),
                    description = stringResource(R.string.screen_home_contacts_no_contacts_message),
                    onStartChatClick = onStartChatClick,
                    modifier = Modifier.weight(1f),
                )
            }
            query.isNotEmpty() && hasContacts && hasFilteredContacts.not() -> {
                HomeContactsEmptyState(
                    title = stringResource(R.string.screen_home_contacts_no_results_title),
                    description = null,
                    onStartChatClick = onStartChatClick,
                    modifier = Modifier.weight(1f),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                ) {
                    if (showContactsPermissionCard) {
                        item {
                            HomeContactsPermissionCard(
                                onRequestContactsPermission = {
                                    contactsState.eventSink(HomeContactsEvent.RequestContactsPermission)
                                },
                            )
                        }
                    }
                    if (contactsState.contacts.isLoading() && contactsData == null) {
                        item {
                            HomeContactsStatusCard(
                                title = stringResource(R.string.screen_home_contacts_syncing_title),
                                description = stringResource(R.string.screen_home_contacts_syncing_message),
                            )
                        }
                    }
                    if (contactsState.contacts.isFailure()) {
                        item {
                            HomeContactsStatusCard(
                                title = stringResource(R.string.screen_home_contacts_sync_error_title),
                                description = stringResource(R.string.screen_home_contacts_sync_error_message),
                                actionText = stringResource(R.string.screen_home_contacts_sync_retry),
                                onActionClick = { contactsState.eventSink(HomeContactsEvent.RetryContactsSync) },
                            )
                        }
                    }
                    items(
                        items = filteredContacts,
                        key = { it.roomId.value },
                    ) { contact ->
                        HomeContactRow(
                            contact = contact,
                            onClick = { onRoomClick(contact) },
                        )
                    }
                    if (filteredMomentContacts.isNotEmpty() || filteredUnavailableContacts.isNotEmpty()) {
                        item {
                            HomeContactsSectionHeader(
                                title = stringResource(R.string.screen_home_contacts_phonebook_section_title),
                            )
                        }
                        items(
                            items = filteredMomentContacts,
                            key = { it.matrixUser.userId.value },
                        ) { contact ->
                            HomeMomentContactRow(
                                contact = contact,
                                onClick = { onUserClick(contact.matrixUser) },
                            )
                        }
                        items(
                            items = filteredUnavailableContacts,
                            key = { it.id },
                        ) { contact ->
                            HomeDeviceContactRow(
                                contact = contact,
                                onClick = {
                                    contactsState.eventSink(HomeContactsEvent.SelectUnavailableContact(contact))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMomentContactRow(
    contact: HomeMomentContact,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomePhonebookAvatar(name = contact.matrixUser.displayName ?: contact.matrixUser.userId.value)
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = contact.matrixUser.displayName ?: contact.matrixUser.userId.value,
                    style = ElementTheme.typography.fontBodyLgMedium,
                    color = ElementTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = contact.subtitle ?: contact.matrixUser.userId.value,
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = CompoundIcons.ChevronRight(),
                contentDescription = null,
                tint = ElementTheme.colors.iconTertiary,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
    }
}

@Composable
private fun HomeContactsTopBar(
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = CompoundIcons.ChevronLeft(),
                contentDescription = stringResource(CommonStrings.action_back),
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
            text = stringResource(R.string.screen_home_contacts_title),
            style = ElementTheme.typography.fontHeadingSmMedium,
            color = ElementTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HomeContactRow(
    contact: RoomListRoomSummary,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                avatarData = contact.avatarData,
                avatarType = AvatarType.Room(
                    heroes = contact.heroes,
                    isTombstoned = contact.isTombstoned,
                ),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = contact.directUserDisplayName ?: contact.name ?: contact.roomId.value,
                    style = ElementTheme.typography.fontBodyLgMedium,
                    color = ElementTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = contact.directUserId?.value ?: contact.name ?: contact.roomId.value,
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = CompoundIcons.ChevronRight(),
                contentDescription = null,
                tint = ElementTheme.colors.iconTertiary,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
    }
}

@Composable
private fun HomeDeviceContactRow(
    contact: HomeDeviceContact,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomePhonebookAvatar(name = contact.displayName)
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = contact.displayName,
                    style = ElementTheme.typography.fontBodyLgMedium,
                    color = ElementTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = contact.primaryPhoneNumber,
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(R.string.screen_home_contacts_unavailable_badge),
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
    }
}

@Composable
private fun HomePhonebookAvatar(
    name: String,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(ElementTheme.colors.bgSubtleSecondary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.initialForAvatar(),
            style = ElementTheme.typography.fontBodyLgMedium,
            color = ElementTheme.colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun HomeContactsSectionHeader(
    title: String,
) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 6.dp),
        text = title,
        style = ElementTheme.typography.fontBodySmMedium,
        color = ElementTheme.colors.textSecondary,
    )
}

@Composable
private fun HomeContactsPermissionCard(
    onRequestContactsPermission: () -> Unit,
) {
    HomeContactsStatusCard(
        title = stringResource(R.string.screen_home_contacts_permission_title),
        description = stringResource(R.string.screen_home_contacts_permission_message),
        actionText = stringResource(R.string.screen_home_contacts_permission_action),
        onActionClick = onRequestContactsPermission,
    )
}

@Composable
private fun HomeContactsStatusCard(
    title: String,
    description: String,
    actionText: String? = null,
    onActionClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ElementTheme.colors.bgSubtleSecondary)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = ElementTheme.typography.fontBodyLgMedium,
            color = ElementTheme.colors.textPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
        )
        if (actionText != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                text = actionText,
                onClick = onActionClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HomeContactsEmptyState(
    title: String,
    description: String?,
    onStartChatClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(ElementTheme.colors.bgSubtleSecondary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = CompoundIcons.UserProfile(),
                contentDescription = null,
                tint = ElementTheme.colors.iconSecondary,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = ElementTheme.typography.fontHeadingSmMedium,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        if (description != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = ElementTheme.typography.fontBodyMdRegular,
                color = ElementTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            text = stringResource(R.string.screen_home_contacts_new_chat),
            onClick = onStartChatClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun RoomListContentState.directContacts(currentUserId: UserId?): List<RoomListRoomSummary> {
    return when (this) {
        is RoomListContentState.Rooms -> summaries.filter { summary ->
            summary.displayType == RoomSummaryDisplayType.ROOM &&
                summary.isDirect &&
                !summary.isArchived &&
                summary.directUserId != null &&
                summary.directUserId != currentUserId
        }
        else -> emptyList()
    }
}

@PreviewsDayNight
@Composable
internal fun HomeContactsViewPreview(@PreviewParameter(HomeStateProvider::class) state: HomeState) = ElementPreview {
    HomeContactsView(
        state = state,
        contactsState = aHomeContactsState(),
        onBackClick = {},
        onStartChatClick = {},
        onRoomClick = {},
        onUserClick = {},
    )
}

private fun String.initialForAvatar(): String {
    return trim().firstOrNull()?.uppercase().orEmpty()
}
