/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.messages.reply

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.core.extensions.toSafeLength
import io.element.android.libraries.designsystem.atomic.atoms.PlaceholderAtom
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.icons.CompoundDrawables
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileDetails
import io.element.android.libraries.matrix.api.timeline.item.event.getAvatarUrl
import io.element.android.libraries.matrix.api.timeline.item.event.getDisambiguatedDisplayName
import io.element.android.libraries.matrix.api.timeline.item.event.getDisplayName
import io.element.android.libraries.matrix.ui.components.AttachmentThumbnail
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun InReplyToView(
    inReplyTo: InReplyToDetails,
    hideImage: Boolean,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
    placement: InReplyToPlacement = InReplyToPlacement.Timeline,
) {
    when (inReplyTo) {
        is InReplyToDetails.Ready -> {
            ReplyToReadyContent(
                senderId = inReplyTo.senderId,
                senderProfile = inReplyTo.senderProfile,
                metadata = inReplyTo.metadata(hideImage),
                maxLines = maxLines,
                modifier = modifier,
                placement = placement,
            )
        }
        is InReplyToDetails.Error ->
            ReplyToErrorContent(data = inReplyTo, maxLines = maxLines, modifier = modifier, placement = placement)
        is InReplyToDetails.Loading ->
            ReplyToLoadingContent(modifier = modifier, placement = placement)
    }
}

enum class InReplyToPlacement {
    Timeline,
    Composer
}

@Composable
private fun ReplyToReadyContent(
    senderId: UserId,
    senderProfile: ProfileDetails,
    metadata: InReplyToMetadata?,
    maxLines: Int,
    placement: InReplyToPlacement,
    modifier: Modifier = Modifier,
) {
    ReplyShell(
        placement = placement,
        modifier = modifier,
    ) {
        val senderName = senderProfile.getDisambiguatedDisplayName(senderId)
        val a11InReplyToText = stringResource(CommonStrings.common_in_reply_to, senderName)
        Column(
            modifier = Modifier.semantics(mergeDescendants = false) { isTraversalGroup = true },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (placement == InReplyToPlacement.Timeline) {
                ReplySenderBadge(
                    senderId = senderId,
                    senderProfile = senderProfile,
                    contentDescription = a11InReplyToText,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                if (metadata is InReplyToMetadata.Thumbnail) {
                    val iconSize = if (placement == InReplyToPlacement.Composer) 20.dp else 24.dp
                    AttachmentThumbnail(
                        info = metadata.attachmentThumbnailInfo,
                        backgroundColor = momentReplyIconBackground(),
                        modifier = Modifier
                            .size(iconSize)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
                ReplyToContentText(metadata, maxLines)
            }
        }
    }
}

@Composable
private fun ReplyToLoadingContent(
    modifier: Modifier = Modifier,
    placement: InReplyToPlacement,
) {
    ReplyShell(
        placement = placement,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (placement == InReplyToPlacement.Timeline) {
                PlaceholderAtom(width = 96.dp, height = 22.dp)
            }
            PlaceholderAtom(width = 140.dp, height = 14.dp)
        }
    }
}

@Composable
private fun ReplyToErrorContent(
    data: InReplyToDetails.Error,
    maxLines: Int,
    modifier: Modifier = Modifier,
    placement: InReplyToPlacement,
) {
    ReplyShell(
        placement = placement,
        modifier = modifier,
    ) {
        Text(
            text = data.message,
            style = ElementTheme.typography.fontBodySmRegular,
            color = ElementTheme.colors.textCriticalPrimary,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ReplyShell(
    placement: InReplyToPlacement,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(if (placement == InReplyToPlacement.Composer) 16.dp else 14.dp))
            .background(momentReplyBackground())
            .height(IntrinsicSize.Min)
            .padding(
                horizontal = if (placement == InReplyToPlacement.Composer) 10.dp else 8.dp,
                vertical = if (placement == InReplyToPlacement.Composer) 8.dp else 6.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(if (placement == InReplyToPlacement.Composer) 10.dp else 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(1.dp))
                .background(momentReplyStripe())
        )
        content()
    }
}

@Composable
private fun ReplySenderBadge(
    senderId: UserId,
    senderProfile: ProfileDetails,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(momentReplySenderBadgeBackground())
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
            .semantics {
                this.contentDescription = contentDescription
                isTraversalGroup = true
                traversalIndex = 1f
            },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            avatarData = AvatarData(
                id = senderId.value,
                name = senderProfile.getDisplayName(),
                url = senderProfile.getAvatarUrl(),
                size = AvatarSize.TimelineReadReceipt,
            ),
            avatarType = AvatarType.User,
        )
        Text(
            text = senderProfile.getDisambiguatedDisplayName(senderId),
            style = ElementTheme.typography.fontBodyXsMedium,
            color = ElementTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ReplyToContentText(
    metadata: InReplyToMetadata?,
    maxLines: Int,
) {
    val text = when (metadata) {
        InReplyToMetadata.Redacted -> stringResource(id = CommonStrings.common_message_removed)
        InReplyToMetadata.UnableToDecrypt -> stringResource(id = CommonStrings.common_waiting_for_decryption_key)
        // Add a limit to the text length to avoid a crash in Compose
        is InReplyToMetadata.Text -> metadata.text.toSafeLength()
        // Add a limit to the text length to avoid a crash in Compose
        is InReplyToMetadata.Thumbnail -> metadata.text.toSafeLength()
        null -> ""
    }
    val iconResourceId = when (metadata) {
        InReplyToMetadata.Redacted -> CompoundDrawables.ic_compound_delete
        InReplyToMetadata.UnableToDecrypt -> CompoundDrawables.ic_compound_time
        else -> null
    }
    val fontStyle = when (metadata) {
        is InReplyToMetadata.Informative -> FontStyle.Italic
        else -> FontStyle.Normal
    }
    Row(
        modifier = Modifier.semantics(mergeDescendants = false) {
            isTraversalGroup = true
            traversalIndex = -1f
        },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconResourceId != null) {
            Icon(
                resourceId = iconResourceId,
                tint = ElementTheme.colors.iconSecondary,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            style = ElementTheme.typography.fontBodySmRegular,
            fontStyle = fontStyle,
            textAlign = TextAlign.Start,
            color = ElementTheme.colors.textSecondary,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun momentReplyBackground(): Color {
    return if (ElementTheme.isLightTheme) {
        Color.Black.copy(alpha = 0.035f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
}

@Composable
private fun momentReplyStripe(): Color {
    return if (ElementTheme.isLightTheme) {
        Color.Black.copy(alpha = 0.12f)
    } else {
        Color.White.copy(alpha = 0.18f)
    }
}

@Composable
private fun momentReplySenderBadgeBackground(): Color {
    return if (ElementTheme.isLightTheme) {
        Color.Black.copy(alpha = 0.06f)
    } else {
        Color.White.copy(alpha = 0.12f)
    }
}

@Composable
private fun momentReplyIconBackground(): Color {
    return if (ElementTheme.isLightTheme) {
        Color.Black.copy(alpha = 0.04f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
}

@PreviewsDayNight
@Composable
internal fun InReplyToViewPreview(@PreviewParameter(provider = InReplyToDetailsProvider::class) inReplyTo: InReplyToDetails) = ElementPreview {
    InReplyToView(
        inReplyTo = inReplyTo,
        hideImage = false,
    )
}
