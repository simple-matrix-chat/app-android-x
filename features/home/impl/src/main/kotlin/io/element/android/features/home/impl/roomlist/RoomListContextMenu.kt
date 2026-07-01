/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.roomlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.PreviewParameter
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.home.impl.R
import io.element.android.features.home.impl.filters.MomentHomeMuteDuration
import io.element.android.libraries.designsystem.components.list.ListItemContent
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.ListItemStyle
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.features.leaveroom.api.R as LeaveRoomR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListContextMenu(
    contextMenu: RoomListState.ContextMenu.Shown,
    canReportRoom: Boolean,
    eventSink: (RoomListEvent.ContextMenuEvent) -> Unit,
    onRoomSettingsClick: (roomId: RoomId) -> Unit,
    onReportRoomClick: (roomId: RoomId) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { eventSink(RoomListEvent.HideContextMenu) },
        scrollable = false,
    ) {
        RoomListModalBottomSheetContent(
            contextMenu = contextMenu,
            canReportRoom = canReportRoom,
            onRoomMarkReadClick = {
                eventSink(RoomListEvent.HideContextMenu)
                eventSink(RoomListEvent.MarkAsRead(contextMenu.roomId))
            },
            onRoomMarkUnreadClick = {
                eventSink(RoomListEvent.HideContextMenu)
                eventSink(RoomListEvent.MarkAsUnread(contextMenu.roomId))
            },
            onRoomSettingsClick = {
                eventSink(RoomListEvent.HideContextMenu)
                onRoomSettingsClick(contextMenu.roomId)
            },
            onLeaveRoomClick = {
                eventSink(RoomListEvent.HideContextMenu)
                eventSink(RoomListEvent.LeaveRoom(contextMenu.roomId, needsConfirmation = true))
            },
            onFavoriteChange = { isFavorite ->
                eventSink(RoomListEvent.HideContextMenu)
                eventSink(RoomListEvent.SetRoomIsFavorite(contextMenu.roomId, isFavorite))
            },
            onArchiveChange = { isArchived ->
                eventSink(RoomListEvent.HideContextMenu)
                eventSink(RoomListEvent.SetRoomIsArchived(contextMenu.roomId, isArchived))
            },
            onMuteDurationClick = { duration ->
                eventSink(RoomListEvent.HideContextMenu)
                eventSink(RoomListEvent.SetRoomMuteDuration(contextMenu.roomId, duration))
            },
            onUnmuteClick = {
                eventSink(RoomListEvent.HideContextMenu)
                eventSink(RoomListEvent.UnmuteRoom(contextMenu.roomId))
            },
            onDirectUserBlockClick = { userId, displayName, blocked ->
                eventSink(RoomListEvent.HideContextMenu)
                eventSink(RoomListEvent.ShowDirectUserBlockConfirmation(userId, displayName, blocked))
            },
            onClearCacheRoomClick = {
                eventSink(RoomListEvent.HideContextMenu)
                eventSink(RoomListEvent.ClearCacheOfRoom(contextMenu.roomId))
            },
            onReportRoomClick = {
                eventSink(RoomListEvent.HideContextMenu)
                onReportRoomClick(contextMenu.roomId)
            },
        )
    }
}

@Composable
private fun RoomListModalBottomSheetContent(
    contextMenu: RoomListState.ContextMenu.Shown,
    canReportRoom: Boolean,
    onRoomSettingsClick: () -> Unit,
    onLeaveRoomClick: () -> Unit,
    onFavoriteChange: (isFavorite: Boolean) -> Unit,
    onArchiveChange: (isArchived: Boolean) -> Unit,
    onMuteDurationClick: (MomentHomeMuteDuration) -> Unit,
    onUnmuteClick: () -> Unit,
    onDirectUserBlockClick: (UserId, String, Boolean) -> Unit,
    onRoomMarkReadClick: () -> Unit,
    onRoomMarkUnreadClick: () -> Unit,
    onClearCacheRoomClick: () -> Unit,
    onReportRoomClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = contextMenu.roomName ?: stringResource(id = CommonStrings.common_no_room_name),
                    style = ElementTheme.typography.fontBodyLgMedium,
                    fontStyle = FontStyle.Italic.takeIf { contextMenu.roomName == null }
                )
            }
        )
        if (contextMenu.hasNewContent) {
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.screen_roomlist_mark_as_read),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                onClick = onRoomMarkReadClick,
                leadingContent = ListItemContent.Icon(
                    iconSource = IconSource.Vector(CompoundIcons.MarkAsRead())
                ),
                style = ListItemStyle.Primary,
            )
        } else {
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(id = R.string.screen_roomlist_mark_as_unread),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                onClick = onRoomMarkUnreadClick,
                leadingContent = ListItemContent.Icon(
                    iconSource = IconSource.Vector(CompoundIcons.MarkAsUnread())
                ),
                style = ListItemStyle.Primary,
            )
        }
        val (textResId, icon) = if (contextMenu.isFavorite) {
            CommonStrings.action_unpin to CompoundIcons.PinSolid()
        } else {
            CommonStrings.action_pin to CompoundIcons.Pin()
        }
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(id = textResId),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            leadingContent = ListItemContent.Icon(
                iconSource = IconSource.Vector(
                    icon,
                )
            ),
            onClick = {
                onFavoriteChange(!contextMenu.isFavorite)
            },
            style = ListItemStyle.Primary,
        )
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(id = if (contextMenu.isArchived) R.string.action_unarchive else R.string.action_archive),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            leadingContent = ListItemContent.Icon(
                iconSource = IconSource.Vector(
                    CompoundIcons.ExportArchive(),
                )
            ),
            onClick = {
                onArchiveChange(!contextMenu.isArchived)
            },
            style = ListItemStyle.Primary,
        )
        if (contextMenu.isMuted) {
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(id = CommonStrings.common_unmute),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                leadingContent = ListItemContent.Icon(
                    iconSource = IconSource.Vector(
                        CompoundIcons.Notifications(),
                    )
                ),
                onClick = onUnmuteClick,
                style = ListItemStyle.Primary,
            )
        } else {
            MomentHomeMuteDuration.entries.forEach { duration ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(id = duration.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    leadingContent = ListItemContent.Icon(
                        iconSource = IconSource.Vector(
                            CompoundIcons.NotificationsOff(),
                        )
                    ),
                    onClick = {
                        onMuteDurationClick(duration)
                    },
                    style = ListItemStyle.Primary,
                )
            }
        }
        if (contextMenu.isDm && contextMenu.directUserId != null && contextMenu.directUserDisplayName != null) {
            val isBlocked = contextMenu.isDirectUserBlocked
            ListItem(
                headlineContent = {
                    val textResId = if (isBlocked) {
                        R.string.screen_home_direct_user_unblock_user
                    } else {
                        R.string.screen_home_direct_user_block_user
                    }
                    Text(
                        text = stringResource(id = textResId),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                leadingContent = ListItemContent.Icon(
                    iconSource = IconSource.Vector(
                        CompoundIcons.Block(),
                    )
                ),
                onClick = {
                    onDirectUserBlockClick(contextMenu.directUserId, contextMenu.directUserDisplayName, !isBlocked)
                },
                style = if (isBlocked) ListItemStyle.Primary else ListItemStyle.Destructive,
            )
        }
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(id = CommonStrings.common_settings),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            modifier = Modifier.clickable { onRoomSettingsClick() },
            leadingContent = ListItemContent.Icon(
                iconSource = IconSource.Vector(
                    CompoundIcons.Settings(),
                )
            ),
            style = ListItemStyle.Primary,
        )
        if (canReportRoom) {
            ListItem(
                headlineContent = {
                    Text(text = stringResource(CommonStrings.action_report_room))
                },
                modifier = Modifier.clickable { onReportRoomClick() },
                leadingContent = ListItemContent.Icon(
                    iconSource = IconSource.Vector(
                        CompoundIcons.ChatProblem(),
                    )
                ),
                style = ListItemStyle.Destructive,
            )
        }
        if (contextMenu.canLeaveRoom) {
            ListItem(
                headlineContent = {
                    Text(text = stringResource(if (contextMenu.isDm) LeaveRoomR.string.action_delete_chat else CommonStrings.action_leave_room))
                },
                modifier = Modifier.clickable { onLeaveRoomClick() },
                leadingContent = ListItemContent.Icon(
                    iconSource = IconSource.Vector(
                        if (contextMenu.isDm) CompoundIcons.Delete() else CompoundIcons.Leave(),
                    )
                ),
                style = ListItemStyle.Destructive,
            )
        }
        if (contextMenu.displayClearRoomCacheAction) {
            ListItem(
                headlineContent = {
                    Text(text = "Clear cache for this room")
                },
                modifier = Modifier.clickable { onClearCacheRoomClick() },
                leadingContent = ListItemContent.Icon(
                    iconSource = IconSource.Vector(CompoundIcons.Delete())
                ),
                style = ListItemStyle.Primary,
            )
        }
    }
}

@PreviewsDayNight
@Composable
internal fun RoomListContextMenuPreview(
    @PreviewParameter(RoomListStateContextMenuShownProvider::class) contextMenu: RoomListState.ContextMenu.Shown
) = ElementPreview {
    RoomListContextMenu(
        contextMenu = contextMenu,
        canReportRoom = true,
        onRoomSettingsClick = {},
        onReportRoomClick = {},
        eventSink = {},
    )
}

private val MomentHomeMuteDuration.labelRes: Int
    get() = when (this) {
        MomentHomeMuteDuration.Hours8 -> R.string.action_mute_for_8_hours
        MomentHomeMuteDuration.OneWeek -> R.string.action_mute_for_1_week
        MomentHomeMuteDuration.Forever -> R.string.action_mute_forever
    }
