/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.home.impl.roomlist

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.element.android.features.home.impl.HomeView
import io.element.android.features.home.impl.R
import io.element.android.features.home.impl.aHomeState
import io.element.android.features.home.impl.components.RoomListMenuAction
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.model.RoomSummaryDisplayType
import io.element.android.features.home.impl.model.aRoomListRoomSummary
import io.element.android.features.home.impl.search.aRoomListSearchMessageResult
import io.element.android.features.home.impl.search.aRoomListSearchState
import io.element.android.features.home.impl.search.aRoomListSearchUserResult
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.test.AN_EVENT_ID
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.A_USER_ID
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.tests.testutils.EnsureNeverCalled
import io.element.android.tests.testutils.EnsureNeverCalledWithParam
import io.element.android.tests.testutils.EnsureNeverCalledWithTwoParams
import io.element.android.tests.testutils.EventsRecorder
import io.element.android.tests.testutils.clickOn
import io.element.android.tests.testutils.ensureCalledOnce
import io.element.android.tests.testutils.ensureCalledOnceWithParam
import io.element.android.tests.testutils.ensureCalledOnceWithTwoParams
import io.element.android.tests.testutils.setSafeContent
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class RoomListViewTest {
    @Config(qualifiers = "h1024dp")
    @Test
    fun `displaying the view automatically sends a couple of UpdateVisibleRangeEvents`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        setRoomListView(
            state = aRoomListState(
                eventSink = eventsRecorder,
            )
        )

        eventsRecorder.assertList(
            listOf(
                RoomListEvent.UpdateVisibleRange(0..4),
            )
        )
    }

    @Test
    fun `clicking on start chat when the session has no room invokes the expected callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>(expectEvents = false)
        ensureCalledOnce { callback ->
            setRoomListView(
                state = aRoomListState(
                    eventSink = eventsRecorder,
                    contentState = anEmptyContentState(),
                ),
                onCreateRoomClick = callback,
            )
            clickOn(CommonStrings.action_start_chat)
        }
    }

    @Test
    fun `clicking on profile bottom tab invokes settings callback`() = runAndroidComposeUiTest {
        ensureCalledOnce { callback ->
            setRoomListView(
                state = aRoomListState(),
                onSettingsClick = callback,
            )

            onNode(hasContentDescription(activity!!.getString(R.string.screen_home_tab_profile))).performClick()
        }
    }

    @Test
    fun `top start chat action uses Moment label and invokes callback`() = runAndroidComposeUiTest<ComponentActivity> {
        ensureCalledOnce { callback ->
            setRoomListView(
                state = aRoomListState(),
                onCreateRoomClick = callback,
            )

            onNode(hasContentDescription(activity!!.getString(CommonStrings.action_create_room))).assertDoesNotExist()
            onNode(hasContentDescription(activity!!.getString(CommonStrings.action_start_chat))).performClick()
        }
    }

    @Test
    fun `top contacts action invokes the contacts callback`() = runAndroidComposeUiTest<ComponentActivity> {
        ensureCalledOnce { callback ->
            setRoomListView(
                state = aRoomListState(),
                onOpenContactsClick = callback,
            )

            onNode(hasContentDescription(activity!!.getString(R.string.action_open_contacts))).performClick()
        }
    }

    @Test
    fun `room with unread messages only does not show Moment unread dot`() = runAndroidComposeUiTest<ComponentActivity> {
        setRoomListView(
            state = aRoomListState(
                contentState = aRoomsContentState(
                    summaries = persistentListOf(
                        aRoomListRoomSummary(
                            name = "Unread room",
                            numberOfUnreadMessages = 2,
                            numberOfUnreadMentions = 0,
                            numberOfUnreadNotifications = 0,
                        )
                    )
                )
            )
        )

        onNode(hasContentDescription(activity!!.getString(CommonStrings.a11y_notifications_new_messages))).assertDoesNotExist()
    }

    @Test
    fun `active search overlay does not show Android cancel button`() = runAndroidComposeUiTest<ComponentActivity> {
        setRoomListView(
            state = aRoomListState(
                searchState = aRoomListSearchState(isSearchActive = true),
            )
        )

        onNodeWithText(activity!!.getString(CommonStrings.action_cancel)).assertDoesNotExist()
    }

    @Test
    fun `clicking on a room invokes the expected callback`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val state = aRoomListState(
            eventSink = eventsRecorder,
        )
        val room0 = state.contentAsRooms().summaries.first {
            it.displayType == RoomSummaryDisplayType.ROOM
        }
        ensureCalledOnceWithTwoParams<RoomId, EventId?>(room0.roomId, null) { callback ->
            setRoomListView(
                state = state,
                onRoomClick = callback,
            )

            // Remove automatic initial events
            eventsRecorder.clear()

            onNodeWithText(room0.latestEvent.content().toString()).performClick()
        }

        eventsRecorder.assertEmpty()
    }

    @Test
    fun `clicking on a room twice invokes the expected callback only once`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val state = aRoomListState(
            eventSink = eventsRecorder,
        )
        val room0 = state.contentAsRooms().summaries.first {
            it.displayType == RoomSummaryDisplayType.ROOM
        }
        ensureCalledOnceWithTwoParams<RoomId, EventId?>(room0.roomId, null) { callback ->
            setRoomListView(
                state = state,
                onRoomClick = callback,
            )
            // Remove automatic initial events
            eventsRecorder.clear()
            onNodeWithText(room0.latestEvent.content().toString())
                .performClick()
                .performClick()
        }
        eventsRecorder.assertEmpty()
    }

    @Test
    fun `clicking on a message search result invokes room callback with event id`() = runAndroidComposeUiTest {
        val result = aRoomListSearchMessageResult(
            roomId = A_ROOM_ID,
            eventId = AN_EVENT_ID,
            roomTitle = "Messages room",
            description = "Alice: hello from search",
        )
        val state = aRoomListState(
            searchState = aRoomListSearchState(
                isSearchActive = true,
                query = "hello",
                messageResults = persistentListOf(result),
            ),
        )
        ensureCalledOnceWithTwoParams<RoomId, EventId?>(A_ROOM_ID, AN_EVENT_ID) { callback ->
            setRoomListView(
                state = state,
                onRoomClick = callback,
            )

            onNodeWithText("Alice: hello from search").performClick()
        }
    }

    @Test
    fun `clicking on a user search result with direct room invokes room callback`() = runAndroidComposeUiTest {
        val result = aRoomListSearchUserResult(
            userId = A_USER_ID,
            directRoomId = A_ROOM_ID,
            title = "Alice",
            description = "+1 555 0100",
        )
        val state = aRoomListState(
            searchState = aRoomListSearchState(
                isSearchActive = true,
                query = "+1 555 0100",
                userResults = persistentListOf(result),
            ),
        )
        ensureCalledOnceWithTwoParams<RoomId, EventId?>(A_ROOM_ID, null) { callback ->
            setRoomListView(
                state = state,
                onRoomClick = callback,
            )

            onNodeWithText("Alice").performClick()
        }
    }

    @Test
    fun `clicking on a user search result without direct room invokes user callback`() = runAndroidComposeUiTest {
        val result = aRoomListSearchUserResult(
            userId = A_USER_ID,
            directRoomId = null,
            title = "Alice",
            description = "+1 555 0100",
        )
        val state = aRoomListState(
            searchState = aRoomListSearchState(
                isSearchActive = true,
                query = "+1 555 0100",
                userResults = persistentListOf(result),
            ),
        )
        ensureCalledOnceWithParam(result.matrixUser) { callback ->
            setRoomListView(
                state = state,
                onUserClick = callback,
            )

            onNodeWithText("Alice").performClick()
        }
    }

    @Test
    fun `long clicking on a room emits the expected Event`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val state = aRoomListState(
            eventSink = eventsRecorder,
        )
        val room0 = state.contentAsRooms().summaries.first {
            it.displayType == RoomSummaryDisplayType.ROOM
        }
        setRoomListView(
            state = state,
        )
        // Remove automatic initial events
        eventsRecorder.clear()

        onNodeWithText(room0.latestEvent.content().toString()).performTouchInput { longClick() }
        eventsRecorder.assertSingle(RoomListEvent.ShowContextMenu(room0))
    }

    @Test
    fun `clicking on a room setting invokes the expected callback and emits expected Event`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val state = aRoomListState(
            contextMenu = aContextMenuShown(),
            eventSink = eventsRecorder,
        )
        val room0 = (state.contextMenu as RoomListState.ContextMenu.Shown).roomId
        ensureCalledOnceWithParam(room0) { callback ->
            setRoomListView(
                state = state,
                onRoomSettingsClick = callback,
            )

            // Remove automatic initial events
            eventsRecorder.clear()

            clickOn(CommonStrings.common_settings)
        }

        eventsRecorder.assertSingle(RoomListEvent.HideContextMenu)
    }

    @Test
    fun `confirming direct user block emits the expected Event`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val state = aRoomListState(
            directUserBlockConfirmation = RoomListState.DirectUserBlockConfirmation.Shown(
                userId = A_USER_ID,
                displayName = "Alice",
                blocked = true,
            ),
            eventSink = eventsRecorder,
        )
        setRoomListView(state = state)

        // Remove automatic initial events
        eventsRecorder.clear()

        clickOn(R.string.screen_home_direct_user_block_alert_action)
        eventsRecorder.assertSingle(RoomListEvent.SetDirectUserBlocked(A_USER_ID, blocked = true))
    }

    @Test
    fun `clicking on accept and decline invite emits the expected Events`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val state = aRoomListState(
            eventSink = eventsRecorder,
        )
        val invitedRoom = state.contentAsRooms().summaries.first {
            it.displayType == RoomSummaryDisplayType.INVITE
        }
        setRoomListView(state = state)

        // Remove automatic initial events
        eventsRecorder.clear()

        clickOn(CommonStrings.action_accept)
        clickOn(CommonStrings.action_decline)
        eventsRecorder.assertList(
            listOf(
                RoomListEvent.AcceptInvite(invitedRoom),
                RoomListEvent.ShowDeclineInviteMenu(invitedRoom),
            )
        )
    }
}

private fun AndroidComposeUiTest<ComponentActivity>.setRoomListView(
    state: RoomListState,
    onRoomClick: (RoomId, EventId?) -> Unit = EnsureNeverCalledWithTwoParams(),
    onUserClick: (MatrixUser) -> Unit = EnsureNeverCalledWithParam(),
    onSettingsClick: () -> Unit = EnsureNeverCalled(),
    onCreateRoomClick: () -> Unit = EnsureNeverCalled(),
    onOpenContactsClick: () -> Unit = EnsureNeverCalled(),
    onCreateSpaceClick: () -> Unit = EnsureNeverCalled(),
    onRoomSettingsClick: (RoomId) -> Unit = EnsureNeverCalledWithParam(),
    onMenuActionClick: (RoomListMenuAction) -> Unit = EnsureNeverCalledWithParam(),
    onReportRoomClick: (RoomId) -> Unit = EnsureNeverCalledWithParam(),
    onDeclineInviteAndBlockUser: (RoomListRoomSummary) -> Unit = EnsureNeverCalledWithParam(),
) {
    setSafeContent {
        HomeView(
            homeState = aHomeState(roomListState = state),
            onRoomClick = onRoomClick,
            onUserClick = onUserClick,
            onSettingsClick = onSettingsClick,
            onStartChatClick = onCreateRoomClick,
            onOpenContactsClick = onOpenContactsClick,
            onCreateSpaceClick = onCreateSpaceClick,
            onRoomSettingsClick = onRoomSettingsClick,
            onMenuActionClick = onMenuActionClick,
            onDeclineInviteAndBlockUser = onDeclineInviteAndBlockUser,
            onReportRoomClick = onReportRoomClick,
            acceptDeclineInviteView = {},
            leaveRoomView = {},
        )
    }
}
