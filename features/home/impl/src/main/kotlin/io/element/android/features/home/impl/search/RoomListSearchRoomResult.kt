/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.search

import androidx.compose.runtime.Immutable
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class RoomListSearchRoomResult(
    val roomId: RoomId,
    val title: String,
    val description: String?,
    val avatarData: AvatarData,
    val heroes: ImmutableList<AvatarData>,
    val isTombstoned: Boolean,
)
