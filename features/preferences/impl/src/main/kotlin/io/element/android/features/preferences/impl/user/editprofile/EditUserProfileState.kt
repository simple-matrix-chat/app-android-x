/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user.editprofile

import io.element.android.libraries.architecture.AsyncAction
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.ui.media.AvatarAction
import io.element.android.libraries.permissions.api.PermissionsState
import kotlinx.collections.immutable.ImmutableList

data class EditUserProfileState(
    val userId: UserId,
    val displayName: String,
    val username: String,
    val phoneNumber: String,
    val status: String,
    val profileShareText: String?,
    val userAvatarUrl: String?,
    val avatarActions: ImmutableList<AvatarAction>,
    val saveButtonEnabled: Boolean,
    val saveAction: AsyncAction<Unit>,
    val isLoadingProfileStatus: Boolean,
    val isLoadingProfileUsername: Boolean,
    val isLoadingMomentProfile: Boolean,
    val showProfileUsername: Boolean,
    val usernameError: EditUserProfileUsernameError?,
    val cameraPermissionState: PermissionsState,
    val canChangeDisplayName: Boolean,
    val canChangeAvatarUrl: Boolean,
    val eventSink: (EditUserProfileEvent) -> Unit
)

enum class EditUserProfileUsernameError {
    Required,
    TooShort,
    TooLong,
    Invalid,
    Taken,
    Unsupported,
    SaveFailed,
}
