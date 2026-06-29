/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.home.impl.contacts

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.element.android.features.home.impl.R
import io.element.android.features.home.impl.aHomeState
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.model.aRoomListRoomSummary
import io.element.android.features.home.impl.roomlist.aRoomListState
import io.element.android.features.home.impl.roomlist.aRoomsContentState
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.tests.testutils.EnsureNeverCalled
import io.element.android.tests.testutils.EnsureNeverCalledWithParam
import io.element.android.tests.testutils.ensureCalledOnce
import io.element.android.tests.testutils.ensureCalledOnceWithParam
import io.element.android.tests.testutils.setSafeContent
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeContactsViewTest {
    @Test
    fun `empty contacts state uses Moment copy and invokes new chat`() = runAndroidComposeUiTest<ComponentActivity> {
        ensureCalledOnce { callback ->
            setHomeContactsView(
                state = aHomeState(
                    roomListState = aRoomListState(
                        contentState = aRoomsContentState(summaries = persistentListOf()),
                    ),
                ),
                onStartChatClick = callback,
            )

            onNodeWithText(activity!!.getString(R.string.screen_home_contacts_no_contacts_title)).assertExists()
            onNodeWithText(activity!!.getString(R.string.screen_home_contacts_no_contacts_message)).assertExists()
            onNodeWithText(activity!!.getString(R.string.screen_home_contacts_new_chat)).performClick()
        }
    }

    @Test
    fun `contacts list filters out self and non contact rooms`() = runAndroidComposeUiTest<ComponentActivity> {
        val currentUserId = UserId("@me:example.org")
        val aliceRoom = aRoomListRoomSummary(
            id = "!alice:example.org",
            name = "Alice room",
            isDirect = true,
            directUserId = UserId("@alice:example.org"),
            directUserDisplayName = "Alice",
        )
        val bobRoom = aRoomListRoomSummary(
            id = "!bob:example.org",
            name = "Bob room",
            isDirect = true,
            directUserId = UserId("@bob:example.org"),
            directUserDisplayName = "Bob",
        )
        val savedMessagesRoom = aRoomListRoomSummary(
            id = "!saved:example.org",
            name = "Saved Messages",
            isDirect = true,
            directUserId = currentUserId,
            directUserDisplayName = "Saved Messages",
        )
        val groupRoom = aRoomListRoomSummary(
            id = "!group:example.org",
            name = "Project Group",
            isDirect = false,
        )
        val archivedDirectRoom = aRoomListRoomSummary(
            id = "!archived:example.org",
            name = "Archived Alice",
            isDirect = true,
            directUserId = UserId("@archived:example.org"),
            directUserDisplayName = "Archived Alice",
            isArchived = true,
        )

        ensureCalledOnceWithParam<RoomListRoomSummary>(bobRoom) { callback ->
            setHomeContactsView(
                state = aHomeState(
                    matrixUser = MatrixUser(userId = currentUserId),
                    roomListState = aRoomListState(
                        contentState = aRoomsContentState(
                            summaries = persistentListOf(
                                aliceRoom,
                                bobRoom,
                                savedMessagesRoom,
                                groupRoom,
                                archivedDirectRoom,
                            ),
                        ),
                    ),
                ),
                onRoomClick = callback,
            )

            onNodeWithText("Alice").assertExists()
            onNodeWithText("Bob").assertExists()
            onNodeWithText("Saved Messages").assertDoesNotExist()
            onNodeWithText("Project Group").assertDoesNotExist()
            onNodeWithText("Archived Alice").assertDoesNotExist()

            onNodeWithText(activity!!.getString(R.string.screen_home_contacts_search_placeholder)).performTextInput("bob")

            onNodeWithText("Alice").assertDoesNotExist()
            onNodeWithText("Bob").performClick()
        }
    }

    @Test
    fun `phonebook contacts are shown and unavailable contact emits event`() = runAndroidComposeUiTest<ComponentActivity> {
        val phonebookContact = HomeDeviceContact(
            id = "phonebook-1",
            displayName = "Moment QA Contact",
            phoneNumbers = listOf("+7 999 000-00-01"),
        )
        val events = mutableListOf<HomeContactsEvent>()
        setHomeContactsView(
            state = aHomeState(
                roomListState = aRoomListState(
                    contentState = aRoomsContentState(summaries = persistentListOf()),
                ),
            ),
            contactsState = aHomeContactsState(
                contacts = AsyncData.Success(
                    aHomeContactsData(
                        unavailableContacts = persistentListOf(phonebookContact),
                    )
                ),
                eventSink = events::add,
            ),
        )

        onNodeWithText(activity!!.getString(R.string.screen_home_contacts_phonebook_section_title)).assertExists()
        onNodeWithText("Moment QA Contact").performClick()

        assertThat(events).containsExactly(HomeContactsEvent.SelectUnavailableContact(phonebookContact))
    }

    @Test
    fun `matched phonebook contact opens Matrix user and deduplicates direct room`() = runAndroidComposeUiTest<ComponentActivity> {
        val matrixUser = MatrixUser(
            userId = UserId("@alice:example.org"),
            displayName = "Alice Phonebook",
        )
        val aliceRoom = aRoomListRoomSummary(
            id = "!alice:example.org",
            name = "Alice room",
            isDirect = true,
            directUserId = matrixUser.userId,
            directUserDisplayName = "Alice Room",
        )

        ensureCalledOnceWithParam(matrixUser) { callback ->
            setHomeContactsView(
                state = aHomeState(
                    roomListState = aRoomListState(
                        contentState = aRoomsContentState(summaries = persistentListOf(aliceRoom)),
                    ),
                ),
                contactsState = aHomeContactsState(
                    contacts = AsyncData.Success(
                        aHomeContactsData(
                            momentContacts = persistentListOf(
                                HomeMomentContact(
                                    matrixUser = matrixUser,
                                    subtitle = "+7 999 000-00-01",
                                )
                            ),
                        )
                    ),
                ),
                onUserClick = callback,
            )

            onNodeWithText("Alice Room").assertDoesNotExist()
            onNodeWithText("Alice Phonebook").performClick()
        }
    }

    @Test
    fun `unavailable phonebook contact dialog is shown`() = runAndroidComposeUiTest<ComponentActivity> {
        val phonebookContact = HomeDeviceContact(
            id = "phonebook-1",
            displayName = "Moment QA Contact",
            phoneNumbers = listOf("+7 999 000-00-01"),
        )
        setHomeContactsView(
            state = aHomeState(
                roomListState = aRoomListState(
                    contentState = aRoomsContentState(summaries = persistentListOf()),
                ),
            ),
            contactsState = aHomeContactsState(
                contacts = AsyncData.Success(
                    aHomeContactsData(
                        unavailableContacts = persistentListOf(phonebookContact),
                    )
                ),
                unavailableContactDialog = phonebookContact,
            ),
        )

        onNodeWithText(activity!!.getString(R.string.screen_home_contacts_unavailable_dialog_title)).assertExists()
        onNodeWithText(activity!!.getString(R.string.screen_home_contacts_unavailable_dialog_message, "Moment QA Contact")).assertExists()
    }

    @Test
    fun `contacts permission card requests permission`() = runAndroidComposeUiTest<ComponentActivity> {
        val events = mutableListOf<HomeContactsEvent>()
        setHomeContactsView(
            state = aHomeState(
                roomListState = aRoomListState(
                    contentState = aRoomsContentState(summaries = persistentListOf()),
                ),
            ),
            contactsState = aHomeContactsState(
                contactsPermissionGranted = false,
                contacts = AsyncData.Uninitialized,
                eventSink = events::add,
            ),
        )

        onNodeWithText(activity!!.getString(R.string.screen_home_contacts_permission_title)).assertExists()
        onNodeWithText(activity!!.getString(R.string.screen_home_contacts_permission_action)).performClick()

        assertThat(events).containsExactly(HomeContactsEvent.RequestContactsPermission)
    }
}

private fun androidx.compose.ui.test.AndroidComposeUiTest<ComponentActivity>.setHomeContactsView(
    state: io.element.android.features.home.impl.HomeState,
    contactsState: HomeContactsState = aHomeContactsState(contacts = AsyncData.Success(aHomeContactsData())),
    onBackClick: () -> Unit = EnsureNeverCalled(),
    onStartChatClick: () -> Unit = EnsureNeverCalled(),
    onRoomClick: (RoomListRoomSummary) -> Unit = EnsureNeverCalledWithParam(),
    onUserClick: (MatrixUser) -> Unit = EnsureNeverCalledWithParam(),
) {
    setSafeContent {
        HomeContactsView(
            state = state,
            contactsState = contactsState,
            onBackClick = onBackClick,
            onStartChatClick = onStartChatClick,
            onRoomClick = onRoomClick,
            onUserClick = onUserClick,
        )
    }
}
