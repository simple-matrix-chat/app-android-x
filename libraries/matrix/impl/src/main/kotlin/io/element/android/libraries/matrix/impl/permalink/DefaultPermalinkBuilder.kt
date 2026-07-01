/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.permalink

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.appconfig.MatrixConfiguration
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.matrix.api.core.MatrixPatterns
import io.element.android.libraries.matrix.api.core.RoomAlias
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.permalink.PermalinkBuilder
import io.element.android.libraries.matrix.api.permalink.PermalinkBuilderError
import org.matrix.rustcomponents.sdk.matrixToRoomAliasPermalink
import org.matrix.rustcomponents.sdk.matrixToUserPermalink

@ContributesBinding(AppScope::class)
class DefaultPermalinkBuilder : PermalinkBuilder {
    override fun permalinkForUser(userId: UserId): Result<String> {
        if (!MatrixPatterns.isUserId(userId.value)) {
            return Result.failure(PermalinkBuilderError.InvalidData)
        }
        return runCatchingExceptions {
            clientPermalinkFor(userId.value) ?: matrixToUserPermalink(userId.value)
        }
    }

    override fun permalinkForRoomAlias(roomAlias: RoomAlias): Result<String> {
        if (!MatrixPatterns.isRoomAlias(roomAlias.value)) {
            return Result.failure(PermalinkBuilderError.InvalidData)
        }
        return runCatchingExceptions {
            clientPermalinkFor(roomAlias.value) ?: matrixToRoomAliasPermalink(roomAlias.value)
        }
    }

    private fun clientPermalinkFor(matrixId: String): String? {
        return MatrixConfiguration.clientPermalinkBaseUrl
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.trimEnd('/')
            ?.let { baseUrl -> "$baseUrl/${matrixId.trimStart('/')}" }
    }
}
