/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.filters

import androidx.annotation.StringRes
import io.element.android.features.home.impl.R

/**
 * Holds the resources for the empty state when filters are applied to the room list.
 * @param title the title of the empty state
 * @param subtitle the subtitle of the empty state
 */
data class RoomListFiltersEmptyStateResources(
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
) {
    companion object {
        /**
         * Create a [RoomListFiltersEmptyStateResources] from a list of selected filters.
         */
        fun fromSelectedFilters(selectedFilters: List<RoomListFilter>, isSpaceFilterSelected: Boolean): RoomListFiltersEmptyStateResources? {
            return when {
                isSpaceFilterSelected -> RoomListFiltersEmptyStateResources(
                    title = R.string.screen_roomlist_filter_mixed_empty_state_title,
                    subtitle = R.string.screen_roomlist_filter_mixed_empty_state_subtitle
                )
                selectedFilters.isEmpty() -> RoomListFiltersEmptyStateResources(
                    title = R.string.screen_moment_home_empty_all_title,
                    subtitle = R.string.screen_moment_home_empty_all_message
                )
                selectedFilters.size == 1 -> {
                    when (selectedFilters.first()) {
                        RoomListFilter.Direct -> RoomListFiltersEmptyStateResources(
                            title = R.string.screen_moment_home_empty_direct_title,
                            subtitle = R.string.screen_moment_home_empty_direct_message
                        )
                        RoomListFilter.Groups -> RoomListFiltersEmptyStateResources(
                            title = R.string.screen_moment_home_empty_all_title,
                            subtitle = R.string.screen_moment_home_empty_all_message
                        )
                        RoomListFilter.Channels -> RoomListFiltersEmptyStateResources(
                            title = R.string.screen_moment_home_empty_channels_title,
                            subtitle = R.string.screen_moment_home_empty_channels_message
                        )
                        RoomListFilter.Archived -> RoomListFiltersEmptyStateResources(
                            title = R.string.screen_moment_home_empty_archived_title,
                            subtitle = R.string.screen_moment_home_empty_archived_message
                        )
                    }
                }
                else -> RoomListFiltersEmptyStateResources(
                    title = R.string.screen_roomlist_filter_mixed_empty_state_title,
                    subtitle = R.string.screen_roomlist_filter_mixed_empty_state_subtitle
                )
            }
        }
    }
}
