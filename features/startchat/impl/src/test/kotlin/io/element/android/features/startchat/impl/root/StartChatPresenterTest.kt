/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.startchat.impl.root

import androidx.compose.runtime.MutableState
import com.google.common.truth.Truth.assertThat
import io.element.android.features.invitepeople.test.FakeStartDMAction
import io.element.android.features.startchat.api.ConfirmingStartDmWithMatrixUser
import io.element.android.features.startchat.api.StartDMAction
import io.element.android.features.startchat.impl.userlist.FakeUserListPresenter
import io.element.android.features.startchat.impl.userlist.FakeUserListPresenterFactory
import io.element.android.features.startchat.impl.userlist.UserListDataStore
import io.element.android.features.startchat.impl.userlist.aUserListState
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.theme.components.SearchBarResultState
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.createroom.MomentRoomKind
import io.element.android.libraries.matrix.api.roomdirectory.RoomVisibility
import io.element.android.libraries.matrix.api.user.MatrixMomentUserSearchMatch
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.test.AN_EXCEPTION
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.A_USER_ID
import io.element.android.libraries.matrix.test.A_USER_ID_2
import io.element.android.libraries.matrix.test.A_USER_ID_3
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.core.aBuildMeta
import io.element.android.libraries.usersearch.api.UserSearchResult
import io.element.android.libraries.usersearch.test.FakeUserRepository
import io.element.android.services.analytics.api.AnalyticsService
import io.element.android.services.analytics.test.FakeAnalyticsService
import io.element.android.tests.testutils.WarmUpRule
import io.element.android.tests.testutils.consumeItemsUntilPredicate
import io.element.android.tests.testutils.lambda.any
import io.element.android.tests.testutils.lambda.lambdaRecorder
import io.element.android.tests.testutils.lambda.value
import io.element.android.tests.testutils.test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class StartChatPresenterTest {
    @get:Rule
    val warmUpRule = WarmUpRule()

    @Test
    fun `present - start DM action failure scenario`() = runTest {
        val startDMFailureResult = AsyncAction.Failure(AN_EXCEPTION)
        val executeResult = lambdaRecorder<MatrixUser, Boolean, MutableState<AsyncAction<RoomId>>, Unit> { _, _, actionState ->
            actionState.value = startDMFailureResult
        }
        val startDMAction = FakeStartDMAction(executeResult = executeResult)
        val presenter = createStartChatPresenter(startDMAction)
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.startDmAction).isInstanceOf(AsyncAction.Uninitialized::class.java)
            assertThat(initialState.applicationName).isEqualTo(aBuildMeta().applicationName)
            assertThat(initialState.userListState.selectedUsers).isEmpty()
            assertThat(initialState.userListState.isSearchActive).isFalse()
            assertThat(initialState.userListState.isMultiSelectionEnabled).isFalse()
            val matrixUser = MatrixUser(UserId("@name:domain"))
            initialState.eventSink(StartChatEvents.StartDM(matrixUser))
            awaitItem().also { state ->
                assertThat(state.startDmAction).isEqualTo(startDMFailureResult)
                executeResult.assertions().isCalledOnce().with(
                    value(matrixUser),
                    value(false),
                    any(),
                )
                state.eventSink(StartChatEvents.CancelStartDM)
            }
            awaitItem().also { state ->
                assertThat(state.startDmAction.isUninitialized()).isTrue()
            }
        }
    }

    @Test
    fun `present - start DM action success scenario`() = runTest {
        val startDMSuccessResult = AsyncAction.Success(A_ROOM_ID)
        val executeResult = lambdaRecorder<MatrixUser, Boolean, MutableState<AsyncAction<RoomId>>, Unit> { _, _, actionState ->
            actionState.value = startDMSuccessResult
        }
        val startDMAction = FakeStartDMAction(executeResult = executeResult)
        val presenter = createStartChatPresenter(startDMAction)
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.startDmAction).isInstanceOf(AsyncAction.Uninitialized::class.java)
            assertThat(initialState.applicationName).isEqualTo(aBuildMeta().applicationName)
            assertThat(initialState.userListState.selectedUsers).isEmpty()
            assertThat(initialState.userListState.isSearchActive).isFalse()
            assertThat(initialState.userListState.isMultiSelectionEnabled).isFalse()
            val matrixUser = MatrixUser(UserId("@name:domain"))
            initialState.eventSink(StartChatEvents.StartDM(matrixUser))
            awaitItem().also { state ->
                assertThat(state.startDmAction).isEqualTo(startDMSuccessResult)
                executeResult.assertions().isCalledOnce().with(
                    value(matrixUser),
                    value(false),
                    any(),
                )
            }
        }
    }

    @Test
    fun `present - create Moment group success scenario`() = runTest {
        val matrixClient = FakeMatrixClient()
        val presenter = createStartChatPresenter(matrixClient = matrixClient)
        presenter.test {
            val initialState = awaitItem()

            initialState.eventSink(
                StartChatEvents.CreateMomentRoom(
                    name = "  Project team  ",
                    momentRoomKind = MomentRoomKind.Group,
                    isPublic = false,
                )
            )

            assertThat(awaitItem().createMomentRoomAction).isInstanceOf(AsyncAction.Loading::class.java)
            assertThat(awaitItem().createMomentRoomAction).isEqualTo(AsyncAction.Success(A_ROOM_ID))

            val createRoomParameters = matrixClient.latestCreateRoomParameters
            assertThat(createRoomParameters?.name).isEqualTo("Project team")
            assertThat(createRoomParameters?.momentRoomKind).isEqualTo(MomentRoomKind.Group)
            assertThat(createRoomParameters?.visibility).isEqualTo(RoomVisibility.Private)
            assertThat(createRoomParameters?.isEncrypted).isFalse()
        }
    }

    @Test
    fun `present - create Moment channel success scenario`() = runTest {
        val matrixClient = FakeMatrixClient()
        val presenter = createStartChatPresenter(matrixClient = matrixClient)
        presenter.test {
            val initialState = awaitItem()

            initialState.eventSink(
                StartChatEvents.CreateMomentRoom(
                    name = "Announcements",
                    momentRoomKind = MomentRoomKind.Channel,
                    isPublic = true,
                )
            )

            assertThat(awaitItem().createMomentRoomAction).isInstanceOf(AsyncAction.Loading::class.java)
            assertThat(awaitItem().createMomentRoomAction).isEqualTo(AsyncAction.Success(A_ROOM_ID))

            val createRoomParameters = matrixClient.latestCreateRoomParameters
            assertThat(createRoomParameters?.name).isEqualTo("Announcements")
            assertThat(createRoomParameters?.momentRoomKind).isEqualTo(MomentRoomKind.Channel)
            assertThat(createRoomParameters?.visibility).isEqualTo(RoomVisibility.Public)
            assertThat(createRoomParameters?.isEncrypted).isFalse()
        }
    }

    @Test
    fun `present - moment user search results are merged for active phone query`() = runTest {
        var calls = 0
        val matrixClient = FakeMatrixClient(
            searchMomentUsersResult = { query, limit, _ ->
                calls++
                assertThat(query).isEqualTo("+7 999 123-45-67")
                assertThat(limit).isEqualTo(20)
                Result.success(
                    listOf(
                        MatrixMomentUserSearchMatch(
                            userId = A_USER_ID,
                            displayName = "Alice",
                            avatarUrl = null,
                            phoneNumber = "+70000000000",
                        ),
                        MatrixMomentUserSearchMatch(
                            userId = A_USER_ID_2,
                            displayName = "Bob",
                            avatarUrl = null,
                            phoneNumber = "+79991234567",
                        ),
                        MatrixMomentUserSearchMatch(
                            userId = A_USER_ID_2,
                            displayName = "Bob duplicate",
                            avatarUrl = null,
                            phoneNumber = "+79991234567",
                        ),
                    )
                )
            }
        )
        val existingUser = MatrixUser(A_USER_ID_3, displayName = "Carol")
        val userListPresenter = FakeUserListPresenter().apply {
            givenState(
                aUserListState(
                    isSearchActive = true,
                    searchQuery = "+7 999 123-45-67",
                    searchResults = SearchBarResultState.Results(
                        persistentListOf(UserSearchResult(existingUser))
                    ),
                )
            )
        }
        val presenter = createStartChatPresenter(
            matrixClient = matrixClient,
            userListPresenter = userListPresenter,
        )

        presenter.test {
            val states = consumeItemsUntilPredicate { state ->
                state.userListState.searchResults.searchResultUserIds().contains(A_USER_ID_2)
            }
            val resultState = states.last().userListState.searchResults
            assertThat(resultState.searchResultUserIds()).containsExactly(A_USER_ID_3, A_USER_ID_2).inOrder()
            assertThat(resultState.userSearchResults().last().subtitle).isEqualTo("+79991234567")
            assertThat(calls).isEqualTo(1)
        }
    }

    @Test
    fun `present - moment user search results are merged for active name query`() = runTest {
        var calls = 0
        val matrixClient = FakeMatrixClient(
            searchMomentUsersResult = { query, limit, _ ->
                calls++
                assertThat(query).isEqualTo("Alice")
                assertThat(limit).isEqualTo(20)
                Result.success(
                    listOf(
                        MatrixMomentUserSearchMatch(
                            userId = A_USER_ID_2,
                            displayName = "Alice",
                            avatarUrl = null,
                            phoneNumber = null,
                        )
                    )
                )
            }
        )
        val userListPresenter = FakeUserListPresenter().apply {
            givenState(
                aUserListState(
                    isSearchActive = true,
                    searchQuery = "Alice",
                    searchResults = SearchBarResultState.NoResultsFound(),
                )
            )
        }
        val presenter = createStartChatPresenter(
            matrixClient = matrixClient,
            userListPresenter = userListPresenter,
        )

        presenter.test {
            val states = consumeItemsUntilPredicate { state ->
                state.userListState.searchResults.searchResultUserIds().contains(A_USER_ID_2)
            }
            val resultState = states.last().userListState.searchResults
            assertThat(resultState.searchResultUserIds()).containsExactly(A_USER_ID_2)
            assertThat(resultState.userSearchResults().single().subtitle).isEqualTo(A_USER_ID_2.value)
            assertThat(calls).isEqualTo(1)
        }
    }

    @Test
    fun `present - phonebook contacts are looked up and use device contact display names`() = runTest {
        var calls = 0
        val matrixClient = FakeMatrixClient(
            searchMomentUsersResult = { query, limit, _ ->
                calls++
                assertThat(query).isEqualTo("+79991234567")
                assertThat(limit).isEqualTo(1)
                Result.success(
                    listOf(
                        MatrixMomentUserSearchMatch(
                            userId = A_USER_ID_2,
                            displayName = "Alice Matrix",
                            avatarUrl = null,
                            phoneNumber = "+79991234567",
                        )
                    )
                )
            }
        )
        val presenter = createStartChatPresenter(
            matrixClient = matrixClient,
            deviceContactsDataSource = FakeDeviceContactsDataSource(
                listOf(
                    DeviceContact(
                        displayName = "Alice From Phone",
                        phoneNumbers = listOf("+79991234567"),
                    )
                )
            ),
        )

        presenter.test {
            val states = consumeItemsUntilPredicate { state ->
                state.phonebookContacts.any { it.matrixUser.userId == A_USER_ID_2 }
            }
            val phonebookContact = states.last().phonebookContacts.single()
            assertThat(phonebookContact.matrixUser.displayName).isEqualTo("Alice From Phone")
            assertThat(phonebookContact.subtitle).isEqualTo("+79991234567")
            assertThat(calls).isEqualTo(1)
        }
    }

    @Test
    fun `present - start DM action confirmation scenario - cancel`() = runTest {
        val matrixUser = MatrixUser(UserId("@name:domain"))
        val startDMConfirmationResult = ConfirmingStartDmWithMatrixUser(matrixUser, isUserIdentityUnknown = false)
        val executeResult = lambdaRecorder<MatrixUser, Boolean, MutableState<AsyncAction<RoomId>>, Unit> { _, _, actionState ->
            actionState.value = startDMConfirmationResult
        }
        val startDMAction = FakeStartDMAction(executeResult = executeResult)
        val presenter = createStartChatPresenter(startDMAction)
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.startDmAction).isInstanceOf(AsyncAction.Uninitialized::class.java)
            initialState.eventSink(StartChatEvents.StartDM(matrixUser))
            val confirmingState = awaitItem()
            assertThat(confirmingState.startDmAction).isEqualTo(startDMConfirmationResult)
            executeResult.assertions().isCalledOnce().with(
                value(matrixUser),
                value(false),
                any(),
            )
            // Cancelling should not create the DM
            confirmingState.eventSink(StartChatEvents.CancelStartDM)
            val finalState = awaitItem()
            assertThat(finalState.startDmAction.isUninitialized()).isTrue()
            executeResult.assertions().isCalledExactly(1)
        }
    }

    @Test
    fun `present - start DM action confirmation scenario - confirm`() = runTest {
        val matrixUser = MatrixUser(UserId("@name:domain"))
        val startDMConfirmationResult = ConfirmingStartDmWithMatrixUser(matrixUser, isUserIdentityUnknown = false)
        val executeResult = lambdaRecorder<MatrixUser, Boolean, MutableState<AsyncAction<RoomId>>, Unit> { _, _, actionState ->
            actionState.value = startDMConfirmationResult
        }
        val startDMAction = FakeStartDMAction(executeResult = executeResult)
        val presenter = createStartChatPresenter(startDMAction)
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.startDmAction).isInstanceOf(AsyncAction.Uninitialized::class.java)
            initialState.eventSink(StartChatEvents.StartDM(matrixUser))
            val confirmingState = awaitItem()
            assertThat(confirmingState.startDmAction).isEqualTo(startDMConfirmationResult)
            executeResult.assertions().isCalledOnce().with(
                value(matrixUser),
                value(false),
                any(),
            )
            // Start DM again should invoke the action with createIfDmDoesNotExist = true
            confirmingState.eventSink(StartChatEvents.StartDM(matrixUser))
            executeResult.assertions().isCalledExactly(2).withSequence(
                listOf(value(matrixUser), value(false), any()),
                listOf(value(matrixUser), value(true), any()),
            )
        }
    }
}

internal fun createStartChatPresenter(
    startDMAction: StartDMAction = FakeStartDMAction(),
    matrixClient: MatrixClient = FakeMatrixClient(),
    analyticsService: AnalyticsService = FakeAnalyticsService(),
    userListPresenter: FakeUserListPresenter = FakeUserListPresenter(),
    deviceContactsDataSource: DeviceContactsDataSource = FakeDeviceContactsDataSource(),
): StartChatPresenter {
    return StartChatPresenter(
        presenterFactory = FakeUserListPresenterFactory(userListPresenter),
        userRepository = FakeUserRepository(),
        userListDataStore = UserListDataStore(),
        startDMAction = startDMAction,
        matrixClient = matrixClient,
        analyticsService = analyticsService,
        buildMeta = aBuildMeta(),
        deviceContactsDataSource = deviceContactsDataSource,
    )
}

private class FakeDeviceContactsDataSource(
    private val contacts: List<DeviceContact> = emptyList(),
) : DeviceContactsDataSource {
    override suspend fun getContacts(): Result<List<DeviceContact>> {
        return Result.success(contacts)
    }
}

private fun SearchBarResultState<*>.searchResultUserIds(): List<UserId> {
    return userSearchResults().map { result -> result.matrixUser.userId }
}

private fun SearchBarResultState<*>.userSearchResults(): List<UserSearchResult> {
    return when (this) {
        is SearchBarResultState.Results<*> -> results as? List<*> ?: emptyList<Any>()
        else -> emptyList()
    }
        .filterIsInstance<UserSearchResult>()
}
