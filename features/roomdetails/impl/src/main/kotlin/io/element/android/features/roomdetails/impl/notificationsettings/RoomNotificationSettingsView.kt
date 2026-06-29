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
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.roomdetails.impl.R
import io.element.android.libraries.core.bool.orTrue
import io.element.android.libraries.designsystem.components.ClickableLinkText
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.text.buildAnnotatedStringWithStyledPart
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun RoomNotificationSettingsView(
    state: RoomNotificationSettingsState,
    onShowGlobalNotifications: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.showUserDefinedSettingStyle) {
        UserDefinedRoomNotificationSettingsView(
            state = state,
            modifier = modifier,
            onBackClick = onBackClick,
        )
    } else {
        RoomSpecificNotificationSettingsView(
            state = state,
            modifier = modifier,
            onShowGlobalNotifications = onShowGlobalNotifications,
            onBackClick = onBackClick,
        )
    }
}

@Composable
private fun RoomSpecificNotificationSettingsView(
    state: RoomNotificationSettingsState,
    onShowGlobalNotifications: () -> Unit,
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
            val roomNotificationSettings = state.roomNotificationSettings.dataOrNull()
            MomentRoomNotificationTopBar(
                title = stringResource(R.string.screen_room_details_notification_title),
                onBackClick = onBackClick,
            )
            MomentRoomNotificationSection {
                MomentRoomNotificationSwitchRow(
                    checked = !state.displayIsDefault.orTrue(),
                    onCheckedChange = {
                        state.eventSink(RoomNotificationSettingsEvent.SetNotificationMode(!it))
                    },
                    title = stringResource(id = R.string.screen_room_notification_settings_allow_custom),
                    subtitle = stringResource(id = R.string.screen_room_notification_settings_allow_custom_footnote),
                    icon = CompoundIcons.Settings(),
                    enabled = roomNotificationSettings != null && !state.setNotificationSettingAction.isLoading() && !state.restoreDefaultAction.isLoading(),
                )
            }
            if (state.displayIsDefault.orTrue()) {
                MomentRoomNotificationSection(
                    title = stringResource(id = R.string.screen_room_notification_settings_default_setting_title),
                    footer = {
                        val text = buildAnnotatedStringWithStyledPart(
                            R.string.screen_room_notification_settings_default_setting_footnote,
                            R.string.screen_room_notification_settings_default_setting_footnote_content_link,
                            color = ElementTheme.colors.textSecondary,
                            underline = false,
                            bold = true,
                        )
                        ClickableLinkText(
                            annotatedString = text,
                            onClick = {
                                onShowGlobalNotifications()
                            },
                            style = ElementTheme.typography.fontBodyMdRegular.copy(
                                color = ElementTheme.colors.textSecondary,
                            )
                        )
                    },
                ) {
                    val defaultRoomNotificationMode = state.defaultRoomNotificationMode
                    val isLoading = state.restoreDefaultAction.isLoading() || defaultRoomNotificationMode == null
                    val defaultModeTitle = when (defaultRoomNotificationMode) {
                        RoomNotificationMode.ALL_MESSAGES -> stringResource(id = R.string.screen_room_notification_settings_mode_all_messages)
                        RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY -> {
                            stringResource(id = R.string.screen_room_notification_settings_mode_mentions_and_keywords)
                        }
                        RoomNotificationMode.MUTE -> stringResource(id = CommonStrings.common_mute)
                        null -> stringResource(id = CommonStrings.common_loading)
                    }
                    MomentRoomNotificationValueRow(
                        title = defaultModeTitle,
                        isLoading = isLoading,
                    )
                }
            } else {
                MomentRoomNotificationSection(
                    title = stringResource(id = R.string.screen_room_notification_settings_custom_settings_title),
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

            AsyncActionView(
                async = state.setNotificationSettingAction,
                onSuccess = {},
                errorMessage = { stringResource(R.string.screen_notification_settings_edit_failed_updating_default_mode) },
                onErrorDismiss = { state.eventSink(RoomNotificationSettingsEvent.ClearSetNotificationError) },
            )

            AsyncActionView(
                async = state.restoreDefaultAction,
                onSuccess = {},
                errorMessage = { stringResource(R.string.screen_notification_settings_edit_failed_updating_default_mode) },
                onErrorDismiss = { state.eventSink(RoomNotificationSettingsEvent.ClearRestoreDefaultError) },
            )
        }
    }
}

@PreviewsDayNight
@Composable
internal fun RoomNotificationSettingsViewPreview(
    @PreviewParameter(RoomNotificationSettingsStateProvider::class) state: RoomNotificationSettingsState
) = ElementPreview {
    RoomNotificationSettingsView(
        state = state,
        onShowGlobalNotifications = {},
        onBackClick = {},
    )
}
