/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.user

import io.element.android.libraries.matrix.api.core.UserId

data class MatrixMomentUserSearchMatch(
    val userId: UserId,
    val displayName: String?,
    val avatarUrl: String?,
    val phoneNumber: String?,
) {
    val matrixUser: MatrixUser
        get() = MatrixUser(
            userId = userId,
            displayName = displayName,
            avatarUrl = avatarUrl,
        )
}
