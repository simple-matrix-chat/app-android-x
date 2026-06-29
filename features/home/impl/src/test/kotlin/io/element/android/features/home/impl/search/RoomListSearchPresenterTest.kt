/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.search

import com.google.common.truth.Truth.assertThat
import io.element.android.features.home.impl.datasource.aRoomListRoomSummaryFactory
import io.element.android.libraries.dateformatter.test.FakeDateFormatter
import io.element.android.libraries.eventformatter.test.FakeRoomLatestEventFormatter
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.roomlist.RoomListFilter
import io.element.android.libraries.matrix.api.roomlist.RoomListService
import io.element.android.libraries.matrix.api.search.MatrixMessageSearchPage
import io.element.android.libraries.matrix.api.search.MatrixMessageSearchResult
import io.element.android.libraries.matrix.api.user.MatrixMomentUserSearchMatch
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.A_USER_ID_2
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.room.FakeBaseRoom
import io.element.android.libraries.matrix.test.room.aRoomInfo
import io.element.android.libraries.matrix.test.room.aRoomSummary
import io.element.android.libraries.matrix.test.roomlist.FakeDynamicRoomList
import io.element.android.libraries.matrix.test.roomlist.FakeRoomListService
import io.element.android.tests.testutils.consumeItemsUntilPredicate
import io.element.android.tests.testutils.lambda.assert
import io.element.android.tests.testutils.lambda.lambdaRecorder
import io.element.android.tests.testutils.test
import io.element.android.tests.testutils.testCoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RoomListSearchPresenterTest {
    @Test
    fun `present - initial state`() = runTest {
        val presenter = createRoomListSearchPresenter()
        presenter.test {
            awaitItem().let { state ->
                assertThat(state.isSearchActive).isFalse()
                assertThat(state.query.text.toString()).isEmpty()
                assertThat(state.results).isEmpty()
                assertThat(state.userResults).isEmpty()
                assertThat(state.messageResults).isEmpty()
                assertThat(state.isSearchingUsers).isFalse()
                assertThat(state.isSearchingMessages).isFalse()
                assertThat(state.hasMoreMessageResults).isFalse()
                assertThat(state.hasMessageSearchError).isFalse()
                assertThat(state.recentlyViewedRooms).isEmpty()
                assertThat(state.recentSearches).isEmpty()
            }
        }
    }

    @Test
    fun `present - toggle search visibility`() = runTest {
        val presenter = createRoomListSearchPresenter()
        presenter.test {
            awaitItem().let { state ->
                assertThat(state.isSearchActive).isFalse()
                state.eventSink(RoomListSearchEvent.ToggleSearchVisibility)
            }
            awaitItem().let { state ->
                assertThat(state.isSearchActive).isTrue()
                state.eventSink(RoomListSearchEvent.ToggleSearchVisibility)
            }
            awaitItem().let { state ->
                assertThat(state.isSearchActive).isFalse()
            }
        }
    }

    @Test
    fun `present - query search changes`() = runTest {
        val roomList = FakeDynamicRoomList()
        val roomListService = FakeRoomListService(
            createRoomListLambda = { roomList }
        )
        val presenter = createRoomListSearchPresenter(roomListService)
        presenter.test {
            awaitItem().let { state ->
                assertThat(
                    roomList.currentFilter.value
                ).isEqualTo(
                    RoomListFilter.None
                )
                state.query.edit { append("Search") }
            }
            awaitItem().let { state ->
                assertThat(state.query.text).isEqualTo("Search")
                assertThat(
                    roomList.currentFilter.value
                ).isEqualTo(
                    RoomListFilter.NormalizedMatchRoomName("Search")
                )
                state.eventSink(RoomListSearchEvent.ClearQuery)
            }
            awaitItem().let { state ->
                assertThat(state.query.text.toString()).isEmpty()
                assertThat(
                    roomList.currentFilter.value
                ).isEqualTo(
                    RoomListFilter.None
                )
            }
        }
    }

    @Test
    fun `present - room list changes`() = runTest {
        val roomList = FakeDynamicRoomList()
        val roomListService = FakeRoomListService(
            createRoomListLambda = { roomList }
        )
        val presenter = createRoomListSearchPresenter(roomListService)
        presenter.test {
            awaitItem().let { state ->
                assertThat(state.results).isEmpty()
                state.query.edit { append("Search") }
            }
            awaitItem().let { state ->
                assertThat(state.query.text).isEqualTo("Search")
            }
            roomList.summaries.emit(
                listOf(aRoomSummary())
            )
            awaitItem().let { state ->
                assertThat(state.results).hasSize(1)
            }
            roomList.summaries.emit(emptyList())
            awaitItem().let { state ->
                assertThat(state.results).isEmpty()
            }
        }
    }

    @Test
    fun `present - opening search loads recently viewed rooms`() = runTest {
        val roomId = RoomId("!recent:server.org")
        val matrixClient = FakeMatrixClient()
        matrixClient.givenGetRoomResult(
            roomId = roomId,
            result = FakeBaseRoom(
                roomId = roomId,
                initialRoomInfo = aRoomInfo(
                    id = roomId,
                    name = "Recent room",
                    topic = "Recent topic",
                )
            )
        )
        matrixClient.trackRecentlyVisitedRoom(roomId)

        val presenter = createRoomListSearchPresenter(matrixClient = matrixClient)
        presenter.test {
            awaitItem().eventSink(RoomListSearchEvent.ToggleSearchVisibility)
            skipItems(1)
            advanceUntilIdle()
            awaitItem().let { state ->
                assertThat(state.recentlyViewedRooms.map { it.roomId }).containsExactly(roomId)
                assertThat(state.recentlyViewedRooms.single().title).isEqualTo("Recent room")
                assertThat(state.shouldShowRecents).isTrue()
            }
        }
    }

    @Test
    fun `present - TrackRecentSearch stores recent search room id`() = runTest {
        val roomId = RoomId("!searched:server.org")
        val recentSearchesStore = InMemoryRoomListRecentSearchesStore()
        val presenter = createRoomListSearchPresenter(recentSearchesStore = recentSearchesStore)
        presenter.test {
            awaitItem().eventSink(RoomListSearchEvent.TrackRecentSearch(roomId))
            advanceUntilIdle()

            assertThat(recentSearchesStore.roomIds.value).containsExactly(roomId)
        }
    }

    @Test
    fun `present - message search results are loaded for query`() = runTest {
        val roomId = RoomId("!message-room:server.org")
        val eventId = EventId("\$event")
        val matrixClient = FakeMatrixClient(
            searchMessagesResult = { searchTerm, nextBatch, _ ->
                assertThat(searchTerm).isEqualTo("hello")
                assertThat(nextBatch).isNull()
                Result.success(
                    MatrixMessageSearchPage(
                        results = listOf(aMatrixMessageSearchResult(roomId = roomId, eventId = eventId)),
                        nextBatch = null,
                    )
                )
            }
        )
        matrixClient.givenGetRoomResult(
            roomId = roomId,
            result = FakeBaseRoom(
                roomId = roomId,
                initialRoomInfo = aRoomInfo(id = roomId, name = "Messages room")
            )
        )

        val presenter = createRoomListSearchPresenter(matrixClient = matrixClient)
        presenter.test {
            awaitItem().query.edit { append("hello") }
            val states = consumeItemsUntilPredicate { state -> state.messageResults.isNotEmpty() }
            states.last().let { state ->
                assertThat(state.isSearchingMessages).isFalse()
                assertThat(state.messageResults).hasSize(1)
                assertThat(state.messageResults.single().roomTitle).isEqualTo("Messages room")
                assertThat(state.messageResults.single().description).isEqualTo("Alice: hello from search")
            }
        }
    }

    @Test
    fun `present - moment user search results are loaded for phone query`() = runTest {
        val matrixClient = FakeMatrixClient(
            searchMomentUsersResult = { query, limit, _ ->
                assertThat(query).isEqualTo("+7 999 123-45-67")
                assertThat(limit).isEqualTo(10)
                Result.success(
                    listOf(
                        MatrixMomentUserSearchMatch(
                            userId = A_USER_ID_2,
                            displayName = "Bob",
                            avatarUrl = null,
                            phoneNumber = "+79991234567",
                        )
                    )
                )
            }
        )
        matrixClient.givenFindDmResult(Result.success(A_ROOM_ID))

        val presenter = createRoomListSearchPresenter(matrixClient = matrixClient)
        presenter.test {
            awaitItem().query.edit { append("+7 999 123-45-67") }
            val states = consumeItemsUntilPredicate { state -> state.userResults.isNotEmpty() }
            states.last().let { state ->
                assertThat(state.isSearchingUsers).isFalse()
                assertThat(state.userResults).hasSize(1)
                assertThat(state.userResults.single().matrixUser.userId).isEqualTo(A_USER_ID_2)
                assertThat(state.userResults.single().directRoomId).isEqualTo(A_ROOM_ID)
                assertThat(state.userResults.single().title).isEqualTo("Bob")
                assertThat(state.userResults.single().description).isEqualTo("+79991234567")
            }
        }
    }

    @Test
    fun `present - moment user search is skipped for queries with fewer than five digits`() = runTest {
        var calls = 0
        val matrixClient = FakeMatrixClient(
            searchMomentUsersResult = { _, _, _ ->
                calls++
                Result.success(emptyList())
            }
        )

        val presenter = createRoomListSearchPresenter(matrixClient = matrixClient)
        presenter.test {
            awaitItem().query.edit { append("1234") }
            advanceUntilIdle()

            assertThat(calls).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - message search error is exposed`() = runTest {
        val matrixClient = FakeMatrixClient(
            searchMessagesResult = { _, _, _ -> Result.failure(IllegalStateException("boom")) }
        )
        val presenter = createRoomListSearchPresenter(matrixClient = matrixClient)

        presenter.test {
            awaitItem().query.edit { append("hello") }
            val states = consumeItemsUntilPredicate { state -> state.hasMessageSearchError }
            states.last().let { state ->
                assertThat(state.isSearchingMessages).isFalse()
                assertThat(state.messageResults).isEmpty()
                assertThat(state.hasEmptySearchResults).isFalse()
            }
        }
    }

    @Test
    fun `present - message search loads more when near bottom`() = runTest {
        val roomId = RoomId("!message-room:server.org")
        val eventId1 = EventId("\$event1")
        val eventId2 = EventId("\$event2")
        val requestedBatches = mutableListOf<String?>()
        val matrixClient = FakeMatrixClient(
            searchMessagesResult = { _, nextBatch, _ ->
                requestedBatches.add(nextBatch)
                if (nextBatch == null) {
                    Result.success(
                        MatrixMessageSearchPage(
                            results = listOf(aMatrixMessageSearchResult(roomId = roomId, eventId = eventId1)),
                            nextBatch = "next",
                        )
                    )
                } else {
                    Result.success(
                        MatrixMessageSearchPage(
                            results = listOf(aMatrixMessageSearchResult(roomId = roomId, eventId = eventId2)),
                            nextBatch = null,
                        )
                    )
                }
            }
        )
        matrixClient.givenGetRoomResult(
            roomId = roomId,
            result = FakeBaseRoom(
                roomId = roomId,
                initialRoomInfo = aRoomInfo(id = roomId, name = "Messages room")
            )
        )

        val presenter = createRoomListSearchPresenter(matrixClient = matrixClient)
        presenter.test {
            awaitItem().query.edit { append("hello") }
            val firstPageState = consumeItemsUntilPredicate { state -> state.messageResults.size == 1 && state.hasMoreMessageResults }.last()
            firstPageState.eventSink(RoomListSearchEvent.UpdateVisibleRange(0..10))
            advanceUntilIdle()

            val secondPageState = consumeItemsUntilPredicate { state -> state.messageResults.size == 2 }.last()
            assertThat(secondPageState.hasMoreMessageResults).isFalse()
            assertThat(requestedBatches).containsExactly(null, "next").inOrder()
        }
    }

    @Test
    fun `present - UpdateVisibleRange triggers pagination when near end`() = runTest {
        val loadMoreLambda = lambdaRecorder<Unit> { }
        val roomList = FakeDynamicRoomList(loadMoreLambda = loadMoreLambda)
        val roomListService = FakeRoomListService(
            createRoomListLambda = { roomList }
        )
        val presenter = createRoomListSearchPresenter(roomListService)
        presenter.test {
            val initialState = awaitItem()
            initialState.query.edit { append("Search") }
            skipItems(1)
            // Post some rooms to simulate loaded content
            val rooms = (1..10).map { aRoomSummary() }
            roomList.summaries.emit(rooms)
            skipItems(1)

            // UpdateVisibleRange near end should trigger loadMore
            initialState.eventSink(RoomListSearchEvent.UpdateVisibleRange(IntRange(0, 9)))
            // Give time for the coroutine to complete
            testScheduler.advanceUntilIdle()

            assert(loadMoreLambda).isCalledOnce()
        }
    }
}

fun TestScope.createRoomListSearchPresenter(
    roomListService: RoomListService = FakeRoomListService(),
    matrixClient: FakeMatrixClient = FakeMatrixClient(),
    recentSearchesStore: RoomListRecentSearchesStore = InMemoryRoomListRecentSearchesStore(),
): RoomListSearchPresenter {
    return RoomListSearchPresenter(
        dataSourceFactory = object : RoomListSearchDataSource.Factory {
            override fun create(coroutineScope: CoroutineScope): RoomListSearchDataSource {
                return RoomListSearchDataSource(
                    matrixClient = matrixClient,
                    roomListService = roomListService,
                    roomSummaryFactory = aRoomListRoomSummaryFactory(
                        dateFormatter = FakeDateFormatter(),
                        roomLatestEventFormatter = FakeRoomLatestEventFormatter(),
                    ),
                    coroutineDispatchers = testCoroutineDispatchers(),
                    recentSearchesStore = recentSearchesStore,
                    coroutineScope = coroutineScope,
                )
            }
        }
    )
}

private class InMemoryRoomListRecentSearchesStore : RoomListRecentSearchesStore {
    override val roomIds = MutableStateFlow<List<RoomId>>(emptyList())

    override suspend fun track(roomIds: List<RoomId>) {
        val uniqueNewRoomIds = roomIds.distinct()
        this.roomIds.value = uniqueNewRoomIds + this.roomIds.value.filterNot(uniqueNewRoomIds::contains)
    }
}

private fun aMatrixMessageSearchResult(
    roomId: RoomId,
    eventId: EventId,
) = MatrixMessageSearchResult(
    roomId = roomId,
    eventId = eventId,
    senderId = null,
    senderDisplayName = "Alice",
    message = "hello from search",
    originServerTimestamp = null,
)
