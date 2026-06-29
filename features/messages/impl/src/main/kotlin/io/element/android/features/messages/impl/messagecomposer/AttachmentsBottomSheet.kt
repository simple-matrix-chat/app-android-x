/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.messagecomposer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.messages.impl.R
import io.element.android.libraries.androidutils.ui.hideKeyboard
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AttachmentsBottomSheet(
    state: MessageComposerState,
    onSendLocationClick: () -> Unit,
    onCreatePollClick: () -> Unit,
    onStartVoiceMessageRecordingClick: () -> Unit,
    enableTextFormatting: Boolean,
    modifier: Modifier = Modifier,
) {
    val localView = LocalView.current
    var isVisible by rememberSaveable { mutableStateOf(state.showAttachmentSourcePicker) }

    BackHandler(enabled = isVisible) {
        isVisible = false
    }

    LaunchedEffect(state.showAttachmentSourcePicker) {
        isVisible = if (state.showAttachmentSourcePicker) {
            // We need to use this instead of `LocalFocusManager.clearFocus()` to hide the keyboard when focus is on an Android View
            localView.hideKeyboard()
            true
        } else {
            false
        }
    }
    // Send 'DismissAttachmentMenu' event when the bottomsheet was just hidden
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            state.eventSink(MessageComposerEvent.DismissAttachmentMenu)
        }
    }

    if (isVisible) {
        ModalBottomSheet(
            modifier = modifier,
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            ),
            onDismissRequest = { isVisible = false },
            scrollable = false,
        ) {
            AttachmentSourcePickerMenu(
                state = state,
                enableTextFormatting = enableTextFormatting,
                onSendLocationClick = onSendLocationClick,
                onCreatePollClick = onCreatePollClick,
                onStartVoiceMessageRecordingClick = onStartVoiceMessageRecordingClick,
            )
        }
    }
}

@Composable
private fun AttachmentSourcePickerMenu(
    state: MessageComposerState,
    onSendLocationClick: () -> Unit,
    onCreatePollClick: () -> Unit,
    onStartVoiceMessageRecordingClick: () -> Unit,
    enableTextFormatting: Boolean,
) {
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        AttachmentSourceItem(
            icon = CompoundIcons.ReactionAdd(),
            title = stringResource(R.string.screen_room_attachment_source_emoji),
            onClick = { state.eventSink(MessageComposerEvent.PickAttachmentSource.Emoji) },
        )
        AttachmentSourceItem(
            icon = CompoundIcons.Image(),
            title = stringResource(CommonStrings.common_sticker),
            onClick = { state.eventSink(MessageComposerEvent.PickAttachmentSource.Sticker) },
        )
        if (state.canShareLocation) {
            AttachmentSourceItem(
                icon = CompoundIcons.LocationPin(),
                title = stringResource(R.string.screen_room_attachment_source_location),
                onClick = {
                    state.eventSink(MessageComposerEvent.PickAttachmentSource.Location)
                    onSendLocationClick()
                },
            )
        }
        AttachmentSourceItem(
            icon = CompoundIcons.Image(),
            title = stringResource(R.string.screen_room_attachment_source_photo),
            onClick = { state.eventSink(MessageComposerEvent.PickAttachmentSource.FromGallery) },
        )
        AttachmentSourceItem(
            icon = CompoundIcons.VideoCall(),
            title = stringResource(CommonStrings.common_video),
            onClick = { state.eventSink(MessageComposerEvent.PickAttachmentSource.FromVideoGallery) },
        )
        AttachmentSourceItem(
            icon = CompoundIcons.Attachment(),
            title = stringResource(CommonStrings.common_file),
            onClick = { state.eventSink(MessageComposerEvent.PickAttachmentSource.FromFiles) },
        )
        AttachmentSourceItem(
            icon = CompoundIcons.UserProfile(),
            title = stringResource(R.string.screen_room_attachment_source_contact),
            onClick = { state.eventSink(MessageComposerEvent.PickAttachmentSource.Contact) },
        )
        AttachmentSourceItem(
            icon = CompoundIcons.MicOn(),
            title = stringResource(CommonStrings.common_voice_message),
            onClick = {
                state.eventSink(MessageComposerEvent.PickAttachmentSource.VoiceMessage)
                onStartVoiceMessageRecordingClick()
            },
        )
        AttachmentSourceItem(
            icon = CompoundIcons.TakePhoto(),
            title = stringResource(R.string.screen_room_attachment_source_camera),
            onClick = { state.eventSink(MessageComposerEvent.PickAttachmentSource.FromCamera) },
        )
        AttachmentSourceItem(
            icon = CompoundIcons.Polls(),
            title = stringResource(R.string.screen_room_attachment_source_poll),
            onClick = {
                state.eventSink(MessageComposerEvent.PickAttachmentSource.Poll)
                onCreatePollClick()
            },
        )
        if (enableTextFormatting) {
            AttachmentSourceItem(
                icon = CompoundIcons.TextFormatting(),
                title = stringResource(R.string.screen_room_attachment_text_formatting),
                onClick = { state.eventSink(MessageComposerEvent.ToggleTextFormatting(enabled = true)) },
            )
        }
    }
}

@Composable
private fun AttachmentSourceItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(ElementTheme.colors.bgSubtleSecondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(22.dp),
                imageVector = icon,
                contentDescription = null,
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = title,
            style = ElementTheme.typography.fontBodyLgRegular,
            color = ElementTheme.colors.textPrimary,
        )
    }
}

@PreviewsDayNight
@Composable
internal fun AttachmentSourcePickerMenuPreview() = ElementPreview {
    AttachmentSourcePickerMenu(
        state = aMessageComposerState(
            canShareLocation = true,
        ),
        onSendLocationClick = {},
        onCreatePollClick = {},
        onStartVoiceMessageRecordingClick = {},
        enableTextFormatting = true,
    )
}
