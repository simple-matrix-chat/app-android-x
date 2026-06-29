/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.user

import java.util.Locale

sealed class MatrixProfileUsernameException(message: String) : Exception(message) {
    data object Required : MatrixProfileUsernameException("Profile username is required")
    data object TooShort : MatrixProfileUsernameException("Profile username is too short")
    data object TooLong : MatrixProfileUsernameException("Profile username is too long")
    data object Invalid : MatrixProfileUsernameException("Profile username is invalid")
    data object Taken : MatrixProfileUsernameException("Profile username is already taken")
    data object Unsupported : MatrixProfileUsernameException("Profile username is not supported by this homeserver")
    data object MissingUserId : MatrixProfileUsernameException("Missing user id")
    data object SaveFailed : MatrixProfileUsernameException("Failed to save profile username")
    data class HttpError(val statusCode: Int) : MatrixProfileUsernameException("Profile username request failed with HTTP $statusCode")
}

object MatrixProfileUsername {
    private const val MIN_LENGTH = 3
    private const val MAX_LENGTH = 32
    private val usernameRegex = Regex("^[a-z0-9_]+$")

    fun normalize(value: String): String {
        return value.trim()
            .lowercase(Locale.ROOT)
            .removePrefix("@")
    }

    fun validationError(value: String): MatrixProfileUsernameException? {
        val normalized = normalize(value)
        return when {
            normalized.isEmpty() -> MatrixProfileUsernameException.Required
            normalized.length < MIN_LENGTH -> MatrixProfileUsernameException.TooShort
            normalized.length > MAX_LENGTH -> MatrixProfileUsernameException.TooLong
            !usernameRegex.matches(normalized) -> MatrixProfileUsernameException.Invalid
            else -> null
        }
    }
}
