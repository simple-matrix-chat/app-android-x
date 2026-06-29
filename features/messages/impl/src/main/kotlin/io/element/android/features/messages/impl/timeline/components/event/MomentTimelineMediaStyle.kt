/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.event

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme

internal val MomentTimelineMediaRadius = 18.dp
internal val MomentTimelineCaptionedMediaRadius = 14.dp
internal val MomentTimelineAttachmentShape = RoundedCornerShape(16.dp)
internal val MomentTimelineAttachmentIconShape = RoundedCornerShape(8.dp)

@Composable
internal fun momentTimelineMediaBorderColor(): Color {
    return if (ElementTheme.isLightTheme) {
        Color(0xFFDEE2E9)
    } else {
        Color.White.copy(alpha = 0.10f)
    }
}

@Composable
internal fun momentTimelineAttachmentBackgroundColor(): Color {
    return if (ElementTheme.isLightTheme) {
        Color(0xFFF2F2F7)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
}

@Composable
internal fun momentTimelineAttachmentBorderColor(): Color {
    return if (ElementTheme.isLightTheme) {
        Color.Black.copy(alpha = 0.06f)
    } else {
        Color.White.copy(alpha = 0.12f)
    }
}

@Composable
internal fun momentTimelineAttachmentIconBackgroundColor(): Color {
    return if (ElementTheme.isLightTheme) {
        Color.Black.copy(alpha = 0.045f)
    } else {
        Color.White.copy(alpha = 0.10f)
    }
}
