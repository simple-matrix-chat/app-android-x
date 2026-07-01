/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roomdetails.impl

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import im.vector.app.features.analytics.plan.Interaction
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.roomcall.api.hasPermissionToJoin
import io.element.android.features.userprofile.shared.blockuser.BlockUserDialogs
import io.element.android.features.userprofile.shared.blockuser.BlockUserSection
import io.element.android.libraries.androidutils.system.copyToClipboard
import io.element.android.libraries.architecture.coverage.ExcludeFromCoverage
import io.element.android.libraries.designsystem.atomic.atoms.MatrixBadgeAtom
import io.element.android.libraries.designsystem.atomic.molecules.MatrixBadgeRowMolecule
import io.element.android.libraries.designsystem.components.ClickableLinkText
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.list.ListItemContent
import io.element.android.libraries.designsystem.components.preferences.PreferenceCategory
import io.element.android.libraries.designsystem.modifiers.niceClickable
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.ElementPreviewDark
import io.element.android.libraries.designsystem.preview.ElementPreviewLight
import io.element.android.libraries.designsystem.preview.PreviewWithLargeHeight
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.DropdownMenu
import io.element.android.libraries.designsystem.theme.components.DropdownMenuItem
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Switch
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarHost
import io.element.android.libraries.designsystem.utils.snackbar.rememberSnackbarHostState
import io.element.android.libraries.matrix.api.core.RoomAlias
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.notification.CallIntent
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.matrix.api.room.getBestName
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.services.analytics.compose.LocalAnalyticsService
import io.element.android.services.analyticsproviders.api.trackers.captureInteraction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import io.element.android.features.leaveroom.api.R as LeaveRoomR

@Composable
fun RoomDetailsView(
    state: RoomDetailsState,
    goBack: () -> Unit,
    onActionClick: (RoomDetailsAction) -> Unit,
    onShareRoom: () -> Unit,
    openRoomMemberList: () -> Unit,
    openRoomNotificationSettings: () -> Unit,
    invitePeople: () -> Unit,
    openAvatarPreview: (name: String, url: String) -> Unit,
    openPollHistory: () -> Unit,
    openMediaGallery: () -> Unit,
    openAdminSettings: () -> Unit,
    openSecurityAndPrivacy: () -> Unit,
    onJoinCallClick: (CallIntent) -> Unit,
    onPinnedMessagesClick: () -> Unit,
    onKnockRequestsClick: () -> Unit,
    onProfileClick: (UserId) -> Unit,
    onReportRoomClick: () -> Unit,
    modifier: Modifier = Modifier,
    leaveRoomView: @Composable () -> Unit,
) {
    val snackbarHostState = rememberSnackbarHostState(snackbarMessage = state.snackbarMessage)
    val isMomentRoom = state.isMomentRoom
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .consumeWindowInsets(padding)
                .padding(horizontal = if (isMomentRoom) 16.dp else 20.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(if (isMomentRoom) 18.dp else 24.dp),
        ) {
            leaveRoomView()
            RoomDetailsTopBar(
                goBack = goBack,
                showEdit = state.canEdit && !isMomentRoom,
                showTitle = !isMomentRoom,
                onActionClick = onActionClick
            )

            when (state.roomType) {
                RoomDetailsType.Room -> {
                    RoomHeaderSection(
                        avatarUrl = state.roomAvatarUrl,
                        roomId = state.roomId,
                        roomName = state.roomName,
                        roomAlias = state.roomAlias,
                        heroes = state.heroes,
                        roomTopic = state.roomTopic,
                        roomBadges = state.roomBadges,
                        isTombstoned = state.isTombstoned,
                        isMomentRoom = isMomentRoom,
                        openAvatarPreview = { avatarUrl ->
                            openAvatarPreview(state.roomName, avatarUrl)
                        },
                        onSubtitleClick = { subtitle ->
                            state.eventSink(RoomDetailsEvent.CopyToClipboard(subtitle))
                        }
                    )
                }
                is RoomDetailsType.Dm -> {
                    DmHeaderSection(
                        otherMember = state.roomType.otherMember,
                        roomName = state.roomName,
                        roomTopic = state.roomTopic,
                        roomBadges = state.roomBadges,
                        isTombstoned = state.isTombstoned,
                        openAvatarPreview = { name, avatarUrl ->
                            openAvatarPreview(name, avatarUrl)
                        },
                        onSubtitleClick = { subtitle ->
                            state.eventSink(RoomDetailsEvent.CopyToClipboard(subtitle))
                        }
                    )
                }
            }
            if (isMomentRoom && state.roomCallState.hasPermissionToJoin()) {
                MomentRoomCallSection(
                    onCall = onJoinCallClick,
                )
            } else if (!isMomentRoom) {
                MainActionsSection(
                    state = state,
                    onShareRoom = onShareRoom,
                    onInvitePeople = invitePeople,
                    onCall = onJoinCallClick,
                )
            }

            if (state.roomTopic is RoomTopicState.CanAddTopic) {
                TopicSection(
                    roomTopic = state.roomTopic,
                    onActionClick = onActionClick,
                )
            }

            if (isMomentRoom) {
                MomentRoomDetailsSection(title = stringResource(R.string.screen_moment_room_profile_section_details_android)) {
                    RoomSettingsItem(
                        momentRoomType = state.momentRoomType,
                        onClick = { onActionClick(RoomDetailsAction.Edit) },
                    )
                    if (state.roomNotificationSettings != null) {
                        NotificationItem(
                            isDefaultMode = state.roomNotificationSettings.isDefault,
                            momentRoomType = state.momentRoomType,
                            isMomentRoom = true,
                            openRoomNotificationSettings = openRoomNotificationSettings
                        )
                    }
                    MediaGalleryItem(
                        onClick = openMediaGallery,
                        momentRoomType = state.momentRoomType,
                        isMomentRoom = true,
                        showDivider = state.isPublic,
                    )
                    if (state.isPublic) {
                        PublicLinkItem(
                            onClick = onShareRoom,
                            momentRoomType = state.momentRoomType,
                        )
                    }
                }
            } else {
                MomentRoomDetailsCard {
                    if (state.roomNotificationSettings != null) {
                        NotificationItem(
                            isDefaultMode = state.roomNotificationSettings.isDefault,
                            momentRoomType = state.momentRoomType,
                            isMomentRoom = false,
                            openRoomNotificationSettings = openRoomNotificationSettings
                        )
                    }

                    FavoriteItem(
                        isFavorite = state.isFavorite,
                        onFavoriteChanges = {
                            state.eventSink(RoomDetailsEvent.SetFavorite(it))
                        }
                    )
                    PinnedMessagesItem(
                        pinnedMessagesCount = state.pinnedMessagesCount,
                        onPinnedMessagesClick = onPinnedMessagesClick
                    )
                    PollsItem(
                        openPollHistory = openPollHistory
                    )
                    MediaGalleryItem(
                        onClick = openMediaGallery
                    )
                }
            }

            if (state.roomMemberDetailsState != null || state.roomType is RoomDetailsType.Room) {
                val peopleContent: @Composable ColumnScope.() -> Unit = {
                    state.roomMemberDetailsState?.let { dmMemberDetails ->
                        ProfileItem(
                            onClick = { onProfileClick(dmMemberDetails.userId) }
                        )
                    }
                    if (state.roomType is RoomDetailsType.Room) {
                        MembersItem(
                            momentRoomType = state.momentRoomType,
                            memberCount = state.memberCount,
                            isMomentRoom = isMomentRoom,
                            showDivider = state.canShowKnockRequests,
                            openRoomMemberList = openRoomMemberList,
                        )
                        if (state.canShowKnockRequests) {
                            KnockRequestsItem(
                                knockRequestsCount = state.knockRequestsCount,
                                onKnockRequestsClick = onKnockRequestsClick
                            )
                        }
                        if (!state.isMomentRoom && state.displayRolesAndPermissionsSettings) {
                            MomentRoomDetailsRow(
                                title = stringResource(R.string.screen_room_details_roles_and_permissions),
                                imageVector = CompoundIcons.Admin(),
                                onClick = openAdminSettings,
                            )
                        }
                        if (!state.isMomentRoom && state.displaySecurityAndPrivacySettings) {
                            MomentRoomDetailsRow(
                                title = stringResource(R.string.screen_room_details_security_and_privacy_title),
                                imageVector = CompoundIcons.LockSolid(),
                                onClick = openSecurityAndPrivacy,
                            )
                        }
                    }
                }
                if (isMomentRoom) {
                    MomentRoomDetailsSection(
                        title = stringResource(R.string.screen_moment_room_profile_section_people_android),
                        content = peopleContent
                    )
                } else {
                    MomentRoomDetailsCard(content = peopleContent)
                }
            }

            if (state.roomType is RoomDetailsType.Dm && state.roomMemberDetailsState != null) {
                val roomMemberState = state.roomMemberDetailsState
                BlockUserSection(roomMemberState)
                BlockUserDialogs(roomMemberState)
            }

            if (state.canLeaveRoom) {
                OtherActionsSection(
                    isDm = state.roomType is RoomDetailsType.Dm,
                    momentRoomType = state.momentRoomType,
                    canReportRoom = state.canReportRoom,
                    onReportRoomClick = onReportRoomClick,
                    onLeaveRoomClick = { state.eventSink(RoomDetailsEvent.LeaveRoom(needsConfirmation = true)) }
                )
            }

            if (state.showDebugInfo && !isMomentRoom) {
                DebugInfoSection(
                    roomId = state.roomId,
                    roomVersion = state.roomVersion,
                )
            }
        }
    }
}

@Composable
private fun KnockRequestsItem(knockRequestsCount: Int?, onKnockRequestsClick: () -> Unit) {
    MomentRoomDetailsRow(
        title = stringResource(R.string.screen_room_details_requests_to_join_title),
        imageVector = CompoundIcons.AskToJoin(),
        trailingText = knockRequestsCount?.takeIf { it > 0 }?.toString(),
        onClick = onKnockRequestsClick,
    )
}

@Composable
private fun RoomDetailsTopBar(
    goBack: () -> Unit,
    onActionClick: (RoomDetailsAction) -> Unit,
    showEdit: Boolean,
    showTitle: Boolean,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp),
            onClick = goBack,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = CompoundIcons.ChevronLeft(),
                contentDescription = stringResource(CommonStrings.action_back),
                tint = ElementTheme.colors.iconPrimary,
            )
        }

        if (showTitle) {
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp),
                text = stringResource(R.string.screen_room_details_title),
                style = ElementTheme.typography.fontHeadingSmMedium,
                color = ElementTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (showEdit) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(
                        imageVector = CompoundIcons.OverflowVertical(),
                        contentDescription = stringResource(id = CommonStrings.a11y_user_menu),
                        tint = ElementTheme.colors.iconPrimary,
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = CommonStrings.action_edit)) },
                        onClick = {
                            // Explicitly close the menu before handling the action, as otherwise it stays open during the
                            // transition and renders really badly.
                            showMenu = false
                            onActionClick(RoomDetailsAction.Edit)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MainActionsSection(
    state: RoomDetailsState,
    onShareRoom: () -> Unit,
    onInvitePeople: () -> Unit,
    onCall: (callIntent: CallIntent) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            if (!state.isMomentRoom) {
                state.roomNotificationSettings?.let { roomNotificationSettings ->
                    if (roomNotificationSettings.mode == RoomNotificationMode.MUTE) {
                        MomentRoomShortcutButton(
                            title = stringResource(CommonStrings.common_unmute),
                            imageVector = CompoundIcons.NotificationsOff(),
                            onClick = {
                                state.eventSink(RoomDetailsEvent.UnmuteNotification)
                            },
                        )
                    } else {
                        MomentRoomShortcutButton(
                            title = stringResource(CommonStrings.common_mute),
                            imageVector = CompoundIcons.Notifications(),
                            onClick = {
                                state.eventSink(RoomDetailsEvent.MuteNotification)
                            },
                        )
                    }
                }
            }

            if (state.roomCallState.hasPermissionToJoin()) {
                // As per existing behavior, only show voice call in DM.
                if (state.roomType is RoomDetailsType.Dm) {
                    MomentRoomShortcutButton(
                        title = stringResource(CommonStrings.action_call),
                        imageVector = CompoundIcons.VoiceCall(),
                        onClick = { onCall(CallIntent.AUDIO) },
                    )
                }

                MomentRoomShortcutButton(
                    title = stringResource(CommonStrings.common_video),
                    imageVector = CompoundIcons.VideoCall(),
                    onClick = { onCall(CallIntent.VIDEO) },
                )
            }

            if (!state.isMomentRoom && state.canInvite && state.roomType !is RoomDetailsType.Dm) {
                MomentRoomShortcutButton(
                    title = stringResource(CommonStrings.action_invite),
                    imageVector = CompoundIcons.UserAdd(),
                    onClick = onInvitePeople,
                )
            }

            if (!state.isMomentRoom && state.roomType is RoomDetailsType.Room) {
                // Share CTA should be hidden for DMs.
                MomentRoomShortcutButton(
                    title = stringResource(CommonStrings.action_share),
                    imageVector = CompoundIcons.ShareAndroid(),
                    onClick = onShareRoom
                )
            }
        }
    }
}

@Composable
private fun MomentRoomCallSection(
    onCall: (callIntent: CallIntent) -> Unit,
) {
    MomentRoomDetailsSection(title = stringResource(R.string.screen_moment_room_profile_section_calls_android)) {
        MomentRoomDetailsRow(
            title = stringResource(CommonStrings.common_video),
            imageVector = CompoundIcons.VideoCall(),
            onClick = { onCall(CallIntent.VIDEO) },
            showDivider = false,
        )
    }
}

@Composable
private fun MomentRoomShortcutButton(
    title: String,
    imageVector: ImageVector,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .size(width = 72.dp, height = 72.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(ElementTheme.colors.bgSubtleSecondary, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = imageVector,
                contentDescription = null,
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = ElementTheme.typography.fontBodySmMedium,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RoomHeaderSection(
    avatarUrl: String?,
    roomId: RoomId,
    roomName: String,
    roomAlias: RoomAlias?,
    heroes: ImmutableList<MatrixUser>,
    roomTopic: RoomTopicState,
    roomBadges: ImmutableList<RoomBadge>,
    isTombstoned: Boolean,
    isMomentRoom: Boolean,
    openAvatarPreview: (url: String) -> Unit,
    onSubtitleClick: (String) -> Unit,
) {
    if (isMomentRoom) {
        RoomHeaderContent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            avatarUrl = avatarUrl,
            roomId = roomId,
            roomName = roomName,
            roomAlias = roomAlias,
            heroes = heroes,
            roomTopic = roomTopic,
            roomBadges = roomBadges,
            isTombstoned = isTombstoned,
            isMomentRoom = true,
            openAvatarPreview = openAvatarPreview,
            onSubtitleClick = onSubtitleClick,
        )
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.55f)),
    ) {
        RoomHeaderContent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            avatarUrl = avatarUrl,
            roomId = roomId,
            roomName = roomName,
            roomAlias = roomAlias,
            heroes = heroes,
            roomTopic = roomTopic,
            roomBadges = roomBadges,
            isTombstoned = isTombstoned,
            isMomentRoom = false,
            openAvatarPreview = openAvatarPreview,
            onSubtitleClick = onSubtitleClick,
        )
    }
}

@Composable
private fun RoomHeaderContent(
    avatarUrl: String?,
    roomId: RoomId,
    roomName: String,
    roomAlias: RoomAlias?,
    heroes: ImmutableList<MatrixUser>,
    roomTopic: RoomTopicState,
    roomBadges: ImmutableList<RoomBadge>,
    isTombstoned: Boolean,
    isMomentRoom: Boolean,
    openAvatarPreview: (url: String) -> Unit,
    onSubtitleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Avatar(
            avatarData = AvatarData(roomId.value, roomName, avatarUrl, AvatarSize.RoomDetailsHeader),
            avatarType = AvatarType.Room(
                heroes = heroes.map { user ->
                    user.getAvatarData(size = AvatarSize.RoomDetailsHeader)
                }.toImmutableList(),
                isTombstoned = isTombstoned,
            ),
            contentDescription = stringResource(CommonStrings.a11y_room_avatar),
            modifier = Modifier
                .clip(CircleShape)
                .clickable(
                    enabled = avatarUrl != null,
                    onClickLabel = stringResource(CommonStrings.action_view),
                ) {
                    openAvatarPreview(avatarUrl!!)
                }
                .testTag(TestTags.roomDetailAvatar)
        )
        TitleAndSubtitle(
            title = roomName,
            subtitle = roomAlias?.value,
            roomTopic = roomTopic,
            isMomentRoom = isMomentRoom,
            onSubtitleClick = onSubtitleClick,
        )
        BadgeList(
            roomBadge = roomBadges,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun DmHeaderSection(
    otherMember: RoomMember,
    roomName: String,
    roomTopic: RoomTopicState,
    roomBadges: ImmutableList<RoomBadge>,
    isTombstoned: Boolean,
    openAvatarPreview: (name: String, url: String) -> Unit,
    onSubtitleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(
                avatarData = AvatarData(otherMember.userId.value, roomName, otherMember.avatarUrl, AvatarSize.RoomDetailsHeader),
                avatarType = AvatarType.Room(
                    heroes = persistentListOf(
                        otherMember.getAvatarData(size = AvatarSize.RoomDetailsHeader)
                    ),
                    isTombstoned = isTombstoned,
                ),
                contentDescription = stringResource(CommonStrings.a11y_room_avatar),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        enabled = otherMember.avatarUrl != null,
                        onClickLabel = stringResource(CommonStrings.action_view),
                    ) {
                        openAvatarPreview(otherMember.getBestName(), otherMember.avatarUrl!!)
                    }
                    .testTag(TestTags.roomDetailAvatar)
            )
            TitleAndSubtitle(
                title = roomName,
                subtitle = otherMember.userId.value,
                roomTopic = roomTopic,
                onSubtitleClick = onSubtitleClick,
            )
            BadgeList(
                roomBadge = roomBadges,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun TitleAndSubtitle(
    title: String,
    subtitle: String?,
    roomTopic: RoomTopicState,
    isMomentRoom: Boolean = false,
    onSubtitleClick: (String) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(if (isMomentRoom) 12.dp else 16.dp))
        Text(
            text = title,
            style = if (isMomentRoom) ElementTheme.typography.fontHeadingMdBold else ElementTheme.typography.fontHeadingLgBold,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                modifier = Modifier.niceClickable { onSubtitleClick(subtitle) },
                text = subtitle,
                style = ElementTheme.typography.fontBodyMdMedium,
                color = ElementTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        if (roomTopic is RoomTopicState.ExistingTopic) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = roomTopic.topic,
                style = ElementTheme.typography.fontBodyMdRegular,
                color = ElementTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BadgeList(
    roomBadge: ImmutableList<RoomBadge>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (roomBadge.isNotEmpty()) {
            MatrixBadgeRowMolecule(
                data = roomBadge.map {
                    it.toMatrixBadgeData()
                }.toImmutableList(),
            )
        }
    }
}

@Composable
private fun RoomBadge.toMatrixBadgeData(): MatrixBadgeAtom.MatrixBadgeData {
    return when (this) {
        RoomBadge.PUBLIC -> {
            MatrixBadgeAtom.MatrixBadgeData(
                text = stringResource(R.string.screen_room_details_badge_public),
                icon = CompoundIcons.Public(),
                type = MatrixBadgeAtom.Type.Info,
            )
        }
    }
}

@Composable
private fun MomentRoomDetailsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 4.dp),
            text = title,
            style = ElementTheme.typography.fontBodySmMedium,
            color = ElementTheme.colors.textSecondary,
        )
        Column(content = content)
    }
}

@Composable
private fun MomentRoomDetailsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.55f)),
    ) {
        Column(content = content)
    }
}

@Composable
private fun MomentRoomDetailsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp),
        color = ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.45f),
    )
}

@Composable
private fun MomentRoomDetailsRow(
    title: String,
    imageVector: ImageVector,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingText: String? = null,
    isDestructive: Boolean = false,
    isWaiting: Boolean = false,
    showsError: Boolean = false,
    showDivider: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (subtitle == null) 60.dp else 74.dp)
                .semantics(mergeDescendants = true) {}
                .then(
                    if (onClick == null) {
                        Modifier
                    } else {
                        Modifier.clickable(onClick = onClick)
                    }
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isDestructive) {
                            ElementTheme.colors.bgCriticalSubtle
                        } else {
                            ElementTheme.colors.bgSubtleSecondary
                        },
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = if (isDestructive) ElementTheme.colors.iconCriticalPrimary else ElementTheme.colors.iconPrimary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = ElementTheme.typography.fontBodyLgMedium,
                    color = if (isDestructive) ElementTheme.colors.textCriticalPrimary else ElementTheme.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = ElementTheme.colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailingContent != null) {
                trailingContent()
            } else if (isWaiting) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else if (showsError) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = CompoundIcons.InfoSolid(),
                    contentDescription = null,
                    tint = ElementTheme.colors.iconCriticalPrimary,
                )
            } else if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = ElementTheme.typography.fontBodyLgRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onClick != null && trailingContent == null) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = CompoundIcons.ChevronRight(),
                    contentDescription = null,
                    tint = ElementTheme.colors.iconSecondary,
                )
            }
        }
        if (showDivider) {
            MomentRoomDetailsDivider()
        }
    }
}

@Composable
private fun TopicSection(
    roomTopic: RoomTopicState,
    onActionClick: (RoomDetailsAction) -> Unit,
) {
    MomentRoomDetailsCard {
        if (roomTopic is RoomTopicState.CanAddTopic) {
            MomentRoomDetailsRow(
                title = stringResource(id = R.string.screen_room_details_add_topic_title),
                imageVector = CompoundIcons.Plus(),
                onClick = { onActionClick(RoomDetailsAction.AddTopic) },
                showDivider = false,
            )
        } else if (roomTopic is RoomTopicState.ExistingTopic) {
            ClickableLinkText(
                text = roomTopic.topic,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
                interactionSource = remember { MutableInteractionSource() },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.tertiary,
                ),
            )
        }
    }
}

@Composable
private fun RoomSettingsItem(
    momentRoomType: MomentRoomDetailsType,
    onClick: () -> Unit,
) {
    val (title, subtitle) = when (momentRoomType) {
        MomentRoomDetailsType.Channel -> (
            stringResource(R.string.screen_moment_room_profile_settings_title_channel_android)
                to stringResource(R.string.screen_moment_room_profile_settings_subtitle_channel_android)
            )
        else -> (
            stringResource(R.string.screen_moment_room_profile_settings_title_group_android)
                to stringResource(R.string.screen_moment_room_profile_settings_subtitle_group_android)
            )
    }
    MomentRoomDetailsRow(
        title = title,
        subtitle = subtitle,
        imageVector = CompoundIcons.Settings(),
        onClick = onClick,
    )
}

@Composable
private fun NotificationItem(
    isDefaultMode: Boolean,
    momentRoomType: MomentRoomDetailsType,
    isMomentRoom: Boolean,
    openRoomNotificationSettings: () -> Unit,
) {
    val notificationMode = if (isDefaultMode) {
        stringResource(R.string.screen_room_details_notification_mode_default)
    } else {
        stringResource(R.string.screen_room_details_notification_mode_custom)
    }
    val momentSubtitle = when (momentRoomType) {
        MomentRoomDetailsType.Channel -> stringResource(R.string.screen_moment_room_profile_notifications_subtitle_channel_android)
        else -> stringResource(R.string.screen_moment_room_profile_notifications_subtitle_group_android)
    }
    MomentRoomDetailsRow(
        title = stringResource(R.string.screen_room_details_notification_title),
        subtitle = if (isMomentRoom) momentSubtitle else notificationMode,
        imageVector = CompoundIcons.Notifications(),
        trailingText = if (isMomentRoom) notificationMode else null,
        onClick = openRoomNotificationSettings,
    )
}

@Composable
private fun FavoriteItem(
    isFavorite: Boolean,
    onFavoriteChanges: (Boolean) -> Unit,
) {
    val (textResId, icon) = if (isFavorite) {
        CommonStrings.common_favourited to CompoundIcons.FavouriteSolid()
    } else {
        CommonStrings.common_favourite to CompoundIcons.Favourite()
    }
    MomentRoomDetailsRow(
        title = stringResource(id = textResId),
        imageVector = icon,
        onClick = { onFavoriteChanges(!isFavorite) },
        trailingContent = {
            Switch(
                checked = isFavorite,
                onCheckedChange = onFavoriteChanges,
            )
        }
    )
}

@Composable
private fun ProfileItem(
    onClick: () -> Unit,
) {
    MomentRoomDetailsRow(
        title = stringResource(id = R.string.screen_room_details_profile_row_title),
        imageVector = CompoundIcons.UserProfile(),
        onClick = onClick,
    )
}

@Composable
private fun MembersItem(
    momentRoomType: MomentRoomDetailsType,
    memberCount: Long,
    isMomentRoom: Boolean = false,
    showDivider: Boolean = true,
    openRoomMemberList: () -> Unit,
) {
    val (title, subtitle) = when (momentRoomType) {
        MomentRoomDetailsType.Group -> (
            stringResource(R.string.screen_moment_room_profile_members_title_android)
                to stringResource(R.string.screen_moment_room_profile_members_subtitle_android)
            )
        MomentRoomDetailsType.Channel -> (
            stringResource(R.string.screen_moment_room_profile_subscribers_title_android)
                to stringResource(R.string.screen_moment_room_profile_subscribers_subtitle_android)
            )
        MomentRoomDetailsType.Unknown -> stringResource(CommonStrings.common_people) to null
    }
    MomentRoomDetailsRow(
        title = title,
        subtitle = subtitle.takeIf { isMomentRoom },
        imageVector = CompoundIcons.User(),
        trailingText = memberCount.toString(),
        onClick = openRoomMemberList,
        showDivider = showDivider,
    )
}

@Composable
private fun InviteItem(
    onClick: () -> Unit,
) {
    MomentRoomDetailsRow(
        title = stringResource(R.string.screen_room_details_invite_title),
        imageVector = CompoundIcons.UserAdd(),
        onClick = onClick,
    )
}

@Composable
private fun PinnedMessagesItem(
    pinnedMessagesCount: Int?,
    onPinnedMessagesClick: () -> Unit,
) {
    val analyticsService = LocalAnalyticsService.current
    MomentRoomDetailsRow(
        title = stringResource(R.string.screen_room_details_pinned_events_row_title),
        imageVector = CompoundIcons.Pin(),
        trailingText = pinnedMessagesCount?.toString(),
        isWaiting = pinnedMessagesCount == null,
        onClick = {
            analyticsService.captureInteraction(Interaction.Name.PinnedMessageRoomInfoButton)
            onPinnedMessagesClick()
        }
    )
}

@Composable
private fun PollsItem(
    openPollHistory: () -> Unit,
) {
    MomentRoomDetailsRow(
        title = stringResource(R.string.screen_polls_history_title),
        imageVector = CompoundIcons.Polls(),
        onClick = openPollHistory,
    )
}

@Composable
private fun MediaGalleryItem(
    onClick: () -> Unit,
    momentRoomType: MomentRoomDetailsType = MomentRoomDetailsType.Unknown,
    isMomentRoom: Boolean = false,
    showDivider: Boolean = false,
) {
    val title = if (isMomentRoom) {
        stringResource(R.string.screen_moment_room_profile_media_title_android)
    } else {
        stringResource(R.string.screen_room_details_media_gallery_title)
    }
    val subtitle = when (momentRoomType) {
        MomentRoomDetailsType.Channel -> stringResource(R.string.screen_moment_room_profile_media_subtitle_channel_android)
        MomentRoomDetailsType.Group -> stringResource(R.string.screen_moment_room_profile_media_subtitle_group_android)
        MomentRoomDetailsType.Unknown -> null
    }
    MomentRoomDetailsRow(
        title = title,
        subtitle = subtitle.takeIf { isMomentRoom },
        imageVector = CompoundIcons.Image(),
        onClick = onClick,
        showDivider = showDivider,
    )
}

@Composable
private fun PublicLinkItem(
    onClick: () -> Unit,
    momentRoomType: MomentRoomDetailsType,
) {
    val subtitle = when (momentRoomType) {
        MomentRoomDetailsType.Channel -> stringResource(R.string.screen_moment_room_profile_public_link_subtitle_channel_android)
        else -> stringResource(R.string.screen_moment_room_profile_public_link_subtitle_group_android)
    }
    MomentRoomDetailsRow(
        title = stringResource(R.string.screen_moment_room_profile_public_link_title_android),
        subtitle = subtitle,
        imageVector = CompoundIcons.Link(),
        onClick = onClick,
        showDivider = false,
    )
}

@Composable
private fun OtherActionsSection(
    isDm: Boolean,
    momentRoomType: MomentRoomDetailsType,
    canReportRoom: Boolean,
    onReportRoomClick: () -> Unit,
    onLeaveRoomClick: () -> Unit,
) {
    val content: @Composable ColumnScope.() -> Unit = {
        if (canReportRoom) {
            MomentRoomDetailsRow(
                title = stringResource(CommonStrings.action_report_room),
                imageVector = CompoundIcons.ChatProblem(),
                onClick = onReportRoomClick,
                isDestructive = true,
            )
        }
        val leaveTitle = when (momentRoomType) {
            MomentRoomDetailsType.Unknown if isDm -> stringResource(LeaveRoomR.string.action_delete_chat)
            MomentRoomDetailsType.Group -> stringResource(R.string.screen_moment_room_profile_leave_group_android)
            MomentRoomDetailsType.Channel -> stringResource(R.string.screen_moment_room_profile_leave_channel_android)
            MomentRoomDetailsType.Unknown -> stringResource(CommonStrings.action_leave_room)
        }
        MomentRoomDetailsRow(
            title = leaveTitle,
            imageVector = if (isDm) CompoundIcons.Delete() else CompoundIcons.Leave(),
            onClick = onLeaveRoomClick,
            isDestructive = true,
            showDivider = false,
        )
    }
    if (momentRoomType == MomentRoomDetailsType.Unknown) {
        MomentRoomDetailsCard(content = content)
    } else {
        MomentRoomDetailsSection(
            title = stringResource(R.string.screen_moment_room_profile_section_safety_android),
            content = content
        )
    }
}

@Composable
private fun DebugInfoSection(
    roomId: RoomId,
    roomVersion: String?,
) {
    val context = LocalContext.current
    PreferenceCategory(showTopDivider = true) {
        val toastMessage = stringResource(CommonStrings.common_copied_to_clipboard)
        ListItem(
            headlineContent = {
                Text("Internal room ID")
            },
            supportingContent = {
                Text(
                    text = roomId.value,
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            },
            leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Code())),
            trailingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Copy())),
            onClick = {
                context.copyToClipboard(
                    text = roomId.value,
                    toastMessage = toastMessage,
                )
            },
        )
        ListItem(
            headlineContent = {
                Text("Room version")
            },
            supportingContent = {
                Text(
                    text = roomVersion ?: "Unknown",
                    style = ElementTheme.typography.fontBodySmRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            },
            leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Info())),
        )
    }
}

@PreviewWithLargeHeight
@Composable
internal fun RoomDetailsPreview(@PreviewParameter(RoomDetailsStateProvider::class) state: RoomDetailsState) =
    ElementPreviewLight { ContentToPreview(state) }

@PreviewWithLargeHeight
@Composable
internal fun RoomDetailsDarkPreview(@PreviewParameter(RoomDetailsStateProvider::class) state: RoomDetailsState) =
    ElementPreviewDark { ContentToPreview(state) }

@PreviewWithLargeHeight
@Composable
internal fun RoomDetailsA11yPreview() = ElementPreview {
    ContentToPreview(
        state = aRoomDetailsState(displayAdminSettings = true)
    )
}

@ExcludeFromCoverage
@Composable
private fun ContentToPreview(state: RoomDetailsState) {
    RoomDetailsView(
        state = state,
        goBack = {},
        onActionClick = {},
        onShareRoom = {},
        openRoomMemberList = {},
        openRoomNotificationSettings = {},
        invitePeople = {},
        openAvatarPreview = { _, _ -> },
        openPollHistory = {},
        openMediaGallery = {},
        openAdminSettings = {},
        openSecurityAndPrivacy = {},
        onJoinCallClick = {},
        onPinnedMessagesClick = {},
        onKnockRequestsClick = {},
        onProfileClick = {},
        onReportRoomClick = {},
        leaveRoomView = {},
    )
}
