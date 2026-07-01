/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.roomdetails.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.element.android.features.roomdetails.impl.members.aRoomMember
import io.element.android.features.userprofile.shared.aUserProfileState
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.notification.CallIntent
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.matrix.test.A_USER_ID
import io.element.android.libraries.testtags.TestTags
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.tests.testutils.EnsureCalledOnceWithTwoParams
import io.element.android.tests.testutils.EnsureNeverCalled
import io.element.android.tests.testutils.EnsureNeverCalledWithParam
import io.element.android.tests.testutils.EnsureNeverCalledWithTwoParams
import io.element.android.tests.testutils.EventsRecorder
import io.element.android.tests.testutils.clickOn
import io.element.android.tests.testutils.ensureCalledOnce
import io.element.android.tests.testutils.ensureCalledOnceWithParam
import io.element.android.tests.testutils.pressBack
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import io.element.android.features.leaveroom.api.R as LeaveRoomR

@RunWith(AndroidJUnit4::class)
class RoomDetailsViewTest {
    @Test
    fun `click on back invokes expected callback`() = runAndroidComposeUiTest {
        ensureCalledOnce { callback ->
            setRoomDetailView(
                goBack = callback,
            )
            pressBack()
        }
    }

    @Test
    fun `click on share invokes expected callback`() = runAndroidComposeUiTest {
        ensureCalledOnce { callback ->
            setRoomDetailView(
                onShareRoom = callback,
            )
            clickOn(CommonStrings.action_share)
        }
    }

    @Config(qualifiers = "h1024dp")
    @Test
    fun `click on room members invokes expected callback`() = runAndroidComposeUiTest {
        ensureCalledOnce { callback ->
            setRoomDetailView(
                openRoomMemberList = callback,
            )
            clickOn(CommonStrings.common_people)
        }
    }

    @Config(qualifiers = "h1024dp")
    @Test
    fun `click on polls invokes expected callback`() = runAndroidComposeUiTest {
        ensureCalledOnce { callback ->
            setRoomDetailView(
                openPollHistory = callback,
            )
            clickOn(R.string.screen_polls_history_title)
        }
    }

    @Config(qualifiers = "h1024dp")
    @Test
    fun `click on media gallery invokes expected callback`() = runAndroidComposeUiTest {
        ensureCalledOnce { callback ->
            setRoomDetailView(
                openMediaGallery = callback,
            )
            clickOn(R.string.screen_room_details_media_gallery_title)
        }
    }

    @Config(qualifiers = "h1024dp")
    @Test
    fun `click on notification invokes expected callback`() = runAndroidComposeUiTest {
        ensureCalledOnce { callback ->
            setRoomDetailView(
                openRoomNotificationSettings = callback,
            )
            clickOn(R.string.screen_room_details_notification_title)
        }
    }

    @Test
    fun `click on invite invokes expected callback`() = runAndroidComposeUiTest {
        ensureCalledOnce { callback ->
            setRoomDetailView(
                state = aRoomDetailsState(
                    eventSink = EventsRecorder(expectEvents = false),
                    canInvite = true,
                ),
                invitePeople = callback,
            )
            clickOn(CommonStrings.action_invite)
        }
    }

    @Test
    fun `click on call invokes expected callback`() = runAndroidComposeUiTest {
        ensureCalledOnceWithParam(CallIntent.AUDIO) { callback ->
            setRoomDetailView(
                state = aRoomDetailsState(
                    eventSink = EventsRecorder(expectEvents = false),
                    canInvite = true,
                    roomType = RoomDetailsType.Dm(aRoomMember(UserId("@other:local.org"))),
                ),
                onJoinCallClick = callback,
            )
            clickOn(CommonStrings.action_call)
        }
    }

    @Test
    fun `click on video call invokes expected callback`() = runAndroidComposeUiTest {
        ensureCalledOnceWithParam(CallIntent.VIDEO) { callback ->
            setRoomDetailView(
                state = aRoomDetailsState(
                    eventSink = EventsRecorder(expectEvents = false),
                    canInvite = true,
                ),
                onJoinCallClick = callback,
            )
            clickOn(CommonStrings.common_video)
        }
    }

    @Config(qualifiers = "h1024dp")
    @Test
    fun `click on pinned messages invokes expected callback`() = runAndroidComposeUiTest {
        ensureCalledOnce { callback ->
            setRoomDetailView(
                state = aRoomDetailsState(
                    eventSink = EventsRecorder(expectEvents = false),
                    canInvite = true,
                ),
                onPinnedMessagesClick = callback,
            )
            clickOn(R.string.screen_room_details_pinned_events_row_title)
        }
    }

    @Config(qualifiers = "h1024dp")
    @Test
    fun `click on add topic emit expected event`() = runAndroidComposeUiTest {
        ensureCalledOnceWithParam<RoomDetailsAction>(RoomDetailsAction.AddTopic) { callback ->
            setRoomDetailView(
                state = aRoomDetailsState(
                    eventSink = EventsRecorder(expectEvents = false),
                    roomTopic = RoomTopicState.CanAddTopic,
                ),
                onActionClick = callback,
            )
            clickOn(R.string.screen_room_details_add_topic_title)
        }
    }

    @Test
    fun `click on menu edit emit expected event`() = runAndroidComposeUiTest {
        ensureCalledOnceWithParam<RoomDetailsAction>(RoomDetailsAction.Edit) { callback ->
            setRoomDetailView(
                state = aRoomDetailsState(
                    eventSink = EventsRecorder(expectEvents = false),
                    canEdit = true,
                ),
                onActionClick = callback,
            )
            val menuContentDescription = activity!!.getString(CommonStrings.a11y_user_menu)
            onNodeWithContentDescription(menuContentDescription).performClick()
            clickOn(CommonStrings.action_edit)
        }
    }

    @Config(qualifiers = "h1024dp")
    @Test
    fun `click on Moment channel settings emits edit action`() = runAndroidComposeUiTest {
        ensureCalledOnceWithParam<RoomDetailsAction>(RoomDetailsAction.Edit) { callback ->
            setRoomDetailView(
                state = aRoomDetailsState(
                    eventSink = EventsRecorder(expectEvents = false),
                    momentRoomType = MomentRoomDetailsType.Channel,
                ),
                onActionClick = callback,
            )
            clickOn(R.string.screen_moment_room_profile_settings_title_channel_android)
        }
    }

    @Config(qualifiers = "h1024dp")
    @Test
    fun `click on Moment public link invokes share callback`() = runAndroidComposeUiTest {
        ensureCalledOnce { callback ->
            setRoomDetailView(
                state = aRoomDetailsState(
                    eventSink = EventsRecorder(expectEvents = false),
                    momentRoomType = MomentRoomDetailsType.Channel,
                    isPublic = true,
                ),
                onShareRoom = callback,
            )
            clickOn(R.string.screen_moment_room_profile_public_link_title_android)
        }
    }

    @Config(qualifiers = "h1024dp")
    @Test
    fun `click on Moment channel subscribers invokes member list callback`() = runAndroidComposeUiTest {
        ensureCalledOnce { callback ->
            setRoomDetailView(
                state = aRoomDetailsState(
                    eventSink = EventsRecorder(expectEvents = false),
                    momentRoomType = MomentRoomDetailsType.Channel,
                ),
                openRoomMemberList = callback,
            )
            clickOn(R.string.screen_moment_room_profile_subscribers_title_android)
        }
    }

    @Config(qualifiers = "h1500dp")
    @Test
    fun `Moment channel keeps call shortcut but hides generic shortcuts`() = runAndroidComposeUiTest {
        setRoomDetailView(
            state = aRoomDetailsState(
                eventSink = EventsRecorder(expectEvents = false),
                momentRoomType = MomentRoomDetailsType.Channel,
                canInvite = true,
            ),
        )

        onNodeWithText(activity!!.getString(CommonStrings.common_video)).assertExists()
        onNodeWithText("Share").assertDoesNotExist()
        onNodeWithText("Invite").assertDoesNotExist()
        onNodeWithText("Mute").assertDoesNotExist()
        onNodeWithText("Unmute").assertDoesNotExist()
    }

    @Test
    fun `click on avatar test`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomDetailsEvent>(expectEvents = false)
        val state = aRoomDetailsState(
            eventSink = eventsRecorder,
            roomAvatarUrl = "an_avatar_url",
        )
        val callback = EnsureCalledOnceWithTwoParams(state.roomName, "an_avatar_url")
        setRoomDetailView(
            state = state,
            openAvatarPreview = callback,
        )
        onNodeWithTag(TestTags.roomDetailAvatar.value).performClick()
        callback.assertSuccess()
    }

    @Test
    fun `click on avatar test on DM`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomDetailsEvent>(expectEvents = false)
        val state = aRoomDetailsState(
            roomType = RoomDetailsType.Dm(aDmRoomMember(avatarUrl = "an_avatar_url"),),
            roomName = "Daniel",
            eventSink = eventsRecorder,
        )
        val callback = EnsureCalledOnceWithTwoParams("Daniel", "an_avatar_url")
        setRoomDetailView(
            state = state,
            openAvatarPreview = callback,
        )
        onNodeWithTag(TestTags.roomDetailAvatar.value).performClick()
        callback.assertSuccess()
    }

    @Test
    fun `click on mute emit expected event`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomDetailsEvent>()
        val state = aRoomDetailsState(
            eventSink = eventsRecorder,
            roomNotificationSettings = aRoomNotificationSettings(mode = RoomNotificationMode.ALL_MESSAGES),
        )
        setRoomDetailView(
            state = state,
        )
        clickOn(CommonStrings.common_mute)
        eventsRecorder.assertSingle(RoomDetailsEvent.MuteNotification)
    }

    @Test
    fun `click on unmute emit expected event`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomDetailsEvent>()
        val state = aRoomDetailsState(
            eventSink = eventsRecorder,
            roomNotificationSettings = aRoomNotificationSettings(mode = RoomNotificationMode.MUTE),
        )
        setRoomDetailView(
            state = state,
        )
        clickOn(CommonStrings.common_unmute)
        eventsRecorder.assertSingle(RoomDetailsEvent.UnmuteNotification)
    }

    @Config(qualifiers = "h1024dp")
    @Test
    fun `click on favorite emit expected Event`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomDetailsEvent>()
        setRoomDetailView(
            state = aRoomDetailsState(
                eventSink = eventsRecorder,
            ),
        )
        clickOn(CommonStrings.common_favourite)
        eventsRecorder.assertSingle(RoomDetailsEvent.SetFavorite(true))
    }

    @Config(qualifiers = "h1500dp")
    @Test
    fun `click on leave emit expected Event`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomDetailsEvent>()
        setRoomDetailView(
            state = aRoomDetailsState(
                eventSink = eventsRecorder,
            ),
        )
        clickOn(R.string.screen_room_details_leave_room_title)
        eventsRecorder.assertSingle(RoomDetailsEvent.LeaveRoom(needsConfirmation = true))
    }

    @Config(qualifiers = "h1500dp")
    @Test
    fun `click on Moment channel leave emits expected Event`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomDetailsEvent>()
        setRoomDetailView(
            state = aRoomDetailsState(
                eventSink = eventsRecorder,
                momentRoomType = MomentRoomDetailsType.Channel,
            ),
        )
        clickOn(R.string.screen_moment_room_profile_leave_channel_android)
        eventsRecorder.assertSingle(RoomDetailsEvent.LeaveRoom(needsConfirmation = true))
    }

    @Config(qualifiers = "h1500dp")
    @Test
    fun `click on DM delete chat emits expected Event`() = runAndroidComposeUiTest {
        val eventsRecorder = EventsRecorder<RoomDetailsEvent>()
        setRoomDetailView(
            state = aDmRoomDetailsState(
                roomName = "Alice",
                eventSink = eventsRecorder,
            ),
        )
        clickOn(LeaveRoomR.string.action_delete_chat)
        eventsRecorder.assertSingle(RoomDetailsEvent.LeaveRoom(needsConfirmation = true))
    }

    @Config(qualifiers = "h1500dp")
    @Test
    fun `self direct room does not show leave or report actions`() = runAndroidComposeUiTest {
        setRoomDetailView(
            state = aRoomDetailsState(
                canLeaveRoom = false,
                canReportRoom = true,
                eventSink = EventsRecorder(expectEvents = false),
            ),
        )
        onNodeWithText(activity!!.getString(CommonStrings.action_leave_room)).assertDoesNotExist()
        onNodeWithText("Report room").assertDoesNotExist()
    }

    @Config(qualifiers = "h1500dp")
    @Test
    fun `click on report room  invokes expected callback`() = runAndroidComposeUiTest {
        ensureCalledOnce { callback ->
            setRoomDetailView(
                state = aRoomDetailsState(
                    eventSink = EventsRecorder(expectEvents = false),
                ),
                onReportRoomClick = callback,
            )
            clickOn(CommonStrings.action_report_room)
        }
    }

    @Config(qualifiers = "h1024dp")
    @Test
    fun `click on knock requests invokes expected callback`() = runAndroidComposeUiTest {
        ensureCalledOnce { callback ->
            setRoomDetailView(
                state = aRoomDetailsState(
                    eventSink = EventsRecorder(expectEvents = false),
                    canShowKnockRequests = true,
                ),
                onKnockRequestsClick = callback,
            )
            clickOn(R.string.screen_room_details_requests_to_join_title)
        }
    }

    @Config(qualifiers = "h1024dp")
    @Test
    fun `click on profile invokes the expected callback`() = runAndroidComposeUiTest {
        ensureCalledOnceWithParam(A_USER_ID) { callback ->
            setRoomDetailView(
                state = aRoomDetailsState(
                    eventSink = EventsRecorder(expectEvents = false),
                    roomMemberDetailsState = aUserProfileState(userId = A_USER_ID),
                ),
                onProfileClick = callback,
            )
            clickOn(R.string.screen_room_details_profile_row_title)
        }
    }

    @Config(qualifiers = "h1024dp")
    @Test
    fun `dm room details does not show invite row`() = runAndroidComposeUiTest {
        setRoomDetailView(
            state = aRoomDetailsState(
                eventSink = EventsRecorder(expectEvents = false),
                roomType = RoomDetailsType.Dm(
                    aDmRoomMember(userId = UserId("@other:local.org")),
                ),
                roomMemberDetailsState = aUserProfileState(userId = A_USER_ID),
                canInvite = true,
            ),
        )
        onNodeWithText(activity!!.getString(R.string.screen_room_details_invite_title)).assertDoesNotExist()
    }

    @Config(qualifiers = "h1024dp")
    @Test
    fun `click on security and privacy invokes the expected callback`() = runAndroidComposeUiTest {
        ensureCalledOnce { callback ->
            setRoomDetailView(
                state = aRoomDetailsState(displaySecurityAndPrivacySettings = true),
                openSecurityAndPrivacy = callback,
            )
            clickOn(R.string.screen_room_details_security_and_privacy_title)
        }
    }
}

private fun AndroidComposeUiTest<ComponentActivity>.setRoomDetailView(
    state: RoomDetailsState = aRoomDetailsState(
        eventSink = EventsRecorder(expectEvents = false),
    ),
    goBack: () -> Unit = EnsureNeverCalled(),
    onActionClick: (RoomDetailsAction) -> Unit = EnsureNeverCalledWithParam(),
    onShareRoom: () -> Unit = EnsureNeverCalled(),
    openRoomMemberList: () -> Unit = EnsureNeverCalled(),
    openRoomNotificationSettings: () -> Unit = EnsureNeverCalled(),
    invitePeople: () -> Unit = EnsureNeverCalled(),
    openAvatarPreview: (name: String, url: String) -> Unit = EnsureNeverCalledWithTwoParams(),
    openPollHistory: () -> Unit = EnsureNeverCalled(),
    openMediaGallery: () -> Unit = EnsureNeverCalled(),
    openAdminSettings: () -> Unit = EnsureNeverCalled(),
    openSecurityAndPrivacy: () -> Unit = EnsureNeverCalled(),
    onJoinCallClick: (CallIntent) -> Unit = EnsureNeverCalledWithParam(),
    onPinnedMessagesClick: () -> Unit = EnsureNeverCalled(),
    onKnockRequestsClick: () -> Unit = EnsureNeverCalled(),
    onProfileClick: (UserId) -> Unit = EnsureNeverCalledWithParam(),
    onReportRoomClick: () -> Unit = EnsureNeverCalled(),
) {
    setContent {
        RoomDetailsView(
            state = state,
            goBack = goBack,
            onActionClick = onActionClick,
            onShareRoom = onShareRoom,
            openRoomMemberList = openRoomMemberList,
            openRoomNotificationSettings = openRoomNotificationSettings,
            invitePeople = invitePeople,
            openAvatarPreview = openAvatarPreview,
            openPollHistory = openPollHistory,
            openMediaGallery = openMediaGallery,
            openAdminSettings = openAdminSettings,
            openSecurityAndPrivacy = openSecurityAndPrivacy,
            onJoinCallClick = onJoinCallClick,
            onPinnedMessagesClick = onPinnedMessagesClick,
            onKnockRequestsClick = onKnockRequestsClick,
            onProfileClick = onProfileClick,
            onReportRoomClick = onReportRoomClick,
            leaveRoomView = {},
        )
    }
}
