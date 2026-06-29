/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securityandprivacy.impl.root

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.element.android.appconfig.LearnMoreConfig
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.securityandprivacy.impl.R
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.coverage.ExcludeFromCoverage
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.components.async.AsyncActionViewDefaults
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.libraries.designsystem.components.dialogs.SaveChangesDialog
import io.element.android.libraries.designsystem.preview.ElementPreviewDark
import io.element.android.libraries.designsystem.preview.ElementPreviewLight
import io.element.android.libraries.designsystem.preview.PreviewWithLargeHeight
import io.element.android.libraries.designsystem.text.stringWithLink
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.RadioButton
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Switch
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SecurityAndPrivacyView(
    state: SecurityAndPrivacyState,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        state.eventSink(SecurityAndPrivacyEvent.Exit)
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 32.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            MomentSecurityTopBar(
                isSaveActionEnabled = state.canBeSaved,
                onBackClick = { state.eventSink(SecurityAndPrivacyEvent.Exit) },
                onSaveClick = { state.eventSink(SecurityAndPrivacyEvent.Save) },
            )
            if (state.showRoomAccessSection) {
                RoomAccessSection(state = state)
            }
            if (state.showRoomVisibilitySections) {
                RoomVisibilitySection(state.homeserverName)
                RoomAddressSection(
                    roomAddress = state.editedSettings.address,
                    homeserverName = state.homeserverName,
                    onRoomAddressClick = { state.eventSink(SecurityAndPrivacyEvent.EditRoomAddress) },
                    isVisibleInRoomDirectory = state.editedSettings.isVisibleInRoomDirectory,
                    onVisibilityChange = {
                        state.eventSink(SecurityAndPrivacyEvent.ToggleRoomVisibility)
                    },
                )
            }
            if (state.showEncryptionSection) {
                EncryptionSection(
                    isRoomEncrypted = state.editedSettings.isEncrypted,
                    // encryption can't be disabled once enabled
                    canToggleEncryption = !state.savedSettings.isEncrypted,
                    onToggleEncryption = { state.eventSink(SecurityAndPrivacyEvent.ToggleEncryptionState) },
                    showConfirmation = state.showEnableEncryptionConfirmation,
                    onDismissConfirmation = { state.eventSink(SecurityAndPrivacyEvent.CancelEnableEncryption) },
                    onConfirmEncryption = { state.eventSink(SecurityAndPrivacyEvent.ConfirmEnableEncryption) },
                )
            }
            if (state.showHistoryVisibilitySection) {
                HistoryVisibilitySection(
                    editedOption = state.editedSettings.historyVisibility,
                    savedOptions = state.savedSettings.historyVisibility,
                    availableOptions = state.availableHistoryVisibilities,
                    onSelectOption = { state.eventSink(SecurityAndPrivacyEvent.ChangeHistoryVisibility(it)) },
                    onLinkClick = onLinkClick,
                )
            }
        }
    }
    AsyncActionView(
        async = state.saveAction,
        onSuccess = { },
        onErrorDismiss = { state.eventSink(SecurityAndPrivacyEvent.DismissSaveError) },
        confirmationDialog = { confirming ->
            when (confirming) {
                is AsyncAction.ConfirmingCancellation ->
                    SaveChangesDialog(
                        onSaveClick = { state.eventSink(SecurityAndPrivacyEvent.Save) },
                        onDiscardClick = { state.eventSink(SecurityAndPrivacyEvent.Exit) },
                        onDismiss = { state.eventSink(SecurityAndPrivacyEvent.DismissExitConfirmation) }
                    )
            }
        },
        errorMessage = { stringResource(CommonStrings.error_unknown) },
        progressDialog = {
            AsyncActionViewDefaults.ProgressDialog(
                progressText = stringResource(CommonStrings.common_saving),
            )
        },
        onRetry = { state.eventSink(SecurityAndPrivacyEvent.Save) },
    )
}

@Composable
private fun MomentSecurityTopBar(
    isSaveActionEnabled: Boolean,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBackClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = CompoundIcons.ChevronLeft(),
                contentDescription = stringResource(CommonStrings.action_back),
                tint = ElementTheme.colors.iconPrimary,
            )
        }
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            text = stringResource(R.string.screen_security_and_privacy_title),
            style = ElementTheme.typography.fontHeadingSmMedium,
            color = ElementTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        TextButton(
            text = stringResource(CommonStrings.action_save),
            enabled = isSaveActionEnabled,
            onClick = onSaveClick,
        )
    }
}

@Composable
private fun MomentSecuritySection(
    title: String,
    modifier: Modifier = Modifier,
    footer: AnnotatedString? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = ElementTheme.typography.fontBodySmMedium,
            color = ElementTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        content()
        if (footer != null) {
            Text(
                text = footer,
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun MomentSecurityCard(
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
private fun RoomAccessSection(
    state: SecurityAndPrivacyState,
    modifier: Modifier = Modifier,
) {
    val edited = state.editedSettings.roomAccess

    fun onSelectOption(option: SecurityAndPrivacyRoomAccess) {
        state.eventSink(SecurityAndPrivacyEvent.ChangeRoomAccess(option))
    }

    fun onSpaceMemberAccessClick() {
        state.eventSink(SecurityAndPrivacyEvent.SelectSpaceMemberAccess)
    }

    fun onAskToJoinWithSpaceMembersClick() {
        state.eventSink(SecurityAndPrivacyEvent.SelectAskToJoinWithSpaceMembersAccess)
    }

    fun onManageSpacesClick() {
        state.eventSink(SecurityAndPrivacyEvent.ManageAuthorizedSpaces)
    }

    MomentSecuritySection(
        title = stringResource(R.string.screen_security_and_privacy_room_access_section_header),
        modifier = modifier,
        footer = if (state.showManageSpaceFooter) {
            stringWithLink(
                textRes = R.string.screen_security_and_privacy_room_access_footer,
                url = "",
                linkTextRes = R.string.screen_security_and_privacy_room_access_footer_manage_spaces_action,
                onLinkClick = { onManageSpacesClick() },
            )
        } else {
            null
        },
    ) {
        MomentSecurityCard {
            MomentSecuritySelectionRow(
                title = stringResource(R.string.screen_security_and_privacy_room_access_anyone_option_title),
                description = stringResource(R.string.screen_security_and_privacy_room_access_anyone_option_description),
                imageVector = CompoundIcons.Public(),
                selected = edited == SecurityAndPrivacyRoomAccess.Anyone,
                onClick = { onSelectOption(SecurityAndPrivacyRoomAccess.Anyone) },
            )
            if (state.showSpaceMemberOption) {
                MomentSecuritySelectionRow(
                    title = stringResource(R.string.screen_security_and_privacy_room_access_space_members_option_title),
                    description = state.spaceMemberDescription(),
                    imageVector = CompoundIcons.Space(),
                    selected = state.editedSettings.roomAccess is SecurityAndPrivacyRoomAccess.SpaceMember,
                    enabled = state.isSpaceMemberSelectable,
                    onClick = ::onSpaceMemberAccessClick,
                )
            }
            if (state.showAskToJoinOption) {
                MomentSecuritySelectionRow(
                    title = stringResource(R.string.screen_security_and_privacy_ask_to_join_option_title),
                    description = stringResource(R.string.screen_security_and_privacy_ask_to_join_option_description),
                    imageVector = CompoundIcons.UserAdd(),
                    selected = edited == SecurityAndPrivacyRoomAccess.AskToJoin,
                    enabled = state.isAskToJoinSelectable,
                    onClick = { onSelectOption(SecurityAndPrivacyRoomAccess.AskToJoin) },
                )
            }
            if (state.showAskToJoinWithSpaceMemberOption) {
                MomentSecuritySelectionRow(
                    title = stringResource(R.string.screen_security_and_privacy_ask_to_join_option_title),
                    description = state.askToJoinWithSpaceMembersDescription(),
                    imageVector = CompoundIcons.UserAdd(),
                    selected = edited is SecurityAndPrivacyRoomAccess.AskToJoinWithSpaceMember,
                    enabled = state.isAskToJoinWithSpaceMembersSelectable,
                    onClick = ::onAskToJoinWithSpaceMembersClick,
                )
            }
            MomentSecuritySelectionRow(
                title = stringResource(R.string.screen_security_and_privacy_room_access_invite_only_option_title),
                description = stringResource(R.string.screen_security_and_privacy_room_access_invite_only_option_description),
                imageVector = CompoundIcons.Lock(),
                selected = edited == SecurityAndPrivacyRoomAccess.InviteOnly,
                onClick = { onSelectOption(SecurityAndPrivacyRoomAccess.InviteOnly) },
                showDivider = false,
            )
        }
    }
}

@Composable
private fun RoomVisibilitySection(
    homeserverName: String,
    modifier: Modifier = Modifier,
) {
    MomentSecuritySection(
        title = stringResource(R.string.screen_security_and_privacy_room_visibility_section_header),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.screen_security_and_privacy_room_visibility_section_footer, homeserverName),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun RoomAddressSection(
    roomAddress: String?,
    homeserverName: String,
    isVisibleInRoomDirectory: AsyncData<Boolean>,
    onRoomAddressClick: () -> Unit,
    onVisibilityChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MomentSecuritySection(
        title = stringResource(R.string.screen_security_and_privacy_room_address_section_header),
        modifier = modifier,
        footer = AnnotatedString(stringResource(R.string.screen_security_and_privacy_room_address_section_footer)),
    ) {
        MomentSecurityCard {
            MomentSecurityNavigationRow(
                title = roomAddress ?: stringResource(R.string.screen_security_and_privacy_add_room_address_action),
                description = null,
                imageVector = if (roomAddress.isNullOrEmpty()) CompoundIcons.Plus() else CompoundIcons.Public(),
                onClick = onRoomAddressClick,
                accent = roomAddress.isNullOrEmpty(),
            )
            MomentSecuritySwitchRow(
                title = stringResource(R.string.screen_security_and_privacy_room_directory_visibility_toggle_title),
                description = stringResource(R.string.screen_security_and_privacy_room_directory_visibility_toggle_description, homeserverName),
                imageVector = CompoundIcons.Public(),
                checked = isVisibleInRoomDirectory is AsyncData.Success && isVisibleInRoomDirectory.data,
                enabled = isVisibleInRoomDirectory.isSuccess(),
                onCheckedChange = onVisibilityChange,
                showDivider = false,
                trailingContent = {
                    when (isVisibleInRoomDirectory) {
                        is AsyncData.Uninitialized,
                        is AsyncData.Loading -> CircularProgressIndicator(
                            modifier = Modifier
                                .progressSemantics()
                                .size(20.dp),
                            strokeWidth = 2.dp
                        )
                        is AsyncData.Failure -> Switch(
                            checked = false,
                            enabled = false,
                            onCheckedChange = null,
                        )
                        is AsyncData.Success -> Switch(
                            checked = isVisibleInRoomDirectory.data,
                            onCheckedChange = { onVisibilityChange() },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun EncryptionSection(
    isRoomEncrypted: Boolean,
    canToggleEncryption: Boolean,
    showConfirmation: Boolean,
    onToggleEncryption: () -> Unit,
    onConfirmEncryption: () -> Unit,
    onDismissConfirmation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MomentSecuritySection(
        title = stringResource(R.string.screen_security_and_privacy_encryption_section_header),
        modifier = modifier,
        footer = AnnotatedString(stringResource(R.string.screen_security_and_privacy_encryption_section_footer)),
    ) {
        MomentSecurityCard {
            MomentSecuritySwitchRow(
                title = stringResource(R.string.screen_security_and_privacy_encryption_toggle_title),
                imageVector = CompoundIcons.Lock(),
                checked = isRoomEncrypted,
                enabled = canToggleEncryption,
                onCheckedChange = onToggleEncryption,
                showDivider = false,
            )
        }
    }
    if (showConfirmation) {
        ConfirmationDialog(
            title = stringResource(R.string.screen_security_and_privacy_enable_encryption_alert_title),
            content = stringResource(R.string.screen_security_and_privacy_enable_encryption_alert_description),
            submitText = stringResource(R.string.screen_security_and_privacy_enable_encryption_alert_confirm_button_title),
            onSubmitClick = onConfirmEncryption,
            onDismiss = onDismissConfirmation,
        )
    }
}

@Composable
private fun HistoryVisibilitySection(
    editedOption: SecurityAndPrivacyHistoryVisibility?,
    savedOptions: SecurityAndPrivacyHistoryVisibility?,
    availableOptions: ImmutableList<SecurityAndPrivacyHistoryVisibility>,
    onSelectOption: (SecurityAndPrivacyHistoryVisibility) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    MomentSecuritySection(
        title = stringResource(R.string.screen_security_and_privacy_room_history_section_header),
        footer = stringWithLink(
            textRes = R.string.screen_security_and_privacy_room_history_section_footer,
            url = LearnMoreConfig.HISTORY_VISIBLE_URL,
            onLinkClick = onLinkClick,
        ),
        modifier = modifier,
    ) {
        MomentSecurityCard {
            for ((index, availableOption) in availableOptions.withIndex()) {
                val isSelected = availableOption == editedOption
                HistoryVisibilityItem(
                    option = availableOption,
                    isSelected = isSelected,
                    onSelectOption = onSelectOption,
                    showDivider = index != availableOptions.lastIndex || savedOptions != null && !availableOptions.contains(savedOptions),
                )
            }
            // Also show the saved option if it's not in the available options, but disabled
            if (savedOptions != null && !availableOptions.contains(savedOptions)) {
                HistoryVisibilityItem(
                    option = savedOptions,
                    isSelected = true,
                    isEnabled = false,
                    onSelectOption = {},
                    showDivider = false,
                )
            }
        }
    }
}

@Composable
private fun HistoryVisibilityItem(
    option: SecurityAndPrivacyHistoryVisibility,
    isSelected: Boolean,
    onSelectOption: (SecurityAndPrivacyHistoryVisibility) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    showDivider: Boolean = true,
) {
    val headlineText = when (option) {
        SecurityAndPrivacyHistoryVisibility.Invited -> stringResource(R.string.screen_security_and_privacy_room_history_since_invite_option_title)
        SecurityAndPrivacyHistoryVisibility.Shared -> stringResource(R.string.screen_security_and_privacy_room_history_since_selecting_option_title)
        SecurityAndPrivacyHistoryVisibility.WorldReadable -> stringResource(R.string.screen_security_and_privacy_room_history_anyone_option_title)
    }
    MomentSecuritySelectionRow(
        title = headlineText,
        selected = isSelected,
        enabled = isEnabled,
        onClick = { onSelectOption(option) },
        modifier = modifier,
        showDivider = showDivider,
    )
}

@Composable
private fun MomentSecurityNavigationRow(
    title: String,
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    accent: Boolean = false,
    showDivider: Boolean = true,
) {
    MomentSecurityRowFrame(
        modifier = modifier,
        title = title,
        description = description,
        imageVector = imageVector,
        enabled = true,
        titleColor = if (accent) ElementTheme.colors.textActionAccent else ElementTheme.colors.textPrimary,
        iconTint = if (accent) ElementTheme.colors.iconAccentPrimary else ElementTheme.colors.iconPrimary,
        showDivider = showDivider,
        onClick = onClick,
        trailing = {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = CompoundIcons.ChevronRight(),
                contentDescription = null,
                tint = ElementTheme.colors.iconSecondary,
            )
        },
    )
}

@Composable
private fun MomentSecuritySelectionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    imageVector: ImageVector? = null,
    enabled: Boolean = true,
    showDivider: Boolean = true,
) {
    MomentSecurityRowFrame(
        modifier = modifier,
        title = title,
        description = description,
        imageVector = imageVector,
        enabled = enabled,
        showDivider = showDivider,
        onClick = onClick,
        trailing = {
            RadioButton(
                selected = selected,
                enabled = enabled,
                onClick = if (enabled) onClick else null,
            )
        },
    )
}

@Composable
private fun MomentSecuritySwitchRow(
    title: String,
    imageVector: ImageVector,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    showDivider: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    MomentSecurityRowFrame(
        modifier = modifier,
        title = title,
        description = description,
        imageVector = imageVector,
        enabled = enabled,
        showDivider = showDivider,
        onClick = onCheckedChange,
        trailing = trailingContent ?: {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = if (enabled) {
                    { onCheckedChange() }
                } else {
                    null
                },
            )
        },
    )
}

@Composable
private fun MomentSecurityRowFrame(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    imageVector: ImageVector? = null,
    titleColor: Color = if (enabled) ElementTheme.colors.textPrimary else ElementTheme.colors.textDisabled,
    iconTint: Color = if (enabled) ElementTheme.colors.iconPrimary else ElementTheme.colors.iconDisabled,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (description == null) 64.dp else 82.dp)
                .semantics(mergeDescendants = true) {}
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (imageVector != null) {
                MomentSecurityIconTile(
                    imageVector = imageVector,
                    tint = iconTint,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = ElementTheme.typography.fontBodyLgMedium,
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = if (enabled) ElementTheme.colors.textSecondary else ElementTheme.colors.textDisabled,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailing()
        }
        if (showDivider) {
            MomentSecurityDivider(start = if (imageVector == null) 16.dp else 76.dp)
        }
    }
}

@Composable
private fun MomentSecurityIconTile(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = ElementTheme.colors.iconPrimary,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(
                color = ElementTheme.colors.bgSubtleSecondary,
                shape = RoundedCornerShape(16.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = imageVector,
            contentDescription = null,
            tint = tint,
        )
    }
}

@Composable
private fun MomentSecurityDivider(
    modifier: Modifier = Modifier,
    start: Dp = 76.dp,
) {
    HorizontalDivider(
        modifier = modifier.padding(start = start),
        color = ElementTheme.colors.borderDisabled,
    )
}

@PreviewWithLargeHeight
@Composable
internal fun SecurityAndPrivacyViewLightPreview(@PreviewParameter(SecurityAndPrivacyStateProvider::class) state: SecurityAndPrivacyState) =
    ElementPreviewLight { ContentToPreview(state) }

@PreviewWithLargeHeight
@Composable
internal fun SecurityAndPrivacyViewDarkPreview(@PreviewParameter(SecurityAndPrivacyStateProvider::class) state: SecurityAndPrivacyState) =
    ElementPreviewDark { ContentToPreview(state) }

@ExcludeFromCoverage
@Composable
private fun ContentToPreview(state: SecurityAndPrivacyState) {
    SecurityAndPrivacyView(
        state = state,
        onLinkClick = {},
    )
}
