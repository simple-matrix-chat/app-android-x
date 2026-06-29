/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user.editprofile

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.designsystem.preview.USER_NAME_JOHN_DOE
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.ui.media.AvatarAction
import io.element.android.libraries.permissions.api.PermissionsState
import io.element.android.libraries.permissions.api.aPermissionsState
import kotlinx.collections.immutable.toImmutableList

open class EditUserProfileStateProvider : PreviewParameterProvider<EditUserProfileState> {
    override val values: Sequence<EditUserProfileState>
        get() = sequenceOf(
            aEditUserProfileState(),
            aEditUserProfileState(username = "john_doe", status = "Available"),
            aEditUserProfileState(username = "jd", usernameError = EditUserProfileUsernameError.TooShort),
            aEditUserProfileState(userAvatarUrl = "example://uri"),
            aEditUserProfileState(saveAction = AsyncAction.ConfirmingCancellation),
            aEditUserProfileState(canChangeAvatarUrl = false, canChangeDisplayName = false),
        )
}

fun aEditUserProfileState(
    userId: UserId = UserId("@john.doe:matrix.org"),
    displayName: String = USER_NAME_JOHN_DOE,
    username: String = "",
    phoneNumber: String = "",
    status: String = "",
    profileShareText: String? = "John Doe\nhttps://unmoment.app/l/profile",
    userAvatarUrl: String? = null,
    avatarActions: List<AvatarAction> = emptyList(),
    saveButtonEnabled: Boolean = true,
    saveAction: AsyncAction<Unit> = AsyncAction.Uninitialized,
    isLoadingProfileStatus: Boolean = false,
    isLoadingProfileUsername: Boolean = false,
    isLoadingMomentProfile: Boolean = false,
    showProfileUsername: Boolean = true,
    usernameError: EditUserProfileUsernameError? = null,
    cameraPermissionState: PermissionsState = aPermissionsState(showDialog = false),
    canChangeDisplayName: Boolean = true,
    canChangeAvatarUrl: Boolean = true,
    eventSink: (EditUserProfileEvent) -> Unit = {},
) = EditUserProfileState(
    userId = userId,
    displayName = displayName,
    username = username,
    phoneNumber = phoneNumber,
    status = status,
    profileShareText = profileShareText,
    userAvatarUrl = userAvatarUrl,
    avatarActions = avatarActions.toImmutableList(),
    saveButtonEnabled = saveButtonEnabled,
    saveAction = saveAction,
    isLoadingProfileStatus = isLoadingProfileStatus,
    isLoadingProfileUsername = isLoadingProfileUsername,
    isLoadingMomentProfile = isLoadingMomentProfile,
    showProfileUsername = showProfileUsername,
    usernameError = usernameError,
    cameraPermissionState = cameraPermissionState,
    canChangeDisplayName = canChangeDisplayName,
    canChangeAvatarUrl = canChangeAvatarUrl,
    eventSink = eventSink,
)
