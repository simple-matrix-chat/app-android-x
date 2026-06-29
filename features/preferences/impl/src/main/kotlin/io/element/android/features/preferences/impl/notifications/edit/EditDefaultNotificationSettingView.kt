/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.notifications.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.preferences.impl.R
import io.element.android.libraries.designsystem.components.async.AsyncActionView
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.ui.strings.CommonStrings

/**
 * A view that allows a user to edit the default notification setting for rooms. This can be set separately
 * for one-to-one and group rooms, indicated by [EditDefaultNotificationSettingState.isOneToOne].
 */
@Composable
fun EditDefaultNotificationSettingView(
    state: EditDefaultNotificationSettingState,
    openRoomNotificationSettings: (roomId: RoomId) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = if (state.isOneToOne) {
        R.string.screen_notification_settings_direct_chats
    } else {
        R.string.screen_notification_settings_group_chats
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
            MomentNotificationEditTopBar(
                title = stringResource(id = title),
                onBackClick = onBackClick,
            )
            // Only ALL_MESSAGES and MENTIONS_AND_KEYWORDS_ONLY are valid global defaults.
            val validModes = listOf(RoomNotificationMode.ALL_MESSAGES, RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY)
            val rowsEnabled = state.mode != null && !state.changeNotificationSettingAction.isLoading()
            val categoryTitle = if (state.isOneToOne) {
                R.string.screen_notification_settings_edit_screen_direct_section_header
            } else {
                R.string.screen_notification_settings_edit_screen_group_section_header
            }
            MomentNotificationEditSection(
                title = stringResource(id = categoryTitle),
            ) {
                Column(modifier = Modifier.selectableGroup()) {
                    validModes.forEachIndexed { index, item ->
                        MomentNotificationDefaultModeRow(
                            title = getTitleForDefaultNotificationMode(item),
                            description = getDefaultNotificationModeDescription(
                                mode = item,
                                displayMentionsOnlyDisclaimer = state.displayMentionsOnlyDisclaimer
                            ),
                            selected = state.pendingMode == null && state.mode == item,
                            enabled = rowsEnabled,
                            loading = state.pendingMode == item,
                            onSelect = { state.eventSink(EditDefaultNotificationSettingStateEvents.SetNotificationMode(item)) },
                        )
                        if (index != validModes.lastIndex) {
                            MomentNotificationEditDivider()
                        }
                    }
                }
            }

            if (state.roomsWithUserDefinedMode.isNotEmpty()) {
                MomentNotificationEditSection(
                    title = stringResource(id = R.string.screen_notification_settings_edit_custom_settings_section_title),
                ) {
                    state.roomsWithUserDefinedMode.forEachIndexed { index, summary ->
                        MomentNotificationCustomRoomRow(
                            summary = summary,
                            title = summary.name ?: stringResource(id = CommonStrings.common_no_room_name),
                            modeLabel = getTitleForRoomNotificationMode(summary.notificationMode),
                            onClick = {
                                openRoomNotificationSettings(summary.roomId)
                            }
                        )
                        if (index != state.roomsWithUserDefinedMode.lastIndex) {
                            MomentNotificationEditDivider(start = 84.dp)
                        }
                    }
                }
            }

            AsyncActionView(
                async = state.changeNotificationSettingAction,
                errorMessage = { stringResource(R.string.screen_notification_settings_edit_failed_updating_default_mode) },
                onErrorDismiss = { state.eventSink(EditDefaultNotificationSettingStateEvents.ClearError) },
                onSuccess = {},
            )
        }
    }
}

@Composable
private fun getTitleForDefaultNotificationMode(mode: RoomNotificationMode) =
    when (mode) {
        RoomNotificationMode.ALL_MESSAGES -> stringResource(id = R.string.screen_notification_settings_edit_mode_all_messages)
        RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY -> stringResource(id = R.string.screen_notification_settings_edit_mode_mentions_and_keywords)
        RoomNotificationMode.MUTE -> stringResource(id = CommonStrings.common_mute)
    }

@Composable
private fun getDefaultNotificationModeDescription(
    mode: RoomNotificationMode,
    displayMentionsOnlyDisclaimer: Boolean,
) = when {
    mode == RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY && displayMentionsOnlyDisclaimer -> {
        stringResource(id = R.string.screen_notification_settings_mentions_only_disclaimer)
    }
    else -> null
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
internal fun EditDefaultNotificationSettingViewPreview(
    @PreviewParameter(EditDefaultNotificationSettingStateProvider::class) state: EditDefaultNotificationSettingState
) = ElementPreview {
    EditDefaultNotificationSettingView(
        state = state,
        openRoomNotificationSettings = {},
        onBackClick = {},
    )
}
