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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.modifiers.onKeyboardContextMenuAction
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.mediaviewer.impl.model.MediaItem
import io.element.android.libraries.mediaviewer.impl.model.aMediaItemImage
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun ImageItemView(
    image: MediaItem.Image,
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
            model = image.thumbnailMediaRequestData,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            contentDescription = null,
        )
    }
}

@PreviewsDayNight
@Composable
internal fun ImageItemViewPreview() = ElementPreview {
    ImageItemView(
        image = aMediaItemImage(),
        onClick = {},
        onLongClick = {},
    )
}
