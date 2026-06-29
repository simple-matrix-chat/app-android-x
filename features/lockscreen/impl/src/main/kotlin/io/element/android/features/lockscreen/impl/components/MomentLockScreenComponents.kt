/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.lockscreen.impl.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Switch
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
internal fun MomentLockScreenTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBackClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = CompoundIcons.ChevronLeft(),
                contentDescription = stringResource(CommonStrings.action_back),
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        if (title != null) {
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp),
                text = title,
                style = ElementTheme.typography.fontBodyLgMedium.copy(fontWeight = FontWeight.Bold),
                color = ElementTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun MomentLockScreenHeader(
    title: String,
    subtitle: String,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    subtitleColor: Color = ElementTheme.colors.textSecondary,
    iconSize: Dp = 72.dp,
    iconTileSize: Dp = 96.dp,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(iconTileSize)
                .background(
                    color = ElementTheme.colors.bgSubtleSecondary,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(iconSize),
                imageVector = imageVector,
                contentDescription = null,
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = title,
            style = ElementTheme.typography.fontHeadingMdBold,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = ElementTheme.typography.fontBodyMdRegular,
            color = subtitleColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun MomentLockScreenCard(
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
internal fun MomentLockScreenSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = ElementTheme.typography.fontBodySmMedium.copy(fontWeight = FontWeight.Bold),
            color = ElementTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        content()
    }
}

@Composable
internal fun MomentLockScreenRow(
    title: String,
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .semantics(mergeDescendants = true) {}
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MomentLockScreenIconTile(
                imageVector = imageVector,
                tint = if (destructive) ElementTheme.colors.iconCriticalPrimary else ElementTheme.colors.iconPrimary,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = ElementTheme.typography.fontBodyLgMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (destructive) ElementTheme.colors.textCriticalPrimary else ElementTheme.colors.textPrimary,
            )
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = CompoundIcons.ChevronRight(),
                contentDescription = null,
                tint = ElementTheme.colors.iconSecondary,
            )
        }
        if (showDivider) {
            MomentLockScreenDivider(start = 60.dp)
        }
    }
}

@Composable
internal fun MomentLockScreenSwitchRow(
    title: String,
    imageVector: ImageVector,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .semantics(mergeDescendants = true) {}
                .clickable(onClick = onCheckedChange)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MomentLockScreenIconTile(imageVector = imageVector)
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = ElementTheme.typography.fontBodyLgMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ElementTheme.colors.textPrimary,
            )
            Switch(
                checked = checked,
                onCheckedChange = { onCheckedChange() },
            )
        }
        if (showDivider) {
            MomentLockScreenDivider(start = 60.dp)
        }
    }
}

@Composable
internal fun MomentLockScreenIconTile(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = ElementTheme.colors.iconPrimary,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .background(
                color = ElementTheme.colors.bgSubtleSecondary,
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = imageVector,
            contentDescription = null,
            tint = tint,
        )
    }
}

@Composable
internal fun MomentLockScreenDivider(
    start: Dp,
) {
    HorizontalDivider(
        modifier = Modifier.padding(start = start),
        color = ElementTheme.colors.borderDisabled,
    )
}
