/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.roomselect.impl

import android.icu.text.ListFormatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.RadioButton
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.SearchBar
import io.element.android.libraries.designsystem.theme.components.SearchBarResultState
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.designsystem.utils.OnVisibleRangeChangeEffect
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.ui.components.SelectedRoom
import io.element.android.libraries.matrix.ui.model.SelectRoomInfo
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.roomselect.api.RoomSelectMode
import io.element.android.libraries.ui.strings.CommonPlurals
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.util.Locale

@Suppress("MultipleEmitters") // False positive
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomSelectView(
    state: RoomSelectState,
    onDismiss: () -> Unit,
    onSubmit: (List<RoomId>, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_PARAMETER")
    fun onRoomRemoved(roomInfo: SelectRoomInfo) {
        // TODO toggle selection when multi-selection is enabled
        state.eventSink(RoomSelectEvents.RemoveSelectedRoom)
    }

    @Composable
    fun SelectedRoomsHelper(selectedRooms: ImmutableList<SelectRoomInfo>) {
        SelectedRooms(
            selectedRooms = selectedRooms,
            onRemoveRoom = ::onRoomRemoved,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }

    var canHandleBack by remember { mutableStateOf(true) }
    fun onBackButton(state: RoomSelectState) {
        if (state.isSearchActive) {
            state.eventSink(RoomSelectEvents.ToggleSearchActive)
        } else if (canHandleBack) {
            canHandleBack = false
            onDismiss()
        }
    }

    BackHandler(
        enabled = canHandleBack,
        onBack = { onBackButton(state) }
    )

    val lazyListState = rememberLazyListState()
    OnVisibleRangeChangeEffect(lazyListState) { visibleRange ->
        state.eventSink(RoomSelectEvents.UpdateVisibleRange(visibleRange))
    }

    Scaffold(
        modifier = modifier,
        containerColor = ElementTheme.colors.bgSubtleSecondary,
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .padding(top = 8.dp),
        ) {
            MomentRoomSelectHeader(
                title = when (state.mode) {
                    RoomSelectMode.Forward -> stringResource(CommonStrings.common_forward_message)
                    RoomSelectMode.Share -> stringResource(CommonStrings.common_send_to)
                },
                canSubmit = state.selectedRooms.isNotEmpty(),
                onCancel = { onBackButton(state) },
                onSubmit = {
                    onSubmit(
                        state.selectedRooms.map { it.roomId },
                        state.forwardComment.trim().takeIf { it.isNotEmpty() },
                    )
                },
            )
            if (state.mode == RoomSelectMode.Forward) {
                MomentForwardCommentField(
                    value = state.forwardComment,
                    onValueChange = { state.eventSink(RoomSelectEvents.UpdateForwardComment(it)) },
                )
            }
            SearchBar(
                modifier = Modifier.fillMaxWidth(),
                placeHolderTitle = stringResource(CommonStrings.action_search),
                queryState = state.searchQuery,
                active = state.isSearchActive,
                onActiveChange = { state.eventSink(RoomSelectEvents.ToggleSearchActive) },
                resultState = state.resultState,
                showBackButton = false,
            ) { summaries ->
                MomentRoomSelectList(
                    rooms = summaries,
                    selectedRooms = state.selectedRooms,
                    showSelectedRooms = state.mode == RoomSelectMode.Share,
                    lazyListState = lazyListState,
                    selectedRoomsContent = {
                        SelectedRoomsHelper(selectedRooms = state.selectedRooms)
                    },
                    onSelection = { roomSummary ->
                        state.eventSink(RoomSelectEvents.SetSelectedRoom(roomSummary))
                    },
                )
            }

            if (!state.isSearchActive) {
                Spacer(modifier = Modifier.height(20.dp))

                if (state.resultState is SearchBarResultState.Results) {
                    MomentRoomSelectList(
                        rooms = state.resultState.results,
                        selectedRooms = state.selectedRooms,
                        showSelectedRooms = state.mode == RoomSelectMode.Share,
                        lazyListState = lazyListState,
                        selectedRoomsContent = {
                            SelectedRoomsHelper(selectedRooms = state.selectedRooms)
                        },
                        onSelection = { roomSummary ->
                            state.eventSink(RoomSelectEvents.SetSelectedRoom(roomSummary))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MomentForwardCommentField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        border = BorderStroke(1.dp, ElementTheme.colors.borderDisabled),
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            value = value,
            onValueChange = onValueChange,
            placeholder = stringResource(CommonStrings.common_message),
            minLines = 1,
            maxLines = 4,
        )
    }
}

@Composable
private fun MomentRoomSelectHeader(
    title: String,
    canSubmit: Boolean,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides ElementTheme.colors.textSecondary) {
            TextButton(
                modifier = Modifier.align(Alignment.CenterStart),
                text = stringResource(CommonStrings.action_cancel),
                onClick = onCancel,
            )
        }
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 96.dp),
            text = title,
            color = ElementTheme.colors.textPrimary,
            style = ElementTheme.typography.fontBodyLgMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        CompositionLocalProvider(LocalContentColor provides ElementTheme.colors.textActionAccent) {
            TextButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                text = stringResource(CommonStrings.action_send),
                enabled = canSubmit,
                onClick = onSubmit,
            )
        }
    }
}

@Composable
private fun MomentRoomSelectList(
    rooms: ImmutableList<SelectRoomInfo>,
    selectedRooms: ImmutableList<SelectRoomInfo>,
    showSelectedRooms: Boolean,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    selectedRoomsContent: @Composable () -> Unit,
    onSelection: (SelectRoomInfo) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = lazyListState,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showSelectedRooms && selectedRooms.isNotEmpty()) {
            item {
                selectedRoomsContent()
            }
        }
        items(rooms, key = { it.roomId.value }) { roomSummary ->
            MomentRoomSummaryView(
                roomInfo = roomSummary,
                isSelected = selectedRooms.any { it.roomId == roomSummary.roomId },
                onSelection = onSelection,
            )
        }
    }
}

@Composable
private fun SelectedRooms(
    selectedRooms: ImmutableList<SelectRoomInfo>,
    onRemoveRoom: (SelectRoomInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        items(selectedRooms, key = { it.roomId.value }) { selectRoomInfo ->
            SelectedRoom(roomInfo = selectRoomInfo, onRemoveRoom = onRemoveRoom)
        }
    }
}

@Composable
private fun MomentRoomSummaryView(
    roomInfo: SelectRoomInfo,
    isSelected: Boolean,
    onSelection: (SelectRoomInfo) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelection(roomInfo) },
        shape = RoundedCornerShape(18.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) ElementTheme.colors.borderAccentSubtle else ElementTheme.colors.borderDisabled,
        ),
    ) {
        RoomSummaryViewContent(
            roomInfo = roomInfo,
            isSelected = isSelected,
            onSelection = onSelection,
        )
    }
}

@Composable
private fun RoomSummaryViewContent(
    roomInfo: SelectRoomInfo,
    isSelected: Boolean,
    onSelection: (SelectRoomInfo) -> Unit,
) {
    val description = roomInfo.roomListDescription()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(64.dp)
            .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            modifier = Modifier.size(44.dp),
            avatarData = roomInfo.getAvatarData(size = AvatarSize.RoomSelectRoomListItem),
            avatarType = AvatarType.Room(
                heroes = roomInfo.heroes.map { user ->
                    user.getAvatarData(size = AvatarSize.RoomSelectRoomListItem)
                }.toImmutableList(),
                isTombstoned = roomInfo.isTombstoned,
            ),
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp, end = 8.dp)
                .weight(1f)
        ) {
            Text(
                style = ElementTheme.typography.fontBodyMdRegular,
                text = roomInfo.name ?: stringResource(id = CommonStrings.common_no_room_name),
                fontStyle = FontStyle.Italic.takeIf { roomInfo.name == null },
                color = ElementTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            description?.let {
                Text(
                    text = it,
                    color = ElementTheme.colors.textSecondary,
                    style = ElementTheme.typography.fontBodySmRegular,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        RadioButton(selected = isSelected, onClick = { onSelection(roomInfo) })
    }
}

@Composable
private fun SelectRoomInfo.roomListDescription(): String? {
    val alias = canonicalAlias?.value?.takeIf { it.isNotBlank() }
    if (isDirect) {
        return alias
    }
    if (alias != null) {
        return alias
    }

    val heroNames = heroes.mapNotNull { hero ->
        hero.displayName?.takeIf { it.isNotBlank() }
    }
    if (heroNames.isEmpty() && activeMembersCount <= heroes.size) {
        return null
    }

    val othersCount = (activeMembersCount - heroes.size).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    val components = buildList {
        addAll(heroNames)
        if (othersCount > 0) {
            add(pluralStringResource(CommonPlurals.common_many_members, othersCount, othersCount))
        }
    }
    return formatListDescription(components)
}

private fun formatListDescription(components: List<String>): String? {
    return when (components.size) {
        0 -> null
        1 -> components.first()
        else -> ListFormatter.getInstance(Locale.getDefault()).format(components)
    }
}

@PreviewsDayNight
@Composable
internal fun RoomSelectViewPreview(@PreviewParameter(RoomSelectStateProvider::class) state: RoomSelectState) = ElementPreview {
    RoomSelectView(
        state = state,
        onDismiss = {},
        onSubmit = { _, _ -> },
    )
}
