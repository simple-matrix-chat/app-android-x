/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.mediaviewer.impl.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.core.mimetype.MimeTypes
import io.element.android.libraries.designsystem.colors.AvatarColorsProvider
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.modifiers.niceClickable
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.mediaviewer.api.MediaInfo
import io.element.android.libraries.mediaviewer.impl.R
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.libraries.ui.strings.Strings

/**
 * Ref: https://www.figma.com/design/pDlJZGBsri47FNTXMnEdXB/Compound-Android-Templates?node-id=2229-149220
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailsBottomSheet(
    state: MediaBottomSheetState.Details,
    onViewInTimeline: (EventId) -> Unit,
    onShare: (EventId) -> Unit,
    onForward: (EventId) -> Unit,
    onDownload: (EventId) -> Unit,
    onOpenWith: (EventId) -> Unit,
    onDelete: (EventId) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        scrollable = false,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Title()
            MomentMediaDetailsCard(
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Section(
                    title = stringResource(R.string.screen_media_details_uploaded_by),
                ) {
                    SenderRow(
                        mediaInfo = state.mediaInfo,
                    )
                }
                SectionText(
                    title = stringResource(R.string.screen_media_details_uploaded_on),
                    text = state.mediaInfo.dateSentFull.orEmpty(),
                )
                SectionText(
                    title = stringResource(R.string.screen_media_details_filename),
                    text = state.mediaInfo.filename,
                )
                SectionText(
                    title = stringResource(R.string.screen_media_details_file_format),
                    text = state.mediaInfo.mimeType + Strings.NICE_SEPARATOR + state.mediaInfo.formattedFileSize,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (state.eventId != null) {
                MomentMediaDetailsCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    MomentMediaActionRow(
                        text = stringResource(CommonStrings.action_view_in_timeline),
                        onClick = {
                            onViewInTimeline(state.eventId)
                        },
                    ) { iconColor ->
                        Icon(imageVector = CompoundIcons.VisibilityOn(), contentDescription = null, tint = iconColor)
                    }
                    MomentMediaDetailsDivider()
                    MomentMediaActionRow(
                        text = stringResource(CommonStrings.action_share),
                        onClick = {
                            onShare(state.eventId)
                        },
                    ) { iconColor ->
                        Icon(imageVector = CompoundIcons.ShareAndroid(), contentDescription = null, tint = iconColor)
                    }
                    MomentMediaDetailsDivider()
                    MomentMediaActionRow(
                        text = stringResource(CommonStrings.action_forward),
                        onClick = {
                            onForward(state.eventId)
                        },
                    ) { iconColor ->
                        Icon(imageVector = CompoundIcons.Forward(), contentDescription = null, tint = iconColor)
                    }
                    MomentMediaDetailsDivider()
                    MomentMediaActionRow(
                        text = stringResource(CommonStrings.action_download),
                        onClick = {
                            onDownload(state.eventId)
                        },
                    ) { iconColor ->
                        Icon(imageVector = CompoundIcons.Download(), contentDescription = null, tint = iconColor)
                    }
                    MomentMediaDetailsDivider()
                    val mimeType = state.mediaInfo.mimeType
                    val wording = when (mimeType) {
                        MimeTypes.Apk -> stringResource(id = CommonStrings.common_install_apk_android)
                        else -> stringResource(id = CommonStrings.action_open_with)
                    }
                    MomentMediaActionRow(
                        text = wording,
                        onClick = {
                            onOpenWith(state.eventId)
                        },
                    ) { iconColor ->
                        when (mimeType) {
                            MimeTypes.Apk -> Icon(
                                resourceId = R.drawable.ic_apk_install,
                                contentDescription = null,
                                tint = iconColor,
                            )
                            else -> Icon(imageVector = CompoundIcons.PopOut(), contentDescription = null, tint = iconColor)
                        }
                    }
                    if (state.canDelete) {
                        MomentMediaDetailsDivider()
                        MomentMediaActionRow(
                            text = stringResource(CommonStrings.action_delete),
                            destructive = true,
                            onClick = {
                                onDelete(state.eventId)
                            },
                        ) { iconColor ->
                            Icon(imageVector = CompoundIcons.Delete(), contentDescription = null, tint = iconColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MomentMediaDetailsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        border = BorderStroke(1.dp, ElementTheme.colors.borderDisabled.copy(alpha = 0.6f)),
        shadowElevation = 3.dp,
    ) {
        Column(content = content)
    }
}

@Composable
private fun MomentMediaDetailsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 64.dp),
        color = ElementTheme.colors.borderDisabled,
    )
}

@Composable
private fun MomentMediaActionRow(
    text: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
    leadingIcon: @Composable (Color) -> Unit,
) {
    val textColor = if (destructive) ElementTheme.colors.textCriticalPrimary else ElementTheme.colors.textPrimary
    val iconColor = if (destructive) ElementTheme.colors.iconCriticalPrimary else ElementTheme.colors.iconSecondary
    val iconBackground = if (destructive) ElementTheme.colors.bgCriticalSubtle else ElementTheme.colors.bgSubtleSecondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .niceClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconBackground, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            leadingIcon(iconColor)
        }
        Text(
            modifier = Modifier
                .padding(start = 14.dp)
                .weight(1f),
            text = text,
            color = textColor,
            style = ElementTheme.typography.fontBodyLgMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SenderRow(
    mediaInfo: MediaInfo,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val id = mediaInfo.senderId?.value ?: "@Alice:domain"
        Avatar(
            avatarData = AvatarData(
                id = id,
                name = mediaInfo.senderName,
                url = mediaInfo.senderAvatar,
                size = AvatarSize.MediaSender,
            ),
            avatarType = AvatarType.User,
        )
        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
        ) {
            // Name
            val bestName = mediaInfo.senderName ?: mediaInfo.senderId?.value.orEmpty()
            val avatarColors = AvatarColorsProvider.provide(id)
            Text(
                modifier = Modifier.clipToBounds(),
                text = bestName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = avatarColors.foreground,
                style = ElementTheme.typography.fontBodyMdMedium,
            )
            // Id
            if (!mediaInfo.senderName.isNullOrEmpty()) {
                Text(
                    text = mediaInfo.senderId?.value.orEmpty(),
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = ElementTheme.typography.fontBodyMdRegular,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.Title() {
    Text(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
            .semantics {
                heading()
            },
        text = stringResource(R.string.screen_media_details_title),
        textAlign = TextAlign.Center,
        style = ElementTheme.typography.fontBodyLgMedium,
        color = ElementTheme.colors.textPrimary,
    )
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = ElementTheme.typography.fontBodyMdMedium,
            color = ElementTheme.colors.textSecondary,
        )
        content()
    }
}

@Composable
private fun SectionText(
    title: String,
    text: String,
) {
    Section(title = title) {
        Text(
            text = text,
            style = ElementTheme.typography.fontBodyLgRegular,
            color = ElementTheme.colors.textPrimary,
        )
    }
}

@PreviewsDayNight
@Composable
internal fun MediaDetailsBottomSheetPreview(
    @PreviewParameter(MediaBottomSheetStateDetailsProvider::class) state: MediaBottomSheetState.Details,
) = ElementPreview {
    MediaDetailsBottomSheet(
        state = state,
        onViewInTimeline = {},
        onShare = {},
        onForward = {},
        onDownload = {},
        onOpenWith = {},
        onDelete = {},
        onDismiss = {},
    )
}
