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
        val requestedStateEvents = mutableListOf<Pair<RoomId, String>>()
        val service = createService(
            getRoomStateEventContentResult = { roomId, eventType ->
                requestedStateEvents += roomId to eventType
                Result.success("""{"kind":"channel"}""")
            }
        )

        service.resolveRoomTypes(
            listOf(aRoomListRoomSummary(id = "!room/id:server.org"))
        )
        advanceUntilIdle()

        assertThat(requestedStateEvents).containsExactly(RoomId("!room/id:server.org") to "io.moment.room_kind")
        assertThat(service.roomTypes.value).containsEntry(RoomId("!room/id:server.org"), MomentHomeRoomType.Channel)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `resolveRoomTypes maps non-channel state to group rooms`() = runTest {
        val service = createService(
            getRoomStateEventContentResult = { _, _ ->
                Result.success("""{"kind":"group"}""")
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
    fun `resolveRoomTypes retries room kind state fetches`() = runTest {
        var requestCount = 0
        val service = createService(
            getRoomStateEventContentResult = { _, _ ->
                requestCount++
                if (requestCount == 1) {
                    Result.failure(IllegalStateException("Room kind state is not synced yet"))
                } else {
                    Result.success("""{"kind":"channel"}""")
                }
            }
        )

        service.resolveRoomTypes(
            listOf(aRoomListRoomSummary(id = "!room:server.org"))
        )
        advanceUntilIdle()

        assertThat(requestCount).isEqualTo(2)
        assertThat(service.roomTypes.value).containsEntry(RoomId("!room:server.org"), MomentHomeRoomType.Channel)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `resolveRoomTypes does not cache failed room kind fetches as unknown`() = runTest {
        var shouldFail = true
        var requestCount = 0
        val service = createService(
            getRoomStateEventContentResult = { _, _ ->
                requestCount++
                if (shouldFail) {
                    Result.failure(IllegalStateException("Room kind state is not synced yet"))
                } else {
                    Result.success("""{"kind":"channel"}""")
                }
            }
        )

        service.resolveRoomTypes(
            listOf(aRoomListRoomSummary(id = "!room:server.org"))
        )
        advanceUntilIdle()

        assertThat(requestCount).isAtLeast(2)
        assertThat(service.roomTypes.value).isEmpty()

        shouldFail = false
        service.resolveRoomTypes(
            listOf(aRoomListRoomSummary(id = "!room:server.org"))
        )
        advanceUntilIdle()

        assertThat(service.roomTypes.value).containsEntry(RoomId("!room:server.org"), MomentHomeRoomType.Channel)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `resolveRoomTypes ignores direct and space rooms`() = runTest {
        val service = createService(
            getRoomStateEventContentResult = { _, _ -> lambdaError() }
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
        getRoomStateEventContentResult: (RoomId, String) -> Result<String>,
    ): DefaultMomentHomeRoomTypeService {
        val matrixClient = FakeMatrixClient(
            sessionId = A_SESSION_ID,
            sessionCoroutineScope = this,
            getRoomStateEventContentLambda = getRoomStateEventContentResult,
        )
        return DefaultMomentHomeRoomTypeService(
            matrixClient = matrixClient,
            dispatchers = testCoroutineDispatchers(),
            sessionCoroutineScope = this,
        )
    }
}
