/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roomdetails.impl.members

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.roomdetails.impl.MomentRoomDetailsType
import io.element.android.features.roomdetails.impl.R
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.atomic.molecules.IconTitleSubtitleMolecule
import io.element.android.libraries.designsystem.components.BigIcon
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.LinearProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.SearchField
import io.element.android.libraries.designsystem.theme.components.SegmentedButton
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.room.RoomMembershipState
import io.element.android.libraries.matrix.api.room.getBestName
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun RoomMemberListView(
    state: RoomMemberListState,
    navigator: RoomMemberListNavigator,
    modifier: Modifier = Modifier,
) {
    fun onSelectUser(roomMember: RoomMember) {
        state.eventSink(RoomMemberListEvent.RoomMemberSelected(roomMember))
    }

    Scaffold(
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RoomMemberListTopBar(
                momentRoomType = state.momentRoomType,
                canInvite = state.canInvite,
                onBackClick = navigator::exitRoomMemberList,
                onInviteClick = navigator::openInviteMembers,
            )
            SearchField(
                state = state.searchQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(CommonStrings.common_search_for_someone),
            )
            RoomMemberList(
                roomMembersData = state.filteredRoomMembers,
                selectedSection = state.selectedSection,
                showBannedSection = state.showBannedSection,
                searchQuery = state.searchQuery.text.toString(),
                onSelectedSectionChange = { state.eventSink(RoomMemberListEvent.ChangeSelectedSection(it)) },
                onSelectUser = ::onSelectUser,
            )
        }
    }
}

@Composable
private fun RoomMemberList(
    roomMembersData: AsyncData<RoomMembers>,
    selectedSection: SelectedSection,
    showBannedSection: Boolean,
    searchQuery: String,
    onSelectedSectionChange: (SelectedSection) -> Unit,
    onSelectUser: (RoomMember) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth(), state = rememberLazyListState()) {
        stickyHeader {
            Column {
                AnimatedVisibility(visible = showBannedSection) {
                    val segmentedButtonTitles = persistentListOf(
                        stringResource(id = R.string.screen_room_member_list_mode_members),
                        stringResource(id = R.string.screen_room_member_list_mode_banned),
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .background(ElementTheme.colors.bgCanvasDefault)
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    ) {
                        for ((index, title) in segmentedButtonTitles.withIndex()) {
                            SegmentedButton(
                                index = index,
                                count = segmentedButtonTitles.size,
                                selected = selectedSection.ordinal == index,
                                onClick = { onSelectedSectionChange(SelectedSection.entries[index]) },
                                text = title,
                            )
                        }
                    }
                }
                AnimatedVisibility(visible = roomMembersData.isLoading()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        when (roomMembersData) {
            is AsyncData.Failure -> failureItem(roomMembersData.error)
            is AsyncData.Loading -> {
                val roomMembers = roomMembersData.dataOrNull()
                if (roomMembers == null) {
                    loadingItems()
                } else if (roomMembers.isEmpty(selectedSection)) {
                    emptySearchItem(searchQuery, selectedSection)
                } else {
                    memberItems(
                        roomMembers = roomMembers,
                        selectedSection = selectedSection,
                        onSelectUser = onSelectUser,
                    )
                }
            }
            is AsyncData.Success -> {
                val roomMembers = roomMembersData.dataOrNull() ?: return@LazyColumn
                if (roomMembers.isEmpty(selectedSection)) {
                    emptySearchItem(searchQuery, selectedSection)
                } else {
                    memberItems(
                        roomMembers = roomMembers,
                        selectedSection = selectedSection,
                        onSelectUser = onSelectUser,
                    )
                }
            }
            AsyncData.Uninitialized -> Unit
        }
    }
}

private fun LazyListScope.memberItems(
    roomMembers: RoomMembers,
    selectedSection: SelectedSection,
    onSelectUser: (RoomMember) -> Unit,
) {
    when (selectedSection) {
        SelectedSection.MEMBERS -> {
            if (roomMembers.invited.isNotEmpty()) {
                roomMemberListSectionHeader(
                    text = {
                        val memberCount = roomMembers.invited.count()
                        pluralStringResource(id = R.plurals.screen_room_member_list_pending_header_title, memberCount, memberCount)
                    },
                )
                roomMemberListSectionItems(
                    members = roomMembers.invited,
                    onMemberSelected = { onSelectUser(it) }
                )
            }
            if (roomMembers.joined.isNotEmpty()) {
                roomMemberListSectionHeader(
                    text = {
                        val memberCount = roomMembers.joined.count()
                        pluralStringResource(id = R.plurals.screen_room_member_list_header_title, count = memberCount, memberCount)
                    },
                )
                roomMemberListSectionItems(
                    members = roomMembers.joined,
                    onMemberSelected = { onSelectUser(it) }
                )
            }
        }
        SelectedSection.BANNED -> {
            if (roomMembers.banned.isNotEmpty()) {
                roomMemberListSectionHeader(
                    text = {
                        val memberCount = roomMembers.banned.count()
                        pluralStringResource(id = R.plurals.screen_room_member_list_banned_header_title, memberCount, memberCount)
                    },
                    isCritical = true,
                )
                roomMemberListSectionItems(
                    members = roomMembers.banned,
                    onMemberSelected = { onSelectUser(it) }
                )
            }
        }
    }
}

private fun LazyListScope.failureItem(failure: Throwable) {
    item {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 32.dp),
            text = stringResource(id = CommonStrings.error_unknown) + "\n\n" + failure.localizedMessage,
            color = ElementTheme.colors.textCriticalPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

private fun LazyListScope.roomMemberListSectionHeader(
    text: @Composable (() -> String),
    modifier: Modifier = Modifier,
    isCritical: Boolean = false,
) {
    item {
        Text(
            modifier = modifier.padding(top = 10.dp, bottom = 8.dp),
            text = text(),
            style = ElementTheme.typography.fontBodySmMedium,
            color = if (isCritical) ElementTheme.colors.textCriticalPrimary else ElementTheme.colors.textSecondary,
        )
    }
}

private fun LazyListScope.roomMemberListSectionItems(
    members: ImmutableList<RoomMemberListMember>?,
    onMemberSelected: (RoomMember) -> Unit,
) {
    val sectionMembers = members.orEmpty()
    item {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            shape = RoundedCornerShape(24.dp),
            color = ElementTheme.colors.bgCanvasDefault,
            border = BorderStroke(1.dp, ElementTheme.colors.borderDisabled),
        ) {
            Column {
                sectionMembers.forEachIndexed { index, matrixUser ->
                    RoomMemberListItem(
                        modifier = Modifier.fillMaxWidth(),
                        roomMemberListMember = matrixUser,
                        showDivider = index != sectionMembers.lastIndex,
                        onClick = { onMemberSelected(matrixUser.roomMember) }
                    )
                }
            }
        }
    }
}

private fun LazyListScope.loadingItems() {
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(5) {
                RoomMemberListLoadingItem()
            }
        }
    }
}

@Composable
private fun RoomMemberListLoadingItem(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.32f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(AvatarSize.UserListItem.dp)
                    .background(ElementTheme.colors.bgSubtleSecondary, RoundedCornerShape(AvatarSize.UserListItem.dp / 2)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .fillMaxWidth(0.48f)
                        .background(ElementTheme.colors.bgSubtleSecondary, RoundedCornerShape(7.dp)),
                )
                Box(
                    modifier = Modifier
                        .height(12.dp)
                        .fillMaxWidth(0.72f)
                        .background(ElementTheme.colors.bgSubtleSecondary, RoundedCornerShape(6.dp)),
                )
            }
            Box(
                modifier = Modifier
                    .height(12.dp)
                    .width(48.dp)
                    .background(ElementTheme.colors.bgSubtleSecondary, RoundedCornerShape(6.dp)),
            )
        }
    }
}

private fun LazyListScope.emptySearchItem(
    searchQuery: String,
    selectedSection: SelectedSection,
) {
    item {
        val trimmedQuery = searchQuery.trim()
        val isEmptyBannedSection = selectedSection == SelectedSection.BANNED && trimmedQuery.isEmpty()
        IconTitleSubtitleMolecule(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 40.dp),
            iconStyle = BigIcon.Style.Default(
                vectorIcon = CompoundIcons.Search(),
                contentDescription = null,
            ),
            title = if (isEmptyBannedSection) {
                stringResource(R.string.screen_room_member_list_banned_empty)
            } else {
                stringResource(R.string.screen_room_member_list_empty_search_title, searchQuery)
            },
            subTitle = if (trimmedQuery.isEmpty() || isEmptyBannedSection) {
                null
            } else {
                stringResource(R.string.screen_room_member_list_empty_search_subtitle)
            },
        )
    }
}

@Composable
private fun RoomMemberListItem(
    roomMemberListMember: RoomMemberListMember,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
) {
    val member = roomMemberListMember.roomMember
    val isBanned = member.membership == RoomMembershipState.BAN
    val title = if (isBanned) member.userId.value else member.getBestName()
    val subtitle = if (isBanned) null else member.userId.value
    val avatarName = if (isBanned) null else member.getBestName()
    val avatarUrl = if (isBanned) null else member.avatarUrl
    val roleText = when (member.role) {
        RoomMember.Role.Admin -> stringResource(R.string.screen_room_member_list_role_administrator)
        RoomMember.Role.Moderator -> stringResource(R.string.screen_room_member_list_role_moderator)
        is RoomMember.Role.Owner -> stringResource(R.string.screen_room_member_list_role_owner)
        else -> null
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                avatarData = AvatarData(
                    id = member.userId.value,
                    name = avatarName,
                    url = avatarUrl,
                    size = AvatarSize.UserListItem,
                ),
                avatarType = AvatarType.User,
                contentDescription = null,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = ElementTheme.typography.fontBodyLgMedium,
                    color = ElementTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = ElementTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            roleText?.let {
                Text(
                    text = it,
                    style = ElementTheme.typography.fontBodySmMedium,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 72.dp)
                    .height(1.dp)
                    .background(ElementTheme.colors.borderDisabled),
            )
        }
    }
}

@Composable
private fun RoomMemberListTopBar(
    momentRoomType: MomentRoomDetailsType,
    canInvite: Boolean,
    onBackClick: () -> Unit,
    onInviteClick: () -> Unit,
) {
    val title = when (momentRoomType) {
        MomentRoomDetailsType.Channel -> stringResource(R.string.screen_moment_room_profile_subscribers_title_android)
        MomentRoomDetailsType.Group -> stringResource(R.string.screen_moment_room_profile_members_title_android)
        MomentRoomDetailsType.Unknown -> stringResource(CommonStrings.common_people)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp),
            onClick = onBackClick,
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
                .padding(horizontal = 88.dp),
            text = title,
            style = ElementTheme.typography.fontHeadingSmMedium,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (canInvite) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                TextButton(
                    text = stringResource(CommonStrings.action_invite),
                    onClick = onInviteClick,
                )
            }
        }
    }
}

@PreviewsDayNight
@Composable
internal fun RoomMemberListViewPreview(@PreviewParameter(RoomMemberListStateProvider::class) state: RoomMemberListState) = ElementPreview {
    RoomMemberListView(
        state = state,
        navigator = object : RoomMemberListNavigator {},
    )
}
