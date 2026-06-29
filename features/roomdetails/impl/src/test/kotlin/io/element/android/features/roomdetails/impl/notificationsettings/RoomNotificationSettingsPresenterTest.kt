/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.roomdetails.impl.notificationsettings

import com.google.common.truth.Truth.assertThat
import io.element.android.features.roomdetails.impl.aJoinedRoom
import io.element.android.libraries.matrix.api.room.RoomNotificationMode
import io.element.android.libraries.matrix.test.AN_EXCEPTION
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.notificationsettings.FakeNotificationSettingsService
import io.element.android.libraries.matrix.test.room.FakeJoinedRoom
import io.element.android.tests.testutils.consumeItemsUntilPredicate
import io.element.android.tests.testutils.test
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RoomNotificationSettingsPresenterTest {
    @Test
    fun `present - initial state is created from room info`() = runTest {
        val presenter = createRoomNotificationSettingsPresenter()
        presenter.test {
            val initialState = awaitItem()
            assertThat(initialState.roomNotificationSettings.dataOrNull()).isNull()
            assertThat(initialState.defaultRoomNotificationMode).isNull()
            val loadedState = awaitItem()
            assertThat(loadedState.displayMentionsOnlyDisclaimer).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - fetch notification settings uses room encryption and active member count`() = runTest {
        val notificationSettingsService = FakeNotificationSettingsService()
        val room = aJoinedRoom(
            notificationSettingsService = notificationSettingsService,
            isEncrypted = true,
            activeMemberCount = 2,
        )
        val presenter = createRoomNotificationSettingsPresenter(notificationSettingsService, room)
        presenter.test {
            consumeItemsUntilPredicate {
                it.roomNotificationSettings.isSuccess() && it.defaultRoomNotificationMode != null
            }
            assertThat(notificationSettingsService.lastGetRoomNotificationSettingsRequest).isEqualTo(
                FakeNotificationSettingsService.RoomNotificationSettingsRequest(
                    roomId = room.roomId,
                    isEncrypted = true,
                    isOneToOne = true,
                )
            )
            assertThat(notificationSettingsService.lastGetDefaultRoomNotificationModeRequest).isEqualTo(
                FakeNotificationSettingsService.DefaultRoomNotificationModeRequest(
                    isEncrypted = true,
                    isOneToOne = true,
                )
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - notification mode changed`() = runTest {
        val presenter = createRoomNotificationSettingsPresenter()
        presenter.test {
            awaitItem().eventSink(RoomNotificationSettingsEvent.ChangeRoomNotificationMode(RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY))
            val updatedState = consumeItemsUntilPredicate {
                it.roomNotificationSettings.dataOrNull()?.mode == RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY
            }.last()
            assertThat(updatedState.roomNotificationSettings.dataOrNull()?.mode).isEqualTo(RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - observe notification mode changed`() = runTest {
        val notificationSettingsService = FakeNotificationSettingsService()
        val presenter = createRoomNotificationSettingsPresenter(notificationSettingsService)
        presenter.test {
            notificationSettingsService.setRoomNotificationMode(A_ROOM_ID, RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY)
            val updatedState = consumeItemsUntilPredicate {
                it.roomNotificationSettings.dataOrNull()?.mode == RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY
            }.last()
            assertThat(updatedState.roomNotificationSettings.dataOrNull()?.mode).isEqualTo(RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY)
        }
    }

    @Test
    fun `present - notification settings set custom failed`() = runTest {
        val notificationSettingsService = FakeNotificationSettingsService()
        notificationSettingsService.givenSetNotificationModeError(AN_EXCEPTION)
        val presenter = createRoomNotificationSettingsPresenter(notificationSettingsService)
        presenter.test {
            val initialState = awaitItem()
            initialState.eventSink(RoomNotificationSettingsEvent.SetNotificationMode(false))
            val failedState = consumeItemsUntilPredicate {
                it.setNotificationSettingAction.isFailure()
            }.last()

            assertThat(failedState.roomNotificationSettings.dataOrNull()?.isDefault).isTrue()
            assertThat(failedState.pendingSetDefault).isNull()
            assertThat(failedState.setNotificationSettingAction.isFailure()).isTrue()

            failedState.eventSink(RoomNotificationSettingsEvent.ClearSetNotificationError)

            val errorClearedState = consumeItemsUntilPredicate {
                it.setNotificationSettingAction.isUninitialized()
            }.last()
            assertThat(errorClearedState.setNotificationSettingAction.isUninitialized()).isTrue()
        }
    }

    @Test
    fun `present - notification settings set custom`() = runTest {
        val notificationSettingsService = FakeNotificationSettingsService()
        val presenter = createRoomNotificationSettingsPresenter(notificationSettingsService)
        presenter.test {
            val initialState = awaitItem()
            initialState.eventSink(RoomNotificationSettingsEvent.SetNotificationMode(false))
            skipItems(3)
            val defaultState = awaitItem()
            assertThat(defaultState.roomNotificationSettings.dataOrNull()?.isDefault).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - notification settings restore default`() = runTest {
        val presenter = createRoomNotificationSettingsPresenter()
        presenter.test {
            val initialState = awaitItem()
            initialState.eventSink(RoomNotificationSettingsEvent.ChangeRoomNotificationMode(RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY))
            initialState.eventSink(RoomNotificationSettingsEvent.SetNotificationMode(true))
            val defaultState = consumeItemsUntilPredicate {
                it.roomNotificationSettings.dataOrNull()?.mode == RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY
            }.last()
            assertThat(defaultState.roomNotificationSettings.dataOrNull()?.mode).isEqualTo(RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - notification settings restore default failed`() = runTest {
        val notificationSettingsService = FakeNotificationSettingsService()
        notificationSettingsService.givenRestoreDefaultNotificationModeError(AN_EXCEPTION)
        val presenter = createRoomNotificationSettingsPresenter(notificationSettingsService)
        presenter.test {
            val initialState = awaitItem()
            initialState.eventSink(RoomNotificationSettingsEvent.ChangeRoomNotificationMode(RoomNotificationMode.MENTIONS_AND_KEYWORDS_ONLY))
            initialState.eventSink(RoomNotificationSettingsEvent.SetNotificationMode(true))
            val failedState = consumeItemsUntilPredicate {
                it.restoreDefaultAction.isFailure()
            }.last()
            assertThat(failedState.restoreDefaultAction.isFailure()).isTrue()
            failedState.eventSink(RoomNotificationSettingsEvent.ClearRestoreDefaultError)

            val errorClearedState = consumeItemsUntilPredicate {
                it.restoreDefaultAction.isUninitialized()
            }.last()
            assertThat(errorClearedState.restoreDefaultAction.isUninitialized()).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - displays mentions only warning when encrypted push is unsupported`() = runTest {
        val notificationService = FakeNotificationSettingsService().apply {
            givenCanHomeServerPushEncryptedEventsToDeviceResult(Result.success(false))
        }
        val room = aJoinedRoom(notificationSettingsService = notificationService, isEncrypted = true)
        val presenter = createRoomNotificationSettingsPresenter(notificationService, room)
        presenter.test {
            val loadedState = consumeItemsUntilPredicate { it.displayMentionsOnlyDisclaimer }.last()
            assertThat(loadedState.displayMentionsOnlyDisclaimer).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `present - does not display mentions only warning when room is unencrypted`() = runTest {
        val notificationService = FakeNotificationSettingsService().apply {
            givenCanHomeServerPushEncryptedEventsToDeviceResult(Result.success(false))
        }
        val room = aJoinedRoom(notificationSettingsService = notificationService, isEncrypted = false)
        val presenter = createRoomNotificationSettingsPresenter(notificationService, room)
        presenter.test {
            val loadedState = consumeItemsUntilPredicate { it.roomNotificationSettings.isSuccess() }.last()
            assertThat(loadedState.displayMentionsOnlyDisclaimer).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createRoomNotificationSettingsPresenter(
        notificationSettingsService: FakeNotificationSettingsService = FakeNotificationSettingsService(),
        room: FakeJoinedRoom = aJoinedRoom(notificationSettingsService = notificationSettingsService),
    ): RoomNotificationSettingsPresenter {
        return RoomNotificationSettingsPresenter(
            room = room,
            notificationSettingsService = notificationSettingsService,
            showUserDefinedSettingStyle = false,
        )
    }
}
