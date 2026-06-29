/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.filters

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.notificationsettings.FakeNotificationSettingsService
import io.element.android.libraries.matrix.test.room.FakeBaseRoom
import io.element.android.libraries.matrix.test.room.aRoomInfo
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Test

class DefaultMomentMutedChatsStoreTest {
    @Test
    fun `muteRoom with finite duration stores expiry and mutes room`() = runTest {
        val notificationSettingsService = FakeNotificationSettingsService()
        val client = FakeMatrixClient(notificationSettingsService = notificationSettingsService)
        val store = DefaultMomentMutedChatsStore(client, backgroundScope)
        advanceUntilIdle()

        val result = store.muteRoom(A_ROOM_ID, MomentHomeMuteDuration.Hours8, isEncrypted = false, isOneToOne = false)

        assertThat(result.isSuccess).isTrue()
        assertThat(store.finiteMutedRoomIds.value).containsExactly(A_ROOM_ID)
        assertThat(notificationSettingsService.getRoomNotificationSettings(A_ROOM_ID, isEncrypted = false, isOneToOne = false).getOrThrow().mode)
            .isEqualTo(RoomNotificationMode.MUTE)
        val expiryMillis = client.getAccountData(MOMENT_MUTED_CHATS_ACCOUNT_DATA_TYPE)
            .getOrThrow()
            .expiryFor(A_ROOM_ID)
        assertThat(expiryMillis).isGreaterThan(System.currentTimeMillis())
    }

    @Test
    fun `muteRoom forever clears finite expiry and keeps room muted`() = runTest {
        val notificationSettingsService = FakeNotificationSettingsService()
        val client = FakeMatrixClient(notificationSettingsService = notificationSettingsService).apply {
            givenAccountData(MOMENT_MUTED_CHATS_ACCOUNT_DATA_TYPE, """{"${A_ROOM_ID.value}":1}""")
        }
        val store = DefaultMomentMutedChatsStore(client, backgroundScope)
        advanceUntilIdle()

        val result = store.muteRoom(A_ROOM_ID, MomentHomeMuteDuration.Forever, isEncrypted = false, isOneToOne = false)

        assertThat(result.isSuccess).isTrue()
        assertThat(store.finiteMutedRoomIds.value).isEmpty()
        assertThat(client.getAccountData(MOMENT_MUTED_CHATS_ACCOUNT_DATA_TYPE).getOrThrow()).isEqualTo("{}")
        assertThat(notificationSettingsService.getRoomNotificationSettings(A_ROOM_ID, isEncrypted = false, isOneToOne = false).getOrThrow().mode)
            .isEqualTo(RoomNotificationMode.MUTE)
    }

    @Test
    fun `unmuteRoom clears finite expiry and restores default notification settings`() = runTest {
        val notificationSettingsService = FakeNotificationSettingsService()
        val client = FakeMatrixClient(notificationSettingsService = notificationSettingsService)
        val store = DefaultMomentMutedChatsStore(client, backgroundScope)
        advanceUntilIdle()

        store.muteRoom(A_ROOM_ID, MomentHomeMuteDuration.Hours8, isEncrypted = false, isOneToOne = false)
        val result = store.unmuteRoom(A_ROOM_ID, isEncrypted = false, isOneToOne = false)

        assertThat(result.isSuccess).isTrue()
        assertThat(store.finiteMutedRoomIds.value).isEmpty()
        assertThat(client.getAccountData(MOMENT_MUTED_CHATS_ACCOUNT_DATA_TYPE).getOrThrow()).isEqualTo("{}")
        assertThat(notificationSettingsService.getRoomNotificationSettings(A_ROOM_ID, isEncrypted = false, isOneToOne = false).getOrThrow().isDefault)
            .isTrue()
    }

    @Test
    fun `syncExpiredMutedChats unmutes expired finite rooms`() = runTest {
        val notificationSettingsService = FakeNotificationSettingsService()
        val client = FakeMatrixClient(notificationSettingsService = notificationSettingsService).apply {
            givenGetRoomResult(
                A_ROOM_ID,
                FakeBaseRoom(initialRoomInfo = aRoomInfo(isEncrypted = false, isDm = true)),
            )
        }
        val store = DefaultMomentMutedChatsStore(client, backgroundScope)
        advanceUntilIdle()

        store.muteRoom(A_ROOM_ID, MomentHomeMuteDuration.Hours8, isEncrypted = false, isOneToOne = true)
        store.syncExpiredMutedChats(nowMillis = Long.MAX_VALUE)

        assertThat(store.finiteMutedRoomIds.value).isEmpty()
        assertThat(client.getAccountData(MOMENT_MUTED_CHATS_ACCOUNT_DATA_TYPE).getOrThrow()).isEqualTo("{}")
        assertThat(notificationSettingsService.getRoomNotificationSettings(A_ROOM_ID, isEncrypted = false, isOneToOne = true).getOrThrow().isDefault)
            .isTrue()
    }
}

private const val MOMENT_MUTED_CHATS_ACCOUNT_DATA_TYPE = "io.moment.muted_chats"

private fun String?.expiryFor(roomId: RoomId): Long {
    return Json.parseToJsonElement(checkNotNull(this))
        .jsonObject
        .getValue(roomId.value)
        .jsonPrimitive
        .long
}
