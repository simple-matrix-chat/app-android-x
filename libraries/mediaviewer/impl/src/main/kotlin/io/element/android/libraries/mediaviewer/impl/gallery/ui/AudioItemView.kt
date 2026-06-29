/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.mediaviewer.impl.gallery.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.mediaviewer.impl.model.MediaItem
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun AudioItemView(
    audio: MediaItem.Audio,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        FilenameRow(
            audio = audio,
            onClick = onClick,
            onLongClick = onLongClick,
        )
        val caption = audio.mediaInfo.caption
        if (caption != null) {
            CaptionView(caption)
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FilenameRow(
    audio: MediaItem.Audio,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    MomentMediaGalleryAttachmentRow(
        icon = CompoundIcons.Audio(),
        iconContentDescription = null,
        filename = audio.mediaInfo.filename,
        description = audio.mediaInfo.duration?.takeIf { it.isNotBlank() }
            ?: stringResource(CommonStrings.action_view),
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@PreviewsDayNight
@Composable
internal fun AudioItemViewPreview(
    @PreviewParameter(MediaItemAudioProvider::class) audio: MediaItem.Audio,
) = ElementPreview {
    AudioItemView(
        audio = audio,
        onClick = {},
        onLongClick = {},
    )
}
