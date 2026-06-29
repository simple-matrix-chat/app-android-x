/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.filters

import com.google.common.truth.Truth.assertThat
import io.element.android.features.home.impl.R
import org.junit.Test

class RoomListFiltersEmptyStateResourcesTest {
    @Test
    fun `fromSelectedFilters should return all empty resources when selectedFilters is empty`() {
        val selectedFilters = emptyList<RoomListFilter>()
        val result = RoomListFiltersEmptyStateResources.fromSelectedFilters(selectedFilters, isSpaceFilterSelected = false)
        assertThat(result).isNotNull()
        assertThat(result?.title).isEqualTo(R.string.screen_moment_home_empty_all_title)
        assertThat(result?.subtitle).isEqualTo(R.string.screen_moment_home_empty_all_message)
    }

    @Test
    fun `fromSelectedFilters should return exact RoomListFiltersEmptyStateResources when selectedFilters has only direct filter`() {
        val selectedFilters = listOf(RoomListFilter.Direct)
        val result = RoomListFiltersEmptyStateResources.fromSelectedFilters(selectedFilters, isSpaceFilterSelected = false)
        assertThat(result).isNotNull()
        assertThat(result?.title).isEqualTo(R.string.screen_moment_home_empty_direct_title)
        assertThat(result?.subtitle).isEqualTo(R.string.screen_moment_home_empty_direct_message)
    }

    @Test
    fun `fromSelectedFilters should return exact RoomListFiltersEmptyStateResources when selectedFilters has only groups filter`() {
        val selectedFilters = listOf(RoomListFilter.Groups)
        val result = RoomListFiltersEmptyStateResources.fromSelectedFilters(selectedFilters, isSpaceFilterSelected = false)
        assertThat(result).isNotNull()
        assertThat(result?.title).isEqualTo(R.string.screen_moment_home_empty_all_title)
        assertThat(result?.subtitle).isEqualTo(R.string.screen_moment_home_empty_all_message)
    }

    @Test
    fun `fromSelectedFilters should return exact RoomListFiltersEmptyStateResources when selectedFilters has only channels filter`() {
        val selectedFilters = listOf(RoomListFilter.Channels)
        val result = RoomListFiltersEmptyStateResources.fromSelectedFilters(selectedFilters, isSpaceFilterSelected = false)
        assertThat(result).isNotNull()
        assertThat(result?.title).isEqualTo(R.string.screen_moment_home_empty_channels_title)
        assertThat(result?.subtitle).isEqualTo(R.string.screen_moment_home_empty_channels_message)
    }

    @Test
    fun `fromSelectedFilters should return exact RoomListFiltersEmptyStateResources when selectedFilters has only archived filter`() {
        val selectedFilters = listOf(RoomListFilter.Archived)
        val result = RoomListFiltersEmptyStateResources.fromSelectedFilters(selectedFilters, isSpaceFilterSelected = false)
        assertThat(result).isNotNull()
        assertThat(result?.title).isEqualTo(R.string.screen_moment_home_empty_archived_title)
        assertThat(result?.subtitle).isEqualTo(R.string.screen_moment_home_empty_archived_message)
    }

    @Test
    fun `fromSelectedFilters should return exact RoomListFiltersEmptyStateResources when selectedFilters has multiple filters`() {
        val selectedFilters = listOf(RoomListFilter.Direct, RoomListFilter.Groups)
        val result = RoomListFiltersEmptyStateResources.fromSelectedFilters(selectedFilters, isSpaceFilterSelected = false)
        assertThat(result).isNotNull()
        assertThat(result?.title).isEqualTo(R.string.screen_roomlist_filter_mixed_empty_state_title)
        assertThat(result?.subtitle).isEqualTo(R.string.screen_roomlist_filter_mixed_empty_state_subtitle)
    }

    @Test
    fun `fromSelectedFilters should return exact RoomListFiltersEmptyStateResources when isSpaceFilterSelected is true`() {
        val result = RoomListFiltersEmptyStateResources.fromSelectedFilters(emptyList(), isSpaceFilterSelected = true)
        assertThat(result).isNotNull()
        assertThat(result?.title).isEqualTo(R.string.screen_roomlist_filter_mixed_empty_state_title)
        assertThat(result?.subtitle).isEqualTo(R.string.screen_roomlist_filter_mixed_empty_state_subtitle)
    }
}
