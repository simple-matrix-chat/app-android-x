/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.messagecomposer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.messages.impl.R
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactAttachmentPickerBottomSheet(
    state: MessageComposerState,
    modifier: Modifier = Modifier,
) {
    val pickerState = state.contactAttachmentPickerState
    if (!pickerState.isVisible) return

    BackHandler {
        state.eventSink(MessageComposerEvent.DismissContactAttachmentPicker)
    }

    ModalBottomSheet(
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = { state.eventSink(MessageComposerEvent.DismissContactAttachmentPicker) },
        scrollable = false,
    ) {
        ContactAttachmentPickerContent(
            state = pickerState,
            onDismiss = { state.eventSink(MessageComposerEvent.DismissContactAttachmentPicker) },
            onRequestPermission = { state.eventSink(MessageComposerEvent.RequestContactAttachmentPermission) },
            onRetry = { state.eventSink(MessageComposerEvent.RetryLoadContactAttachments) },
            onSelectContact = { state.eventSink(MessageComposerEvent.SelectContactAttachment(it.formattedContact)) },
        )
    }
}

@Composable
private fun ContactAttachmentPickerContent(
    state: ContactAttachmentPickerState,
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit,
    onSelectContact: (ContactAttachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        ContactAttachmentPickerHeader(onDismiss = onDismiss)
        when {
            state.permissionState != ContactAttachmentPermissionState.Granted -> ContactAttachmentPermissionContent(
                permissionState = state.permissionState,
                onRequestPermission = onRequestPermission,
            )
            state.isLoading -> ContactAttachmentLoadingContent()
            state.hasError -> ContactAttachmentMessageContent(
                icon = CompoundIcons.Warning(),
                title = stringResource(R.string.screen_room_attachment_contact_picker_error_title),
                message = stringResource(R.string.screen_room_attachment_contact_picker_error_message),
                actionText = stringResource(CommonStrings.action_retry),
                onActionClick = onRetry,
            )
            state.contacts.isEmpty() -> ContactAttachmentMessageContent(
                icon = CompoundIcons.UserProfile(),
                title = stringResource(R.string.screen_room_attachment_contact_picker_empty_title),
                message = stringResource(R.string.screen_room_attachment_contact_picker_empty_message),
                actionText = null,
                onActionClick = null,
            )
            else -> ContactAttachmentList(
                contacts = state.contacts,
                onSelectContact = onSelectContact,
            )
        }
    }
}

@Composable
private fun ContactAttachmentPickerHeader(
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.screen_room_attachment_contact_picker_title),
            style = ElementTheme.typography.fontBodyLgMedium,
            color = ElementTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = CompoundIcons.Close(),
                contentDescription = stringResource(CommonStrings.action_close),
                tint = ElementTheme.colors.iconPrimary,
            )
        }
    }
}

@Composable
private fun ContactAttachmentPermissionContent(
    permissionState: ContactAttachmentPermissionState,
    onRequestPermission: () -> Unit,
) {
    ContactAttachmentMessageContent(
        icon = if (permissionState == ContactAttachmentPermissionState.Denied) CompoundIcons.Settings() else CompoundIcons.UserProfile(),
        title = stringResource(R.string.screen_room_attachment_contact_picker_permission_title),
        message = stringResource(R.string.screen_room_attachment_contact_picker_permission_message),
        actionText = if (permissionState == ContactAttachmentPermissionState.Denied) {
            stringResource(CommonStrings.action_open_settings)
        } else {
            stringResource(R.string.screen_room_attachment_contact_picker_permission_allow)
        },
        onActionClick = onRequestPermission,
    )
}

@Composable
private fun ContactAttachmentLoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = ElementTheme.colors.iconPrimary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.screen_room_attachment_contact_picker_loading),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ContactAttachmentMessageContent(
    icon: ImageVector,
    title: String,
    message: String,
    actionText: String?,
    onActionClick: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(ElementTheme.colors.bgSubtleSecondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = icon,
                contentDescription = null,
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = ElementTheme.typography.fontBodyLgMedium,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                text = actionText,
                onClick = onActionClick,
            )
        }
    }
}

@Composable
private fun ContactAttachmentList(
    contacts: List<ContactAttachment>,
    onSelectContact: (ContactAttachment) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 492.dp)
            .padding(bottom = 8.dp),
    ) {
        items(
            items = contacts,
            key = { it.id },
        ) { contact ->
            ContactAttachmentRow(
                contact = contact,
                onClick = { onSelectContact(contact) },
            )
        }
    }
}

@Composable
private fun ContactAttachmentRow(
    contact: ContactAttachment,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
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
                imageVector = CompoundIcons.UserProfile(),
                contentDescription = null,
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = contact.displayName,
                style = ElementTheme.typography.fontBodyLgRegular,
                color = ElementTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (contact.details != null) {
                Text(
                    text = contact.details,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@PreviewsDayNight
@Composable
internal fun ContactAttachmentPickerContactsPreview() = ElementPreview {
    ContactAttachmentPickerContent(
        state = aContactAttachmentPickerState(
            contacts = persistentListOf(
                aContactAttachment(displayName = "Alice Smith", details = "+44 7700 900000"),
                aContactAttachment(id = "2", displayName = "Bob Johnson", details = "bob@example.org"),
            ),
        ),
        onDismiss = {},
        onRequestPermission = {},
        onRetry = {},
        onSelectContact = {},
    )
}

@PreviewsDayNight
@Composable
internal fun ContactAttachmentPickerPermissionPreview() = ElementPreview {
    ContactAttachmentPickerContent(
        state = aContactAttachmentPickerState(permissionState = ContactAttachmentPermissionState.Request),
        onDismiss = {},
        onRequestPermission = {},
        onRetry = {},
        onSelectContact = {},
    )
}

@PreviewsDayNight
@Composable
internal fun ContactAttachmentPickerEmptyPreview() = ElementPreview {
    ContactAttachmentPickerContent(
        state = aContactAttachmentPickerState(contacts = persistentListOf()),
        onDismiss = {},
        onRequestPermission = {},
        onRetry = {},
        onSelectContact = {},
    )
}

@PreviewsDayNight
@Composable
internal fun ContactAttachmentPickerErrorPreview() = ElementPreview {
    ContactAttachmentPickerContent(
        state = aContactAttachmentPickerState(hasError = true, contacts = persistentListOf()),
        onDismiss = {},
        onRequestPermission = {},
        onRetry = {},
        onSelectContact = {},
    )
}
