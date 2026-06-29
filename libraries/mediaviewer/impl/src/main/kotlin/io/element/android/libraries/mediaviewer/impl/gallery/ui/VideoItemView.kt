/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.mediaviewer.impl.gallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.modifiers.onKeyboardContextMenuAction
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.mediaviewer.impl.model.MediaItem
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun VideoItemView(
    video: MediaItem.Video,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mediaShape = RoundedCornerShape(MomentMediaGalleryMediaRadius)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(mediaShape)
            .background(ElementTheme.colors.bgSubtleSecondary)
            .border(1.dp, momentMediaGalleryMediaBorderColor(), mediaShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = stringResource(CommonStrings.action_open_context_menu),
            )
            .onKeyboardContextMenuAction(onLongClick),
    ) {
        AsyncImage(
            modifier = Modifier.fillMaxWidth(),
            model = video.thumbnailMediaRequestData,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            contentDescription = null,
        )
        VideoInfoRow(
            video = video,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun VideoInfoRow(
    video: MediaItem.Video,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ElementTheme.colors.bgCanvasDefault.copy(alpha = 0f),
                        ElementTheme.colors.bgCanvasDefault,
                    )
                )
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = CompoundIcons.VideoCallSolid(),
            contentDescription = null
        )
        video.mediaInfo.duration?.let { duration ->
            Spacer(Modifier.weight(1f))
            Text(
                text = duration,
                style = ElementTheme.typography.fontBodySmMedium,
                color = ElementTheme.colors.textPrimary,
            )
        }
    }
}

@PreviewsDayNight
@Composable
internal fun VideoItemViewPreview(
    @PreviewParameter(MediaItemVideoProvider::class) video: MediaItem.Video,
) = ElementPreview {
    VideoItemView(
        video = video,
        onClick = {},
        onLongClick = {},
    )
}
