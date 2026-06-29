/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.topbars

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
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
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.anAvatarData
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.preview.ROOM_NAME
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.ui.components.aMatrixUserList
import io.element.android.libraries.matrix.ui.model.getAvatarData
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun ThreadTopBar(
    roomName: String?,
    roomAvatarData: AvatarData,
    heroes: ImmutableList<AvatarData>,
    isTombstoned: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ElementTheme.colors.bgCanvasDefault)
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 6.dp)
            .heightIn(min = 44.dp),
    ) {
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp),
            onClick = onBackClick,
        ) {
            Icon(
                imageVector = CompoundIcons.ChevronLeft(),
                contentDescription = stringResource(CommonStrings.action_back),
                tint = ElementTheme.colors.iconPrimary,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 56.dp)
                .semantics {
                    heading()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = stringResource(CommonStrings.common_thread),
                style = ElementTheme.typography.fontBodyLgMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = ElementTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ThreadSubtitle(roomName)
        }
    }
}

@Composable
private fun ThreadSubtitle(roomName: String?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(ElementTheme.colors.textSecondary.copy(alpha = 0.75f)),
        )
        Text(
            text = roomName ?: stringResource(CommonStrings.common_no_room_name),
            style = ElementTheme.typography.fontBodyXsMedium,
            fontStyle = FontStyle.Italic.takeIf { roomName == null },
            color = ElementTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@PreviewsDayNight
@Composable
internal fun ThreadTopBarPreview() = ElementPreview {
    @Composable
    fun AThreadTopBar(
        roomName: String? = ROOM_NAME,
        roomAvatarData: AvatarData = anAvatarData(
            name = ROOM_NAME,
            size = AvatarSize.TimelineRoom,
        ),
        isTombstoned: Boolean = false,
        heroes: ImmutableList<AvatarData> = persistentListOf(),
    ) = ThreadTopBar(
        roomName = roomName,
        roomAvatarData = roomAvatarData,
        isTombstoned = isTombstoned,
        heroes = heroes,
        onBackClick = {},
    )
    Column {
        AThreadTopBar()
        HorizontalDivider()
        AThreadTopBar(
            heroes = aMatrixUserList().map { it.getAvatarData(AvatarSize.TimelineRoom) }.toImmutableList(),
        )
        HorizontalDivider()
        AThreadTopBar(
            roomName = null,
        )
        HorizontalDivider()
        AThreadTopBar(
            roomAvatarData = anAvatarData(
                name = ROOM_NAME,
                url = "https://some-avatar.jpg",
                size = AvatarSize.TimelineRoom,
            ),
        )
        HorizontalDivider()
        AThreadTopBar(
            isTombstoned = true,
        )
    }
}
