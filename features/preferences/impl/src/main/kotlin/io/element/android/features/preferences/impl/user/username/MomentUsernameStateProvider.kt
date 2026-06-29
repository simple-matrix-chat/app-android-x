/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user.username

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.libraries.matrix.api.core.UserId

open class MomentUsernameStateProvider : PreviewParameterProvider<MomentUsernameState> {
    override val values: Sequence<MomentUsernameState>
        get() = sequenceOf(
            aMomentUsernameState(username = "moment_user", currentUsername = "moment_user"),
            aMomentUsernameState(username = "jo", usernameError = MomentUsernameError.TooShort),
            aMomentUsernameState(username = "new_username", currentUsername = "moment_user", canSave = true),
            aMomentUsernameState(username = "new_username", currentUsername = "moment_user", isSaving = true),
        )
}

fun aMomentUsernameState(
    userId: UserId = UserId("@alice:matrix.org"),
    displayName: String = "Alice",
    username: String = "",
    currentUsername: String = "",
    isSaving: Boolean = false,
    canSave: Boolean = false,
    usernameError: MomentUsernameError? = null,
    eventSink: (MomentUsernameEvent) -> Unit = {},
) = MomentUsernameState(
    userId = userId,
    displayName = displayName,
    username = username,
    currentUsername = currentUsername,
    isSaving = isSaving,
    canSave = canSave && !isSaving,
    usernameError = usernameError,
    eventSink = eventSink,
)
