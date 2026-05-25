/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roomdetails.impl.members.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import io.element.android.features.userprofile.api.UserProfileEvents
import io.element.android.features.userprofile.api.UserProfilePresenterFactory
import io.element.android.features.userprofile.api.UserProfileState
import io.element.android.libraries.androidutils.clipboard.ClipboardHelper
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.designsystem.utils.snackbar.LocalSnackbarDispatcher
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarMessage
import io.element.android.libraries.designsystem.utils.snackbar.collectSnackbarMessageAsState
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.JoinedRoom
import io.element.android.libraries.matrix.ui.room.getRoomMemberAsState
import io.element.android.libraries.ui.strings.CommonStrings

/**
 * Presenter for room member details screen.
 * Rely on UserProfilePresenter, but override some fields with room member info when available.
 */
@AssistedInject
class RoomMemberDetailsPresenter(
    @Assisted private val roomMemberId: UserId,
    private val room: JoinedRoom,
    private val clipboardHelper: ClipboardHelper,
    userProfilePresenterFactory: UserProfilePresenterFactory,
) : Presenter<UserProfileState> {
    interface Factory {
        fun create(roomMemberId: UserId): RoomMemberDetailsPresenter
    }

    private val userProfilePresenter = userProfilePresenterFactory.create(roomMemberId)

    @Composable
    override fun present(): UserProfileState {
        val snackbarDispatcher = LocalSnackbarDispatcher.current
        val snackbarMessage by snackbarDispatcher.collectSnackbarMessageAsState()
        val roomMember by room.getRoomMemberAsState(roomMemberId)
        LaunchedEffect(Unit) {
            // Update room member info when opening this screen
            // We don't need to assign the result as it will be automatically propagated by `room.getRoomMemberAsState`
            room.getUpdatedMember(roomMemberId)
        }

        val roomUserName: String? by produceState(
            initialValue = roomMember?.displayName,
            key1 = roomMember,
        ) {
            value = room.userDisplayName(roomMemberId).getOrNull() ?: roomMember?.displayName
        }

        val roomUserAvatar: String? by produceState(
            initialValue = roomMember?.avatarUrl,
            key1 = roomMember,
        ) {
            value = room.userAvatarUrl(roomMemberId).getOrNull() ?: roomMember?.avatarUrl
        }

        val userProfileState = userProfilePresenter.present()

        fun handleEvent(event: UserProfileEvents) {
            when (event) {
                UserProfileEvents.WithdrawVerification -> Unit
                is UserProfileEvents.CopyToClipboard -> {
                    clipboardHelper.copyPlainText(event.text)
                    snackbarDispatcher.post(SnackbarMessage(CommonStrings.common_copied_to_clipboard))
                }
                else -> userProfileState.eventSink(event)
            }
        }

        return userProfileState.copy(
            userName = roomUserName ?: userProfileState.userName,
            avatarUrl = roomUserAvatar ?: userProfileState.avatarUrl,
            snackbarMessage = snackbarMessage,
            eventSink = ::handleEvent,
        )
    }
}
