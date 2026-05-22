/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roomdetails.impl.members.details

import com.google.common.truth.Truth.assertThat
import io.element.android.features.roomdetails.impl.aJoinedRoom
import io.element.android.features.roomdetails.impl.members.aRoomMember
import io.element.android.features.userprofile.api.UserProfilePresenterFactory
import io.element.android.features.userprofile.shared.aUserProfileState
import io.element.android.libraries.androidutils.clipboard.ClipboardHelper
import io.element.android.libraries.androidutils.clipboard.FakeClipboardHelper
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.JoinedRoom
import io.element.android.libraries.matrix.api.room.RoomMembersState
import io.element.android.libraries.matrix.test.AN_EXCEPTION
import io.element.android.tests.testutils.WarmUpRule
import io.element.android.tests.testutils.test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class RoomMemberDetailsPresenterTest {
    @get:Rule
    val warmUpRule = WarmUpRule()

    @Test
    fun `present - returns the room member's data, then updates it if needed`() = runTest {
        val roomMember = aRoomMember(displayName = "Alice")
        val room = aJoinedRoom(
            userDisplayNameResult = { Result.success("A custom name") },
            userAvatarUrlResult = { Result.success("A custom avatar") },
            getUpdatedMemberResult = { Result.success(roomMember) },
        ).apply {
            givenRoomMembersState(RoomMembersState.Ready(persistentListOf(roomMember)))
        }
        val presenter = createRoomMemberDetailsPresenter(
            room = room,
        )
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.userName).isEqualTo("Alice")
            assertThat(initialState.avatarUrl).isEqualTo("Profile avatar url")
            skipItems(1)
            val nextState = awaitItem()
            assertThat(nextState.userName).isEqualTo("A custom name")
            assertThat(nextState.avatarUrl).isEqualTo("A custom avatar")
        }
    }

    @Test
    fun `present - will recover when retrieving room member details fails`() = runTest {
        val roomMember = aRoomMember(
            displayName = "Alice",
            avatarUrl = "Alice Avatar url",
        )
        val room = aJoinedRoom(
            userDisplayNameResult = { Result.failure(RuntimeException()) },
            userAvatarUrlResult = { Result.failure(RuntimeException()) },
            getUpdatedMemberResult = { Result.failure(AN_EXCEPTION) },
        ).apply {
            givenRoomMembersState(RoomMembersState.Ready(persistentListOf(roomMember)))
        }

        val presenter = createRoomMemberDetailsPresenter(
            room = room,
        )
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.userName).isEqualTo("Alice")
            assertThat(initialState.avatarUrl).isEqualTo("Alice Avatar url")
        }
    }

    @Test
    fun `present - will fallback to original data if the updated data is null`() = runTest {
        val roomMember = aRoomMember(displayName = "Alice")
        val room = aJoinedRoom(
            userDisplayNameResult = { Result.success(null) },
            userAvatarUrlResult = { Result.success(null) },
            getUpdatedMemberResult = { Result.success(roomMember) }
        ).apply {
            givenRoomMembersState(RoomMembersState.Ready(persistentListOf(roomMember)))
        }
        val presenter = createRoomMemberDetailsPresenter(
            room = room,
        )
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.userName).isEqualTo("Alice")
            assertThat(initialState.avatarUrl).isEqualTo("Profile avatar url")
        }
    }

    @Test
    fun `present - will fallback to user profile if user is not a member of the room`() = runTest {
        val room = aJoinedRoom(
            userDisplayNameResult = { Result.failure(Exception("Not a member!")) },
            userAvatarUrlResult = { Result.failure(Exception("Not a member!")) },
            getUpdatedMemberResult = { Result.failure(AN_EXCEPTION) },
        )
        val presenter = createRoomMemberDetailsPresenter(
            room = room,
        )
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.userName).isEqualTo("Profile user name")
            assertThat(initialState.avatarUrl).isEqualTo("Profile avatar url")
        }
    }

    @Test
    fun `present - null cases`() = runTest {
        val roomMember = aRoomMember(
            displayName = null,
            avatarUrl = null,
        )
        val room = aJoinedRoom(
            userDisplayNameResult = { Result.success(null) },
            userAvatarUrlResult = { Result.success(null) },
            getUpdatedMemberResult = { Result.success(roomMember) },
        )
        val presenter = createRoomMemberDetailsPresenter(
            room = room,
            userProfilePresenterFactory = {
                Presenter {
                    aUserProfileState(
                        userName = null,
                        avatarUrl = null,
                    )
                }
            },
        )
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.userName).isNull()
            assertThat(initialState.avatarUrl).isNull()
        }
    }

    private fun createRoomMemberDetailsPresenter(
        room: JoinedRoom,
        userProfilePresenterFactory: UserProfilePresenterFactory = UserProfilePresenterFactory {
            Presenter {
                aUserProfileState(
                    userName = "Profile user name",
                    avatarUrl = "Profile avatar url",
                )
            }
        },
        clipboardHelper: ClipboardHelper = FakeClipboardHelper(),
    ): RoomMemberDetailsPresenter {
        return RoomMemberDetailsPresenter(
            roomMemberId = UserId("@alice:server.org"),
            room = room,
            userProfilePresenterFactory = userProfilePresenterFactory,
            clipboardHelper = clipboardHelper,
        )
    }
}
