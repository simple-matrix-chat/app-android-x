/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.startchat.impl.root

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.element.android.features.startchat.impl.R
import io.element.android.features.startchat.impl.userlist.UserListEvents
import io.element.android.features.startchat.impl.userlist.aRecentDirectRoomList
import io.element.android.features.startchat.impl.userlist.aUserListState
import io.element.android.libraries.designsystem.theme.components.SearchBarResultState
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.createroom.MomentRoomKind
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.model.getBestName
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.libraries.usersearch.api.UserSearchResult
import io.element.android.tests.testutils.EnsureNeverCalled
import io.element.android.tests.testutils.EnsureNeverCalledWithParam
import io.element.android.tests.testutils.EventsRecorder
import io.element.android.tests.testutils.clickOn
import io.element.android.tests.testutils.ensureCalledOnce
import io.element.android.tests.testutils.ensureCalledOnceWithParam
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class StartChatViewTest {
    @Test
    fun `clicking on back invokes the expected callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<StartChatEvents>(expectEvents = false)
        ensureCalledOnce {
            setStartChatView(
                aCreateRoomRootState(
                    eventSink = eventsRecorder,
                ),
                onCloseClick = it
            )
            val close = activity!!.getString(CommonStrings.action_close)
            onNodeWithContentDescription(close).performClick()
        }
    }

    @Test
    fun `creating a group emits the expected event`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<StartChatEvents>()
        setStartChatView(
            aCreateRoomRootState(
                eventSink = eventsRecorder,
            )
        )
        clickOn(R.string.screen_start_chat_moment_group_title)
        onNodeWithText(activity!!.getString(R.string.screen_start_chat_moment_group_placeholder))
            .performTextInput("Project team")
        clickOn(R.string.screen_start_chat_moment_group_primary_action)

        eventsRecorder.assertSingle(
            StartChatEvents.CreateMomentRoom(
                name = "Project team",
                momentRoomKind = MomentRoomKind.Group,
                isPublic = false,
            )
        )
    }

    @Test
    fun `creating a channel emits the expected event`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<StartChatEvents>()
        setStartChatView(
            aCreateRoomRootState(
                eventSink = eventsRecorder,
            )
        )
        clickOn(R.string.screen_start_chat_moment_channel_title)
        onNodeWithText(activity!!.getString(R.string.screen_start_chat_moment_channel_placeholder))
            .performTextInput("Announcements")
        clickOn(R.string.screen_start_chat_moment_channel_primary_action)

        eventsRecorder.assertSingle(
            StartChatEvents.CreateMomentRoom(
                name = "Announcements",
                momentRoomKind = MomentRoomKind.Channel,
                isPublic = true,
            )
        )
    }

    @Test
    fun `group and channel visibility copy follows selected Moment mode`() = runAndroidComposeUiTest<ComponentActivity> {
        val eventsRecorder = EventsRecorder<StartChatEvents>(expectEvents = false)
        setStartChatView(
            aCreateRoomRootState(
                eventSink = eventsRecorder,
            ),
        )

        clickOn(R.string.screen_start_chat_moment_group_title)
        onNodeWithText(activity!!.getString(R.string.screen_start_chat_moment_visibility_private_group_title)).assertExists()
        onNodeWithText(activity!!.getString(R.string.screen_start_chat_moment_visibility_private_group_description)).assertExists()

        clickOn(R.string.screen_start_chat_moment_channel_title)
        onNodeWithText(activity!!.getString(R.string.screen_start_chat_moment_visibility_public_channel_title)).assertExists()
        onNodeWithText(activity!!.getString(R.string.screen_start_chat_moment_visibility_public_channel_description)).assertExists()
    }

    @Test
    fun `legacy start chat actions are not shown on Moment root`() = runAndroidComposeUiTest<ComponentActivity> {
        val eventsRecorder = EventsRecorder<StartChatEvents>(expectEvents = false)
        setStartChatView(
            aCreateRoomRootState(
                applicationName = "test",
                eventSink = eventsRecorder,
            ),
        )

        val inviteFriends = activity!!.getString(CommonStrings.action_invite_friends_to_app, "test")
        onNodeWithText(inviteFriends).assertDoesNotExist()
        onNodeWithText(activity!!.getString(R.string.screen_room_directory_search_title)).assertDoesNotExist()
        onNodeWithText(activity!!.getString(R.string.screen_start_chat_join_room_by_address_action)).assertDoesNotExist()
    }

    @Test
    fun `direct input activates search without generic search bar chrome`() = runAndroidComposeUiTest<ComponentActivity> {
        val startChatEventsRecorder = EventsRecorder<StartChatEvents>(expectEvents = false)
        val userListEventsRecorder = EventsRecorder<UserListEvents>()
        setStartChatView(
            aCreateRoomRootState(
                userListState = aUserListState(
                    eventSink = userListEventsRecorder,
                ),
                eventSink = startChatEventsRecorder,
            ),
        )

        onNodeWithContentDescription(activity!!.getString(CommonStrings.action_search)).assertDoesNotExist()
        onNodeWithText(activity!!.getString(R.string.screen_start_chat_moment_direct_placeholder))
            .performTextInput("alice")

        userListEventsRecorder.assertSingle(UserListEvents.OnSearchActiveChanged(true))
    }

    @Test
    fun `active direct search keeps Moment root chrome`() = runAndroidComposeUiTest<ComponentActivity> {
        val eventsRecorder = EventsRecorder<StartChatEvents>(expectEvents = false)
        setStartChatView(
            aCreateRoomRootState(
                userListState = aUserListState(
                    isSearchActive = true,
                    searchQuery = "zzzmoment",
                    searchResults = SearchBarResultState.NoResultsFound(),
                ),
                eventSink = eventsRecorder,
            ),
        )

        onNodeWithText(activity!!.getString(R.string.screen_start_chat_moment_title)).assertExists()
        onNodeWithText(activity!!.getString(R.string.screen_start_chat_moment_direct_footer_title)).assertExists()
        onNodeWithContentDescription(activity!!.getString(CommonStrings.action_back)).assertDoesNotExist()
        onNodeWithText(activity!!.getString(R.string.screen_start_chat_moment_direct_not_found_title)).assertExists()
        onNodeWithText(activity!!.getString(R.string.screen_start_chat_moment_direct_not_found_body)).assertExists()
        onNodeWithText(activity!!.getString(CommonStrings.common_no_results)).assertDoesNotExist()
    }

    @Test
    fun `active direct search ready card starts a direct chat from the primary action`() = runAndroidComposeUiTest<ComponentActivity> {
        val matrixUser = MatrixUser(
            userId = UserId("@alice:example.org"),
            displayName = "Alice",
        )
        val eventsRecorder = EventsRecorder<StartChatEvents>()
        setStartChatView(
            aCreateRoomRootState(
                userListState = aUserListState(
                    isSearchActive = true,
                    searchQuery = "alice",
                    searchResults = SearchBarResultState.Results(
                        persistentListOf(
                            UserSearchResult(
                                matrixUser = matrixUser,
                                subtitle = "+79991234567",
                            )
                        )
                    ),
                ),
                eventSink = eventsRecorder,
            ),
        )

        onNodeWithText(activity!!.getString(R.string.screen_start_chat_moment_direct_ready_title)).assertExists()
        onNodeWithText(activity!!.getString(R.string.screen_start_chat_moment_direct_ready_body)).assertExists()
        onNodeWithText("Alice").assertExists()
        onNodeWithText("+79991234567").assertExists()
        clickOn(R.string.screen_start_chat_moment_direct_primary_action)

        eventsRecorder.assertSingle(StartChatEvents.StartDM(matrixUser))
    }

    @Test
    fun `active direct search ready card prefers exact localpart match`() = runAndroidComposeUiTest<ComponentActivity> {
        val firstResult = MatrixUser(
            userId = UserId("@team:example.org"),
            displayName = "Alice Team",
        )
        val localPartMatch = MatrixUser(
            userId = UserId("@alice:example.org"),
            displayName = "Zed",
        )
        val eventsRecorder = EventsRecorder<StartChatEvents>()
        setStartChatView(
            aCreateRoomRootState(
                userListState = aUserListState(
                    isSearchActive = true,
                    searchQuery = "alice",
                    searchResults = SearchBarResultState.Results(
                        persistentListOf(
                            UserSearchResult(firstResult),
                            UserSearchResult(localPartMatch),
                        )
                    ),
                ),
                eventSink = eventsRecorder,
            ),
        )

        onNodeWithText("Zed").assertExists()
        onNodeWithText("@alice:example.org").assertExists()
        clickOn(R.string.screen_start_chat_moment_direct_primary_action)

        eventsRecorder.assertSingle(StartChatEvents.StartDM(localPartMatch))
    }

    @Config(qualifiers = "h1024dp")
    @Test
    fun `clicking on a user suggestion invokes the expected callback`() = runAndroidComposeUiTest {
        val recentDirectRoomList = aRecentDirectRoomList()
        val firstRoom = recentDirectRoomList[0]
        val eventsRecorder = EventsRecorder<StartChatEvents>(expectEvents = false)
        ensureCalledOnceWithParam(firstRoom.roomId) {
            setStartChatView(
                aCreateRoomRootState(
                    userListState = aUserListState(
                        recentDirectRooms = recentDirectRoomList
                    ),
                    eventSink = eventsRecorder,
                ),
                onOpenDM = it
            )
            onNodeWithText(firstRoom.matrixUser.getBestName()).performScrollTo().performClick()
        }
    }

    @Config(qualifiers = "h1024dp")
    @Test
    fun `phonebook contacts are shown and start a direct chat`() = runAndroidComposeUiTest<ComponentActivity> {
        val matrixUser = MatrixUser(
            userId = UserId("@alice:example.org"),
            displayName = "Alice From Phone",
        )
        val eventsRecorder = EventsRecorder<StartChatEvents>()
        setStartChatView(
            aCreateRoomRootState(
                phonebookContacts = listOf(
                    UserSearchResult(
                        matrixUser = matrixUser,
                        subtitle = "+79991234567",
                    )
                ),
                eventSink = eventsRecorder,
            ),
        )

        onNodeWithText(activity!!.getString(R.string.screen_start_chat_moment_contacts_section_title)).performScrollTo().assertExists()
        onNodeWithText("Alice From Phone").performScrollTo().performClick()

        eventsRecorder.assertSingle(StartChatEvents.StartDM(matrixUser))
    }

    @Config(qualifiers = "h1024dp")
    @Test
    fun `phonebook contacts are not duplicated in recent direct rooms`() = runAndroidComposeUiTest<ComponentActivity> {
        val recentDirectRoomList = aRecentDirectRoomList()
        val firstRecentRoom = recentDirectRoomList[0]
        val eventsRecorder = EventsRecorder<StartChatEvents>(expectEvents = false)
        setStartChatView(
            aCreateRoomRootState(
                userListState = aUserListState(
                    recentDirectRooms = recentDirectRoomList,
                ),
                phonebookContacts = listOf(
                    UserSearchResult(
                        matrixUser = firstRecentRoom.matrixUser.copy(displayName = "Phonebook Alice"),
                        subtitle = "+79991234567",
                    )
                ),
                eventSink = eventsRecorder,
            ),
        )

        onNodeWithText("Phonebook Alice").performScrollTo().assertExists()
        onNodeWithText(firstRecentRoom.matrixUser.getBestName()).assertDoesNotExist()
    }
}

private fun AndroidComposeUiTest<ComponentActivity>.setStartChatView(
    state: StartChatState,
    onCloseClick: () -> Unit = EnsureNeverCalled(),
    onOpenDM: (RoomId) -> Unit = EnsureNeverCalledWithParam(),
    onInviteFriendsClick: () -> Unit = EnsureNeverCalled(),
    onJoinRoomByAddressClick: () -> Unit = EnsureNeverCalled(),
    onRoomDirectorySearchClick: () -> Unit = EnsureNeverCalled(),
) {
    setContent {
        StartChatView(
            state = state,
            onCloseClick = onCloseClick,
            onOpenDM = onOpenDM,
            onInviteFriendsClick = onInviteFriendsClick,
            onJoinByAddressClick = onJoinRoomByAddressClick,
            onRoomDirectorySearchClick = onRoomDirectorySearchClick,
        )
    }
}
