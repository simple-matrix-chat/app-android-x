/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.androidutils.system.startNotificationSettingsIntent
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.components.dialogs.ListOption
import io.element.android.libraries.designsystem.components.dialogs.SingleSelectionDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.ButtonSize
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Surface
import io.element.android.libraries.designsystem.theme.components.Switch
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.OnLifecycleEvent
import io.element.android.libraries.fullscreenintent.api.FullScreenIntentPermissionsEvents
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.toImmutableList

/**
 * A view that allows a user edit their global notification settings.
 */
@Composable
fun NotificationSettingsView(
    state: NotificationSettingsState,
    onOpenEditDefault: (isOneToOne: Boolean) -> Unit,
    onTroubleshootNotificationsClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> state.eventSink.invoke(NotificationSettingsEvents.RefreshSystemNotificationsEnabled)
            else -> Unit
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ElementTheme.colors.bgSubtleSecondary,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 32.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            MomentNotificationTopBar(onBackClick = onBackClick)
            when (state.matrixSettings) {
                NotificationSettingsState.MatrixSettings.Uninitialized -> Unit
                is NotificationSettingsState.MatrixSettings.Valid -> {
                    if (state.matrixSettings.inconsistentSettings.isNotEmpty()) {
                        NotificationSettingsConfigurationMismatchView(
                            isLoading = state.changeNotificationSettingAction.isLoading(),
                            onContinueClick = { state.eventSink(NotificationSettingsEvents.FixConfigurationMismatch) },
                        )
                    } else {
                        NotificationSettingsContentView(
                            matrixSettings = state.matrixSettings,
                            state = state,
                            onNotificationsEnabledChange = { state.eventSink(NotificationSettingsEvents.SetNotificationsEnabled(it)) },
                            onGroupChatsClick = { onOpenEditDefault(false) },
                            onDirectChatsClick = { onOpenEditDefault(true) },
                            onMentionNotificationsChange = { state.eventSink(NotificationSettingsEvents.SetAtRoomNotificationsEnabled(it)) },
                            onCallsNotificationsChange = { state.eventSink(NotificationSettingsEvents.SetCallNotificationsEnabled(it)) },
                            onInviteForMeNotificationsChange = { state.eventSink(NotificationSettingsEvents.SetInviteForMeNotificationsEnabled(it)) },
                            onTroubleshootNotificationsClick = onTroubleshootNotificationsClick,
                        )
                    }
                }
            }
        }
        AsyncActionView(
            async = state.changeNotificationSettingAction,
            errorMessage = { stringResource(R.string.screen_notification_settings_edit_failed_updating_default_mode) },
            onErrorDismiss = { state.eventSink(NotificationSettingsEvents.ClearNotificationChangeError) },
            onSuccess = {},
        )
    }
}

@Composable
private fun NotificationSettingsConfigurationMismatchView(
    isLoading: Boolean,
    onContinueClick: () -> Unit,
) {
    MomentNotificationCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.screen_notification_settings_configuration_mismatch),
                    style = ElementTheme.typography.fontBodyLgMedium,
                    color = ElementTheme.colors.textPrimary,
                )
                Text(
                    text = stringResource(id = R.string.screen_notification_settings_configuration_mismatch_description),
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                )
            }
            Button(
                text = stringResource(id = CommonStrings.action_continue),
                size = ButtonSize.Medium,
                enabled = !isLoading,
                showProgress = isLoading,
                onClick = onContinueClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NotificationSettingsContentView(
    matrixSettings: NotificationSettingsState.MatrixSettings.Valid,
    state: NotificationSettingsState,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onGroupChatsClick: () -> Unit,
    onDirectChatsClick: () -> Unit,
    onMentionNotificationsChange: (Boolean) -> Unit,
    onCallsNotificationsChange: (Boolean) -> Unit,
    onInviteForMeNotificationsChange: (Boolean) -> Unit,
    onTroubleshootNotificationsClick: () -> Unit,
) {
    val context = LocalContext.current
    val systemSettings = state.appSettings

    if (systemSettings.appNotificationsEnabled && !systemSettings.systemNotificationsEnabled) {
        MomentNotificationCard {
            MomentNotificationNavigationRow(
                title = stringResource(id = R.string.screen_notification_settings_system_notifications_turned_off),
                description = stringResource(
                    id = R.string.screen_notification_settings_system_notifications_action_required,
                    stringResource(id = R.string.screen_notification_settings_system_notifications_action_required_content_link)
                ),
                imageVector = CompoundIcons.NotificationsOffSolid(),
                onClick = { context.startNotificationSettingsIntent() },
                showDivider = false,
            )
        }
    }

    MomentNotificationCard {
        MomentNotificationSwitchRow(
            title = stringResource(id = R.string.screen_notification_settings_enable_notifications),
            imageVector = CompoundIcons.Notifications(),
            checked = systemSettings.appNotificationsEnabled,
            onCheckedChange = { onNotificationsEnabledChange(!systemSettings.appNotificationsEnabled) },
            showDivider = false,
        )
    }

    if (systemSettings.appNotificationsEnabled) {
        if (!state.fullScreenIntentPermissionsState.permissionGranted) {
            MomentNotificationCard {
                MomentNotificationNavigationRow(
                    title = stringResource(id = R.string.full_screen_intent_banner_title),
                    description = stringResource(id = R.string.full_screen_intent_banner_message),
                    imageVector = CompoundIcons.VoiceCallSolid(),
                    onClick = {
                        state.fullScreenIntentPermissionsState.eventSink(FullScreenIntentPermissionsEvents.OpenSettings)
                    },
                    showDivider = false,
                )
            }
        }

        MomentNotificationSection(
            title = stringResource(id = R.string.screen_notification_settings_notification_section_title),
        ) {
            MomentNotificationCard {
                MomentNotificationNavigationRow(
                    title = stringResource(id = R.string.screen_notification_settings_group_chats),
                    imageVector = CompoundIcons.Room(),
                    trailingText = getTitleForRoomNotificationMode(mode = matrixSettings.defaultGroupNotificationMode),
                    onClick = onGroupChatsClick,
                )
                MomentNotificationNavigationRow(
                    title = stringResource(id = R.string.screen_notification_settings_direct_chats),
                    imageVector = CompoundIcons.Chat(),
                    trailingText = getTitleForRoomNotificationMode(mode = matrixSettings.defaultOneToOneNotificationMode),
                    onClick = onDirectChatsClick,
                    showDivider = false,
                )
            }
        }

        MomentNotificationSection(
            title = stringResource(id = R.string.screen_notification_settings_mentions_section_title),
        ) {
            MomentNotificationCard {
                MomentNotificationSwitchRow(
                    title = stringResource(id = R.string.screen_notification_settings_room_mention_label),
                    imageVector = CompoundIcons.Mention(),
                    checked = matrixSettings.atRoomNotificationsEnabled,
                    onCheckedChange = { onMentionNotificationsChange(!matrixSettings.atRoomNotificationsEnabled) },
                    showDivider = false,
                )
            }
        }

        MomentNotificationSection(
            title = stringResource(id = R.string.screen_notification_settings_additional_settings_section_title),
        ) {
            MomentNotificationCard {
                MomentNotificationSwitchRow(
                    title = stringResource(id = R.string.screen_notification_settings_calls_label),
                    imageVector = CompoundIcons.VoiceCall(),
                    checked = matrixSettings.callNotificationsEnabled,
                    onCheckedChange = { onCallsNotificationsChange(!matrixSettings.callNotificationsEnabled) },
                )
                MomentNotificationSwitchRow(
                    title = stringResource(id = R.string.screen_notification_settings_invite_for_me_label),
                    imageVector = CompoundIcons.UserAdd(),
                    checked = matrixSettings.inviteForMeNotificationsEnabled,
                    onCheckedChange = { onInviteForMeNotificationsChange(!matrixSettings.inviteForMeNotificationsEnabled) },
                    showDivider = false,
                )
            }
        }

        MomentNotificationSection(
            title = stringResource(id = R.string.troubleshoot_notifications_entry_point_section),
        ) {
            MomentNotificationCard {
                MomentNotificationNavigationRow(
                    title = stringResource(id = R.string.troubleshoot_notifications_entry_point_title),
                    imageVector = CompoundIcons.ChatProblem(),
                    onClick = onTroubleshootNotificationsClick,
                    showDivider = false,
                )
            }
        }

        if (state.showAdvancedSettings) {
            MomentNotificationSection(
                title = stringResource(id = CommonStrings.common_advanced_settings),
            ) {
                MomentNotificationCard {
                    MomentNotificationNavigationRow(
                        title = stringResource(id = R.string.screen_advanced_settings_push_provider_android),
                        imageVector = CompoundIcons.Settings(),
                        onClick = {
                            if (state.currentPushDistributor.isReady()) {
                                state.eventSink(NotificationSettingsEvents.ChangePushProvider)
                            }
                        },
                        showDivider = false,
                        trailingContent = {
                            PushProviderTrailing(state = state)
                        }
                    )
                }
            }
            if (state.showChangePushProviderDialog) {
                SingleSelectionDialog(
                    title = stringResource(id = R.string.screen_advanced_settings_choose_distributor_dialog_title_android),
                    options = state.availablePushDistributors.map { distributor ->
                        val title = if (state.availablePushDistributors.count { it.name == distributor.name } > 1) {
                            distributor.fullName
                        } else {
                            distributor.name
                        }
                        ListOption(title = title)
                    }.toImmutableList(),
                    initialSelection = state.availablePushDistributors.indexOf(state.currentPushDistributor.dataOrNull()),
                    onSelectOption = { index ->
                        state.eventSink(
                            NotificationSettingsEvents.SetPushProvider(index)
                        )
                    },
                    onDismissRequest = { state.eventSink(NotificationSettingsEvents.CancelChangePushProvider) },
                )
            }
        }
    }
}

@Composable
private fun MomentNotificationTopBar(
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clickable(onClick = onBackClick),
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
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
            text = stringResource(id = R.string.screen_notification_settings_title),
            style = ElementTheme.typography.fontHeadingSmMedium,
            color = ElementTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MomentNotificationSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = ElementTheme.typography.fontBodySmMedium,
            color = ElementTheme.colors.textSecondary,
        )
        content()
    }
}

@Composable
private fun MomentNotificationCard(
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
private fun MomentNotificationNavigationRow(
    title: String,
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    trailingText: String? = null,
    showDivider: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (description == null) 64.dp else 82.dp)
                .semantics(mergeDescendants = true) {}
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MomentNotificationIconTile(imageVector = imageVector)
            MomentNotificationRowText(
                modifier = Modifier.weight(1f),
                title = title,
                description = description,
            )
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (trailingContent != null) {
                trailingContent()
            }
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = CompoundIcons.ChevronRight(),
                contentDescription = null,
                tint = ElementTheme.colors.iconSecondary,
            )
        }
        if (showDivider) {
            MomentNotificationDivider(start = 60.dp)
        }
    }
}

@Composable
private fun MomentNotificationSwitchRow(
    title: String,
    imageVector: ImageVector,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (description == null) 64.dp else 82.dp)
                .semantics(mergeDescendants = true) {}
                .clickable(onClick = onCheckedChange)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MomentNotificationIconTile(imageVector = imageVector)
            MomentNotificationRowText(
                modifier = Modifier.weight(1f),
                title = title,
                description = description,
            )
            Switch(
                checked = checked,
                onCheckedChange = { onCheckedChange() },
            )
        }
        if (showDivider) {
            MomentNotificationDivider(start = 60.dp)
        }
    }
}

@Composable
private fun MomentNotificationRowText(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = title,
            style = ElementTheme.typography.fontBodyLgMedium,
            color = ElementTheme.colors.textPrimary,
        )
        if (description != null) {
            Text(
                text = description,
                style = ElementTheme.typography.fontBodySmRegular,
                color = ElementTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun MomentNotificationIconTile(
    imageVector: ImageVector,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                color = ElementTheme.colors.bgSubtleSecondary,
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = imageVector,
            contentDescription = null,
            tint = ElementTheme.colors.iconPrimary,
        )
    }
}

@Composable
private fun MomentNotificationDivider(
    start: Dp,
) {
    HorizontalDivider(
        modifier = Modifier.padding(start = start),
        color = ElementTheme.colors.borderDisabled,
    )
}

@Composable
private fun PushProviderTrailing(
    state: NotificationSettingsState,
) {
    when (state.currentPushDistributor) {
        AsyncData.Uninitialized,
        is AsyncData.Loading -> CircularProgressIndicator(
            modifier = Modifier
                .progressSemantics()
                .size(20.dp),
            strokeWidth = 2.dp
        )
        is AsyncData.Failure -> Text(
            text = stringResource(id = CommonStrings.common_error),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        is AsyncData.Success -> Text(
            text = state.currentPushDistributor.dataOrNull()?.name.orEmpty(),
            style = ElementTheme.typography.fontBodyMdRegular,
            color = ElementTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun getTitleForRoomNotificationMode(mode: RoomNotificationMode?) =
    when (mode) {
        RoomNotificationMode.ALL_MESSAGES -> stringResource(id = R.string.screen_notification_settings_edit_mode_all_messages)
        RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY -> stringResource(id = R.string.screen_notification_settings_edit_mode_mentions_and_keywords)
        RoomNotificationMode.MUTE -> stringResource(id = CommonStrings.common_mute)
        null -> ""
    }

@PreviewsDayNight
@Composable
internal fun NotificationSettingsViewPreview(@PreviewParameter(NotificationSettingsStateProvider::class) state: NotificationSettingsState) = ElementPreview {
    NotificationSettingsView(
        state = state,
        onBackClick = {},
        onOpenEditDefault = {},
        onTroubleshootNotificationsClick = {},
    )
}
