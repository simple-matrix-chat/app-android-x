/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.notifications.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.RadioButton
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
internal fun MomentNotificationEditTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = CompoundIcons.ChevronLeft(),
                contentDescription = stringResource(CommonStrings.action_back),
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            text = title,
            style = ElementTheme.typography.fontHeadingMdBold,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(modifier = Modifier.size(48.dp))
    }
}

@Composable
internal fun MomentNotificationEditSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = title,
            style = ElementTheme.typography.fontBodyMdMedium,
            color = ElementTheme.colors.textSecondary,
        )
        MomentNotificationEditCard(content = content)
    }
}

@Composable
internal fun MomentNotificationEditCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        border = BorderStroke(1.dp, ElementTheme.colors.borderDisabled),
        shadowElevation = 3.dp,
    ) {
        Column(content = content)
    }
}

@Composable
internal fun MomentNotificationDefaultModeRow(
    title: String,
    description: String?,
    selected: Boolean,
    enabled: Boolean,
    loading: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MomentNotificationEditRowFrame(
        modifier = modifier.semantics(mergeDescendants = true) {},
        enabled = enabled && !loading,
        onClick = onSelect,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = if (enabled) ElementTheme.colors.textPrimary else ElementTheme.colors.textDisabled,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = if (enabled) ElementTheme.colors.textSecondary else ElementTheme.colors.textDisabled,
                )
            }
        }
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp,
            )
        } else {
            RadioButton(
                selected = selected,
                enabled = enabled,
                onClick = if (enabled) onSelect else null,
            )
        }
    }
}

@Composable
internal fun MomentNotificationCustomRoomRow(
    summary: EditNotificationSettingRoomInfo,
    title: String,
    modeLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MomentNotificationEditRowFrame(
        modifier = modifier,
        enabled = true,
        onClick = onClick,
    ) {
        Avatar(
            avatarData = summary.avatarData,
            avatarType = AvatarType.Room(
                heroes = summary.heroesAvatar,
            ),
            contentDescription = null,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = ElementTheme.colors.textPrimary,
                fontStyle = FontStyle.Italic.takeIf { summary.name == null },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (modeLabel.isNotEmpty()) {
                Text(
                    text = modeLabel,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = CompoundIcons.ChevronRight(),
            contentDescription = null,
            tint = ElementTheme.colors.iconSecondary,
        )
    }
}

@Composable
internal fun MomentNotificationEditDivider(
    start: Dp = 24.dp,
) {
    HorizontalDivider(
        modifier = Modifier.padding(start = start),
        color = ElementTheme.colors.borderDisabled,
    )
}

@Composable
private fun MomentNotificationEditRowFrame(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 76.dp,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(ElementTheme.colors.bgCanvasDefault)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
