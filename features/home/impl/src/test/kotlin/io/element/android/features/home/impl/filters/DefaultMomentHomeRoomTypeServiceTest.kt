/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.filters

import com.google.common.truth.Truth.assertThat
import io.element.android.features.home.impl.model.aRoomListRoomSummary
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.sessionstorage.test.InMemorySessionStore
import io.element.android.libraries.sessionstorage.test.aSessionData
import io.element.android.tests.testutils.lambda.lambdaError
import io.element.android.tests.testutils.testCoroutineDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultMomentHomeRoomTypeServiceTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `resolveRoomTypes fetches room kind state and maps channel rooms`() = runTest {
        val requestedUrls = mutableListOf<String>()
        val service = createService(
            getUrlResult = { url ->
                requestedUrls += url
                Result.success("""{"kind":"channel"}""".encodeToByteArray())
            }
        )

        service.resolveRoomTypes(
            listOf(aRoomListRoomSummary(id = "!room/id:server.org"))
        )
        advanceUntilIdle()

        assertThat(requestedUrls).containsExactly(
            "https://matrix.example.org/api/client/v3/rooms/%21room%2Fid%3Aserver.org/state/io.moment.room_kind/"
        )
        assertThat(service.roomTypes.value).containsEntry(RoomId("!room/id:server.org"), MomentHomeRoomType.Channel)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `resolveRoomTypes maps non-channel state to group rooms`() = runTest {
        val service = createService(
            getUrlResult = {
                Result.success("""{"kind":"group"}""".encodeToByteArray())
            }
        )

        service.resolveRoomTypes(
            listOf(aRoomListRoomSummary(id = "!room:server.org"))
        )
        advanceUntilIdle()

        assertThat(service.roomTypes.value).containsEntry(RoomId("!room:server.org"), MomentHomeRoomType.Group)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `resolveRoomTypes ignores direct and space rooms`() = runTest {
        val service = createService(
            getUrlResult = { lambdaError() }
        )

        service.resolveRoomTypes(
            listOf(
                aRoomListRoomSummary(id = "!direct:server.org", isDirect = true),
                aRoomListRoomSummary(id = "!space:server.org", isSpace = true),
            )
        )
        advanceUntilIdle()

        assertThat(service.roomTypes.value).isEmpty()
    }

    private fun TestScope.createService(
        getUrlResult: (String) -> Result<ByteArray>,
    ): DefaultMomentHomeRoomTypeService {
        val matrixClient = FakeMatrixClient(
            sessionId = A_SESSION_ID,
            sessionCoroutineScope = this,
            getUrlLambda = getUrlResult,
        )
        val sessionStore = InMemorySessionStore(
            initialList = listOf(
                aSessionData(sessionId = A_SESSION_ID.value)
                    .copy(homeserverUrl = "https://matrix.example.org/")
            )
        )
        return DefaultMomentHomeRoomTypeService(
            matrixClient = matrixClient,
            sessionStore = sessionStore,
            dispatchers = testCoroutineDispatchers(),
            sessionCoroutineScope = this,
        )
    }
}
