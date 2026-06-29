/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user.username

import io.element.android.libraries.matrix.api.core.UserId

data class MomentUsernameState(
    val userId: UserId,
    val displayName: String,
    val username: String,
    val currentUsername: String,
    val isSaving: Boolean,
    val canSave: Boolean,
    val usernameError: MomentUsernameError?,
    val eventSink: (MomentUsernameEvent) -> Unit,
)

enum class MomentUsernameError {
    Required,
    TooShort,
    TooLong,
    Invalid,
    Taken,
    Unsupported,
    SaveFailed,
}
