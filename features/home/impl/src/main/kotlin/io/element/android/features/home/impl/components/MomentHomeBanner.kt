/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.ButtonSize
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
internal fun MomentHomeBanner(
    title: String,
    description: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .roomListBannerPadding()
            .clip(RoundedCornerShape(14.dp))
            .background(ElementTheme.colors.bgSubtleSecondary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = title,
                    style = ElementTheme.typography.fontBodyLgMedium,
                    color = ElementTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (onDismissClick != null) {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = onDismissClick,
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            imageVector = CompoundIcons.Close(),
                            contentDescription = stringResource(CommonStrings.action_close),
                            tint = ElementTheme.colors.iconSecondary,
                        )
                    }
                }
            }
            Text(
                text = description,
                style = ElementTheme.typography.fontBodyMdRegular,
                color = ElementTheme.colors.textSecondary,
            )
        }
        Button(
            text = actionText,
            size = ButtonSize.Medium,
            onClick = onActionClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@PreviewsDayNight
@Composable
internal fun MomentHomeBannerPreview() = ElementPreview {
    MomentHomeBanner(
        title = "Set up recovery",
        description = "Keep access to your encrypted messages by setting up recovery.",
        actionText = "Continue",
        onActionClick = {},
        onDismissClick = {},
    )
}
