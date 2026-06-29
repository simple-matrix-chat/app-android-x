/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.search

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.preferences.test.FakePreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultRoomListRecentSearchesStoreTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `track stores unique recent room ids with newest first and max size`() = runTest {
        val store = DefaultRoomListRecentSearchesStore(
            matrixClient = FakeMatrixClient(sessionId = A_SESSION_ID),
            preferenceDataStoreFactory = FakePreferenceDataStoreFactory(),
            sessionCoroutineScope = backgroundScope,
        )
        val rooms = (1..6).map { index -> RoomId("!room$index:server.org") }

        store.track(rooms)
        advanceUntilIdle()
        assertThat(store.roomIds.value).containsExactlyElementsIn(rooms.take(5)).inOrder()

        store.track(listOf(rooms[2], rooms[0]))
        advanceUntilIdle()
        assertThat(store.roomIds.value).containsExactly(rooms[2], rooms[0], rooms[1], rooms[3], rooms[4]).inOrder()
    }
}
