/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roomdirectory.impl.root

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.roomdirectory.api.RoomDescription
import io.element.android.features.roomdirectory.impl.R
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.ButtonSize
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.FilledTextField
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList

@Composable
fun RoomDirectoryView(
    state: RoomDirectoryState,
    onResultClick: (RoomDescription) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ElementTheme.colors.bgCanvasDefault,
        topBar = {
            RoomDirectoryTopBar(onBackClick = onBackClick)
        },
        content = { padding ->
            RoomDirectoryContent(
                state = state,
                onResultClick = onResultClick,
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .fillMaxSize()
            )
        }
    )
}

@Composable
private fun RoomDirectoryTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(
            text = stringResource(CommonStrings.action_cancel),
            onClick = onBackClick,
            size = ButtonSize.Medium,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = stringResource(id = R.string.screen_room_directory_search_title),
            style = ElementTheme.typography.fontBodyLgMedium,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 96.dp),
        )
    }
}

@Composable
private fun RoomDirectoryContent(
    state: RoomDirectoryState,
    onResultClick: (RoomDescription) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SearchTextField(
            query = state.query,
            onQueryChange = { state.eventSink(RoomDirectoryEvents.Search(it)) },
            placeholder = stringResource(id = CommonStrings.action_search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
        )
        RoomDirectoryRoomList(
            roomDescriptions = state.roomDescriptions,
            displayLoadMoreIndicator = state.displayLoadMoreIndicator,
            displayEmptyState = state.displayEmptyState,
            onResultClick = onResultClick,
            onReachedLoadMore = { state.eventSink(RoomDirectoryEvents.LoadMore) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RoomDirectoryRoomList(
    roomDescriptions: ImmutableList<RoomDescription>,
    displayLoadMoreIndicator: Boolean,
    displayEmptyState: Boolean,
    onResultClick: (RoomDescription) -> Unit,
    onReachedLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        itemsIndexed(
            items = roomDescriptions,
            key = { _, roomDescription -> roomDescription.roomId.value },
        ) { index, roomDescription ->
            RoomDirectoryRoomRow(
                roomDescription = roomDescription,
                onClick = {
                    onResultClick(roomDescription)
                },
            )
            if (index < roomDescriptions.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 68.dp),
                    color = ElementTheme.colors.borderDisabled,
                )
            }
        }
        if (displayEmptyState) {
            item {
                Text(
                    text = stringResource(id = CommonStrings.common_no_results),
                    style = ElementTheme.typography.fontBodyLgRegular,
                    color = ElementTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                )
            }
        }
        if (displayLoadMoreIndicator) {
            item {
                LoadMoreIndicator(modifier = Modifier.fillMaxWidth())
                LaunchedEffect(onReachedLoadMore) {
                    onReachedLoadMore()
                }
            }
        }
    }
}

@Composable
private fun LoadMoreIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    colors: TextFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = ElementTheme.colors.bgSubtleSecondary,
        unfocusedContainerColor = ElementTheme.colors.bgSubtleSecondary,
        disabledContainerColor = ElementTheme.colors.bgSubtleSecondary,
        errorContainerColor = ElementTheme.colors.bgSubtleSecondary,
        unfocusedPlaceholderColor = ElementTheme.colors.textSecondary,
        focusedPlaceholderColor = ElementTheme.colors.textSecondary,
        focusedTextColor = ElementTheme.colors.textPrimary,
        unfocusedTextColor = ElementTheme.colors.textPrimary,
        cursorColor = ElementTheme.colors.textActionAccent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
    ),
) {
    val focusManager = LocalFocusManager.current
    FilledTextField(
        modifier = modifier.testTag(TestTags.searchTextField.value),
        textStyle = ElementTheme.typography.fontBodyLgRegular,
        singleLine = true,
        value = query,
        onValueChange = onQueryChange,
        keyboardActions = KeyboardActions(
            onSearch = {
                focusManager.clearFocus()
            }
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = colors,
        shape = RoundedCornerShape(99.dp),
        leadingIcon = {
            Icon(
                imageVector = CompoundIcons.Search(),
                contentDescription = null,
                tint = ElementTheme.colors.iconTertiary,
            )
        },
        placeholder = { Text(placeholder) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onQueryChange("")
                    }
                ) {
                    Icon(
                        imageVector = CompoundIcons.Close(),
                        contentDescription = stringResource(CommonStrings.action_clear),
                        tint = ElementTheme.colors.iconSecondary,
                    )
                }
            }
        },
    )
}

@Composable
private fun RoomDirectoryRoomRow(
    roomDescription: RoomDescription,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowClickModifier = if (roomDescription.canJoinOrKnock) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(rowClickModifier)
            .padding(
                start = 16.dp,
                top = 10.dp,
                end = 12.dp,
                bottom = 10.dp,
            )
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            avatarData = roomDescription.avatarData(AvatarSize.RoomDirectoryItem),
            avatarType = AvatarType.Room(),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = roomDescription.computedName,
                maxLines = 1,
                style = ElementTheme.typography.fontBodyLgRegular,
                color = ElementTheme.colors.textPrimary,
                overflow = TextOverflow.Ellipsis,
            )
            if (roomDescription.computedDescription.isNotBlank()) {
                Text(
                    text = roomDescription.computedDescription,
                    maxLines = 1,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (roomDescription.canJoinOrKnock) {
            Icon(
                imageVector = CompoundIcons.ChevronRight(),
                contentDescription = null,
                tint = ElementTheme.colors.iconTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@PreviewsDayNight
@Composable
internal fun RoomDirectoryViewPreview(@PreviewParameter(RoomDirectoryStateProvider::class) state: RoomDirectoryState) = ElementPreview {
    RoomDirectoryView(
        state = state,
        onResultClick = {},
        onBackClick = {},
    )
}
