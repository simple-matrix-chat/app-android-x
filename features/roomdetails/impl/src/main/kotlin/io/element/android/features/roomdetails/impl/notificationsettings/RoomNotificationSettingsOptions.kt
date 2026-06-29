/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roomdetails.impl.notificationsettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.element.android.libraries.matrix.api.room.RoomNotificationMode

@Composable
fun RoomNotificationSettingsOptions(
    selected: RoomNotificationMode?,
    pending: RoomNotificationMode?,
    enabled: Boolean,
    onSelectOption: (RoomNotificationSettingsItem) -> Unit,
    displayMentionsOnlyDisclaimer: Boolean,
    modifier: Modifier = Modifier,
) {
    val items = roomNotificationSettingsItems()
    Column(modifier = modifier.selectableGroup()) {
        items.forEachIndexed { index, item ->
            MomentRoomNotificationModeRow(
                item = item,
                selected = selected == item.mode && pending == null,
                loading = pending == item.mode,
                enabled = enabled && pending == null,
                onSelect = onSelectOption,
                displayMentionsOnlyDisclaimer = displayMentionsOnlyDisclaimer,
            )
            if (index != items.lastIndex) {
                MomentRoomNotificationDivider()
            }
        }
    }
}
