/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.messagecomposer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton

@Composable
internal fun DisabledComposerView(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(start = 4.dp, end = 6.dp, bottom = 4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(
            modifier = Modifier.size(48.dp),
            enabled = false,
            onClick = {},
        ) {
            Icon(
                modifier = Modifier.size(28.dp),
                imageVector = CompoundIcons.Plus(),
                contentDescription = null,
                tint = ElementTheme.colors.iconDisabled,
            )
        }

        val bgColor = ElementTheme.colors.bgSubtlePrimary
        val borderColor = ElementTheme.colors.borderDisabled

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .border(0.5.dp, borderColor, CircleShape)
                .background(color = bgColor)
                .requiredHeightIn(min = 48.dp)
                .weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Icon(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp),
                imageVector = CompoundIcons.MicOnSolid(),
                contentDescription = null,
                tint = ElementTheme.colors.iconDisabled,
            )
        }
    }
}

@PreviewsDayNight
@Composable
internal fun DisabledComposerViewPreview() = ElementPreview {
    Column {
        DisabledComposerView(
            modifier = Modifier.height(IntrinsicSize.Min),
        )
    }
}
