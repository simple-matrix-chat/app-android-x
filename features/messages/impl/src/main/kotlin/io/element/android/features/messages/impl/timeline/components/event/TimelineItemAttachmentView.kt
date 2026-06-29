/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.event

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.timeline.components.layout.ContentAvoidingLayout
import io.element.android.features.messages.impl.timeline.components.layout.ContentAvoidingLayoutData
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text

/**
 * package-private, you should only use TimelineItemFileView and TimelineItemAudioView.
 * https://www.figma.com/design/G1xy0HDZKJf5TCRFmKb5d5/Compound-Android-Components?node-id=2019-8180
 */
@Composable
fun TimelineItemAttachmentView(
    icon: ImageVector,
    iconContentDescription: String?,
    filename: String,
    description: String,
    caption: String?,
    onContentLayoutChange: (ContentAvoidingLayoutData) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        TimelineItemAttachmentHeaderView(
            icon = icon,
            iconContentDescription = iconContentDescription,
            filename = filename,
            description = description,
            hasCaption = caption != null,
            onContentLayoutChange = onContentLayoutChange,
        )
        if (caption != null) {
            TimelineItemAttachmentCaptionView(
                modifier = Modifier.padding(top = 8.dp),
                caption = caption,
                onContentLayoutChange = onContentLayoutChange,
            )
        }
    }
}

@Composable
private fun TimelineItemAttachmentHeaderView(
    icon: ImageVector,
    iconContentDescription: String?,
    filename: String,
    description: String,
    hasCaption: Boolean,
    onContentLayoutChange: (ContentAvoidingLayoutData) -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconSize = 28.dp
    val spacing = 10.dp
    Row(
        modifier = modifier
            .widthIn(min = 184.dp)
            .background(momentTimelineAttachmentBackgroundColor(), MomentTimelineAttachmentShape)
            .border(1.dp, momentTimelineAttachmentBorderColor(), MomentTimelineAttachmentShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .background(momentTimelineAttachmentIconBackgroundColor(), MomentTimelineAttachmentIconShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconContentDescription,
                tint = ElementTheme.colors.iconPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
        Column {
            Text(
                text = filename,
                color = ElementTheme.colors.textPrimary,
                maxLines = 1,
                style = ElementTheme.typography.fontBodyMdRegular,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                color = ElementTheme.colors.textSecondary,
                style = ElementTheme.typography.fontBodyXsRegular,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = if (hasCaption) {
                    {}
                } else {
                    ContentAvoidingLayout.measureLastTextLine(
                        onContentLayoutChange = onContentLayoutChange,
                        extraWidth = iconSize + spacing + 24.dp
                    )
                },
            )
        }
    }
}

@Composable
private fun TimelineItemAttachmentCaptionView(
    caption: String,
    onContentLayoutChange: (ContentAvoidingLayoutData) -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = caption,
        color = ElementTheme.colors.textPrimary,
        style = ElementTheme.typography.fontBodyLgRegular,
        onTextLayout = ContentAvoidingLayout.measureLastTextLine(
            onContentLayoutChange = onContentLayoutChange,
        )
    )
}
