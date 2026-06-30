/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.roomdetails.impl.members

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.element.android.features.roomdetails.impl.MomentRoomDetailsType
import io.element.android.features.roomdetails.impl.R
import io.element.android.libraries.architecture.AsyncData
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMemberListViewTest {
    @Test
    fun `banned empty state uses banned copy without search subtitle`() = runAndroidComposeUiTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        setRoomMemberListView(
            aRoomMemberListState(
                roomMembers = AsyncData.Success(emptyRoomMembers()),
                moderationState = aRoomMemberModerationState(canBan = true),
                selectedSection = SelectedSection.BANNED,
            )
        )

        onNodeWithText(context.getString(R.string.screen_room_member_list_banned_empty))
            .assertIsDisplayed()
        onNodeWithText(context.getString(R.string.screen_room_member_list_empty_search_subtitle))
            .assertDoesNotExist()
    }

    @Test
    fun `search empty state uses query title and helper subtitle`() = runAndroidComposeUiTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val query = "Dana"
        setRoomMemberListView(
            aRoomMemberListState(
                roomMembers = AsyncData.Success(
                    RoomMembers(
                        invited = persistentListOf(),
                        joined = persistentListOf(RoomMemberListMember(anAlice())),
                        banned = persistentListOf(),
                    )
                ),
                searchQuery = query,
                selectedSection = SelectedSection.MEMBERS,
            )
        )

        onNodeWithText(context.getString(R.string.screen_room_member_list_empty_search_title, query))
            .assertIsDisplayed()
        onNodeWithText(context.getString(R.string.screen_room_member_list_empty_search_subtitle))
            .assertIsDisplayed()
    }

    @Test
    fun `banned member row hides profile display name`() = runAndroidComposeUiTest {
        setRoomMemberListView(
            aRoomMemberListState(
                roomMembers = AsyncData.Success(
                    RoomMembers(
                        invited = persistentListOf(),
                        joined = persistentListOf(),
                        banned = persistentListOf(RoomMemberListMember(aBannedMallory())),
                    )
                ),
                moderationState = aRoomMemberModerationState(canBan = true),
                selectedSection = SelectedSection.BANNED,
            )
        )

        onNodeWithText("@mallory:server.org").assertIsDisplayed()
        onNodeWithText("Mallory").assertDoesNotExist()
    }

    @Test
    fun `Moment channel member list uses subscribers title`() = runAndroidComposeUiTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        setRoomMemberListView(
            aRoomMemberListState(
                roomMembers = AsyncData.Success(emptyRoomMembers()),
                momentRoomType = MomentRoomDetailsType.Channel,
            )
        )

        onNodeWithText(context.getString(R.string.screen_moment_room_profile_subscribers_title_android))
            .assertIsDisplayed()
    }
}

private fun AndroidComposeUiTest<ComponentActivity>.setRoomMemberListView(
    state: RoomMemberListState,
) {
    setContent {
        RoomMemberListView(
            state = state,
            navigator = object : RoomMemberListNavigator {},
        )
    }
}

private fun emptyRoomMembers(): RoomMembers {
    return RoomMembers(
        invited = persistentListOf(),
        joined = persistentListOf(),
        banned = persistentListOf(),
    )
}
