/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.topbars

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.messages.impl.MessagesMenuActions
import io.element.android.features.messages.impl.R
import io.element.android.features.roomcall.api.RoomCallState
import io.element.android.features.roomcall.api.aStandByCallState
import io.element.android.features.roomcall.api.anOngoingCallState
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.preview.ROOM_NAME
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.testtags.testTag
import io.element.android.libraries.ui.strings.CommonStrings

internal val MomentRoomHeaderActionSize = 32.dp
internal val MomentRoomHeaderActionSpacing = 12.dp
internal val MomentRoomHeaderTitleStartPadding = 44.dp
internal val MomentRoomHeaderTitleEndPadding = 184.dp

@Composable
internal fun MessagesViewTopBar(
    roomName: String?,
    onRoomDetailsClick: () -> Unit,
    onBackClick: () -> Unit,
    onAIBriefingClick: () -> Unit,
    showAIBriefingAction: Boolean,
    modifier: Modifier = Modifier,
    menuActions: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ElementTheme.colors.bgCanvasDefault)
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 6.dp)
            .heightIn(min = 44.dp)
            .testTag(TestTags.roomHeader),
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides MomentRoomHeaderActionSize) {
            IconButton(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(MomentRoomHeaderActionSize),
                onClick = onBackClick,
            ) {
                Icon(
                    imageVector = CompoundIcons.ChevronLeft(),
                    contentDescription = stringResource(CommonStrings.action_back),
                    tint = ElementTheme.colors.iconPrimary,
                )
            }
        }

        RoomTitle(
            roomName = roomName,
            onRoomDetailsClick = onRoomDetailsClick,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(
                    start = MomentRoomHeaderTitleStartPadding,
                    end = MomentRoomHeaderTitleEndPadding,
                )
                .semantics {
                    heading()
                }
                .testTag(TestTags.roomHeaderTitle),
        )

        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides MomentRoomHeaderActionSize) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(MomentRoomHeaderActionSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showAIBriefingAction) {
                    IconButton(
                        modifier = Modifier.size(MomentRoomHeaderActionSize),
                        onClick = onAIBriefingClick,
                    ) {
                        Icon(
                            resourceId = R.drawable.ic_moment_sparkles,
                            contentDescription = stringResource(R.string.screen_room_ai_briefing_title),
                            tint = ElementTheme.colors.iconPrimary,
                        )
                    }
                }
                menuActions()
                IconButton(
                    modifier = Modifier.size(MomentRoomHeaderActionSize),
                    onClick = onRoomDetailsClick,
                ) {
                    Icon(
                        imageVector = CompoundIcons.Settings(),
                        contentDescription = stringResource(CommonStrings.common_settings),
                        tint = ElementTheme.colors.iconPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomTitle(
    roomName: String?,
    onRoomDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onRoomDetailsClick() }
            .padding(horizontal = 8.dp, vertical = 2.dp),
        text = roomName ?: stringResource(CommonStrings.common_no_room_name),
        style = ElementTheme.typography.fontBodyLgMedium.copy(
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        ),
        fontStyle = FontStyle.Italic.takeIf { roomName == null },
        color = ElementTheme.colors.textPrimary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@PreviewsDayNight
@Composable
internal fun MessagesViewTopBarPreview() = ElementPreview {
    @Composable
    fun AMessagesViewTopBar(
        roomName: String? = ROOM_NAME,
        roomCallState: RoomCallState = RoomCallState.Unavailable,
        displayThreads: Boolean = false,
        showAIBriefingAction: Boolean = true,
    ) = MessagesViewTopBar(
        roomName = roomName,
        onRoomDetailsClick = {},
        onBackClick = {},
        onAIBriefingClick = {},
        showAIBriefingAction = showAIBriefingAction,
        menuActions = {
            MessagesMenuActions(
                roomCallState = roomCallState,
                displayThreads = displayThreads,
                displaySearch = true,
                onSearchClick = {},
                onJoinCallClick = {},
                onThreadsListClick = {},
            )
        }
    )
    Column {
        AMessagesViewTopBar()
        HorizontalDivider()
        AMessagesViewTopBar(
            roomCallState = anOngoingCallState(),
        )
        HorizontalDivider()
        AMessagesViewTopBar(
            roomName = null,
            roomCallState = anOngoingCallState(canJoinCall = false),
        )
        HorizontalDivider()
        AMessagesViewTopBar(
            roomName = "A DM with a very very very long name",
            roomCallState = aStandByCallState(canStartCall = false),
        )
        HorizontalDivider()
        AMessagesViewTopBar(
            displayThreads = true,
        )
        HorizontalDivider()
        AMessagesViewTopBar(
            roomName = "Daily briefing",
            showAIBriefingAction = false,
        )
    }
}
