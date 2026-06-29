/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.mediaviewer.impl.gallery.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme

internal val MomentMediaGalleryMediaRadius = 18.dp
internal val MomentMediaGalleryAttachmentShape = RoundedCornerShape(16.dp)
internal val MomentMediaGalleryAttachmentIconShape = RoundedCornerShape(8.dp)

@Composable
internal fun momentMediaGalleryMediaBorderColor(): Color {
    return if (ElementTheme.isLightTheme) {
        Color(0xFFDEE2E9)
    } else {
        Color.White.copy(alpha = 0.10f)
    }
}

@Composable
internal fun momentMediaGalleryAttachmentBackgroundColor(): Color {
    return if (ElementTheme.isLightTheme) {
        Color(0xFFF2F2F7)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
}

@Composable
internal fun momentMediaGalleryAttachmentBorderColor(): Color {
    return if (ElementTheme.isLightTheme) {
        Color.Black.copy(alpha = 0.06f)
    } else {
        Color.White.copy(alpha = 0.12f)
    }
}

@Composable
internal fun momentMediaGalleryAttachmentIconBackgroundColor(): Color {
    return if (ElementTheme.isLightTheme) {
        Color.Black.copy(alpha = 0.045f)
    } else {
        Color.White.copy(alpha = 0.10f)
    }
}
