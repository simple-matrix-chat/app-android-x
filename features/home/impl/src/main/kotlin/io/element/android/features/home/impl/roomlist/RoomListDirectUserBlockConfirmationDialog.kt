/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.roomlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.element.android.features.home.impl.R
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog

@Composable
fun RoomListDirectUserBlockConfirmationDialog(
    confirmation: RoomListState.DirectUserBlockConfirmation.Shown,
    eventSink: (RoomListEvent) -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(
            id = if (confirmation.blocked) {
                R.string.screen_home_direct_user_block_user
            } else {
                R.string.screen_home_direct_user_unblock_user
            }
        ),
        content = stringResource(
            id = if (confirmation.blocked) {
                R.string.screen_home_direct_user_block_alert_description
            } else {
                R.string.screen_home_direct_user_unblock_alert_description
            }
        ),
        submitText = stringResource(
            id = if (confirmation.blocked) {
                R.string.screen_home_direct_user_block_alert_action
            } else {
                R.string.screen_home_direct_user_unblock_alert_action
            }
        ),
        destructiveSubmit = confirmation.blocked,
        onSubmitClick = {
            eventSink(RoomListEvent.SetDirectUserBlocked(confirmation.userId, confirmation.blocked))
        },
        onDismiss = {
            eventSink(RoomListEvent.HideDirectUserBlockConfirmation)
        },
    )
}
