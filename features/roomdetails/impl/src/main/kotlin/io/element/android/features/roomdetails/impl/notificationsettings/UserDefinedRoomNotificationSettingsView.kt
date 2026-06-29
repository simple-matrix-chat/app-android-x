/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roomdetails.impl.notificationsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.roomdetails.impl.R
import io.element.android.libraries.core.bool.orTrue
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Scaffold

@Composable
fun UserDefinedRoomNotificationSettingsView(
    state: RoomNotificationSettingsState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            MomentRoomNotificationTopBar(
                title = state.roomName,
                onBackClick = onBackClick,
            )
            val roomNotificationSettings = state.roomNotificationSettings.dataOrNull()
            if (roomNotificationSettings != null && state.displayNotificationMode != null) {
                MomentRoomNotificationSection(
                    title = stringResource(id = R.string.screen_room_notification_settings_room_custom_settings_title),
                ) {
                    RoomNotificationSettingsOptions(
                        selected = state.displayNotificationMode,
                        pending = state.pendingRoomNotificationMode,
                        enabled = !state.displayIsDefault.orTrue() && !state.setNotificationSettingAction.isLoading(),
                        displayMentionsOnlyDisclaimer = state.displayMentionsOnlyDisclaimer,
                        onSelectOption = {
                            state.eventSink(RoomNotificationSettingsEvent.ChangeRoomNotificationMode(it.mode))
                        },
                    )
                }
            }

            MomentRoomNotificationCard {
                MomentRoomNotificationDestructiveRow(
                    title = stringResource(R.string.screen_room_notification_settings_edit_remove_setting),
                    enabled = !state.restoreDefaultAction.isLoading(),
                    loading = state.restoreDefaultAction.isLoading(),
                    onClick = {
                        state.eventSink(RoomNotificationSettingsEvent.DeleteCustomNotification)
                    },
                )
            }

            AsyncActionView(
                async = state.setNotificationSettingAction,
                onSuccess = {},
                errorMessage = { stringResource(R.string.screen_notification_settings_edit_failed_updating_default_mode) },
                onErrorDismiss = { state.eventSink(RoomNotificationSettingsEvent.ClearSetNotificationError) },
            )

            AsyncActionView(
                async = state.restoreDefaultAction,
                onSuccess = { onBackClick() },
                errorMessage = { stringResource(R.string.screen_notification_settings_edit_failed_updating_default_mode) },
                onErrorDismiss = { state.eventSink(RoomNotificationSettingsEvent.ClearRestoreDefaultError) },
            )
        }
    }
}

@PreviewsDayNight
@Composable
internal fun UserDefinedRoomNotificationSettingsViewPreview(
    @PreviewParameter(UserDefinedRoomNotificationSettingsStateProvider::class) state: RoomNotificationSettingsState
) = ElementPreview {
    UserDefinedRoomNotificationSettingsView(
        state = state,
        onBackClick = {},
    )
}
