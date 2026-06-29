/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.messages.impl.timeline.TimelineRoomInfo
import io.element.android.features.messages.impl.timeline.aTimelineItemEvent
import io.element.android.features.messages.impl.timeline.aTimelineRoomInfo
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.features.messages.impl.timeline.model.event.RtcNotificationState
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemRtcNotificationContent
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.modifiers.onKeyboardContextMenuAction
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.notification.CallIntent
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
internal fun TimelineItemCallNotifyView(
    timelineRoomInfo: TimelineRoomInfo,
    event: TimelineItem.Event,
    content: TimelineItemRtcNotificationContent,
    onLongClick: (TimelineItem.Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ElementTheme.colors.bgCanvasDefault)
            .border(1.dp, ElementTheme.colors.borderInteractiveSecondary, shape)
            .combinedClickable(
                enabled = true,
                onClick = {},
                onLongClick = { onLongClick(event) },
                onLongClickLabel = stringResource(CommonStrings.action_open_context_menu),
            )
            .onKeyboardContextMenuAction { onLongClick(event) }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            avatarData = event.senderAvatar,
            avatarType = AvatarType.User,
            contentDescription = null,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = event.safeSenderName,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = ElementTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = getIcon(timelineRoomInfo, content),
                    contentDescription = null,
                    tint = ElementTheme.colors.iconSecondary,
                )
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = stringResource(getTextRes(timelineRoomInfo, content)),
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Text(
            text = event.sentTime,
            style = ElementTheme.typography.fontBodyXsRegular,
            color = ElementTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@StringRes
private fun getTextRes(
    timelineRoomInfo: TimelineRoomInfo,
    content: TimelineItemRtcNotificationContent
): Int = if (timelineRoomInfo.isDm) {
    when (content.state) {
        is RtcNotificationState.Declined -> {
            if (content.state.byMe) CommonStrings.common_call_you_declined else CommonStrings.common_call_declined
        }
        RtcNotificationState.Started -> CommonStrings.common_call_started
    }
} else {
    // In Rooms, do not show declined info.
    CommonStrings.common_call_started
}

@Composable
private fun getIcon(
    timelineRoomInfo: TimelineRoomInfo,
    content: TimelineItemRtcNotificationContent
): ImageVector {
    val showAsDeclined = timelineRoomInfo.isDm && content.state is RtcNotificationState.Declined
    val icon = if (showAsDeclined) {
        if (content.callIntent == CallIntent.AUDIO) CompoundIcons.VoiceCallDeclinedSolid() else CompoundIcons.VideoCallDeclinedSolid()
    } else {
        if (content.callIntent == CallIntent.AUDIO) CompoundIcons.VoiceCallSolid() else CompoundIcons.VideoCallSolid()
    }
    return icon
}

@PreviewsDayNight
@Composable
internal fun TimelineItemCallNotifyViewPreview() = ElementPreview {
    Column(modifier = Modifier.padding(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(false, true).forEach { isDm ->
            listOf(CallIntent.AUDIO, CallIntent.VIDEO).forEach { callIntent ->
                listOf(
                    RtcNotificationState.Started,
                    RtcNotificationState.Declined(byMe = false),
                    RtcNotificationState.Declined(byMe = true),
                ).forEach { state ->
                    val content = TimelineItemRtcNotificationContent(callIntent, state)
                    TimelineItemCallNotifyView(
                        timelineRoomInfo = aTimelineRoomInfo(isDm = isDm),
                        event = aTimelineItemEvent(content = content),
                        content = content,
                        onLongClick = {},
                    )
                }
            }
        }
    }
}
