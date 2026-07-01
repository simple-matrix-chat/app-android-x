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
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.element.android.features.home.impl.R
import io.element.android.features.home.impl.filters.MomentHomeMuteDuration
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.test.A_USER_ID
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.tests.testutils.EnsureCalledOnceWithParam
import io.element.android.tests.testutils.EnsureNeverCalledWithParam
import io.element.android.tests.testutils.EventsRecorder
import io.element.android.tests.testutils.clickOn
import io.element.android.tests.testutils.setSafeContent
import org.junit.Test
import org.junit.runner.RunWith
import io.element.android.features.leaveroom.api.R as LeaveRoomR

@RunWith(AndroidJUnit4::class)
class RoomListContextMenuTest {
    @Test
    fun `clicking on Mark as read generates expected Events`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val contextMenu = aContextMenuShown(hasNewContent = true)
        setRoomListContextMenu(
            contextMenu = contextMenu,
            eventSink = eventsRecorder,
        )
        clickOn(R.string.screen_roomlist_mark_as_read)
        eventsRecorder.assertList(
            listOf(
                RoomListEvent.HideContextMenu,
                RoomListEvent.MarkAsRead(contextMenu.roomId),
            )
        )
    }

    @Test
    fun `clicking on Mark as unread generates expected Events`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val contextMenu = aContextMenuShown(hasNewContent = false)
        setRoomListContextMenu(
            contextMenu = contextMenu,
            eventSink = eventsRecorder,
        )
        clickOn(R.string.screen_roomlist_mark_as_unread)
        eventsRecorder.assertList(
            listOf(
                RoomListEvent.HideContextMenu,
                RoomListEvent.MarkAsUnread(contextMenu.roomId),
            )
        )
    }

    @Test
    fun `clicking on Leave room generates expected Events`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val contextMenu = aContextMenuShown(isDm = false)
        setRoomListContextMenu(
            contextMenu = contextMenu,
            eventSink = eventsRecorder,
        )
        scrollToAndClickOn(CommonStrings.action_leave_room)
        eventsRecorder.assertList(
            listOf(
                RoomListEvent.HideContextMenu,
                RoomListEvent.LeaveRoom(contextMenu.roomId, needsConfirmation = true),
            )
        )
    }

    @Test
    fun `clicking on Delete chat in DM generates expected Events`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val contextMenu = aContextMenuShown(isDm = true, canLeaveRoom = true)
        setRoomListContextMenu(
            contextMenu = contextMenu,
            eventSink = eventsRecorder,
        )
        scrollToAndClickOn(LeaveRoomR.string.action_delete_chat)
        eventsRecorder.assertList(
            listOf(
                RoomListEvent.HideContextMenu,
                RoomListEvent.LeaveRoom(contextMenu.roomId, needsConfirmation = true),
            )
        )
    }

    @Test
    fun `self direct context menu does not show Delete chat`() = runAndroidComposeUiTest {
        setRoomListContextMenu(
            contextMenu = aContextMenuShown(isDm = true, canLeaveRoom = false),
            eventSink = EventsRecorder(expectEvents = false),
        )
        onNode(hasText(activity!!.getString(LeaveRoomR.string.action_delete_chat)) and hasClickAction())
            .assertDoesNotExist()
    }

    @Test
    fun `clicking on Report room invokes the expected callback and generates expected Event`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val contextMenu = aContextMenuShown()
        val callback = EnsureCalledOnceWithParam(contextMenu.roomId, Unit)
        setRoomListContextMenu(
            contextMenu = contextMenu,
            canReportRoom = true,
            eventSink = eventsRecorder,
            onRoomSettingsClick = EnsureNeverCalledWithParam(),
            onReportRoomClick = callback,
        )
        scrollToAndClickOn(CommonStrings.action_report_room)
        eventsRecorder.assertSingle(RoomListEvent.HideContextMenu)
        callback.assertSuccess()
    }

    @Test
    fun `clicking on Settings invokes the expected callback and generates expected Event`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val contextMenu = aContextMenuShown()
        val callback = EnsureCalledOnceWithParam(contextMenu.roomId, Unit)
        setRoomListContextMenu(
            contextMenu = contextMenu,
            eventSink = eventsRecorder,
            onRoomSettingsClick = callback,
        )
        clickOn(CommonStrings.common_settings)
        eventsRecorder.assertSingle(RoomListEvent.HideContextMenu)
        callback.assertSuccess()
    }

    @Test
    fun `clicking on Pin generates expected Events`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val contextMenu = aContextMenuShown(isDm = false, isFavorite = false)
        val callback = EnsureNeverCalledWithParam<RoomId>()
        setRoomListContextMenu(
            contextMenu = contextMenu,
            eventSink = eventsRecorder,
            onRoomSettingsClick = callback,
        )
        clickOn(CommonStrings.action_pin)
        eventsRecorder.assertList(
            listOf(
                RoomListEvent.HideContextMenu,
                RoomListEvent.SetRoomIsFavorite(contextMenu.roomId, true),
            )
        )
    }

    @Test
    fun `clicking on Unpin generates expected Events`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val contextMenu = aContextMenuShown(isFavorite = true)
        setRoomListContextMenu(
            contextMenu = contextMenu,
            eventSink = eventsRecorder,
        )
        clickOn(CommonStrings.action_unpin)
        eventsRecorder.assertList(
            listOf(
                RoomListEvent.HideContextMenu,
                RoomListEvent.SetRoomIsFavorite(contextMenu.roomId, false),
            )
        )
    }

    @Test
    fun `clicking on Archive generates expected Events`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val contextMenu = aContextMenuShown(isArchived = false)
        setRoomListContextMenu(
            contextMenu = contextMenu,
            eventSink = eventsRecorder,
        )
        clickOn(R.string.action_archive)
        eventsRecorder.assertList(
            listOf(
                RoomListEvent.HideContextMenu,
                RoomListEvent.SetRoomIsArchived(contextMenu.roomId, true),
            )
        )
    }

    @Test
    fun `clicking on Unarchive generates expected Events`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val contextMenu = aContextMenuShown(isArchived = true)
        setRoomListContextMenu(
            contextMenu = contextMenu,
            eventSink = eventsRecorder,
        )
        clickOn(R.string.action_unarchive)
        eventsRecorder.assertList(
            listOf(
                RoomListEvent.HideContextMenu,
                RoomListEvent.SetRoomIsArchived(contextMenu.roomId, false),
            )
        )
    }

    @Test
    fun `clicking on Mute for 8 hours generates expected Events`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val contextMenu = aContextMenuShown(isMuted = false)
        setRoomListContextMenu(
            contextMenu = contextMenu,
            eventSink = eventsRecorder,
        )
        clickOn(R.string.action_mute_for_8_hours)
        eventsRecorder.assertList(
            listOf(
                RoomListEvent.HideContextMenu,
                RoomListEvent.SetRoomMuteDuration(contextMenu.roomId, MomentHomeMuteDuration.Hours8),
            )
        )
    }

    @Test
    fun `clicking on Mute for 1 week generates expected Events`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val contextMenu = aContextMenuShown(isMuted = false)
        setRoomListContextMenu(
            contextMenu = contextMenu,
            eventSink = eventsRecorder,
        )
        clickOn(R.string.action_mute_for_1_week)
        eventsRecorder.assertList(
            listOf(
                RoomListEvent.HideContextMenu,
                RoomListEvent.SetRoomMuteDuration(contextMenu.roomId, MomentHomeMuteDuration.OneWeek),
            )
        )
    }

    @Test
    fun `clicking on Mute forever generates expected Events`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val contextMenu = aContextMenuShown(isMuted = false)
        setRoomListContextMenu(
            contextMenu = contextMenu,
            eventSink = eventsRecorder,
        )
        clickOn(R.string.action_mute_forever)
        eventsRecorder.assertList(
            listOf(
                RoomListEvent.HideContextMenu,
                RoomListEvent.SetRoomMuteDuration(contextMenu.roomId, MomentHomeMuteDuration.Forever),
            )
        )
    }

    @Test
    fun `clicking on Unmute generates expected Events`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val contextMenu = aContextMenuShown(isMuted = true)
        setRoomListContextMenu(
            contextMenu = contextMenu,
            eventSink = eventsRecorder,
        )
        clickOn(CommonStrings.common_unmute)
        eventsRecorder.assertList(
            listOf(
                RoomListEvent.HideContextMenu,
                RoomListEvent.UnmuteRoom(contextMenu.roomId),
            )
        )
    }

    @Test
    fun `clicking on Block user generates expected Events`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val contextMenu = aContextMenuShown(
            isDm = true,
            directUserId = A_USER_ID,
            directUserDisplayName = "Alice",
            isDirectUserBlocked = false,
        )
        setRoomListContextMenu(
            contextMenu = contextMenu,
            eventSink = eventsRecorder,
        )
        scrollToAndClickOn(R.string.screen_home_direct_user_block_user)
        eventsRecorder.assertList(
            listOf(
                RoomListEvent.HideContextMenu,
                RoomListEvent.ShowDirectUserBlockConfirmation(A_USER_ID, "Alice", blocked = true),
            )
        )
    }

    @Test
    fun `clicking on Unblock user generates expected Events`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomListEvent>()
        val contextMenu = aContextMenuShown(
            isDm = true,
            directUserId = A_USER_ID,
            directUserDisplayName = "Alice",
            isDirectUserBlocked = true,
        )
        setRoomListContextMenu(
            contextMenu = contextMenu,
            eventSink = eventsRecorder,
        )
        scrollToAndClickOn(R.string.screen_home_direct_user_unblock_user)
        eventsRecorder.assertList(
            listOf(
                RoomListEvent.HideContextMenu,
                RoomListEvent.ShowDirectUserBlockConfirmation(A_USER_ID, "Alice", blocked = false),
            )
        )
    }

    private fun AndroidComposeUiTest<ComponentActivity>.setRoomListContextMenu(
        contextMenu: RoomListState.ContextMenu.Shown,
        canReportRoom: Boolean = false,
        eventSink: (RoomListEvent) -> Unit,
        onRoomSettingsClick: (RoomId) -> Unit = EnsureNeverCalledWithParam(),
        onReportRoomClick: (RoomId) -> Unit = EnsureNeverCalledWithParam(),
    ) {
        setSafeContent {
            RoomListContextMenu(
                contextMenu = contextMenu,
                canReportRoom = canReportRoom,
                onRoomSettingsClick = onRoomSettingsClick,
                onReportRoomClick = onReportRoomClick,
                eventSink = eventSink,
            )
        }
    }

    private fun AndroidComposeUiTest<ComponentActivity>.scrollToAndClickOn(@androidx.annotation.StringRes res: Int) {
        val text = activity!!.getString(res)
        onNode(hasText(text) and hasClickAction())
            .performScrollTo()
            .performClick()
    }
}
