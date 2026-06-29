/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.filters

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.preferences.test.FakePreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultMomentHomePreferencesStoreTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `setRoomArchived updates archived room ids`() = runTest {
        val store = DefaultMomentHomePreferencesStore(
            matrixClient = FakeMatrixClient(sessionId = A_SESSION_ID),
            preferenceDataStoreFactory = FakePreferenceDataStoreFactory(),
            sessionCoroutineScope = backgroundScope,
        )
        val roomId = RoomId("!room:server.org")

        store.setRoomArchived(roomId, archived = true)
        advanceUntilIdle()
        assertThat(store.archivedRoomIds.value).containsExactly(roomId)

        store.setRoomArchived(roomId, archived = false)
        advanceUntilIdle()
        assertThat(store.archivedRoomIds.value).isEmpty()
    }
}
