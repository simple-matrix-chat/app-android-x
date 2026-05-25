/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roomdetails.impl

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.room.history.RoomHistoryVisibility
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class RoomDetailsStateTest {
    @Test
    fun `room not public not encrypted should not have badges`() {
        val sut = aRoomDetailsState(
            isPublic = false,
            isEncrypted = false,
        )
        assertThat(sut.roomBadges).isEqualTo(
            persistentListOf<RoomBadge>()
        )
    }

    @Test
    fun `room public not encrypted should only have public badge`() {
        val sut = aRoomDetailsState(
            isPublic = true,
            isEncrypted = false,
        )
        assertThat(sut.roomBadges).isEqualTo(
            persistentListOf(RoomBadge.PUBLIC)
        )
    }

    @Test
    fun `room public encrypted should only have public badge`() {
        val sut = aRoomDetailsState(
            isPublic = true,
            isEncrypted = true,
        )
        assertThat(sut.roomBadges).isEqualTo(
            persistentListOf(RoomBadge.PUBLIC)
        )
    }

    @Test
    fun `room not public encrypted should not have badges`() {
        val sut = aRoomDetailsState(
            isPublic = false,
            isEncrypted = true,
        )
        assertThat(sut.roomBadges).isEqualTo(
            persistentListOf<RoomBadge>()
        )
    }

    @Test
    fun `room public not encrypted should ignore history sharing badges`() {
        val sut = aRoomDetailsState(
            isEncrypted = false,
            roomHistoryVisibility = RoomHistoryVisibility.Shared
        )
        assertThat(sut.roomBadges).isEqualTo(
            persistentListOf(RoomBadge.PUBLIC)
        )
    }

    @Test
    fun `room public encrypted should ignore hidden history sharing badge`() {
        val sut = aRoomDetailsState(
            isEncrypted = true,
            roomHistoryVisibility = RoomHistoryVisibility.Joined
        )
        assertThat(sut.roomBadges).isEqualTo(
            persistentListOf(RoomBadge.PUBLIC)
        )
    }

    @Test
    fun `room public encrypted should ignore world readable history sharing badge`() {
        val sut = aRoomDetailsState(
            isEncrypted = true,
            roomHistoryVisibility = RoomHistoryVisibility.WorldReadable
        )
        assertThat(sut.roomBadges).isEqualTo(
            persistentListOf(RoomBadge.PUBLIC)
        )
    }
}
