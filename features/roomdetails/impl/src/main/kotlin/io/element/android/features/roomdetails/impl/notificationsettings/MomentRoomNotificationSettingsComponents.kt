/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roomdetails.impl.notificationsettings

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.roomdetails.impl.R
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.RadioButton
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Switch
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
internal fun MomentRoomNotificationTopBar(
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
internal fun MomentRoomNotificationSection(
    title: String? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (title != null) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = title,
                style = ElementTheme.typography.fontBodyMdMedium,
                color = ElementTheme.colors.textSecondary,
            )
        }
        MomentRoomNotificationCard(content = content)
        if (footer != null) {
            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                footer()
            }
        }
    }
}

@Composable
internal fun MomentRoomNotificationCard(
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
internal fun MomentRoomNotificationSwitchRow(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    MomentRoomNotificationRowFrame(
        modifier = modifier,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
    ) {
        MomentRoomNotificationIconTile(icon)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = if (enabled) ElementTheme.colors.textPrimary else ElementTheme.colors.textDisabled,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = if (enabled) ElementTheme.colors.textSecondary else ElementTheme.colors.textDisabled,
                )
            }
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
internal fun MomentRoomNotificationModeRow(
    item: RoomNotificationSettingsItem,
    selected: Boolean,
    enabled: Boolean,
    loading: Boolean,
    displayMentionsOnlyDisclaimer: Boolean,
    onSelect: (RoomNotificationSettingsItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtitle = when {
        item.mode == RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY && displayMentionsOnlyDisclaimer ->
            stringResource(id = R.string.screen_room_notification_settings_mentions_only_disclaimer)
        else -> null
    }
    MomentRoomNotificationRowFrame(
        modifier = modifier.semantics(mergeDescendants = true) {},
        enabled = enabled && !loading,
        onClick = { onSelect(item) },
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.title,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = if (enabled) ElementTheme.colors.textPrimary else ElementTheme.colors.textDisabled,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
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
                onClick = if (enabled) {
                    { onSelect(item) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
internal fun MomentRoomNotificationValueRow(
    title: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    MomentRoomNotificationRowFrame(
        modifier = modifier,
        enabled = !isLoading,
        onClick = {},
    ) {
        MomentRoomNotificationIconTile(CompoundIcons.Notifications())
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            text = title,
            style = ElementTheme.typography.fontBodyLgMedium,
            color = if (isLoading) ElementTheme.colors.textDisabled else ElementTheme.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp,
            )
        }
    }
}

@Composable
internal fun MomentRoomNotificationDestructiveRow(
    title: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MomentRoomNotificationRowFrame(
        modifier = modifier,
        enabled = enabled && !loading,
        onClick = onClick,
    ) {
        MomentRoomNotificationIconTile(
            imageVector = CompoundIcons.Delete(),
            tint = ElementTheme.colors.iconCriticalPrimary,
            backgroundColor = ElementTheme.colors.bgCriticalSubtle,
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            text = title,
            style = ElementTheme.typography.fontBodyLgMedium,
            color = if (enabled) ElementTheme.colors.textCriticalPrimary else ElementTheme.colors.textDisabled,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp,
            )
        }
    }
}

@Composable
internal fun MomentRoomNotificationFooterText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = text,
        style = ElementTheme.typography.fontBodyMdRegular,
        color = ElementTheme.colors.textSecondary,
    )
}

@Composable
internal fun MomentRoomNotificationDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        color = ElementTheme.colors.borderDisabled,
    )
}

@Composable
private fun MomentRoomNotificationRowFrame(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 82.dp,
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

@Composable
private fun MomentRoomNotificationIconTile(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = ElementTheme.colors.iconPrimary,
    backgroundColor: Color = ElementTheme.colors.bgSubtleSecondary,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = tint,
        )
    }
}
