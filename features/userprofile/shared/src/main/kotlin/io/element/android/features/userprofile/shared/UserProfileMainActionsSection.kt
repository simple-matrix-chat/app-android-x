/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.userprofile.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.DropdownMenu
import io.element.android.libraries.designsystem.theme.components.DropdownMenuItem
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.notification.CallIntent
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun UserProfileMainActionsSection(
    isCurrentUser: Boolean,
    canCall: Boolean,
    canShare: Boolean,
    onShareUser: () -> Unit,
    onStartDM: () -> Unit,
    onCall: (CallIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderDisabled),
    ) {
        Column {
            var hasPreviousAction = false

            if (!isCurrentUser) {
                MomentUserProfileActionRow(
                    title = stringResource(CommonStrings.action_message),
                    imageVector = CompoundIcons.Chat(),
                    onClick = onStartDM,
                )
                hasPreviousAction = true
            }

            if (canCall) {
                if (hasPreviousAction) {
                    MomentUserProfileActionDivider()
                }
                MomentUserProfileCallActionRow(
                    onCall = onCall,
                )
                hasPreviousAction = true
            }

            if (canShare) {
                if (hasPreviousAction) {
                    MomentUserProfileActionDivider()
                }
                MomentUserProfileActionRow(
                    title = stringResource(CommonStrings.action_share),
                    imageVector = CompoundIcons.ShareAndroid(),
                    onClick = onShareUser,
                )
            }
        }
    }
}

@Composable
private fun MomentUserProfileCallActionRow(
    onCall: (CallIntent) -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        MomentUserProfileActionRow(
            title = stringResource(CommonStrings.action_call),
            imageVector = CompoundIcons.VideoCall(),
            onClick = { isMenuExpanded = true },
        )
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(CommonStrings.a11y_start_voice_call)) },
                leadingIcon = {
                    Icon(
                        imageVector = CompoundIcons.VoiceCallSolid(),
                        contentDescription = null,
                    )
                },
                onClick = {
                    isMenuExpanded = false
                    onCall(CallIntent.AUDIO)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(CommonStrings.a11y_start_video_call)) },
                leadingIcon = {
                    Icon(
                        imageVector = CompoundIcons.VideoCallSolid(),
                        contentDescription = null,
                    )
                },
                onClick = {
                    isMenuExpanded = false
                    onCall(CallIntent.VIDEO)
                },
            )
        }
    }
}

@Composable
private fun MomentUserProfileActionRow(
    title: String,
    imageVector: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .semantics(mergeDescendants = true) {}
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(ElementTheme.colors.bgSubtleSecondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = imageVector,
                contentDescription = null,
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = ElementTheme.typography.fontBodyLgMedium,
            color = ElementTheme.colors.textPrimary,
        )
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = CompoundIcons.ChevronRight(),
            contentDescription = null,
            tint = ElementTheme.colors.iconSecondary,
        )
    }
}

@Composable
private fun MomentUserProfileActionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp),
        color = ElementTheme.colors.borderDisabled,
    )
}

@PreviewsDayNight()
@Composable
internal fun UserProfileMainActionsSectionPreview() = ElementPreview {
    UserProfileMainActionsSection(
        isCurrentUser = false,
        canCall = true,
        canShare = true,
        onShareUser = { },
        onStartDM = { },
        onCall = { }
    )
}
