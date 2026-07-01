/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user.editprofile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.components.async.AsyncActionViewDefaults
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.components.dialogs.SaveChangesDialog
import io.element.android.libraries.designsystem.modifiers.clearFocusOnTap
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.matrix.ui.components.AvatarActionBottomSheet
import io.element.android.libraries.matrix.ui.components.AvatarPickerState
import io.element.android.libraries.matrix.ui.components.AvatarPickerView
import io.element.android.libraries.permissions.api.PermissionsView
import io.element.android.libraries.ui.strings.CommonStrings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditUserProfileView(
    state: EditUserProfileState,
    onEditProfileSuccess: () -> Unit,
    onEditUsername: () -> Unit = {},
    onShareProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val isAvatarActionsSheetVisible = remember { mutableStateOf(false) }
    val statusPresets = listOf(
        stringResource(R.string.screen_moment_edit_profile_status_preset_available),
        stringResource(R.string.screen_moment_edit_profile_status_preset_busy),
        stringResource(R.string.screen_moment_edit_profile_status_preset_dnd),
        stringResource(R.string.screen_moment_edit_profile_status_preset_traveling),
    )

    fun onAvatarClick() {
        focusManager.clearFocus()
        isAvatarActionsSheetVisible.value = true
    }

    fun onBackClick() {
        focusManager.clearFocus()
        state.eventSink(EditUserProfileEvent.Exit)
    }

    BackHandler(
        enabled = true,
        ::onBackClick,
    )
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .clearFocusOnTap(focusManager),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            MomentEditProfileTopBar(
                saveEnabled = state.saveButtonEnabled,
                onBackClick = ::onBackClick,
                onSaveClick = {
                    focusManager.clearFocus()
                    state.eventSink(EditUserProfileEvent.Save)
                },
            )
            MomentIdentityCard(
                state = state,
                onAvatarClick = ::onAvatarClick,
            )
            MomentProfileSection(title = stringResource(R.string.screen_edit_profile_display_name)) {
                MomentProfileTextField(
                    value = state.displayName,
                    placeholder = stringResource(R.string.screen_edit_profile_display_name_placeholder),
                    enabled = state.canChangeDisplayName,
                    singleLine = true,
                    onValueChange = { state.eventSink(EditUserProfileEvent.UpdateDisplayName(it)) },
                    onDone = {
                        focusManager.clearFocus()
                        if (state.saveButtonEnabled) {
                            state.eventSink(EditUserProfileEvent.Save)
                        }
                    },
                )
            }
            MomentProfileSection(title = stringResource(R.string.screen_moment_edit_profile_set_status)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.screen_moment_edit_profile_your_status),
                        style = ElementTheme.typography.fontBodyLgMedium,
                        color = ElementTheme.colors.textPrimary,
                    )
                    MomentProfileTextField(
                        value = state.status,
                        placeholder = stringResource(R.string.screen_moment_edit_profile_status_placeholder),
                        enabled = !state.isLoadingProfileStatus,
                        singleLine = false,
                        onValueChange = { state.eventSink(EditUserProfileEvent.UpdateStatus(it)) },
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        statusPresets.forEach { preset ->
                            MomentStatusChip(
                                title = preset,
                                selected = normalizeStatus(state.status) == normalizeStatus(preset),
                                enabled = !state.isLoadingProfileStatus,
                                onClick = { state.eventSink(EditUserProfileEvent.SelectStatusPreset(preset)) },
                            )
                        }
                    }
                }
            }
            if (state.showProfileUsername || state.profileShareText != null) {
                MomentProfileCard {
                    if (state.showProfileUsername) {
                        MomentProfileActionRow(
                            title = stringResource(R.string.screen_moment_edit_profile_username),
                            subtitle = state.username
                                .takeIf { it.isNotBlank() }
                                ?.let { "@$it" }
                                ?: stringResource(R.string.screen_moment_username_not_set),
                            imageVector = CompoundIcons.Mention(),
                            trailingImageVector = CompoundIcons.ChevronRight(),
                            trailingContentDescription = null,
                            onClick = onEditUsername,
                        )
                    }
                    if (state.showProfileUsername && state.profileShareText != null) {
                        MomentProfileRowDivider()
                    }
                    if (state.profileShareText != null) {
                        MomentProfileActionRow(
                            title = stringResource(R.string.screen_moment_edit_profile_profile_link_title),
                            subtitle = stringResource(R.string.screen_moment_edit_profile_profile_link_description),
                            imageVector = CompoundIcons.Link(),
                            trailingImageVector = CompoundIcons.ShareAndroid(),
                            trailingContentDescription = stringResource(CommonStrings.action_share),
                            onClick = { onShareProfile(state.profileShareText) },
                        )
                    }
                }
            }
        }

        AvatarActionBottomSheet(
            actions = state.avatarActions,
            isVisible = isAvatarActionsSheetVisible.value,
            onDismiss = { isAvatarActionsSheetVisible.value = false },
            onSelectAction = { state.eventSink(EditUserProfileEvent.HandleAvatarAction(it)) }
        )

        AsyncActionView(
            async = state.saveAction,
            progressDialog = {
                AsyncActionViewDefaults.ProgressDialog(
                    progressText = stringResource(R.string.screen_edit_profile_updating_details),
                )
            },
            confirmationDialog = { confirming ->
                when (confirming) {
                    is AsyncAction.ConfirmingCancellation -> {
                        SaveChangesDialog(
                            onSaveClick = { state.eventSink(EditUserProfileEvent.Save) },
                            onDiscardClick = { state.eventSink(EditUserProfileEvent.Exit) },
                            onDismiss = { state.eventSink(EditUserProfileEvent.CloseDialog) },
                        )
                    }
                }
            },
            onSuccess = { onEditProfileSuccess() },
            errorTitle = { stringResource(R.string.screen_edit_profile_error_title) },
            errorMessage = { stringResource(R.string.screen_edit_profile_error) },
            onErrorDismiss = { state.eventSink(EditUserProfileEvent.CloseDialog) },
        )
    }
    PermissionsView(
        state = state.cameraPermissionState,
    )
}

@Composable
private fun MomentEditProfileTopBar(
    saveEnabled: Boolean,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MomentProfileIconButton(
                imageVector = CompoundIcons.ChevronLeft(),
                contentDescription = stringResource(CommonStrings.action_back),
                onClick = onBackClick,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (saveEnabled) {
                TextButton(
                    text = stringResource(CommonStrings.action_save),
                    enabled = true,
                    onClick = onSaveClick,
                )
            }
        }
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 8.dp),
            text = stringResource(R.string.screen_moment_edit_profile_title),
            style = ElementTheme.typography.fontHeadingMdBold,
            color = ElementTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MomentIdentityCard(
    state: EditUserProfileState,
    onAvatarClick: () -> Unit,
) {
    val profileName = state.displayName.trim().ifEmpty { state.userId.value }
    val statusText = when {
        state.isLoadingProfileStatus -> stringResource(CommonStrings.common_loading)
        state.status.isBlank() -> stringResource(R.string.screen_moment_edit_profile_no_status)
        else -> state.status.trim()
    }
    val avatarPickerState = remember(state.userAvatarUrl, state.displayName) {
        AvatarPickerState.Selected(
            avatarData = AvatarData(
                id = state.userId.value,
                name = profileName,
                size = AvatarSize.EditProfileDetails,
                url = state.userAvatarUrl,
            ),
            type = AvatarType.User,
        )
    }

    MomentProfileCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AvatarPickerView(
                state = avatarPickerState,
                onClick = onAvatarClick,
                enabled = state.canChangeAvatarUrl,
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = profileName,
                    style = ElementTheme.typography.fontHeadingLgBold,
                    color = ElementTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.showProfileUsername && state.username.isNotBlank()) {
                    Text(
                        text = "@${state.username}",
                        style = ElementTheme.typography.fontBodyMdMedium,
                        color = ElementTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.phoneNumber.isNotBlank()) {
                    Text(
                        text = state.phoneNumber,
                        style = ElementTheme.typography.fontBodyMdMedium,
                        color = ElementTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = state.userId.value,
                    style = ElementTheme.typography.fontBodyMdMedium,
                    color = ElementTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = statusText,
                    style = ElementTheme.typography.fontBodyMdMedium,
                    color = ElementTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MomentProfileActionRow(
    title: String,
    subtitle: String,
    imageVector: ImageVector,
    trailingImageVector: ImageVector,
    trailingContentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = ElementTheme.colors.bgSubtleSecondary,
                    shape = RoundedCornerShape(20.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = imageVector,
                contentDescription = null,
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = ElementTheme.typography.fontBodyLgMedium,
                color = ElementTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            modifier = Modifier.size(22.dp),
            imageVector = trailingImageVector,
            contentDescription = trailingContentDescription,
            tint = ElementTheme.colors.iconSecondary,
        )
    }
}

@Composable
private fun MomentProfileRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 60.dp)
            .height(1.dp)
            .background(ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.35f)),
    )
}

@Composable
private fun MomentProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = title,
            style = ElementTheme.typography.fontBodyMdMedium,
            color = ElementTheme.colors.textSecondary,
        )
        MomentProfileCard(content = content)
    }
}

@Composable
private fun MomentUsernameTextField(
    value: String,
    placeholder: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = ElementTheme.colors.bgSubtleSecondary,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "@",
            style = ElementTheme.typography.fontBodyLgMedium,
            color = if (enabled) ElementTheme.colors.textPrimary else ElementTheme.colors.textDisabled,
        )
        BasicTextField(
            modifier = Modifier.weight(1f),
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = ElementTheme.typography.fontBodyLgMedium.copy(
                color = if (enabled) ElementTheme.colors.textPrimary else ElementTheme.colors.textDisabled,
            ),
            cursorBrush = SolidColor(ElementTheme.colors.textPrimary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = ElementTheme.typography.fontBodyLgMedium,
                            color = ElementTheme.colors.textSecondary,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun usernameValidationText(error: EditUserProfileUsernameError?): String {
    return when (error) {
        null -> stringResource(R.string.screen_moment_edit_profile_username_rules)
        EditUserProfileUsernameError.Required -> stringResource(R.string.screen_moment_edit_profile_username_required)
        EditUserProfileUsernameError.TooShort -> stringResource(R.string.screen_moment_edit_profile_username_too_short)
        EditUserProfileUsernameError.TooLong -> stringResource(R.string.screen_moment_edit_profile_username_too_long)
        EditUserProfileUsernameError.Invalid -> stringResource(R.string.screen_moment_edit_profile_username_invalid)
        EditUserProfileUsernameError.Taken -> stringResource(R.string.screen_moment_edit_profile_username_taken)
        EditUserProfileUsernameError.Unsupported -> stringResource(R.string.screen_moment_edit_profile_username_unsupported)
        EditUserProfileUsernameError.SaveFailed -> stringResource(R.string.screen_moment_edit_profile_username_save_failed)
    }
}

@Composable
private fun MomentProfileTextField(
    value: String,
    placeholder: String,
    enabled: Boolean,
    singleLine: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
) {
    BasicTextField(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = ElementTheme.colors.bgSubtleSecondary,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        maxLines = if (singleLine) 1 else 5,
        textStyle = ElementTheme.typography.fontBodyLgMedium.copy(
            color = if (enabled) ElementTheme.colors.textPrimary else ElementTheme.colors.textDisabled,
        ),
        cursorBrush = SolidColor(ElementTheme.colors.textPrimary),
        keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = ElementTheme.typography.fontBodyLgMedium,
                        color = ElementTheme.colors.textSecondary,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun MomentStatusChip(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) ElementTheme.colors.bgActionPrimaryRest else ElementTheme.colors.bgSubtleSecondary)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = ElementTheme.typography.fontBodyMdMedium,
            color = when {
                !enabled -> ElementTheme.colors.textDisabled
                selected -> ElementTheme.colors.textOnSolidPrimary
                else -> ElementTheme.colors.textPrimary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MomentProfileCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElementTheme.colors.bgCanvasDefault,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, ElementTheme.colors.borderInteractiveSecondary.copy(alpha = 0.55f)),
    ) {
        Column(content = content)
    }
}

@Composable
private fun MomentProfileIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = ElementTheme.colors.iconPrimary,
        )
    }
}

private fun normalizeStatus(value: String): String {
    return value.trim().lowercase()
}

@PreviewsDayNight
@Composable
internal fun EditUserProfileViewPreview(@PreviewParameter(EditUserProfileStateProvider::class) state: EditUserProfileState) =
    ElementPreview {
        EditUserProfileView(
            onEditProfileSuccess = {},
            onEditUsername = {},
            onShareProfile = {},
            state = state,
        )
    }
