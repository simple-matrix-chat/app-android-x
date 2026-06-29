/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.filters.selection

import dev.zacsweers.metro.ContributesBinding
import io.element.android.features.home.impl.filters.RoomListFilter
import io.element.android.libraries.di.SessionScope
import kotlinx.coroutines.flow.MutableStateFlow

@ContributesBinding(SessionScope::class)
class DefaultFilterSelectionStrategy : FilterSelectionStrategy {
    private var selectedFilter: RoomListFilter? = null
    private val availableFilters
        get() = RoomListFilter.entries

    override val filterSelectionStates = MutableStateFlow(buildFilters())

    override fun select(filter: RoomListFilter) {
        selectedFilter = filter
        filterSelectionStates.value = buildFilters()
    }

    override fun deselect(filter: RoomListFilter) {
        if (selectedFilter == filter) {
            selectedFilter = null
            filterSelectionStates.value = buildFilters()
        }
    }

    override fun toggle(filter: RoomListFilter) {
        select(filter)
    }

    override fun isSelected(filter: RoomListFilter): Boolean {
        return selectedFilter == filter
    }

    override fun clear() {
        selectedFilter = null
        filterSelectionStates.value = buildFilters()
    }

    private fun buildFilters(): Set<FilterSelectionState> {
        return availableFilters.map {
            FilterSelectionState(
                filter = it,
                isSelected = it == selectedFilter
            )
        }.toSet()
    }
}
