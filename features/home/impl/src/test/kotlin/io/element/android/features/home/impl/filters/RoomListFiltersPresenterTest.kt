/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.filters

import com.google.common.truth.Truth.assertThat
import io.element.android.features.home.impl.filters.selection.DefaultFilterSelectionStrategy
import io.element.android.features.home.impl.filters.selection.FilterSelectionState
import io.element.android.tests.testutils.awaitLastSequentialItem
import io.element.android.tests.testutils.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RoomListFiltersPresenterTest {
    @Test
    fun `present - initial state`() = runTest {
        val presenter = createRoomListFiltersPresenter()
        presenter.test {
            awaitItem().let { state ->
                assertThat(state.hasAnyFilterSelected).isFalse()
                assertThat(state.filterSelectionStates).containsExactly(
                    filterSelectionState(RoomListFilter.Direct, false),
                    filterSelectionState(RoomListFilter.Groups, false),
                    filterSelectionState(RoomListFilter.Channels, false),
                    filterSelectionState(RoomListFilter.Archived, false),
                )
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `present - select groups filter`() = runTest {
        val presenter = createRoomListFiltersPresenter()
        presenter.test {
            awaitItem().eventSink.invoke(RoomListFiltersEvent.ToggleFilter(RoomListFilter.Groups))
            awaitLastSequentialItem().let { state ->
                assertThat(state.hasAnyFilterSelected).isTrue()
                assertThat(state.filterSelectionStates).containsExactly(
                    filterSelectionState(RoomListFilter.Direct, false),
                    filterSelectionState(RoomListFilter.Groups, true),
                    filterSelectionState(RoomListFilter.Channels, false),
                    filterSelectionState(RoomListFilter.Archived, false),
                ).inOrder()

                assertThat(state.selectedFilters()).containsExactly(
                    RoomListFilter.Groups,
                )
                state.eventSink.invoke(RoomListFiltersEvent.ToggleFilter(RoomListFilter.Channels))
            }
            advanceUntilIdle()
            awaitLastSequentialItem().let { state ->
                assertThat(state.hasAnyFilterSelected).isTrue()
                assertThat(state.filterSelectionStates).containsExactly(
                    filterSelectionState(RoomListFilter.Direct, false),
                    filterSelectionState(RoomListFilter.Groups, false),
                    filterSelectionState(RoomListFilter.Channels, true),
                    filterSelectionState(RoomListFilter.Archived, false),
                ).inOrder()
                assertThat(state.selectedFilters()).containsExactly(RoomListFilter.Channels)
            }
        }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `present - clear filters event`() = runTest {
        val presenter = createRoomListFiltersPresenter()
        presenter.test {
            awaitItem().eventSink.invoke(RoomListFiltersEvent.ToggleFilter(RoomListFilter.Groups))
            awaitLastSequentialItem().let { state ->
                assertThat(state.hasAnyFilterSelected).isTrue()
                state.eventSink.invoke(RoomListFiltersEvent.ClearSelectedFilters)
            }
            advanceUntilIdle()
            awaitLastSequentialItem().let { state ->
                assertThat(state.hasAnyFilterSelected).isFalse()
            }
        }
    }
}

private fun filterSelectionState(filter: RoomListFilter, selected: Boolean) = FilterSelectionState(
    filter = filter,
    isSelected = selected,
)

private fun TestScope.createRoomListFiltersPresenter(): RoomListFiltersPresenter {
    return RoomListFiltersPresenter(
        filterSelectionStrategy = DefaultFilterSelectionStrategy(),
    )
}
