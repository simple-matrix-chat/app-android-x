/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.filters

import io.element.android.features.home.impl.R
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.model.RoomSummaryDisplayType

/**
 * Enum class representing the different filters that can be applied to the room list.
 * Order is important, it'll be used as initial order in the UI.
 */
enum class RoomListFilter(val stringResource: Int) {
    Direct(R.string.screen_roomlist_filter_direct),
    Groups(R.string.screen_roomlist_filter_groups),
    Channels(R.string.screen_roomlist_filter_channels),
    Archived(R.string.screen_roomlist_filter_archived),
}

fun RoomListRoomSummary.matches(filter: RoomListFilter?): Boolean {
    if (filter == null) {
        return displayType != RoomSummaryDisplayType.ROOM || !isArchived
    }
    if (displayType != RoomSummaryDisplayType.ROOM || isSpace) {
        return false
    }
    return when (filter) {
        RoomListFilter.Direct -> !isArchived && momentHomeRoomType == MomentHomeRoomType.Direct
        RoomListFilter.Groups -> !isArchived && momentHomeRoomType == MomentHomeRoomType.Group
        RoomListFilter.Channels -> !isArchived && momentHomeRoomType == MomentHomeRoomType.Channel
        RoomListFilter.Archived -> isArchived
    }
}
