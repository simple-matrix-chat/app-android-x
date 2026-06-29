/*
 * Copyright (c) 2026 Element Creations Ltd.
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.user

import io.element.android.libraries.matrix.api.core.UserId
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class MatrixPublicProfile(
    val userId: UserId,
    val displayName: String?,
    val username: String?,
    val phoneNumber: String?,
)

sealed class MatrixPublicProfileException(message: String) : Exception(message) {
    data object MissingUserId : MatrixPublicProfileException("Missing user id")
    data class HttpError(val statusCode: Int) : MatrixPublicProfileException("Public profile request failed with HTTP $statusCode")
}

object MatrixProfileLink {
    fun fallbackUserLink(userId: UserId): String? {
        val trimmedUserId = userId.value.trim()
        if (trimmedUserId.isEmpty()) return null
        return "moment://profile?user=${trimmedUserId.urlEncodedQueryValue()}"
    }
}

private fun String.urlEncodedQueryValue(): String {
    return URLEncoder.encode(this, StandardCharsets.UTF_8.toString()).replace("+", "%20")
}
